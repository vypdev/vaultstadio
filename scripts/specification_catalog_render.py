"""Deterministic rendering; inventory dates do not establish runtime acceptance."""
from specification_catalog import PATH_FIELDS, STATUS_LABELS


def cell(value):
    return str(value).replace('|', '\\|').replace('\r', ' ').replace('\n', ' ')


def link(path):
    target = './' + path[6:] if path.startswith('specs/') else '../' + path
    return f'[`{path}`]({target})'


def render_catalog(catalog):
    lines = [
        '# Product capability specification catalog', '',
        '> Generated from [`catalog.json`](./catalog.json).',
        '> Generate: `python3 scripts/validate-specification-catalog.py --write`.',
        '> Validate: `python3 scripts/validate-specification-catalog.py`.', '',
        'Inventory status is separate from document readiness and runtime acceptance.',
        '`lastVerified` records structural evidence-inventory review. It does not',
        'refresh historical runtime receipts or resolve blocking decisions.',
        'Original dates (including null), statuses, flat evidence and the root',
        'checkpoint remain preserved; see the',
        '[migration record](../docs/development/verification/SDD_CONSUMER_ADOPTION_145.md).', '',
        '| Capability | Inventory state | Primary SDD | Inventory review | Previous catalog verification |',
        '|---|---|---|---|---|',
    ]
    for item in catalog['capabilities']:
        history = item.get('legacyVerification')
        previous = 'not recorded (no prior catalog value)'
        if isinstance(history, dict) and 'lastVerified' in history:
            previous = history['lastVerified']
            previous = 'not established (original null)' if previous is None else previous
        lines.append(f"| `{item['id']}` | {STATUS_LABELS[item['status']]} | {link(item['specs'][0])} | {item['lastVerified']} (structural) | {cell(previous)} |")
    lines.extend(['', '## Evidence inventory', ''])
    for item in catalog['capabilities']:
        lines.extend([f"### `{item['id']}` — {item['title']}", '',
                      '- Owner: ' + item['owner'], '- Scope: ' + item['scope'],
                      '- Open decisions: ' + item.get('openDecisions', 'See primary SDD.'),
                      '- Inventory review scope: ' + item.get('structuralVerification', {}).get('scope', 'See owning SDD; no runtime acceptance inferred.')])
        for field in PATH_FIELDS:
            rendered = ' · '.join(link(p) for p in item[field]) or 'Not applicable.'
            lines.append('- ' + field.capitalize() + ': ' + rendered)
        lines.append('- Preserved native inventory: ' + ' · '.join(link(p) for p in item.get('evidence', [])))
        lines.append('')
    lines.extend(['## Maintenance', '',
                  'Read the primary SDD and companions before changing a capability.',
                  'Keep requirements, evidence paths, tests and user documentation aligned.',
                  'A green catalog establishes structural validity, not implemented behavior.', ''])
    return '\n'.join(lines)
