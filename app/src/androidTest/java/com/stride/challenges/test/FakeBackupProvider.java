package com.stride.challenges.test;

import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.*;

/** Test APK only. Never shipped in Zelo. Holds a single fixture, not user files. */
public class FakeBackupProvider extends ContentProvider {

  public boolean onCreate() {
    return true;
  }

  public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
    if ("/denied".equals(uri.getPath())) throw new SecurityException("Test revoked access");
    if (!"/fixture.json".equals(uri.getPath())) throw new FileNotFoundException();
    return ParcelFileDescriptor.open(
      new File(getContext().getFilesDir(), "fixture.json"),
      ParcelFileDescriptor.parseMode(mode)
    );
  }

  public String getType(Uri uri) {
    return "application/json";
  }

  public Cursor query(Uri u, String[] p, String s, String[] a, String o) {
    return null;
  }

  public Uri insert(Uri u, ContentValues v) {
    throw new UnsupportedOperationException();
  }

  public int delete(Uri u, String s, String[] a) {
    return 0;
  }

  public int update(Uri u, ContentValues v, String s, String[] a) {
    return 0;
  }
}
