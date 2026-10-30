"""Boundary failures which must never turn incomplete evidence into green."""
import hashlib
import json
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1]/'verification'))
from discovery import VerificationError
from population import snapshot
from process import execute
from reports import collect, coverage, junit
from state import preserve_reports, reset_outputs


class EvidenceBoundaryContracts(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)/'backend'
        self.root.mkdir()
        self.file = self.root/'report.xml'

    def test_xml_limit_enforced_before_parsing(self):
        self.file.write_text('<testsuite/>')
        with patch('reports.MAX_XML_BYTES', 8), self.assertRaises(VerificationError):
            junit(self.file, ':a:test', set())

    def test_unexpected_xml_root_rejected(self):
        self.file.write_text('<testsuites/>')
        with self.assertRaises(VerificationError):
            junit(self.file, ':a:test', set())

    def test_unnamed_case_rejected(self):
        self.file.write_text('<testsuite tests="1" failures="0" errors="0" skipped="0"><testcase name="case"/></testsuite>')
        with self.assertRaises(VerificationError):
            junit(self.file, ':a:test', set())

    def test_no_executable_line_denominator_rejected(self):
        counters = ''.join(f'<counter type="{kind}" missed="0" covered="0"/>'for kind in ['LINE', 'BRANCH', 'INSTRUCTION'])
        self.file.write_text('<report><package><class name="p/C">'+counters+'</class></package>'+counters+'</report>')
        with self.assertRaises(VerificationError):
            coverage(self.file)

    def test_wrong_coverage_format_rejected(self):
        self.file.write_text('<testsuite/>')
        with self.assertRaises(VerificationError):
            coverage(self.file)

    def test_bad_completion_marker_is_failure(self):
        self.file.write_text('<testsuite tests="1" failures="0" errors="0" skipped="0"><testcase name="case" classname="S"/></testsuite>')
        marker = self.root/'marker'
        marker.write_text('{malformed')
        with self.assertRaises(VerificationError):
            collect({'path': ':a:test', 'xml': str(self.root), 'marker': str(marker)}, True)

    def test_collected_skip_retains_separate_outcome(self):
        self.file.write_text('<testsuite tests="1" failures="0" errors="0" skipped="1"><testcase name="case" classname="S"><skipped/></testcase></testsuite>')
        marker = self.root/'marker'
        marker.write_text(json.dumps({'task': ':a:test', 'completed': True, 'runtime': {}}))
        result = collect({'path': ':a:test', 'xml': str(self.root), 'marker': str(marker)}, True)
        self.assertEqual((result['outcome'], result['skipped']), ('skipped-cases', 1))

    def test_failed_generated_cases_remain_failed_without_source_directories(self):
        self.file.write_text('<testsuite tests="1" failures="1" errors="0" skipped="0"><testcase name="case" classname="Generated"><failure/></testcase></testsuite>')
        result = collect({'path': ':generated:test', 'xml': str(self.root), 'marker': str(self.root/'missing-marker')}, False)
        self.assertEqual((result['outcome'], result['failures']), ('failed', 1))

    def test_symlinked_xml_is_not_read(self):
        original = self.root/'original'
        original.write_text('must not follow')
        self.file.symlink_to(original)
        with self.assertRaises(VerificationError):
            junit(self.file, ':a:test', set())

    def test_no_compiled_production_classes_is_failure(self):
        with self.assertRaises(VerificationError):
            snapshot(self.root, {'production': [{'classes': [str(self.root/'build/classes')]}]})

    def test_mutated_report_cannot_be_preserved_as_parsed(self):
        self.file.write_text('original')
        digest = hashlib.sha256(self.file.read_bytes()).hexdigest()
        self.file.write_text('changed')
        with self.assertRaises(VerificationError):
            preserve_reports(self.root, [{'task': ':a:test', 'reports': [{'path': str(self.file), 'sha256': digest}]}])

    def test_symlink_in_owned_test_home_preserves_target(self):
        home = self.root/'build/verification/test-home/module'
        home.mkdir(parents=True)
        target = self.root/'authored'
        target.mkdir()
        (target/'original').write_text('preserve')
        (home/'link').symlink_to(target)
        task = {'xml': str(self.root/'module/build/test-results/test'), 'marker': str(self.root/'build/verification/task-events/a'), 'testHome': str(home)}
        with self.assertRaises(VerificationError):
            reset_outputs(self.root, [task])
        self.assertEqual((target/'original').read_text(), 'preserve')

    def test_console_write_failure_cannot_be_green(self):
        class BrokenOutput:
            @property
            def buffer(self):
                return self

            def write(self, data):
                raise OSError('deliberate closed console')

        with patch('process.sys.stdout', BrokenOutput()):
            code = execute(self.root, [sys.executable, '-c', 'print("original-diagnostic")'])
        self.assertNotEqual(code, 0)
        self.assertIn('original-diagnostic', next((self.root/'build/verification/commands').glob('*.stdout.log')).read_text())


if __name__ == '__main__':
    unittest.main()
