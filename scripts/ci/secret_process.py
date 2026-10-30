"""Bounded credential-free subprocesses; detector output never reaches the terminal."""

import os
import signal
import subprocess
import tempfile


def environment(private):
    return {
        "PATH": os.defpath, "HOME": str(private), "TMPDIR": str(private), "LANG": "C",
        "GIT_CONFIG_NOSYSTEM": "1", "GIT_CONFIG_GLOBAL": os.devnull,
        "GIT_TERMINAL_PROMPT": "0", "GIT_NO_REPLACE_OBJECTS": "1",
    }


def execute(command, private, cwd=None, timeout=600):
    with tempfile.TemporaryFile(dir=private) as output, tempfile.TemporaryFile(dir=private) as errors:
        try:
            process = subprocess.Popen(
                command, cwd=cwd, env=environment(private), stdin=subprocess.DEVNULL,
                stdout=output, stderr=errors, start_new_session=True, umask=0o077,
            )
        except OSError:
            return 127, b"", b""
        try:
            code = process.wait(timeout=timeout)
        except subprocess.TimeoutExpired:
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()
            return 124, b"", b""
        output.seek(0)
        errors.seek(0)
        # Version output is tiny; real reports use the same bounded parse budget.
        maximum = 16 * 1024 * 1024
        data, diagnostics = output.read(maximum + 1), errors.read(maximum + 1)
        if len(data) > maximum or len(diagnostics) > maximum:
            return 125, b"", b""
        return code, data, diagnostics
