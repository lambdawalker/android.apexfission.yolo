"""Destination-independent module source identities. Git tags are never moved."""
import hashlib
import json
import re
import subprocess

SEMVER = r'(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)'

def key(version):
    if not re.fullmatch(SEMVER,version): raise ValueError('Expected X.Y.Z')
    return tuple(map(int,version.split('.')))

def canonical(module, version):
    if module not in ('yolo',): raise ValueError('Unknown module')
    key(version)
    return f'{module}/v{version}'

def fingerprint(module, source, git):
    paths = [f'{module}/src/main',f'{module}/build.gradle.kts',f'{module}/README.md',
             f'{module}/docs',f'{module}/consumer-rules.keep',f'{module}/proguard-rules.pro',
             'build.gradle.kts','settings.gradle.kts','gradle','gradlew','gradlew.bat',
             'LICENSE','jitpack.yml','scripts','publishing','.github/workflows/publish-library.yml','docs/agents']
    entries=git('ls-tree','-r',source,'--',*paths).splitlines()
    # Generated registry dropdowns/history are not source inputs. Shared scripts
    # are deliberately conservative: build protocol changes require a new tag.
    entries=[e for e in entries if not e.split('\t',1)[-1].startswith('scripts/tests/')]
    props=git('show',f'{source}:gradle.properties')
    return hashlib.sha256(('\n'.join(entries)+'\n'+props).encode()).hexdigest()

def history(module,git,tags,records):
    found={}
    pattern=re.compile(r'(?:[a-z][a-z0-9-]*/)?'+re.escape(module)+r'/v('+SEMVER+r')')
    for name in tags:
        match=pattern.fullmatch(name) or re.fullmatch(r'release-(?:pending|uploading)/(?:[a-z][a-z0-9-]*/)?'+re.escape(module)+r'/('+SEMVER+r')',name)
        if not match: match=re.fullmatch(r'v('+SEMVER+r')',name)
        if match:
            version=match[1];source=git('rev-parse',f'{name}^{{commit}}')
            found.setdefault(version,set()).add(source)
    for record in records:
        if record and record['module']==module:
            key(record['version'])
            # A record is only provenance when its immutable ref still agrees.
            name=record.get('legacy_tag') or canonical(module,record['version'])
            if name not in tags:
                dest=record.get('repository','maven-central')
                name=f'{dest}/{module}/v{record["version"]}'
            if name not in tags or git('rev-parse',f'{name}^{{commit}}')!=record['source']:
                raise ValueError('Confirmed release is missing its source tag')
            found.setdefault(record['version'],set()).add(record['source'])
    for version,sources in found.items():
        if len(sources)!=1:raise ValueError(f'Conflicting source identities for {module} {version}; reconcile history before publishing')
    return {v:next(iter(s)) for v,s in found.items()}

def select(module,source,git,tags,records,requested=''):
    releases=history(module,git,tags,records)
    if requested:key(requested)
    if not releases:return requested or '0.1.0',source
    latest=max(releases,key=key);original=releases[latest]
    try:git('merge-base','--is-ancestor',original,source)
    except subprocess.CalledProcessError:
        raise ValueError('Latest module release must be an ancestor of selected source') from None
    same=fingerprint(module,source,git)==fingerprint(module,original,git)
    if requested:
        if key(requested)<key(latest) or (requested==latest and not same):
            raise ValueError('Requested version is older or belongs to different release inputs')
        if requested!=latest:return requested,source
    if same:return latest,original
    x,y,z=key(latest)
    return f'{x}.{y}.{z+1}',source
