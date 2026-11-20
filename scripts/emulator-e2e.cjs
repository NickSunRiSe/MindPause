// Run only against a disposable emulator. DeviceTests clears app preferences.
const {execFileSync} = require('node:child_process');
const fs=require('node:fs'), path=require('node:path');
const root=path.resolve(__dirname,'..');
const adb=process.env.ADB || path.join(root,'.toolchain/android-sdk/platform-tools/adb.exe');
const serial=process.env.ANDROID_SERIAL || 'emulator-5580';
const target='app.mindpause.android', fixture='app.mindpause.fixture';
if(!serial.startsWith('emulator-'))throw Error('Use a disposable emulator only');
const run=(...args)=>execFileSync(adb,['-s',serial,...args],{encoding:'utf8',timeout:30000}).trim();
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const results=[];
function check(ok,name,detail=''){results.push({name,passed:!!ok,detail});console.log(`${ok?'PASS':'FAIL'} ${name} ${detail}`);if(!ok)throw Error(name);}
const decode=s=>s.replace(/&quot;/g,'"').replace(/&apos;/g,"'").replace(/&lt;/g,'<').replace(/&gt;/g,'>').replace(/&#10;/g,'\n').replace(/&amp;/g,'&');
function prefs(){return run('shell','run-as',target,'cat','shared_prefs/pause.xml');}
function used(){const m=prefs().match(/<long name="app\.mindpause\.fixture\.used" value="(\d+)"/);return m?Number(m[1]):0;}
function pending(){return prefs().includes('name="app.mindpause.fixture.pending" value="true"');}
function problem(){return JSON.parse(decode(prefs().match(/<string name="app\.mindpause\.fixture\.problem">([\s\S]*?)<\/string>/)[1]));}
function top(){return run('shell','dumpsys','activity','activities').split('\n').filter(x=>x.includes('topResumedActivity')||x.includes('mResumedActivity')).join('\n');}
function launch(){run('shell','am','start','-n',fixture+'/.FeedActivity');}
function ui(){run('shell','uiautomator','dump','/sdcard/pause-window.xml');return run('shell','cat','/sdcard/pause-window.xml');}
function tapNode(predicate){const xml=ui();const nodes=[...xml.matchAll(/<node\s+([^>]+)>?/g)].map(m=>{const obj={};for(const a of m[1].matchAll(/([\w-]+)="([^"]*)"/g))obj[a[1]]=decode(a[2]);return obj;});const n=nodes.find(predicate);if(!n)throw Error('Node not found: '+xml);const b=n.bounds.match(/\d+/g).map(Number);run('shell','input','tap',String((b[0]+b[2])>>1),String((b[1]+b[3])>>1));}
function screenshot(name){const out=path.join(root,'docs/images',name+'.png');fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,execFileSync(adb,['-s',serial,'exec-out','screencap','-p']));}
(async()=>{
    check(run('shell','getprop','ro.kernel.qemu')==='1','Disposable emulator verified');
    for(const apk of ['app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk','fixture/build/outputs/apk/debug/fixture-debug.apk'])check(run('install','-r',path.join(root,apk)).includes('Success'),'Install '+path.basename(apk));
    const smoke=run('shell','am','instrument','-w',target+'.test/'+target+'.DeviceTests');check(smoke.includes('PASS:')&&!smoke.includes('FAIL:'),'Device persistence and activity smoke',smoke);
    run('shell','settings','put','secure','enabled_accessibility_services',target+'/'+target+'.FocusService');run('shell','settings','put','secure','accessibility_enabled','1');await sleep(2000);
    run('shell','am','start','-n',target+'/.MainActivity');await sleep(1000);screenshot('main');
    launch();await sleep(12000);const beforeHome=used();check(beforeHome>=8000&&beforeHome<18000,'Foreground time counted',String(beforeHome));
    run('shell','input','keyevent','KEYCODE_HOME');await sleep(10000);const afterHome=used();check(afterHome-beforeHome<2000,'Home pauses timer',`${beforeHome} -> ${afterHome}`);
    launch();await sleep(3000);run('shell','input','keyevent','KEYCODE_SLEEP');await sleep(2000);const locked=used();await sleep(8000);check(used()===locked,'Screen-off pauses timer',String(locked));
    run('shell','input','keyevent','KEYCODE_WAKEUP');run('shell','wm','dismiss-keyguard');launch();
    const deadline=Date.now()+80000;while(Date.now()<deadline&&!top().includes('BreakActivity'))await sleep(2000);
    check(top().includes('BreakActivity'),'Timed interruption launches break');check(used()>=60000,'One minute accumulated',String(used()));const initial=problem();screenshot('break');
    tapNode(n=>n.class==='android.widget.EditText');run('shell','input','text','99999');run('shell','input','keyevent','KEYCODE_ENTER');await sleep(500);
    check(pending()&&top().includes('BreakActivity'),'Wrong answer keeps break');
    run('shell','input','keyevent','KEYCODE_BACK');await sleep(700);if(top().includes('BreakActivity'))run('shell','input','keyevent','KEYCODE_BACK');await sleep(700);
    check(!top().includes('BreakActivity'),'Back allows leaving to Home');check(pending(),'Leaving preserves pending task');
    launch();await sleep(2500);check(top().includes('BreakActivity'),'Reopening app restores break');check(problem().q===initial.q,'Same unsolved problem restored');
    tapNode(n=>n.class==='android.widget.EditText');run('shell','input','text',String(initial.a));run('shell','input','keyevent','KEYCODE_ENTER');await sleep(700);
    check(!pending(),'Correct answer clears break');check(used()===0,'Correct answer resets interval');screenshot('success');
    tapNode(n=>n.text==='Продолжить просмотр');await sleep(1000);check(top().includes('FeedActivity'),'Continue returns to feed');
    run('shell','input','keyevent','KEYCODE_HOME');
    const crashes=run('logcat','-d','-s','AndroidRuntime:E');check(!crashes.includes('Process: '+target),'No app crash in logcat');
})().catch(e=>{console.error(e.stack);process.exitCode=1;}).finally(()=>{fs.mkdirSync(path.join(root,'docs'),{recursive:true});fs.writeFileSync(path.join(root,'docs/emulator-results.json'),JSON.stringify({device:'Android 16 / API 36, x86_64 emulator',results},null,2));});
