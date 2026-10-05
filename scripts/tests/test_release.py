import hashlib
import importlib.util
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import urllib.error
import zipfile

SPEC = importlib.util.spec_from_file_location('release', Path(__file__).parents[1] / 'release.py')
release = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(release)


def pom(version='1.2.3'):
    return f'''<project xmlns="http://maven.apache.org/POM/4.0.0"><groupId>org.example</groupId>
<artifactId>yolo</artifactId><version>{version}</version><packaging>aar</packaging><name>Coordinates</name>
<description>Geometry</description><url>https://example.org</url>
<licenses><license><name>Apache-2.0</name><url>https://example.org/license</url></license></licenses>
<developers><developer><id>owner</id><name>Owner</name></developer></developers>
<scm><url>https://example.org</url><connection>scm:git:https://example.org</connection></scm>
<dependencies><dependency><groupId>com.apexfission.android.math</groupId><artifactId>coordinates</artifactId>
<version>0.1.0</version><scope>compile</scope></dependency></dependencies></project>'''.encode()


def jar(name, data):
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, 'w') as archive:
        archive.writestr(name, data)
    return stream.getvalue()


def aar(bytecode=61):
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, 'w') as archive:
        archive.writestr('AndroidManifest.xml', '<manifest/>')
        archive.writestr('classes.jar', jar('com/apexfission/android/yolo/Detection.class',
                                         b'\xca\xfe\xba\xbe\x00\x00' + bytecode.to_bytes(2, 'big')))
    return stream.getvalue()


def artifacts():
    return {'.pom': pom(), '.aar': aar(),
            '-sources.jar': jar('Detection.kt', 'class Detection'), '-javadoc.jar': jar('README.md', 'YOLO'),
            '.module': json.dumps({'component': {'group': 'org.example', 'module': 'yolo', 'version': '1.2.3'}}).encode()}


def record():
    return dict(version='1.2.3', source='a' * 40, group='org.example', artifact='yolo',
                sha256={s: hashlib.sha256(d).hexdigest() for s, d in artifacts().items()}, phase='reserved-before-upload')


class PolicyTests(unittest.TestCase):
    def test_semantic_order(self):
        self.assertEqual(release.next_version(['v0.1.9', 'v0.1.10', 'other', 'v2.0.0-rc1'], ['0.1.9', '0.1.10']), '0.1.11')

    def test_initial_requires_explicit_value(self):
        with self.assertRaisesRegex(ValueError, 'initial_version'):
            release.next_version([], [])
        self.assertEqual(release.next_version([], [], '0.1.0'), '0.1.0')

    def test_bootstrap_existing_metadata_without_fabricated_tags(self):
        self.assertEqual(release.next_version([], ['0.1.9', '0.1.10', '3.0.0-rc1']), '0.1.11')

    def test_every_stable_tag_must_exist(self):
        with self.assertRaisesRegex(ValueError, 'not published'):
            release.next_version(['v0.1.0', 'v0.1.1'], ['0.1.1'])

    def test_central_ahead_stops(self):
        with self.assertRaisesRegex(ValueError, 'ahead'):
            release.next_version(['v0.1.0'], ['0.1.0', '0.1.1'])

    def test_initial_cannot_override_existing_history(self):
        with self.assertRaises(ValueError):
            release.next_version([], ['0.1.0'], '2.0.0')

    def test_stable_versions_only(self):
        for value in ['01.2.3', '1.2', 'v1.2.3', '1.2.3-SNAPSHOT', '1.2.3-rc1', '$(id)', '1.2.3\n']:
            with self.subTest(value=value), self.assertRaises(ValueError):
                release.version_key(value)

    def test_404_metadata_is_empty(self):
        with patch.object(release, 'fetch', return_value=None):
            self.assertEqual(release.published_versions('org.example', 'yolo'), [])

    def test_metadata_network_failure_is_not_empty(self):
        with patch.object(release, 'fetch', side_effect=urllib.error.URLError('offline')):
            with self.assertRaises(urllib.error.URLError):
                release.published_versions('org.example', 'yolo')

    def test_metadata_identity_validation(self):
        with patch.object(release, 'fetch', return_value=b'<metadata><groupId>wrong</groupId></metadata>'):
            with self.assertRaises(ValueError):
                release.published_versions('org.example', 'yolo')

    def test_empty_malformed_metadata_is_not_empty_history(self):
        with patch.object(release, 'fetch', return_value=b'<metadata><groupId>org.example</groupId><artifactId>yolo</artifactId></metadata>'):
            with self.assertRaises(ValueError):
                release.published_versions('org.example', 'yolo')

    def test_transient_retry_is_bounded(self):
        with patch.object(release.urllib.request, 'urlopen', side_effect=urllib.error.URLError('offline')) as request, patch.object(release.time, 'sleep'):
            with self.assertRaises(urllib.error.URLError):
                release.fetch('https://example.invalid')
            self.assertEqual(request.call_count, 3)

    def test_permanent_http_error_is_not_retried(self):
        error = urllib.error.HTTPError('url', 403, 'Forbidden', {}, None)
        with patch.object(release.urllib.request, 'urlopen', side_effect=error) as request:
            with self.assertRaises(urllib.error.HTTPError):
                release.fetch('https://example.invalid', missing=True)
            self.assertEqual(request.call_count, 1)


class ArtifactTests(unittest.TestCase):
    def test_validate_all_unsigned_artifacts(self):
        for suffix, data in artifacts().items():
            release.verify_artifact(data, suffix, 'org.example', 'yolo', '1.2.3')

    def test_wrong_pom_version(self):
        with self.assertRaises(ValueError):
            release.verify_pom(pom('1.2.4'), 'org.example', 'yolo', '1.2.3')

    def test_wrong_packaging(self):
        with self.assertRaises(ValueError):
            release.verify_pom(pom().replace(b'<packaging>aar</packaging>', b'<packaging>jar</packaging>'), 'org.example', 'yolo', '1.2.3')

    def test_coordinates_cannot_be_runtime_only(self):
        with self.assertRaises(ValueError):
            release.verify_pom(pom().replace(b'<scope>compile</scope>', b'<scope>runtime</scope>'), 'org.example', 'yolo', '1.2.3')

    def test_missing_coordinates_dependency(self):
        with self.assertRaises(ValueError):
            release.verify_pom(pom().replace(b'<artifactId>coordinates</artifactId>', b'<artifactId>other</artifactId>'), 'org.example', 'yolo', '1.2.3')

    def test_aar_requires_manifest_and_classes(self):
        for data in (jar('classes.jar', jar('Other.class', b'')), jar('AndroidManifest.xml', '<manifest/>')):
            with self.subTest(data=data), self.assertRaisesRegex(ValueError, 'missing manifest or classes'):
                release.verify_artifact(data, '.aar', 'org.example', 'yolo', '1.2.3')

    def test_wrong_bytecode_target(self):
        with self.assertRaises(ValueError):
            release.verify_artifact(aar(55), '.aar', 'org.example', 'yolo', '1.2.3')

    def test_empty_companion_jars(self):
        for suffix in ('-sources.jar', '-javadoc.jar'):
            with self.subTest(suffix=suffix), self.assertRaises(ValueError):
                release.verify_artifact(jar('META-INF/MANIFEST.MF', ''), suffix, 'org.example', 'yolo', '1.2.3')

    def test_public_checks_every_artifact_and_signature(self):
        files = artifacts()
        def fetch(url):
            if url.endswith('.asc'):
                return b'-----BEGIN PGP SIGNATURE-----'
            return next(data for suffix, data in files.items() if url.endswith('yolo-1.2.3' + suffix))
        with patch.object(release, 'fetch', side_effect=fetch) as request:
            release.verify_public(record())
            self.assertEqual(request.call_count, len(release.SUFFIXES) * 2)

    def test_hash_mismatch_stops(self):
        data = record()
        data['sha256']['.pom'] = 'bad'
        with patch.object(release, 'fetch', return_value=pom()):
            with self.assertRaisesRegex(ValueError, 'hash'):
                release.verify_public(data)

    def test_partial_publication_timeout(self):
        error = urllib.error.HTTPError('url', 404, 'Missing sources', {}, None)
        with patch.object(release, 'verify_public', side_effect=error):
            with self.assertRaises(TimeoutError):
                release.wait_for_publication(record(), timeout=0)

    def test_registry_timeout_retains_attempt(self):
        with patch.object(release, 'verify_public', side_effect=urllib.error.URLError('offline')):
            with self.assertRaises(TimeoutError):
                release.wait_for_publication(record(), timeout=0)


class DocumentationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / 'docs/templates').mkdir(parents=True)
        (self.root / 'docs/templates/IMPORT.md.template').write_text('{{STATUS}}\n{{INSTALLATION}}\n')
        (self.root / 'gradle.properties').write_text('GROUP=org.example\nPOM_ARTIFACT_ID=yolo\n')
        (self.root / 'docs/release.json').write_text('null\n')
        self.patch = patch.object(release, 'ROOT', self.root)
        self.patch.start()
        self.addCleanup(self.patch.stop)

    def test_unreleased_is_explicit_and_deterministic(self):
        release.documentation()
        first = (self.root / 'IMPORT.md').read_text()
        self.assertIn('No Maven Central release', first)
        self.assertNotIn('implementation(', first)
        release.documentation()
        self.assertEqual(first, (self.root / 'IMPORT.md').read_text())
        release.documentation(verify=True)

    def test_published_metadata_survives_development_coordinate_change(self):
        (self.root / 'docs/release.json').write_text(json.dumps(record()))
        release.documentation()
        before = (self.root / 'IMPORT.md').read_text()
        (self.root / 'gradle.properties').write_text('GROUP=org.changed\nPOM_ARTIFACT_ID=yolo\n')
        release.documentation()
        self.assertEqual(before, (self.root / 'IMPORT.md').read_text())
        with self.assertRaisesRegex(ValueError, 'Development coordinates'):
            release.documentation(verify=True)

    def test_unknown_placeholder(self):
        with self.assertRaisesRegex(ValueError, 'placeholder'):
            release.render('{{TYPO}}', None)

    def test_generated_drift(self):
        release.documentation()
        (self.root / 'IMPORT.md').write_text('hand edit')
        with self.assertRaisesRegex(ValueError, 'drift'):
            release.documentation(verify=True)

    def test_failed_confirmation_cannot_change_docs(self):
        release.documentation()
        before = {f: (self.root / f).read_bytes() for f in release.DOC_FILES}
        with patch.object(release, 'read_record', return_value=record()), patch.object(release, 'wait_for_publication', side_effect=TimeoutError):
            with self.assertRaises(TimeoutError):
                release.confirm('1.2.3')
        self.assertEqual(before, {f: (self.root / f).read_bytes() for f in release.DOC_FILES})

    def test_confirmed_version_renders_all_dependency_formats(self):
        with patch.object(release, 'read_record', return_value=record()), patch.object(release, 'wait_for_publication'):
            release.confirm('1.2.3')
        document = (self.root / 'IMPORT.md').read_text()
        for snippet in ('implementation("org.example:yolo:1.2.3")', "implementation 'org.example:yolo:1.2.3'", '[libraries]', '<version>1.2.3</version>'):
            self.assertIn(snippet, document)
        release.documentation(verify=True)
