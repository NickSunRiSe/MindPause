param([switch]$Release, [switch]$DeviceTests)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
# JDK 17 on Windows cannot reliably read Gradle worker argfiles with Cyrillic paths.
# A temporary drive alias keeps all project/cache paths ASCII without moving files.
if ($root -match '[^\x00-\x7F]') {
    $drive = @('P','Q','R','S','T','U','V','W','X','Y','Z') | Where-Object { !(Test-Path "${_}:\") } | Select-Object -First 1
    if (!$drive) { throw 'No free temporary drive letter; build from an ASCII path.' }
    & subst.exe "${drive}:" $root
    if ($LASTEXITCODE) { throw 'Could not create temporary build path' }
    try { & "${drive}:\scripts\build.ps1" -Release:$Release -DeviceTests:$DeviceTests }
    finally { & subst.exe "${drive}:" /d }
    return
}
$env:JAVA_HOME=(Get-ChildItem "$root/.toolchain/jdk" -Directory | Select-Object -First 1).FullName
$env:ANDROID_HOME="$root/.toolchain/android-sdk"
$env:GRADLE_USER_HOME="$root/.toolchain/gradle-cache"
$env:PATH="$env:JAVA_HOME/bin;$env:PATH"
if (!$env:JAVA_HOME) { throw 'Run scripts/bootstrap.ps1 first or build with Android Studio.' }
Push-Location $root
$localProperties="$root/local.properties"
$savedProperties=if(Test-Path $localProperties){[IO.File]::ReadAllBytes($localProperties)}else{$null}
try {
    $sdkProperty=$env:ANDROID_HOME.Replace('\','/').Replace(':','\:')
    [IO.File]::WriteAllText($localProperties,"sdk.dir=$sdkProperty`n",[Text.UTF8Encoding]::new($false))
    $gradle="$root/.toolchain/gradle-8.13/bin/gradle.bat"
    if (!(Test-Path "$root/gradlew.bat")) {
        & $gradle wrapper --gradle-version 8.13 --distribution-type bin
        if ($LASTEXITCODE) { throw 'Gradle wrapper generation failed' }
    }
    if ($Release) {
        New-Item -ItemType Directory -Force "$root/.private" | Out-Null
        $passwordFile="$root/.private/signing-password.txt"
        $keyStore="$root/.private/pause-release.jks"
        if (!(Test-Path $keyStore)) {
            $bytes=[byte[]]::new(32); [Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
            [IO.File]::WriteAllText($passwordFile,[Convert]::ToBase64String($bytes))
            & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $keyStore -storepass:file $passwordFile -keypass:file $passwordFile -alias pause -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=MindPause Local Release' -storetype JKS
            if ($LASTEXITCODE) { throw 'Signing key generation failed' }
        }
        $env:PAUSE_KEYSTORE=$keyStore
        $env:PAUSE_STORE_PASSWORD=[IO.File]::ReadAllText($passwordFile)
        $env:PAUSE_KEY_PASSWORD=$env:PAUSE_STORE_PASSWORD
        & $gradle --no-daemon :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
    } elseif ($DeviceTests) { & $gradle --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest :fixture:assembleDebug :snapshot:assembleDebug }
    else { & $gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug }
    if ($LASTEXITCODE) { throw 'Build or validation failed' }
} finally {
    if($null -ne $savedProperties){[IO.File]::WriteAllBytes($localProperties,$savedProperties)}
    Remove-Item Env:PAUSE_KEYSTORE,Env:PAUSE_STORE_PASSWORD,Env:PAUSE_KEY_PASSWORD -ErrorAction SilentlyContinue
    Pop-Location
}
