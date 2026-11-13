import com.stride.challenges.GoalEngine;
import java.nio.file.*;
import java.time.LocalDate;
import org.json.*;

public class EngineParity {

  public static void main(String[] args) throws Exception {
    JSONArray cases = new JSONArray(Files.readString(Path.of(args[0])));
    for (int i = 0; i < cases.length(); i++) {
      JSONObject item = cases.getJSONObject(i),
        e = item.getJSONObject("expected");
      GoalEngine.State s = GoalEngine.calculate(
        item.getJSONObject("c"),
        LocalDate.parse(item.getString("date"))
      );
      check(i, "total", s.total, e.getDouble("total"));
      check(i, "today", s.today, e.getDouble("today"));
      check(i, "due", s.due, e.getDouble("due"));
      check(i, "target", s.target, e.getDouble("target"));
      check(i, "progress", s.progress, e.getDouble("progress"));
      if (
        s.complete != e.getBoolean("complete") || s.scheduled != e.getBoolean("scheduled")
      ) throw new AssertionError("Boolean mismatch case " + i);
    }
    System.out.println(cases.length() + " Android/JavaScript goal-rule parity checks passed.");
  }

  static void check(int i, String field, double a, double b) {
    if (Math.abs(a - b) > 1e-7) throw new AssertionError(
      "Case " + i + " " + field + ": " + a + " != " + b
    );
  }
}
