#!/usr/bin/env python3
"""Contributor/CI entrypoint: complete independent suites, original failure status."""

from __future__ import annotations

import argparse
import sys

from coverage_run import generate
from discovery import REPOSITORY, SUITES, VerificationError, discover, gradle
from execution import test_suites
from state import exclusive, invalidate


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("operation", choices=("test", "coverage", "lint"))
    parser.add_argument("--root", choices=("backend", "frontend", "all"), default="all")
    parser.add_argument("--suite", choices=("desktop", "browser"))
    parser.add_argument(
        "--reuse-tests",
        action="store_true",
        help="Coverage only: require matching successful existing execution; never silently rerun",
    )
    args = parser.parse_args()
    if args.suite and (args.root != "frontend" or args.operation != "test"):
        parser.error("--suite requires test --root frontend")
    if args.reuse_tests and args.operation != "coverage":
        parser.error("--reuse-tests requires coverage")
    overall = 0
    for area in SUITES if args.root == "all" else (args.root,):
        root = REPOSITORY / area
        try:
            with exclusive(root):
                if args.operation == "lint":
                    code = gradle(
                        root,
                        ["detektMain" if area == "backend" else "detektDesktopMain"],
                    )
                    overall = overall or code
                    continue
                suites = (args.suite,) if args.suite else SUITES[area]
                invalidate(root, () if args.reuse_tests else suites)
                model = discover(root)
                if args.operation == "test" or not args.reuse_tests:
                    code = test_suites(root, suites, model)
                    if code:
                        overall = overall or code
                        continue
                if args.operation == "coverage":
                    code = generate(root, model)
                    overall = overall or code
        except (OSError, VerificationError) as error:
            print(
                f"Verification failed in {area}: {error}", file=sys.stderr, flush=True
            )
            overall = overall or 1
    if not overall:
        print("All required verification stages passed.", flush=True)
    return 0 if overall == 0 else (overall if overall > 0 else 1)


if __name__ == "__main__":
    sys.exit(main())
