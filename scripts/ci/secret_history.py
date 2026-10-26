"""Inventory reachable blobs that ordinary non-merge patch scanners cannot observe."""

import re
import subprocess
from pathlib import Path

from secret_process import environment

MAX_OBJECTS = 100_000
MAX_BLOB = 64 * 1024 * 1024
MAX_TOTAL = 512 * 1024 * 1024


def git(repository, private, *arguments, input_data=None):
    result = subprocess.run(
        ["git", "-C", str(repository), *arguments], input=input_data,
        capture_output=True, env=environment(private), timeout=60, check=True,
    )
    return result.stdout


def changed_blobs(raw):
    records = raw.split(b"\x00")
    blobs = set()
    index = 0
    while index < len(records):
        metadata = records[index].strip(b"\n")
        if not metadata:
            index += 1
            continue
        match = re.fullmatch(rb":[0-7]{6} [0-7]{6} ([a-f0-9]{40}) ([a-f0-9]{40}) [A-Z]", metadata)
        if not match or index + 1 >= len(records):
            raise ValueError("invalid-diff-inventory")
        if match[1] != match[2] and match[2] != b"0" * 40:
            blobs.add(match[2])
        index += 2  # Repository paths remain data, including metadata-shaped names.
    return blobs


def prepare_merge_blobs(repository, head, private):
    private.mkdir(mode=0o700)
    objects = git(repository, private, "rev-list", "--objects", "--no-object-names", head).splitlines()
    if not 0 < len(objects) <= MAX_OBJECTS or any(not re.fullmatch(rb"[a-f0-9]{40}", key) for key in objects):
        raise ValueError("history-object-inventory")
    descriptions = git(repository, private, "cat-file", "--batch-check=%(objectname) %(objecttype) %(objectsize)",
                       input_data=b"\n".join(objects) + b"\n").splitlines()
    if len(descriptions) != len(objects):
        raise ValueError("incomplete-object-inventory")
    blobs = {}
    for entry in descriptions:
        key, kind, size = entry.split()
        if kind == b"blob":
            blobs[key] = int(size)
    pending = set()
    merges = git(repository, private, "rev-list", "--merges", "--parents", head).splitlines()
    for record in merges:
        revision, *parents = record.decode().split()
        if len(parents) < 2 or any(not re.fullmatch(r"[a-f0-9]{40}", key) for key in (revision, *parents)):
            raise ValueError("invalid-merge-inventory")
        introduced = changed_blobs(git(repository, private, "diff-tree", "-r", "--raw", "-z",
                                       "--no-abbrev", "--no-renames", parents[0], revision))
        # Only ancestors count as already scanned. A later rename/mode change
        # must not hide a blob originally introduced solely during this merge.
        ordinary = changed_blobs(git(repository, private, "log", "--raw", "-z", "--no-abbrev",
                                     "--no-renames", "--no-merges", "--root", "--format=", *parents))
        pending.update((introduced - ordinary) & set(blobs))
    pending = sorted(pending)
    total = sum(blobs[key] for key in pending)
    if total > MAX_TOTAL or any(blobs[key] > MAX_BLOB for key in pending):
        raise ValueError("merge-blob-budget")
    for key in pending:
        content = git(repository, private, "cat-file", "blob", key.decode())
        if len(content) != blobs[key]:
            raise ValueError("incomplete-merge-blob")
        # Content is an ordinary file named only by its validated object ID.
        # No repository path, symlink, filter, hook or archive is executed.
        target = private / (key.decode() + ".blob")
        with target.open("xb") as output:
            output.write(content)
        target.chmod(0o600)
    return {"reachableObjects": len(objects), "reachableBlobs": len(blobs), "mergeCommits": len(merges),
            "mergeOnlyBlobs": len(pending), "mergeOnlyBytes": total}
