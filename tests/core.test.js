import { test } from 'node:test';
import assert from 'node:assert/strict';
import {
  stats,
  daysBetween,
  validDate,
  validateChallenge,
  parseBackup,
  dates,
  catchup,
  repeatChallenge,
  chartPoints,
  suggestedDeadline,
} from '../app/src/main/assets/www/core.js';
const base = {
  id: 'one',
  name: 'November pushups',
  mode: 'total',
  goal: 3000,
  unit: 'reps',
  start: '2025-11-01',
  end: '2025-11-30',
  logs: [],
};
const log = (date, amount, id = date) => ({ id, date, amount, note: '' });
test('inclusive dates and leap years', () => {
  assert.equal(daysBetween('2024-02-01', '2024-02-29'), 29);
  assert.equal(daysBetween('2025-11-01', '2025-11-30'), 30);
  assert.equal(validDate('2025-02-29'), false);
});
test('monthly target needs 100/day initially; remaining pace includes today', () => {
  assert.equal(stats(base, '2025-11-01').needed, 100);
  const s = stats({ ...base, logs: [log('2025-11-01', 600)] }, '2025-11-11');
  assert.equal(s.total, 600);
  assert.equal(s.remainingDays, 20);
  assert.equal(s.needed, 120);
  assert.equal(s.status, 'Behind pace');
});
test('multiple logs add up and future logs do not leak into historical stats', () => {
  const c = {
    ...base,
    logs: [log('2025-11-01', 30, 'a'), log('2025-11-01', 70, 'b'), log('2025-11-20', 50)],
  };
  assert.equal(stats(c, '2025-11-01').today, 100);
  assert.equal(stats(c, '2025-11-01').total, 100);
});
test('daily overshoot cannot compensate for a missed day', () => {
  const c = { ...base, mode: 'daily', goal: 3, logs: [log('2025-11-01', 6)] };
  const s = stats(c, '2025-11-03');
  assert.equal(s.hitDays, 1);
  assert.equal(s.missed, 1);
  assert.equal(s.progress, 1 / 30);
  assert.equal(s.completed, false);
});
test('daily logs aggregate before counting a successful day', () => {
  const c = {
    ...base,
    mode: 'daily',
    goal: 3,
    logs: [log('2025-11-01', 1, 'a'), log('2025-11-01', 2, 'b')],
  };
  assert.equal(stats(c, '2025-11-01').hitDays, 1);
  assert.equal(stats(c, '2025-11-01').streak, 1);
});
test('streak can continue from yesterday until today is complete', () => {
  const c = { ...base, logs: [log('2025-11-01', 100), log('2025-11-02', 100)] };
  assert.equal(stats(c, '2025-11-03').streak, 2);
  assert.equal(stats(c, '2025-11-04').streak, 0);
});
test('before start and after deadline have bounded stats', () => {
  assert.equal(stats(base, '2025-10-01').remainingDays, 30);
  assert.equal(stats(base, '2025-10-01').elapsed, 0);
  const s = stats(base, '2025-12-01');
  assert.equal(s.remainingDays, 0);
  assert.equal(s.needed, 0);
  assert.equal(s.status, 'Ended');
});
test('daily all days must succeed to complete', () => {
  const c = {
    ...base,
    mode: 'daily',
    goal: 3,
    end: '2025-11-02',
    logs: [log('2025-11-01', 3), log('2025-11-02', 3)],
  };
  assert.equal(stats(c, '2025-11-02').completed, true);
  assert.equal(stats(c, '2025-11-02').progress, 1);
});
test('validation rejects invalid amounts, reversed dates and logs outside challenge', () => {
  for (const patch of [
    { goal: 0 },
    { goal: NaN },
    { end: '2025-10-01' },
    { logs: [log('2025-10-31', 10)] },
    { logs: [log('2025-11-01', -1)] },
  ])
    assert.throws(() => validateChallenge({ ...base, ...patch }));
});
test('backup validates before replacing data and accepts legacy versions', () => {
  for (const version of [1, 2])
    assert.deepEqual(parseBackup(JSON.stringify({ version, challenges: [base] })), [base]);
  assert.throws(() => parseBackup(JSON.stringify({ version: 2, challenges: [base, base] })));
  assert.throws(() => parseBackup('{"version":3,"challenges":[]}'));
});
test('rest days are excluded from pacing and today target remains stable after partial logging', () => {
  const c = {
    ...base,
    start: '2025-11-03',
    end: '2025-11-09',
    goal: 500,
    schedule: { days: [1, 2, 3, 4, 5], weekly: 5 },
  };
  assert.equal(dates(c).length, 5);
  assert.equal(stats(c, '2025-11-03').due, 100);
  assert.equal(stats({ ...c, logs: [log('2025-11-03', 25)] }, '2025-11-03').due, 75);
  assert.equal(stats(c, '2025-11-08').due, 0);
});
test('three weekly sessions can occur on any allowed days without premature misses', () => {
  const c = {
    ...base,
    mode: 'daily',
    goal: 3,
    start: '2025-11-03',
    end: '2025-11-09',
    schedule: { days: [0, 1, 2, 3, 4, 5, 6], weekly: 3 },
    logs: [log('2025-11-03', 3), log('2025-11-05', 3), log('2025-11-07', 3)],
  };
  assert.equal(stats(c, '2025-11-04').missed, 0);
  const s = stats(c, '2025-11-08');
  assert.equal(s.required, 3);
  assert.equal(s.completed, true);
  assert.equal(s.due, 0);
  assert.equal(s.hitDays, 3);
  assert.equal(chartPoints(c, '2025-11-09').at(-1).planned, 9);
});
test('weekly extra sessions do not replace missed weeks; edge weeks cap quota', () => {
  const c = {
    ...base,
    mode: 'daily',
    goal: 3,
    start: '2025-11-03',
    end: '2025-11-16',
    schedule: { days: [0, 1, 2, 3, 4, 5, 6], weekly: 3 },
    logs: [3, 4, 5, 6, 7].map((d) => log(`2025-11-${String(d).padStart(2, '0')}`, 3)),
  };
  const s = stats(c, '2025-11-17');
  assert.equal(s.hitDays, 3);
  assert.equal(s.missed, 3);
  assert.equal(s.completed, false);
  const edge = { ...c, start: '2025-11-09', end: '2025-11-10', logs: [] };
  assert.equal(stats(edge, '2025-11-09').required, 2);
});
test('catch-up previews preserve logs; extending reduces remaining pace', () => {
  const c = { ...base, logs: [log('2025-11-01', 600)] };
  const a = catchup(c, c.end, '2025-11-11'),
    b = catchup(c, '2025-12-10', '2025-11-11');
  assert.equal(a.perSession, 120);
  assert.ok(b.perSession < a.perSession);
  assert.equal(c.end, '2025-11-30');
  assert.equal(c.logs.length, 1);
  assert.throws(() => catchup({ ...c, mode: 'daily' }, c.end, '2025-11-11'));
  assert.equal(suggestedDeadline(c, '2025-11-11'), '2025-12-04');
});
test('repeat creates clean linked chapter with same duration and schedule', () => {
  const c = { ...base, logs: [log('2025-11-01', 10)], reminder: { enabled: true, time: '18:00' } };
  const r = repeatChallenge(c, '2025-12-01', 'repeat');
  assert.equal(r.end, '2025-12-30');
  assert.equal(r.seriesId, c.id);
  assert.equal(r.logs.length, 0);
  assert.equal(r.reminder.enabled, false);
  assert.equal(c.logs.length, 1);
});
test('invalid schedules and reminder times cannot enter a backup', () => {
  for (const schedule of [
    { days: [], weekly: 1 },
    { days: [1, 1], weekly: 1 },
    { days: [7], weekly: 1 },
    { days: [1], weekly: 2 },
  ])
    assert.throws(() => validateChallenge({ ...base, schedule }));
  assert.throws(() => validateChallenge({ ...base, reminder: { enabled: true, time: '25:00' } }));
});

test('stored future-dated logs remain valid after a clock change and are excluded from earlier stats', () => {
  const c = { ...base, start: '2099-01-01', end: '2099-01-31', logs: [log('2099-01-02', 25)] };
  assert.doesNotThrow(() => parseBackup(JSON.stringify({ version: 2, challenges: [c] })));
  assert.equal(stats(c, '2099-01-01').total, 0);
  assert.equal(stats(c, '2099-01-02').total, 25);
});
