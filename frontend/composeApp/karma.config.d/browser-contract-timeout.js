// Coroutine UI contracts retain 45s overall and 10s per-state deadlines.
// Mocha's default 2s would abort them before those bounded assertions finish.
config.client = config.client || {};
config.client.mocha = Object.assign({}, config.client.mocha, { timeout: 60000 });
