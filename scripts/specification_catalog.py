"""Offline validation of the supported version-1 SDD publication profile."""
from __future__ import annotations

import datetime
import json
import re
from pathlib import Path

PATH_FIELDS = ('specs', 'workflows', 'entrypoints', 'code', 'tests', 'documentation')
STATUS_LABELS = {'as-built-baseline': 'As-built baseline', 'implemented': 'Implemented',
                 'proposed': 'Proposed', 'deprecated': 'Deprecated'}
METADATA = ('Status:', 'Date:', 'Catalog capability ID:', 'Last verified:', 'Owners:',
            'Scope:', 'Related issues/PRs:', 'Required review gates:', 'Open decisions blocking readiness:')
REQUIRED_HEADINGS = ('## 1. Executive summary', '## 4. Goals', '## 8. Clean Architecture',
                     '## 14. Testing strategy', '## 16. Acceptance')


def iso_date(value):
    if not isinstance(value, str) or not re.fullmatch(r'\d{4}-\d{2}-\d{2}', value):
        return False
    try:
        datetime.date.fromisoformat(value)
        return True
    except ValueError:
        return False


def safe_path(value):
    return (isinstance(value, str) and bool(value) and value == value.strip()
            and not re.search(r'[\\:*?"<>|\x00-\x1f\x7f-\x9f]', value)
            and all(p not in {'', '.', '..'} and p.lower() != '.git' for p in value.split('/')))


def boundary(field, value):
    if not safe_path(value):
        return False
    if field == 'specs':
        return bool(re.fullmatch(r'specs/[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.md', value))
    if field == 'workflows':
        return bool(re.fullmatch(r'(?:\.github|setup)/workflows/.+\.ya?ml', value))
    return field in PATH_FIELDS


def regular_file(root, value):
    if not safe_path(value):
        return False
    root = root.resolve()
    current = root
    try:
        for part in value.split('/'):
            current = current / part
            if current.is_symlink():
                return False
        return current.is_file() and current.resolve().is_relative_to(root)
    except OSError:
        return False


def visible_markdown(text):
    text = re.sub(r'<!--.*?(?:-->|\Z)', '\n', text, flags=re.S)
    lines, fence = [], None
    for line in text.replace('\r\n', '\n').splitlines():
        match = re.match(r' {0,3}(`{3,}|~{3,})', line)
        if match:
            value = match[1]
            if fence is None:
                fence = value
            elif value[0] == fence[0] and len(value) >= len(fence):
                fence = None
            continue
        if fence is None:
            lines.append(line)
    return '\n'.join(lines)


def numbered_sections(text, count):
    if not re.match(r'# \S', text):
        return False
    headings = list(re.finditer(r'^## ([1-9]\d*)\. [^\n]+$', text, re.M))
    if len(headings) != count:
        return False
    return all(int(h[1]) == index + 1 and any(c.isalnum() for c in
               text[h.end():headings[index + 1].start() if index + 1 < count else len(text)])
               for index, h in enumerate(headings))


def validate_prerequisites(root):
    errors, documents = [], {}
    for name in ['README.md', '_template.md']:
        relative = 'specs/' + name
        if not regular_file(root, relative):
            errors.append(relative + ': missing regular prerequisite document')
        else:
            documents[name] = visible_markdown((root / relative).read_text())
    standard, template = documents.get('README.md', ''), documents.get('_template.md', '')
    if not numbered_sections(standard, 8):
        errors.append('specs/README.md: expected eight populated numbered standard sections')
    prose = re.split(r'^## [1-9]\d*\. [^\n]+$', standard, flags=re.M)[1:]
    normative = any('A MUST requires an acceptance scenario.' in re.sub(
        r'\s+', ' ', re.sub(r'[`*_]', '', re.sub(r'^#{1,6} [^\n]*$', '', s, flags=re.M))) for s in prose)
    if not normative:
        errors.append('specs/README.md: missing normative acceptance guidance in section prose')
    if not numbered_sections(template, 20) or not all(
            any(line.startswith(h) for line in template.splitlines()) for h in REQUIRED_HEADINGS):
        errors.append('specs/_template.md: expected twenty populated sections and required headings')
    return errors


def validate_primary(root, capability):
    relative = capability['specs'][0]
    text = (root / relative).read_text()
    errors = []
    for marker in METADATA:
        if '- ' + marker not in text:
            errors.append(relative + ': missing metadata ' + marker)
    if f"Catalog capability ID: `{capability['id']}`" not in text:
        errors.append(relative + ': primary capability ID differs')
    visible = visible_markdown(text)
    headings = list(re.finditer(r'^## ([^\n]+)$', visible, re.M))
    for section in range(1, 21):
        matching = [(index, heading) for index, heading in enumerate(headings)
                    if re.match(rf'{section}\.\s', heading[1])]
        if not matching:
            errors.append(f'{relative}: missing required section {section}')
        for index, heading in matching:
            end = headings[index + 1].start() if index + 1 < len(headings) else len(visible)
            body = re.sub(r'^#{3,6} [^\n]*$', '', visible[heading.end():end], flags=re.M)
            if not any(character.isalnum() for character in body):
                errors.append(f'{relative}: primary section {section} requires visible content or a not-applicable explanation')
    if capability['status'] == 'as-built-baseline':
        for token in ['- Status: As-built baseline', 'Observed behavior:', 'Intentional contract:',
                      'Known debt and limitations:', 'Unknown rationale:', 'Proposed improvements:',
                      '```mermaid', 'Pending:', 'Action required:', 'Blocked:', 'Partial:', 'Complete:']:
            if token not in text:
                errors.append(relative + ': missing baseline contract element ' + token)
        if not re.search(r'\| \*\*Total\*\* \| \*\*\d+\*\* \|', text):
            errors.append(relative + ': missing numeric baseline budget total')
    return errors


def validate_catalog(root, catalog):
    errors = []
    if not isinstance(catalog, dict) or type(catalog.get('version')) is not int or catalog['version'] != 1:
        return ['catalog.version must be 1 in a JSON object']
    capabilities = catalog.get('capabilities')
    if not isinstance(capabilities, list) or not capabilities:
        return ['catalog.capabilities must be a nonempty array']
    if '$schema' in catalog or any(isinstance(c, dict) and 'specification' in c for c in capabilities):
        errors.append('catalog must use only the supported SDK version-1 publication profile')
    ids, titles, owners = set(), set(), set()
    for index, capability in enumerate(capabilities):
        prefix = f'capabilities[{index}]'
        if not isinstance(capability, dict) or any(
                not isinstance(capability.get(k), str) or not capability[k].strip()
                for k in ['id', 'title', 'status', 'scope', 'owner', 'lastVerified']):
            errors.append(prefix + ': required metadata must be nonempty strings')
            continue
        cid = capability['id']
        if not re.fullmatch(r'[a-z0-9](?:[a-z0-9-]*[a-z0-9])?', cid) or cid in ids:
            errors.append(prefix + ': invalid or duplicate capability ID')
        if capability['title'] in titles:
            errors.append(prefix + ': duplicate capability title')
        ids.add(cid)
        titles.add(capability['title'])
        if capability['status'] not in STATUS_LABELS:
            errors.append(cid + ': unsupported inventory status')
        if not iso_date(capability['lastVerified']):
            errors.append(cid + ': lastVerified must be an actual YYYY-MM-DD date')
        for field in PATH_FIELDS:
            values = capability.get(field)
            if not isinstance(values, list) or (field != 'workflows' and not values):
                errors.append(f'{cid}.{field}: required path array')
                continue
            seen = set()
            for value in values:
                if not boundary(field, value) or not regular_file(root, value):
                    errors.append(f'{cid}.{field}: invalid boundary or nonregular path {value!r}')
                    continue
                if value in seen:
                    errors.append(f'{cid}.{field}: duplicate path {value}')
                seen.add(value)
                if field == 'specs':
                    if value in owners:
                        errors.append(f'{cid}: duplicate specification owner {value}')
                    owners.add(value)
        primary = capability.get('specs')
        if isinstance(primary, list) and primary and regular_file(root, primary[0]):
            errors.extend(validate_primary(root, capability))
        evidence = capability.get('evidence', [])
        if not isinstance(evidence, list) or any(not safe_path(p) or not (root / p).exists() for p in evidence):
            errors.append(cid + ': preserved native evidence contains an invalid path')
    excluded = {'README.md', '_template.md', 'CATALOG.md'}
    for file in sorted((root / 'specs').rglob('*.md')):
        relative = file.relative_to(root).as_posix()
        if file.parent == root / 'specs' and file.name in excluded:
            continue
        if relative not in owners:
            errors.append('unregistered specification: ' + relative)
    return errors + validate_prerequisites(root)


def read_catalog(root):
    if not regular_file(root, 'specs/catalog.json'):
        raise ValueError('specs/catalog.json must be a regular contained repository file')
    return json.loads((root / 'specs/catalog.json').read_text())
