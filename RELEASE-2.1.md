# Zelo 2.1

This release keeps Today focused on the next action and moves optional controls behind **Schedule & reminders**.

## What changed

- **Consistent planning:** catch-up previews show the same remaining-today amount as Today, followed by dated upcoming targets. Expired goals show outstanding work and an Extend deadline action.
- **Calendar repeats:** monthly challenges default to a full calendar month. Same-duration repeats remain available. Dates are shown before confirming.
- **Daily capacity:** optionally set weekday/weekend maximum planned amounts for a total goal. Zelo spreads effort evenly where possible and caps each planned day. Actual logs may exceed limits. If work no longer fits, the app explains the shortfall and offers a suitable date instead of silently increasing the limits.
- **Undo:** app quick-add offers Undo for ten seconds. Widgets offer Undo for the most recent unchanged widget quick-add on that challenge today. Editing an entry protects it from an older Undo action.
- **Weekly review:** Insights summarises the last completed Monday–Sunday week and the current week’s plan. Before the first eligible week, it explains when the review appears rather than showing irrelevant zero results.
- **Reminders:** partial logs no longer discard delayed same-day alarms. Reminders already shown update quietly and disappear once the day’s work is done.
- **Navigation:** Android Back closes a dialog, returns to the challenge list/Today, then exits from Today.

## Set up automatic backups

1. Open **Settings → Set up automatic backup → Choose backup file**.
2. In Android’s file picker, choose **Google Drive** or another writable location.
3. Save `zelo-backup-history.json` and allow ongoing access.

Automatic backup stays off until you choose a destination. It uses your phone’s document provider; no new account or Google developer credentials are required inside Zelo.

The file contains up to **10 independently restorable versions**, trimmed earlier if necessary to stay within 5 MB. Zelo queues saves after data changes, retries failures, and periodically checks for any changes missed during a write. A local archive is written atomically before updating the selected document. Settings shows the last successful file save, waiting changes, and actionable errors. Failed backups never replace your live progress.

**“Last saved” means the document provider accepted the update.** Google Drive controls cloud upload and may finish syncing later when offline. The app does not claim that a file has finished syncing to the cloud. This is backup/restore, not live synchronisation between devices. Actual Drive-provider behaviour still needs checking after selecting a destination on the phone.

To restore, open **Settings → Manual backup & restore → Restore from a file**. A history file offers a dated version chooser, then a summary and explicit replacement confirmation. Normal Zelo and old Stride JSON backups remain supported. Turning backups off leaves the existing backup file intact.

## Validation

- 23 calculation/validation tests and 14 browser interaction tests.
- 432 Android/JavaScript goal-calculation comparisons.
- Native regression checks for delayed reminders and backup-history retention/size limits.
- Three isolated tests on the Moto G84: actual document-provider backup writes with multiple versions, safe handling of revoked access, and widget Undo plus storage revision checks.
- Android build and lint pass. The phone’s original 2 challenges and 4 logs were retained.

The device tests used a temporary test provider and separate test preferences; they did not modify real challenges or configure a Google Drive destination. The temporary test APK was removed afterwards. Notification timing, launcher behaviour, and the chosen Drive provider remain subject to Android and the provider’s behaviour.
