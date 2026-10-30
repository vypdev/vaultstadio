#!/usr/bin/env python3
"""Validate offline without mutation; --write explicitly generates CATALOG.md."""
from pathlib import Path
import argparse
import sys

sys.dont_write_bytecode = True

from specification_catalog import read_catalog, regular_file, validate_catalog
from specification_catalog_render import render_catalog


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--write', action='store_true', help='Generate only specs/CATALOG.md after validation')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    try:
        catalog = read_catalog(root)
        errors = validate_catalog(root, catalog)
    except (OSError, UnicodeError, ValueError) as exc:
        errors = [str(exc)]
    if errors:
        for error in errors:
            print('ERROR: ' + error)
        print(f'specification_catalog=FAIL errors={len(errors)}')
        return 1
    generated = render_catalog(catalog)
    path = root / 'specs/CATALOG.md'
    if (path.exists() or path.is_symlink()) and not regular_file(root, 'specs/CATALOG.md'):
        print('ERROR: specs/CATALOG.md must be a regular contained repository file')
        return 1
    if args.write:
        path.write_text(generated)
    elif not path.is_file() or path.read_text() != generated:
        print('ERROR: specs/CATALOG.md is stale; run python3 scripts/validate-specification-catalog.py --write')
        return 1
    print(f'specification_catalog=PASS capabilities={len(catalog["capabilities"])} mode={"generate" if args.write else "validate"}')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
