package com.stride.challenges;

import java.nio.charset.StandardCharsets;
import org.json.*;

/** One portable file containing up to ten recent, independently restorable snapshots. */
public final class BackupArchive {

  public static String append(String previous, JSONObject data, String savedAt) throws Exception {
    JSONObject archive =
      previous == null
        ? new JSONObject()
            .put("format", "zelo-backup-history")
            .put("version", 1)
            .put("snapshots", new JSONArray())
        : new JSONObject(previous);
    if (!"zelo-backup-history".equals(archive.optString("format"))) throw new Exception(
      "Unrecognised backup history"
    );
    JSONArray snapshots = archive.getJSONArray("snapshots");
    int revision = data.optInt("revision", 0);
    if (
      snapshots.length() == 0 ||
      snapshots.getJSONObject(snapshots.length() - 1).optInt("revision", -1) != revision
    ) snapshots.put(
      new JSONObject().put("savedAt", savedAt).put("revision", revision).put("data", data)
    );
    while (snapshots.length() > 10) snapshots.remove(0);
    while (
        archive.toString().getBytes(StandardCharsets.UTF_8).length > 5_000_000 &&
        snapshots.length() > 1
      )
      snapshots.remove(0);
    String text = archive.toString();
    if (text.getBytes(StandardCharsets.UTF_8).length > 5_000_000) throw new Exception(
      "Backup exceeds 5 MB"
    );
    return text;
  }
}
