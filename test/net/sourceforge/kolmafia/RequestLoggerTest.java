package net.sourceforge.kolmafia;

import static internal.helpers.Player.withItemMonster;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withMultiFight;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest.ShadowRift;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class RequestLoggerTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("RequestLogger");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("RequestLogger");
  }

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
    public void canReportPreviousLocationInMultiFightInLocation() {
      var cleanups =
          new Cleanups(withLastLocation("The Outskirts of Cobb's Knob"), withMultiFight());

      try (cleanups) {
        var output = withCapturedLogs(RequestLogger::registerLastLocation);

        assertThat(output, equalTo("[1] The Outskirts of Cobb's Knob"));
      }
    }

    @Test
    public void canReportPreviousLocationInMultiFightWithItemMonster() {
      var cleanups =
          new Cleanups(
              withLastLocation((String) null),
              withMultiFight(),
              withItemMonster("Combat Lover's Locket"));

      try (cleanups) {
        var output = withCapturedLogs(RequestLogger::registerLastLocation);

        assertThat(output, equalTo("[1] Combat Lover's Locket"));
      }
    }

    @Test
    public void canReportUnknownLocationInMultiFight() {
      var cleanups =
          new Cleanups(withLastLocation((String) null), withMultiFight(), withItemMonster(null));

      try (cleanups) {
        var output = withCapturedLogs(RequestLogger::registerLastLocation);

        assertThat(output, equalTo("[1] Unknown Location"));
      }
    }
  }

  @Test
  public void angleBracketDoesNotGenerateHTMLtoLog() {
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(ostream, true)) {
      RequestLogger.openCustom(out);
      RequestLogger.printLine("> Wassup?");
      RequestLogger.closeCustom();
    }
    String logged = ostream.toString().trim();
    assertEquals("> Wassup?", logged);
  }

  @Test
  public void mafiaStateDoesNotGenerateHTMLThatIsLogged() {
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (PrintStream out = new PrintStream(ostream, true)) {
      RequestLogger.openCustom(out);
      RequestLogger.printLine(KoLConstants.MafiaState.ERROR, "Am I Blue?", true);
      RequestLogger.closeCustom();
    }
    String logged = ostream.toString().trim();
    assertEquals("Am I Blue?", logged);
  }

  @Nested
  class ShadowRifts {
    @ParameterizedTest
    @EnumSource(ShadowRift.class)
    public void canRegisterShadowRiftIngress(ShadowRift rift) {
      var cleanups = withProperty("shadowRiftIngress", "");
      try (cleanups) {
        String url = rift.getURL();

        // Each Shadow Rift is in a "container" accessed via place.php
        assertTrue(url.startsWith("place.php"));

        // AdventureDatabase maps each such URL to a KoLAdventure
        var adventure = AdventureDatabase.getAdventureByURL(url);
        assertTrue(adventure != null);

        // Simply doing that lookup does not register the current Shadow Rift
        assertEquals(Preferences.getString("shadowRiftIngress"), "");

        // Registering that URL will be claimed by KoLAdventure.
        var request = new GenericRequest(url);
        var output = withCapturedLogs(() -> RequestLogger.registerRequest(request, url));
        var expected = "Entering the Shadow Rift via " + rift.getContainer();
        assertEquals(expected, output);

        // This will remember the last Shadow Rift Ingress
        assertEquals(Preferences.getString("shadowRiftIngress"), rift.getPlace());
      }
    }
  }
}
