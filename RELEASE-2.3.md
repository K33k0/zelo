# Zelo 2.3

- Insights now says **“Your goals, viewed over time.”** It does not presume a particular activity.
- **Scheduled days** replaces fitness-specific “training days” throughout visible interface copy. **Daily average** replaces “Training-day average.”
- Ten varied, month-free challenge-name placeholders use a shuffled rotation. Each new form gets one example; the placeholder does not keep changing while you type. All ten appear before repeating.
- Zero is always displayed as **0**, including rounded negative zero and tiny floating-point remainders. Completed work cannot generate a zero-amount reminder.
- Thirty original, cheeky reminder phrases include the challenge name and remaining amount/unit. They rotate daily per challenge and keep the same line when an existing notification updates. They use the existing opted-in reminder schedule; no new reminder times are added. Completed, unscheduled and reminders-off challenges stay quiet. Old-day notifications are cleared when the date changes.

## Weekly review corrections

The review now opens on **This week**, with an explicit Monday–Sunday date range. **Last week** is a separate view of the completed week.

The rolling seven-day chart is labelled **Recent activity**, keeping it distinct from the calendar-week review.

- **Flexible totals:** report actual logged effort and overall pace, carrying earlier progress forward. Completing a goal early does not invent missed daily targets. Last week excludes current-week logs.
- **Session goals:** count successful eligible days towards that week’s quota. This week separates completed targets, opportunities still remaining, and targets that can no longer be met. Future days are not labelled missed.
- Weeks that only partly overlap a challenge count only eligible scheduled dates. Entries on unscheduled days remain in the activity totals without fulfilling session targets.
- Reviews use the challenge’s current goal and schedule. Changing those settings changes the plan used for comparison; dated activity records remain unchanged.

Verification includes rotation coverage, unsigned-zero and floating-point cases, weekly date boundaries, cumulative progress, quota carryover rules, native reminder opt-in/completion guards, and existing overnight refresh tests. The phrase pool is in `app/src/main/java/com/stride/challenges/ReminderMessages.java`.
