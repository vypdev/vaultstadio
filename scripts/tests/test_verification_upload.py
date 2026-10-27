"""Canonical upload selection rejects mixed, missing and altered evidence."""
import json
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]/'verification'))
from discovery import VerificationError
from reports import coverage
from state import execution_context
from upload_manifest import inputs


class UploadContracts(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        self.directories = [self.root/'backend', self.root/'frontend']
        self.records = []
        counter = ''.join(f'<counter type="{kind}" missed="2" covered="3"/>' for kind in ['LINE', 'BRANCH', 'INSTRUCTION'])
        for directory, suite in zip(self.directories, ['backend', 'desktop']):
            directory.mkdir()
            path = directory/'coverage.xml'
            path.write_text('<report><package name="p"><class name="p/C">'+counter+'</class></package>'+counter+'</report>')
            record = {'success': True, 'source': {'head': 'same-source'}, 'summary': coverage(path), 'suite': suite, 'context': execution_context()}
            (directory/'coverage.json').write_text(json.dumps(record))
            self.records.append(record)

    def change(self, number, **changes):
        self.records[number].update(changes)
        (self.directories[number]/'coverage.json').write_text(json.dumps(self.records[number]))

    def test_exact_two_canonical_inputs(self):
        self.assertEqual(inputs(self.directories), [str(directory/'coverage.xml') for directory in self.directories])

    def test_missing_root_report_rejected(self):
        (self.directories[1]/'coverage.xml').unlink()
        with self.assertRaises(VerificationError):
            inputs(self.directories)

    def test_duplicate_canonical_input_rejected(self):
        extra = self.directories[0]/'stale/coverage.xml'
        extra.parent.mkdir()
        extra.write_text('stale')
        with self.assertRaises(VerificationError):
            inputs(self.directories)

    def test_mutated_report_rejected(self):
        path = self.directories[0]/'coverage.xml'
        path.write_text(path.read_text()+'\n')
        with self.assertRaises(VerificationError):
            inputs(self.directories)

    def test_different_source_populations_rejected(self):
        self.change(1, source={'head': 'other-source'})
        with self.assertRaises(VerificationError):
            inputs(self.directories)

    def test_other_workflow_run_rejected(self):
        self.change(0, context={'GITHUB_RUN_ID': 'different-run'})
        with self.assertRaises(VerificationError):
            inputs(self.directories)

    def test_successful_earlier_attempt_of_same_run_remains_reusable(self):
        self.change(0, context={**execution_context(), 'GITHUB_RUN_ATTEMPT': 'earlier'})
        self.assertEqual(len(inputs(self.directories)), 2)

    def test_failed_receipt_rejected(self):
        self.change(0, success=False)
        with self.assertRaises(VerificationError):
            inputs(self.directories)

    def test_reversed_populations_rejected(self):
        with self.assertRaises(VerificationError):
            inputs(list(reversed(self.directories)))

    def test_real_upload_cli_writes_only_validated_inputs(self):
        output = self.root/'output'
        helper = Path(__file__).resolve().parents[1]/'verification/upload_manifest.py'
        result = subprocess.run([sys.executable, str(helper), '--backend', str(self.directories[0]),
                                 '--frontend', str(self.directories[1]), '--output', str(output)],
                                check=False, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(output.read_text(), 'list='+','.join(str(p/'coverage.xml')for p in self.directories)+'\n')

    def test_real_upload_cli_rejects_output_channel_injection(self):
        renamed = self.root/'front,end'
        self.directories[1].rename(renamed)
        output = self.root/'output'
        helper = Path(__file__).resolve().parents[1]/'verification/upload_manifest.py'
        result = subprocess.run([sys.executable, str(helper), '--backend', str(self.directories[0]),
                                 '--frontend', str(renamed), '--output', str(output)],
                                check=False, capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertFalse(output.exists())

    def test_report_mutated_during_manifest_validation_is_rejected(self):
        def mutate_after_parse(path):
            result = coverage(path)
            path.write_text(path.read_text()+'\n')
            return result
        with patch('upload_manifest.coverage', side_effect=mutate_after_parse), self.assertRaises(VerificationError):
            inputs(self.directories)

    def test_symlinked_input_rejected(self):
        path = self.directories[0]/'coverage.xml'
        original = self.root/'original.xml'
        path.rename(original)
        path.symlink_to(original)
        with self.assertRaises(VerificationError):
            inputs(self.directories)


if __name__ == '__main__':
    unittest.main()
