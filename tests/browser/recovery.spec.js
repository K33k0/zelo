import {test,expect} from './fixtures.js';
const good={version:2,challenges:[{id:'recovered',name:'Recovered challenge',mode:'total',goal:3000,unit:'reps',start:'2026-11-01',end:'2026-11-30',logs:[]}]};
async function restore(page){await page.evaluate(data=>window.restoreBackup(JSON.stringify(data)),good);await page.getByRole('button',{name:'Replace and restore'}).click();}
test('unreadable browser data cannot be overwritten by ordinary saves and is preserved on restore',async({page})=>{
 await page.goto('/');await page.evaluate(()=>localStorage.setItem('stride-v1','broken JSON'));await page.reload();
 await page.getByRole('button',{name:'Close dialog'}).click();await page.getByRole('button',{name:'3,000 pushups',exact:true}).click();await page.getByRole('button',{name:'Create challenge',exact:true}).click();
 expect(await page.evaluate(()=>localStorage.getItem('stride-v1'))).toBe('broken JSON');await expect(page.locator('#dialog-status')).toContainText('Restore a backup');
 await restore(page);expect(await page.evaluate(()=>localStorage.getItem('stride-recovery-v1'))).toBe('broken JSON');await expect(page.locator('.card-title')).toContainText('Recovered challenge');
});
for(const raw of ['broken JSON',JSON.stringify({version:2,revision:7,challenges:[{id:'invalid'}]})])test(`native recovery replaces unreadable data using its exact snapshot: ${raw.slice(0,15)}`,async({page})=>{
 await page.addInitScript(raw=>{if(!localStorage.getItem('native-seeded')){localStorage.setItem('native-seeded','1');localStorage.setItem('native',raw);}window.StrideAndroid={getData:()=>localStorage.getItem('native'),saveData:()=>{throw Error('Recovery must not use ordinary save');},restoreData:(text,expected)=>{const old=localStorage.getItem('native');if(old!==expected)return -1;localStorage.setItem('original',old);const data=JSON.parse(text);data.revision=8;localStorage.setItem('native',JSON.stringify(data));return 8;}};},raw);
 await page.goto('/');await restore(page);expect(await page.evaluate(()=>localStorage.getItem('original'))).toBe(raw);await expect(page.locator('.card-title')).toContainText('Recovered challenge');
});
test('native recovery refuses to overwrite a changed snapshot',async({page})=>{
 await page.addInitScript(()=>{localStorage.setItem('native','broken');window.StrideAndroid={getData:()=>localStorage.getItem('native'),saveData:()=>-1,restoreData:(text,expected)=>localStorage.getItem('native')===expected?1:-1};});await page.goto('/');
 await page.evaluate(data=>window.restoreBackup(JSON.stringify(data)),good);await page.evaluate(()=>localStorage.setItem('native',JSON.stringify({version:2,revision:9,challenges:[]})));await page.getByRole('button',{name:'Replace and restore'}).click();expect(await page.evaluate(()=>JSON.parse(localStorage.getItem('native')).revision)).toBe(9);await expect(page.locator('#toast')).toContainText('Progress changed elsewhere');
});
test('backwards clock preserves existing logs but rejects new future-dated entries',async({page})=>{
 await page.goto('/');await page.evaluate(data=>{data.challenges[0].logs=[{id:'later',date:'2026-11-02',amount:100,note:''}];localStorage.setItem('stride-v1',JSON.stringify(data));},good);await page.reload();await expect(page.getByRole('heading',{name:'Saved data needs attention'})).toHaveCount(0);
 await page.getByRole('button',{name:'Log progress',exact:true}).click();await page.getByLabel('How many reps?').fill('25');await page.getByLabel('Date',{exact:true}).fill('2026-11-02');await page.locator('#log-form').evaluate(form=>form.dispatchEvent(new Event('submit',{bubbles:true,cancelable:true})));await expect(page.locator('#form-error')).toContainText('today or an earlier date');
 await page.getByLabel('Date',{exact:true}).fill('2026-11-01');await page.getByRole('button',{name:'Save progress',exact:true}).click();expect(await page.evaluate(()=>JSON.parse(localStorage.getItem('stride-v1')).challenges[0].logs.length)).toBe(2);
});
test('capacity calendar uses the same original daily target as the streak',async({page})=>{
 await page.clock.setFixedTime(new Date('2026-11-02T12:00:00Z'));await page.goto('/');await page.evaluate(()=>localStorage.setItem('stride-v1',JSON.stringify({version:2,challenges:[{id:'cap',name:'Capacity',mode:'total',goal:900,unit:'reps',start:'2026-11-02',end:'2026-11-08',capacity:{weekday:100,weekend:200},logs:[{id:'one',date:'2026-11-02',amount:100,note:''}]}]})));await page.reload();await page.locator('[data-detail="cap"]').click();await expect(page.locator('.calendar .day').first()).toHaveClass(/hit/);await expect(page.locator('.stat-box').filter({hasText:'Current streak'}).locator('strong')).toHaveText('1');
});
