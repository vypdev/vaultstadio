"""Source-bound Python coverage contracts with disposable reports and real CLI calls."""
import json
import os
import subprocess
import sys
import unittest
from pathlib import Path

from verification_fixture import create

sys.path.insert(0, str(Path(__file__).resolve().parents[1]/'verification'))
from discovery import VerificationError
from reports import coverage
from tooling_report import summary, verified_input


class ToolingCoverageContracts(unittest.TestCase):
    def setUp(self):
        temporary, self.root = create()
        self.addCleanup(temporary.cleanup)
        self.directory = self.root/'build/verification/python'
        self.directory.mkdir(parents=True)
        self.files = sorted(str(path.relative_to(self.root)) for path in (self.root/'scripts/verification').glob('*.py'))

    def cli(self, operation, **environment):
        return subprocess.run([sys.executable, 'scripts/verification/tooling_report.py', operation],
                              cwd=self.root, env={**os.environ, **environment}, check=False, capture_output=True, text=True)

    def reports(self):
        count = len(self.files)
        self.data = {'meta': {'branch_coverage': True, 'version': 'fixture-controls'},
                     'totals': {'num_statements': count, 'covered_lines': count, 'num_branches': 2, 'covered_branches': 1},
                     'files': {file: {} for file in self.files}}
        self.xml = f'<coverage lines-valid="{count}" lines-covered="{count}" branches-valid="2" branches-covered="1"><packages><package><classes>'
        self.xml += ''.join(f'<class filename="{file}"/>' for file in self.files)
        self.xml += '</classes></package></packages></coverage>'
        (self.directory/'coverage.xml').write_text(self.xml)
        (self.directory/'coverage.json').write_text(json.dumps(self.data))

    def test_actual_start_finish_cli_binds_same_source(self):
        self.assertEqual(self.cli('start').returncode, 0)
        self.reports()
        result = self.cli('finish')
        self.assertEqual(result.returncode, 0, result.stderr)
        receipt = json.loads((self.directory/'receipt.json').read_text())
        self.assertEqual(verified_input(self.directory, receipt['source']), self.directory/'coverage.xml')

    def test_actual_upload_cli_selects_three_consistent_populations(self):
        self.assertEqual(self.cli('start').returncode, 0)
        self.reports()
        self.assertEqual(self.cli('finish').returncode, 0)
        python = json.loads((self.directory/'receipt.json').read_text())
        directories = []
        counter = ''.join(f'<counter type="{kind}" missed="0" covered="1"/>' for kind in ['LINE', 'BRANCH', 'INSTRUCTION'])
        for name, suite in [('jvm-backend', 'backend'), ('jvm-frontend', 'desktop')]:
            directory = self.root/'build'/name
            directory.mkdir()
            xml = directory/'coverage.xml'
            xml.write_text('<report><package name="p"><class name="p/C">'+counter+'</class></package>'+counter+'</report>')
            record = {'success': True, 'source': python['source'], 'suite': suite, 'summary': coverage(xml), 'context': python['context']}
            (directory/'coverage.json').write_text(json.dumps(record))
            directories.append(directory)
        output = self.root/'build/output'
        result = subprocess.run([sys.executable, 'scripts/verification/upload_manifest.py',
                                 '--backend', str(directories[0]), '--frontend', str(directories[1]),
                                 '--python', str(self.directory), '--output', str(output)], cwd=self.root,
                                check=False, capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(len(output.read_text().strip().removeprefix('list=').split(',')), 3)

    def test_start_invalidates_previous_reports(self):
        self.reports()
        self.assertEqual(self.cli('start').returncode, 0)
        self.assertFalse((self.directory/'coverage.xml').exists())

    def test_source_changed_during_measurement_rejected(self):
        self.assertEqual(self.cli('start').returncode, 0)
        self.reports()
        (self.root/'changed.kt').write_text('changed')
        self.assertNotEqual(self.cli('finish').returncode, 0)
        self.assertFalse((self.directory/'receipt.json').exists())

    def test_other_run_context_rejected(self):
        self.assertEqual(self.cli('start', GITHUB_RUN_ID='111').returncode, 0)
        self.reports()
        self.assertNotEqual(self.cli('finish', GITHUB_RUN_ID='222').returncode, 0)

    def test_counters_reconcile_between_formats(self):
        self.reports()
        self.data['totals']['covered_lines'] -= 1
        (self.directory/'coverage.json').write_text(json.dumps(self.data))
        with self.assertRaises(VerificationError):
            summary(self.directory/'coverage.xml', self.directory/'coverage.json', self.root)

    def test_branchless_measurement_cannot_claim_branch_coverage(self):
        self.reports()
        self.data['meta']['branch_coverage'] = False
        (self.directory/'coverage.json').write_text(json.dumps(self.data))
        with self.assertRaises(VerificationError):
            summary(self.directory/'coverage.xml', self.directory/'coverage.json', self.root)

    def test_missing_production_file_rejected(self):
        self.reports()
        self.data['files'].pop(self.files[0])
        (self.directory/'coverage.json').write_text(json.dumps(self.data))
        with self.assertRaises(VerificationError):
            summary(self.directory/'coverage.xml', self.directory/'coverage.json', self.root)

    def test_duplicate_xml_population_rejected(self):
        self.reports()
        (self.directory/'coverage.xml').write_text(self.xml.replace('</classes>', f'<class filename="{self.files[0]}"/></classes>'))
        with self.assertRaises(VerificationError):
            summary(self.directory/'coverage.xml', self.directory/'coverage.json', self.root)

    def test_covered_count_cannot_exceed_denominator(self):
        self.reports()
        self.data['totals']['covered_branches'] = 3
        (self.directory/'coverage.json').write_text(json.dumps(self.data))
        (self.directory/'coverage.xml').write_text(self.xml.replace('branches-covered="1"', 'branches-covered="3"'))
        with self.assertRaises(VerificationError):
            summary(self.directory/'coverage.xml', self.directory/'coverage.json', self.root)

    def test_different_jvm_source_rejected(self):
        self.assertEqual(self.cli('start').returncode, 0)
        self.reports()
        self.assertEqual(self.cli('finish').returncode, 0)
        with self.assertRaises(VerificationError):
            verified_input(self.directory, {'head': 'different'})

    def test_mutated_python_xml_rejected(self):
        self.assertEqual(self.cli('start').returncode, 0)
        self.reports()
        self.assertEqual(self.cli('finish').returncode, 0)
        receipt = json.loads((self.directory/'receipt.json').read_text())
        with (self.directory/'coverage.xml').open('a') as output:
            output.write('\n')
        with self.assertRaises(VerificationError):
            verified_input(self.directory, receipt['source'])


if __name__ == '__main__':
    unittest.main()
