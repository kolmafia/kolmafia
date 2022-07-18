package net.sourceforge.kolmafia;

import static internal.helpers.Player.withItemMonster;
import static internal.helpers.Player.withLastLocationName;
import static internal.helpers.Player.withMultiFight;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RequestLoggerTest {
  private String withCapturedLogs(Runnable action) {
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

    try (PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.openCustom(out);
      action.run();
      RequestLogger.closeCustom();
    }

    return ostream.toString().trim();
  }

  @Nested
  class RegisterLastLocation {
    @Test
    public void canReportPreviousLocationInMutliFightInLocation() {
      var cleanups =
          new Cleanups(withLastLocationName("The Outskirts of Cobb's Knob"), withMultiFight());

      try (cleanups) {
        var output = withCapturedLogs(RequestLogger::registerLastLocation);

        assertThat(output, equalTo("[1] The Outskirts of Cobb's Knob"));
      }
    }

    @Test
    public void canReportPreviousLocationInMutliFightWithItemMonster() {
      var cleanups =
          new Cleanups(
              withLastLocationName(null),
              withMultiFight(),
              withItemMonster("Combat Lover's Locket"));

      try (cleanups) {
        var output = withCapturedLogs(RequestLogger::registerLastLocation);

        assertThat(output, equalTo("[1] Combat Lover's Locket"));
      }
    }

    @Test
    public void canReportUnknownLocationInMutliFight() {
      var cleanups =
          new Cleanups(withLastLocationName(null), withMultiFight(), withItemMonster(null));

      try (cleanups) {
        var output = withCapturedLogs(RequestLogger::registerLastLocation);

        assertThat(output, equalTo("[1] Unknown Location"));
      }
    }
  }
}
