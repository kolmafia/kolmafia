package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.request.PingRequest;

public class PingManager {

  public PingManager() {}

  public static class PingTest {
    private final List<Long> pings = new ArrayList<>();
    private long total;

    public PingTest() {
      this.pings.clear();
    }

    public void addPing(long elapsed) {
      this.pings.add(elapsed);
      this.total += elapsed;
    }

    public List<Long> getPings() {
      return this.pings;
    }

    public long getCount() {
      return this.pings.size();
    }

    public long getTotal() {
      return this.total;
    }

    public long getAverage() {
      int count = pings.size();
      return count == 0 ? 0 : this.total / count;
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
