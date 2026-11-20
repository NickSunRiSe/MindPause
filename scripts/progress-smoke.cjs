// Run on the disposable Pause emulator after installing the debug app, device tests and fixture.
// Stop any other feed blocker (including the historical usage-access trial) before running.
// The instrumentation resets test preferences. This never installs packages or creates a release.
const { execFileSync } = require('node:child_process');
const path = require('node:path');
const adb = process.env.ADB || path.resolve(__dirname, '../.toolchain/android-sdk/platform-tools/adb.exe');
const serial = process.env.ANDROID_SERIAL || 'emulator-5580';
const pkg = 'app.mindpause.fixture';
const service = 'app.mindpause.android/app.mindpause.android.FocusService';
const instrument = 'app.mindpause.android.test/app.mindpause.android.DeviceTests';
const wait = ms => new Promise(resolve => setTimeout(resolve, ms));
function run(...args) {
  return execFileSync(adb, ['-s', serial, ...args], { encoding: 'utf8', timeout: 30000 }).trim();
}
function requireCheck(condition, message) {
  if (!condition) throw Error(message);
  console.log('PASS ' + message);
}
function top() {
  return run('shell', 'dumpsys', 'activity', 'activities').split('\n').filter(line => line.includes('topResumedActivity')).join('\n');
}
function amount(file, key) {
  const xml = run('shell', 'run-as', 'app.mindpause.android', 'cat', 'shared_prefs/' + file + '.xml');
  const name = 'name="' + key + '" value="';
  const start = xml.indexOf(name);
  return start < 0 ? 0 : Number(xml.slice(start + name.length).split('"')[0]);
}
async function until(test, timeoutMs) {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    if (test()) return true;
    await wait(450);
  }
  return test();
}
function setting(key,value) {
  if (value === 'null') run('shell','settings','delete','secure',key);
  else run('shell','settings','put','secure',key,value);
}
function feed() { run('shell','am','start','-n',pkg+'/.FeedActivity'); }
const viewing = () => amount('viewing','total.'+pkg);
async function prepare(mode) {
  run('shell','input','keyevent','KEYCODE_HOME');
  setting('enabled_accessibility_services','null');
  requireCheck(run('shell','am','instrument','-w','-e',mode,'true',instrument).includes('PASS:'), mode);
  setting('enabled_accessibility_services',service);
  setting('accessibility_enabled','1');
  await wait(1400);
  feed();
}
async function main() {
  requireCheck(serial.startsWith('emulator-') && run('shell','getprop','ro.kernel.qemu') === '1' &&
    run('emu','avd','name').split(/\s+/)[0] === 'PauseApi36','Dedicated Pause emulator');
  const oldServices = run('shell','settings','get','secure','enabled_accessibility_services');
  const oldEnabled = run('shell','settings','get','secure','accessibility_enabled');
  const permission='android.permission.POST_NOTIFICATIONS';
  const granted=run('shell','dumpsys','package',pkg).includes(permission+': granted=true');
  try {
    run('shell','input','keyevent','KEYCODE_WAKEUP');
    run('shell','wm','dismiss-keyguard');
    if(!granted)run('shell','pm','grant',pkg,permission);
    await prepare('prepareTimer');
    requireCheck(await until(()=>amount('pause',pkg+'.used')>0,12000),'Feed starts interval');
    const before=viewing();
    run('shell','am','broadcast','-n',pkg+'/.NotifyReceiver','--ei','id','306');
    await wait(3500);
    requireCheck(viewing()>=before+2500,'Foreground viewing continues under a heads-up notification');
    run('shell','input','keyevent','KEYCODE_HOME');
    await wait(1100);
    const home=viewing();
    await wait(2200);
    requireCheck(viewing()===home,'Time freezes on Home');
    feed();
    requireCheck(await until(()=>top().includes('BreakActivity'),70000),'Accessibility opens regular break');
    const elapsed=viewing();
    requireCheck(elapsed>=60000 && elapsed<63000,'Break after one active minute ('+elapsed+' ms)');
    await wait(2200);
    requireCheck(viewing()===elapsed,'Time solving the question is excluded from viewing');

    await prepare('prepareLimit');
    requireCheck(await until(()=>top().includes('LimitActivity'),75000),'Daily limit applies after study goal');
    requireCheck(amount('pause',pkg+'.used')===0,'Study goal suppresses ordinary interval');
    const capped=viewing();
    requireCheck(capped>=60000 && capped<63000,'Per-app allowance exhausted ('+capped+' ms)');
    await wait(1500);
    requireCheck(viewing()===capped,'Limit screen excluded from viewing');
    run('shell','input','keyevent','KEYCODE_BACK');
    requireCheck(await until(()=>!top().includes('LimitActivity') && !top().includes('FeedActivity'),5000),'Back on limit screen returns Home');
    feed();
    requireCheck(await until(()=>top().includes('LimitActivity'),7000),'Reopening capped feed returns to daily limit');
  } finally {
    run('shell','input','keyevent','KEYCODE_HOME');
    run('shell','am','broadcast','-n',pkg+'/.NotifyReceiver','--ez','clear','true');
    setting('enabled_accessibility_services',oldServices);
    setting('accessibility_enabled',oldEnabled);
    if(!granted)run('shell','pm','revoke',pkg,permission);
  }
}
main().catch(error=>{console.error(error.message);process.exitCode=1;});
