"""Verify the recorded path migration against immutable Git blob identities."""
import hashlib
import json
from pathlib import Path
import posixpath
import re
from urllib.parse import unquote, urlsplit

from specification_catalog import boundary, regular_file

MANIFEST = 'docs/development/verification/sdd-owner-path-migration-178.json'


def git_blob(data):
    return hashlib.sha1(b'blob ' + str(len(data)).encode() + b'\0' + data).hexdigest()


def load_migration(root):
    record = json.loads((root / MANIFEST).read_text())
    validate_mapping(record)
    return record


def validate_mapping(record):
    rows = record['owners']
    if (record['version'] != 1 or len(rows) != 17
            or sum(row['primary'] for row in rows) != 16
            or len({row['old'] for row in rows}) != 17
            or len({row['new'].casefold() for row in rows}) != 17
            or any(not boundary('specs', row['new']) for row in rows)):
        raise ValueError('Invalid or colliding owner migration map')


def reverse_edits(text, edits):
    previous = len(text) + 1
    for edit in reversed(edits):
        start = edit['after_start']
        end = start + len(edit['after'])
        if start < 0 or end > previous or text[start:end] != edit['after']:
            raise ValueError('Recorded reference edit differs from the current source')
        text = text[:start] + edit['before'] + text[end:]
        previous = start
    return text


def original_owner_bytes(root, old_path, record=None):
    record = record or load_migration(root)
    row = next(row for row in record['owners'] if row['old'] == old_path)
    if not regular_file(root, row['new']):
        raise ValueError('Canonical owner is missing or is not a regular contained file')
    recovered = reverse_edits((root / row['new']).read_text(), row['edits']).encode()
    original = recovered[:row['source_bytes']]
    if git_blob(original) != row['source_git_blob']:
        raise ValueError('Original owner content was changed beyond recorded path rebasing')
    return original


def remap_catalog(value, record):
    mapping = {row['old']: row['new'] for row in record['owners']}
    if isinstance(value, str):
        return mapping.get(value, value)
    if isinstance(value, list):
        return [remap_catalog(item, record) for item in value]
    if isinstance(value, dict):
        return {key: remap_catalog(item, record) for key, item in value.items()}
    return value


def check_owner_links(text, relative, inventory, record):
    mapping = {row['old']: row['new'] for row in record['owners']}
    canonical = set(mapping.values())
    discovery = mapping['specs/discovery/2026-10-29/README.md']
    patterns = [r'\]\((?P<target><[^>]+>|[^)\s]+)',
                r'(?m)^\s*\[[^\]]+\]:\s*(?P<target><[^>]+>|\S+)']
    for pattern in patterns:
        for match in re.finditer(pattern, text):
            raw = match.group('target').strip('<>')
            parts = urlsplit(raw)
            if parts.scheme or parts.netloc or not parts.path or parts.path.startswith('/'):
                continue
            resolved = posixpath.normpath(posixpath.join(posixpath.dirname(relative), unquote(parts.path)))
            if resolved in mapping:
                raise ValueError('Live link retains a historical owner path: ' + resolved)
            if (resolved in canonical or relative == discovery) and resolved not in inventory:
                raise ValueError('Live owner/provenance link does not match the exact file inventory: ' + resolved)
