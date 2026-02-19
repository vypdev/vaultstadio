/**
 * SPA fallback: serve index.html for all routes so client-side routing works
 * (e.g. /files, /settings/change-password open the app instead of 404).
 * See https://webpack.js.org/configuration/dev-server/#devserverhistoryapifallback
 */
config.devServer = config.devServer || {};
config.devServer.historyApiFallback = true;
