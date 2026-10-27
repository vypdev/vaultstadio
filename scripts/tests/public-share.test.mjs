import assert from 'node:assert/strict';
import test from 'node:test';
import { informationRequest, mountSharePage, safeInformation, shareEndpoint } from
  '../../backend/api/src/main/resources/public-share/public-share.mjs';

const valid = { success: true, data: { name: '<img src=x onerror=alert(1)>', size: 17 } };
const reply = (status, body) => ({ ok: status === 200, status, json: async () => body });

function fixture(status = 'Retained POST error') {
  const elements = new Map();
  for (const id of ['share-form', 'password', 'status', 'download', 'load', 'details', 'filename', 'size']) {
    const handlers = new Map();
    elements.set(id, {
      value: '', textContent: id === 'status' ? status : '', disabled: id === 'download', hidden: true,
      addEventListener(name, handler) { handlers.set(name, handler); },
      fire(name, event = {}) { return handlers.get(name)?.(event); },
      set innerHTML(_) { throw Error('HTML interpolation must never be used'); },
    });
  }
  return { document: { getElementById: id => elements.get(id) }, get: id => elements.get(id) };
}

for (const path of ['/api/v1/share/ToKen_-/view', '/api/v1/share/ToKen_-/view/download']) {
  test(`S90-page preserves case in ${path}`, () => assert.equal(shareEndpoint(path), '/api/v1/share/ToKen_-'));
}
for (const path of ['/files', '/share/x', '/api/v1/share/x/view/extra', '/api/v1/share/a%2Fb/view',
  '/api/v1/share//view', `/api/v1/share/${'x'.repeat(65)}/view`]) {
  test(`S90-page rejects malformed location ${path}`, () => assert.equal(shareEndpoint(path), null));
}

test('S90-page credentials are header only, no identity cookies or cache', () => {
  assert.deepEqual(informationRequest('secret'), {
    credentials: 'omit', cache: 'no-store', redirect: 'error', headers: { 'X-Share-Password': 'c2VjcmV0' },
  });
  assert.deepEqual(informationRequest('').headers, {});
});

for (const body of [null, {}, { success: false, data: valid.data }, { success: true, data: null },
  { success: true, data: { name: 'file', size: -1 } }, { success: true, data: { name: 'file', size: 1.5 } },
  { success: true, data: { name: 12, size: 1 } }, { success: true, data: { name: 'file', size: Number.MAX_SAFE_INTEGER + 1 } }]) {
  test(`S90-page rejects unsafe metadata ${JSON.stringify(body)}`, () => assert.equal(safeInformation(body), null));
}

test('S90-page loads protected info, renders filename as text and streams with a native form', async () => {
  const f = fixture();
  const requests = [];
  f.get('password').value = 'secret';
  mountSharePage(f.document, { pathname: '/api/v1/share/ToKen/view' }, async (...args) => {
    requests.push(args); return reply(200, valid);
  });
  assert.equal(f.get('status').textContent, 'Retained POST error');
  assert.equal(requests.length, 0);
  await f.get('load').fire('click');
  assert.equal(requests[0][0], '/api/v1/share/ToKen');
  assert.equal(requests[0][1].headers['X-Share-Password'], 'c2VjcmV0');
  assert.equal(f.get('filename').textContent, valid.data.name);
  assert.equal(f.get('size').textContent, '17 bytes');
  assert.equal(f.get('details').hidden, false);
  assert.equal(f.get('download').disabled, false);
  assert.equal(f.get('share-form').action, '/api/v1/share/ToKen/view/download');
  f.get('share-form').fire('submit');
  assert.equal(f.get('download').disabled, true);
  assert.match(f.get('status').textContent, /cannot confirm/);
  assert.equal(requests.length, 1, 'Submitting must not fetch/buffer the file in JS');
});

for (const [status, code, text] of [
  [403, 'SHARE_PASSWORD_REQUIRED', /Enter the link password/],
  [403, 'SHARE_PASSWORD_INVALID', /Incorrect password/],
  [400, 'INVALID_SHARE_REQUEST', /password length/],
  [404, 'SHARE_NOT_FOUND', /not found/],
  [410, 'SHARE_EXPIRED', /no longer available/],
  [500, 'UNTRUSTED', /could not be loaded/],
]) {
  test(`S90-page denial ${code} is safe, retryable and retains local input`, async () => {
    const f = fixture();
    f.get('password').value = 'retained';
    mountSharePage(f.document, { pathname: '/api/v1/share/ToKen/view' }, async () => reply(status,
      { success: false, error: { code, message: '<script>untrusted-secret</script>' } }));
    await f.get('load').fire('click');
    assert.match(f.get('status').textContent, text);
    assert.doesNotMatch(f.get('status').textContent, /untrusted-secret/);
    assert.equal(f.get('password').value, 'retained');
    assert.equal(f.get('download').disabled, true);
    assert.equal(f.get('load').disabled, false);
    assert.equal(f.get('details').hidden, true);
  });
}

test('S90-page failed network recovers on explicit retry', async () => {
  const f = fixture();
  let fail = true;
  mountSharePage(f.document, { pathname: '/api/v1/share/ToKen/view' }, async () => {
    if (fail) throw Error('untrusted-provider-secret');
    return reply(200, valid);
  });
  await f.get('load').fire('click');
  assert.match(f.get('status').textContent, /Connection failed/);
  assert.equal(f.get('load').disabled, false);
  fail = false;
  await f.get('load').fire('click');
  assert.equal(f.get('download').disabled, false);
});

test('S90-page input change aborts in-flight metadata and cannot apply a stale success', async () => {
  const f = fixture();
  let resolve;
  let signal;
  mountSharePage(f.document, { pathname: '/api/v1/share/ToKen/view' }, async (_, options) => {
    signal = options.signal;
    return new Promise(done => { resolve = done; });
  });
  const pending = f.get('load').fire('click');
  f.get('password').value = 'changed';
  f.get('password').fire('input');
  assert.equal(signal.aborted, true);
  assert.equal(f.get('load').disabled, false);
  resolve(reply(200, valid));
  await pending;
  assert.equal(f.get('download').disabled, true);
  assert.equal(f.get('details').hidden, true);
  assert.match(f.get('status').textContent, /Password changed/);
});

test('S90-page disabled submission cannot attempt a download', () => {
  const f = fixture();
  let prevented = false;
  mountSharePage(f.document, { pathname: '/api/v1/share/ToKen/view' }, () => assert.fail('no request expected'));
  f.get('share-form').fire('submit', { preventDefault() { prevented = true; } });
  assert.equal(prevented, true);
});

test('S90-page invalid route fails without any network access', () => {
  const f = fixture();
  mountSharePage(f.document, { pathname: '/api/v1/share/invalid%2Ftoken/view' }, () => assert.fail('no request expected'));
  assert.match(f.get('status').textContent, /invalid/);
  assert.equal(f.get('load').disabled, true);
});

test('S90-page times out pending metadata and restores retry', async t => {
  t.mock.timers.enable({ apis: ['setTimeout'] });
  const f = fixture();
  mountSharePage(f.document, { pathname: '/api/v1/share/ToKen/view' }, (_, options) => new Promise((_, reject) => {
    options.signal.addEventListener('abort', () => reject(Error('timeout')));
  }));
  const pending = f.get('load').fire('click');
  t.mock.timers.tick(15000);
  await pending;
  assert.match(f.get('status').textContent, /Connection failed/);
  assert.equal(f.get('download').disabled, true);
  assert.equal(f.get('load').disabled, false);
});

test('S90-page fresh GET automatically loads information without an account', async () => {
  const f = fixture('');
  let requested = 0;
  mountSharePage(f.document, { pathname: '/api/v1/share/ToKen/view' }, async () => {
    requested++; return reply(200, valid);
  });
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(requested, 1);
  assert.equal(f.get('download').disabled, false);
});

for (const password of ['☃', 'é'.repeat(36), ' +%\t', '😀']) {
  test(`S90-Unicode-transport ${JSON.stringify(password)}`, () => {
    const request = informationRequest(password);
    assert.doesNotThrow(() => new Headers(request.headers));
    assert.equal(Buffer.from(request.headers['X-Share-Password'], 'base64url').toString('utf8'), password);
  });
}
