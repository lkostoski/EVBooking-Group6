import { test, expect } from '@playwright/test';
import { ADMIN, DRIVER, login } from './helpers.js';

test.describe('Stations page', () => {

  test.beforeEach(async ({ page }) => {
    await login(page, DRIVER);
  });

  test('map container and sidebar station cards are visible', async ({ page }) => {
    await expect(page.locator('#station-map')).toBeVisible();
    // Wait for station cards to load
    await page.waitForSelector('.station-card');
    await expect(page.locator('.station-card').first()).toBeVisible();
  });

  test('station cards show name and address', async ({ page }) => {
    await page.waitForSelector('.station-card');
    const firstCard = page.locator('.station-card').first();
    await expect(firstCard.locator('.station-name')).not.toBeEmpty();
    await expect(firstCard.locator('.station-address')).not.toBeEmpty();
  });

  test('search input filters station cards in real time', async ({ page }) => {
    await page.waitForSelector('.station-card');
    const countBefore = await page.locator('.station-card').count();
    await page.fill('#search-input', 'Aristotelous');
    await page.waitForFunction(
      () => document.querySelectorAll('.station-card').length > 0
    );
    const countAfter = await page.locator('.station-card').count();
    expect(countAfter).toBeLessThanOrEqual(countBefore);
    await expect(page.locator('.station-name').first()).toContainText('Aristotelous');
  });

  test('searching for a non-existent station shows empty state', async ({ page }) => {
    await page.waitForSelector('.station-card');
    await page.fill('#search-input', 'xyznonexistent99');
    await page.waitForSelector('.empty-state');
    await expect(page.locator('.empty-state')).toBeVisible();
  });

  test('clicking a station card opens the detail panel', async ({ page }) => {
    await page.waitForSelector('.station-card');
    await page.locator('.station-card').first().click();
    await expect(page.locator('#station-detail-panel')).toHaveClass(/open/);
    await expect(page.locator('#detail-name')).not.toBeEmpty();
  });

  test('detail panel has a working View & Book link to station.html', async ({ page }) => {
    await page.waitForSelector('.station-card');
    await page.locator('.station-card').first().click();
    await expect(page.locator('#station-detail-panel')).toHaveClass(/open/);
    const href = await page.locator('#detail-book-btn').getAttribute('href');
    expect(href).toMatch(/station\.html\?id=\d+/);
  });

  test('closing the detail panel with ✕ hides it', async ({ page }) => {
    await page.waitForSelector('.station-card');
    await page.locator('.station-card').first().click();
    await expect(page.locator('#station-detail-panel')).toHaveClass(/open/);
    await page.click('#detail-close');
    await expect(page.locator('#station-detail-panel')).not.toHaveClass(/open/);
  });

  test('Admin nav link is NOT visible for a driver', async ({ page }) => {
    await expect(page.locator('#nav-admin-link')).not.toBeVisible();
  });

  test('Admin nav link IS visible for admin', async ({ page }) => {
    await login(page, ADMIN);
    await expect(page.locator('#nav-admin-link')).toBeVisible();
  });

});
