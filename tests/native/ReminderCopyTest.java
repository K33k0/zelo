import com.stride.challenges.*;
import org.json.*;
import java.time.LocalDate;
import java.util.*;

public class ReminderCopyTest {
 public static void main(String[] args) throws Exception {
  JSONObject item=new JSONObject().put("id","copy").put("name","Read ten pages").put("unit","pages").put("reminder",new JSONObject().put("enabled",true));
  GoalEngine.State s=new GoalEngine.State();s.due=5;s.scheduled=true;
  Set<String> messages=new HashSet<>();LocalDate first=LocalDate.of(2026,11,1);
  for(int i=0;i<ReminderMessages.count();i++){String text=ReminderMessages.forChallenge(item,s,first.plusDays(i));require(text.contains("Read ten pages")&&text.contains("5 pages"),"names and remaining work are interpolated");require(!text.contains("{name}")&&!text.contains("{remaining}"),"no unexpanded templates");messages.add(text);}
  require(messages.size()>=20,"at least twenty different original reminders");
  require(ReminderMessages.forChallenge(item,s,first).equals(ReminderMessages.forChallenge(item,s,first)),"edits keep the same day’s phrase");
  item.getJSONObject("reminder").put("enabled",false);require(ReminderMessages.forChallenge(item,s,first)==null,"no nag when reminders are off");
  item.getJSONObject("reminder").put("enabled",true);s.due=0;require(ReminderMessages.forChallenge(item,s,first)==null,"no zero-work nags");s.due=5;s.complete=true;require(ReminderMessages.forChallenge(item,s,first)==null,"no completed-goal nags");s.complete=false;s.scheduled=false;require(ReminderMessages.forChallenge(item,s,first)==null,"no unscheduled-day nags");
  s.scheduled=true;item.put("name","A challenge called {remaining}");require(ReminderMessages.forChallenge(item,s,first).contains("A challenge called {remaining}"),"challenge names are inserted literally");
  require(GoalEngine.number(-0.0).equals("0"),"native zero stays unsigned");
  System.out.println(messages.size()+" reminder phrases verified, including opt-in and outstanding-work guards.");
 }
 static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
