import com.stride.challenges.*;
import org.json.*;
import java.time.*;

public class ReminderBackupTest {
 public static void main(String[] args) throws Exception {
  JSONObject c=new JSONObject("{\"id\":\"a\",\"name\":\"Pushups\",\"mode\":\"total\",\"unit\":\"reps\",\"goal\":3000,\"start\":\"2025-11-01\",\"end\":\"2025-11-30\",\"logs\":[],\"reminder\":{\"enabled\":true,\"time\":\"18:00\"}}");
  ZonedDateTime now=ZonedDateTime.parse("2025-11-01T18:05:00Z");long previous=ZonedDateTime.parse("2025-11-01T18:00:00Z").toInstant().toEpochMilli();
  long next=ReminderPolicy.next(c,now,"",previous,"18:00");require(next==now.plusSeconds(1).toInstant().toEpochMilli(),"delayed pending reminder must survive edits");
  c.getJSONArray("logs").put(new JSONObject("{\"id\":\"l\",\"date\":\"2025-11-01\",\"amount\":25,\"note\":\"\"}"));
  require(ReminderPolicy.next(c,now,"",previous,"18:00")==next,"partial log must not cancel pending reminder");
  long tomorrow=ZonedDateTime.parse("2025-11-02T18:00:00Z").toInstant().toEpochMilli();
  require(ReminderPolicy.next(c,now,"2025-11-01",previous,"18:00")==tomorrow,"one evaluation per day");
  require(ReminderPolicy.next(c,now,"",0,"")==tomorrow,"new reminders after requested time begin tomorrow");
  c.getJSONArray("logs").getJSONObject(0).put("amount",100);
  require(ReminderPolicy.next(c,now,"",previous,"18:00")==tomorrow,"met daily target stays quiet");
  c.getJSONArray("logs").getJSONObject(0).put("amount",3000);
  require(ReminderPolicy.next(c,now,"",previous,"18:00")==0,"completed challenges have no alarms");
  String archive=null;for(int revision=1;revision<=12;revision++)archive=BackupArchive.append(archive,new JSONObject().put("version",2).put("revision",revision).put("challenges",new JSONArray().put(c)),"2025-11-01T18:"+String.format("%02d",revision)+":00Z");
  JSONArray versions=new JSONObject(archive).getJSONArray("snapshots");require(versions.length()==10,"retain ten recent versions");require(versions.getJSONObject(0).getInt("revision")==3,"oldest versions trimmed first");require(versions.getJSONObject(9).getJSONObject("data").getJSONArray("challenges").length()==1,"each snapshot is independently restorable");
  String duplicate=BackupArchive.append(archive,new JSONObject().put("revision",12).put("challenges",new JSONArray()),"2025-11-01T19:00:00Z");require(new JSONObject(duplicate).getJSONArray("snapshots").length()==10,"same revision does not duplicate history");
  boolean tooLarge=false;try{BackupArchive.append(null,new JSONObject().put("revision",1).put("challenges",new JSONArray()).put("oversize","x".repeat(5_000_001)),"2025-11-01T19:00:00Z");}catch(Exception e){tooLarge=true;}require(tooLarge,"oversized backups rejected before writing");
  System.out.println("Reminder delivery and versioned backup regression tests passed.");
 }
 static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
