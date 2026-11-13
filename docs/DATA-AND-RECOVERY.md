# Data, backups and recovery

## Portable format

Manual backups are JSON objects with `version` (1 or 2) and a `challenges` array. Android storage also has a monotonically increasing `revision`; portable imports do not adopt it as their concurrency token.

A challenge requires `id`, `name`, `mode` (`total` or `daily`), positive numeric `goal`, `unit`, inclusive ISO date strings `start` and `end`, and `logs`. Each log requires a unique string `id`, ISO `date` inside the challenge range, positive numeric `amount`, and string `note`. Optional fields include `schedule` (`days`: Sunday=0 through Saturday=6, and `weekly` quota), `capacity` (`weekday` and `weekend` limits for total goals), `quickAdd`, `reminder` (`enabled`, `time`), `link`, `seriesId`, `adjustments`, and `createdAt`. See `validateChallenge` for exact bounds and optional-field validation. Units are labels; changing a unit does not convert logged amounts.

Existing logs may be later than the phone's current date after a clock or timezone change. They stay stored and are excluded from totals until their date arrives. New and edited logs must be dated today or earlier. Historical plan comparisons use the current challenge settings, not a snapshot of its original settings.

Automatic archives have `format: "zelo-backup-history"`, `version: 1`, and `snapshots`. Each snapshot has `savedAt` (ISO timestamp), `revision`, and independently restorable `data` in the portable format. Up to ten snapshots are retained; oldest snapshots are dropped to keep the archive within 5 MB. Import is limited to 5 MB. Restoring replaces all challenges after an explicit confirmation; it does not merge them.

## Saved data needs attention

Ordinary saves are blocked when saved progress cannot be parsed or validated. Closing the warning does not remove this protection. Restore a known-good manual backup or choose a readable version from an automatic archive.

On explicit recovery, the unreadable original is preserved: Android stores `recovery-data` in private `zelo` preferences, and browser preview stores `stride-recovery-v1` in localStorage. This is a developer recovery copy, not an extra cloud backup or a user-facing export. It survives subsequent ordinary saves, but a later recovery replaces it. Do not clear app/browser data while investigating. Android recovery checks the exact original snapshot so a newer widget/app update cannot be silently overwritten. If data changes during recovery, reload and review the restore again.

If no backup exists, preserve the device data and investigate before reinstalling. System Android backup is disabled. Export or enable automatic backup before changing phones, signing keys, or clearing storage.

## Backup and reminder troubleshooting

- **Backup needs attention:** check the selected file provider and connection; use Back up now. If permission changed, choose the destination again. Last saved confirms the provider write, not completion of a remote Drive upload.
- **Restore fails:** check supported versions, the 5 MB file limit and challenge validation. Automatic archives offer only readable snapshots. A stale recovery needs reloading before another attempt.
- **No reminder:** enable it on the challenge, allow Android notifications, and check that today is scheduled with outstanding work. Completed daily targets and weekly quotas stay quiet. Inexact alarms may be delayed by battery settings; reopen a force-stopped app.
- **Different totals after moving the clock backwards:** later-dated records remain intact and become visible in totals when that date arrives.
- **Update cannot install:** the package signing identity must match. Export first; uninstalling removes private progress.
