"""Real local Git remotes exercise journal, races, and atomic finalization."""
import contextlib
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
from unittest.mock import patch

from test_release import release, record

SCRIPTS = Path(__file__).resolve().parents[1]


class GitTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.base = Path(self.temp.name)
        self.remote = self.base / 'remote.git'
        self.repo = self.base / 'repo'
        self.run_git(self.base, 'init', '--bare', '--initial-branch=main', str(self.remote))
        self.run_git(self.base, 'clone', str(self.remote), str(self.repo))
        self.git('config', 'user.name', 'Test')
        self.git('config', 'user.email', 'test@example.invalid')
        (self.repo / 'docs/templates').mkdir(parents=True)
        (self.repo / 'scripts').mkdir()
        for name in ('release.py', 'finalize-release.sh'):
            shutil.copy2(SCRIPTS / name, self.repo / 'scripts' / name)
        (self.repo / 'docs/templates/IMPORT.md.template').write_text('{{STATUS}}\n{{INSTALLATION}}\n')
        (self.repo / 'docs/release.json').write_text('null\n')
        (self.repo / 'gradle.properties').write_text('GROUP=org.example\nPOM_ARTIFACT_ID=yolo\n')
        self.root_patch = patch.object(release, 'ROOT', self.repo)
        self.root_patch.start()
        self.addCleanup(self.root_patch.stop)
        release.documentation()
        self.old_doc = (self.repo / 'IMPORT.md').read_text()
        self.git('add', '.')
        self.git('commit', '-m', 'source')
        self.source = self.git('rev-parse', 'HEAD')
        self.record = record()
        self.record['source'] = self.source
        self.git('tag', '-a', 'release-pending/1.2.3', '-m', json.dumps(self.record))
        self.git('push', 'origin', 'main', '--tags')
        self.git('checkout', '--detach')

    @staticmethod
    def run_git(cwd, *args):
        return subprocess.check_output(['git', *args], cwd=cwd, text=True, stderr=subprocess.PIPE).strip()

    def git(self, *args):
        return self.run_git(self.repo, *args)

    def remote_git(self, *args):
        return self.run_git(self.remote, *args)

    def confirmed(self):
        data = dict(self.record, phase='confirmed-public')
        (self.repo / 'docs/release.json').write_text(json.dumps(data, indent=2, sort_keys=True) + '\n')
        release.documentation()

    def finalize(self):
        self.confirmed()
        return subprocess.run(['bash', 'scripts/finalize-release.sh'], cwd=self.repo, text=True, capture_output=True,
                              env={**os.environ, 'SOURCE_SHA': self.source, 'RELEASE_VERSION': '1.2.3'})

    def test_pending_blocks_new_allocation(self):
        with self.assertRaisesRegex(ValueError, 'Unresolved'):
            release.prepare()

    def test_recovery_resolves_original_source_without_registry_allocation(self):
        with patch.object(release, 'published_versions', side_effect=AssertionError('Must not allocate')), contextlib.redirect_stdout(io.StringIO()):
            result = release.prepare('1.2.3')
        self.assertEqual(result, dict(version='1.2.3', source=self.source, completed='false'))

    def test_recovery_wrong_version_stops(self):
        with self.assertRaisesRegex(ValueError, 'matching'):
            release.prepare('1.2.4')

    def test_recovery_cannot_override_first_version(self):
        with self.assertRaises(ValueError):
            release.prepare('1.2.3', '0.1.0')

    def test_upload_marker_prevents_manual_reupload(self):
        release.guard('1.2.3')
        with self.assertRaisesRegex(ValueError, 'already started'):
            release.guard('1.2.3')
        self.assertEqual(self.remote_git('rev-parse', 'release-uploading/1.2.3^{commit}'), self.source)

    def test_missing_reservation_cannot_upload(self):
        with self.assertRaises(subprocess.CalledProcessError):
            release.guard('1.2.4')

    def test_guard_wrong_source_stops(self):
        (self.repo / 'another').write_text('change')
        self.git('add', '.')
        self.git('commit', '-m', 'different source')
        with self.assertRaisesRegex(ValueError, 'differs'):
            release.guard('1.2.3')

    def test_finalization_pins_artifact_source_and_removes_both_markers(self):
        release.guard('1.2.3')
        result = self.finalize()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.remote_git('rev-parse', 'v1.2.3^{commit}'), self.source)
        self.assertNotEqual(self.remote_git('rev-parse', 'main'), self.source)
        self.assertEqual(self.remote_git('tag', '--list', 'release-*'), '')

    def test_repeated_completed_recovery_is_verification_only(self):
        result = self.finalize()
        self.assertEqual(result.returncode, 0, result.stderr)
        with contextlib.redirect_stdout(io.StringIO()):
            result = release.prepare('1.2.3')
        self.assertEqual(result['completed'], 'true')
        self.assertEqual(result['source'], self.source)

    def test_atomic_rejection_keeps_pending_and_old_docs(self):
        hook = self.remote / 'hooks/pre-receive'
        hook.write_text('#!/bin/sh\nexit 1\n')
        hook.chmod(0o755)
        result = self.finalize()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.remote_git('rev-parse', 'main'), self.source)
        self.assertEqual(self.remote_git('tag', '--list', 'v*'), '')
        self.assertEqual(self.remote_git('tag', '--list', 'release-pending/*'), 'release-pending/1.2.3')
        self.assertEqual(self.remote_git('show', 'main:IMPORT.md'), self.old_doc.strip())

    def advance(self, path):
        self.git('switch', 'main')
        (self.repo / path).write_text('concurrent edit\n')
        self.git('add', path)
        self.git('commit', '-m', 'concurrent')
        self.git('push', 'origin', 'main')
        self.git('checkout', '--detach', self.source)

    def test_concurrent_unrelated_main_work_is_preserved(self):
        self.advance('README.md')
        result = self.finalize()
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.remote_git('show', 'main:README.md'), 'concurrent edit')

    def test_concurrent_coordinate_change_stops(self):
        self.advance('gradle.properties')
        result = self.finalize()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(self.remote_git('tag', '--list', 'v*'), '')
        self.assertEqual(self.remote_git('show', 'main:IMPORT.md'), self.old_doc.strip())

    def test_existing_wrong_source_tag_stops_recovery(self):
        self.advance('README.md')
        self.git('tag', '-a', 'v1.2.3', 'origin/main', '-m', 'wrong source')
        self.git('push', 'origin', 'refs/tags/v1.2.3')
        with self.assertRaisesRegex(ValueError, 'different source'):
            release.prepare('1.2.3')

    def test_existing_correct_tag_allows_finalization(self):
        self.git('tag', '-a', 'v1.2.3', self.source, '-m', 'same source')
        self.git('push', 'origin', 'refs/tags/v1.2.3')
        result = self.finalize()
        self.assertEqual(result.returncode, 0, result.stderr)

    def test_reservation_records_source_and_hashes(self):
        self.assertEqual(release.read_record('release-pending/1.2.3'), self.record)
