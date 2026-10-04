"""Validate DEMP test, documentation, packaging and local HTTP evidence."""
from pathlib import Path
import xml.etree.ElementTree as ET
from zipfile import ZipFile
import json


def require(condition, message):
    if not condition:
        raise RuntimeError(message)


def check_test_reports(reports, required_suites):
    require(isinstance(required_suites, list) and required_suites
            and all(isinstance(name, str) and name.strip() for name in required_suites), 'Required test suites are empty or invalid')
    require(len(set(required_suites)) == len(required_suites), 'Duplicate required test suite names')
    report_files = sorted(Path(reports).glob('TEST-*.xml'))
    require(report_files, 'No test reports')
    suites = [ET.parse(report).getroot() for report in report_files]
    count = sum(int(suite.attrib['tests']) for suite in suites)
    require(count > 0, 'Zero tests executed')
    for suite in suites:
        require(int(suite.attrib['tests']) > 0, 'Empty suite: ' + suite.attrib['name'])
        for field in ('failures', 'errors', 'skipped'):
            require(int(suite.attrib[field]) == 0, field + ': ' + suite.attrib['name'])
    missing = sorted(set(required_suites) - {suite.attrib['name'] for suite in suites})
    require(not missing, 'Missing required test suites: ' + ', '.join(missing))
    return count


def check_documentation(html, snippets):
    content = Path(html).read_bytes()
    require(content, 'Empty documentation')
    require(b'Unresolved directive' not in content and b'include::' not in content, 'Unresolved documentation')
    paths = [path for directory in (Path(html).parent, Path(snippets))
             for path in directory.rglob('*') if path.is_file()]
    for path in paths:
        text = path.read_bytes()
        require(not any(secret in text for secret in (b'test-only-jwt-secret', b'local-only-jwt-secret', b'test-access-key', b'test-secret-key')),
                'Internal test credentials in documentation: ' + str(path))
    return content


def check_packaged_jar(jar, expected):
    with ZipFile(jar) as archive:
        doc_path = 'BOOT-INF/classes/static/docs/index.html'
        require(doc_path in archive.namelist(), 'Packaged documentation missing')
        require(archive.read(doc_path) == expected, 'Packaged documentation mismatch')
        require(not any(name.startswith(('BOOT-INF/classes/architecturefixture/', 'BOOT-INF/classes/com/inhatc/demp/architecture/'))
                        for name in archive.namelist()), 'Test architecture classes packaged')


def main():
    root = Path(__file__).resolve().parents[1]
    evidence = root / 'build/verification'
    evidence.mkdir(parents=True, exist_ok=True)
    result_file = evidence / 'runtime.json'
    result_file.unlink(missing_ok=True)
    required = json.loads((root / 'docs/engineering/required-test-suites.json').read_text())
    tests = check_test_reports(root / 'build/test-results/test', required)
    expected = check_documentation(root / 'build/docs/asciidoc/index.html', root / 'build/generated-snippets')
    jars = [jar for jar in (root / 'build/libs').glob('*.jar') if not jar.name.endswith('-plain.jar')]
    require(len(jars) == 1, 'Expected exactly one application JAR')
    check_packaged_jar(jars[0], expected)
    from verify_runtime import run
    runtime = run(jars[0], expected, evidence)
    result = {'tests': tests, 'required_suites': len(required), 'failures': 0, 'errors': 0, 'skipped': 0,
              'packaged_docs_match': True, 'test_architecture_packaged': False, **runtime}
    result_file.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result), flush=True)


if __name__ == '__main__':
    main()
