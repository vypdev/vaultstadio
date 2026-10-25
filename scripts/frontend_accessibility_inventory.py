"""Check active production text fields against the reviewed interaction inventory."""
import json
import re
from pathlib import Path

FIELD_CALL = re.compile(r"\b(?:OutlinedTextField|BasicTextField)\s*\(")
FIELD_ID = re.compile(r'\.inputSemantics\(\s*"([a-z0-9.-]+)"\s*,')


def call_body(source, start):
    depth, position = 1, start
    quoted = escaped = False
    while position < len(source):
        char = source[position]
        position += 1
        if escaped:
            escaped = False
        elif quoted and char == "\\":
            escaped = True
        elif char == '"':
            quoted = not quoted
        elif not quoted:
            depth += (char == "(") - (char == ")")
            if depth == 0:
                return source[start:position - 1]
    raise ValueError("Unterminated production text-field call")


def verify_inventory(root):
    root = Path(root).resolve()
    inventory = json.loads((root / "docs/development/verification/accessibility-controls-80.json").read_text())
    fields = inventory["fields"]
    expected = {field["id"]: field["source"] for field in fields}
    if len(expected) != len(fields) or inventory["fieldCount"] != len(fields):
        raise ValueError("Duplicate identifiers or inconsistent field inventory count")
    actual = {}
    roots = [root / "frontend/feature", root / "frontend/composeApp/src/main/kotlin/com/vaultstadio/app/feature"]
    for source_root in roots:
        for path in sorted(source_root.rglob("*.kt")):
            relative = path.relative_to(root).as_posix()
            if source_root == roots[0] and "/src/main/" not in relative:
                continue
            source = path.read_text()
            for call in FIELD_CALL.finditer(source):
                identifiers = FIELD_ID.findall(call_body(source, call.end()))
                if len(identifiers) != 1:
                    raise ValueError(f"A production text field has no unique accessible identifier: {relative}")
                identifier = identifiers[0]
                if identifier in actual:
                    raise ValueError(f"Duplicate production input identifier: {identifier}")
                actual[identifier] = relative
    if actual != expected:
        missing = sorted(set(expected) - set(actual))
        added = sorted(set(actual) - set(expected))
        moved = sorted(key for key in set(actual) & set(expected) if actual[key] != expected[key])
        raise ValueError(f"Interaction inventory drift: missing={missing}; added={added}; moved={moved}")
    if not actual:
        raise ValueError("An empty inventory cannot establish accessible input coverage")
    case_path = root / "frontend/composeApp/src/desktopTest/kotlin/com/vaultstadio/app/accessibility/ActiveFieldInteractionTest.kt"
    if not case_path.is_file():
        raise ValueError("Missing collected production-field interaction cases")
    cases = case_path.read_text()
    declaration = re.search(r"fun\s+fields\s*\(\)[^{]*?=\s*listOf\(", cases)
    if declaration is None:
        raise ValueError("Missing collected production-field interaction cases")
    registered = re.findall(r'"([a-z0-9.-]+)"', call_body(cases, declaration.end()))
    if set(registered) != set(actual) or len(registered) != len(actual):
        raise ValueError("Collected production-field interaction cases differ from the inventory")
    return len(actual)


def verify_detekt_reports(root):
    """Require actual source reports and no findings in the new accessibility scope."""
    import xml.etree.ElementTree as element_tree
    root = Path(root)
    scopes = {"feature/auth": None, "core/ui": None, "core/resources": {"InputLabel.kt"}}
    for module, selected_names in scopes.items():
        path = root / "frontend" / module / "build/reports/detekt/detekt.xml"
        if not path.is_file():
            raise ValueError(f"Missing actual-source Detekt report: {module}")
        report = element_tree.parse(path).getroot()
        if report.tag != "checkstyle":
            raise ValueError(f"Unexpected Detekt report format: {module}")
        for source in report.findall("file"):
            if selected_names is not None and Path(source.get("name", "")).name not in selected_names:
                continue
            if source.findall("error"):
                raise ValueError(f"Accessibility source has Detekt findings: {module}/{Path(source.get('name', '')).name}")
    return len(scopes)
