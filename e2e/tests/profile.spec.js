import { test, expect } from '@playwright/test';
import { DRIVER, login } from './helpers.js';

test.describe('Profile page', () => {

  test.beforeEach(async ({ page }) => {
    await login(page, DRIVER);
  });

  test('profile page shows correct username and driver role badge', async ({ page }) => {
    await page.goto('/profile.html');
    await expect(page.locator('#profile-username')).toHaveText(DRIVER.username);
    await expect(page.locator('#profile-role-badge')).toContainText('Driver');
    await expect(page.locator('#info-username')).toHaveText(DRIVER.username);
    await expect(page.locator('#info-role')).toContainText('DRIVER');
  });

  test('clicking the navbar avatar navigates to profile.html', async ({ page }) => {
    await page.goto('/stations.html');
    await page.click('.nav-profile-link');
    await page.waitForURL(/profile\.html/);
    await expect(page).toHaveURL(/profile\.html/);
  });

  test('change password with wrong current password shows error without logging out', async ({ page }) => {
    await page.goto('/profile.html');
    await page.fill('#current-pw', 'definitely_wrong_password');
    await page.fill('#new-pw', 'newpassword123');
    await page.fill('#confirm-pw', 'newpassword123');
    await page.click('#pw-btn');
    await expect(page.locator('#pw-alert')).toBeVisible();
    await expect(page.locator('#pw-alert')).not.toBeEmpty();
    // Still on profile page — not logged out
    await expect(page).toHaveURL(/profile\.html/);
  });

  test('mismatched new passwords shows client-side error', async ({ page }) => {
    await page.goto('/profile.html');
    await page.fill('#current-pw', DRIVER.password);
    await page.fill('#new-pw', 'newpassword123');
    await page.fill('#confirm-pw', 'differentpassword');
    await page.click('#pw-btn');
    await expect(page.locator('#pw-alert')).toBeVisible();
    await expect(page.locator('#pw-alert')).toContainText(/match/i);
    await expect(page).toHaveURL(/profile\.html/);
  });

  test('successful password change shows success message', async ({ page }) => {
    await page.goto('/profile.html');
    // Change to a new password then change back to avoid breaking other tests
    await page.fill('#current-pw', DRIVER.password);
    await page.fill('#new-pw', 'temppassword999');
    await page.fill('#confirm-pw', 'temppassword999');
    await page.click('#pw-btn');
    await expect(page.locator('#pw-alert')).toBeVisible();
    await expect(page.locator('#pw-alert')).toContainText(/updated|success/i);

    // Restore the original password
    await page.fill('#current-pw', 'temppassword999');
    await page.fill('#new-pw', DRIVER.password);
    await page.fill('#confirm-pw', DRIVER.password);
    await page.click('#pw-btn');
    await expect(page.locator('#pw-alert')).toContainText(/updated|success/i);
  });

});
