param([switch]$Install)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    if (Test-Path '.tools/jdk/jdk-17.0.17+10') { $env:JAVA_HOME = (Resolve-Path '.tools/jdk/jdk-17.0.17+10').Path }
    if (Test-Path '.tools/android-sdk') { $env:ANDROID_HOME = (Resolve-Path '.tools/android-sdk').Path }
    $env:GRADLE_USER_HOME = Join-Path (Get-Location) '.tools/gradle-cache'
    if (Test-Path '.tools/gradle-8.11.1/bin/gradle.bat') { & '.tools/gradle-8.11.1/bin/gradle.bat' --no-daemon --console=plain assembleDebug lintDebug }
    elseif (Test-Path './gradlew.bat') { & './gradlew.bat' --no-daemon --console=plain assembleDebug lintDebug }
    else { throw 'Open this project in Android Studio to install the Android SDK and Gradle.' }
    if ($LASTEXITCODE -ne 0) { throw 'Android build failed.' }
    New-Item -ItemType Directory -Force 'dist' | Out-Null
    Copy-Item -LiteralPath 'app/build/outputs/apk/debug/app-debug.apk' -Destination 'dist/zelo-debug.apk' -Force
    if ($Install) {
        & "$env:ANDROID_HOME/platform-tools/adb.exe" install -r 'dist/zelo-debug.apk'
        if ($LASTEXITCODE -ne 0) { throw 'Connect an Android device with USB debugging enabled, then retry installation.' }
    }
} finally { Pop-Location }
