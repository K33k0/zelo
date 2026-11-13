package com.stride.challenges;

import android.app.PendingIntent;
import android.appwidget.*;
import android.content.*;
import android.net.Uri;
import android.widget.RemoteViews;
import java.time.LocalDate;
import org.json.*;

public class ZeloWidget extends AppWidgetProvider {

  private static PendingIntent action(Context c, int widget, String action, String id) {
    Intent intent = new Intent(c, ZeloWidget.class)
      .setAction(action)
      .setData(Uri.parse("zelo://widget/" + widget + "/" + action))
      .putExtra("widget", widget)
      .putExtra("challenge", id);
    return PendingIntent.getBroadcast(
      c,
      widget,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
    );
  }

  @Override
  public void onUpdate(Context c, AppWidgetManager manager, int[] ids) {
    for (int id : ids) update(c, manager, id);
  }

  @Override
  public void onDeleted(Context c, int[] ids) {
    for (int id : ids)
      c.getSharedPreferences("zelo-widgets", 0)
        .edit()
        .remove("selected-" + id)
        .apply();
  }

  @Override
  public void onReceive(Context c, Intent intent) {
    super.onReceive(c, intent);
    String a = intent.getAction();
    int widget = intent.getIntExtra("widget", -1);
    if ("com.stride.challenges.ADD".equals(a)) {
      if (ProgressStore.quickAdd(c, intent.getStringExtra("challenge"))) ProgressStore.changed(c);
      else updateAll(c);
    }
    if ("com.stride.challenges.UNDO".equals(a)) {
      if (ProgressStore.undoWidget(c, intent.getStringExtra("challenge"))) ProgressStore.changed(c);
      else updateAll(c);
    }
    if ("com.stride.challenges.NEXT".equals(a) && widget != -1) {
      JSONArray all = ProgressStore.challenges(c);
      String current = c
        .getSharedPreferences("zelo-widgets", 0)
        .getString("selected-" + widget, "");
      int next = 0;
      for (int i = 0; i < all.length(); i++) if (
        all.optJSONObject(i).optString("id").equals(current)
      ) {
        next = (i + 1) % all.length();
        break;
      }
      if (all.length() > 0) c.getSharedPreferences("zelo-widgets", 0)
        .edit()
        .putString("selected-" + widget, all.optJSONObject(next).optString("id"))
        .apply();
      update(c, AppWidgetManager.getInstance(c), widget);
    }
  }

  public static void updateAll(Context c) {
    AppWidgetManager m = AppWidgetManager.getInstance(c);
    for (int id : m.getAppWidgetIds(new ComponentName(c, ZeloWidget.class))) update(c, m, id);
  }

  private static void update(Context c, AppWidgetManager m, int widget) {
    RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.zelo_widget);
    Intent launch = new Intent(c, MainActivity.class);
    v.setOnClickPendingIntent(
      R.id.widget_body,
      PendingIntent.getActivity(
        c,
        widget,
        launch,
        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
      )
    );
    JSONArray all = ProgressStore.challenges(c);
    String selected = c.getSharedPreferences("zelo-widgets", 0).getString("selected-" + widget, "");
    JSONObject choice = null;
    for (int i = 0; i < all.length(); i++) {
      JSONObject item = all.optJSONObject(i);
      if (item.optString("id").equals(selected)) {
        choice = item;
        break;
      }
    }
    if (choice == null && all.length() > 0) {
      choice = all.optJSONObject(0);
      c.getSharedPreferences("zelo-widgets", 0)
        .edit()
        .putString("selected-" + widget, choice.optString("id"))
        .apply();
    }
    try {
      if (choice == null) {
        v.setTextViewText(R.id.widget_title, "Give your effort a direction");
        v.setTextViewText(R.id.widget_due, "Create your first challenge");
        v.setViewVisibility(R.id.widget_add, android.view.View.GONE);
        v.setViewVisibility(R.id.widget_next, android.view.View.GONE);
      } else {
        GoalEngine.State s = GoalEngine.calculate(choice, LocalDate.now());
        String id = choice.getString("id"),
          unit = choice.getString("unit");
        v.setTextViewText(R.id.widget_title, choice.getString("name"));
        boolean ended = LocalDate.now().isAfter(LocalDate.parse(choice.getString("end")));
        v.setTextViewText(
          R.id.widget_due,
          s.complete
            ? "Challenge complete"
            : ended
              ? choice.getString("mode").equals("daily")
                ? "Ended · review your sessions in Zelo"
                : "Ended · " +
                  GoalEngine.number(Math.max(0, s.target - s.total)) +
                  " " +
                  unit +
                  " remaining"
              : !s.scheduled
                ? "No session scheduled today"
                : s.due <= 0
                  ? "Today’s work is done"
                  : GoalEngine.number(s.due) + " " + unit + " left today"
        );
        v.setProgressBar(R.id.widget_progress, 100, (int) (s.progress * 100), false);
        double amount = GoalEngine.quickAmount(choice);
        v.setTextViewText(R.id.widget_add, "+" + GoalEngine.number(amount) + " " + unit);
        v.setViewVisibility(
          R.id.widget_add,
          s.scheduled && !s.complete && s.due > 0
            ? android.view.View.VISIBLE
            : android.view.View.GONE
        );
        v.setOnClickPendingIntent(
          R.id.widget_add,
          action(c, widget, "com.stride.challenges.ADD", id)
        );
        v.setOnClickPendingIntent(
          R.id.widget_next,
          action(c, widget, "com.stride.challenges.NEXT", id)
        );
        JSONObject undo = ProgressStore.undoEntry(c, id);
        v.setViewVisibility(
          R.id.widget_undo,
          undo == null ? android.view.View.GONE : android.view.View.VISIBLE
        );
        if (undo != null) {
          v.setTextViewText(
            R.id.widget_undo,
            "Undo +" + GoalEngine.number(undo.getDouble("amount"))
          );
          v.setOnClickPendingIntent(
            R.id.widget_undo,
            action(c, widget, "com.stride.challenges.UNDO", id)
          );
        }
      }
    } catch (Exception e) {
      v.setTextViewText(R.id.widget_due, "Open Zelo to refresh");
    }
    m.updateAppWidget(widget, v);
  }
}
