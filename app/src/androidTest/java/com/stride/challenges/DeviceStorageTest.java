package com.stride.challenges;

import android.content.*;
import android.net.Uri;
import android.test.InstrumentationTestCase;
import org.json.*;
import java.io.*;
import java.time.LocalDate;

public class DeviceStorageTest extends InstrumentationTestCase {
 private Context isolated;
 @Override protected void setUp() throws Exception {
  super.setUp();Context target=getInstrumentation().getTargetContext();
  isolated=new ContextWrapper(target){
   @Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("qa-"+name,mode);}
   @Override public File getFilesDir(){File dir=new File(super.getFilesDir(),"qa-backup-tests");dir.mkdirs();return dir;}
  };
  for(String name:new String[]{"zelo","zelo-backup","zelo-widget-undo"})isolated.getSharedPreferences(name,0).edit().clear().commit();
 }
 private JSONObject data() throws Exception {
  String date=LocalDate.now().toString();JSONObject challenge=new JSONObject().put("id","qa").put("name","QA fixture").put("mode","total").put("unit","reps").put("goal",100).put("start",date).put("end",date).put("quickAdd",25).put("logs",new JSONArray());return new JSONObject().put("version",2).put("challenges",new JSONArray().put(challenge));
 }
 public void testBackupWritesVersionsAndReportsSuccess() throws Exception {
  JSONObject data=data();assertEquals(1,ProgressStore.save(isolated,data.toString(),0));
  isolated.getSharedPreferences("zelo-backup",0).edit().putBoolean("enabled",true).putString("uri","content://com.stride.challenges.test.backup/fixture.json").putString("historyFile","fixture-history.json").putInt("savedRevision",-1).commit();
  new File(isolated.getFilesDir(),"fixture-history.json").delete();
  assertFalse(BackupService.writeBackup(isolated));
  JSONObject status=new JSONObject(BackupService.status(isolated));assertFalse(status.getString("lastSaved").isEmpty());assertFalse(status.getBoolean("hasChanges"));
  data.getJSONArray("challenges").getJSONObject(0).put("name","QA second version");assertEquals(2,ProgressStore.save(isolated,data.toString(),1));assertFalse(BackupService.writeBackup(isolated));
  try(InputStream in=isolated.getContentResolver().openInputStream(Uri.parse("content://com.stride.challenges.test.backup/fixture.json"));ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1)out.write(b,0,n);JSONArray snapshots=new JSONObject(out.toString("UTF-8")).getJSONArray("snapshots");assertEquals(2,snapshots.length());assertEquals("QA fixture",snapshots.getJSONObject(0).getJSONObject("data").getJSONArray("challenges").getJSONObject(0).getString("name"));}
 }
 public void testRevokedAccessLeavesSourceIntact() throws Exception {
  String original=data().toString();assertEquals(1,ProgressStore.save(isolated,original,0));
  isolated.getSharedPreferences("zelo-backup",0).edit().putBoolean("enabled",true).putString("uri","content://com.stride.challenges.test.backup/denied").putString("historyFile","denied-history.json").commit();
  assertFalse(BackupService.writeBackup(isolated));assertTrue(new JSONObject(BackupService.status(isolated)).getBoolean("reconnect"));assertEquals("QA fixture",new JSONObject(ProgressStore.read(isolated)).getJSONArray("challenges").getJSONObject(0).getString("name"));
 }
 public void testWidgetUndoAndRevisionProtection() throws Exception {
  assertEquals(1,ProgressStore.save(isolated,data().toString(),0));assertTrue(ProgressStore.quickAdd(isolated,"qa"));assertNotNull(ProgressStore.undoEntry(isolated,"qa"));
  assertEquals(-1,ProgressStore.save(isolated,data().toString(),1));assertTrue(ProgressStore.undoWidget(isolated,"qa"));assertEquals(0,new JSONObject(ProgressStore.read(isolated)).getJSONArray("challenges").getJSONObject(0).getJSONArray("logs").length());assertFalse(ProgressStore.undoWidget(isolated,"qa"));
 }
 @Override protected void tearDown() throws Exception {for(String name:new String[]{"zelo","zelo-backup","zelo-widget-undo"})isolated.getSharedPreferences(name,0).edit().clear().commit();super.tearDown();}
}
