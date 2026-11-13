$ErrorActionPreference='Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    node scripts/native-fixtures.js
    & '.tools/jdk/jdk-17.0.17+10/bin/javac.exe' -encoding UTF-8 -cp .tools/json.jar -d .tools/native-tests app/src/main/java/com/stride/challenges/GoalEngine.java app/src/main/java/com/stride/challenges/ReminderPolicy.java app/src/main/java/com/stride/challenges/ReminderMessages.java app/src/main/java/com/stride/challenges/BackupArchive.java tests/native/EngineParity.java tests/native/ReminderBackupTest.java tests/native/ReminderCopyTest.java
    if ($LASTEXITCODE -ne 0) { throw 'Native test compilation failed.' }
    & '.tools/jdk/jdk-17.0.17+10/bin/java.exe' -cp '.tools/json.jar;.tools/native-tests' EngineParity .tools/native-tests/fixtures.json
    if ($LASTEXITCODE -ne 0) { throw 'Native calculation parity failed.' }
    & '.tools/jdk/jdk-17.0.17+10/bin/java.exe' -cp '.tools/json.jar;.tools/native-tests' ReminderBackupTest
    if ($LASTEXITCODE -ne 0) { throw 'Reminder/backup tests failed.' }
    & '.tools/jdk/jdk-17.0.17+10/bin/java.exe' -cp '.tools/json.jar;.tools/native-tests' ReminderCopyTest
    if ($LASTEXITCODE -ne 0) { throw 'Reminder message tests failed.' }
} finally {Pop-Location}
