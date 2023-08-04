package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.PingRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PingManager {

  public PingManager() {}

  public static class PingTest {
    private final List<Long> pings = new ArrayList<>();
    private final String page;
    private long count = 0L;
    private long total = 0L;
    private long low = 0L;
    private long high = 0L;
    private long bytes = 0L;
    private PingAbortTrigger trigger = null;

    public static String normalizePage(String page) {
      // Backwards compatibility; we no longer save ".php",
      // but saved properties may include it.
      int php = page.indexOf(".php");
      if (php != -1) {
        page = page.substring(0, php);
      }
      return page;
    }

    public PingTest() {
      this("api");
    }

    public PingTest(String page) {
      this.page = normalizePage(page);
    }

    private PingTest(String page, long count, long total, long low, long high, long bytes) {
      this(page);
      this.count = count;
      this.total = total;
      this.low = low;
      this.high = high;
      this.bytes = bytes;
    }

    public void addPing(PingRequest ping) {
      long elapsed = ping.getElapsedTime();
      long bytes = ping.responseText.length();
      this.addPing(elapsed, bytes);
    }

    public void addPing(long elapsed, long bytes) {
      this.pings.add(elapsed);
      this.count++;
      this.total += elapsed;
      if (this.low == 0 || elapsed < this.low) {
        this.low = elapsed;
      }
      if (elapsed > this.high) {
        this.high = elapsed;
      }
      this.bytes += bytes;
    }

    public List<Long> getPings() {
      return this.pings;
    }

    public String getPage() {
      return this.page;
    }

    public long getCount() {
      return this.count;
    }

    public long getLow() {
      return this.low;
    }

    public long getHigh() {
      return this.high;
    }

    public long getTotal() {
      return this.total;
    }

    public long getBytes() {
      return this.bytes;
    }

    public PingAbortTrigger getTrigger() {
      return this.trigger;
    }

    public void setTrigger(PingAbortTrigger trigger) {
      this.trigger = trigger;
    }

    public double getAverage() {
      return this.count == 0 ? 0 : (this.total * 1.0 / this.count);
    }

    public double getBPS() {
      return this.total == 0 ? 0 : (this.bytes * 1000.0) / this.total;
    }

    public String toString() {
      StringBuilder buf = new StringBuilder();
      buf.append(this.page);
      buf.append(":");
      buf.append(String.valueOf(this.count));
      buf.append(":");
      buf.append(String.valueOf(this.low));
      buf.append(":");
      buf.append(String.valueOf(this.high));
      buf.append(":");
      buf.append(String.valueOf(this.total));
      buf.append(":");
      buf.append(String.valueOf(this.getBytes()));
      // Redundant, in that the user can calculate it from total & count
      buf.append(":");
      buf.append(String.valueOf(this.getAverage()));
      return buf.toString();
    }

    public boolean isSaveable() {
      String defaultPage = normalizePage(Preferences.getString("pingDefaultTestPage"));

      return this.getPage().equals(defaultPage);
    }

    public void save() {
      String value = this.toString();

      // Always save the last ping results
      Preferences.setString("pingLatest", value);

      // Only save in historical properties if we tested the default page
      if (!this.isSaveable()) {
        return;
      }

      double average = this.getAverage();
      PingTest shortest = PingTest.parseProperty("pingShortest");
      double shortestAverage = shortest.getAverage();
      PingTest longest = PingTest.parseProperty("pingLongest");
      double longestAverage = longest.getAverage();

      // If the historical data are for a different page than we now
      // require, reset them and start fresh with this test.
      if (!this.getPage().equals(shortest.getPage())) {
        shortestAverage = 0;
        longestAverage = 0;
      }

      if (shortestAverage == 0 || average < shortestAverage) {
        Preferences.setString("pingShortest", value);
      }
      if (longestAverage == 0 || average > longestAverage) {
        Preferences.setString("pingLongest", value);
      }
    }

    public static PingTest parseProperty(String property) {
      String value = Preferences.getString(property);
      String[] values = value.split(":");
      String page = "api";
      long count = 0L;
      long low = 0L;
      long high = 0L;
      long total = 0L;
      long bytes = 0L;
      if (values.length >= 4) {
        page = normalizePage(values[0]);
        count = StringUtilities.parseLong(values[1]);
        low = StringUtilities.parseLong(values[2]);
        high = StringUtilities.parseLong(values[3]);
        total = StringUtilities.parseLong(values[4]);
        bytes = StringUtilities.parseLong(values[5]);
      }
      return new PingTest(page, count, total, low, high, bytes);
    }
  }

  public static class PingAbortTrigger implements Comparable<PingAbortTrigger> {
    private int count;
    private int factor;

    public PingAbortTrigger(int count, int factor) {
      this.count = count;
      this.factor = factor;
    }

    public int getCount() {
      return this.count;
    }

    public void setCount(int count) {
      this.count = count;
    }

    public int getFactor() {
      return this.factor;
    }

    public void setFactor(int factor) {
      this.factor = factor;
    }

    public int compareTo(final PingAbortTrigger o) {
      if (o == null) {
        throw new ClassCastException();
      }
      return this.factor < o.factor
          ? -1
          : this.factor > o.factor ? 1 : this.count < o.count ? -1 : this.count > o.count ? 1 : 0;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof PingAbortTrigger o) {
        return this.count == o.count && this.factor == o.factor;
      }
      return false;
    }

    @Override
    public int hashCode() {
      return this.count * 1000 + this.factor;
    }

    public static Set<PingAbortTrigger> load() {
      Set<PingAbortTrigger> triggers = new TreeSet<>();
      for (String value : Preferences.getString("pingLoginAbort").split("\\s*\\|\\s*")) {
        int index = value.indexOf(":");
        if (index != -1) {
          int count = StringUtilities.parseInt(value.substring(0, index));
          int factor = StringUtilities.parseInt(value.substring(index + 1));
          if (count > 0 && factor > 0) {
            triggers.add(new PingAbortTrigger(count, factor));
          }
        }
      }

      return triggers;
    }

    public static void save(Set<PingAbortTrigger> triggers) {
      StringBuilder buffer = new StringBuilder();
      for (PingAbortTrigger trigger : triggers) {
        if (trigger.count < 1 || trigger.factor < 1) {
          continue;
        }
        if (buffer.length() > 0) {
          buffer.append("|");
        }
        buffer.append(String.valueOf(trigger.count));
        buffer.append(":");
        buffer.append(String.valueOf(trigger.factor));
      }
      Preferences.setString("pingLoginAbort", buffer.toString());
    }
  }

  private static Map<PingAbortTrigger, Integer> getAllAbortTriggers() {
    Map<PingAbortTrigger, Integer> retval = new HashMap<>();
    for (PingAbortTrigger trigger : PingAbortTrigger.load()) {
      retval.put(trigger, 0);
    }
    return retval;
  }

  private static boolean runPing(PingRequest ping, boolean verbose) {
    // Run a single ping
    ping.run();

    String redirectLocation = ping.redirectLocation;
    if (redirectLocation != null) {
      RequestLogger.printLine("Ping redirected to '" + redirectLocation + "'; ping test aborted");
      return false;
    }
    if (ping.responseText == null) {
      RequestLogger.printLine("Ping returned no response; ping test aborted");
      return false;
    }
    return true;
  }

  private static boolean shouldAbortPingTest(
      PingRequest ping, double average, Map<PingAbortTrigger, Integer> triggers, PingTest result) {
    // If the user has not set any abort triggers, nothing to do.
    if (triggers.size() == 0) {
      return false;
    }
    long elapsed = ping.getElapsedTime();
    for (var entry : triggers.entrySet()) {
      PingAbortTrigger trigger = entry.getKey();
      int count = trigger.getCount();
      int factor = trigger.getFactor();
      if (elapsed >= average * factor) {
        // This ping applies. Increment count.
        int seen = entry.getValue() + 1;
        if (seen >= count) {
          // This trigger fires
          result.setTrigger(trigger);
          return true;
        }
        // Increment seen count for trigger
        triggers.put(trigger, seen);
      }
    }
    return false;
  }

  public static PingTest runPingTest() {
    // Run a ping test that qualifies to be saved in ping history.
    String defaultPage = PingTest.normalizePage(Preferences.getString("pingDefaultTestPage"));
    int defaultPings = Preferences.getInteger("pingDefaultTestPings");
    return runPingTest(defaultPings, defaultPage, false);
  }

  public static PingTest runPingTest(int count, String page, boolean verbose) {
    PingTest result = new PingTest(page);

    PingRequest ping = new PingRequest(page);

    Map<PingAbortTrigger, Integer> triggers = getAllAbortTriggers();
    PingTest shortest = PingTest.parseProperty("pingShortest");
    double average = shortest.getAverage();

    // The first ping can be anomalous. Perhaps we were logged out and
    // KoLmafia needs to time us in - which will now run a ping test.
    //
    // Run a single ping first and don't count it.
    if (!runPing(ping, verbose)) {
      return result;
    }

    // But do check if it should trigger an abort
    if (shouldAbortPingTest(ping, average, triggers, result)) {
      result.addPing(ping);
      return result;
    }

    for (int i = 1; i <= count; i++) {
      if (verbose) {
        RequestLogger.printLine("Ping #" + i + " of " + count + "...");
      }

      if (!runPing(ping, verbose)) {
        return result;
      }

      long elapsed = ping.getElapsedTime();
      long bytes = ping.responseText.length();
      result.addPing(elapsed, bytes);

      if (verbose) {
        RequestLogger.printLine("-> " + elapsed + " msec (" + bytes + " bytes)");
      }

      // If this ping should trigger an abort, stop the test
      if (shouldAbortPingTest(ping, average, triggers, result)) {
        break;
      }
    }

    // Save in appropriate properties
    result.save();

    return result;
  }
}
