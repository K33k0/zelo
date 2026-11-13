export const localDate=d=>`${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
export const today=()=>localDate(new Date());
export const dayNumber=s=>Date.parse(s+'T00:00:00Z')/86400000;
export const fromDay=n=>new Date(n*86400000).toISOString().slice(0,10);
export const addDays=(s,n)=>fromDay(dayNumber(s)+n);
export const daysBetween=(a,b)=>dayNumber(b)-dayNumber(a)+1;
export const weekday=s=>new Date(s+'T00:00:00Z').getUTCDay();
export const fmt=n=>new Intl.NumberFormat(undefined,{maximumFractionDigits:2}).format(Math.abs(n)<0.005?0:n);
export const roundUp=n=>{const value=Math.ceil((n-1e-9)*100)/100;return value===0?0:value;};
const nonNegative=n=>n<=1e-9?0:Math.max(0,n);
export const validDate=s=>typeof s==='string'&&/^\d{4}-\d{2}-\d{2}$/.test(s)&&Number.isFinite(dayNumber(s))&&fromDay(dayNumber(s))===s;
export const linkKinds={charity:'Charity event',online:'Online challenge',social:'Social challenge',event:'Other event'};
export function challengeLink(kind,label,value){
 const text=value.trim();let url;
 try{url=new URL(/^[a-z][a-z\d+.-]*:/i.test(text)?text:'https://'+text);}catch{throw Error('Enter a valid page address, such as https://example.com/challenge.');}
 if(!['http:','https:'].includes(url.protocol)||!url.hostname||url.username||url.password||url.href.length>2048)throw Error('Use an http or https page address without a username or password.');
 const result={kind,label:label.trim(),url:url.href};validateLink(result);return result;
}
function validateLink(link){
 if(!link||!Object.hasOwn(linkKinds,link.kind)||typeof link.label!=='string'||link.label.length>80||typeof link.url!=='string'||link.url.length>2048)throw Error('Check the linked page’s name, type, and address.');
 let url;try{url=new URL(link.url);}catch{throw Error('The linked page needs a valid web address.');}
 if(!['http:','https:'].includes(url.protocol)||!url.hostname||url.username||url.password)throw Error('The linked page must use http or https without a username or password.');
}
export const trainingDays=c=>c.schedule?.days||[0,1,2,3,4,5,6];
export const scheduled=(c,d)=>d>=c.start&&d<=c.end&&trainingDays(c).includes(weekday(d));
export function dates(c){const result=[];for(let n=dayNumber(c.start);n<=dayNumber(c.end);n++){const d=fromDay(n);if(scheduled(c,d))result.push(d);}return result;}
export const weekStart=d=>addDays(d,-((weekday(d)+6)%7));
function groups(c,ds){const map=new Map();for(const d of ds){const w=weekStart(d);if(!map.has(w))map.set(w,[]);map.get(w).push(d);}return [...map.values()].map(days=>({days,quota:Math.min(c.schedule?.weekly||7,days.length)}));}
export const dayCapacity=(c,d)=>c.capacity?(weekday(d)===0||weekday(d)===6?c.capacity.weekend:c.capacity.weekday):Infinity;
// Equal effort where possible, capped by the user's own weekday/weekend limits.
export function allocate(c,amount,ds){
 const result={};if(!ds.length)return {amounts:result,shortfall:Math.max(0,amount)};
 const ordered=ds.map(date=>({date,cap:dayCapacity(c,date)})).sort((a,b)=>a.cap-b.cap);let left=Math.max(0,amount);
 ordered.forEach((item,i)=>{const share=Math.min(item.cap,left/(ordered.length-i));result[item.date]=share;left=Math.max(0,left-share);});
 return {amounts:result,shortfall:left<1e-7?0:left};
}
export function stats(c,date=today()){
 const ds=dates(c),logs=c.logs.filter(l=>l.date<=date),byDay={};logs.forEach(l=>byDay[l.date]=(byDay[l.date]||0)+l.amount);
 const total=logs.reduce((s,l)=>s+l.amount,0),duration=daysBetween(c.start,c.end),elapsed=Math.max(0,Math.min(duration,daysBetween(c.start,date)));
 const daily=c.mode==='daily',periods=groups(c,ds),required=periods.reduce((s,p)=>s+p.quota,0);
 const success=d=>(byDay[d]||0)+1e-9>=c.goal;
 const hitDays=periods.reduce((s,p)=>s+Math.min(p.quota,p.days.filter(success).length),0);
 const missed=daily?periods.reduce((s,p)=>s+Math.max(0,p.quota-p.days.filter(d=>d<date&&success(d)).length-p.days.filter(d=>d>=date).length),0):0;
 const target=daily?required*c.goal:c.goal,remainingDays=ds.filter(d=>d>=date).length,remaining=nonNegative(target-total);
 const pastTotal=logs.filter(l=>l.date<date).reduce((s,l)=>s+l.amount,0),todayAmount=byDay[date]||0;
 const thisWeek=periods.find(p=>p.days.includes(date));
 const weekHits=thisWeek?thisWeek.days.filter(success).length:0;
 const weekDone=daily&&thisWeek&&weekHits>=thisWeek.quota;
 const originalPlan=allocate(c,c.goal,ds).amounts,remainingPlan=allocate(c,c.goal-pastTotal,ds.filter(d=>d>=date));
 const baseline=c.goal/ds.length;
 const todayTarget=daily?c.goal:remainingPlan.amounts[date]||0;
 const due=scheduled(c,date)&&!weekDone?nonNegative(todayTarget-todayAmount):0;
 const capacityShortfall=daily?0:allocate(c,Math.max(0,remaining-due),ds.filter(d=>d>date)).shortfall;
 const completed=daily?hitDays===required:total+1e-9>=target;
 let streak=0;const past=ds.filter(d=>d<=date);if(past.at(-1)===date&&!((byDay[date]||0)+1e-9>=(daily?c.goal:originalPlan[date])))past.pop();
 for(let i=past.length-1;i>=0;i--){if((byDay[past[i]]||0)+1e-9>=(daily?c.goal:originalPlan[past[i]]))streak++;else break;}
 const behind=daily?missed>0:total+1e-9<ds.filter(d=>d<date).reduce((sum,d)=>sum+originalPlan[d],0);
 const sessionElapsed=ds.filter(d=>d<=date).length,average=sessionElapsed?total/sessionElapsed:0;
 let forecast=null;
 if(!daily&&average>0&&!completed){let n=dayNumber(date),left=remaining/average;while(left>0&&n<dayNumber(date)+3660){n++;if(trainingDays(c).includes(weekday(fromDay(n))))left--;}if(left<=0)forecast=fromDay(n);}
 return {duration,total,target,remainingDays,elapsed,sessionElapsed,byDay,hitDays,required,completed,streak,missed,today:todayAmount,remaining,needed:remainingDays?remaining/remainingDays:0,todayTarget,due,weekDone,baseline,forecast,average,capacityShortfall,scheduledToday:scheduled(c,date),progress:Math.min(1,daily?hitDays/required:total/target),status:completed?'Completed':date<c.start?'Upcoming':date>c.end?'Ended':behind?'Behind pace':'On track'};
}
export function chartPoints(c,date=today()){
 const ds=dates(c),s=stats(c,date),points=[],rates={},original=allocate(c,c.goal,ds).amounts;let actual=0,planned=0;
 for(const p of groups(c,ds))for(const d of p.days)rates[d]=c.goal*p.quota/p.days.length;
 for(let n=dayNumber(c.start);n<=dayNumber(c.end);n++){const d=fromDay(n);actual+=s.byDay[d]||0;if(scheduled(c,d))planned+=c.mode==='daily'?rates[d]:original[d];points.push({date:d,planned,actual:d<=date?actual:null});}
 return points;
}
export function catchup(c,end=c.end,date=today()){
 if(c.mode!=='total')throw Error('Strict targets keep missed days. Repeat or edit your schedule instead.');
 const next={...c,end};validateChallenge(next);const s=stats(next,date);
 if(!s.remainingDays)throw Error('Choose a deadline with at least one remaining training day.');
 const future=dates(next).filter(d=>d>date),futurePlan=allocate(next,Math.max(0,s.remaining-s.due),future);
 return {end,perSession:roundUp(s.todayTarget||s.needed),todayRemaining:roundUp(s.due),sessions:s.remainingDays,remaining:s.remaining,shortfall:futurePlan.shortfall,future:future.map(d=>({date:d,amount:roundUp(futurePlan.amounts[d])}))};
}
export function suggestedDeadline(c,date=today()){
 const s=stats(c,date);let left=s.remaining,d=date<c.start?c.start:date;
 if(!left)return c.end;for(let i=0;i<3660;i++,d=addDays(d,1)){if(trainingDays(c).includes(weekday(d)))left-=Math.max(0,(c.capacity?dayCapacity(c,d):s.baseline)-(d===date?s.today:0));if(left<=1e-7)return d;}return c.end;
}
export const monthEnd=start=>{const d=new Date(start+'T00:00:00Z');return fromDay(Date.UTC(d.getUTCFullYear(),d.getUTCMonth()+1,0)/86400000);};
export function repeatChallenge(c,start,id,kind='duration'){return {...c,id,start,end:kind==='month'?monthEnd(start):addDays(start,daysBetween(c.start,c.end)-1),logs:[],link:undefined,seriesId:c.seriesId||c.id,adjustments:[],reminder:{enabled:false,time:c.reminder?.time||'18:00'},createdAt:Date.now()};}
export function weeklyReview(c,date=today()){
 const start=addDays(weekStart(date),-7),end=addDays(start,6),ds=dates(c).filter(d=>d>=start&&d<=end);
 const entries=c.logs.filter(l=>l.date>=start&&l.date<=end&&l.date<=date),byDay={};entries.forEach(l=>byDay[l.date]=(byDay[l.date]||0)+l.amount);
 const expected=c.mode==='daily'?Math.min(c.schedule?.weekly||7,ds.length):ds.length;
 const original=allocate(c,c.goal,dates(c)).amounts;
 const completed=Math.min(expected,ds.filter(d=>(byDay[d]||0)+1e-9>=(c.mode==='daily'?c.goal:original[d])).length);
 const plannedToEnd=dates(c).filter(d=>d<=end).reduce((n,d)=>n+original[d],0),totalToEnd=c.logs.filter(l=>l.date<=end&&l.date<=date).reduce((n,l)=>n+l.amount,0);
 const delta=totalToEnd-plannedToEnd,paceDifference=Math.abs(delta)<1e-9?0:delta;
 const nextStart=weekStart(date),nextEnd=addDays(nextStart,6),nextDates=dates(c).filter(d=>d>=nextStart&&d<=nextEnd),s=stats(c,date);
 const futurePlan=allocate(c,Math.max(0,s.remaining-s.due),dates(c).filter(d=>d>date));
 const nextRemaining=s.due+nextDates.filter(d=>d>date).reduce((n,d)=>n+(futurePlan.amounts[d]||0),0);
 const currentEntries=c.logs.filter(l=>l.date>=nextStart&&l.date<=nextEnd&&l.date<=date);
 const nextSessions=c.mode==='daily'?Math.min(c.schedule?.weekly||7,nextDates.length):nextDates.length;
 const nextCompleted=c.mode==='daily'?Math.min(nextSessions,nextDates.filter(d=>(s.byDay[d]||0)+1e-9>=c.goal).length):0;
 const nextRemainingSessions=Math.max(0,nextSessions-nextCompleted),nextOpenDates=nextDates.filter(d=>d>=date&&(s.byDay[d]||0)+1e-9<c.goal).length;
 return {start,end,expected,completed,missed:c.mode==='daily'?Math.max(0,expected-completed):0,total:entries.reduce((n,l)=>n+l.amount,0),activeDays:new Set(entries.map(l=>l.date)).size,paceDifference,completedAtEnd:totalToEnd+1e-9>=c.goal,nextStart,nextEnd,currentTotal:currentEntries.reduce((n,l)=>n+l.amount,0),currentActiveDays:new Set(currentEntries.map(l=>l.date)).size,nextSessions,nextCompleted,nextRemainingSessions,nextAvailableSessions:Math.min(nextRemainingSessions,nextOpenDates),nextMissed:Math.max(0,nextRemainingSessions-nextOpenDates),nextTrainingDays:nextDates.filter(d=>d>=date).length,nextRemaining:roundUp(nextRemaining),todayRemaining:roundUp(s.due),ended:c.end<date};
}
export function validateChallenge(c){
 if(!c||typeof c.name!=='string'||!c.name.trim()||c.name.length>80)throw Error('Enter a challenge name (up to 80 characters).');
 if(!['total','daily'].includes(c.mode)||!Number.isFinite(c.goal)||c.goal<=0||c.goal>1e9)throw Error('Enter a goal greater than zero, up to one billion.');
 if(typeof c.unit!=='string'||!c.unit.trim()||c.unit.length>20)throw Error('Enter a unit, such as reps or km.');
 if(!validDate(c.start)||!validDate(c.end)||c.end<c.start||daysBetween(c.start,c.end)>3660)throw Error('Choose an end date after the start, within 10 years.');
 if(typeof c.id!=='string'||!c.id||!Array.isArray(c.logs)||c.logs.length>100000)throw Error('Invalid challenge data.');
 if(c.schedule){const ds=c.schedule.days;if(!Array.isArray(ds)||!ds.length||new Set(ds).size!==ds.length||ds.some(d=>!Number.isInteger(d)||d<0||d>6)||!Number.isInteger(c.schedule.weekly)||c.schedule.weekly<1||c.schedule.weekly>ds.length)throw Error('Choose training days and a weekly target within that number of days.');}
 if(!dates(c).length)throw Error('Your date range needs at least one training day.');
 if(c.reminder&&(typeof c.reminder.enabled!=='boolean'||!/^([01]\d|2[0-3]):[0-5]\d$/.test(c.reminder.time)))throw Error('Choose a valid reminder time.');
 if(c.quickAdd!==undefined&&(!Number.isFinite(c.quickAdd)||c.quickAdd<=0||c.quickAdd>1e9))throw Error('Quick-add amount must be greater than zero.');
 if(c.link!==undefined)validateLink(c.link);
 if(c.capacity&&(c.mode!=='total'||!Number.isFinite(c.capacity.weekday)||!Number.isFinite(c.capacity.weekend)||c.capacity.weekday<=0||c.capacity.weekend<=0||c.capacity.weekday>1e9||c.capacity.weekend>1e9))throw Error('Enter positive weekday and weekend limits for a total goal.');
 if(c.capacity&&[c.capacity.weekday,c.capacity.weekend].some(n=>Math.abs(n*100-Math.round(n*100))>1e-5))throw Error('Daily limits can have up to two decimal places.');
 if(c.seriesId!==undefined&&typeof c.seriesId!=='string')throw Error('Invalid challenge series.');
 if(c.adjustments!==undefined&&(!Array.isArray(c.adjustments)||c.adjustments.some(a=>!validDate(a.date)||!validDate(a.end)||!validDate(a.oldEnd)||!Number.isFinite(a.perSession)||a.perSession<0)))throw Error('Invalid plan history.');
 const ids=new Set();for(const l of c.logs){if(!l||typeof l.id!=='string'||ids.has(l.id)||!Number.isFinite(l.amount)||l.amount<=0||l.amount>1e9||!validDate(l.date)||l.date<c.start||l.date>c.end||l.date>today()||typeof l.note!=='string'||l.note.length>500)throw Error('A log has an invalid amount, date, or note.');ids.add(l.id);}
 return c;
}
export function parseBackup(text){const data=JSON.parse(text);if(![1,2].includes(data.version)||!Array.isArray(data.challenges)||data.challenges.length>1000)throw Error('This is not a supported Zelo backup.');data.challenges.forEach(validateChallenge);if(new Set(data.challenges.map(c=>c.id)).size!==data.challenges.length)throw Error('Duplicate challenge IDs.');return data.challenges;}
