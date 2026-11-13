import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  allocate,
  stats,
  catchup,
  repeatChallenge,
  suggestedDeadline,
  weeklyReview,
  validateChallenge,
} from '../app/src/main/assets/www/core.js';
const base = {
  id: 'plan',
  name: 'Pushups',
  mode: 'total',
  goal: 3000,
  unit: 'reps',
  start: '2026-11-01',
  end: '2026-11-30',
  logs: [],
};
test('catch-up preview agrees with Today after a partial log', () => {
  const c = { ...base, logs: [{ id: 'a', date: '2026-11-01', amount: 25, note: '' }] };
  const preview = catchup(c, c.end, '2026-11-01');
  assert.equal(preview.todayRemaining, 75);
  assert.equal(preview.todayRemaining, stats(c, '2026-11-01').due);
  assert.equal(preview.future[0].amount, 100);
  assert.equal(suggestedDeadline(c, '2026-11-01'), c.end);
});
test('capacity allocation respects weekdays and uses weekend room', () => {
  const c = {
    ...base,
    start: '2026-11-02',
    end: '2026-11-08',
    goal: 900,
    capacity: { weekday: 100, weekend: 200 },
  };
  const ds = [
    '2026-11-02',
    '2026-11-03',
    '2026-11-04',
    '2026-11-05',
    '2026-11-06',
    '2026-11-07',
    '2026-11-08',
  ];
  const p = allocate(c, c.goal, ds);
  assert.equal(p.shortfall, 0);
  assert.equal(p.amounts['2026-11-02'], 100);
  assert.equal(p.amounts['2026-11-07'], 200);
  assert.equal(
    Object.values(p.amounts).reduce((s, n) => s + n, 0),
    900,
  );
});
test('missed work exceeding limits produces a clear shortfall and a usable suggested date', () => {
  const c = {
    ...base,
    goal: 500,
    start: '2026-11-02',
    end: '2026-11-06',
    capacity: { weekday: 100, weekend: 100 },
  };
  assert.equal(stats(c, '2026-11-03').capacityShortfall, 100);
  const end = suggestedDeadline(c, '2026-11-03');
  assert.equal(end, '2026-11-07');
  assert.equal(catchup(c, end, '2026-11-03').shortfall, 0);
});
test('extra actual effort can rescue a capacity shortfall without changing limits', () => {
  const c = {
    ...base,
    goal: 500,
    start: '2026-11-02',
    end: '2026-11-06',
    capacity: { weekday: 100, weekend: 100 },
    logs: [{ id: 'extra', date: '2026-11-03', amount: 200, note: '' }],
  };
  assert.equal(stats(c, '2026-11-03').capacityShortfall, 0);
});
test('calendar-month repeats handle December and leap February; duration remains an option', () => {
  assert.equal(repeatChallenge(base, '2026-12-01', 'b', 'month').end, '2026-12-31');
  assert.equal(repeatChallenge(base, '2028-02-01', 'b', 'month').end, '2028-02-29');
  assert.equal(repeatChallenge(base, '2026-12-01', 'b', 'duration').end, '2026-12-30');
});
test('weekly review separates missed targets from current-week work', () => {
  const c = {
    ...base,
    goal: 3,
    mode: 'daily',
    start: '2026-10-26',
    end: '2026-11-08',
    schedule: { days: [1, 2, 3, 4, 5], weekly: 5 },
    logs: [
      { id: 'a', date: '2026-10-26', amount: 3, note: '' },
      { id: 'b', date: '2026-10-27', amount: 3, note: '' },
      { id: 'c', date: '2026-10-28', amount: 3, note: '' },
    ],
  };
  const r = weeklyReview(c, '2026-11-02');
  assert.equal(r.completed, 3);
  assert.equal(r.missed, 2);
  assert.equal(r.nextSessions, 5);
  assert.equal(r.total, 9);
});
test('limits cannot be negative, fractional beyond UI precision, or attached to strict sessions', () => {
  for (const patch of [
    { capacity: { weekday: -1, weekend: 100 } },
    { capacity: { weekday: 0.001, weekend: 100 } },
    { mode: 'daily', capacity: { weekday: 100, weekend: 100 } },
  ])
    assert.throws(() => validateChallenge({ ...base, ...patch }));
});
