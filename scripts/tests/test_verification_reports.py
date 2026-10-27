"""Raw-report failure controls; assertions concern evidence, not parser structure."""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'verification'))
from discovery import VerificationError
from reports import collect, coverage, junit


class ReportContracts(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.directory = Path(self.temporary.name)
        self.path = self.directory / 'result.xml'

    def report(self, body='<testcase name="a" classname="Suite"/>', **changes):
        counters = {'tests': '1', 'failures': '0', 'errors': '0', 'skipped': '0'}
        counters.update(changes)
        attributes = ' '.join(f'{key}="{value}"' for key, value in counters.items())
        self.path.write_text(f'<testsuite {attributes}>{body}</testsuite>')
        return junit(self.path, ':module:test', set())

    def test_actual_pass(self):
        self.assertEqual(self.report()['tests'], 1)

    def test_failure_is_separate(self):
        self.assertEqual(self.report('<testcase name="a" classname="S"><failure/></testcase>', failures='1')['failures'], 1)

    def test_error_is_separate(self):
        self.assertEqual(self.report('<testcase name="a" classname="S"><error/></testcase>', errors='1')['errors'], 1)

    def test_skip_is_separate(self):
        self.assertEqual(self.report('<testcase name="a" classname="S"><skipped/></testcase>', skipped='1')['skipped'], 1)

    def test_forged_counters_rejected(self):
        with self.assertRaises(VerificationError):
            self.report(tests='2')

    def test_negative_counters_rejected(self):
        with self.assertRaises(VerificationError):
            self.report(tests='-1')

    def test_missing_counter_rejected(self):
        self.path.write_text('<testsuite tests="0"/>')
        with self.assertRaises(VerificationError):
            junit(self.path, ':a:test', set())

    def test_duplicate_cases_rejected(self):
        case = '<testcase name="a" classname="S"/>'
        with self.assertRaises(VerificationError):
            self.report(case * 2, tests='2')

    def test_distinct_parameterized_cases(self):
        result = self.report('<testcase name="a[1]" classname="S"/><testcase name="a[2]" classname="S"/>', tests='2')
        self.assertEqual(result['tests'], 2)

    def test_same_case_in_distinct_tasks(self):
        self.report()
        seen = set()
        junit(self.path, ':one:test', seen)
        junit(self.path, ':two:test', seen)
        self.assertEqual(len(seen), 2)

    def test_malformed_xml_rejected(self):
        self.path.write_text('<testsuite')
        with self.assertRaises(VerificationError):
            junit(self.path, ':a:test', set())

    def test_entity_expansion_rejected(self):
        self.path.write_text('<!DOCTYPE x [<!ENTITY x "test">]><testsuite/>')
        with self.assertRaises(VerificationError):
            junit(self.path, ':a:test', set())

    def test_conflicting_case_outcomes_rejected(self):
        with self.assertRaises(VerificationError):
            self.report('<testcase name="a" classname="S"><failure/><skipped/></testcase>', failures='1', skipped='1')

    def test_required_sources_without_results_rejected(self):
        with self.assertRaises(VerificationError):
            collect({'path': ':a:test', 'xml': str(self.directory), 'marker': str(self.directory/'marker')}, True)

    def test_no_source_is_not_passed_case(self):
        result = collect({'path': ':a:test', 'xml': str(self.directory), 'marker': str(self.directory/'marker')}, False)
        self.assertEqual((result['outcome'], result['tests']), ('no-test-sources', 0))

    def test_reports_without_completed_task_rejected(self):
        self.report()
        with self.assertRaises(VerificationError):
            collect({'path': ':a:test', 'xml': str(self.directory), 'marker': str(self.directory/'marker')}, True)

    def test_failed_task_retains_failed_assertion(self):
        self.report('<testcase name="a" classname="S"><failure/></testcase>', failures='1')
        result = collect({'path': ':a:test', 'xml': str(self.directory), 'marker': str(self.directory/'marker')}, True)
        self.assertEqual(result['failures'], 1)

    def test_wrong_task_marker_rejected(self):
        self.report()
        marker = self.directory/'marker'
        marker.write_text(json.dumps({'task': ':other:test', 'completed': True, 'runtime': {}}))
        with self.assertRaises(VerificationError):
            collect({'path': ':a:test', 'xml': str(self.directory), 'marker': str(marker)}, True)


class CoverageContracts(unittest.TestCase):
    setUp = ReportContracts.setUp
    def xml(self, missed=2, covered=1):
        def counter(kind):
            return f'<counter type="{kind}" missed="{missed}" covered="{covered}"/>'
        counters = ''.join(counter(kind) for kind in ['LINE', 'BRANCH', 'INSTRUCTION'])
        self.path.write_text(f'<!DOCTYPE report PUBLIC "-//JACOCO//DTD Report 1.1//EN" "report.dtd"><report><package name="p"><class name="p/C">{counters}</class></package>{counters}</report>')

    def test_untested_lines_remain_in_denominator(self):
        self.xml(7, 0)
        self.assertEqual(coverage(self.path)['line'], {'missed': 7, 'covered': 0})

    def test_canonical_class_population_count(self):
        self.xml()
        self.assertEqual(coverage(self.path)['classes'], 1)

    def test_mismatched_class_root_counters_rejected(self):
        self.xml()
        data = self.path.read_text().replace('</package><counter type="LINE" missed="2"', '</package><counter type="LINE" missed="3"')
        self.path.write_text(data)
        with self.assertRaises(VerificationError):
            coverage(self.path)

    def test_branch_free_code_has_zero_branch_denominator(self):
        self.xml()
        self.path.write_text(self.path.read_text().replace('<counter type="BRANCH" missed="2" covered="1"/>', ''))
        self.assertEqual(coverage(self.path)['branch'], {'missed': 0, 'covered': 0})

    def test_empty_report_rejected(self):
        self.path.write_text('<report/>')
        with self.assertRaises(VerificationError):
            coverage(self.path)

    def test_duplicate_classes_rejected(self):
        self.xml()
        data = self.path.read_text().replace('</package>', '<class name="p/C"/></package>')
        self.path.write_text(data)
        with self.assertRaises(VerificationError):
            coverage(self.path)

    def test_shared_source_lines_are_counted_once(self):
        self.path.write_text('<report><package name="p">\n<class name="p/A" sourcefilename="Shared.kt"><counter type="LINE" missed="0" covered="1"/></class>\n<class name="p/B" sourcefilename="Shared.kt"><counter type="LINE" missed="0" covered="1"/></class>\n<sourcefile name="Shared.kt"><counter type="LINE" missed="0" covered="1"/><counter type="INSTRUCTION" missed="0" covered="2"/></sourcefile>\n</package><counter type="LINE" missed="0" covered="1"/><counter type="INSTRUCTION" missed="0" covered="2"/></report>')
        result = coverage(self.path)
        self.assertEqual(result['classes'], 2)
        self.assertEqual(result['line'], {'missed': 0, 'covered': 1})

    def test_missing_referenced_source_file_rejected(self):
        self.xml()
        self.path.write_text(self.path.read_text().replace('class name="p/C"', 'class name="p/C" sourcefilename="Missing.kt"'))
        with self.assertRaises(VerificationError):
            coverage(self.path)

    def test_missing_branch_counter_rejected(self):
        self.xml()
        self.path.write_text('type="UNKNOWN"'.join(self.path.read_text().rsplit('type="BRANCH"', 1)))
        with self.assertRaises(VerificationError):
            coverage(self.path)


if __name__ == '__main__':
    unittest.main()
