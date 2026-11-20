const {execFileSync}=require('node:child_process');
const fs=require('node:fs'),path=require('node:path');
const upgrade=process.argv.includes('--upgrade');
const root=path.resolve(__dirname,'..'),adb=process.env.ADB||path.join(root,'.toolchain/android-sdk/platform-tools/adb.exe'),serial=process.env.ANDROID_SERIAL||'emulator-5580';
const previous=(process.argv.find(a=>a.startsWith('--from='))||'--from=0.2.0').slice(7);
if(!/^\d+\.\d+\.\d+$/.test(previous))throw Error('Invalid previous version');
const version=fs.readFileSync(path.join(root,'app/build.gradle'),'utf8').match(/versionName '([^']+)'/)[1];
if(!serial.startsWith('emulator-'))throw Error('Disposable emulator required: this test removes the existing app');
const run=(...a)=>execFileSync(adb,['-s',serial,...a],{encoding:'utf8',timeout:30000}).trim();
const sleep=ms=>new Promise(r=>setTimeout(r,ms));const results=[];
function check(ok,name){results.push({name,passed:!!ok});console.log((ok?'PASS ':'FAIL ')+name);if(!ok)throw Error(name);}
const unescapeXml=s=>s.replace(/&#(x[\da-f]+|\d+);/gi,(_,n)=>String.fromCodePoint(n[0].toLowerCase()==='x'?parseInt(n.slice(1),16):Number(n))).replace(/&quot;/g,'"').replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&apos;/g,"'").replace(/&amp;/g,'&');
function nodes(){const output=run('shell','am','instrument','-w','app.mindpause.snapshot/app.mindpause.snapshot.SnapshotInstrumentation');if(!output.includes('INSTRUMENTATION_CODE: -1'))throw Error(output);return [...output.matchAll(/<node\s+([^>]+)/g)].map(m=>Object.fromEntries([...m[1].matchAll(/([\w-]+)="([^"]*)"/g)].map(a=>[a[1],unescapeXml(a[2])])));}
async function visible(match,scroll=false){for(let i=0;i<(scroll?7:1);i++){const n=nodes().find(match);if(n)return n;run('shell','input','swipe','540','1900','540','600','300');await sleep(300);}throw Error('Missing UI node');}
async function tap(match,scroll=false){const n=await visible(match,scroll);const b=n.bounds.match(/\d+/g).map(Number);run('shell','input','tap',''+((b[0]+b[2])>>1),''+((b[1]+b[3])>>1));await sleep(400);return n;}
async function scrollToTop(){for(let i=0;i<6;i++)run('shell','input','swipe','540','600','540','1900','200');await sleep(300);}
const top=()=>run('shell','dumpsys','activity','activities').split('\n').filter(s=>s.includes('topResumedActivity')).join('\n');
function screenshot(name){fs.writeFileSync(path.join(root,'docs/images',version+'-'+name+'.png'),execFileSync(adb,['-s',serial,'exec-out','screencap','-p']));}
(async()=>{
 check(run('shell','getprop','ro.kernel.qemu')==='1','Disposable emulator');
 run('shell','cmd','locale','set-app-locales','app.mindpause.android','--locales','ru');
 run('shell','am','force-stop','app.mindpause.android');
 run('shell','am','start','-n','app.mindpause.android/.MainActivity');await sleep(800);
 await tap(n=>n.text==='Язык и тема',true);
 await tap(n=>n.text==='English'&&n.class==='android.widget.RadioButton');await sleep(1000);
 check(nodes().some(n=>n.text==='App language'),'English applies from in-app setting');
 await tap(n=>n.text==='Dark'&&n.class==='android.widget.RadioButton');await sleep(800);screenshot('appearance-en-dark');
 run('shell','input','keyevent','KEYCODE_BACK');await sleep(800);screenshot('main-en-dark');
 check(nodes().some(n=>n.text.includes('reviewed')),'English daily statistics');
 await tap(n=>n.text==='Try a question',true);await tap(n=>n.text==='Show the answer',true);await scrollToTop();
 check(nodes().some(n=>n.text==='Correct answer'),'English answer review');screenshot('review-en-dark');
 await tap(n=>n.text==='Solve a new question',true);await tap(n=>n.text==='Close preview',true);await sleep(500);
 await tap(n=>n.text==='Subjects and difficulty',true);check(nodes().some(n=>n.text==='What to learn'),'English subject settings');screenshot('subjects-en-dark');
 run('shell','input','keyevent','KEYCODE_BACK');await sleep(500);
 await tap(n=>n.text==='Language and theme',true);
 await tap(n=>n.text==='Light'&&n.class==='android.widget.RadioButton');await sleep(600);check(nodes().some(n=>n.text==='English'&&n.checked==='true'),'Changing theme preserves English selection');check(nodes().some(n=>n.text==='App language'),'English stays active in light theme');screenshot('appearance-en-light');
 await tap(n=>n.text==='Русский'&&n.class==='android.widget.RadioButton');await sleep(800);
 check(nodes().some(n=>n.text==='Язык приложения'),'Russian applies from in-app setting');screenshot('appearance-ru-light');
 run('shell','input','keyevent','KEYCODE_BACK');await sleep(500);screenshot('main-ru-light');
 await tap(n=>n.text==='Попробовать задачу',true);await tap(n=>n.text==='Показать ответ',true);await scrollToTop();screenshot('review-ru-light');
 await tap(n=>n.text==='Решить новую задачу',true);await tap(n=>n.text==='Закрыть пробу',true);await sleep(500);
 await tap(n=>n.text==='Язык и тема',true);await tap(n=>n.text==='Тёмная'&&n.class==='android.widget.RadioButton');await sleep(600);check(nodes().some(n=>n.text==='Русский'&&n.checked==='true'),'Changing theme preserves Russian selection');screenshot('appearance-ru-dark');
 run('shell','am','force-stop','app.mindpause.android');run('shell','am','start','-n','app.mindpause.android/.MainActivity');await sleep(1000);
 await tap(n=>n.text==='Язык и тема',true);
 check(nodes().some(n=>n.text==='Русский'&&n.checked==='true'),'Manual language persists after restart');
 check(nodes().some(n=>n.text==='Тёмная'&&n.checked==='true'),'Manual theme persists after restart');
 // Choose the system row inside the theme group (the second system radio).
 const defaults=nodes().filter(n=>n.text==='Как в системе'&&n.class==='android.widget.RadioButton');check(defaults.length===2,'Separate system defaults for language and theme');
 const bounds=defaults[1].bounds;await tap(n=>n.bounds===bounds&&n.text==='Как в системе');await sleep(500);
 run('shell','cmd','uimode','night','yes');await sleep(800);screenshot('system-dark');
 run('shell','cmd','uimode','night','no');await sleep(800);screenshot('system-light');
 await tap(n=>n.text==='Как в системе'&&n.class==='android.widget.RadioButton'&&n.checked==='false');await sleep(800);
 check(nodes().some(n=>n.text==='App language'),'System language follows English emulator');
 check(run('shell','cmd','locale','get-app-locales','app.mindpause.android').includes('[]'),'System choice clears Android per-app locale');
 check(!run('logcat','-d','-s','AndroidRuntime:E').includes('Process: app.mindpause.android'),'No app crash during appearance flow');
})().catch(e=>{console.error(e.stack);process.exitCode=1;}).finally(()=>fs.writeFileSync(path.join(root,'docs/appearance-results-'+version+'.json'),JSON.stringify({device:'Android 16 / API 36 emulator',results},null,2)));
