#!/usr/bin/env python3
"""Keep confirmed installation facts independently of mutable destination pointers."""
import argparse
import json
from pathlib import Path
import re
import import_docs

ROOT = Path(__file__).resolve().parents[1]
HISTORY = Path('docs/releases/history')


def _record(record):
    if not isinstance(record, dict) or record.get('phase') != 'confirmed-public':
        raise ValueError('Only confirmed releases belong in the archive')
    module = record.get('module')
    if module not in import_docs.MODULES:
        raise ValueError('Unknown archive module')
    target = record.get('repository', 'maven-central')
    import_docs._validate(target, module, record)
    return target


def validate(entry):
    if entry.get('schema') != 1 or not entry.get('destinations'):
        raise ValueError('Invalid archive schema or empty destinations')
    if not re.fullmatch(r'[0-9a-f]{40}', entry.get('documentation_ref', '')):
        raise ValueError('Documentation revision must be an immutable commit SHA')
    if 'translation_tree' in entry and not re.fullmatch(r'[0-9a-f]{40}', entry['translation_tree']):
        raise ValueError('Translation tree must be an immutable SHA')
    for target, record in entry['destinations'].items():
        if _record(record) != target or any(record[k] != entry[k] for k in ('module', 'version', 'source')):
            raise ValueError('Archive identity conflict')
    return entry


def archive_record(root, record):
    target = _record(record)
    path = HISTORY / record['module'] / (record['version'] + '.json')
    absolute = root / path
    entry = validate(json.loads(absolute.read_text())) if absolute.exists() else dict(
        schema=1, module=record['module'], version=record['version'], source=record['source'],
        documentation_ref=record['source'], destinations={})
    if entry['source'] != record['source']:
        raise ValueError('Archive source conflict')
    existing = entry['destinations'].get(target)
    if existing is not None and existing != record:
        raise ValueError('Archive confirmation conflict')
    entry['destinations'][target] = record
    validate(entry)
    absolute.parent.mkdir(parents=True, exist_ok=True)
    absolute.write_text(json.dumps(entry, indent=2, sort_keys=True) + '\n', encoding='utf-8')
    return path


def latest_records(root):
    folder = root / 'docs/releases'
    # Deliberately exclude the history hierarchy and null retired destinations.
    paths = list(folder.glob('*.json')) + [p for p in folder.glob('*/*.json') if p.parent.name != 'history']
    for path in sorted(paths):
        record = json.loads(path.read_text())
        if record is not None:
            yield record


def seed(root):
    for record in latest_records(root):
        archive_record(root, record)


def load_catalog(root):
    entries = []
    for path in sorted((root / HISTORY).glob('*/*.json')):
        entry = validate(json.loads(path.read_text()))
        if path.relative_to(root) != HISTORY / entry['module'] / (entry['version'] + '.json'):
            raise ValueError('Archive filename identity conflict')
        entries.append(entry)
    return sorted(entries, key=lambda e: (e['module'], import_docs._version(e['version'])))


def verify(root):
    entries = {(e['module'], e['version']): e for e in load_catalog(root)}
    for record in latest_records(root):
        target = _record(record)
        entry = entries.get((record['module'], record['version']))
        if not entry or entry['destinations'].get(target) != record:
            raise ValueError('Latest confirmation missing from archive; run documentation_history.py seed')


def installation(entry, language='en'):
    validate(entry)
    es = language == 'es'
    module, version = entry['module'], entry['version']
    text = f"# {'Instalación' if es else 'Installation'}: {module} {version}\n\n"
    text += ('Elige **un** destino y **una** sintaxis de dependencia. Todos los destinos que aparecen aquí confirmaron esta versión del mismo código.\n' if es else
             'Choose **one** destination and **one** dependency syntax. Every destination below confirmed this version of the same source.\n')
    for target, record in sorted(entry['destinations'].items()):
        text += f'\n## {target}\n\n'
        examples = import_docs._examples(module, record)
        if es:
            # Only authored prose changes. Dependency examples remain byte-identical.
            replacements = {
                '#### Version catalog':'#### Catálogo de versiones',
                'In `settings.gradle.kts`:':'En `settings.gradle.kts`:',
                "In the app's `build.gradle.kts`:":'En el archivo `build.gradle.kts` de la aplicación:',
                'In `settings.gradle`:':'En `settings.gradle`:',
                "In the app's `build.gradle`:":'En el archivo `build.gradle` de la aplicación:',
                'Use the dependency repositories shown above. Add to `gradle/libs.versions.toml`:':'Usa los repositorios indicados arriba. Añade lo siguiente a `gradle/libs.versions.toml`:',
                "Then use this instead of the direct dependency in the app's `build.gradle.kts`:":'Después, usa este alias en lugar de la dependencia directa en `build.gradle.kts`:',
                'Add these repositories and dependency to `pom.xml`:':'Añade estos repositorios y esta dependencia a `pom.xml`:',
            }
            for original, translation in replacements.items():
                examples = examples.replace(original, translation)
        text += examples + '\n'
    return text


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('command', choices=['seed', 'verify', 'export'])
    args = parser.parse_args()
    if args.command == 'seed':
        seed(ROOT)
    verify(ROOT)
    if args.command == 'export':
        print(json.dumps([{**e, 'installation': {lang: installation(e, lang) for lang in ('en', 'es')}} for e in load_catalog(ROOT)]))


if __name__ == '__main__':
    main()
