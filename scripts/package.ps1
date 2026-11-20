$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot -Parent
$version=[regex]::Match([IO.File]::ReadAllText("$root/app/build.gradle"),"versionName '([^']+)'").Groups[1].Value
if(!$version){throw 'Missing versionName'}
$dist=Join-Path $root 'dist'
New-Item -ItemType Directory -Force $dist | Out-Null
$apk=Join-Path $root 'app/build/outputs/apk/release/app-release.apk'
if(!(Test-Path $apk)){throw 'Build a signed release first.'}
Copy-Item -LiteralPath $apk -Destination "$dist/pause-$version.apk"
Copy-Item -LiteralPath "$root/docs/INSTALL.md" -Destination "$dist/INSTALL.md"
$zipPath="$dist/pause-$version-source.zip"
# Explicit source allowlist: no build output, SDK, local properties, signing key or password.
$files=@('settings.gradle','build.gradle','gradle.properties','gradlew','gradlew.bat','.gitignore','LICENSE','README.md','app/build.gradle','fixture/build.gradle','snapshot/build.gradle') | ForEach-Object {Get-Item -LiteralPath (Join-Path $root $_)}
foreach($directory in @('app/src','fixture/src','snapshot/src','gradle','scripts','docs','.github')) {$files+=Get-ChildItem -LiteralPath (Join-Path $root $directory) -File -Recurse -Force}
$stream=[IO.File]::Open($zipPath,[IO.FileMode]::Create)
$zip=[IO.Compression.ZipArchive]::new($stream,[IO.Compression.ZipArchiveMode]::Create)
try {
    foreach($file in $files){
        $relative=[IO.Path]::GetRelativePath($root,$file.FullName).Replace('\','/')
        if($relative -match '(^|/)(\.private|\.toolchain|build|\.gradle)/|\.(jks|keystore)$|local\.properties'){throw "Excluded file: $relative"}
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip,$file.FullName,"pause/$relative",[IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally {$zip.Dispose();$stream.Dispose()}
$lines=foreach($name in @("pause-$version.apk","pause-$version-source.zip")){ $hash=(Get-FileHash -LiteralPath "$dist/$name" -Algorithm SHA256).Hash.ToLowerInvariant(); "$hash  $name" }
[IO.File]::WriteAllLines("$dist/SHA256SUMS-$version.txt",$lines,[Text.UTF8Encoding]::new($false))
Get-ChildItem -LiteralPath $dist -File | Select-Object Name,Length
