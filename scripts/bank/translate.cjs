// English text is authored locally. No translation services or runtime network access.
const fs=require('node:fs'),path=require('node:path');
const lines=s=>s.trim().split(/\r?\n/).map(s=>s.trim()).filter(Boolean).map(s=>s.split(';'));
const shape=(answer,en)=>{const word=answer.split('|')[0];return en?`Starts with “${word.slice(0,2)}”; ${word.replace(/[^\p{L}]/gu,'').length} letters (spaces and punctuation excluded).`:`Начинается на «${word.slice(0,2)}»; ${word.replace(/[^\p{L}]/gu,'').length} букв без пробелов и знаков.`;};
function pluralExplanation(one,en){
 const rules={goose:['Замени oo на ee; -s не добавляй.','Change oo to ee; do not add -s.'],tooth:['Замени oo на ee; -s не добавляй.','Change oo to ee; do not add -s.'],foot:['Замени oo на ee; -s не добавляй.','Change oo to ee; do not add -s.'],mouse:['Замени ouse на ice.','Replace ouse with ice.'],man:['Замени a на e; -s не добавляй.','Change a to e; do not add -s.'],woman:['Во второй части слова замени a на e.','Change a to e in the second part of the word.'],child:['У этого исключения добавляется -ren.','This irregular plural adds -ren.'],person:['Обычная форма начинается на peo- и заканчивается на -ple.','The usual form begins with peo- and ends in -ple.'],sheep:['Форма не меняется: одна овца и много овец — одно написание.','The spelling is unchanged in the plural.'],deer:['Форма не меняется во множественном числе.','The spelling is unchanged in the plural.']};
 if(rules[one])return rules[one][en?1:0];if(one.endsWith('y'))return en?'After a consonant, replace final y with ies.':'После согласной замени конечную y на ies.';
 if(/(?:f|fe)$/.test(one))return en?'In this word, replace final f or fe with ves.':'В этом слове замени конечные f или fe на ves.';
 return en?'After s, x, sh or ch, add -es.':'После s, x, sh или ch добавь -es.';
}
const learningHints=require('./learning-hints.cjs');
const wordLength=(a,en)=>{const count=a.split('|')[0].length;return en?'The word has '+count+' letters.':'В слове '+count+' букв.';};
const rulesEn=[
'Write И in the combinations ЖИ and ШИ.',
'Write А in the combinations ЧА and ЩА.',
'Write У in the combinations ЧУ and ЩУ.',
'This spelling must be memorized. Pay attention to the vowel.',
'Check an unstressed root vowel using a related word or form where it is stressed.',
'Check a paired consonant using a form where a vowel follows it.',
'In prefixes ending in З/С, write С before a voiceless consonant and З before a voiced one.',
'In alternating roots, the vowel depends on the suffix, stress or neighboring consonant.',
'Use separating Ъ after a consonant-ending prefix before Е, Ё, Ю, Я; separating Ь occurs inside a word.',
'Check the number of consonants: these spellings must be memorized.',
'This is a dictionary spelling: check whether the consonant is single or doubled.',
'Restore the consonant combination carefully without inserting an extra letter.',
'Borrowed words and difficult spellings can be checked in a spelling dictionary.'
];
const contexts={
'множественное число':'plural','змеи, множественное число':'snakes, plural','птицы, множественное число':'birds, plural','вечерняя еда':'evening meal','посуда':'a vessel for drinking','прибор для измерения времени':'a timekeeping device','густой лес':'dense forest','именительный падеж':'nominative case','еда, именительный падеж':'food, nominative case','нечто удивительное':'something marvelous','рыба':'a fish','я сейчас':'I, present tense','я в будущем':'I, future tense','я сейчас лечу больного':'I am treating a patient','с большим количеством гор':'having many mountains','относящийся к саду':'relating to a garden','излучать свет':'to emit light','передвигаться по воздуху':'to move through the air','записывать текст':'to write text','уменьшительное от единицы времени':'diminutive of an hour','съедобный лесной':'edible, found in a forest','первое блюдо':'a soup course','участок с травой':'a grassy area','место с деревьями':'a garden','фрукт':'a fruit','инструмент для резания':'a cutting tool','орган зрения':'organ of sight','музыкальная':'a musical scale','устройство для воспроизведения звука':'an audio speaker','единица массы':'unit of mass','не ранний':'not early','правдивый':'truthful','оказывать почести':'to honor someone','покровительствовать':'to act as a patron'
};
module.exports=function translate(rows){
 const original=rows.map(r=>[...r]),result=new Map();
 const put=(r,q,a,h,e)=>{const en=[...r];en[3]=q;en[4]=a;en[6]=h;en[7]=e;result.set(r[0],en);return en;};
 const ruRules=[...new Set(rows.filter(r=>r[1]==='russian').map(r=>r[6]))];if(ruRules.length!==rulesEn.length)throw Error('Russian rule translation missing');
 for(const r of rows.filter(r=>r[1]==='russian')){
  const rule=rulesEn[ruRules.indexOf(r[6])];let q=r[3].replace('Восстанови слово целиком:','Restore the whole Russian word:');q=q.replace(/\(([^)]+)\)/g,(_,c)=>{if(!contexts[c])throw Error('Missing context '+c);return '('+contexts[c]+')';});
  put(r,q,r[4],rule,`Correct spelling: ${r[4].split('|')[0]}. ${rule}`)[5]=r[5];
 }
 const english=require('./en-english.cjs');const vocab=lines(english.vocab),grammar=lines(english.grammar);let vi=0,gi=0;
 for(const r of rows.filter(r=>r[1]==='english')){
  let q,h,e;const first=r[4].split('|')[0];
  if(r[2]===0){const [check,definition]=vocab[vi++];if(check!==first)throw Error('Vocabulary order '+first);q='Write one English word: '+definition;h=wordLength(r[4],true);e=first+' — '+definition;r[6]=wordLength(r[4],false);}
  else if(r[3].startsWith('Заполни пропуск:')){[q,,e]=grammar[gi];[r[6],h]=learningHints.grammar[gi++];q='Fill the gap: '+q;}
  else if(r[3].includes('множественное')){const one=r[3].match(/существительного (\w+)/)[1];q=`Write the usual plural of ${one}.`;h=learningHints.plural(one,true);r[6]=learningHints.plural(one,false);e=r[7]+' '+pluralExplanation(one,true);r[7]+=' '+pluralExplanation(one,false);}
  else if(r[3].includes('степень')){
   const base=r[3].match(/степень (\w+)/)[1],superlative=r[2]===2,ending=superlative?'est':'er';q=`Write the ${superlative?'superlative':'comparative'} of ${base} as one word${superlative?', without the':''}.`;
   if(base==='good'||base==='bad'){h='These adjectives form comparison from a different stem, rather than by simply adding -er or -est.';r[6]='У этих прилагательных степени сравнения образуются от другой основы, а не простым добавлением -er или -est.';}
   else if(base.endsWith('y')){h=`Replace y with i and add -${ending}.`;r[6]=`Замени y на i и добавь -${ending}.`;}
   else if(base==='big'||base==='hot'){h=`Double the final consonant and add -${ending}.`;r[6]=`Удвой последнюю согласную и добавь -${ending}.`;}
   else if(base.endsWith('e')){h=`The final e is already present: add -${superlative?'st':'r'}.`;r[6]=`Конечная e уже есть: добавь -${superlative?'st':'r'}.`;}
   else{h=`Add -${ending} to the adjective.`;r[6]=`Добавь к прилагательному -${ending}.`;}
   e=r[7]+' '+h;r[7]+=' '+r[6];
  }else{
   const base=r[3].includes('(be)')?'be':r[3].match(/глагола (\w+)/)[1];q=r[2]===2?`Write the past participle of ${base}.`:base==='be'?'Fill the Past Simple gap: I ___ (be) at home yesterday.':`Write the Past Simple form of ${base}.`;
   h=learningHints.verb(base,r[2]===2,true);r[6]=learningHints.verb(base,r[2]===2,false);e=r[7];
  }
  put(r,q,r[4],h,e)[5]=r[5];
 }
 const history=require('./en-history.cjs');
 for(let level=0;level<3;level++){
  const source=rows.filter(r=>r[1]==='history'&&r[2]===level&&/^\d+$/.test(r[4])),data=lines(history.years[level]);if(source.length!==data.length)throw Error('History count '+level);
  source.forEach((r,i)=>{const [year,event]=data[i];if(r[4]!==year)throw Error('History year mismatch '+event);const start=Math.floor(Number(year)/10)*10;const h=`The event was in the decade ${start}–${start+9}.`;r[6]=`Это десятилетие ${start}–${start+9}.`;put(r,`In which year did this happen: ${event}?`,year,h,`${year}: ${event}.`)[5]=r[5];});
 }
 const nonyears=rows.filter(r=>r[1]==='history'&&!/^\d+$/.test(r[4]));const hd=[...lines(history.people),...lines(history.terms)];if(nonyears.length!==hd.length)throw Error('History terms count');
 nonyears.forEach((r,i)=>{const [q,a]=hd[i];r[6]=shape(r[4],false);put(r,q,a,shape(a,true),`${q} Answer: ${a.split('|')[0]}.`);});
 const geography=require('./en-geography.cjs'),regions=new Intl.DisplayNames(['en'],{type:'region'});
 for(let level=0;level<3;level++){
  const source=rows.filter(r=>r[1]==='geography'&&r[2]===level),caps=source.filter(r=>r[3].startsWith('Назови столицу')),facts=source.filter(r=>!r[3].startsWith('Назови столицу')),cd=lines(geography.capitals[level]),fd=lines(geography.facts[level]);
  if(caps.length!==cd.length||facts.length!==fd.length)throw Error('Geography count '+level);
  caps.forEach((r,i)=>{const a=cd[i][0],iso=r[8].match(/iso\/(\w+)\.html/)[1],country=regions.of(iso);r[6]=shape(r[4],false);put(r,`What is the capital of ${country}?`,a,shape(a,true),`${country}: the capital is ${a.split('|')[0]}.`);});
  facts.forEach((r,i)=>{const [q,a]=fd[i];r[6]=shape(r[4],false);put(r,q,a,shape(a,true),`${q} Answer: ${a.split('|')[0]}.`);});
 }
 // Preserve each option's position; reuse translations of existing distractors where known.
 const dictionary=new Map();for(const r of rows){const en=result.get(r[0]);if(r[1]==='russian'||r[1]==='english')continue;for(const a of r[4].split('|'))if(!dictionary.has(a))dictionary.set(a,en[4].split('|')[0]);}
 for(const [ru,en] of lines(require('./en-distractors.cjs')))dictionary.set(ru,en);
 const norm=s=>s.toLowerCase().normalize('NFKC').trim();
 for(const r of rows){const en=result.get(r[0]);if(!en)throw Error('Missing translation '+r[0]);if(['russian','english'].includes(r[1])||/^\d+$/.test(r[4]))continue;
  const accepted=en[4].split('|').map(norm),opts=[en[4].split('|')[0]],pool=rows.filter(x=>x[1]===r[1]&&x[2]===r[2]&&!/^\d+$/.test(x[4])).map(x=>result.get(x[0])[4].split('|')[0]);let cursor=rows.indexOf(r)%pool.length;
  for(const option of r[5].split('|').slice(1)){let v=dictionary.get(option);if(!v)throw Error('Missing distractor translation: '+option);while(accepted.includes(norm(v))||opts.map(norm).includes(norm(v)))v=pool[cursor++%pool.length];opts.push(v);}en[5]=opts.join('|');
 }
 // Lock the editorial arrays to source IDs, so edits cannot silently misalign translations.
 const lock=path.join(__dirname,'en-source-ids.json');const ids=original.map(r=>r[0]);if(fs.existsSync(lock)){if(JSON.stringify(JSON.parse(fs.readFileSync(lock,'utf8')))!==JSON.stringify(ids))throw Error('Source bank changed: review English mappings and update en-source-ids.json.');}else fs.writeFileSync(lock,JSON.stringify(ids,null,2)+'\n');
 return rows.map(r=>result.get(r[0]));
};
