import { apiFetch } from './api.js?v=20260526';

let currentUser = null;
let currentUserPromise = null;

export async function loadCurrentUser() {
  if (currentUser) return currentUser;
  if (!currentUserPromise) {
    currentUserPromise = apiFetch('api/auth/me', { skipAuthRedirect: true })
      .then(user => {
        currentUser = user;
        return currentUser;
      })
      .catch(err => {
        currentUser = null;
        currentUserPromise = null;
        throw err;
      });
  }
  return currentUserPromise;
}

export function getUsername() { return currentUser?.username || null; }
export function getRole()     { return currentUser?.role || null; }
export function isAdmin()     { return getRole() === 'ADMIN'; }

export async function requireAuth() {
  try {
    await loadCurrentUser();
  } catch (_) {
    window.location.href = 'login.html';
    return false;
  }
  return true;
}

export async function requireAdmin() {
  try {
    await loadCurrentUser();
  } catch (_) {
    window.location.href = 'login.html';
    return false;
  }
  if (!isAdmin()) {
    window.location.href = 'stations.html?cb=20260526';
    return false;
  }
  return true;
}

export async function logout() {
  try {
    await apiFetch('api/auth/logout', { method: 'POST', skipAuthRedirect: true });
  } catch (_) {}
  currentUser = null;
  currentUserPromise = null;
  window.location.href = 'login.html';
}

export function setupNavbar() {
  const username = getUsername();

  const avatarEl = document.getElementById('nav-avatar');
  if (avatarEl && username) avatarEl.textContent = username.charAt(0).toUpperCase();

  const usernameEl = document.getElementById('nav-username');
  if (usernameEl) usernameEl.textContent = username || '';

  const adminLink = document.getElementById('nav-admin-link');
  if (adminLink) adminLink.style.display = isAdmin() ? '' : 'none';

  const logoutBtn = document.getElementById('nav-logout');
  if (logoutBtn) logoutBtn.addEventListener('click', logout);

  const currentPage = window.location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav-link[href]').forEach(link => {
    if (link.getAttribute('href') === currentPage) link.classList.add('active');
  });

  // ── Mobile hamburger ───────────────────────────────────────
  const hamburger = document.getElementById('nav-hamburger');
  const dropdown  = document.getElementById('nav-dropdown');
  if (!hamburger || !dropdown) return;

  const dropdownAdminLink = document.getElementById('nav-dropdown-admin');
  if (dropdownAdminLink) dropdownAdminLink.style.display = isAdmin() ? '' : 'none';

  dropdown.querySelectorAll('a[href]').forEach(link => {
    if (link.getAttribute('href') === currentPage) link.classList.add('active');
  });

  hamburger.addEventListener('click', e => {
    e.stopPropagation();
    dropdown.classList.toggle('open');
  });

  dropdown.querySelectorAll('a').forEach(link => {
    link.addEventListener('click', () => dropdown.classList.remove('open'));
  });

  const dropdownLogout = document.getElementById('nav-dropdown-logout');
  if (dropdownLogout) {
    dropdownLogout.addEventListener('click', () => {
      dropdown.classList.remove('open');
      logout();
    });
  }

  document.addEventListener('click', e => {
    if (!dropdown.contains(e.target) && e.target !== hamburger) {
      dropdown.classList.remove('open');
    }
  });
}
