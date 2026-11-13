package com.stride.challenges;

import java.time.*;
import org.json.JSONObject;

/** Decide the next reminder without dropping a same-day alarm delayed by Android. */
public final class ReminderPolicy {

  public static long next(
    JSONObject item,
    ZonedDateTime now,
    String evaluatedDay,
    long previous,
    String previousSetting
  ) throws Exception {
    JSONObject reminder = item.optJSONObject("reminder");
    if (
      reminder == null ||
      !reminder.optBoolean("enabled") ||
      GoalEngine.calculate(item, now.toLocalDate()).complete
    ) return 0;
    LocalDate start = LocalDate.parse(item.getString("start")),
      end = LocalDate.parse(item.getString("end"));
    LocalTime time = LocalTime.parse(reminder.getString("time"));
    LocalDate date = now.toLocalDate().isBefore(start) ? start : now.toLocalDate();
    boolean pendingToday =
      previous > 0 &&
      Instant.ofEpochMilli(previous)
        .atZone(now.getZone())
        .toLocalDate()
        .equals(now.toLocalDate()) &&
      time.toString().equals(previousSetting);
    if (date.equals(now.toLocalDate()) && !date.atTime(time).atZone(now.getZone()).isAfter(now)) {
      if (
        pendingToday &&
        !date.toString().equals(evaluatedDay) &&
        GoalEngine.calculate(item, date).due > 0
      ) return now.plusSeconds(1).toInstant().toEpochMilli();
      date = date.plusDays(1);
    }
    for (; !date.isAfter(end); date = date.plusDays(1)) if (
      GoalEngine.scheduled(item, date) && !date.toString().equals(evaluatedDay)
    ) return date.atTime(time).atZone(now.getZone()).toInstant().toEpochMilli();
    return 0;
  }
}
