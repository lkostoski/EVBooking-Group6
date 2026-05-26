export const ADMIN   = { username: 'admin',  password: 'admin123'  };
export const DRIVER  = { username: 'kostas', password: 'driver123' };
export const DRIVER2 = { username: 'elena',  password: 'driver123' };

/** Logs in via the UI and waits until stations.html is loaded. */
export async function login(page, user) {
  await page.goto('/login.html');
  await page.fill('#username', user.username);
  await page.fill('#password', user.password);
  await page.click('#login-btn');
  await page.waitForURL(/stations\.html/);
}

/** Signs out via the navbar and waits for the login page. */
export async function logout(page) {
  await page.click('#nav-logout');
  await page.waitForURL(/login\.html/);
}

/** Returns tomorrow's date as YYYY-MM-DD (local time). */
export function tomorrow() {
  const d = new Date();
  d.setDate(d.getDate() + 1);
  return d.toISOString().split('T')[0];
}

/** Returns a date N days from now as YYYY-MM-DD (local time). */
export function daysFromNow(n) {
  const d = new Date();
  d.setDate(d.getDate() + n);
  return d.toISOString().split('T')[0];
}
