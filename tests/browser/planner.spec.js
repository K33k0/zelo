import { test, expect } from './fixtures.js';
const sample = (patch = {}) => ({
  id: 'monthly',
  name: 'November pushups',
  mode: 'total',
  goal: 3000,
  unit: 'reps',
  start: '2026-11-01',
  end: '2026-11-30',
  logs: [],
  quickAdd: 25,
  ...patch,
});
async function seed(page, items) {
  await page.goto('/');
  await page.evaluate(
    (items) => localStorage.setItem('stride-v1', JSON.stringify({ version: 2, challenges: items })),
    items,
  );
  await page.reload();
}
test('Today quick-add reduces a stable daily target and survives reload', async ({ page }) => {
  await seed(page, [sample()]);
  await expect(page.locator('.today-amount strong')).toHaveText('100');
  await page.getByRole('button', { name: '＋ 25 reps', exact: true }).click();
  await expect(page.locator('.today-amount strong')).toHaveText('75');
  await page.reload();
  await expect(page.locator('.today-amount strong')).toHaveText('75');
  await page.getByRole('button', { name: 'Activity', exact: true }).click();
  await expect(page.locator('.activity-amount')).toContainText('+25');
});
test('catch-up previews do not mutate data until confirmation; repeat keeps original logs', async ({
  page,
}) => {
  await seed(page, [
    sample({ logs: [{ id: 'a', date: '2026-11-01', amount: 25, note: 'Original' }] }),
  ]);
  await page.getByRole('button', { name: 'Adjust plan', exact: true }).click();
  await page.getByLabel('Preview deadline').fill('2026-12-31');
  await expect(page.locator('#plan-preview')).toContainText('scheduled day');
  expect(
    await page.evaluate(() => JSON.parse(localStorage.getItem('stride-v1')).challenges[0].end),
  ).toBe('2026-11-30');
  await page.getByRole('button', { name: 'Confirm this plan' }).click();
  expect(
    await page.evaluate(() => JSON.parse(localStorage.getItem('stride-v1')).challenges[0].end),
  ).toBe('2026-12-31');
  await page.locator('[data-detail]').click();
  await expect(page.locator('.progress-chart svg')).toBeVisible();
  await page.getByRole('button', { name: 'Repeat challenge', exact: true }).click();
  await page.getByLabel('New start date').fill('2027-01-01');
  await page.getByRole('button', { name: 'Create repeat' }).click();
  await expect(page.locator('table tbody tr')).toHaveCount(2);
  const data = await page.evaluate(() => JSON.parse(localStorage.getItem('stride-v1')).challenges);
  expect(data[0].logs).toHaveLength(1);
  expect(data[1].logs).toHaveLength(0);
  expect(data[1].seriesId).toBe(data[0].id);
});
test('weekly schedule, rest days, quick-add and reminder settings persist', async ({ page }) => {
  await page.goto('/');
  await page.getByRole('button', { name: '3 km a day' }).click();
  await page.getByText('Schedule & reminders', { exact: false }).click();
  await page.getByLabel('Sessions per week').selectOption('3');
  await page.getByLabel('Quick-add amount').fill('0.5');
  await page.getByLabel('Remind me when work remains').check();
  await page.getByLabel('Reminder around').fill('19:30');
  await page.getByRole('button', { name: 'Create challenge', exact: true }).click();
  await expect(page.locator('.schedule-caption')).toContainText('3 sessions / week');
  const c = await page.evaluate(() => JSON.parse(localStorage.getItem('stride-v1')).challenges[0]);
  expect(c.schedule.weekly).toBe(3);
  expect(c.quickAdd).toBe(0.5);
  expect(c.reminder).toEqual({ enabled: true, time: '19:30' });
});
test('Today, detail, and insights remain usable on narrow and wide screens', async ({ page }) => {
  await seed(page, [
    sample(),
    sample({ id: 'run', name: '3 km every day', mode: 'daily', goal: 3, unit: 'km', quickAdd: 1 }),
  ]);
  await page.screenshot({ path: 'artifacts/zelo-today-mobile.png', fullPage: true });
  for (const width of [360, 390, 768, 1280]) {
    await page.setViewportSize({ width, height: 900 });
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(
      true,
    );
  }
  await page.screenshot({ path: 'artifacts/zelo-today-desktop.png', fullPage: true });
  await page.setViewportSize({ width: 390, height: 844 });
  await page.locator('[data-detail]').first().click();
  await page.screenshot({ path: 'artifacts/zelo-detail-mobile.png', fullPage: true });
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
  await page.getByRole('button', { name: 'Insights', exact: true }).click();
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true);
});
