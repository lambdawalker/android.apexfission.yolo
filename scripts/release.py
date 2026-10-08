"""Signed Android release policy, durable attempt journal, and deterministic docs.

Standard library only. Network failures are never treated as an empty registry.
"""
import argparse
import hashlib
import io
import json
import os
from pathlib import Path
import re
import subprocess
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CENTRAL = 'https://repo.maven.apache.org/maven2'
SEMVER = r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)'
SUFFIXES = ('.pom', '.aar', '-sources.jar', '-javadoc.jar', '.module')
DOC_FILES = ('IMPORT.md', 'docs/release.json')
INSTALL_INPUTS = (*DOC_FILES, 'docs/templates/IMPORT.md.template', 'gradle.properties',
                  'build.gradle.kts', 'yolo/build.gradle.kts', 'app/build.gradle.kts', 'settings.gradle.kts', 'gradle/libs.versions.toml',
                  'scripts', '.github/workflows/publish-yolo.yml', '.github/workflows/finalize-yolo.yml')


def version_key(value):
    if not re.fullmatch(SEMVER, value):
        raise ValueError(f'Expected stable X.Y.Z, got {value!r}')
    return tuple(map(int, value.split('.')))


def next_version(tags, published, initial=''):
    tagged = {t[1:] for t in tags if re.fullmatch('v' + SEMVER, t)}
    stable = {v for v in published if re.fullmatch(SEMVER, v)}
    if tagged - stable:
        raise ValueError(f'Tags not published on Central: {sorted(tagged - stable)}')
    if initial:
        version_key(initial)
        if stable or tagged:
            raise ValueError('initial_version is only permitted with no stable release history')
        return initial
    if not stable:
        raise ValueError('First release requires explicit initial_version (for example 0.1.0)')
    latest = max(stable, key=version_key)
    if tagged and max(tagged, key=version_key) != latest:
        raise ValueError('Central is ahead of tags; reconcile source provenance')
    major, minor, patch = version_key(latest)
    return f'{major}.{minor}.{patch + 1}'


def coordinates():
    values = {}
    for line in (ROOT / 'gradle.properties').read_text().splitlines():
        if '=' in line and not line.lstrip().startswith('#'):
            k, v = line.split('=', 1)
            values[k.strip()] = v.strip()
    group, artifact = values['GROUP'], values['POM_ARTIFACT_ID']
    if not re.fullmatch(r'[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+', group):
        raise ValueError('Invalid groupId')
    if not re.fullmatch(r'[A-Za-z0-9_.-]+', artifact):
        raise ValueError('Invalid artifactId')
    return group, artifact


def fetch(url, missing=False, attempts=3):
    for attempt in range(attempts):
        try:
            with urllib.request.urlopen(url, timeout=20) as response:
                return response.read()
        except urllib.error.HTTPError as error:
            if error.code == 404 and missing:
                return None
            if error.code != 429 and error.code < 500:
                raise
            if attempt == attempts - 1:
                raise
        except (urllib.error.URLError, TimeoutError):
            if attempt == attempts - 1:
                raise
        time.sleep(2 ** attempt)


def artifact_base(group, artifact):
    return f'{CENTRAL}/{group.replace(".", "/")}/{artifact}'


def published_versions(group, artifact):
    data = fetch(artifact_base(group, artifact) + '/maven-metadata.xml', missing=True)
    if data is None:
        return []
    xml = ET.fromstring(data)
    if xml.tag != 'metadata' or xml.findtext('groupId') != group or xml.findtext('artifactId') != artifact:
        raise ValueError('Invalid Central metadata identity')
    versions = xml.findall('./versioning/versions/version')
    if not versions or any(not node.text for node in versions):
        raise ValueError('Malformed/empty Central metadata')
    return [node.text for node in versions]


def coordinates_dependency(properties=None):
    properties = properties if properties is not None else (ROOT / 'gradle.properties').read_text()
    dependency = next((line.split('=', 1)[1].strip() for line in properties.splitlines()
                       if line.startswith('COORDINATES_DEPENDENCY=')), '')
    if len(dependency.split(':')) != 3 or not all(dependency.split(':')):
        raise ValueError('Missing explicit public Coordinates dependency')
    return tuple(dependency.split(':'))


def verify_pom(data, group, artifact, version, expected_dependency=None):
    xml = ET.fromstring(data)
    for node in xml.iter():
        node.tag = node.tag.split('}')[-1]
    if tuple(xml.findtext(k) for k in ('groupId', 'artifactId', 'version')) != (group, artifact, version):
        raise ValueError('POM coordinates/version mismatch')
    if xml.findtext('packaging') != 'aar':
        raise ValueError('Publication must be an Android AAR')
    for path in ('name', 'description', 'url', 'licenses/license/name', 'licenses/license/url',
                 'developers/developer/id', 'developers/developer/name', 'scm/url', 'scm/connection'):
        if not xml.findtext(path):
            raise ValueError(f'Missing POM metadata: {path}')
    dependencies = xml.findall('dependencies/dependency')
    expected = expected_dependency or coordinates_dependency()
    if not any(tuple(n.findtext(k) for k in ('groupId', 'artifactId', 'version')) == expected
               and n.findtext('scope', 'compile') == 'compile' for n in dependencies):
        raise ValueError('Public coordinates dependency must be exported with compile scope')
    if any(n.findtext('artifactId') == 'unspecified' or n.findtext('version') == 'unspecified'
           for n in dependencies):
        raise ValueError('Unresolved project dependency in POM')


def verify_artifact(data, suffix, group, artifact, version, expected_dependency=None):
    if suffix == '.pom':
        verify_pom(data, group, artifact, version, expected_dependency)
    elif suffix == '.module':
        module = json.loads(data)
        if tuple(module.get('component', {}).get(k) for k in ('group', 'module', 'version')) != (group, artifact, version):
            raise ValueError('Gradle module coordinates mismatch')
        api_variants = [v for v in module.get('variants', [])
                        if v.get('attributes', {}).get('org.gradle.usage') == 'java-api']
        expected = expected_dependency or coordinates_dependency()
        if not api_variants or any(not any(
                (d.get('group'), d.get('module'), d.get('version', {}).get('requires')) == expected
                for d in variant.get('dependencies', [])) for variant in api_variants):
            raise ValueError('Public coordinates dependency must be exported in Gradle API metadata')

    else:
        with zipfile.ZipFile(io.BytesIO(data)) as archive:
            if archive.testzip():
                raise ValueError('Corrupt JAR')
            names = archive.namelist()
            if suffix == '.aar':
                if 'AndroidManifest.xml' not in names or 'classes.jar' not in names:
                    raise ValueError('AAR is missing manifest or classes.jar')
                with zipfile.ZipFile(io.BytesIO(archive.read('classes.jar'))) as classes_jar:
                    classes = [n for n in classes_jar.namelist()
                               if n.endswith('.class') and n.startswith('com/apexfission/android/yolo/')]
                    if not classes or any(classes_jar.read(n)[:4] != b'\xca\xfe\xba\xbe' or
                                          int.from_bytes(classes_jar.read(n)[6:8], 'big') != 61 for n in classes):
                        raise ValueError('Missing YOLO classes or unexpected JVM bytecode target')
            elif suffix == '-sources.jar' and not any(n.endswith('.kt') for n in names):
                raise ValueError('Sources JAR has no Kotlin source')
            elif suffix == '-javadoc.jar' and not any(n.endswith(('.md', '.html')) for n in names):
                raise ValueError('Documentation JAR has no documentation')


def zip_contents(data):
    with zipfile.ZipFile(io.BytesIO(data)) as archive:
        if len(archive.namelist()) != len(set(archive.namelist())):
            raise ValueError('Duplicate archive entries')
        return {name: hashlib.sha256(archive.read(name)).hexdigest() for name in archive.namelist()
                if not name.endswith('/') and name != 'META-INF/MANIFEST.MF'}


def git(*args):
    return subprocess.check_output(['git', *args], cwd=ROOT, text=True).strip()


def refresh():
    git('fetch', 'origin', 'main', '--tags')


def remote_ref(name):
    result = git('ls-remote', '--refs', 'origin', f'refs/tags/{name}')
    return result.split()[0] if result else None


def read_record(ref):
    if git('cat-file', '-t', f'refs/tags/{ref}') != 'tag':
        raise ValueError('Release journal must be an annotated tag')
    record = json.loads(git('cat-file', '-p', f'refs/tags/{ref}').split('\n\n', 1)[1])
    version_key(record['version'])
    if git('rev-parse', f'refs/tags/{ref}^{{commit}}') != record['source']:
        raise ValueError('Journal source mismatch')
    if set(record['sha256']) != set(SUFFIXES):
        raise ValueError('Journal publication set mismatch')
    return record


def outputs(values):
    text = ''.join(f'{k}={v}\n' for k, v in values.items())
    print(text, end='')
    if os.environ.get('GITHUB_OUTPUT'):
        with open(os.environ['GITHUB_OUTPUT'], 'a') as stream:
            stream.write(text)
    return values


def prepare(resume='', initial=''):
    refresh()
    tags = git('tag', '--list').splitlines()
    # Use remote pending state; stale local deleted tags must not block recovery.
    remote_tags = [line.split('refs/tags/', 1)[1] for line in git('ls-remote', '--refs', '--tags', 'origin').splitlines()]
    pending = [t for t in remote_tags if t.startswith('release-pending/')]
    uploading = [t for t in remote_tags if t.startswith('release-uploading/')]
    if resume:
        version_key(resume)
        if initial:
            raise ValueError('Recovery cannot accept initial_version')
        if pending == [f'release-pending/{resume}']:
            record = read_record(pending[0])
            if record['version'] != resume:
                raise ValueError('Journal version mismatch')
            if f'v{resume}' in remote_tags and git('rev-parse', f'v{resume}^{{commit}}') != record['source']:
                raise ValueError('Existing stable tag has different source')
            completed = 'false'
        elif not pending and not uploading and f'v{resume}' in remote_tags:
            record = json.loads(git('show', 'origin/main:docs/release.json'))
            if not record or record['version'] != resume or git('rev-parse', f'v{resume}^{{commit}}') != record['source']:
                raise ValueError('Completed release does not match recorded source/version')
            completed = 'true'
        else:
            raise ValueError('Recovery requires the matching pending or completed release')
        source, version = record['source'], resume
    else:
        if pending or uploading:
            raise ValueError(f'Unresolved release attempt: {pending + uploading}; use recovery')
        group, artifact = coordinates()
        version = next_version(tags, published_versions(group, artifact), initial)
        source = git('rev-parse', 'HEAD')
        if any(git('rev-parse', f'{t}^{{commit}}') == source for t in tags if re.fullmatch('v' + SEMVER, t)):
            raise ValueError('Source already has a release tag')
        completed = 'false'
    git('merge-base', '--is-ancestor', source, 'origin/main')
    return outputs(dict(version=version, source=source, completed=completed))


def prepare_finalization(version, source=''):
    """Re-read remote state under the shared release job lock; never allocate/upload."""
    version_key(version)
    if source and not re.fullmatch('[0-9a-f]{40}', source):
        raise ValueError('Invalid expected source SHA')
    refresh()
    current = json.loads(git('show', 'origin/main:docs/release.json'))
    pending = remote_ref(f'release-pending/{version}')
    stable = remote_ref(f'v{version}')
    if not pending and stable and not remote_ref(f'release-uploading/{version}'):
        tagged_source = git('rev-parse', f'v{version}^{{commit}}')
        if source and source != tagged_source:
            raise ValueError('Completed release has conflicting source')
        if not current or current['phase'] != 'confirmed-public' or version_key(current['version']) < version_key(version):
            raise ValueError('Stable tag lacks matching or newer confirmed documentation')
        if not remote_ref(f'v{current["version"]}') or git('rev-parse', f'v{current["version"]}^{{commit}}') != current['source']:
            raise ValueError('Confirmed documentation has conflicting source')
        git('merge-base', '--is-ancestor', tagged_source, current['source'])
        git('merge-base', '--is-ancestor', current['source'], 'origin/main')
        print(f'Release {version} already finalized; documentation is at {current["version"]}. Nothing to update.')
        return outputs(dict(version=version, source=tagged_source, skip='true'))
    if current and version_key(current['version']) >= version_key(version):
        raise ValueError('Pending release would overwrite same/newer documentation; reconcile markers')
    selected = prepare(resume=version)
    if source and source != selected['source']:
        raise ValueError('Pending release has conflicting source')
    return outputs(dict(version=version, source=selected['source'], skip='false'))


def local_record(version, source):
    version_key(version)
    if git('rev-parse', 'HEAD') != source:
        raise ValueError('Build checkout differs from selected source')
    git('diff', '--exit-code', source, '--', '.')
    group, artifact = coordinates()
    directory = ROOT / 'build/verification-repository' / group.replace('.', '/') / artifact / version
    hashes = {}
    for suffix in SUFFIXES:
        data = (directory / f'{artifact}-{version}{suffix}').read_bytes()
        verify_artifact(data, suffix, group, artifact, version)
        hashes[suffix] = hashlib.sha256(data).hexdigest()
    return dict(version=version, source=source, group=group, artifact=artifact, sha256=hashes,
                phase='reserved-before-upload', workflow_run=os.environ.get('GITHUB_RUN_ID', 'local'))


def reserve(version, source):
    record = local_record(version, source)
    base = f'{artifact_base(record["group"], record["artifact"])}/{version}/{record["artifact"]}-{version}'
    if any(fetch(base + suffix, missing=True) is not None for suffix in SUFFIXES):
        raise ValueError('Proposed immutable version already has public artifacts; reconcile before reserving')
    refresh()
    if any('refs/tags/release-' in row for row in git('ls-remote', '--refs', '--tags', 'origin').splitlines()):
        raise ValueError('An unresolved remote release attempt already exists')
    git('merge-base', '--is-ancestor', source, 'origin/main')
    name = f'release-pending/{version}'
    git('tag', '-a', name, source, '-m', json.dumps(record, sort_keys=True))
    git('push', 'origin', f'refs/tags/{name}')
    print('Reserved version/source/artifact hashes before upload')


def guard(version):
    version_key(version)
    name = f'release-pending/{version}'
    if remote_ref(name) != git('rev-parse', f'refs/tags/{name}'):
        raise ValueError('Missing/mismatched remote reservation')
    record = read_record(name)
    if record['version'] != version or record['source'] != git('rev-parse', 'HEAD') or (record['group'], record['artifact']) != coordinates():
        raise ValueError('Reservation differs from requested publication')
    if remote_ref(f'v{version}'):
        raise ValueError('Stable version already finalized')
    marker = f'release-uploading/{version}'
    if remote_ref(marker):
        raise ValueError('Upload already started; inspect Central and use recovery, never re-upload')
    # Atomic create (no force) arbitrates manual executions as well as workflow reruns.
    git('tag', '-a', marker, record['source'], '-m', f'Upload may have started; reservation {name}')
    git('push', 'origin', f'refs/tags/{marker}')


def verify_public(record):
    version, group, artifact = (record[k] for k in ('version', 'group', 'artifact'))
    base = f'{artifact_base(group, artifact)}/{version}/{artifact}-{version}'
    for suffix in SUFFIXES:
        data = fetch(base + suffix)
        verify_artifact(data, suffix, group, artifact, version)
        if hashlib.sha256(data).hexdigest() != record['sha256'][suffix]:
            raise ValueError(f'Public {suffix} hash differs from reserved build; reconcile provenance')
        signature = fetch(base + suffix + '.asc')
        if b'BEGIN PGP SIGNATURE' not in signature:
            raise ValueError(f'Missing/invalid signature for {suffix}')


def wait_for_publication(record, timeout=2400):
    deadline = time.monotonic() + timeout
    while True:
        try:
            verify_public(record)
            return
        except urllib.error.HTTPError as error:
            if error.code not in (404, 429) and error.code < 500:
                raise
            last = error
        except (urllib.error.URLError, TimeoutError) as error:
            last = error
        if time.monotonic() >= deadline:
            raise TimeoutError('Public artifact set not confirmed; keep pending state and inspect Central') from last
        time.sleep(min(20, max(0, deadline - time.monotonic())))


def render(template, record):
    if record is None:
        status = '**No Maven Central release has been confirmed yet.**'
        installation = 'Installation snippets will appear here after the first successful publication.\nFor now, use the [source-module instructions](README.md#use-as-a-source-module).'
    else:
        version_key(record['version'])
        group, artifact, version = (record[k] for k in ('group', 'artifact', 'version'))
        gav = f'{group}:{artifact}:{version}'
        status = f'Confirmed release: **{version}** · Maven coordinates: `{gav}`.'
        installation = f'''## Gradle Kotlin DSL

Add `mavenCentral()` to your settings repositories, then:

```kotlin
dependencies {{
    implementation("{gav}")
}}
```

## Gradle Groovy DSL

```groovy
dependencies {{
    implementation '{gav}'
}}
```

## Version catalog

```toml
[versions]
apexfission-yolo = "{version}"

[libraries]
apexfission-yolo = {{ module = "{group}:{artifact}", version.ref = "apexfission-yolo" }}
```

```kotlin
implementation(libs.apexfission.yolo)
```

## Maven

```xml
<dependency>
    <groupId>{group}</groupId>
    <artifactId>{artifact}</artifactId>
    <version>{version}</version>
    <type>aar</type>
</dependency>
```

Built from source commit [`{record['source']}`](https://github.com/lambdawalker/android.apexfission.yolo/commit/{record['source']}).'''
    for key, value in dict(STATUS=status, INSTALLATION=installation).items():
        template = template.replace('{{' + key + '}}', value)
    if '{{' in template or '}}' in template:
        raise ValueError('Unknown template placeholder')
    return template


def documentation(verify=False):
    record = json.loads((ROOT / 'docs/release.json').read_text())
    expected = render((ROOT / 'docs/templates/IMPORT.md.template').read_text(), record)
    if verify:
        if record and (record['group'], record['artifact']) != coordinates():
            raise ValueError('Development coordinates differ from confirmed release; do not advertise them as released')
        if (ROOT / 'IMPORT.md').read_text() != expected:
            raise ValueError('IMPORT.md drift; run ./gradlew generateImportDocs')
    else:
        (ROOT / 'IMPORT.md').write_text(expected, encoding='utf-8', newline='\n')


def confirm(version, completed=False, timeout=2400):
    version_key(version)
    record = (json.loads(git('show', 'origin/main:docs/release.json')) if completed
              else read_record(f'release-pending/{version}'))
    if record['version'] != version:
        raise ValueError('Confirmation version mismatch')
    wait_for_publication(record, timeout)
    # Only confirmed public artifacts may change committed installation metadata.
    if not completed:
        record['phase'] = 'confirmed-public'
        (ROOT / 'docs/release.json').write_text(json.dumps(record, indent=2, sort_keys=True) + '\n')
        documentation()
    print(f'Confirmed complete signed publication {record["group"]}:{record["artifact"]}:{version}')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    for name in ('generate', 'verify'):
        sub.add_parser(name)
    p = sub.add_parser('prepare')
    p.add_argument('--resume', default='')
    p.add_argument('--initial', default='')
    p = sub.add_parser('prepare-finalization')
    p.add_argument('--version', required=True)
    p.add_argument('--source', default='')
    for name in ('check-local', 'reserve', 'guard', 'confirm'):
        p = sub.add_parser(name)
        p.add_argument('--version', required=True)
        if name in ('reserve', 'check-local'):
            p.add_argument('--source', required=True)
        if name == 'confirm':
            p.add_argument('--completed', action='store_true')
            p.add_argument('--timeout', type=int, default=2400)
    args = parser.parse_args()
    if args.command in ('generate', 'verify'):
        # Keep legacy functions for original-journal recovery; CLI uses current confirmed pointers.
        import module_release
        module_release.documentation(args.command == 'verify')
    elif args.command == 'prepare':
        prepare(args.resume, args.initial)
    elif args.command == 'prepare-finalization':
        prepare_finalization(args.version, args.source)
    elif args.command == 'check-local':
        print(json.dumps(local_record(args.version, args.source), indent=2))
    elif args.command == 'reserve':
        reserve(args.version, args.source)
    elif args.command == 'guard':
        guard(args.version)
    elif args.command == 'confirm':
        confirm(args.version, args.completed, args.timeout)


if __name__ == '__main__':
    main()
