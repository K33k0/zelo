export const challengePlaceholders=Object.freeze([
 'Read 10 pages','Learn 5 new words','Write 200 words','Practise an instrument',
 'Make time to draw','Walk a little further','Finish a coding project',
 'Clear the clutter','Spend time outdoors','Build a daily habit'
]);
// One suggestion per form opening. Complete the shuffled set before repeating.
export function createPlaceholderRotation(random=Math.random){
 let bag=[],last;
 return ()=>{if(!bag.length){bag=[...challengePlaceholders];for(let i=bag.length-1;i>0;i--){const j=Math.floor(random()*(i+1));[bag[i],bag[j]]=[bag[j],bag[i]];}if(bag.at(-1)===last)[bag[0],bag[bag.length-1]]=[bag.at(-1),bag[0]];}last=bag.pop();return last;};
}
