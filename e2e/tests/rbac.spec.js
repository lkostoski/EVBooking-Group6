import { test, expect } from '@playwright/test';
import { ADMIN, DRIVER, DRIVER2, login } from './helpers.js';

test.describe('RBAC — role-based access control', () => {

  test('driver visiting admin.html is redirected to stations', async ({ page }) => {
    await login(page, DRIVER);
    await page.goto('/admin.html');
    await page.waitForURL(/stations\.html/);
  });

  test('driver cannot delete a station via API (403)', async ({ page }) => {
    await login(page, DRIVER);
    const resp = await page.request.delete('api/stations/1');
    expect(resp.status()).toBe(403);
  });

  test('driver cannot create a station via API (403)', async ({ page }) => {
    await login(page, DRIVER);
    const resp = await page.request.post('api/stations', {
      data: { name: 'Hack Station', address: 'Nowhere', latitude: 0, longitude: 0 },
    });
    expect(resp.status()).toBe(403);
  });

  test('driver cannot see bookings from another driver via API', async ({ page }) => {
    await login(page, DRIVER);   // kostas
    const resp = await page.request.get('api/bookings');
    expect(resp.ok()).toBeTruthy();
    const bookings = await resp.json();
    // kostas's response must not contain elena's bookings
    const hasElena = bookings.some(b => b.username === DRIVER2.username);
    expect(hasElena).toBeFalsy();
  });

  test('unauthenticated GET /api/stations returns 401', async ({ page }) => {
    // Do not login — no session cookie
    await page.goto('/login.html'); // sets baseURL context without logging in
    const resp = await page.request.get('api/stations');
    expect(resp.status()).toBe(401);
  });

  test('unauthenticated GET /api/bookings returns 401', async ({ page }) => {
    await page.goto('/login.html');
    const resp = await page.request.get('api/bookings');
    expect(resp.status()).toBe(401);
  });

  test('admin can access admin.html without redirect', async ({ page }) => {
    await login(page, ADMIN);
    await page.goto('/admin.html');
    // Should stay on admin.html (not redirect to stations or login)
    await page.waitForSelector('.tab-btn');
    await expect(page).toHaveURL(/admin\.html/);
  });

});
