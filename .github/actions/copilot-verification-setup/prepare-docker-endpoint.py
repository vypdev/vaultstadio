#!/usr/bin/env python3
"""Resolve an operator-provided Unix Docker socket without reading user credentials."""
import os
from pathlib import Path
import pwd
import re
import shutil
import stat
import subprocess
import tempfile


class EndpointUnavailable(RuntimeError):
    pass


def endpoint_path(value):
    if (not value.startswith('unix:///') or any(ord(c) < 32 or ord(c) == 127 for c in value)):
        raise EndpointUnavailable('Docker requires an existing local Unix endpoint')
    return Path(value.removeprefix('unix://'))


def resolve_endpoint(environment, account_home=None):
    binary = shutil.which('docker', path=environment.get('PATH', os.defpath))
    if not binary:
        raise EndpointUnavailable('Docker CLI is an operator prerequisite on this runner')
    supplied = environment.get('DOCKER_HOST')
    if supplied:
        candidates = [endpoint_path(supplied)]
    else:
        home = Path(account_home) if account_home else Path(pwd.getpwuid(os.getuid()).pw_dir)
        if not home.is_absolute():
            raise EndpointUnavailable('The operator account home must be absolute')
        candidates = [Path('/var/run/docker.sock'), home / '.docker/run/docker.sock', home / '.orbstack/run/docker.sock']
    clean = {key: environment[key] for key in ['PATH', 'HOME', 'USER', 'LANG', 'TMPDIR'] if key in environment}
    with tempfile.TemporaryDirectory(prefix='vaultstadio-docker-endpoint-') as raw:
        for candidate in candidates:
            try:
                if not stat.S_ISSOCK(candidate.stat().st_mode):
                    continue
            except OSError:
                continue
            endpoint = 'unix://' + str(candidate)
            endpoint_path(endpoint)
            private = dict(clean, DOCKER_CONFIG=raw, DOCKER_CONTEXT='', DOCKER_HOST=endpoint)
            try:
                result = subprocess.run([binary, '--host', endpoint, 'info', '--format', '{{.ServerVersion}}'],
                                        env=private, capture_output=True, text=True, timeout=15, check=False)
            except (OSError, subprocess.TimeoutExpired):
                continue
            if result.returncode == 0 and re.fullmatch(r'\d+\.\d+\.\d+(?:[-+][A-Za-z0-9._-]+)?\s*', result.stdout):
                return endpoint
    raise EndpointUnavailable('No accessible existing local Docker endpoint was verified')


def main(environment=None):
    environment = dict(os.environ) if environment is None else environment
    output = Path(environment.get('GITHUB_ENV', ''))
    try:
        metadata = output.lstat()
    except OSError:
        raise EndpointUnavailable('The private runtime environment output channel is required') from None
    if (not output.is_absolute() or not stat.S_ISREG(metadata.st_mode) or output.is_symlink()
            or metadata.st_nlink != 1 or metadata.st_uid != os.getuid()):
        raise EndpointUnavailable('The runtime environment output channel must be a regular owned file')
    endpoint = resolve_endpoint(environment)
    try:
        descriptor = os.open(output, os.O_WRONLY | os.O_APPEND | os.O_NOFOLLOW)
    except OSError:
        raise EndpointUnavailable('The runtime environment output channel changed') from None
    try:
        current = os.fstat(descriptor)
        if (current.st_dev, current.st_ino, current.st_nlink, current.st_uid) != (metadata.st_dev, metadata.st_ino, 1, os.getuid()):
            raise EndpointUnavailable('The runtime environment output channel changed')
        os.write(descriptor, f'DOCKER_HOST={endpoint}\nDOCKER_CONTEXT=\n'.encode())
    finally:
        os.close(descriptor)
    print('Verified an existing local Docker endpoint with empty private client credentials.')


if __name__ == '__main__':
    try:
        main()
    except EndpointUnavailable as error:
        print(f'Project verification setup: {error}')
        raise SystemExit(78)
