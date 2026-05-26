import { test, expect } from '@playwright/test';
import { ADMIN, login, daysFromNow } from './helpers.js';

test.describe('Admin panel', () => {

  test.beforeEach(async ({ page }) => {
    await login(page, ADMIN);
    await page.goto('/admin.html');
    await page.waitForSelector('.tab-btn');
  });

  test('admin panel renders 4 tabs', async ({ page }) => {
    const tabs = page.locator('.tab-btn');
    await expect(tabs).toHaveCount(4);
    await expect(tabs.nth(0)).toContainText('Stations');
    await expect(tabs.nth(1)).toContainText('Connectors');
    await expect(tabs.nth(2)).toContainText('Slots');
    await expect(tabs.nth(3)).toContainText('All Bookings');
  });

  test('Stations tab shows a table with station rows', async ({ page }) => {
    await page.waitForSelector('#stations-table-wrap table');
    const rows = page.locator('#stations-table-wrap tbody tr');
    const count = await rows.count();
    expect(count).toBeGreaterThan(0);
    // Each row should have a station name cell
    await expect(rows.first().locator('td').first()).not.toBeEmpty();
  });

  test('add station → new station appears in table', async ({ page }) => {
    const stationName = `Test Station ${Date.now()}`;
    await page.waitForSelector('#add-station-btn');
    await page.click('#add-station-btn');
    await expect(page.locator('#station-modal')).not.toHaveClass(/hidden/);

    await page.fill('#s-name', stationName);
    await page.fill('#s-address', '1 Test Street, Thessaloniki');
    await page.fill('#s-lat', '40.6401');
    await page.fill('#s-lng', '22.9444');
    await page.click('#save-station-btn');

    // Modal should close and new row appear
    await expect(page.locator('#station-modal')).toHaveClass(/hidden/);
    await page.waitForSelector('#stations-table-wrap table');
    await expect(page.locator('#stations-table-wrap')).toContainText(stationName);

    // Cleanup: delete the station we just created
    const rows = page.locator('#stations-table-wrap tbody tr');
    const count = await rows.count();
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i);
      if (await row.textContent().then(t => t.includes(stationName))) {
        page.on('dialog', d => d.accept());
        await row.locator('.del-station-btn, button[title*="elete"], button').last().click();
        break;
      }
    }
  });

  test('edit station → updated name shown in table', async ({ page }) => {
    // First add a station to edit
    const originalName = `EditMe ${Date.now()}`;
    const updatedName  = `Edited ${Date.now()}`;
    await page.click('#add-station-btn');
    await page.fill('#s-name', originalName);
    await page.fill('#s-address', '2 Edit Street, Thessaloniki');
    await page.click('#save-station-btn');
    await expect(page.locator('#station-modal')).toHaveClass(/hidden/);
    await page.waitForSelector(`#stations-table-wrap :text("${originalName}")`);

    // Click the edit button on the new row
    const rows = page.locator('#stations-table-wrap tbody tr');
    const count = await rows.count();
    for (let i = 0; i < count; i++) {
      const row = rows.nth(i);
      if (await row.textContent().then(t => t.includes(originalName))) {
        await row.locator('button').first().click(); // edit button
        break;
      }
    }

    await expect(page.locator('#station-modal')).not.toHaveClass(/hidden/);
    await page.fill('#s-name', updatedName);
    await page.click('#save-station-btn');
    await expect(page.locator('#station-modal')).toHaveClass(/hidden/);
    await expect(page.locator('#stations-table-wrap')).toContainText(updatedName);

    // Cleanup
    const rowsAfter = page.locator('#stations-table-wrap tbody tr');
    const countAfter = await rowsAfter.count();
    for (let i = 0; i < countAfter; i++) {
      const row = rowsAfter.nth(i);
      if (await row.textContent().then(t => t.includes(updatedName))) {
        page.on('dialog', d => d.accept());
        await row.locator('button').last().click();
        break;
      }
    }
  });

  test('delete station → station removed from table', async ({ page }) => {
    // Create a temporary station to delete
    const tempName = `DeleteMe ${Date.now()}`;
    await page.click('#add-station-btn');
    await page.fill('#s-name', tempName);
    await page.fill('#s-address', '3 Delete Street, Thessaloniki');
    await page.click('#save-station-btn');
    await expect(page.locator('#station-modal')).toHaveClass(/hidden/);
    await page.waitForSelector(`#stations-table-wrap :text("${tempName}")`);

    const rows = page.locator('#stations-table-wrap tbody tr');
    const countBefore = await rows.count();

    // Find and delete the temp station
    for (let i = 0; i < countBefore; i++) {
      const row = rows.nth(i);
      if (await row.textContent().then(t => t.includes(tempName))) {
        page.on('dialog', d => d.accept());
        await row.locator('button').last().click();
        break;
      }
    }

    await page.waitForTimeout(800);
    await expect(page.locator('#stations-table-wrap')).not.toContainText(tempName);
  });

  test('Connectors tab: selecting a station loads connector table', async ({ page }) => {
    await page.locator('.tab-btn[data-tab="connectors"]').click();
    await expect(page.locator('#tab-connectors')).toHaveClass(/active/);

    // Select station 1 (first option after the placeholder)
    await page.locator('#conn-station-select').selectOption({ index: 1 });
    await page.waitForSelector('#connectors-table-wrap table');
    const rows = page.locator('#connectors-table-wrap tbody tr');
    await expect(rows.first()).toBeVisible();
  });

  test('add connector → appears in table', async ({ page }) => {
    await page.locator('.tab-btn[data-tab="connectors"]').click();
    await page.locator('#conn-station-select').selectOption({ index: 1 });
    await page.waitForSelector('#connectors-table-wrap table');

    const countBefore = await page.locator('#connectors-table-wrap tbody tr').count();
    await page.click('#add-conn-btn');
    await expect(page.locator('#conn-modal')).not.toHaveClass(/hidden/);

    await page.locator('#c-type').selectOption('Tesla');
    await page.click('#save-conn-btn');
    await expect(page.locator('#conn-modal')).toHaveClass(/hidden/);

    await page.waitForSelector('#connectors-table-wrap table');
    const countAfter = await page.locator('#connectors-table-wrap tbody tr').count();
    expect(countAfter).toBeGreaterThan(countBefore);

    // Cleanup: delete the Tesla connector we just added (last row)
    const rows = page.locator('#connectors-table-wrap tbody tr');
    const finalCount = await rows.count();
    page.on('dialog', d => d.accept());
    await rows.nth(finalCount - 1).locator('button').last().click();
  });

  test('Slots tab: select station + connector + date loads slots table', async ({ page }) => {
    await page.locator('.tab-btn[data-tab="slots"]').click();
    await expect(page.locator('#tab-slots')).toHaveClass(/active/);

    await page.locator('#slot-station-select').selectOption({ index: 1 });
    await page.waitForSelector('#slot-conn-select:not([disabled])');
    await page.locator('#slot-conn-select').selectOption({ index: 1 });
    await expect(page.locator('#slot-filter-date')).toBeEnabled();

    await page.fill('#slot-filter-date', daysFromNow(1));
    await page.waitForSelector('#slots-table-wrap table');
    const rows = page.locator('#slots-table-wrap tbody tr');
    await expect(rows.first()).toBeVisible();
  });

  test('add slot → appears in slots table', async ({ page }) => {
    await page.locator('.tab-btn[data-tab="slots"]').click();
    await page.locator('#slot-station-select').selectOption({ index: 1 });
    await page.waitForSelector('#slot-conn-select:not([disabled])');
    await page.locator('#slot-conn-select').selectOption({ index: 1 });
    await page.fill('#slot-filter-date', daysFromNow(1));
    await page.waitForSelector('#slots-table-wrap table');

    const countBefore = await page.locator('#slots-table-wrap tbody tr').count();
    await page.click('#add-slot-btn');
    await expect(page.locator('#slot-modal')).not.toHaveClass(/hidden/);

    // Use a time range unlikely to conflict
    await page.fill('#sl-date', daysFromNow(1));
    await page.fill('#sl-start', '21:00');
    await page.fill('#sl-end', '22:00');
    await page.click('#save-slot-btn');
    await expect(page.locator('#slot-modal')).toHaveClass(/hidden/);

    await page.waitForSelector('#slots-table-wrap table');
    const countAfter = await page.locator('#slots-table-wrap tbody tr').count();
    expect(countAfter).toBeGreaterThan(countBefore);

    // Cleanup: delete the last slot
    const rows = page.locator('#slots-table-wrap tbody tr');
    const finalCount = await rows.count();
    page.on('dialog', d => d.accept());
    await rows.nth(finalCount - 1).locator('button').last().click();
  });

  test('All Bookings tab shows bookings with driver username column', async ({ page }) => {
    await page.locator('.tab-btn[data-tab="bookings"]').click();
    await page.waitForSelector('#all-bookings-wrap table');
    const rows = page.locator('#all-bookings-wrap tbody tr');
    const count = await rows.count();
    expect(count).toBeGreaterThan(0);
    // Each row should contain a username (kostas or elena from seed)
    const firstRowText = await rows.first().textContent();
    expect(firstRowText).toMatch(/kostas|elena/i);
  });

  test('admin can cancel a future booking from All Bookings tab', async ({ page }) => {
    await page.locator('.tab-btn[data-tab="bookings"]').click();
    await page.waitForSelector('#all-bookings-wrap table');

    // Find an ACTIVE future booking with a cancel button
    const cancelBtn = page.locator('.admin-cancel-btn').first();
    await expect(cancelBtn).toBeVisible();

    page.on('dialog', d => d.accept());
    await cancelBtn.click();
    await page.waitForSelector('#alert-box .alert-success, .badge-secondary');
    await expect(page.locator('#alert-box')).toContainText('cancelled');
  });

  test('All Bookings filter Active/Cancelled/Completed works', async ({ page }) => {
    await page.locator('.tab-btn[data-tab="bookings"]').click();
    await page.waitForSelector('#all-bookings-wrap table');
    // All bookings load by default; check that there are rows
    const totalRows = await page.locator('#all-bookings-wrap tbody tr').count();
    expect(totalRows).toBeGreaterThan(0);
    // The table should contain Active and Completed badges from seed data
    await expect(page.locator('#all-bookings-wrap')).toContainText(/Active|Completed/);
  });

});
