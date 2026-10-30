#!/usr/bin/env python3
"""Fail on an active field missing from the production interaction test inventory."""
from pathlib import Path
import argparse
from frontend_accessibility_inventory import verify_inventory, verify_detekt_reports

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--detekt-reports", action="store_true")
    options = parser.parse_args()
    if options.detekt_reports:
        verify_detekt_reports(Path(__file__).resolve().parents[1])
    count = verify_inventory(Path(__file__).resolve().parents[1])
    print(f"Accessible input inventory verified: {count} inventoried production fields")
