import {test,expect} from './fixtures.js';
test('add, open, edit, and remove a linked charity page without changing progress',async({page})=>{
 await page.addInitScript(()=>{window.StrideAndroid={openExternalLink:url=>window.lastExternalPage=url};});
 await page.goto('/');await page.getByRole('button',{name:'3,000 pushups',exact:true}).click();
 await page.locator('#link-options summary').click();await page.getByLabel('Connect to a page online').check();
 await page.getByLabel('Page type').selectOption('charity');await page.getByLabel('Event or challenge name (optional)').fill('November charity challenge');await page.getByLabel('Page address').fill('example.org/fundraising/november');
 await page.getByRole('button',{name:'Create challenge',exact:true}).click();await page.locator('[data-detail]').click();
 await expect(page.locator('.linked-page')).toContainText('Charity event');await expect(page.locator('.linked-page')).toContainText('November charity challenge');
 await page.getByRole('link',{name:'Open page',exact:false}).click();expect(await page.evaluate(()=>window.lastExternalPage)).toBe('https://example.org/fundraising/november');
 await page.getByRole('button',{name:'Edit',exact:true}).click();await expect(page.getByLabel('Page address')).toHaveValue('https://example.org/fundraising/november');await page.getByLabel('Connect to a page online').uncheck();await page.getByRole('button',{name:'Save changes',exact:true}).click();
 await expect(page.locator('.linked-page')).toHaveCount(0);expect(await page.evaluate(()=>JSON.parse(localStorage.getItem('stride-v1')).challenges[0].logs.length)).toBe(0);
});
test('invalid link is explained and repeating offers an explicit keep-link choice',async({page})=>{
 await page.goto('/');await page.getByRole('button',{name:'3 km a day',exact:true}).click();await page.locator('#link-options summary').click();await page.getByLabel('Connect to a page online').check();await page.getByLabel('Page address').fill('javascript:alert(1)');await page.getByRole('button',{name:'Create challenge',exact:true}).click();await expect(page.locator('#form-error')).toContainText('http or https');
 await page.getByLabel('Page address').fill('https://example.org/community');await page.getByRole('button',{name:'Create challenge',exact:true}).click();await page.locator('[data-detail]').click();
 const link=page.getByRole('link',{name:'Open page',exact:false});await expect(link).toHaveAttribute('target','_blank');await expect(link).toHaveAttribute('rel','noopener noreferrer');
 await page.screenshot({path:'artifacts/zelo-linked-event-mobile.png',fullPage:true});
 await page.getByRole('button',{name:'Repeat challenge',exact:true}).click();await expect(page.getByLabel('Keep the linked page')).not.toBeChecked();await page.getByLabel('Keep the linked page').check();await page.getByRole('button',{name:'Create repeat'}).click();await expect(page.locator('.linked-page')).toContainText('example.org');
});
