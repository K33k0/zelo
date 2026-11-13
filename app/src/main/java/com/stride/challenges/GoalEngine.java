package com.stride.challenges;

import java.time.LocalDate;
import java.util.*;
import org.json.*;

/** Pure date/goal rules shared by the home-screen widget and reminder receiver. */
public final class GoalEngine {

  public static final class State {

    public double total, today, due, target, progress;
    public boolean scheduled, complete;
  }

  public static boolean scheduled(JSONObject c, LocalDate day) throws JSONException {
    if (
      day.isBefore(LocalDate.parse(c.getString("start"))) ||
      day.isAfter(LocalDate.parse(c.getString("end")))
    ) return false;
    JSONObject schedule = c.optJSONObject("schedule");
    if (schedule == null) return true;
    JSONArray days = schedule.getJSONArray("days");
    int n = day.getDayOfWeek().getValue() % 7;
    for (int i = 0; i < days.length(); i++) if (days.getInt(i) == n) return true;
    return false;
  }

  public static State calculate(JSONObject c, LocalDate now) throws JSONException {
    State s = new State();
    Map<String, Double> amounts = new HashMap<>();
    JSONArray logs = c.getJSONArray("logs");
    for (int i = 0; i < logs.length(); i++) {
      JSONObject l = logs.getJSONObject(i);
      String date = l.getString("date");
      if (LocalDate.parse(date).isAfter(now)) continue;
      double amount = l.getDouble("amount");
      s.total += amount;
      amounts.put(date, amounts.getOrDefault(date, 0.0) + amount);
    }
    s.today = amounts.getOrDefault(now.toString(), 0.0);
    double goal = c.getDouble("goal");
    boolean daily = c.getString("mode").equals("daily");
    int quota =
      c.optJSONObject("schedule") == null ? 7 : c.getJSONObject("schedule").optInt("weekly", 7);
    int remaining = 0;
    List<LocalDate> future = new ArrayList<>();
    Map<LocalDate, List<LocalDate>> groups = new HashMap<>();
    LocalDate start = LocalDate.parse(c.getString("start")),
      end = LocalDate.parse(c.getString("end"));
    for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
      if (!scheduled(c, d)) continue;
      if (!d.isBefore(now)) {
        remaining++;
        future.add(d);
      }
      LocalDate week = d.minusDays(d.getDayOfWeek().getValue() - 1);
      groups.computeIfAbsent(week, k -> new ArrayList<>()).add(d);
    }
    int required = 0,
      achieved = 0;
    boolean weekDone = false;
    for (List<LocalDate> days : groups.values()) {
      int q = Math.min(quota, days.size()),
        hits = 0;
      required += q;
      for (LocalDate d : days) if (amounts.getOrDefault(d.toString(), 0.0) + 1e-9 >= goal) hits++;
      achieved += Math.min(q, hits);
      if (days.contains(now) && hits >= q) weekDone = true;
    }
    s.target = daily ? required * goal : goal;
    s.complete = daily ? achieved == required : s.total + 1e-9 >= goal;
    s.scheduled = scheduled(c, now);
    s.progress = Math.min(
      1,
      daily ? (required == 0 ? 0 : (double) achieved / required) : s.total / goal
    );
    double planned = daily
      ? goal
      : todayAllocation(c, Math.max(0, goal - (s.total - s.today)), future, now);
    s.due = s.scheduled && !(daily && weekDone) ? Math.max(0, planned - s.today) : 0;
    if (s.due <= 1e-9) s.due = 0;
    return s;
  }

  public static String number(double n) {
    return java.math.BigDecimal.valueOf(Math.ceil((n - 1e-9) * 100) / 100)
      .stripTrailingZeros()
      .toPlainString();
  }

  public static double quickAmount(JSONObject c) {
    String text = (c.optString("unit") + " " + c.optString("name")).toLowerCase(
      java.util.Locale.ROOT
    );
    return c.optDouble("quickAdd", text.matches(".*(km|mile|run|walk).*") ? 1 : 25);
  }

  private static double cap(JSONObject c, LocalDate d) {
    JSONObject limits = c.optJSONObject("capacity");
    return limits == null
      ? Double.POSITIVE_INFINITY
      : limits.optDouble(
          d.getDayOfWeek().getValue() >= 6 ? "weekend" : "weekday",
          Double.POSITIVE_INFINITY
        );
  }

  private static double todayAllocation(
    JSONObject c,
    double amount,
    List<LocalDate> days,
    LocalDate now
  ) {
    days.sort(Comparator.comparingDouble(d -> cap(c, d)));
    double left = amount,
      result = 0;
    for (int i = 0; i < days.size(); i++) {
      LocalDate d = days.get(i);
      double share = Math.min(cap(c, d), left / (days.size() - i));
      if (d.equals(now)) result = share;
      left = Math.max(0, left - share);
    }
    return result;
  }
}
