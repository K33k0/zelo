# Zelo 2.3.2

- Recover unreadable saved progress using a known-good backup, preserving the original and rejecting stale recovery writes.
- Block ordinary saves after a load failure, including in browser preview.
- Keep existing logs readable after backwards clock/timezone changes; continue rejecting future-dated new entries and edits.
- Use capacity-aware original daily targets for consistency calendar markers, matching streaks.
- Add setup, architecture, backup-format and recovery documentation, formatting checks, and GitHub CI/release workflows.
- Preserve the original personal APK signing identity for local and tagged GitHub builds when the configured key is available.

Validation: 34 unit tests, 36 browser flows, 432 Java/JavaScript parity cases and reminder/backup regressions. Device recovery tests are compiled separately; they require a phone or emulator to execute.
