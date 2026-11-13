import fs from 'node:fs';
import {stats,addDays} from '../app/src/main/assets/www/core.js';
const fixtures=[];
for(const mode of ['total','daily'])for(const schedule of [undefined,{days:[1,2,3,4,5],weekly:5},{days:[0,1,2,3,4,5,6],weekly:3},{days:[1,3,5],weekly:2}])for(const capacity of [undefined,{weekday:10,weekend:30},{weekday:1,weekend:2}])for(const amounts of [[0,0,0],[3,6,0],[25,50,200]]){
 const c={id:'test',name:'Test',mode,goal:mode==='total'?300:3,unit:'reps',start:'2025-11-01',end:'2025-11-30',logs:amounts.map((amount,i)=>({id:String(i),amount,date:addDays('2025-11-01',i),note:''})).filter(l=>l.amount>0),...(schedule?{schedule}:{}),...(capacity&&mode==='total'?{capacity}:{})};
 for(const date of ['2025-10-31','2025-11-01','2025-11-02','2025-11-04','2025-11-10','2025-12-01']){const s=stats(c,date);fixtures.push({c,date,expected:{total:s.total,today:s.today,due:s.due,target:s.target,progress:s.progress,complete:s.completed,scheduled:s.scheduledToday}});}
}
fs.mkdirSync('.tools/native-tests',{recursive:true});fs.writeFileSync('.tools/native-tests/fixtures.json',JSON.stringify(fixtures));
console.log(`${fixtures.length} native parity scenarios generated.`);
