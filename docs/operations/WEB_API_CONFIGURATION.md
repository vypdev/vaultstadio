# Web API configuration

The operator chooses one principal API endpoint for a Web installation. Users sign in; they do not select or change the server. Additional managed nodes and independent installations remain roadmap work. This configuration does not implement federation.

## Default installation

Serve the application at the root of its Web origin and expose the backend through `/api` on that origin. The production package ships `/vaultstadio-config.json` containing `{}`. For a page at `https://vault.example/files`, the API root is `https://vault.example/api`, and versioned requests use `/api/v1`.

`docker/nginx.conf` proxies `/api` to the backend. The development server also proxies `/api` to `http://127.0.0.1:8080`; run the backend separately. Neither route requires an endpoint-specific Web rebuild.

## Operator override

Serve this UTF-8 JSON file at exactly `/vaultstadio-config.json`:

```json
{"apiBaseUrl":"https://storage.example/gateway/api"}
```

In the supplied Nginx image, mount the operator file read-only over `/usr/share/nginx/html/vaultstadio-config.json`. Keep the normal JSON MIME type and `Cache-Control: no-store`. A Kubernetes operator can use an equivalent read-only ConfigMap file mount. Do not place credentials in this public file. The browser fetches it without cookies and refuses redirects.

| Supplied value | Versioned request prefix |
| --- | --- |
| No `apiBaseUrl` key | Serving origin + `/api/v1` |
| `https://storage.example` | `https://storage.example/api/v1` |
| `https://storage.example/api/` | `https://storage.example/api/v1` |
| `https://storage.example/gateway/api` | `https://storage.example/gateway/api/v1` |
| `https://storage.example/gateway/v1/` | `https://storage.example/gateway/v1` |

A nonempty configured path is literal: `/gateway` means `/gateway/v1`, not `/gateway/api/v1`. IPv6 hosts must be bracketed. Supported escaped prefixes are preserved. This is an API prefix; hosting the application itself under a Web subpath is not certified here.

For an API on another origin, set backend `CORS_ALLOWED_ORIGINS` to the exact Web origin, including its scheme and nondefault port. Use a comma-separated list for multiple approved origins. Prefer explicit origins to the existing wildcard deployment option. Private image requests need the admitted `Authorization` and `Cache-Control` headers. TLS Web pages cannot select an HTTP API. Browser certificate and mixed-content rules still apply.

A reverse proxy exposing `/gateway/api` must map that path to backend `/api`, including public share pages and `/share-assets`. Preserve the existing public-share Origin checks, CSP and referrer policy. Redact the whole public capability path from access logs and suppress upstream errors that could print it; do not log query strings or Referer. The supplied root-prefix Nginx policy demonstrates these protections, but does not automatically create a custom proxy mapping.

## Failure and recovery

Only a JSON object with the optional string key `apiBaseUrl` is accepted. Null, empty strings, unknown keys, malformed JSON/UTF-8, unsupported schemes, userinfo, query/fragment, unsafe navigation/encoding and invalid ports fail before application startup. The file is limited to 16 KiB, the URL to 2,048 UTF-16 units, and the entire fetch/read to five seconds. A missing file or an HTML SPA fallback is an error, not a localhost fallback.

The page displays a native accessible error and a **Reload** button. Correct the server file and reload. The application takes one immutable configuration snapshot per startup; changing the file affects the next reload. Query strings, hash parameters, user preferences and arbitrary window globals cannot override it.

Check configuration status, MIME, no-store headers and proxy mapping first. Then verify normal login and an authenticated file request in the deployed browser. Public links use the configured public API address; it must be reachable by recipients. Account bearer tokens are never added to generated share or version URLs, and automatic bearer attachment is restricted to the configured authority.

Historical version download currently returns metadata (#109); collaboration protocol interoperability remains #112. Fixing these locations does not certify either feature. The legacy text-preview display cap is 100 KiB, while its domain download operation still buffers the full body. Audio/video/PDF playback and the current file-screen preview action have separate implementation gaps; URL/component checks do not prove those interactions work.
