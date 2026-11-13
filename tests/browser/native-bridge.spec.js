import {test,expect} from './fixtures.js';
test('legacy data migrates to native storage and widget updates cannot be overwritten by stale forms',async({page})=>{
 await page.addInitScript(()=>{
  if(!localStorage.getItem('test-seeded')){localStorage.setItem('test-seeded','1');localStorage.setItem('stride-v1',JSON.stringify({version:1,challenges:[{id:'legacy',name:'Legacy pushups',mode:'total',goal:3000,unit:'reps',start:'2026-11-01',end:'2026-11-30',logs:[{id:'old',date:'2026-11-01',amount:25,note:'Preserve me'}]}]}));}
  window.StrideAndroid={getData:()=>localStorage.getItem('test-native'),saveData:(raw,expected)=>{const old=JSON.parse(localStorage.getItem('test-native')||'{"revision":0}');if(old.revision!==expected)return -1;const next=JSON.parse(raw);next.revision=expected+1;localStorage.setItem('test-native',JSON.stringify(next));return next.revision;}};
 });
 await page.goto('/');await expect(page.locator('.today-amount strong')).toHaveText('75');
 await page.getByRole('button',{name:'Log progress',exact:true}).click();await page.getByLabel('How many reps?').fill('10');
 await page.evaluate(()=>{const d=JSON.parse(localStorage.getItem('test-native'));d.challenges[0].logs.push({id:'widget',date:'2026-11-01',amount:25,note:'Widget update'});d.revision++;localStorage.setItem('test-native',JSON.stringify(d));window.refreshNative();});
 await page.getByRole('button',{name:'Save progress',exact:true}).click();
 await expect(page.locator('#toast')).toContainText('A widget updated your progress');
 const d=await page.evaluate(()=>JSON.parse(localStorage.getItem('test-native')));expect(d.challenges[0].logs).toHaveLength(2);expect(d.challenges[0].logs[0].note).toBe('Preserve me');
 await expect(page.locator('.today-amount strong')).toHaveText('50');
});
