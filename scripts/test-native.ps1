$ErrorActionPreference='Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    $javaBin = if (Test-Path '.tools/jdk/jdk-17.0.17+10/bin/java.exe') { (Resolve-Path '.tools/jdk/jdk-17.0.17+10/bin').Path } elseif ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin' } else { Split-Path (Get-Command java -ErrorAction Stop).Source }
    $jsonJar = if ($env:ZELO_JSON_JAR) { $env:ZELO_JSON_JAR } else { Join-Path $PWD '.tools/json.jar' }
    if (!(Test-Path $jsonJar)) { throw 'Missing org.json dependency. Run ./scripts/setup.ps1 first.' }
    node scripts/native-fixtures.js
    if ($LASTEXITCODE -ne 0) { throw 'Native fixture generation failed.' }
    & (Join-Path $javaBin 'javac.exe') -encoding UTF-8 -cp $jsonJar -d .tools/native-tests app/src/main/java/com/stride/challenges/GoalEngine.java app/src/main/java/com/stride/challenges/ReminderPolicy.java app/src/main/java/com/stride/challenges/ReminderMessages.java app/src/main/java/com/stride/challenges/BackupArchive.java tests/native/EngineParity.java tests/native/ReminderBackupTest.java tests/native/ReminderCopyTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Native test compilation failed.' }
    & (Join-Path $javaBin 'java.exe') -cp "$jsonJar;.tools/native-tests" EngineParity .tools/native-tests/fixtures.json
    if ($LASTEXITCODE -ne 0) { throw 'Native calculation parity failed.' }
    & (Join-Path $javaBin 'java.exe') -cp "$jsonJar;.tools/native-tests" ReminderBackupTest
    if ($LASTEXITCODE -ne 0) { throw 'Reminder/backup tests failed.' }
    & (Join-Path $javaBin 'java.exe') -cp "$jsonJar;.tools/native-tests" ReminderCopyTest
    if ($LASTEXITCODE -ne 0) { throw 'Reminder message tests failed.' }
} finally {Pop-Location}
