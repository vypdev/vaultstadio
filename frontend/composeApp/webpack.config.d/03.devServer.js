/**
 * SPA fallback: serve index.html for all routes so client-side routing works
 * (e.g. /files, /settings/change-password open the app instead of 404).
 * See https://webpack.js.org/configuration/dev-server/#devserverhistoryapifallback
 */
config.devServer = config.devServer || {};
config.devServer.historyApiFallback = true;
// Same-origin defaults also work against the normal standalone local backend.
config.devServer.proxy = [{ context: ['/api'], target: 'http://127.0.0.1:8080', changeOrigin: false }];
