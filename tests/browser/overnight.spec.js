import { test, expect } from '@playwright/test';
const challenge = {
  id: 'overnight',
  name: 'Daily distance',
  mode: 'daily',
  goal: 3,
  unit: 'km',
  start: '2026-11-01',
  end: '2026-11-30',
  logs: [{ id: 'yesterday', date: '2026-11-01', amount: 3, note: 'Evening run' }],
};
async function seed(page) {
  await page.clock.install({ time: new Date('2026-11-01T23:59:50Z') });
  await page.goto('/');
  await page.evaluate(
    (c) => localStorage.setItem('stride-v1', JSON.stringify({ version: 2, challenges: [c] })),
    challenge,
  );
  await page.reload();
  await expect(page.locator('.done-row')).toContainText('3 km');
}
test('foreground screen rolls over immediately at midnight', async ({ page }) => {
  await seed(page);
  await page.clock.fastForward(11000);
  await expect(page.locator('.heading .eyebrow')).toContainText('2');
  await expect(page.locator('.today-amount strong')).toHaveText('3');
  await expect(page.locator('.mini-route')).toContainText('0 logged today');
});
for (const event of ['focus', 'pageshow', 'visibilitychange'])
  test(`returning after suspended timers refreshes the day on ${event}`, async ({ page }) => {
    await seed(page);
    await page.clock.setSystemTime(new Date('2026-11-02T08:00:00Z'));
    await page.evaluate((event) => {
      (event === 'visibilitychange' ? document : window).dispatchEvent(new Event(event));
    }, event);
    await expect(page.locator('.today-amount strong')).toHaveText('3');
    await expect(page.locator('.done-row')).toHaveCount(0);
  });
test('an unfinished default-date log follows today without losing amount or notes', async ({
  page,
}) => {
  await seed(page);
  await page.getByRole('button', { name: 'Challenges', exact: true }).click();
  await page.getByRole('button', { name: 'Log progress', exact: false }).click();
  await page.getByLabel('How many km?').fill('1.5');
  await page.getByLabel('A note (optional)').fill('Keep this draft');
  await page.clock.setSystemTime(new Date('2026-11-02T08:00:00Z'));
  await page.evaluate(() => window.dispatchEvent(new Event('focus')));
  await expect(page.getByLabel('Date', { exact: true })).toHaveValue('2026-11-02');
  await expect(page.getByLabel('How many km?')).toHaveValue('1.5');
  await expect(page.getByLabel('A note (optional)')).toHaveValue('Keep this draft');
  await page.getByRole('button', { name: 'Save progress', exact: true }).click();
  const entries = await page.evaluate(
    () => JSON.parse(localStorage.getItem('stride-v1')).challenges[0].logs,
  );
  expect(entries[0].date).toBe('2026-11-01');
  expect(entries[1].date).toBe('2026-11-02');
});
test('an explicitly chosen date and an existing log keep their dates overnight', async ({
  page,
}) => {
  await seed(page);
  await page.getByRole('button', { name: 'Challenges', exact: true }).click();
  await page.getByRole('button', { name: 'Log progress', exact: false }).click();
  await page.getByLabel('Date', { exact: true }).fill('2026-11-01');
  await page.clock.setSystemTime(new Date('2026-11-02T08:00:00Z'));
  await page.evaluate(() => window.dispatchEvent(new Event('focus')));
  await expect(page.getByLabel('Date', { exact: true })).toHaveValue('2026-11-01');
  await expect(page.getByLabel('Date', { exact: true })).toHaveAttribute('max', '2026-11-02');
  await page.getByRole('button', { name: 'Close dialog' }).click();
  await page.getByRole('button', { name: 'Activity', exact: true }).click();
  await page.getByRole('button', { name: 'Edit log', exact: true }).click();
  await page.clock.setSystemTime(new Date('2026-11-03T08:00:00Z'));
  await page.evaluate(() => window.onAppResume());
  await expect(page.getByLabel('Date', { exact: true })).toHaveValue('2026-11-01');
});
test('Android resume refreshes Today even without a native data bridge', async ({ page }) => {
  await seed(page);
  await page.clock.setSystemTime(new Date('2026-11-02T08:00:00Z'));
  await page.evaluate(() => window.onAppResume());
  await expect(page.locator('.today-amount strong')).toHaveText('3');
  await expect(page.locator('.heading .eyebrow')).toContainText('2');
});
test('first action after sleeping rechecks the date before saving a log', async ({ page }) => {
  await seed(page);
  await page.getByRole('button', { name: 'Challenges', exact: true }).click();
  await page.getByRole('button', { name: 'Log progress', exact: false }).click();
  await page.getByLabel('How many km?').fill('1');
  await page.clock.setSystemTime(new Date('2026-11-02T08:00:00Z'));
  await page.getByRole('button', { name: 'Save progress', exact: true }).click();
  const entries = await page.evaluate(
    () => JSON.parse(localStorage.getItem('stride-v1')).challenges[0].logs,
  );
  expect(entries.at(-1).date).toBe('2026-11-02');
});
test('month rollover removes ended challenges from Today without changing history', async ({
  page,
}) => {
  await seed(page);
  await page.clock.setSystemTime(new Date('2026-12-01T08:00:00Z'));
  await page.evaluate(() => window.onAppResume());
  await expect(page.locator('.mission-card')).toHaveCount(0);
  await expect(page.locator('.heading .eyebrow')).toContainText('December');
  expect(
    await page.evaluate(
      () => JSON.parse(localStorage.getItem('stride-v1')).challenges[0].logs[0].date,
    ),
  ).toBe('2026-11-01');
});
