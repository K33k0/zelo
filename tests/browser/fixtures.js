import {test as base,expect} from '@playwright/test';
// Keep existing goal fixtures independent of the calendar date on the test machine.
export const test=base.extend({page:async({page},use)=>{await page.clock.setFixedTime(new Date('2026-11-01T12:00:00Z'));await use(page);}});
export {expect};
