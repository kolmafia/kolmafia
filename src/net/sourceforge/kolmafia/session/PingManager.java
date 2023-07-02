package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.PingRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PingManager {

  public PingManager() {}

  public static class PingTest {
    private final List<Long> pings = new ArrayList<>();
    private long count = 0L;
    private long total = 0L;
    private long low = 0L;
    private long high = 0L;

    public PingTest() {}

    public PingTest(long count, long total, long low, long high) {
      this.count = count;
      this.total = total;
      this.low = low;
      this.high = high;
    }

    public void addPing(long elapsed) {
      this.pings.add(elapsed);
      this.count++;
      this.total += elapsed;
      if (this.low == 0 || elapsed < this.low) {
        this.low = elapsed;
      }
      if (elapsed > this.high) {
        this.high = elapsed;
      }
    }

    public List<Long> getPings() {
      return this.pings;
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

    public long getAverage() {
      return this.count == 0 ? 0 : this.total / this.count;
    }

    public String toString() {
      StringBuilder buf = new StringBuilder();
      buf.append(String.valueOf(this.count));
      buf.append(":");
      buf.append(String.valueOf(this.low));
      buf.append(":");
      buf.append(String.valueOf(this.high));
      buf.append(":");
      buf.append(String.valueOf(this.total));
      buf.append(":");
      buf.append(String.valueOf(this.getAverage()));
      return buf.toString();
    }

    public void save() {
      String value = this.toString();
      long average = this.getAverage();
      PingTest longest = PingTest.parseProperty("pingLongest");
      long longestAverage = longest.getAverage();
      PingTest shortest = PingTest.parseProperty("pingShortest");
      long shortestAverage = shortest.getAverage();
      if (shortestAverage == 0 || average < shortestAverage) {
        Preferences.setString("pingShortest", value);
      }
      if (longestAverage == 0 || average > longestAverage) {
        Preferences.setString("pingLongest", value);
      }
      Preferences.setString("pingLatest", value);
    }

    public static PingTest parseProperty(String property) {
      String value = Preferences.getString(property);
      String[] values = value.split(":");
      long count = 0L;
      long low = 0L;
      long high = 0L;
      long total = 0L;
      if (values.length >= 4) {
        count = StringUtilities.parseLong(values[0]);
        low = StringUtilities.parseLong(values[1]);
        high = StringUtilities.parseLong(values[2]);
        total = StringUtilities.parseLong(values[3]);
      }
      return new PingTest(count, total, low, high);
    }
  }

  public static PingTest runPingTest(int count, boolean verbose) {
    PingTest result = new PingTest();

    PingRequest ping = new PingRequest();
    for (int i = 1; i <= count; i++) {
      if (verbose) {
        RequestLogger.printLine("Ping #" + i + " of " + count + "...");
      }
      ping.run();
      long elapsed = ping.getElapsedTime();
      result.addPing(elapsed);
      if (verbose) {
        RequestLogger.printLine("-> " + elapsed + " msec");
      }
    }

    return result;
  }
}
