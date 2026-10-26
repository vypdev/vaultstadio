// Requires Node.js and the frontend Gradle npm installation; run from repository root.
import assert from 'node:assert/strict';
import { once } from 'node:events';
import { createRequire } from 'node:module';
const require = createRequire(process.cwd() + '/frontend/build/wasm/package.json');
const { WebSocket, WebSocketServer } = require('ws');
const version = require('ws/package.json').version;
assert.equal(version, '8.21.3');
const deadline = setTimeout(() => { console.error('bounded websocket verification timed out'); process.exit(1); }, 8000);
async function fixture(exercise) {
  const server = new WebSocketServer({ host: '127.0.0.1', port: 0, maxPayload: 1024 });
  await once(server, 'listening');
  try { await exercise(server, 'ws://127.0.0.1:' + server.address().port); }
  finally { for (const client of server.clients) client.terminate(); await new Promise(resolve => server.close(resolve)); }
}
try {
  await fixture(async (server, url) => {
    server.on('connection', socket => socket.on('message', data => socket.send(data)));
    const client = new WebSocket(url);
    try {
      await once(client, 'open');
      const reply = once(client, 'message');
      client.send('Entrada', { fin: false });
      client.send(' autónoma ', { fin: false });
      client.send('ñ', { fin: true });
      assert.equal((await reply)[0].toString(), 'Entrada autónoma ñ');
    } finally { client.terminate(); }
  });
  await fixture(async (server, url) => {
    server.on('connection', socket => {
      assert.throws(() => socket.close(1000, new Float32Array(20)), TypeError);
      socket.terminate();
    });
    const client = new WebSocket(url);
    try {
      const [code, reason] = await once(client, 'close');
      assert.equal(code, 1006);
      assert.equal(reason.length, 0);
    } finally { client.terminate(); }
  });
  await fixture(async (server, url) => {
    server.on('connection', socket => socket.close(1000, new Uint8Array(80)));
    const client = new WebSocket(url);
    try {
      const [code, reason] = await once(client, 'close');
      assert.equal(code, 1000);
      assert.deepEqual(reason, Buffer.alloc(80));
    } finally { client.terminate(); }
  });
  console.log(JSON.stringify({ version, cases: 3, failures: 0, scope: 'real installed ws; bounded loopback fragmentation/Unicode and typed-array close reason' }));
} finally { clearTimeout(deadline); }
