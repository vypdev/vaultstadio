// Full 32 MiB byte-preservation cases are resource-boundary contracts, not 2s benchmarks.
// Keep a finite outer budget; coroutine and production transport deadlines stay unchanged.
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 60000 });
