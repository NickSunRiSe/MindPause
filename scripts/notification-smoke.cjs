// Disposable emulator only. Uses debug preferences to measure time, real heads-up
// notifications, and the native window list. No notification text is read by Pause.
const {execFileSync}=require('node:child_process');
const fs=require('node:fs'),path=require('node:path');
const root=path.resolve(__dirname,'..'),serial=process.env.ANDROID_SERIAL||'emulator-5580';
const adb=process.env.ADB||path.join(root,'.toolchain/android-sdk/platform-tools/adb.exe');
const version=fs.readFileSync(path.join(root,'app/build.gradle'),'utf8').match(/versionName '([^']+)'/)[1];
if(!serial.startsWith('emulator-'))throw Error('Disposable emulator required');
const run=(...a)=>execFileSync(adb,['-s',serial,...a],{encoding:'utf8',timeout:30000}).trim();
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const results=[],samples=[],windows=[];let failure=null,lastUsed=0;
function check(ok,name){results.push({name,passed:!!ok});console.log((ok?'PASS ':'FAIL ')+name);if(!ok)throw Error(name);}
function preferences(){return run('shell','run-as','app.mindpause.android','cat','shared_prefs/pause.xml');}
function sample(stage){const xml=preferences(),used=Number(xml.match(/<long name="app\.mindpause\.fixture\.used" value="(\d+)"/)?.[1]||0);if(used<lastUsed)throw Error('Accumulated time reset at '+stage);lastUsed=used;const s={stage,at:Date.now(),used};samples.push(s);console.log(stage+': '+used+' ms');return s;}
function windowSnapshot(stage){const text=run('shell','am','instrument','-w','-e','windows','true','app.mindpause.snapshot/app.mindpause.snapshot.SnapshotInstrumentation');const data=JSON.parse(text.match(/INSTRUMENTATION_RESULT: windows=(.*)/)[1]);windows.push({stage,windows:data});return data;}
const feed=()=>run('shell','am','start','-n','app.mindpause.fixture/.FeedActivity');
const top=()=>run('shell','dumpsys','activity','activities').split('\n').filter(s=>s.includes('topResumedActivity')).join('\n');
(async()=>{
 check(run('shell','getprop','ro.kernel.qemu')==='1','Disposable emulator');
 run('shell','settings','put','secure','enabled_accessibility_services','null');run('uninstall','app.mindpause.android');
 for(const apk of ['app/build/outputs/apk/debug/app-debug.apk','app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk','fixture/build/outputs/apk/debug/fixture-debug.apk','snapshot/build/outputs/apk/debug/snapshot-debug.apk'])check(run('install','-r',path.join(root,apk)).includes('Success'),'Install '+path.basename(apk));
 run('shell','pm','grant','app.mindpause.fixture','android.permission.POST_NOTIFICATIONS');
 check(run('shell','am','instrument','-w','-e','prepareTimer','true','app.mindpause.android.test/app.mindpause.android.DeviceTests').includes('PASS:'),'Prepare one-minute test');
 run('shell','settings','put','secure','enabled_accessibility_services','app.mindpause.android/app.mindpause.android.FocusService');run('shell','settings','put','secure','accessibility_enabled','1');await sleep(1500);
 run('shell','am','broadcast','-n','app.mindpause.fixture/.NotifyReceiver','--ez','clear','true');
 run('logcat','-c');feed();
 const ready=Date.now()+15000;while(Date.now()<ready&&!preferences().includes('name="app.mindpause.fixture.used"'))await sleep(500);
 await sleep(1000);const start=sample('before-notifications');check(start.used>0,'Timer has started before notification burst');
 for(let i=0;i<4;i++){
  const since=Date.now();run('shell','am','broadcast','-n','app.mindpause.fixture/.NotifyReceiver','--ei','id',String(101+i));await sleep(1500);
  if(i===0)fs.writeFileSync(path.join(root,'docs/images',version+'-heads-up.png'),execFileSync(adb,['-s',serial,'exec-out','screencap','-p']));
  const w=windowSnapshot('notification-'+i);
  check(w.some(x=>x.package==='app.mindpause.fixture'&&x.focused),'Feed keeps input focus under notification '+i);
  await sleep(Math.max(0,8000-(Date.now()-since)));sample('notification-'+i);
 }
 const after=sample('after-notifications');
 check(after.used-start.used>=after.at-start.at-2000,'Notification burst does not stall accumulated time');
 check(!top().includes('BreakActivity'),'No early break before one active minute');
 run('shell','cmd','statusbar','expand-notifications');await sleep(1200);windowSnapshot('expanded-shade');const shade=sample('shade-open');await sleep(5000);
 check(sample('shade-wait').used===shade.used,'Expanded shade pauses time without resetting it');
 run('shell','cmd','statusbar','collapse');await sleep(1000);const resumed=sample('shade-closed');await sleep(3500);
 check(sample('resumed').used>=resumed.used+2500,'Time continues after shade closes');
 run('shell','input','keyevent','KEYCODE_HOME');await sleep(1000);const home=sample('home');await sleep(3500);
 check(sample('home-wait').used===home.used,'Home pauses without resetting accumulated time');
 feed();await sleep(1500);run('shell','input','keyevent','KEYCODE_SLEEP');await sleep(1000);const locked=sample('screen-off');await sleep(3500);
 check(sample('screen-off-wait').used===locked.used,'Screen off pauses without resetting accumulated time');
 run('shell','input','keyevent','KEYCODE_WAKEUP');run('shell','wm','dismiss-keyguard');feed();
 const deadline=Date.now()+45000;while(Date.now()<deadline&&!top().includes('BreakActivity'))await sleep(1000);
 check(top().includes('BreakActivity'),'Break opens after remaining active time');
 const end=sample('break');check(end.used>=60000&&end.used<62000,'Threshold is one accumulated active minute');
 check(!run('logcat','-d','-s','AndroidRuntime:E').includes('Process: app.mindpause'),'No app or fixture crash');
})().catch(e=>{failure=e.stack;console.error(failure);process.exitCode=1;}).finally(()=>{
 fs.writeFileSync(path.join(root,'docs/notification-results-'+version+'.json'),JSON.stringify({device:'Android 16 / API 36 emulator',passed:!failure,failure,results,samples,windows},null,2));
});
