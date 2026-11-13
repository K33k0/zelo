import { test, expect } from './fixtures.js';
const base = {
  id: 'book',
  name: 'Read a book',
  mode: 'total',
  goal: 700,
  unit: 'pages',
  start: '2026-10-26',
  end: '2026-11-08',
  logs: [],
};
const log = (date, amount) => ({ id: date, date, amount, note: '' });
async function seed(page, c) {
  await page.clock.setFixedTime(new Date('2026-11-02T12:00:00Z'));
  await page.goto('/');
  await page.evaluate(
    (c) => localStorage.setItem('stride-v1', JSON.stringify({ version: 2, challenges: [c] })),
    c,
  );
  await page.reload();
}
test('neutral wording and ten rotating placeholders work for non-fitness goals', async ({
  page,
}) => {
  await seed(page, base);
  await page.getByRole('button', { name: 'Insights', exact: true }).click();
  await expect(page.locator('.heading>div>p')).toHaveText('Your goals, viewed over time.');
  await expect(page.locator('.card-stats')).toContainText('Scheduled days left');
  await expect(page.locator('main')).not.toContainText('Training');
  const placeholders = [];
  await page.getByRole('button', { name: 'Challenges', exact: true }).click();
  for (let i = 0; i < 10; i++) {
    await page.getByRole('button', { name: 'New challenge', exact: false }).click();
    placeholders.push(
      await page.getByLabel('Challenge name', { exact: true }).getAttribute('placeholder'),
    );
    await expect(page.getByLabel('Challenge name', { exact: true })).toHaveValue('');
    await page.getByRole('button', { name: 'Close dialog', exact: true }).click();
  }
  expect(new Set(placeholders).size).toBe(10);
  expect(placeholders.join(' ')).not.toMatch(/november|december|january/i);
});
test('flexible weekly review does not invent missed days for an early completed goal', async ({
  page,
}) => {
  await seed(page, { ...base, logs: [log('2026-10-26', 700)] });
  await page.getByRole('button', { name: 'Insights', exact: true }).click();
  await page.getByRole('button', { name: 'Last week', exact: true }).click();
  await expect(page.locator('.review-period')).toContainText('Oct 26');
  await expect(page.locator('.review-period')).toContainText('Nov 1');
  await expect(page.locator('.review-row')).toContainText('Overall goal was complete');
  await expect(page.locator('.review-row')).not.toContainText('missed');
  await page.screenshot({ path: 'artifacts/zelo-23-weekly-review.png', fullPage: true });
});
test('weekly quota review distinguishes current remaining targets from last-week misses', async ({
  page,
}) => {
  await seed(page, {
    ...base,
    mode: 'daily',
    goal: 10,
    schedule: { days: [1, 2, 3, 4, 5], weekly: 3 },
    logs: [log('2026-10-26', 10), log('2026-10-27', 10), log('2026-11-02', 10)],
  });
  await page.getByRole('button', { name: 'Insights', exact: true }).click();
  await expect(page.locator('.review-period')).toContainText('Nov 2');
  await expect(page.locator('.review-row')).toContainText('1 of 3 target days met');
  await expect(page.locator('.review-row')).toContainText('2 more target days');
  await expect(page.locator('.review-row')).not.toContainText('missed');
  await page.getByRole('button', { name: 'Last week', exact: true }).click();
  await expect(page.locator('.review-row')).toContainText('2 of 3 target days met · 1 missed');
});
test('reminder tone information appears only with the opt-in reminder controls', async ({
  page,
}) => {
  await seed(page, base);
  await page.getByRole('button', { name: 'Challenges', exact: true }).click();
  await page.getByRole('button', { name: 'New challenge', exact: false }).click();
  await page.locator('#schedule-options summary').click();
  await expect(page.getByText('Cheeky reminders', { exact: false })).not.toBeVisible();
  await page.getByLabel('Remind me when work remains').check();
  await expect(page.getByText('Cheeky reminders', { exact: false })).toBeVisible();
});
test('unscheduled activity does not invent targets in a partial week', async ({ page }) => {
  await seed(page, {
    ...base,
    mode: 'daily',
    goal: 10,
    start: '2026-10-27',
    end: '2026-11-02',
    schedule: { days: [1], weekly: 1 },
    logs: [log('2026-10-28', 10)],
  });
  await page.getByRole('button', { name: 'Insights', exact: true }).click();
  await page.getByRole('button', { name: 'Last week', exact: true }).click();
  await expect(page.locator('.review-row')).toContainText('10 pages logged on 1 day');
  await expect(page.locator('.review-row')).toContainText('No targets scheduled');
  await expect(page.locator('.review-row')).not.toContainText('0 of 0');
});
