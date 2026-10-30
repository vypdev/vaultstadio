"""Execute actual Make/shell/verification entrypoints with a disposable producer."""
import json
import os
import subprocess
import sys
import unittest

from verification_fixture import add_module, create


class EntrypointContracts(unittest.TestCase):
    def setUp(self):
        temporary, self.root = create()
        self.addCleanup(temporary.cleanup)

    def run_cli(self, *arguments, **environment):
        result = subprocess.run([sys.executable, 'scripts/verification/run.py', *arguments],
                                cwd=self.root, env={**os.environ, **environment}, check=False, capture_output=True, text=True)
        return result

    def trace(self):
        return (self.root/'build/trace.txt').read_text()

    def receipt(self, area, suite):
        return json.loads((self.root/area/f'build/verification/{suite}.json').read_text())

    def test_complete_roots_and_suites(self):
        result = self.run_cli('test')
        self.assertEqual(result.returncode, 0, result.stderr+result.stdout)
        for area, suite in [('backend', 'backend'), ('frontend', 'desktop'), ('frontend', 'browser')]:
            self.assertEqual(self.receipt(area, suite)['totals']['tests'], 1)

    def test_backend_failure_retains_status_and_runs_frontend(self):
        result = self.run_cli('test', FIXTURE_FAIL='backend')
        self.assertEqual(result.returncode, 42, result.stderr+result.stdout)
        self.assertIn('original-backend-failure', result.stderr)
        self.assertTrue(self.receipt('frontend', 'browser')['success'])
        self.assertNotIn('All required verification stages passed.', result.stdout)

    def test_desktop_failure_does_not_skip_browser(self):
        result = self.run_cli('test', '--root', 'frontend', FIXTURE_FAIL='desktop')
        self.assertEqual(result.returncode, 42)
        self.assertTrue(self.receipt('frontend', 'browser')['success'])

    def test_browser_failure_not_suppressed(self):
        result = self.run_cli('test', '--root', 'frontend', FIXTURE_FAIL='browser')
        self.assertEqual(result.returncode, 42)
        self.assertIn('original-browser-failure', result.stderr)

    def test_new_backend_module_automatically_selected(self):
        add_module(self.root, 'backend', 'moduleNew')
        result = self.run_cli('test', '--root', 'backend')
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(self.receipt('backend', 'backend')['totals']['tests'], 2)
        self.assertIn(':moduleNew:test --rerun', self.trace())

    def test_new_frontend_module_in_both_destinations(self):
        add_module(self.root, 'frontend', 'moduleNew')
        result = self.run_cli('test', '--root', 'frontend')
        self.assertEqual(result.returncode, 0, result.stderr)
        for suite in ['desktop', 'browser']:
            self.assertEqual(self.receipt('frontend', suite)['totals']['tests'], 2)

    def test_zero_exit_missing_results_is_failure(self):
        result = self.run_cli('test', '--root', 'backend', FIXTURE_MISSING='backend')
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(self.receipt('backend', 'backend')['success'])

    def test_unknown_flag_rejected(self):
        result = self.run_cli('test', '--skip-browser')
        self.assertEqual(result.returncode, 2)
        self.assertFalse((self.root/'build/trace.txt').exists())

    def test_inapplicable_suite_rejected(self):
        self.assertEqual(self.run_cli('test', '--root', 'backend', '--suite', 'browser').returncode, 2)

    def test_reuse_without_execution_fails(self):
        result = self.run_cli('coverage', '--root', 'backend', '--reuse-tests')
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn('verificationCoverage', self.trace())

    def test_matching_coverage_reuses_tests_once(self):
        first = self.run_cli('test', '--root', 'backend')
        self.assertEqual(first.returncode, 0, first.stderr)
        result = self.run_cli('coverage', '--root', 'backend', '--reuse-tests')
        self.assertEqual(result.returncode, 0, result.stderr+result.stdout)
        self.assertEqual(self.trace().count(':module1:test --rerun'), 1)

    def test_coverage_reuse_rejects_source_change(self):
        self.assertEqual(self.run_cli('test', '--root', 'backend').returncode, 0)
        (self.root/'new-source.kt').write_text('changed')
        result = self.run_cli('coverage', '--root', 'backend', '--reuse-tests')
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn('verificationCoverage', self.trace())

    def test_coverage_reuse_rejects_mutated_execution(self):
        self.assertEqual(self.run_cli('test', '--root', 'backend').returncode, 0)
        (self.root/'backend/module1/build/jacoco/test.exec').write_bytes(b'changed')
        self.assertNotEqual(self.run_cli('coverage', '--root', 'backend', '--reuse-tests').returncode, 0)

    def test_coverage_reuse_rejects_mutated_production_class(self):
        self.assertEqual(self.run_cli('test', '--root', 'backend').returncode, 0)
        (self.root/'backend/module1/build/classes/C.class').write_bytes(b'changed')
        self.assertNotEqual(self.run_cli('coverage', '--root', 'backend', '--reuse-tests').returncode, 0)

    def test_frontend_coverage_requires_successful_browser_results(self):
        self.assertEqual(self.run_cli('test', '--root', 'frontend', '--suite', 'desktop').returncode, 0)
        result = self.run_cli('coverage', '--root', 'frontend', '--reuse-tests')
        self.assertNotEqual(result.returncode, 0)
        self.assertNotIn('verificationCoverage', self.trace())

    def test_failed_coverage_keeps_failure(self):
        result = self.run_cli('coverage', FIXTURE_COVERAGE_EXIT='42')
        self.assertEqual(result.returncode, 42, result.stderr+result.stdout)
        self.assertEqual(self.trace().count('verificationCoverage --rerun'), 2)

    def test_root_make_propagates_browser_failure(self):
        result = subprocess.run(['make', 'test'], cwd=self.root, env={**os.environ, 'FIXTURE_FAIL': 'browser'}, check=False, capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('original-browser-failure', result.stderr)

    def test_frontend_make_propagates_desktop_failure(self):
        result = subprocess.run(['make', '-C', 'frontend', 'test'], cwd=self.root, env={**os.environ, 'FIXTURE_FAIL': 'desktop'}, check=False, capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertTrue(self.receipt('frontend', 'browser')['success'])

    def test_shell_entrypoint_from_another_directory(self):
        result = subprocess.run(['bash', str(self.root/'scripts/run-tests.sh')], cwd=self.root.parent, env={**os.environ, 'FIXTURE_FAIL': 'browser'}, check=False, capture_output=True, text=True)
        self.assertEqual(result.returncode, 42)
        self.assertIn('original-browser-failure', result.stderr)

    def test_shell_unknown_flag_rejected(self):
        result = subprocess.run(['bash', 'scripts/run-tests.sh', '--unknown'], cwd=self.root, check=False, capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)
        self.assertFalse((self.root/'build/trace.txt').exists())

    def test_lint_checks_both_roots_and_keeps_diagnostic(self):
        result = self.run_cli('lint', FIXTURE_LINT_EXIT='42')
        self.assertEqual(result.returncode, 42)
        self.assertIn('lint-original-diagnostic', result.stderr)
        self.assertIn('detektMain', self.trace())
        self.assertIn('detektDesktopMain', self.trace())


if __name__ == '__main__':
    unittest.main()
