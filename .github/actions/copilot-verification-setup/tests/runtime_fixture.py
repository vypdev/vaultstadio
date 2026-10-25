"""Isolated executable fixtures, not evidence of real Java/Chrome/Docker readiness."""
import json
from pathlib import Path
import shutil
import socket
import subprocess
import sys
import tempfile

ACTION = Path(__file__).resolve().parents[1]
SCRIPT = ACTION / "prepare-environment.sh"


class RuntimeFixture:
    def __init__(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="vfy79-", dir="/tmp")
        self.root = Path(self.temporary.name).resolve()
        self.home = self.root / "profile"
        self.home.mkdir(mode=0o700)
        self.bin = self.root / "commands"
        self.bin.mkdir()
        for tool in ("bash", "mkdir", "mktemp", "chmod", "touch"):
            (self.bin / tool).symlink_to(shutil.which(tool))
        self.java = self.root / "java 17"
        self.executable(self.java / "bin/java", """#!/bin/bash
printf 'openjdk version "%s"\\n' "${_VF_JAVA_VERSION:-17.0.0}"
exit "${_VF_JAVA_EXIT:-0}"
""")
        self.sdk = self.root / "sdk ' $(literal)"
        self.jar = self.sdk / "platforms/android-36/android.jar"
        self.jar.parent.mkdir(parents=True)
        self.jar.write_bytes(b"fixture; never used to compile application code")
        self.chrome = self.root / "chrome ' $(literal)"
        self.executable(self.chrome, "#!/bin/bash\nexit 0\n")
        self.executable(self.bin / "docker", f"#!{sys.executable}\n" + """
import os,sys
sys.exit(int(os.environ.get('_VF_DOCKER_EXIT','0')))
""")
        self.executable(self.bin / "make", f"#!{sys.executable}\n" + """
import json,os,sys
from pathlib import Path
keys = ['HOME','ANDROID_HOME','ANDROID_SDK_ROOT','CHROME_BIN','DOCKER_HOST',
        'DOCKER_CONFIG','DOCKER_CONTEXT','GRADLE_USER_HOME',
        'DOCKER_TLS_VERIFY','DOCKER_CERT_PATH','DOCKER_AUTH_CONFIG']
Path(os.environ['_VF_CAPTURE']).write_text(json.dumps({
    'arguments':sys.argv[1:], 'environment':{k:os.environ.get(k) for k in keys}}))
print(os.environ.get('_VF_STDERR',''),file=sys.stderr)
sys.exit(int(os.environ.get('_VF_MAKE_EXIT','0')))
""")
        self.socket_path = self.root / "d.sock"
        self.socket = socket.socket(socket.AF_UNIX)
        try:
            self.socket.bind(str(self.socket_path))
        except OSError:
            self.socket.close()
            self.temporary.cleanup()
            raise
        self.output = self.root / "github-path"
        self.output.write_text("")
        self.capture = self.root / "capture.json"
        self.environment = {
            "HOME": str(self.home), "PATH": str(self.bin),
            "JAVA_HOME": str(self.java), "ANDROID_HOME": str(self.sdk),
            "CHROME_BIN": str(self.chrome), "GITHUB_PATH": str(self.output),
            "DOCKER_HOST": "unix://" + str(self.socket_path),
            "_VF_CAPTURE": str(self.capture),
        }

    @staticmethod
    def executable(path, source):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(source)
        path.chmod(0o700)

    def prepare(self):
        return subprocess.run(["/bin/bash", str(SCRIPT)], cwd=self.root,
                              env=self.environment, capture_output=True, text=True, timeout=10)

    def run_make(self, *arguments):
        directory = Path(self.output.read_text().splitlines()[-1])
        return subprocess.run([str(directory / "make"), *arguments], cwd=self.root,
                              env=self.environment, capture_output=True, text=True, timeout=10)

    def captured(self):
        return json.loads(self.capture.read_text())

    def close(self):
        self.socket.close()
        self.temporary.cleanup()
