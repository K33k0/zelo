package com.stride.challenges;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import java.time.*;
import org.json.*;

public class ReminderReceiver extends BroadcastReceiver {

  public static final String CHANNEL = "zelo-progress";

  public static void channel(Context c) {
    c.getSystemService(NotificationManager.class).createNotificationChannel(
      new NotificationChannel(
        CHANNEL,
        "Challenge reminders",
        NotificationManager.IMPORTANCE_DEFAULT
      )
    );
  }

  private static PendingIntent pending(Context c, String id, long time) {
    String day =
      time > 0
        ? Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        : "";
    return PendingIntent.getBroadcast(
      c,
      0,
      new Intent(c, ReminderReceiver.class)
        .setAction("com.stride.challenges.REMIND")
        .setData(Uri.parse("zelo://reminder/" + Uri.encode(id)))
        .putExtra("challenge", id)
        .putExtra("day", day),
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );
  }

  public static synchronized void scheduleAll(Context c) {
    channel(c);
    AlarmManager alarm = c.getSystemService(AlarmManager.class);
    SharedPreferences prefs = c.getSharedPreferences("zelo-alarms", 0);
    java.util.Set<String> old = new java.util.HashSet<>(
        prefs.getStringSet("ids", new java.util.HashSet<>())
      ),
      ids = new java.util.HashSet<>();
    JSONArray all = ProgressStore.challenges(c);
    SharedPreferences.Editor edit = prefs.edit();
    for (int i = 0; i < all.length(); i++) try {
      JSONObject item = all.getJSONObject(i);
      String id = item.getString("id");
      long next = ReminderPolicy.next(
        item,
        ZonedDateTime.now(),
        prefs.getString("evaluated-" + id, ""),
        prefs.getLong("time-" + id, 0),
        prefs.getString("setting-" + id, "")
      );
      if (next > 0) {
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending(c, id, next));
        ids.add(id);
        edit
          .putLong("time-" + id, next)
          .putString("setting-" + id, item.getJSONObject("reminder").getString("time"));
      }
    } catch (Exception e) {
      android.util.Log.e("Zelo", "Reminder schedule failed", e);
    }
    for (String id : old)
      if (!ids.contains(id)) {
        alarm.cancel(pending(c, id, 0));
        edit.remove("time-" + id).remove("setting-" + id);
      }
    edit.putStringSet("ids", ids).apply();
  }

  private static void show(Context c, JSONObject item, GoalEngine.State s) throws Exception {
    String message = ReminderMessages.forChallenge(item, s, LocalDate.now());
    if (message == null) return;
    if (
      Build.VERSION.SDK_INT >= 33 &&
      c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) return;
    channel(c);
    PendingIntent launch = PendingIntent.getActivity(
      c,
      0,
      new Intent(c, MainActivity.class),
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );
    Notification n = new Notification.Builder(c, CHANNEL)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(item.getString("name"))
      .setContentText(message)
      .setStyle(new Notification.BigTextStyle().bigText(message))
      .setContentIntent(launch)
      .setOnlyAlertOnce(true)
      .setAutoCancel(true)
      .build();
    c.getSystemService(NotificationManager.class).notify(item.getString("id"), 1, n);
  }

  public static void refreshNotifications(Context c) {
    NotificationManager manager = c.getSystemService(NotificationManager.class);
    JSONArray all = ProgressStore.challenges(c);
    for (android.service.notification.StatusBarNotification shown : manager.getActiveNotifications())
      if (CHANNEL.equals(shown.getNotification().getChannelId())) {
        if (
          !java.time.Instant.ofEpochMilli(shown.getPostTime())
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .equals(LocalDate.now())
        ) {
          manager.cancel(shown.getTag(), shown.getId());
          continue;
        }
        JSONObject found = null;
        for (int i = 0; i < all.length(); i++) if (
          all.optJSONObject(i).optString("id").equals(shown.getTag())
        ) {
          found = all.optJSONObject(i);
          break;
        }
        try {
          GoalEngine.State s = found == null ? null : GoalEngine.calculate(found, LocalDate.now());
          if (
            s == null || s.due <= 0 || !found.optJSONObject("reminder").optBoolean("enabled")
          ) manager.cancel(shown.getTag(), shown.getId());
          else show(c, found, s);
        } catch (Exception e) {
          manager.cancel(shown.getTag(), shown.getId());
        }
      }
  }

  @Override
  public void onReceive(Context c, Intent intent) {
    if ("com.stride.challenges.REMIND".equals(intent.getAction())) {
      String id = intent.getStringExtra("challenge"),
        day = intent.getStringExtra("day");
      SharedPreferences prefs = c.getSharedPreferences("zelo-alarms", 0);
      if (
        LocalDate.now().toString().equals(day) &&
        !day.equals(prefs.getString("evaluated-" + id, ""))
      ) {
        prefs
          .edit()
          .putString("evaluated-" + id, day)
          .apply();
        JSONArray all = ProgressStore.challenges(c);
        for (int i = 0; i < all.length(); i++) try {
          JSONObject item = all.getJSONObject(i);
          if (!item.getString("id").equals(id)) continue;
          GoalEngine.State s = GoalEngine.calculate(item, LocalDate.now());
          if (
            s.due > 0 &&
            !s.complete &&
            item.optJSONObject("reminder") != null &&
            item.getJSONObject("reminder").optBoolean("enabled")
          ) show(c, item, s);
        } catch (Exception e) {
          android.util.Log.e("Zelo", "Reminder failed", e);
        }
      }
    }
    refreshNotifications(c);
    scheduleAll(c);
    ZeloWidget.updateAll(c);
    BackupService.schedule(c, false);
  }
}
