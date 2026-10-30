"""Actual owner-path migration, source reconstruction and failure controls."""
import copy
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
from specification_catalog import boundary, read_catalog
from sdd_path_preservation import (check_owner_links, git_blob, load_migration,
                                   original_owner_bytes, require_catalog_preservation, validate_mapping)


class OwnerPathMigrationControls(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.record = load_migration(REPO)

    def owned_fixture(self):
        temporary = tempfile.TemporaryDirectory(prefix='sdd-owner-preservation-')
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        row = next(row for row in self.record['owners'] if row['capability_id'] == 'metadata-search-and-ai')
        target = root / row['new']
        target.parent.mkdir(parents=True)
        shutil.copyfile(REPO / row['new'], target)
        return root, row, target

    def test_flat_lowercase_owner_paths_are_supported(self):
        for name in ['specs/a.md', 'specs/sdd-0012-metadata-search-and-ai.md',
                     'specs/product-discovery-2026-10-29.md']:
            with self.subTest(name=name):
                self.assertTrue(boundary('specs', name))

    def test_uppercase_owner_is_not_publishable(self):
        self.assertFalse(boundary('specs', 'specs/SDD-0012-metadata-search-and-ai.md'))

    def test_nested_owner_is_not_readable_by_published_writer(self):
        self.assertFalse(boundary('specs', 'specs/discovery/2026-10-29/README.md'))

    def test_complete_metadata_and_evidence_survive_path_rebasing(self):
        require_catalog_preservation(read_catalog(REPO), self.record)

    def test_every_primary_reconstructs_its_original_git_blob(self):
        primaries = [row for row in self.record['owners'] if row['primary']]
        self.assertEqual(16, len(primaries))
        for row in primaries:
            with self.subTest(owner=row['old']):
                original = original_owner_bytes(REPO, row['old'], self.record)
                self.assertEqual(row['source_git_blob'], git_blob(original))

    def test_discovery_companion_keeps_original_accepted_contract(self):
        old = 'specs/discovery/2026-10-29/README.md'
        original = original_owner_bytes(REPO, old, self.record).decode()
        self.assertEqual(30, sum(line.startswith('| PD-') for line in original.splitlines()))
        self.assertIn('90 planned acceptance families', original)

    def test_verbatim_answers_remain_byte_exact(self):
        answer = self.record['verbatim_answers']
        self.assertEqual(answer['source_git_blob'], git_blob((REPO / answer['path']).read_bytes()))

    def test_owner_mapping_is_bijective_and_preserves_primary_order(self):
        validate_mapping(self.record)
        current = read_catalog(REPO)
        original_ids = [c['id'] for c in self.record['catalog_before']['capabilities']]
        original_paths = {row['new'] for row in self.record['owners']}
        self.assertEqual(original_ids, [c['id'] for c in current['capabilities'] if c['id'] in original_ids])
        self.assertEqual([row['new'] for row in self.record['owners']],
                         [name for c in current['capabilities'] for name in c['specs'] if name in original_paths])

    def test_colliding_migration_map_is_rejected(self):
        changed = copy.deepcopy(self.record)
        changed['owners'][1]['new'] = changed['owners'][0]['new']
        with self.assertRaisesRegex(ValueError, 'colliding'):
            validate_mapping(changed)

    def test_original_requirement_tamper_is_rejected(self):
        root, row, target = self.owned_fixture()
        content = target.read_text()
        self.assertTrue(content.startswith('# Metadata'))
        target.write_text(content.replace('# Metadata', '# Unwanted', 1))
        with self.assertRaisesRegex(ValueError, 'Original owner content|reference edit'):
            original_owner_bytes(root, row['old'], self.record)

    def test_changed_reference_edit_is_rejected(self):
        root, row, target = self.owned_fixture()
        edit = next(edit for edit in row['edits'] if edit['after'])
        text = target.read_text()
        at = edit['after_start']
        target.write_text(text[:at] + '!' + text[at + 1:])
        with self.assertRaisesRegex(ValueError, 'reference edit'):
            original_owner_bytes(root, row['old'], self.record)

    def test_live_links_use_exact_case_and_rebased_provenance(self):
        paths = subprocess.check_output(['git', 'ls-files', '-z'], cwd=REPO).decode().split('\0')
        inventory = set(paths)
        for name in paths:
            if name.endswith('.md'):
                with self.subTest(path=name):
                    check_owner_links((REPO / name).read_text(), name, inventory, self.record)

    def test_legacy_link_is_denied_even_on_case_insensitive_filesystem(self):
        row = self.record['owners'][0]
        with self.assertRaisesRegex(ValueError, 'historical owner path'):
            check_owner_links('[contract](' + row['old'] + ')', 'README.md',
                              {row['new']}, self.record)

    def test_partial_migration_missing_owner_is_rejected(self):
        root, row, target = self.owned_fixture()
        target.unlink()  # Owned disposable fixture only.
        with self.assertRaisesRegex(ValueError, 'missing'):
            original_owner_bytes(root, row['old'], self.record)

    def test_rollback_reconstructs_original_regular_file_set(self):
        temporary = tempfile.TemporaryDirectory(prefix='sdd-owner-rollback-')
        self.addCleanup(temporary.cleanup)
        root = Path(temporary.name)
        for row in self.record['owners']:
            original = original_owner_bytes(REPO, row['old'], self.record)
            target = root / row['old']
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(original)
            self.assertEqual(row['source_git_blob'], git_blob(target.read_bytes()))
        self.assertEqual(17, len(list(root.rglob('*.md'))))


    def test_supported_additive_companion_and_evidence_are_allowed(self):
        current = copy.deepcopy(read_catalog(REPO))
        item = next(c for c in current['capabilities'] if c['id'] == 'metadata-search-and-ai')
        item['specs'].append('specs/filename-search-and-suggestions.md')
        item['tests'].append('backend/domain/storage/src/test/kotlin/FilenameSearchTest.kt')
        item['evidence'].append('docs/development/verification/future-filename-evidence.md')
        require_catalog_preservation(current, self.record)

    def test_new_capability_does_not_hide_original_inventory(self):
        current = copy.deepcopy(read_catalog(REPO))
        future = copy.deepcopy(current['capabilities'][-1])
        future['id'] = 'future-distinct-capability'
        future['title'] = 'Future distinct capability'
        future['status'] = 'proposed'
        future['specs'] = ['specs/future-distinct-capability.md']
        current['capabilities'].append(future)
        require_catalog_preservation(current, self.record)

    def test_additive_work_cannot_drop_original_evidence(self):
        current = copy.deepcopy(read_catalog(REPO))
        current['capabilities'][0]['tests'].pop(0)
        with self.assertRaisesRegex(ValueError, 'evidence or owner removed'):
            require_catalog_preservation(current, self.record)

    def test_companion_cannot_replace_the_original_primary(self):
        current = copy.deepcopy(read_catalog(REPO))
        current['capabilities'][0]['specs'].insert(0, 'specs/replacement-primary.md')
        with self.assertRaisesRegex(ValueError, 'primary owner was replaced'):
            require_catalog_preservation(current, self.record)


if __name__ == '__main__':
    unittest.main()
