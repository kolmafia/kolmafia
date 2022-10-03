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
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.BeachCombRequest;
import net.sourceforge.kolmafia.request.BeachCombRequest.BeachCombCommand;
import net.sourceforge.kolmafia.request.CakeArenaRequest;
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
import net.sourceforge.kolmafia.request.RichardRequest;
import net.sourceforge.kolmafia.request.SuburbanDisRequest;
import net.sourceforge.kolmafia.request.UseItemRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.request.VolcanoIslandRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

      private static final KoLAdventure BASEMENT =
          AdventureDatabase.getAdventureByName("Fernswarthy's Basement");

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
            "Itznotyerzitz Mine (in Disguise)",
            "The Knob Shaft (Mining)",
            "Anemone Mine (Mining)",
            "The Velvet / Gold Mine (Mining)"
          })
      public void thatMiningTakesOneTurn(String adventureName) {
        var cleanups = new Cleanups();
        try (cleanups) {
          KoLAdventure adventure = AdventureDatabase.getAdventure(adventureName);
          testKoLAdventure(adventure, 1);
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
      public void thatFernswarthysBasementTakesOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testKoLAdventure(BASEMENT, 1);
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
    }

    @Nested
    class VolcanoIsland {
      // volcanoisland.php&action=tniat
      // - First entry into The Nemesis' Lair
      // volcanoisland.php&action=tuba
      // - redirect to The Island Barracks (only after Nemesis defeated?)

      public void testVolcano(String action, String adventureName, int turns) {
        // Automation
        var request = new VolcanoIslandRequest(action);
        assertEquals(turns, TurnCounter.getTurnsUsed(request));

        // Logging
        String url = "volcanoisland.php?action=" + action;
        KoLAdventure adventure = AdventureDatabase.getAdventureByURL(url);
        assertEquals(adventureName, adventure.getAdventureName());

        // Relay Browser
        var relay = new RelayRequest(false);
        relay.constructURLString(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(relay));

        // visit_url
        var generic = new GenericRequest(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(generic));
      }

      @Test
      public void thatInitialNemesisLairVisitUsesNoTurns() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testVolcano("tniat", "The Nemesis' Lair", 0);
        }
      }

      @Test
      public void thatThePostNemesisBarracksTakesOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testVolcano("tuba", "The Island Barracks", 1);
        }
      }
    }

    @Nested
    class TavernCellar {
      @AfterEach
      public void afterEach() {
        CELLAR.getRequest().removeFormField("action");
      }

      private static final KoLAdventure CELLAR =
          AdventureDatabase.getAdventureByName("The Typical Tavern Cellar");

      public void testCellar(String action, int turns) {
        // Automation
        var request = CELLAR.getRequest();
        if (action != null) {
          request.addFormField("action", action);
        }
        assertEquals(turns, TurnCounter.getTurnsUsed(request));

        // Relay Browser
        var relay = new RelayRequest(false);
        var url = "cellar.php";
        if (action != null) {
          url += "?action=" + action;
        }
        relay.constructURLString(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(relay));

        // visit_url
        var generic = new GenericRequest(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(generic));
      }

      @Test
      public void thatVisitingTavernCellarTakesNoTurns() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testCellar(null, 0);
        }
      }

      @Test
      public void thatExploringTavernCellarSquaresTakeOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testCellar("explore", 1);
        }
      }
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
    class BarrelFullOfBarrels {
      private static final KoLAdventure BARRELS =
          AdventureDatabase.getAdventureByName("The Barrel Full of Barrels");

      public void testChoice(int option, String slot, int turns) {
        // Automation
        // Ever since KoL moved the Barrel Full of Barrels from
        // barrel.php to choice.php, we no longer support automation.

        // Relay Browser
        var relay = new RelayRequest(false);
        // choice.php?whichchoice=1099&pwd&option=1&slot=00
        String url = "choice.php?whichchoice=1099&option=" + option;
        if (slot != null) {
          url += "slot=" + slot;
        }
        relay.constructURLString(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(relay));
        // visit_url
        var generic = new GenericRequest(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(generic));
      }

      @Test
      public void thatSmashingABarrelTakesOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testChoice(1, "22", 1);
        }
      }

      @Test
      public void thatTurningTheCrankTakesOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testChoice(2, null, 1);
        }
      }

      @Test
      public void thatExitingTakesNoTurns() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testChoice(3, null, 0);
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
      public void testMonster(String name, int turns) {
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
          testMonster("Black Crayon Penguin", 1);
        }
      }
    }

    @Nested
    class CakeArena {
      public void testArena(boolean compete, int turns) {
        // Automation
        var request = compete ? new CakeArenaRequest(1, 1) : new CakeArenaRequest();
        assertEquals(turns, TurnCounter.getTurnsUsed(request));

        // Relay Browser
        var relay = new RelayRequest(false);
        String url = "arena.php";
        if (compete) {
          url += "?action=go&whichopp=1&event=1";
        }

        relay.constructURLString(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(relay));

        // visit_url
        var generic = new GenericRequest(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(generic));
      }

      @Test
      public void thatNotCompetingTakesNoTurns() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testArena(false, 0);
        }
      }

      @Test
      public void thatCompetingTakesOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testArena(true, 1);
        }
      }
    }

    @Nested
    class Richard {
      @Test
      public void thatHelpingRichardTakesTurns() {
        int turns = 5;

        // Automation
        var request = new RichardRequest(1);
        request.setTurnCount(5);
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
    }

    @Nested
    class SuburbanDis {
      public void testAction(String action, int turns) {
        // Automation
        var request = new SuburbanDisRequest(action);
        assertEquals(turns, TurnCounter.getTurnsUsed(request));

        // Relay Browser
        var relay = new RelayRequest(false);
        String url = "suburbandis.php?action=" + action;
        relay.constructURLString(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(relay));

        // visit_url
        var generic = new GenericRequest(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(generic));
      }

      @Test
      public void thatDoingThisTakesATurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testAction("dothis", 1);
        }
      }

      @Test
      public void thatVisitingALtarTakesNoTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          testAction("altar", 0);
        }
      }
    }

    @Nested
    class BeachComb {
      public void testCommand(BeachCombCommand command, int turns) {
        // Automation
        var request =
            switch (command) {
              case VISIT -> new BeachCombRequest();
              case EXIT, COMMON, RANDOM -> new BeachCombRequest(command);
              case HEAD -> new BeachCombRequest(BeachManager.idToBeachHead.get(1));
              case WANDER -> new BeachCombRequest(137);
              case COMB -> new BeachCombRequest(6, 6);
            };
        assertEquals(turns, TurnCounter.getTurnsUsed(request));

        // Relay Browser
        var relay = new RelayRequest(false);
        String url =
            switch (command) {
              case VISIT -> "main.php?comb=1";
              case EXIT, COMMON, RANDOM -> "choice.php?whichchoice=1388&option=" + command.option();
              case HEAD -> "choice.php?whichchoice=1388&option=" + command.option() + "&buff=1";
              case WANDER -> "choice.php?whichchoice=1388&option="
                  + command.option()
                  + "&minutes=137";
              case COMB -> "choice.php?whichchoice=1388&option="
                  + command.option()
                  + "&coords=6,1364";
            };
        relay.constructURLString(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(relay));

        // visit_url
        var generic = new GenericRequest(url);
        assertEquals(turns, TurnCounter.getTurnsUsed(generic));
      }

      @Test
      public void thatFreeCombsTakeNoTurns() {
        var cleanups = new Cleanups(withProperty("_freeBeachWalksUsed", 0));
        try (cleanups) {
          // Taking out your Beach Comb
          testCommand(BeachCombCommand.VISIT, 0);
          // Putting away your Beach Comb
          testCommand(BeachCombCommand.EXIT, 0);
          // Getting common items using 10 combs
          testCommand(BeachCombCommand.COMMON, 0);
          // Getting a buff from a Beach Head
          testCommand(BeachCombCommand.HEAD, 0);
          // Wandering to a random spot on the beach
          testCommand(BeachCombCommand.RANDOM, 0);
          // Wandering to a specific spot on the beach
          testCommand(BeachCombCommand.WANDER, 0);
          // Combing a square
          testCommand(BeachCombCommand.COMB, 0);
        }
      }

      @Test
      public void thatNonFreeCombsTakeOneTurn() {
        var cleanups = new Cleanups(withProperty("_freeBeachWalksUsed", 11));
        try (cleanups) {
          // Taking out your Beach Comb
          testCommand(BeachCombCommand.VISIT, 0);
          // Putting away your Beach Comb
          testCommand(BeachCombCommand.EXIT, 0);
          // Getting common items using 10 combs
          testCommand(BeachCombCommand.COMMON, 0);
          // Getting a buff from a Beach Head
          testCommand(BeachCombCommand.HEAD, 1);
          // Wandering to a random spot on the beach
          testCommand(BeachCombCommand.RANDOM, 1);
          // Wandering to a specific spot on the beach
          testCommand(BeachCombCommand.WANDER, 1);
          // Combing a square
          //
          // It takes a turn to wander down the beach.  Once there, you can
          // comb a patch of sand. Additional patches take another turn.  each.
          //
          // *** We do not account for that in getAdventuresUsed,, yet
          testCommand(BeachCombCommand.COMB, 1);
        }
      }
    }

    @Nested
    class UseItem {
      public void testUsage(int itemId, int turns) {
        // Automation
        var request = UseItemRequest.getInstance(itemId);
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

      @ParameterizedTest
      @ValueSource(
          ints = {ItemPool.BLACK_PUDDING, ItemPool.SPOOKY_PUTTY_MONSTER, ItemPool.WHITE_PAGE})
      public void thatUsingThisTakesATurn(int itemId) {
        var cleanups = new Cleanups();
        try (cleanups) {
          testUsage(itemId, 1);
        }
      }

      @ParameterizedTest
      @ValueSource(ints = {ItemPool.SEAL_TOOTH, ItemPool.COTTAGE, ItemPool.BRIEFCASE})
      public void thatUsingThisTakesNoTurn(int itemId) {
        var cleanups = new Cleanups();
        try (cleanups) {
          testUsage(itemId, 0);
        }
      }
    }

    @Nested
    class UseSkill {
      public void testUsage(int skillId, int turns) {
        // Automation
        var request = UseSkillRequest.getInstance(skillId);
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

      @ParameterizedTest
      @ValueSource(ints = {SkillPool.HIBERNATE, SkillPool.SIMMER})
      public void thatUsingThisTakesATurn(int itemId) {
        var cleanups = new Cleanups();
        try (cleanups) {
          testUsage(itemId, 1);
        }
      }

      @ParameterizedTest
      @ValueSource(ints = {SkillPool.BEND_HELL, SkillPool.COCOON})
      public void thatUsingThisTakesNoTurn(int itemId) {
        var cleanups = new Cleanups();
        try (cleanups) {
          testUsage(itemId, 0);
        }
      }
    }

    @Nested
    class Craft {
      @Test
      public void thatCreatingGinAndTonicTakesOneTurn() {
        var cleanups = new Cleanups();
        try (cleanups) {
          String url = "craft.php?action=craft&mode=cocktail&ajax=1&qty=1&a=1553&b=1559";
          var generic = new GenericRequest(url);
          assertEquals(1, TurnCounter.getTurnsUsed(generic));
        }
      }

      @Test
      public void thatCreatingFineWineTakesNoTurns() {
        var cleanups = new Cleanups();
        try (cleanups) {
          String url = "craft.php?action=craft&mode=cocktail&ajax=1&qty=1&a=247&b=244";
          var generic = new GenericRequest(url);
          assertEquals(0, TurnCounter.getTurnsUsed(generic));
        }
      }
    }
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
