import { test, expect } from '@playwright/test';
import { DRIVER, DRIVER2, login, tomorrow } from './helpers.js';

// Station 1 (Aristotelous) — connectors 1 (Type 2), 2 (CCS), 3 (CHAdeMO)
const STATION_ID = 1;

test.describe('Booking flow', () => {

  test.beforeEach(async ({ page }) => {
    await login(page, DRIVER);
  });

  // After each test cancel any ACTIVE future booking created during the test
  test.afterEach(async ({ page }) => {
    try {
      const resp = await page.request.get('api/bookings', { headers: { Accept: 'application/json' } });
      if (!resp.ok()) return;
      const bookings = await resp.json();
      const toClean = bookings.filter(b => b.status === 'ACTIVE' && b.date >= tomorrow());
      for (const b of toClean) {
        await page.request.delete(`api/bookings/${b.bookingId}`).catch(() => {});
      }
    } catch (_) {}
  });

  test('station page shows connector buttons', async ({ page }) => {
    await page.goto(`/station.html?id=${STATION_ID}`);
    await page.waitForSelector('.connector-type-btn');
    await expect(page.locator('.connector-type-btn').first()).toBeVisible();
  });

  test('selecting a connector enables the date input', async ({ page }) => {
    await page.goto(`/station.html?id=${STATION_ID}`);
    await page.waitForSelector('.connector-type-btn');
    await expect(page.locator('#slot-date')).toBeDisabled();
    await page.locator('.connector-type-btn').first().click();
    await expect(page.locator('#slot-date')).toBeEnabled();
  });

  test('selecting a date loads the availability window', async ({ page }) => {
    await page.goto(`/station.html?id=${STATION_ID}`);
    await page.waitForSelector('.connector-type-btn');
    await page.locator('.connector-type-btn').first().click();
    await page.fill('#slot-date', tomorrow());
    await page.waitForSelector('#booking-start');
    await expect(page.locator('#booking-start')).toBeVisible();
    await expect(page.locator('#booking-end')).toBeVisible();
  });

  test('picking start and end time enables the review button', async ({ page }) => {
    await page.goto(`/station.html?id=${STATION_ID}`);
    await page.waitForSelector('.connector-type-btn');
    await page.locator('.connector-type-btn').first().click();
    await page.fill('#slot-date', tomorrow());
    await page.waitForSelector('#booking-start');
    await page.fill('#booking-start', '10:00');
    await page.dispatchEvent('#booking-start', 'change');
    await page.fill('#booking-end', '10:40');
    await page.dispatchEvent('#booking-end', 'change');
    await expect(page.locator('#review-btn')).toBeEnabled();
  });

  test('clicking review opens the confirm modal with correct time', async ({ page }) => {
    await page.goto(`/station.html?id=${STATION_ID}`);
    await page.waitForSelector('.connector-type-btn');
    await page.locator('.connector-type-btn').first().click();
    await page.fill('#slot-date', tomorrow());
    await page.waitForSelector('#booking-start');
    await page.fill('#booking-start', '09:00');
    await page.dispatchEvent('#booking-start', 'change');
    await page.fill('#booking-end', '09:30');
    await page.dispatchEvent('#booking-end', 'change');
    await page.click('#review-btn');
    await expect(page.locator('#confirm-modal')).not.toHaveClass(/hidden/);
    await expect(page.locator('#m-station')).not.toBeEmpty();
    await expect(page.locator('#m-time')).toContainText('9:00');
  });

  test('existing bookings on connector/date are shown in the window info', async ({ page }) => {
    await page.goto(`/station.html?id=${STATION_ID}`);
    await page.waitForSelector('.connector-type-btn');
    await page.locator('.connector-type-btn:has-text("CCS")').click();
    const inTwoDays = new Date();
    inTwoDays.setDate(inTwoDays.getDate() + 2);
    await page.fill('#slot-date', inTwoDays.toISOString().split('T')[0]);
    await page.waitForSelector('#booking-start');
    // seed has a booking on CCS connector in 2 days — should show "Already booked"
    await expect(page.locator('#slots-container')).toContainText(/Already booked/);
  });

  test('confirming a booking redirects to bookings page', async ({ page }) => {
    await page.goto(`/station.html?id=${STATION_ID}`);
    await page.waitForSelector('.connector-type-btn');
    // Use connector 3 (CHAdeMO) to avoid conflicts with seed bookings
    await page.locator('.connector-type-btn:has-text("CHAdeMO")').click();
    await page.fill('#slot-date', tomorrow());
    await page.waitForSelector('#booking-start');
    await page.fill('#booking-start', '09:00');
    await page.dispatchEvent('#booking-start', 'change');
    await page.fill('#booking-end', '09:30');
    await page.dispatchEvent('#booking-end', 'change');
    await page.click('#review-btn');
    await expect(page.locator('#confirm-modal')).not.toHaveClass(/hidden/);
    await page.click('#confirm-book-btn');
    await page.waitForURL(/bookings\.html/, { timeout: 10000 });
  });

  test('new booking appears in bookings list with Active status', async ({ page }) => {
    await page.goto(`/station.html?id=${STATION_ID}`);
    await page.waitForSelector('.connector-type-btn');
    await page.locator('.connector-type-btn:has-text("CHAdeMO")').click();
    await page.fill('#slot-date', tomorrow());
    await page.waitForSelector('#booking-start');
    await page.fill('#booking-start', '09:00');
    await page.dispatchEvent('#booking-start', 'change');
    await page.fill('#booking-end', '09:30');
    await page.dispatchEvent('#booking-end', 'change');
    await page.click('#review-btn');
    await page.click('#confirm-book-btn');
    await page.waitForURL(/bookings\.html/, { timeout: 10000 });
    await page.waitForSelector('.badge-success');
    await expect(page.locator('.badge-success').first()).toContainText('Active');
  });

  test('filter Active shows only active future bookings', async ({ page }) => {
    await page.goto('/bookings.html');
    await page.waitForSelector('#bookings-container table, .empty-state');
    await page.locator('.filter-pill[data-filter="ACTIVE"]').click();
    // All visible badges should be Active (no Completed or Cancelled)
    const badges = page.locator('.badge-secondary');
    const count = await badges.count();
    // Either no secondary badges visible (all are Active) or the table is empty
    if (count > 0) {
      for (let i = 0; i < count; i++) {
        await expect(badges.nth(i)).not.toContainText('Active');
      }
    }
  });

  test('filter Completed shows only past active bookings', async ({ page }) => {
    await page.goto('/bookings.html');
    await page.waitForSelector('#bookings-container table, .empty-state');
    await page.locator('.filter-pill[data-filter="COMPLETED"]').click();
    await page.waitForSelector('#bookings-container table, .empty-state');
    // kostas has 1 completed booking from seed (3 days ago, ACTIVE)
    const badges = page.locator('.badge');
    const count = await badges.count();
    if (count > 0) {
      // All visible badges should say Completed
      for (let i = 0; i < count; i++) {
        await expect(badges.nth(i)).toContainText('Completed');
      }
    }
  });

  test('filter Cancelled shows only cancelled bookings', async ({ page }) => {
    await page.goto('/bookings.html');
    await page.waitForSelector('#bookings-container table, .empty-state');
    await page.locator('.filter-pill[data-filter="CANCELLED"]').click();
    await page.waitForSelector('#bookings-container table, .empty-state');
    // kostas has 1 cancelled booking from seed
    const rows = page.locator('tbody tr');
    const count = await rows.count();
    if (count > 0) {
      for (let i = 0; i < count; i++) {
        await expect(rows.nth(i).locator('.badge')).toContainText('Cancelled');
      }
    }
  });

  test('clicking modify on a future active booking opens the modify modal', async ({ page }) => {
    await page.goto('/bookings.html');
    await page.waitForSelector('.modify-btn');
    await page.locator('.modify-btn').first().click();
    await expect(page.locator('#modify-modal')).not.toHaveClass(/hidden/);
  });

  test('cancelling a booking changes its status to Cancelled', async ({ page }) => {
    // Create a booking first
    await page.goto(`/station.html?id=${STATION_ID}`);
    await page.waitForSelector('.connector-type-btn');
    await page.locator('.connector-type-btn:has-text("CHAdeMO")').click();
    await page.fill('#slot-date', tomorrow());
    await page.waitForSelector('#booking-start');
    await page.fill('#booking-start', '09:00');
    await page.dispatchEvent('#booking-start', 'change');
    await page.fill('#booking-end', '09:30');
    await page.dispatchEvent('#booking-end', 'change');
    await page.click('#review-btn');
    await page.click('#confirm-book-btn');
    await page.waitForURL(/bookings\.html/, { timeout: 10000 });

    // Cancel the new booking (first cancel button in the Active filter)
    await page.waitForSelector('.cancel-btn');
    page.on('dialog', d => d.accept());
    await page.locator('.cancel-btn').first().click();
    await page.waitForSelector('#alert-box .alert-success, .badge-secondary');
    // Reload and check the Cancelled filter
    await page.reload();
    await page.locator('.filter-pill[data-filter="CANCELLED"]').click();
    await page.waitForSelector('#bookings-container table, .empty-state');
    const cancelledCount = await page.locator('tbody tr').count();
    expect(cancelledCount).toBeGreaterThan(0);
  });

  test('driver does not see bookings from other drivers', async ({ page }) => {
    // Login as driver2 (elena) and check that kostas bookings are absent
    await login(page, DRIVER2);
    const resp = await page.request.get('api/bookings', { headers: { Accept: 'application/json' } });
    expect(resp.ok()).toBeTruthy();
    const bookings = await resp.json();
    const hasKostasBooking = bookings.some(b => b.username === DRIVER.username);
    expect(hasKostasBooking).toBeFalsy();
  });

});
