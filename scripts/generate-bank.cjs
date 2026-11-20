// Original prompts and reference facts. Offline build: no downloads or model calls.
const fs=require('node:fs'),path=require('node:path'),{createHash}=require('node:crypto');
const rows=[],counts={};
const normalize=s=>s.normalize('NFKC').trim().replace(/\s+/g,' ').toLowerCase();
function lines(s){return s.trim().split(/\r?\n/).map(s=>s.trim()).filter(Boolean).map(s=>s.split(';'));}
function options(answers,pool,seed=0){
  const accepted=answers.split('|').map(normalize),result=[answers.split('|')[0]];
  for(let i=0;i<pool.length && result.length<4;i++){
    const candidate=pool[(i+seed)%pool.length].split('|')[0];
    if(!accepted.includes(normalize(candidate))&&!result.map(normalize).includes(normalize(candidate)))result.push(candidate);
  }
  if(result.length<3)throw Error('Insufficient distractors: '+answers);return result;
}
function add(subject,level,question,answers,opts,hint,explanation,reference){
  const id=subject+'-'+createHash('sha256').update(question).digest('hex').slice(0,16);
  if(rows.some(r=>r[0]===id))throw Error('Duplicate question: '+question);
  const values=[id,subject,level,question,answers,opts.join('|'),hint,explanation,reference];
  if(values.some(v=>/[\t\r\n]/.test(String(v))))throw Error('TSV delimiter in question: '+id);
  const accepted=answers.split('|').map(normalize);
  if(opts.filter(o=>accepted.includes(normalize(o))).length!==1 || new Set(opts.map(normalize)).size!==opts.length)throw Error('Ambiguous options: '+question);
  rows.push(values);const key=subject+'/'+level;counts[key]=(counts[key]||0)+1;
}
for(const subject of ['russian','english','history','geography'])require('./bank/'+subject+'.cjs')({add,lines,options});
const russianWords=new Set();for(const row of rows)if(row[1]==='russian'){const word=row[4].split('|')[0];if(russianWords.has(word))throw Error('Repeated Russian word: '+word);russianWords.add(word);}
for(const subject of ['russian','english','history','geography'])for(let level=0;level<3;level++)if((counts[subject+'/'+level]||0)<100)throw Error('Too few questions: '+subject+'/'+level+' = '+counts[subject+'/'+level]);
const destination=path.join(__dirname,'../app/src/main/assets/questions.tsv');
const english=require('./bank/translate.cjs')(rows);
for(const r of english){
 if(r.length!==9 || r.some(v=>/[\t\r\n]/.test(String(v))))throw Error('Invalid translation '+r[0]);
 const accepted=r[4].split('|').map(normalize),opts=r[5].split('|').map(normalize);
 if(opts.filter(o=>accepted.includes(o)).length!==1 || new Set(opts).size!==opts.length)throw Error('Ambiguous English options '+r[0]);
 if(r[1]!=='russian' && /[А-Яа-яЁё]/.test(r.slice(3,8).join(' ')))throw Error('Untranslated English field '+r[0]);
}
fs.writeFileSync(destination.replace('questions.tsv','questions-en.tsv'),'# English questions; same IDs and aligned option positions as questions.tsv\n'+english.map(r=>r.join('\t')).join('\n')+'\n');
fs.writeFileSync(destination,'# id\tsubject\tlevel\tquestion\taccepted answers\toptions\thint\texplanation\treference\n'+rows.map(r=>r.join('\t')).join('\n')+'\n');
fs.writeFileSync(path.join(__dirname,'../docs/bank-counts.json'),JSON.stringify({total:rows.length,groups:counts},null,2)+'\n');
console.log(JSON.stringify({total:rows.length,groups:counts},null,2));
