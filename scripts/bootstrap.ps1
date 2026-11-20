$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$tool = Join-Path $root '.toolchain'
New-Item -ItemType Directory -Force $tool | Out-Null
function Fetch($url, $file, $hash, $algorithm='SHA256') {
    if (!(Test-Path -LiteralPath $file)) {
        & curl.exe -sS --fail -L --retry 3 $url -o $file
        if ($LASTEXITCODE) { throw "Download failed: $url" }
    }
    if ($hash -and (Get-FileHash -LiteralPath $file -Algorithm $algorithm).Hash -ne $hash) { throw "Checksum mismatch: $file" }
}
Fetch 'https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=x64&image_type=jdk&os=windows' "$tool/jdk-assets.json" ''
$jdk = (Get-Content "$tool/jdk-assets.json" -Raw | ConvertFrom-Json)[0].binary.package
Fetch $jdk.link "$tool/jdk.zip" $jdk.checksum
if (!(Test-Path "$tool/jdk")) { Expand-Archive "$tool/jdk.zip" "$tool/jdk" }
Fetch 'https://services.gradle.org/distributions/gradle-8.13-bin.zip.sha256' "$tool/gradle.sha256" ''
Fetch 'https://services.gradle.org/distributions/gradle-8.13-bin.zip' "$tool/gradle.zip" (Get-Content "$tool/gradle.sha256" -Raw).Trim()
if (!(Test-Path "$tool/gradle-8.13")) { Expand-Archive "$tool/gradle.zip" $tool }
Fetch 'https://dl.google.com/android/repository/repository2-1.xml' "$tool/repository.xml" ''
[xml]$repo = Get-Content "$tool/repository.xml" -Raw
$sdk = Join-Path $tool 'android-sdk'
New-Item -ItemType Directory -Force $sdk | Out-Null
$pkg = $repo.SelectNodes('//*[local-name()="remotePackage"]') | Where-Object path -eq 'cmdline-tools;19.0' | Select-Object -First 1
$archive = $pkg.archives.archive | Where-Object { $_.'host-os' -eq 'windows' }
Fetch ("https://dl.google.com/android/repository/" + $archive.complete.url) "$tool/cmdline19.zip" ([string]$archive.complete.checksum) 'SHA1'
if (!(Test-Path "$sdk/cmdline-tools/19.0")) {
    Expand-Archive "$tool/cmdline19.zip" "$tool/cmdline19-extract" -Force
    New-Item -ItemType Directory -Force "$sdk/cmdline-tools" | Out-Null
    $source = [IO.Path]::GetFullPath("$tool/cmdline19-extract/cmdline-tools")
    $destination = [IO.Path]::GetFullPath("$sdk/cmdline-tools/19.0")
    if (!$source.StartsWith($tool+[IO.Path]::DirectorySeparatorChar) -or !$destination.StartsWith($tool+[IO.Path]::DirectorySeparatorChar)) { throw 'Invalid extraction paths' }
    Move-Item -LiteralPath $source -Destination $destination
}
$env:JAVA_HOME = (Get-ChildItem "$tool/jdk" -Directory | Select-Object -First 1).FullName
$env:ANDROID_HOME = $sdk
$env:PATH = "$env:JAVA_HOME/bin;$env:PATH"
# The Android SDK license must be reviewed and accepted by the person building.
& "$sdk/cmdline-tools/19.0/bin/sdkmanager.bat" --sdk_root=$sdk 'platforms;android-36' 'build-tools;35.0.0' 'platform-tools'
if ($LASTEXITCODE) { throw 'SDK installation failed or licenses not accepted.' }
# Java .properties files require Unicode escapes for non-ASCII paths.
$sdkProperty = $sdk.Replace('\','/').Replace(':','\:')
$escaped = -join ($sdkProperty.ToCharArray() | ForEach-Object { if ([int]$_ -gt 127) { '\u{0:x4}' -f [int]$_ } else { [string]$_ } })
[IO.File]::WriteAllText("$root/local.properties", "sdk.dir=$escaped`n", [Text.UTF8Encoding]::new($false))
Write-Output 'Toolchain ready. Run scripts/build.ps1.'
