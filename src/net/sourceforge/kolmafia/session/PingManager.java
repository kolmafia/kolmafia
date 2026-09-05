package net.sourceforge.kolmafia.session;

import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.PingRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class PingManager {

  private PingManager() {}

  public static class PingTest {
    private final String page;
    private long count = 0L;
    private long total = 0L;
    private long low = 0L;
    private long high = 0L;
    private long bytes = 0L;

    public static String normalizePage(String page) {
      // Backwards compatibility; we no longer save ".php",
      // but saved properties may include it.
      int php = page.indexOf(".php");
      if (php != -1) {
        page = page.substring(0, php);
      }
      return page;
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

    public void addPing(long elapsed, long bytes) {
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
      buf.append(KoLConstants.FLOAT_FORMAT.format(this.getAverage()));
      return buf.toString();
    }

    public void save() {
      Preferences.setString("pingLatest", this.toString());
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

  private static boolean runPing(PingRequest ping) {
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

  public static String defaultTestPage() {
    return PingTest.normalizePage(Preferences.getString("pingDefaultTestPage"));
  }

  public static int defaultTestPings() {
    return Preferences.getInteger("pingDefaultTestPings");
  }

  public static PingTest runPingTest() {
    return runPingTest(defaultTestPings(), defaultTestPage(), false);
  }

  public static PingTest runPingTest(int count, String page, boolean verbose) {
    page = PingTest.normalizePage(page);

    PingTest result = new PingTest(page);

    PingRequest ping = new PingRequest(page);

    // A time-in can land inside the first ping's measured time.
    //
    // Run a single ping first and don't count it.
    if (!runPing(ping)) {
      return result;
    }

    for (int i = 1; i <= count; i++) {
      if (verbose) {
        RequestLogger.printLine("Ping #" + i + " of " + count + "...");
      }

      if (!runPing(ping)) {
        return result;
      }

      long elapsed = ping.getElapsedTime();
      long bytes = ping.responseText.length();
      result.addPing(elapsed, bytes);

      if (verbose) {
        RequestLogger.printLine("-> " + elapsed + " msec (" + bytes + " bytes)");
      }
    }

    result.save();

    return result;
  }
}
