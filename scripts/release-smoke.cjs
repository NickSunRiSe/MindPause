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
    for(const apk of ['fixture/build/outputs/apk/debug/fixture-debug.apk','snapshot/build/outputs/apk/debug/snapshot-debug.apk'])check(run('install','-r',path.join(root,apk)).includes('Success'),'Install '+path.basename(apk));
    run('uninstall','app.mindpause.android');check(run('install',path.join(root,upgrade?'dist/pause-'+previous+'.apk':'app/build/outputs/apk/release/app-release.apk')).includes('Success'),'Signed release installation');
    run('shell','cmd','locale','set-app-locales','app.mindpause.android','--locales','ru');
    run('shell','am','start','-n','app.mindpause.android/.MainActivity');await sleep(1000);
    await tap(n=>n.text.includes('Выбрать приложения'),true);
    await tap(n=>n.class==='android.widget.EditText');run('shell','input','text','fixture');await sleep(500);
    await tap(n=>n.text==='Лента • тест');await tap(n=>n.text.toLowerCase()==='сохранить');
    check(nodes().some(n=>n.text==='Лента • тест'),'App picker saves selection');
    await tap(n=>n.text==='Настроить',true);await tap(n=>n.text==='5 минут');await tap(n=>n.text==='1 минуту');
    screenshot('rule-settings');await tap(n=>n.text.toLowerCase()==='сохранить');
    if(upgrade && previous!=='0.1.0'){
        await tap(n=>n.text==='Предметы и сложность',true);
        await tap(n=>n.text==='Русский язык'&&n.checked==='true',true);
        await tap(n=>n.text==='История'&&n.checked==='true',true);
        run('shell','input','keyevent','KEYCODE_BACK');await sleep(500);
    }
    if(upgrade){
        run('shell','settings','put','secure','enabled_accessibility_services','app.mindpause.android/app.mindpause.android.FocusService');run('shell','settings','put','secure','accessibility_enabled','1');await sleep(1500);
        check(run('install','-r',path.join(root,'app/build/outputs/apk/release/app-release.apk')).includes('Success'),'In-place upgrade from signed '+previous);
        run('shell','cmd','locale','set-app-locales','app.mindpause.android','--locales','ru');
        run('shell','am','start','-n','app.mindpause.android/.MainActivity');await sleep(1800);
        check(run('shell','settings','get','secure','enabled_accessibility_services').includes('app.mindpause.android.FocusService'),'Upgrade preserves enabled accessibility service');
        check(nodes().some(n=>n.text==='Лента • тест'),'Upgrade preserves selected app');
        check((await visible(n=>n.text.startsWith('До паузы:')&&n.text.includes('с активного просмотра'),true)).text.length>0,'Remaining minute is displayed in seconds');
        await tap(n=>n.text==='Настроить',true);check(nodes().some(n=>n.text==='1 минуту'),'Upgrade preserves one-minute interval');await tap(n=>n.text.toLowerCase()==='отмена');
    }
    await tap(n=>n.text==='Дополнительные настройки',true);
    await tap(n=>n.text==='Расписание и дни недели',true);check(nodes().filter(n=>n.class==='android.widget.CheckBox').length>=5,'Schedule editor opens');screenshot('schedule');await tap(n=>n.text.toLowerCase()==='отмена');
    run('shell','settings','put','secure','enabled_accessibility_services','app.mindpause.android/app.mindpause.android.FocusService');run('shell','settings','put','secure','accessibility_enabled','1');await sleep(1500);
    await scrollToTop();
    check(nodes().some(n=>n.text.replace(/\s+/g,' ')==='✓ Доступ настроен'),'Ready state visibly confirmed');
    check(!nodes().some(n=>n.text==='Проверить разрешение'||n.text==='Включить доступ'),'No misleading permission action when ready');screenshot('main');
    await tap(n=>n.text==='Пробный перерыв');check(top().includes('BreakActivity'),'Ready action opens exercise instead of permission disclosure');run('shell','input','keyevent','KEYCODE_BACK');await sleep(500);
    await visible(n=>n.text==='Предметы и сложность',true);screenshot('buttons-main');
    await tap(n=>n.text==='Предметы и сложность');check(nodes().some(n=>n.text==='Чему учиться'),'Subject customization opens');screenshot('subjects');
    if(upgrade){check((await visible(n=>n.text==='Русский язык',true)).checked==='false','Upgrade preserves disabled Russian');await scrollToTop();}
    await tap(n=>n.text==='Готовый лёгкий набор');check((await visible(n=>n.text==='Русский язык',true)).checked==='true','One-tap preset enables subjects');await scrollToTop();
    check(nodes().some(n=>n.text==='Новое задание'),'Default reveal policy requires a new question');
    await tap(n=>n.text==='Новое задание');await tap(n=>n.text==='Выбирать каждый раз');screenshot('review-policy');
    run('shell','input','keyevent','KEYCODE_BACK');await sleep(500);
    run('shell','am','start','-n','app.mindpause.fixture/.FeedActivity');
    const deadline=Date.now()+85000;while(Date.now()<deadline&&!top().includes('BreakActivity'))await sleep(2000);
    check(top().includes('BreakActivity'),'Release interrupts after configured minute');
    run('shell','settings','put','system','font_scale','1.3');await sleep(1000);screenshot('large-text');
    run('shell','settings','put','system','font_scale','1.0');await sleep(1000);
    await scrollToTop();
    const currentQuestion=()=>{const t=nodes().filter(n=>n.text&&n.class==='android.widget.TextView');const k=t.findIndex(n=>n.text==='Задача 1 из 1');if(k<1)throw Error('Question counter missing');return t[k-1].text;};
    const original=currentQuestion();
    await tap(n=>n.text==='Показать ответ',true);await scrollToTop();screenshot('review-before-replacement');
    check(!nodes().some(n=>n.class==='android.widget.EditText'),'Revealed answer cannot be typed back');
    await tap(n=>n.text==='Решить новую задачу',true);await scrollToTop();
    check(currentQuestion()!==original,'Replacement is a different question at the same step');
    await tap(n=>n.text==='Показать ответ',true);await sleep(10500);await visible(n=>n.text.startsWith('Потратить пропуск')&&n.enabled==='true',true);screenshot('review-use-skip');
    await tap(n=>n.text.startsWith('Потратить пропуск')&&n.enabled==='true');await sleep(1000);
    check(top().includes('FeedActivity'),'Skip returns to feed');
    run('shell','am','start','-n','app.mindpause.android/.MainActivity');await sleep(600);check(nodes().some(n=>n.text.includes('1 пропущено')),'Skip reflected in daily statistics');check(nodes().some(n=>n.text.includes('0 решено')&&n.text.includes('2 разобрано')),'Two reveals earn no solved credit');
    run('shell','input','keyevent','KEYCODE_HOME');check(!run('logcat','-d','-s','AndroidRuntime:E').includes('Process: app.mindpause.android'),'Release has no crash');
})().catch(e=>{console.error(e.stack);process.exitCode=1;}).finally(()=>fs.writeFileSync(path.join(root,'docs/'+(upgrade?'upgrade':'release')+'-results-'+version+'.json'),JSON.stringify({device:'Android 16 / API 36 emulator',results},null,2)));
