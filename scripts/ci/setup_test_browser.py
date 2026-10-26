#!/usr/bin/env python3
"""Run the shared project-owned browser installer used by CI and trusted setup."""
from pathlib import Path
import runpy

if __name__ == "__main__":
    runpy.run_path(str(Path(__file__).resolve().parents[2] /
                      ".github/actions/copilot-verification-setup/setup-test-browser.py"), run_name="__main__")
