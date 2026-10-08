"""JitPack entry point: only canonical module tags may publish one library."""
import hashlib
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
TAG = re.compile(r'(yolo)[/~]v((?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*))')


def selection(value):
    match = TAG.fullmatch(value)
    if not match:
        raise ValueError('VERSION must be a canonical yolo/vX.Y.Z tag (slash or ~)')
    return match.groups()


def run(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()


def command(module, version):
    selection(f'{module}/v{version}')
    return ['./gradlew', '--no-daemon', f':{module}:publishToMavenLocal',
            '-PjitpackBuild=true', f'-PreleaseModule={module}', f'-PreleaseVersion={version}']


def main():
    module, version = selection(os.environ.get('VERSION', ''))
    source = run('git', 'rev-parse', 'HEAD')
    if run('git', 'rev-parse', f'refs/tags/{module}/v{version}^{{commit}}') != source:
        raise ValueError('Canonical tag does not identify checkout')
    if os.environ.get('GIT_COMMIT', source) != source:
        raise ValueError('JitPack GIT_COMMIT differs from checkout')
    subprocess.run(command(module, version), cwd=ROOT, check=True)


if __name__ == '__main__':
    main()
