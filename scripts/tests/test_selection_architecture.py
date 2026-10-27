"""Selection policy and orchestration must remain independent of transport and SQL."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
SOURCES = {
    'backend/domain/storage/src/main/kotlin/com/vaultstadio/domain/storage/model/StorageSelection.kt': (
        'arrow.core.',
    ),
    'backend/core/storage/src/main/kotlin/com/vaultstadio/core/domain/service/StorageSelections.kt': (
        'arrow.core.', 'com.vaultstadio.domain.',
    ),
}


class SelectionArchitectureTests(unittest.TestCase):
    def test_selection_policy_and_orchestration_depend_only_on_domain_values_and_ports(self):
        for name, allowed in SOURCES.items():
            source = (ROOT / name).read_text()
            imports = re.findall(r'^import\s+([\w.]+)', source, re.MULTILINE)
            self.assertTrue(imports)
            for imported in imports:
                with self.subTest(source=name, imported=imported):
                    self.assertTrue(imported.startswith(allowed), imported)

    def test_pure_failure_model_has_no_http_presentation_or_database_contract(self):
        source = (ROOT / next(iter(SOURCES))).read_text()
        self.assertNotRegex(source, r'\b(?:httpStatus|statusCode|description|message|SQLException)\b')
        self.assertNotRegex(source, r'\b(?:400|401|403|404|409|500)\b')


if __name__ == '__main__':
    unittest.main()
