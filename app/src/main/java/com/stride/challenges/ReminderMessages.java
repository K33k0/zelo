package com.stride.challenges;

import java.time.LocalDate;
import org.json.JSONObject;

/** Original, challenge-neutral reminders. Tease the procrastination, not the person. */
public final class ReminderMessages {

  private static final String[] PHRASES = {
    "{remaining} left for “{name}”. Your plan has noticed the creative absence of progress.",
    "“{name}”: {remaining} left today. “Later” has contributed absolutely nothing so far.",
    "A thrilling update: “{name}” still needs {remaining}. Staring at this won’t count.",
    "Remember “{name}”? It remembers you. {remaining} left today. Go say hello.",
    "“{name}” needs {remaining}. Your excuses have had plenty of screen time. Your turn.",
    "{remaining} left on “{name}”. Apparently the target refuses to complete itself. Rude.",
    "Quick question about “{name}”: were we doing this, or just naming it? {remaining} left.",
    "“{name}” has {remaining} left. An excellent moment to stop rehearsing your intentions.",
    "{remaining} left for “{name}”. Future you would like a word. Preferably after you’ve started.",
    "“{name}”: {remaining} remaining. This is the bit where the plan becomes something you actually do.",
    "Still {remaining} left on “{name}”. I’d offer to do it, but that would rather miss the point.",
    "Your daily plot twist could be finishing “{name}”. {remaining} left. Astonish us.",
    "“{name}” is waiting for {remaining}. Bold of you to assume another notification will do the work.",
    "{remaining} left for “{name}”. The deadline has declined your invitation to move itself.",
    "“{name}”: {remaining} left today. Let’s give that beautifully organised plan an actual result.",
    "I checked “{name}” again. Still {remaining} left. The suspense is becoming unnecessary.",
    "{remaining} left on “{name}”. You set the target. I’m merely being inconveniently specific.",
    "“{name}” needs {remaining}. Consider this your very polite interruption of absolutely everything else.",
    "{remaining} left for “{name}”. A tiny bit of action would ruin your perfect procrastination record.",
    "“{name}”: {remaining} remaining. Shall we turn that ambitious title into evidence?",
    "Checking on “{name}”. {remaining} left. Still waiting for “any minute now” to become a time.",
    "{remaining} left on “{name}”. The progress bar would love to experience movement today.",
    "“{name}” is asking for {remaining}. Your notification tray is not a completion strategy.",
    "{remaining} left for “{name}”. We’ve established the goal exists. Let’s establish the effort does too.",
    "“{name}”: {remaining} left. Feel free to prove this reminder unnecessary. Immediately, ideally.",
    "A friendly nudge for “{name}”. All right, a pointed one. {remaining} left today.",
    "{remaining} left on “{name}”. The plan was your idea. I have the receipts.",
    "“{name}” still needs {remaining}. Let’s retire the phrase “I’ll get round to it” for today.",
    "{remaining} left for “{name}”. Yes, this again. You did ask me to remind you.",
    "“{name}”: {remaining} left today. One less excuse, one more bit of progress. Off you go.",
  };

  public static int count() {
    return PHRASES.length;
  }

  public static String forChallenge(JSONObject item, GoalEngine.State state, LocalDate day) {
    JSONObject reminder = item.optJSONObject("reminder");
    if (
      reminder == null ||
      !reminder.optBoolean("enabled") ||
      !state.scheduled ||
      state.complete ||
      state.due <= 1e-9
    ) return null;
    int index = (int) Math.floorMod(
      day.toEpochDay() + (long) item.optString("id").hashCode(),
      (long) PHRASES.length
    );
    // Insert values in one pass so names containing placeholder-like text stay literal.
    String template = PHRASES[index];
    java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(
      "\\{name\\}|\\{remaining\\}"
    ).matcher(template);
    StringBuffer result = new StringBuffer();
    while (matcher.find())
      matcher.appendReplacement(
        result,
        java.util.regex.Matcher.quoteReplacement(
          matcher.group().equals("{name}")
            ? item.optString("name")
            : GoalEngine.number(state.due) + " " + item.optString("unit")
        )
      );
    matcher.appendTail(result);
    return result.toString();
  }
}
