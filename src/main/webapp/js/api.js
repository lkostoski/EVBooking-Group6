export async function apiFetch(path, options = {}) {
  const { skipAuthRedirect, ...fetchOptions } = options;

  const res = await fetch(path, {
    ...fetchOptions,
    credentials: 'include',
    headers: { 'Content-Type': 'application/json', ...(fetchOptions.headers || {}) }
  });

  if (res.status === 401) {
    let msg = 'Unauthorized';
    try { const d = await res.json(); msg = d.message || d.error || msg; } catch (_) {}
    if (!skipAuthRedirect && !window.location.pathname.endsWith('login.html')) {
      window.location.href = 'login.html';
    }
    throw new Error(msg);
  }

  if (res.status === 204) return null;

  if (!res.ok) {
    let msg = `Request failed (${res.status})`;
    try { const d = await res.json(); msg = d.message || d.error || msg; } catch (_) {}
    throw new Error(msg);
  }

  try { return await res.json(); } catch (_) { return null; }
}

export function showAlert(containerId, message, type = 'danger') {
  const el = document.getElementById(containerId);
  if (!el) return;
  el.innerHTML = `
    <div class="alert alert-${type}" role="alert">
      <span class="alert-message">${message}</span>
      <button class="alert-close" onclick="this.parentElement.remove()" aria-label="Close">✕</button>
    </div>`;
}

export function clearAlert(containerId) {
  const el = document.getElementById(containerId);
  if (el) el.innerHTML = '';
}

export function fmtTime(t) {
  if (!t) return '';
  return t.substring(0, 5);
}

export function fmtDate(d) {
  if (!d) return '';
  const [y, m, day] = d.split('-');
  const date = new Date(+y, +m - 1, +day);
  return date.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
}

export function todayISO() {
  return new Date().toISOString().split('T')[0];
}

// HTML-escape any value before interpolating it into innerHTML or HTML attributes.
// Defends against stored XSS via admin-controlled station names, addresses, and
// usernames that flow through templated strings.
export function esc(s) {
  if (s == null) return '';
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
