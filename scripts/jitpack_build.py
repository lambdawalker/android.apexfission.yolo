"""JitPack entry point: only canonical module tags may publish one library."""
import hashlib
import os
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[1]
TAG = re.compile(r'(yolo)[/~]v((?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*))(-jitpack)?')


def selection(value):
    match = TAG.fullmatch(value)
    if not match:
        raise ValueError('VERSION must be a canonical yolo/vX.Y.Z tag (slash or ~)')
    return match.group(1), match.group(2)


def run(*args):
    return subprocess.check_output(args, cwd=ROOT, text=True).strip()


def command(module, version, scoped=False):
    selection(f'{module}/v{version}')
    return ['./gradlew', '--no-daemon', f':{module}:publishToMavenLocal',
            '-PjitpackBuild=true', f'-PreleaseModule={module}', f'-PreleaseVersion={version}', f'-PjitpackScopedTag={str(scoped).lower()}']


def main():
    requested_tag = os.environ.get('VERSION', '')
    module, version = selection(requested_tag)
    tag = requested_tag.replace('~', '/')
    source = run('git', 'rev-parse', 'HEAD')
    if run('git', 'rev-parse', f'refs/tags/{tag}^{{commit}}') != source:
        raise ValueError('Canonical tag does not identify checkout')
    if os.environ.get('GIT_COMMIT', source) != source:
        raise ValueError('JitPack GIT_COMMIT differs from checkout')
    subprocess.run(command(module, version, tag.endswith('-jitpack')), cwd=ROOT, check=True)


if __name__ == '__main__':
    main()
