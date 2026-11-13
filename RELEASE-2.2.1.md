# Zelo 2.2.1 — overnight date refresh

Fixes stale Today screens when the app stays open across midnight or returns from sleep with suspended JavaScript timers.

- Refresh at the next local midnight, with the existing periodic check as a fallback.
- Recheck when Android resumes, the window regains focus, the device unlocks, or the system date/time zone changes.
- Browser visibility, focus, and page restoration use the same refresh path.
- Recheck before handling an action so the first tap after sleep uses the current day.
- An unfinished new log follows the new day if its default date was untouched. Amount and note drafts are preserved. Explicitly selected dates and existing historical logs are not changed.
- Existing logs are retained. Yesterday’s activity stays in Activity; Today shows the current local day’s targets and contributions.

Regression tests control the clock rather than changing the phone’s system date. They cover midnight, suspended timers, native resume, open drafts, explicit dates, first-action saving, and month rollover. The older browser fixtures now use a fixed date so the suite itself does not break as real days pass.
