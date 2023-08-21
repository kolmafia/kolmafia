package net.sourceforge.kolmafia.textui;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withCurrentRun;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BeachManagerTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("BeachManager");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("BeachManager");
  }

  @Test
  void wanderingBeachSetsProperties() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withCurrentRun(100),
            withHandlingChoice(1388),
            withProperty("_beachCombing", false),
            withProperty("_beachMinutes", 0),
            withProperty("_beachTides", -1),
            withProperty("_beachLayout", ""));

    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (cleanups;
        PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.openCustom(out);
      client.addResponse(200, html("request/test_beach_visit_whale.html"));

      String url = "choice.php?whichchoice=1388&pwd&option=1&minutes=6079";
      var request = new GenericRequest(url);
      request.run();

      RequestLogger.closeCustom();

      String output = ostream.toString().trim();
      assertThat(output, startsWith("[101] Wandering 6079 minutes down the beach"));
      assertTrue(ChoiceManager.handlingChoice);
      assertEquals(1388, ChoiceManager.lastChoice);
      assertTrue(Preferences.getBoolean("_beachCombing"));
      assertEquals(6079, Preferences.getInteger("_beachMinutes"));
      assertEquals(2, Preferences.getInteger("_beachTides"));
      assertEquals(
          "3:rrrrrrrrrr,4:rrrrrrrrrr,5:rrrrrrrrrr,6:rrrrrrrrrr,7:rrrrrrrrrr,8:rrrrrrrrrr,9:rrrrrWrrrr,10:rrrrrrrrrr",
          Preferences.getString("_beachLayout"));
    }
  }

  @Test
  void combingWhaleSetsProperties() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withMeat(0),
            withHandlingChoice(1388),
            withProperty("_beachCombing", true),
            withProperty("_beachMinutes", 6079),
            withProperty(
                "_beachLayout",
                "3:rrrrrrrrrr,4:rrrrrrrrrr,5:rrrrrrrrrr,6:rrrrrrrrrr,7:rrrrrrrrrr,8:rrrrrrrrrr,9:rrrrrWrrrr,10:rrrrrrrrrr"));

    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    try (cleanups;
        PrintStream out = new PrintStream(ostream, true)) {
      // Inject custom output stream.
      RequestLogger.openCustom(out);
      client.addResponse(200, html("request/test_beach_comb_whale.html"));

      String url = "choice.php?whichchoice=1388&pwd&option=4&coords=9,60785";
      var request = new GenericRequest(url);
      request.run();

      RequestLogger.closeCustom();

      String output = ostream.toString();
      assertThat(
          output,
          is(
              """
              Combing square 9,6 (6079 minutes down the beach)
              You gain 10,491,186 Meat
              """));
      assertEquals(10_491_186, KoLCharacter.getAvailableMeat());
      assertTrue(ChoiceManager.handlingChoice);
      assertEquals(1388, ChoiceManager.lastChoice);
      assertFalse(Preferences.getBoolean("_beachCombing"));
      assertEquals(6079, Preferences.getInteger("_beachMinutes"));
      // The "W" is now "c"
      assertEquals(
          "3:rrrrrrrrrr,4:rrrrrrrrrr,5:rrrrrrrrrr,6:rrrrrrrrrr,7:rrrrrrrrrr,8:rrrrrrrrrr,9:rrrrrcrrrr,10:rrrrrrrrrr",
          Preferences.getString("_beachLayout"));
    }
  }
}
