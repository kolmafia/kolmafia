package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withCounter;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withLimitMode;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static org.junit.jupiter.api.Assertions.*;
import static org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampAwayRequest;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest;
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest.EveryCard;
import net.sourceforge.kolmafia.request.FalloutShelterRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.LocketRequest;
import net.sourceforge.kolmafia.request.PlaceRequest;
import net.sourceforge.kolmafia.request.PyramidRequest;
import net.sourceforge.kolmafia.request.RelayRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
  class AdventuresUsed {
    @Nested
    class Adventure {
      public void testKoLAdventure(KoLAdventure adventure, int turns) {
        // Automation
        var request = adventure.getRequest();
        assertEquals(turns, TurnCounter.getTurnsUsed(request));

        // Relay Browser
        var relay = new RelayRequest(false);
        String url = request.getURLString();
        relay.constructURLString(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(relay));

        // visit_url
        var generic = new GenericRequest(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(generic));
      }

      private static final KoLAdventure THE_SHORE =
          AdventureDatabase.getAdventureByName("The Shore, Inc. Travel Agency");

      @Test
      public void thatNormalShoreTakesThreeTurns() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testKoLAdventure(THE_SHORE, 3);
        }
      }

      @Test
      public void thatFistcoreShoreTakesFiveTurns() {
        var cleanups = new Cleanups(withPath(Path.SURPRISING_FIST));
        try (cleanups) {
          testKoLAdventure(THE_SHORE, 5);
        }
      }

      @ParameterizedTest
      @ValueSource(
          strings = {
            "The Briny Deeps",
            "Gingerbread Reef",
            "The Sunken Party Yacht",
            "The Ice Hole"
          })
      public void thatUnderWaterNonFishyTakesTwoTurns(String adventureName) {
        var cleanups = new Cleanups();
        try (cleanups) {
          KoLAdventure adventure = AdventureDatabase.getAdventure(adventureName);
          testKoLAdventure(adventure, 2);
        }
      }

      @ParameterizedTest
      @ValueSource(
          strings = {
            "The Briny Deeps",
            "Gingerbread Reef",
            "The Sunken Party Yacht",
            "The Ice Hole"
          })
      public void thatUnderWaterFishyTakesOneTurn(String adventureName) {
        var cleanups = new Cleanups(withEffect("Fishy"));
        try (cleanups) {
          KoLAdventure adventure = AdventureDatabase.getAdventure(adventureName);
          testKoLAdventure(adventure, 1);
        }
      }

      @Test
      public void thatAnytNormalAdventureTakesOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          KoLAdventure adventure = AdventureDatabase.getAdventureByName("The Haunted Pantry");
          testKoLAdventure(adventure, 1);
        }
      }

      @Test
      public void thatTheSummoningChamberTakesOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          KoLAdventure adventure = AdventureDatabase.getAdventureByName("Summoning Chamber");
          testKoLAdventure(adventure, 1);
        }
      }

      @Test
      public void thatSpelunkyUsesNoTurns() {
        var cleanups = new Cleanups(withLimitMode(LimitMode.SPELUNKY));
        try (cleanups) {
          KoLAdventure adventure = AdventureDatabase.getAdventureByName("The City of Goooold");
          testKoLAdventure(adventure, 0);
        }
      }

      @Test
      public void thatBatfellowUsesNoTurns() {
        var cleanups = new Cleanups(withLimitMode(LimitMode.BATMAN));
        try (cleanups) {
          KoLAdventure adventure = AdventureDatabase.getAdventureByName("Porkham Asylum");
          testKoLAdventure(adventure, 0);
        }
      }

      @ParameterizedTest
      @ValueSource(
          strings = {
            "Fastest Adventurer Contest",
            "Strongest Adventurer Contest",
            "Hottest Adventurer Contest",
            // The Hedge Maze
            "Tower Level 1",
            "Tower Level 2",
            "Tower Level 3",
            "Tower Level 4",
            "Tower Level 5",
            "The Naughty Sorceress' Chamber",
          })
      public void thatSorceressMonstersTakeOneTurn(String adventureName) {
        var cleanups = new Cleanups();
        try (cleanups) {
          // These are actually place.php?whichplace=nstower
          KoLAdventure adventure = AdventureDatabase.getAdventure(adventureName);
          testKoLAdventure(adventure, 1);
        }
      }

      // The following are adventures that do not use adventure.php
      //
      // barrel.php
      // - The Barrel Full of Barrels
      // basement.php
      // - Fernswarthy's Basement
      // cellar.php
      // - The Typical Tavern Cellar
      // mining.php
      // - Itznotyerzitz Mine (in Disguise)
      // - The Knob Shaft (Mining)
      // - Anemone Mine (Mining)
      // - The Crimbonium Mine
      // - The Velvet / Gold Mine (Mining)
      // volcanoisland.php&action=tniat
      // - First entry into The Nemesis' Lair
      // volcanoisland.php&action=tuba
      // - redirect to The Island Barracks (only after Nemesis defeated?)
    }

    @Nested
    class Places {

      // The following are place.php requests but are automated as adventures:
      //
      //    The Summoning Chamber
      //    The Lower Chambers
      //    Naughty Sorceress Lair monsters

      public void testPlace(String place, String action, int turns) {

        // Automation
        var request = new PlaceRequest(place, action);
        assertEquals(turns, TurnCounter.getTurnsUsed(request));

        // Relay Browser
        var relay = new RelayRequest(false);
        String url = "place.php?whichplace=" + place + "&action=" + action;
        relay.constructURLString(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(relay));
        // visit_url
        var generic = new GenericRequest(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(generic));
      }

      @Test
      public void thatTheNemesisCaveTakesOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testPlace("nemesiscave", "nmcave_boss", 1);
        }
      }

      @Test
      public void thatFightingChateauMonsterTakesOneTurn() {
        var cleanups = new Cleanups(withProperty("_chateauMonsterFought", false));
        try (cleanups) {
          testPlace("chateau", "chateau_painting", 1);
        }
      }

      @Test
      public void thatNotFightingChateauMonsterTakesNoTurns() {
        var cleanups = new Cleanups(withProperty("_chateauMonsterFought", true));
        try (cleanups) {
          testPlace("chateau", "chateau_painting", 0);
        }
      }
    }

    @Nested
    class Resting {
      // Places you can rest:
      //
      // campground.php&action=rest
      //
      // place.php?whichplace=chateau&action=chateau_restbox
      // place.php?whichplace=chateau&action=chateau_restlabelfree
      // place.php?whichplace=chateau&action=cheateau_restlabel
      //
      // place.php?whichplace=campaway&action=campaway_tentclick
      // place.php?whichplace=campaway&action=campaway_tentturn
      // place.php?whichplace=campaway&action=campaway_tentfree
      //
      // place.php?whichplace=falloutshelter&action=vault1

      class Campground {
        public void testRest(boolean rest, int turns) {
          // Automation
          var action = rest ? "rest" : "garden";
          var request = new CampgroundRequest(action);
          assertEquals(turns, TurnCounter.getTurnsUsed(request));

          // Relay Browser
          var relay = new RelayRequest(false);
          String url = "campground.php?action=" + action;
          relay.constructURLString(url);
          assertEquals(turns, TurnCounter.getTurnsUsed(relay));

          // visit_url
          var generic = new GenericRequest(url);
          assertEquals(turns, TurnCounter.getTurnsUsed(generic));
        }

        @Test
        public void thatNonRestsTakeNoTurns() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 0));
          try (cleanups) {
            testRest(false, 0);
          }
        }

        @Test
        public void thatFreeRestsTakeNoTurns() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 0));
          try (cleanups) {
            testRest(true, 0);
          }
        }

        @Test
        public void thatNonFreeRestsTakeOneTurns() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 1));
          try (cleanups) {
            testRest(true, 1);
          }
        }
      }

      class Chateau {
        public void testRest(boolean rest, int turns) {
          // Automation
          var action = rest ? ChateauRequest.BED : "chateau_desk";
          var request = new ChateauRequest(action);
          assertEquals(turns, TurnCounter.getTurnsUsed(request));

          // Relay Browser
          var relay = new RelayRequest(false);
          String url = "place.php?whichplace=chateau&action=" + action;
          relay.constructURLString(url);
          assertEquals(turns, TurnCounter.getTurnsUsed(relay));

          // visit_url
          var generic = new GenericRequest(url);
          assertEquals(turns, TurnCounter.getTurnsUsed(generic));
        }

        @Test
        public void thatNonRestsTakeNoTurns() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 0));
          try (cleanups) {
            testRest(false, 0);
          }
        }

        @Test
        public void thatFreeRestsTakeNoTurns() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 0));
          try (cleanups) {
            testRest(true, 0);
          }
        }

        @Test
        public void thatNonFreeRestsTakeOneTurns() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 1));
          try (cleanups) {
            testRest(true, 1);
          }
        }
      }

      class CampAway {
        public void testRest(boolean rest, int turns) {
          // Automation
          var action = rest ? CampAwayRequest.TENT : CampAwayRequest.SKY;
          var request = new CampAwayRequest(action);
          assertEquals(turns, TurnCounter.getTurnsUsed(request));

          // Relay Browser
          var relay = new RelayRequest(false);
          String url = "place.php?whichplace=campaway&action=" + action;
          relay.constructURLString(url);
          assertEquals(turns, TurnCounter.getTurnsUsed(relay));

          // visit_url
          var generic = new GenericRequest(url);
          assertEquals(turns, TurnCounter.getTurnsUsed(generic));
        }

        @Test
        public void thatNonRestsTakeNoTurns() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 0));
          try (cleanups) {
            testRest(false, 0);
          }
        }

        @Test
        public void thatFreeRestsTakeNoTurns() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 0));
          try (cleanups) {
            testRest(true, 0);
          }
        }

        @Test
        public void thatNonFreeRestsTakeOneTurns() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 1));
          try (cleanups) {
            testRest(true, 1);
          }
        }
      }

      class FalloutShelter {
        public void testRest(boolean rest, int turns) {
          // Automation
          var action = rest ? FalloutShelterRequest.CRYO_SLEEP_CHAMBER : "vault_term";
          var request = new FalloutShelterRequest(action);
          assertEquals(turns, TurnCounter.getTurnsUsed(request));

          // Relay Browser
          var relay = new RelayRequest(false);
          String url = "place.php?whichplace=campaway&action=" + action;
          relay.constructURLString(url);
          assertEquals(turns, TurnCounter.getTurnsUsed(relay));

          // visit_url
          var generic = new GenericRequest(url);
          assertEquals(turns, TurnCounter.getTurnsUsed(generic));
        }

        @Test
        public void thatNonRestsTakeNoTurns() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 0));
          try (cleanups) {
            testRest(false, 0);
          }
        }

        @Test
        public void thatFreeRestsNotAvailable() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 0));
          try (cleanups) {
            testRest(true, 1);
          }
        }

        @Test
        public void thatNonFreeRestsTakeOneTurns() {
          var cleanups = new Cleanups(withSkill("Disco Nap"), withProperty("timesRested", 1));
          try (cleanups) {
            testRest(true, 1);
          }
        }
      }
    }

    @Nested
    class DeckOfEveryCard {
      public void testCard(String name, int turns) {
        EveryCard card = DeckOfEveryCardRequest.findCard(name);
        assertNotNull(card);

        // Automation
        var request = new DeckOfEveryCardRequest(card);
        assertEquals(turns, TurnCounter.getTurnsUsed(request));

        // Relay Browser
        var relay = new RelayRequest(false);
        String url = "choice.php?whichchoice=1086&option=1&which=" + card.id;
        relay.constructURLString(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(relay));

        // visit_url
        var generic = new GenericRequest(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(generic));
      }

      @Test
      public void thatMonsterCardsTakeOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testCard("Green Card", 1);
        }
      }

      @Test
      public void thaNonMonsterCardsTakeNoTurns() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testCard("Ancestral Recall", 0);
        }
      }
    }

    @Nested
    class CombatLoversLocket {
      public void testCard(String name, int turns) {
        MonsterData monster = MonsterDatabase.findMonster(name);
        assertNotNull(monster);

        // Automation
        var request = new LocketRequest(monster);
        assertEquals(turns, TurnCounter.getTurnsUsed(request));

        // Relay Browser
        var relay = new RelayRequest(false);
        String url = "choice.php?whichchoice=1463&option=1&mid=" + monster.getId();
        relay.constructURLString(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(relay));

        // visit_url
        var generic = new GenericRequest(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(generic));
      }

      @Test
      public void thatReminiscingTakesOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testCard("Black Crayon Penguin", 1);
        }
      }
    }

    @Nested
    class BeachComb {}
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
