package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.PingRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PingManager {

  public static final String DEFAULT_PAGE = "api.php";
  public static final int MINIMUM_HISTORY_PINGS = 10;

  public PingManager() {}

  public static class PingTest {
    private final List<Long> pings = new ArrayList<>();
    private final String page;
    private long count = 0L;
    private long total = 0L;
    private long low = 0L;
    private long high = 0L;
    private long bytes = 0L;

    public PingTest() {
      this("api.php");
    }

    public PingTest(String page) {
      this.page = page;
    }

    private PingTest(String page, long count, long total, long low, long high, long bytes) {
      this(page);
      this.count = count;
      this.total = total;
      this.low = low;
      this.high = high;
      this.bytes = bytes;
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

    public long getAverage() {
      return this.count == 0 ? 0 : this.total / this.count;
    }

    public long getBPS() {
      return this.total == 0 ? 0 : (this.bytes * 1000) / this.total;
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
      buf.append(":");
      buf.append(String.valueOf(this.getAverage()));
      return buf.toString();
    }

    public void save() {
      String value = this.toString();

      // Always save the last ping results
      Preferences.setString("pingLatest", value);

      // Only save in historical properties if we tested the default
      // page and there are enough pings.
      if (!this.page.equals(DEFAULT_PAGE) || this.count < MINIMUM_HISTORY_PINGS) {
        return;
      }

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
    }

    public static PingTest parseProperty(String property) {
      String value = Preferences.getString(property);
      String[] values = value.split(":");
      String page = "api.php";
      long count = 0L;
      long low = 0L;
      long high = 0L;
      long total = 0L;
      long bytes = 0L;
      if (values.length >= 4) {
        page = values[0];
        count = StringUtilities.parseLong(values[1]);
        low = StringUtilities.parseLong(values[2]);
        high = StringUtilities.parseLong(values[3]);
        total = StringUtilities.parseLong(values[4]);
        bytes = StringUtilities.parseLong(values[5]);
      }
      return new PingTest(page, count, total, low, high, bytes);
    }
  }

  public static PingTest runPingTest(int count, String page, boolean verbose) {
    PingTest result = new PingTest(page);

    PingRequest ping = new PingRequest(page);
    for (int i = 1; i <= count; i++) {
      if (verbose) {
        RequestLogger.printLine("Ping #" + i + " of " + count + "...");
      }
      ping.run();
      long elapsed = ping.getElapsedTime();
      long bytes = ping.responseText == null ? 0 : ping.responseText.length();
      result.addPing(elapsed, bytes);
      if (verbose) {
        RequestLogger.printLine("-> " + elapsed + " msec (" + bytes + " bytes)");
      }
    }

    // Save in appropriate properties
    result.save();

    return result;
  }
}
