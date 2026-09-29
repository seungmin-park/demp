#!/usr/bin/env python3
"""Offline manifest/docs and reaction-write boundary checks for this repository."""
import argparse
import json
from pathlib import Path
import re
import tempfile

ROOT = Path(__file__).resolve().parents[1]
DOC = ROOT / 'docs/engineering/official-docs.json'


def check_versions(doc):
    entries = {entry['name']: entry for entry in json.loads(doc.read_text())['entries']}
    assert set(entries) == {'Java', 'Spring Boot', 'Gradle'}, 'Official documentation entries changed'
    tool_versions = (ROOT / '.tool-versions').read_text()
    build = (ROOT / 'build.gradle').read_text()
    wrapper = (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text()
    observed = {
        'Java': re.search(r'^java\s+[^\n]*?-(\d+)(?:\.|\s|$)', tool_versions, re.M).group(1),
        'Spring Boot': re.search(r"id 'org\.springframework\.boot' version '([^']+)'", build).group(1),
        'Gradle': re.search(r'gradle-([0-9.]+)-bin\.zip', wrapper).group(1),
    }
    for name, version in observed.items():
        assert entries[name]['version'] == version, f'{name}: manifest {version}, official-docs {entries[name]["version"]}'
        assert entries[name]['url'].startswith('https://'), f'{name}: missing official URL'
    return observed


def check_writers(root):
    failures = []
    for path in (root / 'src/main/java').rglob('*.java'):
        if path.relative_to(root).as_posix() == 'src/main/java/com/inhatc/demp/service/ContentReactionService.java':
            continue
        source = path.read_text()
        if 'ContentReactionRepository' not in source:
            continue
        aliases = re.findall(r'\bContentReactionRepository\s+(\w+)', source)
        for alias in aliases:
            if re.search(r'\b' + re.escape(alias) + r'\s*\.\s*(?:save\w*|delete\w*|flush)\s*\(', source):
                failures.append(str(path.relative_to(root)) + ': direct reaction write by ' + alias)
    return failures


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--probe-violation', action='store_true')
    parser.add_argument('--scan-root', type=Path, default=ROOT,
                        help='Override source root for a temporary negative check fixture')
    parser.add_argument('--docs-json', type=Path, default=DOC,
                        help='Override documentation manifest for a temporary negative check fixture')
    args = parser.parse_args()
    print('Versioned official docs:', check_versions(args.docs_json))
    failures = check_writers(args.scan_root)
    assert not failures, '\n'.join(failures)
    if args.probe_violation:
        with tempfile.TemporaryDirectory() as temp:
            item = Path(temp) / 'src/main/java/demo/RogueReactionWriter.java'
            item.parent.mkdir(parents=True)
            item.write_text('class RogueReactionWriter { ContentReactionRepository reactions; void write() { reactions.save(null); } }')
            detected = check_writers(Path(temp))
            assert len(detected) == 1, f'Guard failed to reject violation: {detected}'
            print('PASS: temporary direct repository writer rejected:', detected[0])
    print('PASS: reaction write ownership')


if __name__ == '__main__':
    main()
