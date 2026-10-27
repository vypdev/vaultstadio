"""Preservation and receipt integrity at filesystem/process boundaries."""
import hashlib
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'verification'))
from discovery import VerificationError, owned_output, source_identity
from state import exclusive, reset_outputs, verify_saved


class StateContracts(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)/'backend'
        self.root.mkdir()

    def test_generated_module_output_accepted(self):
        path = self.root/'core/build/test-results/test'
        self.assertEqual(owned_output(self.root, str(path)), path)

    def test_source_path_rejected(self):
        with self.assertRaises(VerificationError):
            owned_output(self.root, str(self.root/'core/src/main/file.kt'))

    def test_parent_escape_rejected(self):
        with self.assertRaises(VerificationError):
            owned_output(self.root, str(self.root/'core/build/../../src/file.kt'))

    def test_outside_root_rejected(self):
        with self.assertRaises(VerificationError):
            owned_output(self.root, str(self.root.parent/'frontend/build/test.xml'))

    def test_build_directory_itself_rejected(self):
        with self.assertRaises(VerificationError):
            owned_output(self.root, str(self.root/'build'))

    def test_symlinked_output_rejected(self):
        (self.root/'build').symlink_to(self.root.parent)
        with self.assertRaises(VerificationError):
            owned_output(self.root, str(self.root/'build/results/file'))

    def test_concurrent_verification_rejected(self):
        with exclusive(self.root), self.assertRaises(VerificationError), exclusive(self.root):
            self.fail('lock admitted concurrent producer')

    def test_lock_released_after_failure(self):
        with self.assertRaises(ValueError), exclusive(self.root):
            raise ValueError('deliberate')
        with exclusive(self.root):
            pass

    def task(self):
        base = self.root/'module/build'
        return {'xml': str(base/'test-results/test'), 'execution': str(base/'jacoco/test.exec'),
                'marker': str(self.root/'build/verification/task-events/module.json'),
                'testHome': str(self.root/'build/verification/test-home/module')}

    def test_stale_owned_xml_exec_marker_removed(self):
        task = self.task()
        files = [Path(task['xml'])/'old.xml', Path(task['execution']), Path(task['marker'])]
        for path in files:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text('stale')
        reset_outputs(self.root, [task])
        self.assertFalse(any(path.exists() for path in files))

    def test_unrelated_artifact_preserved(self):
        task = self.task()
        keep = Path(task['xml'])/'keep.txt'
        keep.parent.mkdir(parents=True)
        keep.write_text('authored evidence')
        reset_outputs(self.root, [task])
        self.assertEqual(keep.read_text(), 'authored evidence')

    def test_real_home_not_reset(self):
        task = self.task()
        home = self.root/'module/build/important'
        home.mkdir(parents=True)
        original = home/'original'
        original.write_text('must preserve')
        task['testHome'] = str(home)
        with self.assertRaises(VerificationError):
            reset_outputs(self.root, [task])
        self.assertEqual(original.read_text(), 'must preserve')

    def test_saved_report_mutation_rejected(self):
        path = self.root/'report.xml'
        path.write_text('original')
        receipt = {'savedReports': [{'path': str(path), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest()}], 'executionData': []}
        path.write_text('changed')
        with self.assertRaises(VerificationError):
            verify_saved(receipt)

    def test_missing_execution_data_rejected(self):
        with self.assertRaises(VerificationError):
            verify_saved({'savedReports': [], 'executionData': [{'path': str(self.root/'missing.exec'), 'sha256': '0'*64}]})

    def test_source_identity_detects_untracked_and_modified_input(self):
        subprocess.run(['git', 'init', '-q'], cwd=self.root, check=True)
        file = self.root/'source.kt'
        file.write_text('original')
        subprocess.run(['git', 'add', 'source.kt'], cwd=self.root, check=True)
        subprocess.run(['git', '-c', 'user.name=Fixture', '-c', 'user.email=fixture@audit.invalid', 'commit', '-qm', 'fixture'], cwd=self.root, check=True)
        before = source_identity(self.root)
        file.write_text('modified')
        self.assertNotEqual(source_identity(self.root), before)
        file.write_text('original')
        (self.root/'new.kt').write_text('new source')
        self.assertNotEqual(source_identity(self.root), before)


if __name__ == '__main__':
    unittest.main()
