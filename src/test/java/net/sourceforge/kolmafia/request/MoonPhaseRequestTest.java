package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withHttpClientBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Month;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.persistence.HolidayDatabase;
import org.junit.jupiter.api.Test;

class MoonPhaseRequestTest {
  @Test
  void canParseMoonPhase() {
    var ostream = new ByteArrayOutputStream();
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups = new Cleanups(withDay(2023, Month.AUGUST, 5), withHttpClientBuilder(builder));

    try (cleanups) {
      try (var out = new PrintStream(ostream, true)) {
        RequestLogger.openCustom(out);
        var html = html("request/test_awesomemenu_1.html");
        client.addResponse(200, html);
        new MoonPhaseRequest().run();
        RequestLogger.closeCustom();
      }

      assertThat(HolidayDatabase.getRonaldPhase(), is(3));
      assertThat(HolidayDatabase.getGrimacePhase(), is(2));
      assertThat(ostream.toString(), not(containsString("phase error")));
    }
  }
}
