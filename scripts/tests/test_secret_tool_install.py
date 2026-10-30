import hashlib
import io
import os
import subprocess
import sys
import tarfile
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "ci"))
import install_secret_tools as installer


class SecretToolInstallation(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        self.addCleanup(self.temporary.cleanup)

    def archive(self, members=(("gitleaks", b"executable", tarfile.REGTYPE),)):
        target = self.root / "fixture.tar.gz"
        with tarfile.open(target, "w:gz") as package:
            for name, data, kind in members:
                entry = tarfile.TarInfo(name)
                entry.type, entry.size = kind, len(data)
                package.addfile(entry, io.BytesIO(data) if kind == tarfile.REGTYPE else None)
        return target, hashlib.sha256(target.read_bytes()).hexdigest()

    def test_valid_binary_only_is_extracted_and_executable(self):
        archive, digest = self.archive((("../outside", b"never", tarfile.REGTYPE),
                                        ("gitleaks", b"executable", tarfile.REGTYPE)))
        output = self.root / "gitleaks"
        installer.install_archive(archive, digest, output)
        self.assertEqual(output.read_bytes(), b"executable")
        self.assertEqual(output.stat().st_mode & 0o777, 0o700)
        self.assertFalse((self.root.parent / "outside").exists())

    def test_altered_archive_is_rejected_before_execution(self):
        archive, digest = self.archive()
        archive.write_bytes(archive.read_bytes() + b"altered")
        with self.assertRaisesRegex(ValueError, "integrity"):
            installer.install_archive(archive, digest, self.root / "gitleaks")
        self.assertFalse((self.root / "gitleaks").exists())

    def test_missing_duplicate_link_empty_and_oversized_binary_are_rejected(self):
        cases = [(("../gitleaks", b"x", tarfile.REGTYPE),),
                 (("gitleaks", b"a", tarfile.REGTYPE), ("gitleaks", b"b", tarfile.REGTYPE)),
                 (("gitleaks", b"", tarfile.SYMTYPE),), (("gitleaks", b"", tarfile.REGTYPE),),
                 (("gitleaks", b"12345", tarfile.REGTYPE),)]
        for members in cases:
            with self.subTest(members=members), patch.object(installer, "MAX_BINARY", 4):
                archive, digest = self.archive(members)
                with self.assertRaises(ValueError):
                    installer.install_archive(archive, digest, self.root / "gitleaks")

    def test_existing_output_is_never_overwritten(self):
        archive, digest = self.archive()
        output = self.root / "gitleaks"
        output.write_text("preserved")
        with self.assertRaises(FileExistsError):
            installer.install_archive(archive, digest, output)
        self.assertEqual(output.read_text(), "preserved")

    def test_platform_aliases_and_unsupported_hosts(self):
        for system in ("Linux", "Darwin"):
            for machine in ("arm64", "aarch64", "x86_64", "amd64"):
                self.assertEqual(len(installer.selected_assets(system, machine)), 2)
        with self.assertRaises(KeyError):
            installer.selected_assets("Windows", "arm64")

    def test_install_requires_private_owned_empty_regular_directory(self):
        directory = self.root / "tools"
        directory.mkdir(mode=0o755)
        with self.assertRaises(ValueError):
            installer.install(directory)
        directory.chmod(0o700)
        (directory / "human-work").write_text("preserved")
        with self.assertRaises(ValueError):
            installer.install(directory)
        self.assertEqual((directory / "human-work").read_text(), "preserved")
        link = self.root / "link"
        link.symlink_to(directory, target_is_directory=True)
        with self.assertRaises(ValueError):
            installer.install(link)

    def test_download_failure_cleans_partial_install_and_private_downloads(self):
        directory = self.root / "tools"
        directory.mkdir(mode=0o700)
        archive, digest = self.archive()
        selected = [("gitleaks", "test", "upstream/project", "one.tar.gz", digest),
                    ("trufflehog", "test", "upstream/project", "two.tar.gz", digest)]

        def download(url, target):
            if "two.tar.gz" in url:
                raise subprocess.CalledProcessError(22, "curl")
            target.write_bytes(archive.read_bytes())

        with patch.object(installer, "selected_assets", return_value=selected):
            with self.assertRaises(subprocess.CalledProcessError):
                installer.install(directory, download)
        self.assertEqual(list(directory.iterdir()), [])

    def test_compressed_size_budget_is_enforced(self):
        archive, digest = self.archive()
        with patch.object(installer, "MAX_ARCHIVE", 1), self.assertRaises(ValueError):
            installer.install_archive(archive, digest, self.root / "gitleaks")


if __name__ == "__main__":
    unittest.main()
