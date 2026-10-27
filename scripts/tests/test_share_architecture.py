"""Public share authority must remain independent of transport and provider implementations."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[2]
MODULES = ['backend/domain/share', 'backend/core/share-access']


class ShareArchitectureTests(unittest.TestCase):
    def test_only_domain_projects_supply_production_dependencies(self):
        for name in MODULES:
            with self.subTest(module=name):
                build = (ROOT / name / 'build.gradle.kts').read_text()
                projects = re.findall(r'(?:api|implementation)\(project\("([^"]+)"\)\)', build)
                self.assertTrue(projects, 'The policy module must declare its domain dependencies')
                self.assertTrue(all(p.startswith(':domain:') for p in projects), projects)
                libraries = re.findall(r'(?:api|implementation)\((libs\.[\w.]+)\)', build)
                self.assertTrue(all(p in {
                    'libs.kotlinx.coroutines.core', 'libs.kotlinx.datetime',
                    'libs.kotlinx.serialization.json', 'libs.bundles.arrow',
                } for p in libraries), libraries)

    def test_failure_model_has_no_http_or_presentation_mapping(self):
        failure = (ROOT / 'backend/domain/share/src/main/kotlin/com/vaultstadio/domain/share/access/PublicShareFailure.kt').read_text()
        self.assertNotRegex(failure, r'\b(?:400|401|403|404|409|410|500)\b')
        self.assertNotIn('val description:', failure)
        self.assertNotIn('val status:', failure)

    def test_domain_and_orchestration_cannot_import_frameworks_or_concrete_adapters(self):
        allowed = ('com.vaultstadio.domain.', 'com.vaultstadio.core.share.access.')
        forbidden = ('io.ktor.', 'org.jetbrains.exposed.', 'org.koin.', 'software.amazon.',
                     'io.github.', 'java.nio.file.', 'java.net.', 'java.sql.')
        for name in MODULES:
            paths = list((ROOT / name / 'src/main/kotlin').rglob('*.kt'))
            self.assertTrue(paths, f'No production sources found for {name}')
            for path in paths:
                for imported in re.findall(r'^import\s+([\w.]+)', path.read_text(), re.MULTILINE):
                    with self.subTest(file=str(path.relative_to(ROOT)), imported=imported):
                        self.assertFalse(imported.startswith(forbidden))
                        if imported.startswith('com.vaultstadio.'):
                            self.assertTrue(imported.startswith(allowed))


if __name__ == '__main__':
    unittest.main()
