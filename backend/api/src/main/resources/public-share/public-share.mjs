export function shareEndpoint(pathname) {
  const match = /^\/api\/v1\/share\/([A-Za-z0-9_-]{1,64})\/view(?:\/download)?$/.exec(pathname);
  return match ? `/api/v1/share/${match[1]}` : null;
}

export function encodeSharePassword(password) {
  encodeURIComponent(password); // Reject unpaired surrogates before TextEncoder can replace them.
  const bytes = new TextEncoder().encode(password);
  return btoa(String.fromCharCode(...bytes)).replaceAll('+', '-').replaceAll('/', '_').replaceAll('=', '');
}

export function informationRequest(password) {
  return {
    credentials: 'omit', cache: 'no-store', redirect: 'error',
    headers: password ? { 'X-Share-Password': encodeSharePassword(password) } : {},
  };
}

export function safeInformation(body) {
  const data = body?.data;
  return body?.success === true && typeof data?.name === 'string' &&
    Number.isSafeInteger(data.size) && data.size >= 0 ? data : null;
}

export function mountSharePage(document, location, fetchInformation) {
  const form = document.getElementById('share-form');
  const password = document.getElementById('password');
  const status = document.getElementById('status');
  const download = document.getElementById('download');
  const load = document.getElementById('load');
  const details = document.getElementById('details');
  const endpoint = shareEndpoint(location.pathname);
  let generation = 0;
  let pending = null;
  if (!endpoint) {
    status.textContent = 'This share link is invalid. Ask its owner for a new link.';
    load.disabled = true;
    return;
  }
  form.action = `${endpoint}/view/download`;
  const invalidate = () => {
    generation++;
    pending?.abort();
    pending = null;
    download.disabled = true;
    details.hidden = true;
    load.disabled = false;
  };
  password.addEventListener('input', () => {
    invalidate();
    status.textContent = 'Password changed. Load file information again.';
  });
  form.addEventListener('submit', event => {
    if (download.disabled) { event.preventDefault(); return; }
    status.textContent = 'Download requested. Check your browser downloads; this page cannot confirm that the file was saved.';
    invalidate();
  });
  async function inspect() {
    const current = ++generation;
    pending?.abort();
    const controller = new AbortController();
    pending = controller;
    const timeout = setTimeout(() => controller.abort(), 15000);
    download.disabled = true;
    details.hidden = true;
    load.disabled = true;
    status.textContent = 'Loading shared file…';
    try {
      const response = await fetchInformation(endpoint, { ...informationRequest(password.value), signal: controller.signal });
      const body = await response.json();
      if (current !== generation) return;
      const data = response.ok && safeInformation(body);
      if (!data) {
        // Only known safe codes influence UI text; server messages remain untrusted.
        const code = body?.error?.code;
        status.textContent = code === 'SHARE_PASSWORD_REQUIRED' ? 'Enter the link password and load again.' :
          code === 'SHARE_PASSWORD_INVALID' ? 'Incorrect password. Check it and load again.' :
          code === 'INVALID_SHARE_REQUEST' ? 'Check the password length (1 to 72 UTF-8 bytes) and load again.' :
          response.status === 410 ? 'This link is no longer available. Ask its owner for a new link.' :
          response.status === 404 ? 'Shared file not found. Check the link with its owner.' :
          'The shared file could not be loaded. Try again later.';
        return;
      }
      document.getElementById('filename').textContent = data.name;
      document.getElementById('size').textContent = `${data.size} bytes`;
      details.hidden = false;
      download.disabled = false;
      status.textContent = 'File ready. You can download it now.';
    } catch {
      if (current === generation) status.textContent = 'Connection failed. Check your connection and load again.';
    } finally {
      clearTimeout(timeout);
      if (current === generation) {
        load.disabled = false;
        pending = null;
      }
    }
  }
  load.addEventListener('click', inspect);
  // Keep a POST denial visible until the user explicitly retries.
  if (!status.textContent.trim()) inspect();
}

if (typeof document !== 'undefined') mountSharePage(document, window.location, window.fetch.bind(window));
