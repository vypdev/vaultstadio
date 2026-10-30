#!/usr/bin/env python3
"""Collect bounded, secret-safe prerequisite facts; never install or certify setup."""
import argparse
import json
import os
from pathlib import Path
import platform
import re
import shutil
import stat
import subprocess

SANDBOX = Path('/opt/vaultstadio-verification-sandbox/chrome-sandbox')


def command(arguments, environment, timeout=20):
    try:
        result = subprocess.run(arguments, env=environment, capture_output=True, text=True,
                                timeout=timeout, check=False)
        return result.returncode, (result.stdout + result.stderr)[:8192]
    except (OSError, subprocess.TimeoutExpired):
        return None, ''


def version(binary, arguments, pattern, environment):
    if not binary:
        return {'available': False, 'version': None}
    code, text = command([binary, *arguments], environment)
    found = re.search(pattern, text) if code == 0 else None
    return {'available': found is not None, 'version': found.group(1) if found else None}


def docker_status(binary, environment):
    if not binary:
        return {'cli': False, 'localEndpoint': None, 'daemon': None, 'version': None}
    endpoint = environment.get('DOCKER_HOST', '')
    if not endpoint:
        code, text = command([binary, 'context', 'inspect', '--format', '{{.Endpoints.docker.Host}}'], environment)
        endpoint = text.strip() if code == 0 else ''
    local = bool(re.fullmatch(r'unix:///[^\r\n\x00]+', endpoint))
    if not local:
        return {'cli': True, 'localEndpoint': False, 'daemon': False, 'version': None}
    # No registry operation, credential helper or default client credentials are used.
    import tempfile
    with tempfile.TemporaryDirectory(prefix='verification-docker-inventory-') as raw:
        private = dict(environment, DOCKER_HOST=endpoint, DOCKER_CONTEXT='', DOCKER_CONFIG=raw)
        code, text = command([binary, 'info', '--format', '{{.ServerVersion}}'], private)
    found = re.fullmatch(r'\d+\.\d+\.\d+(?:[-+][A-Za-z0-9._-]+)?', text.strip()) if code == 0 else None
    return {'cli': True, 'localEndpoint': True, 'daemon': found is not None,
            'version': found.group(0) if found else None}


def sandbox_status(path=SANDBOX):
    try:
        item = path.lstat()
        parent = path.parent.lstat()
    except OSError:
        return {'present': False, 'protected': False}
    protected = (stat.S_ISREG(item.st_mode) and item.st_uid == 0 and item.st_gid == 0
                 and item.st_mode & stat.S_ISUID != 0 and item.st_mode & 0o111 != 0
                 and item.st_mode & 0o022 == 0 and stat.S_ISDIR(parent.st_mode)
                 and parent.st_uid == 0 and parent.st_mode & 0o022 == 0)
    return {'present': True, 'protected': protected}



def socket_inventory():
    candidates = [Path('/var/run/docker.sock'), Path(f'/run/user/{os.getuid()}/docker.sock')]
    for candidate in candidates:
        try:
            if stat.S_ISSOCK(candidate.lstat().st_mode):
                return {'present': True, 'accessible': os.access(candidate, os.R_OK | os.W_OK)}
        except OSError:
            continue
    return {'present': False, 'accessible': False}


def inventory(environment):
    # Only non-secret configuration used by read-only local tools is forwarded.
    allowed = ['PATH', 'HOME', 'USER', 'LANG', 'TMPDIR', 'JAVA_HOME', 'ANDROID_HOME',
               'ANDROID_SDK_ROOT', 'CHROME_BIN', 'DOCKER_HOST', 'DOCKER_CONTEXT']
    clean = {key: environment[key] for key in allowed if key in environment}
    path = clean.get('PATH', os.defpath)
    java = shutil.which('java', path=path)
    chrome = clean.get('CHROME_BIN') or next((p for name in ['chrome', 'google-chrome', 'chromium', 'chromium-browser']
                                            if (p := shutil.which(name, path=path))), None)
    java_info = version(java, ['-version'], r'version "(\d+(?:\.\d+){0,3}(?:[+_-][A-Za-z0-9.]+)?)"', clean)
    browser_info = version(chrome, ['--version'], r'^(?:Google Chrome(?: for Testing)?|Chromium) (\d+(?:\.\d+){3})\s*$', clean)
    sdk = clean.get('ANDROID_HOME') or clean.get('ANDROID_SDK_ROOT')
    try:
        jar = Path(sdk) / 'platforms/android-37.0/android.jar' if sdk else None
        android = bool(jar and jar.is_file() and jar.stat().st_size > 0)
    except OSError:
        android = False
    docker = docker_status(shutil.which('docker', path=path), clean)
    sandbox = sandbox_status()
    requirements = {'java17': java_info['version'] is not None and java_info['version'].split('.')[0] == '17',
                    'android37': android, 'chrome155': browser_info['version'] == '155.0.8059.84',
                    'localDocker': docker['daemon'],
                    'linuxSandbox': platform.system() != 'Linux' or sandbox['protected']}
    return {'inventoryOnly': True, 'projectSetupExecuted': False, 'readinessCertified': False,
            'system': platform.system(), 'architecture': platform.machine(),
            'runner': environment.get('RUNNER_NAME'), 'effectiveUid': os.geteuid(),
            'containerMarkerPresent': Path('/.dockerenv').exists(),
            'standardDockerSocket': socket_inventory(), 'java': java_info, 'browser': browser_info,
            'android37Present': android, 'docker': docker, 'sandbox': sandbox,
            'missing': [name for name, present in requirements.items() if not present]}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--report', required=True, type=Path)
    args = parser.parse_args()
    result = inventory(dict(os.environ))
    result['source'] = subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip()
    args.report.write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
