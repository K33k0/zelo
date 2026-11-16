# Zelo 2.4.0

- Add an editable local time to progress logs and show times in Activity, ordered newest first within each date.
- Capture the current local time automatically for app quick-add and home-screen widget entries.
- Show effort by time of day in Insights: morning (06:00–11:59), afternoon (12:00–17:59), evening (18:00–23:59), and night (00:00–05:59). Each challenge keeps its own units.
- Preserve log times through editing, reloads, and backup/restore. Times stay as recorded when changing time zones.
- Keep older logs compatible without inventing times. Untimed effort is shown separately in Insights; add a time in Activity to include it.

Validation: 35 unit tests, 36 browser flows, 432 Java/JavaScript parity cases, reminder/backup regressions, Android assembly and lint. GitHub Actions builds and signs the release APK using the existing signing identity.
