// Real AudioManager focus callbacks against a signed APK. No Spotify/TikTok emulation
// or audible playback: this isolates whether the silent quiz interferes with a player.
// Destructive only to the app installation on a disposable Android emulator.
const {execFileSync}=require('node:child_process');
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'..');
const adb=process.env.ADB||path.join(root,'.toolchain/android-sdk/platform-tools/adb.exe');
const serial=process.env.ANDROID_SERIAL||'emulator-5580';
const baseline=process.argv.includes('--baseline');
const pausePlayers=process.argv.includes('--pause-players');
if(baseline&&pausePlayers)throw Error('Baseline has no music setting');
const version=fs.readFileSync(path.join(root,'app/build.gradle'),'utf8').match(/versionName '([^']+)'/)[1];
if(!serial.startsWith('emulator-'))throw Error('Disposable emulator required');
const run=(...a)=>execFileSync(adb,['-s',serial,...a],{encoding:'utf8',timeout:30000}).trim();
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const results=[],observations=[];
let failure=null;
function check(ok,name){results.push({name,passed:!!ok});console.log((ok?'PASS ':'FAIL ')+name);if(!ok)throw Error(name);}
const decode=s=>s.replace(/&#(x[\da-f]+|\d+);/gi,(_,n)=>String.fromCodePoint(n[0].toLowerCase()==='x'?parseInt(n.slice(1),16):Number(n))).replace(/&quot;/g,'"').replace(/&apos;/g,"'").replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&amp;/g,'&');
function nodes(){const xml=run('shell','am','instrument','-w','app.mindpause.snapshot/app.mindpause.snapshot.SnapshotInstrumentation');if(!xml.includes('INSTRUMENTATION_CODE: -1'))throw Error(xml);return [...xml.matchAll(/<node\s+([^>]+)/g)].map(m=>Object.fromEntries([...m[1].matchAll(/([\w-]+)="([^"]*)"/g)].map(a=>[a[1],decode(a[2])])));}
async function tap(match,scroll=false){for(let i=0;i<(scroll?8:1);i++){const n=nodes().find(match);if(n){const b=n.bounds.match(/\d+/g).map(Number);run('shell','input','tap',String((b[0]+b[2])>>1),String((b[1]+b[3])>>1));await sleep(500);return;}run('shell','input','swipe','540','1900','540','600','250');await sleep(250);}throw Error('UI control missing');}
const top=()=>run('shell','dumpsys','activity','activities').split('\n').filter(s=>s.includes('topResumedActivity')).join('\n');
const callbacks=()=>[...run('logcat','-d','-v','brief','PauseAudioProbe:I','*:S').matchAll(/focus=(-?\d+)/g)].map(m=>Number(m[1]));
function observe(stage){
    const dump=run('shell','dumpsys','audio');
    const start=dump.indexOf('Audio Focus stack entries');
    const end=dump.toLowerCase().indexOf('external focus policy',start);
    if(start<0 || end<0)throw Error('Audio focus stack format changed');
    const focusStack=dump.slice(start,end).trim();
    const events=callbacks();observations.push({stage,callbacks:events,focusStack});
    console.log(stage+': callbacks='+JSON.stringify(events));return focusStack;
}
(async()=>{
    check(run('shell','getprop','ro.kernel.qemu')==='1','Disposable emulator');
    for(const apk of ['fixture/build/outputs/apk/debug/fixture-debug.apk','snapshot/build/outputs/apk/debug/snapshot-debug.apk'])check(run('install','-r',path.join(root,apk)).includes('Success'),'Install '+path.basename(apk));
    run('shell','settings','put','secure','enabled_accessibility_services','null');
    run('uninstall','app.mindpause.android');
    check(run('install',path.join(root,baseline?'dist/pause-0.4.1.apk':'app/build/outputs/apk/release/app-release.apk')).includes('Success'),'Install signed '+(baseline?'0.4.1 baseline':version));
    run('shell','cmd','locale','set-app-locales','app.mindpause.android','--locales','ru');
    run('shell','am','start','-n','app.mindpause.android/.MainActivity');await sleep(900);
    await tap(n=>n.text.includes('Выбрать приложения'),true);
    await tap(n=>n.class==='android.widget.EditText');run('shell','input','text','fixture');await sleep(400);
    await tap(n=>n.text==='Лента • тест');await tap(n=>n.text.toLowerCase()==='сохранить');
    await tap(n=>n.text==='Настроить',true);await tap(n=>n.text==='5 минут');await tap(n=>n.text==='1 минуту');await tap(n=>n.text.toLowerCase()==='сохранить');
    if(pausePlayers){
        await tap(n=>n.text==='Дополнительные настройки',true);await tap(n=>n.text==='Музыка во время задач',true);
        check(nodes().some(n=>n.text==='Не вмешиваться (по умолчанию)'&&n.checked==='true'),'Music defaults to unchanged playback');
        await tap(n=>n.text==='Приостанавливать плееры');await tap(n=>n.text.toLowerCase()==='сохранить');
        await tap(n=>n.text==='Музыка во время задач');
        check(nodes().some(n=>n.text==='Приостанавливать плееры'&&n.checked==='true'),'Music choice is saved');
        fs.writeFileSync(path.join(root,'docs/images',version+'-music-setting.png'),execFileSync(adb,['-s',serial,'exec-out','screencap','-p']));
        await tap(n=>n.text.toLowerCase()==='отмена');
    }
    run('shell','settings','put','secure','enabled_accessibility_services','app.mindpause.android/app.mindpause.android.FocusService');run('shell','settings','put','secure','accessibility_enabled','1');await sleep(1500);
    run('shell','am','force-stop','app.mindpause.fixture');run('logcat','-c');
    run('shell','am','start','-n','app.mindpause.fixture/.AudioProbeActivity');await sleep(1000);
    check(run('logcat','-d','-s','PauseAudioProbe:I').includes('request=1'),'External probe granted audio focus');
    check(observe('player').includes('app.mindpause.fixture'),'Probe present in Android focus stack');
    run('shell','am','start','-n','app.mindpause.android/.MainActivity');await sleep(600);
    await tap(n=>n.text==='Попробовать задачу',true);check(top().includes('BreakActivity'),'Preview opens');
    const during=observe('preview-open');
    check(during.includes('pack: app.mindpause.android')===(baseline||pausePlayers),'Preview audio focus follows setting');
    run('shell','input','keyevent','KEYCODE_BACK');await sleep(800);observe('preview-closed');
    check(JSON.stringify(callbacks())===JSON.stringify(baseline?[-2,1]:pausePlayers?[-1]:[]),'Preview '+(baseline?'reproduces LOSS_TRANSIENT then GAIN':pausePlayers?'pauses player without a resume signal':'causes no loss/resume callbacks'));
    if(pausePlayers){run('shell','am','force-stop','app.mindpause.fixture');run('logcat','-c');}
    run('shell','am','start','-n','app.mindpause.fixture/.AudioProbeActivity');
    const deadline=Date.now()+85000;
    while(Date.now()<deadline&&!top().includes('BreakActivity'))await sleep(2000);
    check(top().includes('BreakActivity'),'Timed break opens over probe');observe('timed-break');
    run('shell','input','keyevent','KEYCODE_HOME');await sleep(700);observe('home');
    run('shell','am','start','-n','app.mindpause.fixture/.AudioProbeActivity');await sleep(2500);
    check(top().includes('BreakActivity'),'Unfinished break returns');observe('break-returned');
    await sleep(11000);await tap(n=>n.text.startsWith('Пропустить')&&n.enabled==='true',true);await sleep(800);
    check(top().includes('AudioProbeActivity'),'Paid skip returns to probe');observe('skip-closed');
    const events=callbacks();
    check(JSON.stringify(events)===JSON.stringify(baseline?[-2,1,-2,1,-2,1]:pausePlayers?[-1]:[]),'All open/close paths '+(baseline?'reproduce focus churn':pausePlayers?'never restart a stopped player':'leave player focus untouched'));
    const final=observe('final');
    check(final.includes('pack: app.mindpause.fixture')===!pausePlayers,'Player focus after skip follows selected policy');
    check(!final.includes('pack: app.mindpause.android'),'Pause releases any held focus on exit');
    check(!run('logcat','-d','-s','AndroidRuntime:E').includes('Process: app.mindpause'),'No app or fixture crash');
})().catch(e=>{failure=e.stack;console.error(failure);process.exitCode=1;}).finally(()=>{
    fs.writeFileSync(path.join(root,'docs/audio-results-'+(baseline?'0.4.1-baseline':version+(pausePlayers?'-pause':''))+'.json'),JSON.stringify({device:'Android 16 / API 36 emulator',scope:'Real audio focus callbacks; silent fixture, no Spotify, TikTok or audible playback',passed:!failure&&results.every(r=>r.passed),failure,results,observations},null,2));
});
