from pathlib import Path
import tempfile
import unittest
from zipfile import ZipFile
import verify_ci


class VerificationTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)

    def report(self, name, tests=1, failures=0, errors=0, skipped=0):
        (self.root / ('TEST-' + name + '.xml')).write_text(
            f'<testsuite name="{name}" tests="{tests}" failures="{failures}" errors="{errors}" skipped="{skipped}"/>')

    def test_accepts_required_and_additional_suites(self):
        self.report('required', tests=2)
        self.report('new')
        self.assertEqual(verify_ci.check_test_reports(self.root, ['required']), 3)

    def test_rejects_missing_suite_even_when_other_tests_pass(self):
        self.report('present')
        with self.assertRaisesRegex(RuntimeError, 'Missing required test suites: missing'):
            verify_ci.check_test_reports(self.root, ['present', 'missing'])

    def test_rejects_no_reports(self):
        with self.assertRaisesRegex(RuntimeError, 'No test reports'):
            verify_ci.check_test_reports(self.root, ['required'])

    def test_rejects_zero_tests(self):
        self.report('required', tests=0)
        with self.assertRaisesRegex(RuntimeError, 'Zero tests'):
            verify_ci.check_test_reports(self.root, ['required'])

    def test_rejects_empty_suite_even_when_other_tests_run(self):
        self.report('required')
        self.report('empty', tests=0)
        with self.assertRaisesRegex(RuntimeError, 'Empty suite: empty'):
            verify_ci.check_test_reports(self.root, ['required'])

    def test_rejects_failures_errors_and_skips(self):
        for field in ('failures', 'errors', 'skipped'):
            with self.subTest(field=field):
                self.report('required', **{field: 1})
                with self.assertRaisesRegex(RuntimeError, field + ': required'):
                    verify_ci.check_test_reports(self.root, ['required'])

    def test_rejects_empty_required_list(self):
        self.report('required')
        with self.assertRaisesRegex(RuntimeError, 'Required test suites'):
            verify_ci.check_test_reports(self.root, [])

    def test_rejects_duplicate_required_names(self):
        self.report('required')
        with self.assertRaisesRegex(RuntimeError, 'Duplicate required'):
            verify_ci.check_test_reports(self.root, ['required', 'required'])

    def test_accepts_complete_documentation(self):
        html = self.root / 'index.html'
        html.write_bytes(b'<html>API documentation</html>')
        self.assertEqual(verify_ci.check_documentation(html, self.root / 'snippets'), b'<html>API documentation</html>')

    def test_rejects_unresolved_documentation(self):
        html = self.root / 'index.html'
        html.write_bytes(b'<html>Unresolved directive in index.adoc</html>')
        with self.assertRaisesRegex(RuntimeError, 'Unresolved documentation'):
            verify_ci.check_documentation(html, self.root / 'snippets')

    def test_rejects_internal_test_credentials_in_snippets(self):
        html = self.root / 'index.html'
        html.write_bytes(b'<html>API</html>')
        snippets = self.root / 'snippets'
        snippets.mkdir()
        (snippets / 'request.adoc').write_text('test-access-key')
        with self.assertRaisesRegex(RuntimeError, 'Internal test credentials'):
            verify_ci.check_documentation(html, snippets)

    def test_accepts_matching_packaged_documentation(self):
        jar = self.root / 'application.jar'
        with ZipFile(jar, 'w') as archive:
            archive.writestr('BOOT-INF/classes/static/docs/index.html', b'expected')
        verify_ci.check_packaged_jar(jar, b'expected')

    def test_rejects_different_packaged_documentation(self):
        jar = self.root / 'application.jar'
        with ZipFile(jar, 'w') as archive:
            archive.writestr('BOOT-INF/classes/static/docs/index.html', b'old')
        with self.assertRaisesRegex(RuntimeError, 'Packaged documentation mismatch'):
            verify_ci.check_packaged_jar(jar, b'expected')

    def test_rejects_missing_packaged_documentation(self):
        jar = self.root / 'application.jar'
        with ZipFile(jar, 'w') as archive:
            archive.writestr('BOOT-INF/classes/Application.class', b'class')
        with self.assertRaisesRegex(RuntimeError, 'Packaged documentation missing'):
            verify_ci.check_packaged_jar(jar, b'expected')

    def test_rejects_packaged_architecture_fixtures(self):
        jar = self.root / 'application.jar'
        with ZipFile(jar, 'w') as archive:
            archive.writestr('BOOT-INF/classes/static/docs/index.html', b'expected')
            archive.writestr('BOOT-INF/classes/architecturefixture/BoundarySamples.class', b'fixture')
        with self.assertRaisesRegex(RuntimeError, 'Test architecture classes packaged'):
            verify_ci.check_packaged_jar(jar, b'expected')


if __name__ == '__main__':
    unittest.main()
