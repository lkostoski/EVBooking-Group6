import { test, expect } from '@playwright/test';
import { ADMIN, DRIVER, login, logout } from './helpers.js';

test.describe('Authentication', () => {

  test('admin login lands on stations page and shows username in navbar', async ({ page }) => {
    await login(page, ADMIN);
    await expect(page.locator('#nav-username')).toHaveText(ADMIN.username);
    await expect(page).toHaveURL(/stations\.html/);
  });

  test('driver login lands on stations page', async ({ page }) => {
    await login(page, DRIVER);
    await expect(page).toHaveURL(/stations\.html/);
    await expect(page.locator('#nav-username')).toHaveText(DRIVER.username);
  });

  test('wrong password shows error and stays on login page', async ({ page }) => {
    await page.goto('/login.html');
    await page.fill('#username', DRIVER.username);
    await page.fill('#password', 'wrongpassword');
    await page.click('#login-btn');
    await expect(page.locator('#alert-box')).toBeVisible();
    await expect(page.locator('#alert-box')).not.toBeEmpty();
    await expect(page).toHaveURL(/login\.html/);
  });

  test('empty username field triggers native form validation', async ({ page }) => {
    await page.goto('/login.html');
    await page.fill('#password', 'somepassword');
    await page.click('#login-btn');
    // Native validation prevents submission — still on login page
    await expect(page).toHaveURL(/login\.html/);
  });

  test('logout redirects to login page and clears session', async ({ page }) => {
    await login(page, DRIVER);
    await logout(page);
    await expect(page).toHaveURL(/login\.html/);
    // Navigating to stations without a session redirects back to login
    await page.goto('/stations.html');
    await page.waitForURL(/login\.html/);
  });

  test('visiting stations.html while logged out redirects to login', async ({ page }) => {
    await page.goto('/stations.html');
    await page.waitForURL(/login\.html/);
  });

  test('visiting admin.html while logged out redirects to login', async ({ page }) => {
    await page.goto('/admin.html');
    await page.waitForURL(/login\.html/);
  });

  test('visiting admin.html as driver redirects to stations', async ({ page }) => {
    await login(page, DRIVER);
    await page.goto('/admin.html');
    await page.waitForURL(/stations\.html/);
  });

  test('register new user auto-logs in and lands on stations page', async ({ page }) => {
    const username = `testuser_${Date.now()}`;
    await page.goto('/register.html');
    await page.fill('#username', username);
    await page.fill('#password', 'test1234');
    await page.fill('#confirm', 'test1234');
    await page.click('#reg-btn');
    // Auto-login: should land directly on stations, not login
    await page.waitForURL(/stations\.html/, { timeout: 10000 });
    await expect(page.locator('#nav-username')).toHaveText(username);
  });

  test('register with mismatched passwords shows error and stays on register page', async ({ page }) => {
    await page.goto('/register.html');
    await page.fill('#username', 'anyuser');
    await page.fill('#password', 'password1');
    await page.fill('#confirm', 'password2');
    await page.click('#reg-btn');
    await expect(page.locator('#alert-box')).toBeVisible();
    await expect(page).toHaveURL(/register\.html/);
  });

});
