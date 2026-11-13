# Zelo · by ecletra

**Turn effort into progress.** An offline Android challenge planner built around the question: “What do I need to do today to finish on time?”

**Current release: 2.3.1.** Custom challenges start with a blank Unit field so its example placeholder is visible. Templates and existing challenges retain their units. [Neutral wording, rotating placeholders, cheeky reminders and weekly-review corrections](RELEASE-2.3.md). Reminders still require explicit opt-in and outstanding work.

[Overnight refresh fix](RELEASE-2.2.1.md): Today updates at midnight and when returning from sleep. Untouched default log dates follow the new day; historical and explicitly chosen dates are preserved.

Challenges can link to a charity event, social challenge, online challenge, or other event. In Create/Edit, expand **Link an event or challenge**, enable the link, and add a name and address. The detail page opens it in your browser. These are reference links: registration, fundraising and social activity remain on the external site; Zelo does not import or publish progress. Links are included in backups. Repeats only copy a link if **Keep the linked page** is selected.

See [the 2.1 release notes](RELEASE-2.1.md) for daily limits, Undo, calendar repeats, weekly reviews, reminder fixes, and automatic backups to a selected Drive/file destination.

Zelo is a spin on **Zelos**, the Greek spirit of zeal and emulation. Your benchmark is your own previous chapter. The name is a working product name, not a trademark/domain clearance. Mythology reference: https://www.theoi.com/Daimon/Zelos.html

## Install / update

`dist/zelo-debug.apk` is the development build for Android 8.0+. Install it over Stride: the application ID and signing identity are unchanged, so existing data is retained. The first Zelo launch migrates the old on-device data into shared native storage for widgets and reminders. Old version-1 JSON backups remain supported.

The previous Stride APK remains in `dist/stride-debug.apk` as a historical build. Do not uninstall before updating; uninstalling removes app data. The newer schema and native storage are not intended for downgrading to Stride.

## What’s included

1. **Today:** remaining effort on each scheduled challenge, inline quick-add, log forms, and completed daily targets.
2. **Flexible schedules:** choose training weekdays and rest days. Strict session goals can require every selected day or a quota such as 3 sessions per week. Weeks run Monday–Sunday; partial first/last weeks cap the quota to available eligible days.
3. **Catch-up planner:** flexible total goals can keep their deadline and redistribute remaining effort, or suggest a new deadline using the original pace. Preview the amount per remaining training day before confirming. Log data stays intact; plan changes are recorded.
4. **Android reminders and widget:** per-challenge reminders recheck outstanding work at delivery time. The home-screen widget shows remaining effort, cycles through challenges, and logs a configurable amount without opening the app.
5. **Forecasts and charts:** actual versus planned cumulative effort, estimated total-goal finish date, individual totals, training-day averages, streaks, and consistency calendar.
6. **Repeat and compare:** create a clean calendar-month or same-duration challenge, linked to the original. Compare chapters, completion percentage, active days, and your best single day. Reminders start disabled on repeats.

Also includes editable dated logs with notes, delete confirmations, activity filtering, weekly log counts, and portable JSON backup/restore.

## Use reminders and widgets

- Open a challenge → **Edit** → choose a reminder time and enable **Remind me when work remains**. On Android 13+, allow the system notification permission. **Settings → Notification settings** opens Android’s app settings if permission is denied.
- Open **Settings → Add home-screen widget**, or long-press your Android home screen → Widgets → Zelo. Confirm placement using the launcher’s prompt. Tap **Next** on the widget to select another challenge. Each widget keeps its own selection.
- Quick-add is configurable per challenge in **Edit**. Widget logs appear in Activity and can be corrected there.
- Reminders use inexact Android alarms, can be delayed by battery-saving settings, and do not require exact-alarm permission. Rest days, completed goals, met weekly quotas, and met daily targets remain quiet. Alarms are rescheduled after reboot, time-zone/time changes, package updates, and data changes. A force-stopped app needs reopening before alarms resume.
- Widget changes update app data using revisions. An open form based on older data is refused rather than overwriting a newer widget log.

## Calculation rules

**Flexible totals:** remaining work is divided across remaining training days, including today when scheduled. Today’s target is fixed using progress before today, then today’s logs are subtracted. Logging 25 toward a 100-rep day leaves 75, instead of moving the target as you log. You may still log effort on rest days; it counts toward the overall total.

**Strict sessions:** multiple entries on an eligible date combine. Extra work on one date does not replace another date or another week. Rest-day entries remain in your effort total, but do not fulfil required sessions. A weekly missed count appears when the quota can no longer be fulfilled by remaining eligible dates. With every-day scheduling, yesterday’s missed target is immediately visible.

**Forecast:** average effort per elapsed training day, including today, projected onto future eligible weekdays. It needs logged effort and is an estimate. The graph for weekly quotas spreads the weekly planned effort over eligible days; success itself is counted by target sessions, not by cumulative effort.

**Streak:** consecutive eligible dates meeting the original per-day pace (total goals) or session amount (strict goals). Rest days do not break a streak. This is a day-based measure even when a weekly quota is enabled. A current unfinished day does not break yesterday’s streak until the next eligible day.

## Design

The supplied Ecletra Brand Guidelines v1.2 informed the charcoal background, indigo primary controls, violet/lavender highlights, selective pink accents, local Inter typeface, and direct copy. The publisher credit appears in the header, settings, footer, and widget. The Z product icon is separate from the Ecletra ribbon; no substitute ribbon artwork has been invented. Inter’s OFL license is bundled at `app/src/main/assets/www/fonts/OFL.txt`.

Naming shortlist considered: **Kairo** (Kairos, opportunity), **Protea** (Proteus, adaptability and a link to Ecletra’s origin), and **Zelo** (Zelos, zeal and emulation). Zelo was selected by the user.

## Build / preview

```powershell
./scripts/build.ps1
# Install to a specific connected wireless phone:
./.tools/android-sdk/platform-tools/adb.exe -s IP:PORT install -r dist/zelo-debug.apk
```

Local Java 17, Gradle 8.11.1 and Android SDK 35 are under `.tools/`. The Gradle wrapper is included for other machines. With JDK 17 and `ANDROID_HOME` configured, use `./gradlew assembleDebug lintDebug`, or open the folder in Android Studio.

```powershell
npm ci
npm start
# http://127.0.0.1:4173
npm test
$env:PLAYWRIGHT_BROWSERS_PATH = "$PWD/.tools/browsers"
npm run test:browser
./scripts/test-native.ps1
```

For a new machine, install Chromium using `npx playwright install chromium`. The native parity script uses the local JDK and `.tools/json.jar` (org.json 20240303) to compare 432 Android/JavaScript calculation scenarios, plus reminder and backup-history regressions. The browser preview stores data locally in that browser; it does not sync with the phone.

## Verification and limits

- 33 calculation/validation tests and 30 browser flow tests cover the current app (including 9 overnight/date-refresh cases).
- 432 native/JavaScript calculation parity checks pass, plus reminder/backup regressions.
- Three isolated on-device storage/backup/Undo tests pass; see the 2.1 release notes.
- Android assembly and lint pass with no reported issues.
- An in-place update on the paired Moto G84 retained 2 challenges and 4 existing logs during migration.
- Actual notification delivery timing and widget placement/tapping still require use on the phone. Notification permission is requested only when the user enables reminders; widget placement uses the launcher’s confirmation.

This is a debug-signed personal testing build, not a Play Store release. No account, analytics, direct internet access, or hosted backend is included. Android uses native private preferences as the source of truth; bundled HTML/CSS/JS renders the UI in a restricted WebView at the original private origin. Optional backups use Android’s document provider and network-state permission to queue Drive-backed jobs. In the browser, localStorage remains the source. System Android backup is disabled; use Zelo’s explicit export or opt-in file backups before clearing data or changing phones.
