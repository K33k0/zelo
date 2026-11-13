import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'./tests/browser',use:{baseURL:'http://127.0.0.1:4173',timezoneId:'Europe/London',viewport:{width:390,height:844}},webServer:{command:'node scripts/serve.js',url:'http://127.0.0.1:4173',reuseExistingServer:true},reporter:'list'});
