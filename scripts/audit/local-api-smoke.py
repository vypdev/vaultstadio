"""Exercise a disposable localhost VaultStadio instance; retain no tokens in results."""
import argparse
import re
import sys
import base64
import hashlib
import json
import os
from pathlib import Path
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--base-url', default='http://127.0.0.1:8080')
parser.add_argument('--output-dir', required=True, type=Path)
args = parser.parse_args()
url = urllib.parse.urlsplit(args.base_url)
if url.hostname not in {'localhost', '127.0.0.1', '::1'} or url.scheme not in {'http', 'https'} or url.username or url.password or url.query or url.fragment or url.path not in {'', '/'}:
    parser.error('Only an explicit loopback HTTP(S) origin is allowed.')
BASE = args.base_url.rstrip('/')
ROOT = args.output_dir.resolve()
repository = Path(__file__).resolve().parents[2]
if ROOT == repository or repository in ROOT.parents:
    parser.error('Keep test credentials outside the repository; use a new temporary output directory.')
ROOT.mkdir(parents=True, exist_ok=False, mode=0o700)
(ROOT / 'runtime').mkdir(mode=0o700)
(ROOT / 'evidence').mkdir(mode=0o700)
results = []
tokens = {}
users = {}
run = uuid.uuid4().hex[:10]
password = 'Audit-local-71-' + run + '-A9'
content = ('VaultStadio isolated audit ' + run + '\n').encode()

class RejectRedirects(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, request, response, code, message, headers, new_url):
        raise urllib.error.HTTPError(
            request.full_url, code, 'Redirects are disabled for isolated audit requests', headers, response,
        )

# Keep fixture credentials on the explicitly selected loopback origin, regardless
# of shell proxy variables or redirects returned by the target.
opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), RejectRedirects())

def http(method, path, actor=None, body=None, raw=None, headers=None):
    h = dict(headers or {})
    if actor:
        h['Authorization'] = 'Bearer ' + tokens[actor]
    if body is not None:
        raw = json.dumps(body).encode()
        h['Content-Type'] = 'application/json'
    request = urllib.request.Request(BASE + path, data=raw, headers=h, method=method)
    try:
        response = opener.open(request, timeout=30)
    except urllib.error.HTTPError as error:
        response = error
    data = response.read()
    try:
        parsed = json.loads(data)
    except (ValueError, UnicodeDecodeError):
        parsed = None
    return response.status, parsed, data, dict(response.headers)

def check(name, method, path, actor=None, body=None, expected=(200,), predicate=None, raw=None, headers=None):
    started = time.monotonic()
    try:
        status, payload, data, response_headers = http(method, path, actor, body, raw, headers)
        ok = status in expected and (predicate(payload, data, response_headers) if predicate else True)
        error = payload.get('error') if isinstance(payload, dict) else None
        row = {'name': name, 'method': method, 'path': re.sub(r'/share/[^/]+', '/share/[redacted]', path.split('?')[0]),
               'status': status, 'expected': list(expected), 'passed': bool(ok),
               'response_bytes': len(data), 'error': error,
               'elapsed_ms': round(1000 * (time.monotonic() - started), 1)}
    except Exception as error:
        status, payload, data, response_headers = 0, None, b'', {}
        row = {'name': name, 'method': method, 'path': re.sub(r'/share/[^/]+', '/share/[redacted]', path.split('?')[0]),
               'passed': False, 'exception_type': type(error).__name__, 'error': str(error)}
    results.append(row)
    print(json.dumps(row), flush=True)
    return payload, status, data

def data_of(value):
    return value.get('data') if isinstance(value, dict) else None

def multipart(name, data):
    boundary = 'VaultAudit' + uuid.uuid4().hex
    body = (f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{name}"\r\n'
            'Content-Type: text/plain\r\n\r\n').encode() + data + f'\r\n--{boundary}--\r\n'.encode()
    return body, {'Content-Type': 'multipart/form-data; boundary=' + boundary}

def upload(name, actor='a', parent=None):
    raw, headers = multipart(name, content)
    path = '/api/v1/storage/upload' + ('?parentId=' + parent if parent else '')
    payload, _, _ = check('upload ' + name, 'POST', path, actor, raw=raw, headers=headers, expected=(201,))
    return (data_of(payload) or {}).get('id')

try:
    check('liveness', 'GET', '/health')
    check('readiness database and storage', 'GET', '/ready', predicate=lambda p, *_: p.get('ready') is True)
    check('anonymous folder denied', 'GET', '/api/v1/storage/folder', expected=(401,))
    check('invalid bearer denied', 'GET', '/api/v1/storage/folder', headers={'Authorization': 'Bearer invalid-audit-token'}, expected=(401,))
    check('invalid registration email rejected', 'POST', '/api/v1/auth/register', body={'email': 'invalid', 'username': 'invalid', 'password': password}, expected=(400,422))
    check('weak password rejected', 'POST', '/api/v1/auth/register', body={'email': f'weak-{run}@audit.invalid', 'username': f'weak-{run}', 'password': 'short'}, expected=(400,422))
    for actor in ('a', 'b', 'ui'):
        email = f'{actor}-{run}@audit.invalid'
        body = {'email': email, 'username': f'audit-{actor}-{run}', 'password': password}
        payload, _, _ = check('register ' + actor, 'POST', '/api/v1/auth/register', body=body, expected=(201,))
        users[actor] = data_of(payload) or {}
        users[actor]['audit_email'] = email
        login, _, _ = check('login ' + actor, 'POST', '/api/v1/auth/login', body={'email': email, 'password': password})
        credentials = data_of(login) or {}
        if credentials.get('token'):
            tokens[actor] = credentials['token']
            users[actor]['refresh'] = credentials.get('refreshToken')
    private = ROOT / 'runtime' / 'dogfood-session.json'
    private.write_text(json.dumps({'run': run, 'password': password, 'tokens': tokens, 'users': users}, indent=2))
    private.chmod(0o600)
    if 'a' not in tokens or 'b' not in tokens:
        raise RuntimeError('Required disposable accounts could not log in')
    check('duplicate registration rejected', 'POST', '/api/v1/auth/register', body={'email': users['a']['audit_email'], 'username': f'duplicate-{run}', 'password': password}, expected=(400,409,422))
    check('bad login rejected', 'POST', '/api/v1/auth/login', body={'email': users['a']['audit_email'], 'password': 'Wrong-local-password-123'}, expected=(401,))
    check('current user', 'GET', '/api/v1/user/me', 'a', predicate=lambda p, *_: data_of(p)['id'] == users['a']['id'])
    check('auth alias current user', 'GET', '/api/v1/auth/me', 'a')
    check('quota', 'GET', '/api/v1/user/me/quota', 'a')
    check('non-admin users denied', 'GET', '/api/v1/admin/users', 'a', expected=(403,))
    check('non-admin statistics denied', 'GET', '/api/v1/admin/statistics', 'a', expected=(403,))
    folder, _, _ = check('create folder', 'POST', '/api/v1/storage/folder', 'a', {'name': 'audit-' + run}, expected=(201,))
    folder_id = (data_of(folder) or {}).get('id')
    check('path traversal folder rejected', 'POST', '/api/v1/storage/folder', 'a', {'name': '../audit-escape-' + run}, expected=(400,422))
    check('empty folder name rejected', 'POST', '/api/v1/storage/folder', 'a', {'name': ''}, expected=(400,422))
    file_id = upload('audit-' + run + '.txt', parent=folder_id)
    if file_id:
        path = '/api/v1/storage/item/' + file_id
        check('get uploaded item', 'GET', path, 'a')
        check('download byte identity', 'GET', '/api/v1/storage/download/' + file_id, 'a', predicate=lambda _, b, __: b == content)
        check('cross-user item denied', 'GET', path, 'b', expected=(403,404))
        check('cross-user download denied', 'GET', '/api/v1/storage/download/' + file_id, 'b', expected=(403,404))
        check('cross-user rename denied', 'PATCH', path + '/rename', 'b', {'name': 'forbidden.txt'}, expected=(403,404))
        check('cross-user folder upload denied', 'POST', '/api/v1/storage/folder', 'b', {'name':'forbidden-child','parentId':folder_id}, expected=(403,404))
        check('breadcrumbs', 'GET', path + '/breadcrumbs', 'a')
        check('rename file', 'PATCH', path + '/rename', 'a', {'name':'renamed-' + run + '.txt'})
        check('move file root', 'POST', path + '/move', 'a', {'destinationId':None})
        check('copy file', 'POST', path + '/copy', 'a', {'destinationId':folder_id, 'newName':'copied-' + run + '.txt'}, expected=(200,201))
        check('star file', 'POST', path + '/star', 'a')
        check('list starred', 'GET', '/api/v1/storage/starred', 'a')
        check('list recent', 'GET', '/api/v1/storage/recent', 'a')
        share, _, _ = check('create public share', 'POST', '/api/v1/shares', 'a', {'itemId':file_id,'expirationDays':1}, expected=(201,))
        share_data = data_of(share) or {}
        if share_data.get('token'):
            share_path='/api/v1/share/' + share_data['token']
            check('public share info', 'GET', share_path)
            check('public share download byte identity', 'GET', share_path + '/download', predicate=lambda _, b, __: b == content)
            check('cross-user revoke denied', 'DELETE', '/api/v1/shares/' + share_data['id'], 'b', expected=(403,404))
            check('owner revoke share', 'DELETE', '/api/v1/shares/' + share_data['id'], 'a')
            check('revoked share denied', 'GET', share_path, expected=(403,404,410))
        protected, _, _ = check('create password share', 'POST', '/api/v1/shares', 'a', {'itemId':file_id,'expirationDays':1,'password':'Audit-share-only-123'}, expected=(201,))
        protected = data_of(protected) or {}
        if protected.get('token'):
            pp='/api/v1/share/' + protected['token']
            check('password share missing password denied', 'GET', pp, expected=(401,403))
            check('password share wrong password denied', 'GET', pp+'?password=wrong', expected=(401,403))
            check('password share correct password', 'GET', pp+'?password=Audit-share-only-123')
        check('version list', 'GET', '/api/v1/versions/item/' + file_id, 'a')
        check('cross-user version list denied', 'GET', '/api/v1/versions/item/' + file_id, 'b', expected=(403,404))
        collab, _, _ = check('join collaboration owner', 'POST', '/api/v1/collaboration/sessions/join', 'a', {'itemId':file_id}, expected=(201,))
        check('cross-user collaboration join denied', 'POST', '/api/v1/collaboration/sessions/join', 'b', {'itemId':file_id}, expected=(403,404))
        check('cross-user document denied', 'GET', '/api/v1/collaboration/documents/'+file_id, 'b', expected=(403,404))
        check('trash file', 'DELETE', path, 'a')
        check('trash listing contains file', 'GET', '/api/v1/storage/trash', 'a', predicate=lambda p,*_: file_id in json.dumps(data_of(p)))
        check('restore file', 'POST', path + '/restore', 'a')
        check('restored bytes unchanged', 'GET', '/api/v1/storage/download/' + file_id, 'a', predicate=lambda _, b, __: b == content)
    check('list root', 'GET', '/api/v1/storage/folder', 'a')
    check('invalid sort rejected as client error', 'GET', '/api/v1/storage/folder?sortBy=invalid', 'a', expected=(400,422))
    check('negative page limit rejected', 'GET', '/api/v1/storage/folder?limit=-1', 'a', expected=(400,422))
    check('register sync device', 'POST', '/api/v1/sync/devices', 'a', {'deviceId':'audit-device-'+run,'deviceName':'Audit Mac','deviceType':'DESKTOP'}, expected=(200,201))
    check('list sync devices', 'GET', '/api/v1/sync/devices', 'a')
    check('sync pull', 'POST', '/api/v1/sync/pull', 'a', {}, headers={'X-Device-ID':'audit-device-'+run})
    check('pending sync conflicts', 'GET', '/api/v1/sync/conflicts', 'a')
    check('search own file', 'GET', '/api/v1/search?q=renamed', 'a')
    check('activity feed', 'GET', '/api/v1/activity', 'a')
    check('plugins list', 'GET', '/api/v1/plugins', 'a')
    check('AI providers non-admin denied (no external calls)', 'GET', '/api/v1/ai/providers', 'a', expected=(403,))
    check('federation liveness', 'GET', '/api/v1/federation/health')
    check('S3 absent under LOCAL', 'GET', '/s3', 'a', expected=(404,))
    basic = base64.b64encode((users['a']['audit_email']+':'+password).encode()).decode()
    check('WebDAV PROPFIND owner', 'PROPFIND', '/webdav/', headers={'Authorization':'Basic '+basic,'Depth':'1'}, expected=(207,))
    # Completing before all chunks arrive must preserve an explicit incomplete state.
    chunk, _, _ = check('chunk session init', 'POST', '/api/v1/storage/upload/init', 'a', {'fileName':'chunk-'+run+'.txt','totalSize':len(content),'chunkSize':len(content),'mimeType':'text/plain'}, expected=(201,))
    upload_id=(data_of(chunk) or {}).get('uploadId')
    if upload_id:
        up='/api/v1/storage/upload/'+upload_id
        check('incomplete chunk completion rejected', 'POST', up+'/complete', 'a', expected=(400,409,422))
        check('cross-user chunk status denied', 'GET', up+'/status', 'b', expected=(403,404))
        raw,headers=multipart('chunk.txt',content)
        check('chunk upload', 'POST', up+'/chunk/0', 'a', raw=raw,headers=headers)
        completed,_,_=check('chunk complete', 'POST',up+'/complete','a',expected=(201,))
        item=(data_of(completed) or {}).get('id')
        if item:check('chunk assembled byte identity','GET','/api/v1/storage/download/'+item,'a',predicate=lambda _,b,__: b==content)
        check('duplicate chunk completion idempotent', 'POST',up+'/complete','a',expected=(200,201))
    refresh=users['a'].get('refresh')
    if refresh:
        rotated,_,_=check('refresh rotates token','POST','/api/v1/auth/refresh',body={'refreshToken':refresh})
        check('old refresh token rejected','POST','/api/v1/auth/refresh',body={'refreshToken':refresh},expected=(401,))
        check('old access token rejected after rotation','GET','/api/v1/user/me','a',expected=(401,))
        if (data_of(rotated) or {}).get('token'):tokens['a']=data_of(rotated)['token']
    check('logout','POST','/api/v1/auth/logout','a')
    check('logged out token denied','GET','/api/v1/user/me','a',expected=(401,))
finally:
    output={'run':run,'base_url':BASE,'fixture_sha256':hashlib.sha256(content).hexdigest(),
            'total':len(results),'passed':sum(r['passed'] for r in results),
            'failed':sum(not r['passed'] for r in results),'results':results}
    (ROOT/'evidence'/'api-dogfood-results.json').write_text(json.dumps(output,indent=2)+'\n')
    print(json.dumps({k:v for k,v in output.items() if k!='results'}),flush=True)

sys.exit(1 if output['failed'] else 0)
