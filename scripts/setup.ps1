param([switch]$SkipBrowser)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    npm ci
    if ($LASTEXITCODE -ne 0) { throw 'npm dependency installation failed.' }
    New-Item -ItemType Directory -Force '.tools' | Out-Null
    $jsonJar = Join-Path $PWD '.tools/json.jar'
    $expectedHash = '3CF6CD6892E32E2B4C1C39E0F52F5248A2F5B37646FDFBB79A66B46B618414ED'
    if (!(Test-Path $jsonJar)) {
        Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/json/json/20240303/json-20240303.jar' -OutFile $jsonJar
    }
    if ((Get-FileHash -LiteralPath $jsonJar -Algorithm SHA256).Hash -ne $expectedHash) { throw 'org.json checksum mismatch. Replace .tools/json.jar with the official 20240303 artifact.' }
    if (!$SkipBrowser) {
        $env:PLAYWRIGHT_BROWSERS_PATH = Join-Path $PWD '.tools/browsers'
        npx playwright install chromium
        if ($LASTEXITCODE -ne 0) { throw 'Chromium installation failed.' }
    }
    Write-Host 'Dependencies ready. See docs/DEVELOPMENT.md for JDK 17 and Android SDK setup.'
} finally { Pop-Location }
