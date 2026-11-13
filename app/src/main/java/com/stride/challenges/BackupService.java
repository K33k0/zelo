package com.stride.challenges;

import android.app.job.*;
import android.content.*;
import android.net.Uri;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.json.*;

public class BackupService extends JobService {

  private static final int JOB = 4201;
  private static final int SAFETY_JOB = 4202;
  private static final java.util.concurrent.ExecutorService executor =
    java.util.concurrent.Executors.newSingleThreadExecutor();

  private static SharedPreferences prefs(Context c) {
    return c.getSharedPreferences("zelo-backup", 0);
  }

  public static void schedule(Context c, boolean now) {
    SharedPreferences p = prefs(c);
    if (!p.getBoolean("enabled", false) || p.getBoolean("reconnect", false)) return;
    JobScheduler scheduler = c.getSystemService(JobScheduler.class);
    if (scheduler.getPendingJob(SAFETY_JOB) == null) {
      JobInfo.Builder safety = new JobInfo.Builder(
        SAFETY_JOB,
        new ComponentName(c, BackupService.class)
      )
        .setPeriodic(15 * 60 * 1000)
        .setPersisted(true);
      if (
        p.getString("uri", "").contains("com.google.android.apps.docs")
      ) safety.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);
      scheduler.schedule(safety.build());
    }
    if (!now && scheduler.getPendingJob(JOB) != null) return;
    JobInfo.Builder job = new JobInfo.Builder(JOB, new ComponentName(c, BackupService.class))
      .setMinimumLatency(now ? 0 : 15000)
      .setBackoffCriteria(60000, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
      .setPersisted(true);
    String uri = p.getString("uri", "");
    if (uri.contains("com.google.android.apps.docs")) job.setRequiredNetworkType(
      JobInfo.NETWORK_TYPE_ANY
    );
    if (scheduler.schedule(job.build()) == JobScheduler.RESULT_FAILURE) p.edit()
      .putString("error", "Couldn’t schedule a backup. Open Zelo and try again.")
      .apply();
  }

  public static void connect(Context c, Uri uri, String name) {
    c.getSystemService(JobScheduler.class).cancel(JOB);
    c.getSystemService(JobScheduler.class).cancel(SAFETY_JOB);
    // A new destination receives a new history; old backup files remain untouched.
    prefs(c)
      .edit()
      .putBoolean("enabled", true)
      .putBoolean("reconnect", false)
      .putString("historyFile", "backup-history-" + java.util.UUID.randomUUID() + ".json")
      .putString("uri", uri.toString())
      .putString("name", name)
      .remove("error")
      .remove("lastSaved")
      .putInt("savedRevision", -1)
      .apply();
    schedule(c, true);
  }

  public static void stop(Context c) {
    prefs(c).edit().putBoolean("enabled", false).apply();
    c.getSystemService(JobScheduler.class).cancel(JOB);
    c.getSystemService(JobScheduler.class).cancel(SAFETY_JOB);
  }

  public static String status(Context c) {
    try {
      SharedPreferences p = prefs(c);
      JSONObject result = new JSONObject()
        .put("enabled", p.getBoolean("enabled", false))
        .put("reconnect", p.getBoolean("reconnect", false))
        .put("name", p.getString("name", ""))
        .put("lastSaved", p.getString("lastSaved", ""))
        .put("error", p.getString("error", ""))
        .put("pending", c.getSystemService(JobScheduler.class).getPendingJob(JOB) != null);
      String raw = ProgressStore.read(c);
      result.put(
        "hasChanges",
        raw != null && new JSONObject(raw).optInt("revision", 0) != p.getInt("savedRevision", -1)
      );
      return result.toString();
    } catch (Exception e) {
      return "{}";
    }
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    executor.execute(() -> {
      boolean retry = writeBackup(this);
      jobFinished(params, retry);
      if (!retry && params.getJobId() == JOB) {
        try {
          String raw = ProgressStore.read(this);
          if (
            raw != null &&
            new JSONObject(raw).optInt("revision", 0) != prefs(this).getInt("savedRevision", -1)
          ) schedule(this, false);
        } catch (Exception e) {
          android.util.Log.w("Zelo", "Backup follow-up deferred", e);
        }
      }
    });
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    return prefs(this).getBoolean("enabled", false) && !prefs(this).getBoolean("reconnect", false);
  }

  static synchronized boolean writeBackup(Context c) {
    SharedPreferences p = prefs(c);
    if (!p.getBoolean("enabled", false) || p.getBoolean("reconnect", false)) return false;
    String destination = p.getString("uri", "");
    try {
      String raw = ProgressStore.read(c);
      if (raw == null) return false;
      JSONObject data = new JSONObject(raw);
      if (
        data.optInt("revision", 0) == p.getInt("savedRevision", -1) &&
        p.getString("error", "").isEmpty()
      ) return false;
      AtomicFile local = new AtomicFile(
        new File(c.getFilesDir(), p.getString("historyFile", "backup-history.json"))
      );
      String previous = null;
      if (local.getBaseFile().exists()) try (InputStream in = local.openRead()) {
        previous = read(in);
      }
      String history = BackupArchive.append(previous, data, Instant.now().toString());
      byte[] bytes = history.getBytes(StandardCharsets.UTF_8);
      FileOutputStream out = null;
      try {
        out = local.startWrite();
        out.write(bytes);
        local.finishWrite(out);
      } catch (Exception e) {
        if (out != null) local.failWrite(out);
        throw e;
      }
      if (
        !p.getBoolean("enabled", false) || !destination.equals(p.getString("uri", ""))
      ) return false;
      try (
        OutputStream remote = c.getContentResolver().openOutputStream(Uri.parse(destination), "wt")
      ) {
        if (remote == null) throw new IOException("No backup stream");
        remote.write(bytes);
        remote.flush();
      }
      if (destination.equals(p.getString("uri", ""))) p.edit()
        .putString("lastSaved", Instant.now().toString())
        .putInt("savedRevision", data.optInt("revision", 0))
        .remove("error")
        .apply();
      // A log may have changed during the write. Ask Android to run again with the latest revision.
      String latest = ProgressStore.read(c);
      return (
        latest != null && new JSONObject(latest).optInt("revision", 0) != data.optInt("revision", 0)
      );
    } catch (SecurityException e) {
      p.edit()
        .putBoolean("reconnect", true)
        .putString("error", "Access to the backup file changed. Choose a backup file again.")
        .apply();
      return false;
    } catch (Exception e) {
      p.edit()
        .putString(
          "error",
          "Couldn’t update the backup file. Zelo will retry; your progress is safe on this device."
        )
        .apply();
      android.util.Log.e("Zelo", "Backup write failed", e);
      return true;
    }
  }

  private static String read(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] b = new byte[8192];
    int n;
    while ((n = in.read(b)) != -1) {
      out.write(b, 0, n);
      if (out.size() > 5_000_000) throw new IOException("Backup too large");
    }
    return out.toString("UTF-8");
  }
}
