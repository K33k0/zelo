import {test} from 'node:test';
import assert from 'node:assert/strict';
import {challengeLink,validateChallenge,parseBackup,repeatChallenge} from '../app/src/main/assets/www/core.js';
const base={id:'link',name:'Pushups',mode:'total',goal:3000,unit:'reps',start:'2026-11-01',end:'2026-11-30',logs:[]};
test('linked pages accept common copied addresses and preserve path/query',()=>{const link=challengeLink('charity',' Charity challenge ','example.org/events/november?team=ecletra#join');assert.equal(link.url,'https://example.org/events/november?team=ecletra#join');assert.equal(link.label,'Charity challenge');assert.equal(validateChallenge({...base,link}).link,link);});
test('links reject unsafe schemes, credentials, missing addresses and invalid categories',()=>{for(const url of ['javascript:alert(1)','file:///sdcard/test','intent://example','https://user:pass@example.org',''])assert.throws(()=>challengeLink('online','Challenge',url));assert.throws(()=>challengeLink('unknown','Challenge','https://example.org'));});
test('backup round-trip retains links while old challenges remain valid',()=>{const c={...base,id:'second',link:challengeLink('social','Community challenge','https://example.org/post')};assert.deepEqual(parseBackup(JSON.stringify({version:2,challenges:[base,c]})),[base,c]);});
test('repeating does not silently carry forward an old event link',()=>{const c={...base,link:challengeLink('event','November race','https://example.org/race')};const next=repeatChallenge(c,'2026-12-01','repeat','month');assert.equal(next.link,undefined);assert.ok(c.link);});
