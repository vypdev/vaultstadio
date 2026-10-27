"""Negative producer/source controls through the real verification CLI."""
import json
import os
import subprocess
import sys
import unittest

from verification_fixture import add_module, create


class NegativeProducerContracts(unittest.TestCase):
    def setUp(self):
        temporary, self.root = create()
        self.addCleanup(temporary.cleanup)

    def call(self, operation='test', **environment):
        return subprocess.run([sys.executable, 'scripts/verification/run.py', operation, '--root', 'backend',
                               *(['--reuse-tests'] if operation == 'coverage' else [])], cwd=self.root,
                              env={**os.environ, **environment}, check=False, capture_output=True, text=True)

    def test_zero_exit_stale_inventory_cannot_omit_new_module(self):
        self.assertEqual(self.call().returncode, 0)
        add_module(self.root, 'backend', 'moduleNew')
        result = self.call(FIXTURE_SKIP_INVENTORY='1')
        self.assertNotEqual(result.returncode, 0, 'stale inventory silently omitted the new module')

    def test_empty_inventory_rejected(self):
        self.assertNotEqual(self.call(FIXTURE_MODEL='empty').returncode, 0)

    def test_duplicate_inventory_rejected(self):
        self.assertNotEqual(self.call(FIXTURE_MODEL='duplicate').returncode, 0)

    def test_wrong_project_inventory_rejected(self):
        self.assertNotEqual(self.call(FIXTURE_MODEL='wrong-root').returncode, 0)

    def test_unsupported_suite_rejected(self):
        self.assertNotEqual(self.call(FIXTURE_MODEL='unsupported').returncode, 0)

    def test_no_applicable_tasks_rejected(self):
        self.assertNotEqual(self.call(FIXTURE_MODEL='additional').returncode, 0)

    def test_missing_production_population_rejected(self):
        self.assertNotEqual(self.call(FIXTURE_MODEL='missing-population').returncode, 0)

    def test_failed_inventory_preserves_original_diagnostic(self):
        result = self.call(FIXTURE_FAIL_INVENTORY='1')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('inventory-original-failure', result.stderr)

    def test_unexecutable_wrapper_is_actionable_failure(self):
        (self.root/'backend/gradlew').chmod(0o600)
        result = self.call()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Cannot execute', result.stderr)

    def test_source_mutation_during_suite_is_rejected(self):
        result = self.call(FIXTURE_MUTATE_SOURCE='backend')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Source changed during verification', result.stdout)

    def test_missing_jvm_execution_data_rejected(self):
        result = self.call(FIXTURE_NO_EXECUTION='backend')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Missing JVM execution data', result.stdout)

    def test_coverage_reuse_rejects_failed_suite(self):
        self.assertNotEqual(self.call(FIXTURE_FAIL='backend').returncode, 0)
        self.assertNotEqual(self.call('coverage').returncode, 0)

    def test_coverage_reuse_rejects_different_ci_context(self):
        self.assertEqual(self.call(GITHUB_RUN_ID='111').returncode, 0)
        result = self.call('coverage', GITHUB_RUN_ID='222')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('another CI run', result.stderr)

    def test_compilation_mutation_during_coverage_is_rejected(self):
        self.assertEqual(self.call().returncode, 0)
        result = self.call('coverage', FIXTURE_COVERAGE_MUTATE_CLASS='1')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('changed the tested production classes', result.stderr)

    def test_source_mutation_during_coverage_is_rejected(self):
        self.assertEqual(self.call().returncode, 0)
        result = self.call('coverage', FIXTURE_COVERAGE_MUTATE_SOURCE='1')
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('Source changed during coverage', result.stderr)

    def test_failed_new_inventory_invalidates_previous_green_receipt(self):
        self.assertEqual(self.call().returncode, 0)
        self.assertNotEqual(self.call(FIXTURE_FAIL_INVENTORY='1').returncode, 0)
        self.assertFalse((self.root/'backend/build/verification/backend.json').exists())
        self.assertNotEqual(self.call('coverage').returncode, 0)

    def test_failed_reuse_invalidates_previous_canonical_report(self):
        self.assertEqual(self.call().returncode, 0)
        self.assertEqual(self.call('coverage').returncode, 0)
        (self.root/'changed.kt').write_text('changed')
        self.assertNotEqual(self.call('coverage').returncode, 0)
        self.assertFalse((self.root/'backend/build/verification/coverage.json').exists())
        self.assertFalse((self.root/'backend/build/verification/coverage/coverage.xml').exists())

    def test_missing_execution_population_in_receipt_rejected(self):
        self.assertEqual(self.call().returncode, 0)
        path = self.root/'backend/build/verification/backend.json'
        record = json.loads(path.read_text())
        record['executionData'] = []
        path.write_text(json.dumps(record))
        self.assertNotEqual(self.call('coverage').returncode, 0)

    def test_expired_task_inventory_in_receipt_rejected(self):
        self.assertEqual(self.call().returncode, 0)
        path = self.root/'backend/build/verification/backend.json'
        record = json.loads(path.read_text())
        record['selectedTasks'].append(':retired:test')
        path.write_text(json.dumps(record))
        self.assertNotEqual(self.call('coverage').returncode, 0)

    def test_reuse_flag_cannot_skip_test_operation(self):
        result = subprocess.run([sys.executable, 'scripts/verification/run.py', 'test', '--reuse-tests'],
                                cwd=self.root, check=False, capture_output=True, text=True)
        self.assertEqual(result.returncode, 2)


if __name__ == '__main__':
    unittest.main()
