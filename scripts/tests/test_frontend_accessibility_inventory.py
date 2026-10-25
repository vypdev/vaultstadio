import json
from pathlib import Path
import sys
import tempfile
import unittest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from frontend_accessibility_inventory import call_body, verify_inventory, verify_detekt_reports


class AccessibilityInventoryTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.source = self.root / "frontend/feature/demo/src/main/kotlin/Demo.kt"
        self.source.parent.mkdir(parents=True)
        self.source.write_text('OutlinedTextField(value = text, modifier = Modifier.inputSemantics("demo.name", label))')
        self.inventory = self.root / "docs/development/verification/accessibility-controls-80.json"
        self.inventory.parent.mkdir(parents=True)
        self.cases = self.root / "frontend/composeApp/src/desktopTest/kotlin/com/vaultstadio/app/accessibility/ActiveFieldInteractionTest.kt"
        self.cases.parent.mkdir(parents=True)
        self.cases.write_text('fun fields() = listOf("demo.name")')
        self.write_inventory([{"id": "demo.name", "source": self.source.relative_to(self.root).as_posix()}])

    def write_inventory(self, fields):
        self.inventory.write_text(json.dumps({"fields": fields, "fieldCount": len(fields)}))

    def test_complete_source_matches_inventory(self):
        self.assertEqual(1, verify_inventory(self.root))

    def test_new_unlabeled_field_fails(self):
        self.source.write_text(self.source.read_text() + '\nBasicTextField(value = text)')
        with self.assertRaisesRegex(ValueError, "no unique accessible identifier"):
            verify_inventory(self.root)

    def test_new_named_field_requires_an_inventory_case(self):
        self.source.write_text(self.source.read_text() + '\nBasicTextField(modifier = Modifier.inputSemantics("demo.other", label))')
        with self.assertRaisesRegex(ValueError, "added"):
            verify_inventory(self.root)

    def test_duplicate_production_identifier_fails(self):
        self.source.write_text(self.source.read_text() * 2)
        with self.assertRaisesRegex(ValueError, "Duplicate production"):
            verify_inventory(self.root)

    def test_removed_field_fails(self):
        self.source.write_text("")
        with self.assertRaisesRegex(ValueError, "missing"):
            verify_inventory(self.root)

    def test_wrong_source_ownership_fails(self):
        self.write_inventory([{"id": "demo.name", "source": "other.kt"}])
        with self.assertRaisesRegex(ValueError, "moved"):
            verify_inventory(self.root)

    def test_duplicate_inventory_entry_fails(self):
        field = {"id": "demo.name", "source": self.source.relative_to(self.root).as_posix()}
        self.write_inventory([field, field])
        with self.assertRaisesRegex(ValueError, "Duplicate identifiers"):
            verify_inventory(self.root)

    def test_empty_source_and_inventory_cannot_pass(self):
        self.source.write_text("")
        self.write_inventory([])
        with self.assertRaisesRegex(ValueError, "empty inventory"):
            verify_inventory(self.root)

    def test_nested_calls_and_quoted_parentheses_are_balanced(self):
        self.assertEqual('value = "(quoted)", onChange = { update(it) }', call_body('value = "(quoted)", onChange = { update(it) }) trailing', 0))

    def test_missing_collected_field_cases_fails(self):
        self.cases.unlink()
        with self.assertRaisesRegex(ValueError, "Missing collected"):
            verify_inventory(self.root)

    def test_an_inventory_entry_without_a_registered_field_case_fails(self):
        self.cases.write_text('fun fields() = listOf("demo.other")')
        with self.assertRaisesRegex(ValueError, "differ from the inventory"):
            verify_inventory(self.root)

    def clean_reports(self):
        for module in ["feature/auth", "core/ui", "core/resources"]:
            path = self.root / "frontend" / module / "build/reports/detekt/detekt.xml"
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("<checkstyle />")

    def test_real_source_reports_are_required(self):
        with self.assertRaisesRegex(ValueError, "Missing actual-source"):
            verify_detekt_reports(self.root)

    def test_clean_actual_source_reports_pass(self):
        self.clean_reports()
        self.assertEqual(3, verify_detekt_reports(self.root))

    def test_authentication_finding_cannot_be_ignored(self):
        self.clean_reports()
        path = self.root / "frontend/feature/auth/build/reports/detekt/detekt.xml"
        path.write_text('<checkstyle><file name="AuthScreen.kt"><error /></file></checkstyle>')
        with self.assertRaisesRegex(ValueError, "has Detekt findings"):
            verify_detekt_reports(self.root)

    def test_new_localized_label_finding_cannot_be_ignored(self):
        self.clean_reports()
        path = self.root / "frontend/core/resources/build/reports/detekt/detekt.xml"
        path.write_text('<checkstyle><file name="InputLabel.kt"><error /></file></checkstyle>')
        with self.assertRaisesRegex(ValueError, "has Detekt findings"):
            verify_detekt_reports(self.root)

    def test_malformed_report_format_cannot_pass(self):
        self.clean_reports()
        path = self.root / "frontend/core/ui/build/reports/detekt/detekt.xml"
        path.write_text("<unexpected />")
        with self.assertRaisesRegex(ValueError, "Unexpected Detekt"):
            verify_detekt_reports(self.root)

    def test_unterminated_call_fails(self):
        with self.assertRaisesRegex(ValueError, "Unterminated"):
            call_body("no closing delimiter", 0)


if __name__ == "__main__":
    unittest.main()
