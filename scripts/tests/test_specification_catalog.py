"""Real filesystem/CLI rejection controls and immutable migration preservation."""
import copy
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

SCRIPTS = Path(__file__).resolve().parents[1]
REPO = SCRIPTS.parent
sys.path.insert(0, str(SCRIPTS))
from specification_catalog import read_catalog, validate_catalog
from specification_catalog_render import render_catalog
from sdd_path_preservation import load_migration, original_owner_bytes, remap_catalog


class CatalogControls(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix='sdd-catalog-control-')
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for directory in ['specs', 'scripts', 'code', 'tests', 'docs', '.github/workflows']:
            (self.root / directory).mkdir(parents=True, exist_ok=True)
        for name in ['README.md', '_template.md']:
            shutil.copyfile(REPO / 'specs' / name, self.root / 'specs' / name)
        for name in ['specification_catalog.py', 'specification_catalog_render.py', 'validate-specification-catalog.py']:
            shutil.copyfile(SCRIPTS / name, self.root / 'scripts' / name)
        primary = '# Fixture contract\n\n' + '\n'.join(
            '- ' + value for value in ['Status: Proposed', 'Date: 2026-10-30',
            'Catalog capability ID: `fixture`', 'Last verified: not applicable',
            'Owners: Maintainers', 'Scope: Filesystem fixture', 'Related issues/PRs: none',
            'Required review gates: documentation', 'Open decisions blocking readiness: none'])
        primary += '\n\n' + '\n\n'.join(f'## {n}. Section\n\nFixture contract.' for n in range(1, 21)) + '\n'
        (self.root / 'specs/fixture.md').write_text(primary)
        for path in ['code/entry.py', 'code/policy.py', 'tests/control.py', 'docs/reference.md', '.github/workflows/ci.yml']:
            (self.root / path).write_text('Synthetic fixture; never executed.\n')
        self.catalog = {'version': 1, 'capabilities': [{
            'id': 'fixture', 'title': 'Fixture contract', 'status': 'proposed',
            'scope': 'Filesystem controls', 'owner': 'Maintainers', 'lastVerified': '2026-10-30',
            'specs': ['specs/fixture.md'], 'workflows': ['.github/workflows/ci.yml'],
            'entrypoints': ['code/entry.py'], 'code': ['code/policy.py'],
            'tests': ['tests/control.py'], 'documentation': ['docs/reference.md'],
            'legacyVerification': {'status': 'Proposed', 'lastVerified': None},
        }]}
        self.persist()

    @property
    def item(self):
        return self.catalog['capabilities'][0]

    def persist(self):
        (self.root / 'specs/catalog.json').write_text(json.dumps(self.catalog))
        (self.root / 'specs/CATALOG.md').write_text(render_catalog(self.catalog))

    def errors(self):
        return validate_catalog(self.root, self.catalog)

    def rejected(self, substring):
        self.assertTrue(any(substring in error for error in self.errors()), self.errors())

    def cli(self, *arguments):
        return subprocess.run([sys.executable, str(self.root / 'scripts/validate-specification-catalog.py'), *arguments],
                              cwd=self.root, text=True, capture_output=True)

    def snapshot(self):
        return {p.relative_to(self.root).as_posix():p.read_bytes() for p in self.root.rglob('*') if p.is_file()}

    def test_clean_supported_profile(self):
        self.assertEqual([], self.errors())

    def test_empty_primary_sections_do_not_establish_a_contract(self):
        path = self.root / 'specs/fixture.md'
        text = path.read_text()
        metadata = text[:text.index('## 1.')]
        path.write_text(metadata + '\n'.join(f'## {n}. Empty\n' for n in range(1, 21)))
        self.rejected('requires visible content')

    def test_primary_section_accepts_explicit_not_applicable_explanation(self):
        path = self.root / 'specs/fixture.md'
        text = path.read_text()
        text = text.replace('## 1. Section\n\nFixture contract.', '## 1. Section\n\nNot applicable: this fixture has no user-facing execution journey.', 1)
        path.write_text(text)
        self.assertEqual([], self.errors())

    def test_primary_subheadings_do_not_replace_a_section_body(self):
        path = self.root / 'specs/fixture.md'
        text = path.read_text()
        text = text.replace('## 1. Section\n\nFixture contract.', '## 1. Section\n\n### Placeholder without contract text\n', 1)
        path.write_text(text)
        self.rejected('requires visible content')

    def test_populated_later_historical_section_is_preserved(self):
        path = self.root / 'specs/fixture.md'
        path.write_text(path.read_text() + '\n## 21. Historical revision\n\nRetained prior acceptance and evidence.\n')
        self.assertEqual([], self.errors())
        self.assertIn('Retained prior acceptance and evidence.', path.read_text())

    def test_malformed_json_cli_fails_without_mutation(self):
        (self.root / 'specs/catalog.json').write_text('{invalid')
        before = self.snapshot()
        self.assertEqual(1, self.cli().returncode)
        self.assertEqual(before, self.snapshot())

    def test_malformed_root_rejected(self):
        self.assertTrue(validate_catalog(self.root, []))

    def test_duplicate_capability_id_rejected(self):
        self.catalog['capabilities'].append(copy.deepcopy(self.item))
        self.rejected('duplicate capability ID')

    def test_duplicate_title_rejected(self):
        other = copy.deepcopy(self.item)
        other['id'] = 'other'
        self.catalog['capabilities'].append(other)
        self.rejected('duplicate capability title')

    def test_duplicate_specification_owner_rejected(self):
        other = copy.deepcopy(self.item)
        other.update(id='other', title='Other')
        self.catalog['capabilities'].append(other)
        self.rejected('duplicate specification owner')

    def test_unsupported_inventory_state_rejected(self):
        self.item['status'] = 'Ready for implementation'
        self.rejected('unsupported inventory status')

    def test_impossible_date_rejected(self):
        self.item['lastVerified'] = '2026-02-29'
        self.rejected('actual YYYY-MM-DD')

    def test_missing_evidence_file_rejected(self):
        (self.root / 'code/policy.py').unlink()
        self.rejected('nonregular path')

    def test_directory_is_not_typed_file(self):
        self.item['code'] = ['code']
        self.rejected('nonregular path')

    def test_traversal_rejected_before_read(self):
        self.item['documentation'] = ['../outside.md']
        self.rejected('invalid boundary')

    def test_file_symlink_rejected(self):
        (self.root / 'code/link.py').symlink_to('policy.py')
        self.item['code'] = ['code/link.py']
        self.rejected('nonregular path')

    def test_parent_directory_symlink_rejected(self):
        (self.root / 'linked').symlink_to(self.root / 'code', target_is_directory=True)
        self.item['code'] = ['linked/policy.py']
        self.rejected('nonregular path')

    def test_workflow_boundary_rejected(self):
        self.item['workflows'] = ['code/policy.py']
        self.rejected('invalid boundary')

    def test_specification_boundary_rejected(self):
        self.item['specs'] = ['docs/reference.md']
        self.rejected('invalid boundary')

    def test_unregistered_specification_rejected(self):
        (self.root / 'specs/extra.md').write_text('# Unregistered\n')
        self.rejected('unregistered specification')

    def test_empty_standard_section_rejected(self):
        path = self.root / 'specs/README.md'
        text = path.read_text()
        start, end = text.index('## 1.'), text.index('## 2.')
        path.write_text(text[:start]+'## 1. Empty\n\n'+text[end:])
        self.rejected('eight populated')

    def test_normative_guidance_in_comment_is_insufficient(self):
        path = self.root / 'specs/README.md'
        path.write_text(path.read_text().replace('A `MUST` requires an acceptance\nscenario.', '<!-- A MUST requires an acceptance scenario. -->'))
        self.rejected('normative acceptance guidance')

    def test_missing_template_section_rejected(self):
        path = self.root / 'specs/_template.md'
        path.write_text(path.read_text().replace('## 20.', '### 20.'))
        self.rejected('twenty populated')

    def test_stale_generated_view_fails_read_only(self):
        (self.root / 'specs/CATALOG.md').write_text('stale')
        before = self.snapshot()
        result = self.cli()
        self.assertEqual(1, result.returncode)
        self.assertIn('stale', result.stdout)
        self.assertEqual(before, self.snapshot())

    def test_generation_changes_only_view_and_is_idempotent(self):
        (self.root / 'specs/CATALOG.md').write_text('stale')
        before = self.snapshot()
        self.assertEqual(0, self.cli('--write').returncode)
        after = self.snapshot()
        self.assertEqual({'specs/CATALOG.md'}, {p for p in before if before[p] != after[p]})
        self.assertEqual(0, self.cli('--write').returncode)
        self.assertEqual(after, self.snapshot())

    def test_valid_default_cli_never_mutates_inputs(self):
        before = self.snapshot()
        self.assertEqual(0, self.cli().returncode)
        self.assertEqual(before, self.snapshot())

    def test_generation_cannot_follow_output_symlink(self):
        target = self.root / 'retained.md'
        target.write_text('retained user content')
        output = self.root / 'specs/CATALOG.md'
        output.unlink()
        output.symlink_to(target)
        self.assertEqual(1, self.cli('--write').returncode)
        self.assertEqual('retained user content', target.read_text())
        self.assertTrue(output.is_symlink())

    def test_historical_null_is_explicit_in_view(self):
        view = render_catalog(self.catalog)
        self.assertIn('not established (original null)', view)
        self.assertIn('2026-10-30 (structural)', view)
        self.assertIsNone(read_catalog(self.root)['capabilities'][0]['legacyVerification']['lastVerified'])

    def test_new_capability_without_history_does_not_invent_original_null(self):
        del self.item['legacyVerification']
        self.persist()
        self.assertEqual(0, self.cli().returncode)
        view = (self.root / 'specs/CATALOG.md').read_text()
        self.assertIn('not recorded (no prior catalog value)', view)
        self.assertNotIn('original null', view)

    def test_unknown_metadata_is_preserved(self):
        self.item['retainedDecision'] = {'accepted': True, 'runtimeVerified': False}
        self.persist()
        self.assertEqual(self.catalog, read_catalog(self.root))
        self.assertEqual([], self.errors())

    def test_profile_selector_rejected(self):
        self.catalog['$schema'] = 'custom-schema.json'
        self.rejected('only the supported')

    def test_empty_capability_inventory_rejected(self):
        self.catalog['capabilities'] = []
        self.rejected('nonempty array')

    def test_nonstring_metadata_rejected_without_crash(self):
        self.item['id'] = {'not': 'an identifier'}
        self.rejected('required metadata')

    def test_date_format_is_not_coerced(self):
        self.item['lastVerified'] = '20261030'
        self.rejected('actual YYYY-MM-DD')

    def test_missing_prerequisite_document_rejected(self):
        (self.root / 'specs/_template.md').unlink()
        self.rejected('missing regular prerequisite')

    def test_standard_requires_a_visible_product_title(self):
        path = self.root / 'specs/README.md'
        path.write_text(path.read_text().replace('# VaultStadio Product Specification Standard', 'No title', 1))
        self.rejected('eight populated')

    def test_nonarray_typed_evidence_rejected(self):
        self.item['tests'] = 'tests/control.py'
        self.rejected('required path array')

    def test_duplicate_typed_evidence_rejected(self):
        self.item['code'] *= 2
        self.rejected('duplicate path')

    def test_as_built_contract_cannot_omit_states_and_budget(self):
        self.item['status'] = 'as-built-baseline'
        errors = self.errors()
        self.assertTrue(any('missing baseline contract element' in e for e in errors))
        self.assertTrue(any('numeric baseline budget' in e for e in errors))

    def test_preserved_native_inventory_is_still_checked(self):
        self.item['evidence'] = ['missing-native-inventory']
        self.rejected('preserved native evidence')

    def test_catalog_symlink_rejected_without_mutating_target(self):
        path = self.root / 'specs/catalog.json'
        content = path.read_bytes()
        target = self.root / 'original.json'
        path.rename(target)
        path.symlink_to(target)
        self.assertEqual(1, self.cli().returncode)
        self.assertEqual(content, target.read_bytes())

    def test_primary_owner_cannot_escape_repository(self):
        self.item['specs'] = ['../outside.md']
        self.rejected('invalid boundary')


class ActualMigrationPreservation(unittest.TestCase):
    def test_original_owners_decisions_evidence_and_nullable_dates_remain(self):
        record = json.loads((REPO/'docs/development/verification/sdd-consumer-adoption-145-preservation.json').read_text())
        before = record['original_catalog']
        after = read_catalog(REPO)
        migration = load_migration(REPO)
        self.assertEqual({k:v for k,v in before.items() if k!='capabilities'},
                         {k:v for k,v in after.items() if k!='capabilities'})
        self.assertEqual(16, len(after['capabilities']))
        for old, new in zip(before['capabilities'], after['capabilities']):
            for key in ['id', 'title', 'scope', 'owner', 'evidence', 'openDecisions']:
                self.assertEqual(remap_catalog(old[key], migration), new[key])
            self.assertEqual(remap_catalog(old['specs'][0], migration), new['specs'][0])
            self.assertEqual({'status':old['status'],'lastVerified':old['lastVerified']}, new['legacyVerification'])
            self.assertEqual({'Proposed':'proposed','As-built baseline':'as-built-baseline'}[old['status']], new['status'])

    def test_original_sdd_and_discovery_bytes_remain(self):
        record = json.loads((REPO/'docs/development/verification/sdd-consumer-adoption-145-preservation.json').read_text())
        migration = load_migration(REPO)
        for name, previous in record['files'].items():
            prefix = original_owner_bytes(REPO, name, migration)[:previous['original_bytes']]
            self.assertEqual(previous['original_sha256'], hashlib.sha256(prefix).hexdigest(), name)
        for name, digest in record['discovery_sha256'].items():
            content = (original_owner_bytes(REPO, name, migration) if name.endswith('/README.md')
                       else (REPO/name).read_bytes())
            self.assertEqual(digest, hashlib.sha256(content).hexdigest(), name)


if __name__ == '__main__':
    unittest.main()
