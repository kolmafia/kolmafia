package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withCounter;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.*;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

public class TurnCounterTest {

  // KoLmafia creates TurnCounters to track many timed events.
  //
  // Some events happen at a fixed number of turns from now.
  //
  // For example, the Lights Out NC in Spookyraven Manor occurs every 37
  // turns. In particular, whenever (KoLCharacter.getTurnsPlayed() % 37) ==
  // 0. That's total turns, not turns this run.
  //
  // Other counters frame a "window" in which a wandering monster is expected,
  // say.  For such, there will be a "start" counter and an "end" counter.
  //
  // We check counters both during automation and during Relay Browser
  // adventuring.  If you are about to submit a URL which will consume a turn
  // (or more than one), if you are automating, a "counter script", if you have
  // one, will have a chance to look at. If it doesn't handle it (or you have
  // none), a Preference controls whether automation will stop, allowing you
  // the chance to manually handle it, if you wish.
  //
  // In the Relay Browser, when you click on a link that submits a URL that
  // will consume one or more turns, we look for counters that would be
  // ignored, we pop up a counter warning in the browser and give you a chance
  // to confirm that you wish to ignore it - or to go and manually handle it.
  //
  // GenericRequest.stopForCounters() handles automation.  The boolean
  // preference "dontStopForCounters" controls whether expiring counters
  // generate an ERROR.
  //
  // RelayRequest.sendCounterWarning() displays a warning in the Relay Browser
  // before submitting a request. Doing so clears the expiring counters so that
  // GenericRequest will not ALSO kick in.
  //
  // This test suite is intended, especially, to test those two methods.

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("counter user");
    Preferences.saveSettingsToFile = false;
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("counter user");
    RelayRequest.reset();
  }

  @AfterAll
  public static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  @Nested
  class Pyramid {
    private static final KoLAdventure LOWER_CHAMBERS =
        AdventureDatabase.getAdventureByName("The Lower Chambers");

    @CartesianTest
    public void thatCounterWarningsInPyramidWork(
        @Values(ints = {1, 2, 3, 4, 5}) int position,
        @Values(booleans = {true, false}) boolean bombed,
        @Values(ints = {0, 3, 7}) int counter) {

      // Going to the Lower Chamber consumes seven turns when going after
      // Ed and one turn if simply visiting after turning the wheel.
      //
      // I did not get a Spookyraven Lights Out warning when about to do
      // the latter. Let's fix that.

      // "pyramidPosition"
      // 1 = "Empty/Rubble"
      //   = "Empty/Empty/Ed's Chamber"
      // 2 = "Rats/Token"
      // 3 = "Rubble/Bomb"
      // 4 = "Token/Empty"
      // 5 = "Bomb/Rats"

      var cleanups =
          new Cleanups(
              withProperty("pyramidPosition", position),
              withProperty("pyramidBombUsed", bombed),
              withProperty("dontStopForCounters", false),
              withContinuationState(),
              withCounter(counter, "label", "image"));
      try (cleanups) {
        int expected = PyramidRequest.lowerChamberTurnsUsed();
        int actual = LOWER_CHAMBERS.getRequest().getAdventuresUsed();
        assertEquals(expected, actual);

        // There are two ways to get to the lower chamber:
        //
        // From the Control Chamber:
        // choice.php?pwd&whichchoice=929&option=5
        var choiceRequest = new RelayRequest(false);
        choiceRequest.constructURLString("choice.php?whichchoice=929&option=5");
        assertEquals(expected, TurnCounter.getTurnsUsed(choiceRequest));
        boolean warned = choiceRequest.sendCounterWarning();
        assertEquals(expected > counter, warned);

        // (which probably redirects to:)
        //
        // From the Pyramid:
        // place.php?whichplace=pyramid&action=pyramid_state1a
        var placeRequest = new RelayRequest(false);
        var buffer = new StringBuilder("place.php?whichplace=pyramid&action=pyramid_state");
        buffer.append(position);
        if (bombed) {
          buffer.append("a");
        }
        placeRequest.constructURLString(buffer.toString());
        assertEquals(expected, TurnCounter.getTurnsUsed(placeRequest));

        // Restart the counter
        TurnCounter.stopCounting("label");
        TurnCounter.startCounting(counter, "label", "image");

        warned = placeRequest.sendCounterWarning();
        assertEquals(expected > counter, warned);

        // This is the same check for automation - i.e. a counter warning
        var adventureRequest = LOWER_CHAMBERS.getRequest();
        assertEquals(expected, TurnCounter.getTurnsUsed(adventureRequest));

        // Restart the counter
        TurnCounter.stopCounting("label");
        TurnCounter.startCounting(counter, "label", "image");

        warned = adventureRequest.stopForCounters();
        assertEquals(expected > counter, warned);
        if (warned) {
          assertEquals(MafiaState.ERROR, StaticEntity.getContinuationState());
        }
      }
    }
  }

  @Nested
  class Nemesis {
    @Test
    public void thatCounterWarningsInNemesisCaveWorks() {
      var cleanups =
          new Cleanups(
              withProperty("dontStopForCounters", false),
              withContinuationState(),
              withCounter(0, "label", "image"));
      try (cleanups) {
        var placeRequest = new RelayRequest(false);
        String url = "place.php?whichplace=nemesiscave&action=nmcave_boss";
        placeRequest.constructURLString(url);
        assertEquals(1, TurnCounter.getTurnsUsed(placeRequest));
        boolean warned = placeRequest.sendCounterWarning();
        assertTrue(warned);

        // Restart the counter
        TurnCounter.stopCounting("label");
        TurnCounter.startCounting(0, "label", "image");

        // This is the same check for automation - i.e. a counter warning
        var adventureRequest = new GenericRequest(url);
        assertEquals(1, TurnCounter.getTurnsUsed(adventureRequest));

        warned = adventureRequest.stopForCounters();
        if (warned) {
          assertEquals(MafiaState.ERROR, StaticEntity.getContinuationState());
        }
      }
    }
  }
}
