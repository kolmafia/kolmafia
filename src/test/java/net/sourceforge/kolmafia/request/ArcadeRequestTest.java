package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import internal.helpers.Player;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ArcadeRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("ArcadeRequestTest");
  }

  @BeforeEach
  public void beforeEach() {
    KoLConstants.inventory.clear();
    Preferences.reset("ArcadeRequestTest");
  }

  private static String withCapturedLogs(Runnable action) {
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();

    try (PrintStream out = new PrintStream(ostream, true)) {
      RequestLogger.openCustom(out);
      action.run();
      RequestLogger.closeCustom();
    }

    return ostream.toString().trim();
  }

  @Nested
  class Skeeball {
    @Test
    public void playingSkeeballUsesAGameGridToken() {
      AdventureResult.addResultToList(KoLConstants.inventory, ArcadeRequest.TOKEN);

      ArcadeRequest.parseResponse(
          "place.php?whichplace=arcade&action=arcade_skeeball",
          html("request/test_arcade_skeeball.html"));

      assertThat(ArcadeRequest.TOKEN.getCount(KoLConstants.inventory), is(0));
    }

    @Test
    public void visitingJackassPlumberRecordsTheDailyCheck() {
      ArcadeRequest.parseResponse(
          "place.php?whichplace=arcade&action=arcade_plumber",
          html("request/test_arcade_plumber.html"));

      assertThat(Preferences.getBoolean("_defectiveTokenChecked"), is(true));
    }
  }

  @Nested
  class FightersAutoPlay {
    @Test
    public void startsTheMatchWhenTheEntryPageShowsOne() {
      var request = new GenericRequest("choice.php?whichchoice=485&option=1");
      request.responseText = html("request/test_arcade_fighters_entry.html");

      assertThat(ArcadeRequest.autoChoiceFightersOfFighting(request), equalTo("6"));
    }

    @Test
    public void picksAMoveThatCountersTheThreat() {
      var request = new GenericRequest("choice.php?whichchoice=485&option=1");
      request.responseText = html("request/test_arcade_fighters_round_two.html");

      assertThat(ArcadeRequest.autoChoiceFightersOfFighting(request), equalTo("1"));
      // Serenity's Leg Sweep threat is countered by a Head Kick.
      assertThat(request.getFormField("attack"), equalTo("hk"));
    }
  }

  @Nested
  class DungeonFistAutoPlay {
    @Test
    public void followsTheScriptForEachStep() {
      assertThat(ArcadeRequest.autoDungeonFist(0, ""), equalTo("3"));
      assertThat(ArcadeRequest.autoDungeonFist(1, ""), equalTo("1"));
      assertThat(ArcadeRequest.autoDungeonFist(2, ""), equalTo("1"));
    }

    @Test
    public void stopsOutsideTheScript() {
      assertThat(ArcadeRequest.autoDungeonFist(-1, ""), equalTo("0"));
      assertThat(ArcadeRequest.autoDungeonFist(10000, ""), equalTo("0"));
    }
  }

  @Nested
  class RegisterRequest {
    // Pin the ascension so registerRequest never posts the town_wrong visit.
    private static final int ASCENSION = 7;

    private static void addToken() {
      AdventureResult.addResultToList(KoLConstants.inventory, ArcadeRequest.TOKEN);
    }

    @Test
    public void ignoresOtherPlaces() {
      try (var ascension = Player.withAscensions(ASCENSION)) {
        Preferences.setInteger("lastArcadeAscension", ASCENSION);
        assertThat(ArcadeRequest.registerRequest("place.php?whichplace=town"), is(false));
      }
    }

    @Test
    public void ignoresTheArcadeWithoutAnAction() {
      try (var ascension = Player.withAscensions(ASCENSION)) {
        Preferences.setInteger("lastArcadeAscension", ASCENSION);
        assertThat(ArcadeRequest.registerRequest("place.php?whichplace=arcade"), is(false));
      }
    }

    @Test
    public void ignoresActionsThatAreNotGames() {
      try (var ascension = Player.withAscensions(ASCENSION);
          var turns = Player.withAdventuresLeft(50)) {
        Preferences.setInteger("lastArcadeAscension", ASCENSION);
        addToken();
        assertThat(
            ArcadeRequest.registerRequest("place.php?whichplace=arcade&action=arcade_highscores"),
            is(false));
      }
    }

    @Test
    public void plumberNeedsNoToken() {
      try (var ascension = Player.withAscensions(ASCENSION)) {
        Preferences.setInteger("lastArcadeAscension", ASCENSION);
        assertThat(
            ArcadeRequest.registerRequest("place.php?whichplace=arcade&action=arcade_plumber"),
            is(true));
      }
    }

    @Test
    public void gamesWithoutATokenAreRecognisedButNotCounted() {
      try (var ascension = Player.withAscensions(ASCENSION)) {
        Preferences.setInteger("lastArcadeAscension", ASCENSION);
        assertThat(
            ArcadeRequest.registerRequest("place.php?whichplace=arcade&action=arcade_spacetrip"),
            is(true));
      }
    }

    @Test
    public void gamesUseOneTokenWhenTheyHaveTurns() {
      var games = new String[] {"spacetrip", "demonstar", "meteoid", "fighters", "fist"};
      try (var ascension = Player.withAscensions(ASCENSION);
          var turns = Player.withAdventuresLeft(50)) {
        Preferences.setInteger("lastArcadeAscension", ASCENSION);
        for (String game : games) {
          KoLConstants.inventory.clear();
          addToken();
          var url = "place.php?whichplace=arcade&action=arcade_" + game;
          assertThat(game, ArcadeRequest.registerRequest(url), is(true));
          assertThat(game, ArcadeRequest.TOKEN.getCount(KoLConstants.inventory), is(0));
        }
      }
    }

    @Test
    public void gamesKeepTheTokenWhenTurnsAreShort() {
      try (var ascension = Player.withAscensions(ASCENSION);
          var turns = Player.withAdventuresLeft(4)) {
        Preferences.setInteger("lastArcadeAscension", ASCENSION);
        addToken();
        assertThat(
            ArcadeRequest.registerRequest("place.php?whichplace=arcade&action=arcade_spacetrip"),
            is(true));
        assertThat(ArcadeRequest.TOKEN.getCount(KoLConstants.inventory), is(1));
      }
    }

    @Test
    public void skeeballUsesNoTurnsAndKeepsTheToken() {
      try (var ascension = Player.withAscensions(ASCENSION)) {
        Preferences.setInteger("lastArcadeAscension", ASCENSION);
        addToken();
        assertThat(
            ArcadeRequest.registerRequest("place.php?whichplace=arcade&action=arcade_skeeball"),
            is(true));
        assertThat(ArcadeRequest.TOKEN.getCount(KoLConstants.inventory), is(1));
      }
    }
  }

  @Nested
  class ParseResponseGuards {
    @Test
    public void ignoresOtherPlaces() {
      Preferences.setBoolean("_defectiveTokenChecked", false);

      ArcadeRequest.parseResponse("place.php?whichplace=town", "");

      assertThat(Preferences.getBoolean("_defectiveTokenChecked"), is(false));
    }

    @Test
    public void ignoresTheArcadeWithoutAnAction() {
      Preferences.setBoolean("_defectiveTokenChecked", false);

      ArcadeRequest.parseResponse("place.php?whichplace=arcade", "");

      assertThat(Preferences.getBoolean("_defectiveTokenChecked"), is(false));
    }
  }

  @Nested
  class SkeeballMessages {
    // An earlier abort would otherwise silence updateDisplay.
    @BeforeEach
    public void continueAfterAnyAbort() {
      StaticEntity.setContinuationState(MafiaState.CONTINUE);
    }

    @Test
    public void reportsTheTicketsForAPlay() {
      var request = new ArcadeRequest("arcade_skeeball");
      request.responseText = html("request/test_arcade_skeeball.html");

      var logs = withCapturedLogs(request::processResults);

      assertThat(logs, containsString("Token transformed into tickets."));
    }
  }

  @Nested
  class FightersLogging {
    @Test
    public void gameOverLogsTheFinalRound() {
      try (var visit = Player.withChoice(485, html("request/test_arcade_fighters_entry.html"))) {
        var request = new GenericRequest("choice.php?whichchoice=485&option=1&attack=lk");
        request.responseText = html("request/test_arcade_fighters_game_over.html");

        var logs = withCapturedLogs(() -> ArcadeRequest.postChoiceFightersOfFighting(request, 1));

        assertThat(logs, equalTo("Round 0: Leg Sweep Result: You Lose! Final Score: 0"));
      }
    }

    @Test
    public void anUnknownAttackLogsNothing() {
      var request = new GenericRequest("choice.php?whichchoice=485&option=1&attack=zz");
      request.responseText = html("request/test_arcade_fighters_round_two.html");

      var logs = withCapturedLogs(() -> ArcadeRequest.postChoiceFightersOfFighting(request, 1));

      assertThat(logs, equalTo(""));
    }
  }

  @Nested
  class DemonStarMoves {
    private static String moveLog(String direction) {
      var request = new GenericRequest("choice.php?whichchoice=471&option=1&" + direction);
      request.responseText = "";
      return withCapturedLogs(() -> ArcadeRequest.postChoiceDemonStar(request, 1));
    }

    @Test
    public void everyDirectionIsNamed() {
      try (var visit = Player.withChoice(471, html("request/test_arcade_demonstar_entry.html"))) {
        assertThat(moveLog("mv=-1,-1"), equalTo("Action: Move northwest"));
        assertThat(moveLog("mv=0,-1"), equalTo("Action: Move north"));
        assertThat(moveLog("mv=1,-1"), equalTo("Action: Move northeast"));
        assertThat(moveLog("mv=-1,0"), equalTo("Action: Move west"));
        assertThat(moveLog("mv=0,0"), equalTo("Action: Stay put"));
        assertThat(moveLog("mv=1,0"), equalTo("Action: Move east"));
        assertThat(moveLog("mv=-1,1"), equalTo("Action: Move southwest"));
        assertThat(moveLog("mv=0,1"), equalTo("Action: Move south"));
        assertThat(moveLog("mv=1,1"), equalTo("Action: Move southeast"));
      }
    }

    @Test
    public void anUnreadableMoveIsNotLogged() {
      try (var visit = Player.withChoice(471, html("request/test_arcade_demonstar_entry.html"))) {
        assertThat(moveLog("mv=2,2"), equalTo(""));
      }
    }
  }

  @Nested
  class ProcessResultsWithoutAGame {
    @Test
    public void aRequestWithNoActionLogsNothing() {
      var request = new ArcadeRequest();
      request.responseText = html("request/test_arcade_skeeball.html");

      var logs = withCapturedLogs(request::processResults);

      assertThat(logs, equalTo(""));
    }

    @Test
    public void aGameActionOtherThanSkeeballLogsNothing() {
      var request = new ArcadeRequest("arcade_plumber");
      request.responseText = "";

      var logs = withCapturedLogs(request::processResults);

      assertThat(logs, equalTo(""));
    }
  }

  @Nested
  class DemonStarUnusualMoves {
    private static String moveLog(String direction) {
      var request = new GenericRequest("choice.php?whichchoice=471&option=1&" + direction);
      request.responseText = "";
      return withCapturedLogs(() -> ArcadeRequest.postChoiceDemonStar(request, 1));
    }

    @Test
    public void movesOutsideTheGridAreNotNamed() {
      try (var visit = Player.withChoice(471, html("request/test_arcade_demonstar_entry.html"))) {
        assertThat(moveLog("mv=10,0"), equalTo(""));
        assertThat(moveLog("mv=0,10"), equalTo(""));
        assertThat(moveLog("mv=-1,10"), equalTo(""));
        assertThat(moveLog("mv=1,10"), equalTo(""));
      }
    }
  }

  @Nested
  class FightersDecoration {
    @Test
    public void nothingChangesWithHintsOff() {
      Preferences.setBoolean("arcadeGameHints", false);
      var original = html("request/test_arcade_fighters_round_two.html");
      var buffer = new StringBuffer(original);

      ArcadeRequest.decorateFightersOfFighting(buffer);

      assertThat(buffer.toString(), equalTo(original));
    }

    @Test
    public void choicesAreAnnotatedWithTheirEffectiveness() {
      Preferences.setBoolean("arcadeGameHints", true);
      var buffer = new StringBuffer(html("request/test_arcade_fighters_round_two.html"));

      ArcadeRequest.decorateFightersOfFighting(buffer);

      assertThat(buffer.toString(), containsString("<br><font size=-1>("));
      assertThat(buffer.toString(), containsString("Good)</font></form>"));
    }

    private static final String CHOICE_FORM = "<form method=\"post\" action=\"choice.php\">";
  }

  @Nested
  class MeteoidSpaceMallClose {
    @Test
    public void closingTheShopLogsNothing() {
      try (var visit = Player.withChoice(488, html("request/test_arcade_meteoid_entry.html"));
          var shop =
              Player.withChoice(488, 2, null, html("request/test_arcade_meteoid_spacemall.html"))) {
        var logs =
            withCapturedLogs(
                () -> {
                  try (var close =
                      Player.withChoice(
                          489, 6, null, html("request/test_arcade_meteoid_spacemall.html"))) {}
                });

        assertThat(logs, equalTo(""));
      }
    }

    @Test
    public void buyingAnUpgradeLogsIt() {
      try (var visit = Player.withChoice(488, html("request/test_arcade_meteoid_entry.html"));
          var shop =
              Player.withChoice(488, 2, null, html("request/test_arcade_meteoid_spacemall.html"))) {
        var logs =
            withCapturedLogs(
                () -> {
                  try (var buy =
                      Player.withChoice(
                          489, 1, null, html("request/test_arcade_meteoid_spacemall.html"))) {}
                });

        assertThat(logs, equalTo("Action: Increase Bomb Capacity from 1 to 2 (50 Credcrystals)"));
      }
    }
  }

  @Nested
  class PlumberRequests {
    private static final int ASCENSION = 9;

    @Test
    public void registeringAGameAfterAnAscensionChangeVisitsTheWrongSide() {
      try (var ascension = Player.withAscensions(ASCENSION);
          var threads = Mockito.mockStatic(RequestThread.class)) {
        Preferences.setInteger("lastArcadeAscension", -1);

        ArcadeRequest.registerRequest("place.php?whichplace=arcade&action=arcade_spacetrip");

        assertThat(Preferences.getInteger("lastArcadeAscension"), is(ASCENSION));
        threads.verify(() -> RequestThread.postRequest(any(GenericRequest.class)), times(1));
      }
    }

    @Test
    public void registeringAGameInTheSameAscensionDoesNotVisitTheWrongSide() {
      try (var ascension = Player.withAscensions(ASCENSION);
          var threads = Mockito.mockStatic(RequestThread.class)) {
        Preferences.setInteger("lastArcadeAscension", ASCENSION);

        ArcadeRequest.registerRequest("place.php?whichplace=arcade&action=arcade_plumber");

        threads.verify(() -> RequestThread.postRequest(any(GenericRequest.class)), never());
      }
    }

    @Test
    public void aFirstDailyCheckWithATokenVisitsTheWrongSideAndThePlumber() {
      try (var ascension = Player.withAscensions(ASCENSION);
          var threads = Mockito.mockStatic(RequestThread.class)) {
        Preferences.setBoolean("_defectiveTokenChecked", false);
        Preferences.setInteger("lastArcadeAscension", -1);
        AdventureResult.addResultToList(KoLConstants.inventory, ArcadeRequest.TOKEN);

        ArcadeRequest.checkJackassPlumber();

        assertThat(Preferences.getInteger("lastArcadeAscension"), is(ASCENSION));
        threads.verify(() -> RequestThread.postRequest(any(GenericRequest.class)), times(2));
      }
    }

    @Test
    public void anUnlockedCabinetOnlyVisitsThePlumber() {
      try (var ascension = Player.withAscensions(ASCENSION);
          var threads = Mockito.mockStatic(RequestThread.class)) {
        Preferences.setBoolean("_defectiveTokenChecked", false);
        Preferences.setInteger("lastArcadeAscension", ASCENSION);

        ArcadeRequest.checkJackassPlumber();

        threads.verify(() -> RequestThread.postRequest(any(GenericRequest.class)), times(1));
      }
    }
  }

  @Nested
  class DungeonFistRealPages {
    // Each captured room, with the log it produces when entered.
    private static String room(String page) {
      ArcadeRequest.visitDungeonFistChoice(html("request/test_arcade_fist_entry.html"));
      var request = new GenericRequest("choice.php?whichchoice=486&option=1");
      request.responseText = html(page);
      return withCapturedLogs(() -> ArcadeRequest.postChoiceDungeonFist(request, 99));
    }

    @Test
    public void grunts() {
      assertThat(room("request/test_arcade_fist_north.html"), equalTo("Encounter: Grunts"));
    }

    @Test
    public void stoneBox() {
      assertThat(
          room("request/test_arcade_fist_stone_box.html"), equalTo("Encounter: Large Stone Boxes"));
    }

    @Test
    public void demons() {
      assertThat(room("request/test_arcade_fist_demons.html"), equalTo("Encounter: Demons"));
    }

    @Test
    public void ghosts() {
      assertThat(
          room("request/test_arcade_fist_ghosts_and_bones.html"), equalTo("Encounter: Ghosts"));
    }

    @Test
    public void spirits() {
      assertThat(room("request/test_arcade_fist_spirits.html"), equalTo("Encounter: Ghosts"));
    }

    @Test
    public void bones() {
      assertThat(room("request/test_arcade_fist_bones.html"), equalTo("Encounter: Bone Piles"));
    }

    @Test
    public void death() {
      assertThat(room("request/test_arcade_fist_death.html"), equalTo("Encounter: Death"));
    }

    @Test
    public void musclePotion() {
      assertThat(
          room("request/test_arcade_fist_muscle_potion.html"), equalTo("You find a Muscle Potion"));
    }

    @Test
    public void treasureBehindTheLockedDoor() {
      assertThat(
          room("request/test_arcade_fist_treasure.html"),
          equalTo("You find a locked door\nYou unlock the door\nYou find treasure"));
    }

    @Test
    public void magicPotion() {
      assertThat(
          room("request/test_arcade_fist_magic_potion.html"), equalTo("You find a Magic Potion"));
    }
  }

  @Nested
  class ChooserFallThrough {
    @Test
    public void aMatchupWithNoGoodMoveReturnsNothing() throws ReflectiveOperationException {
      var tableField = ArcadeRequest.class.getDeclaredField("EFFECTIVENESS");
      tableField.setAccessible(true);
      int[][][] table = (int[][][]) tableField.get(null);
      var failField = ArcadeRequest.class.getDeclaredField("FAIL");
      failField.setAccessible(true);
      int fail = failField.getInt(null);

      // Serenity against Leg Sweep is the round_two matchup; blank its row so no move is GOOD.
      int[] row = table[3][2];
      int[] saved = row.clone();
      try {
        java.util.Arrays.fill(row, fail);
        var request = new GenericRequest("choice.php?whichchoice=485&option=1");
        request.responseText = html("request/test_arcade_fighters_round_two.html");

        assertThat(ArcadeRequest.autoChoiceFightersOfFighting(request), is(nullValue()));
        assertThat(request.getFormField("attack"), is(nullValue()));
      } finally {
        System.arraycopy(saved, 0, row, 0, row.length);
      }
    }
  }

  @Nested
  class Helpers {
    @Test
    public void everyGameCountsAsFiveTurns() {
      for (String game : new String[] {"fist", "meteoid", "fighters", "demonstar", "spacetrip"}) {
        assertThat(
            game,
            ArcadeRequest.getTurnsUsed(
                new GenericRequest("place.php?whichplace=arcade&action=arcade_" + game)),
            is(5));
      }
    }

    @Test
    public void arcadeGamesCountAsFiveTurns() {
      assertThat(
          ArcadeRequest.getTurnsUsed(
              new GenericRequest("place.php?whichplace=arcade&action=arcade_fist")),
          is(5));
      assertThat(
          ArcadeRequest.getTurnsUsed(
              new GenericRequest("place.php?whichplace=arcade&action=arcade_skeeball")),
          is(0));
      assertThat(
          ArcadeRequest.getTurnsUsed(new GenericRequest("place.php?whichplace=arcade")), is(0));
    }

    @Test
    public void gameChoicesAreRecognised() {
      for (int choice : new int[] {460, 471, 485, 486, 488, 491}) {
        assertThat("choice " + choice, ArcadeRequest.arcadeChoice(choice), is(true));
      }
      for (int choice : new int[] {459, 492}) {
        assertThat("choice " + choice, ArcadeRequest.arcadeChoice(choice), is(false));
      }
    }
  }

  @Nested
  class JackassPlumber {
    @Test
    public void alreadyCheckedTodayDoesNothing() {
      Preferences.setBoolean("_defectiveTokenChecked", true);
      Preferences.setInteger("lastArcadeAscension", -1);

      ArcadeRequest.checkJackassPlumber();

      assertThat(Preferences.getInteger("lastArcadeAscension"), is(-1));
    }

    @Test
    public void withoutATokenOrTicketTheCabinetStaysLocked() {
      Preferences.setBoolean("_defectiveTokenChecked", false);
      Preferences.setInteger("lastArcadeAscension", -1);

      ArcadeRequest.checkJackassPlumber();

      assertThat(Preferences.getInteger("lastArcadeAscension"), is(-1));
    }
  }

  @Nested
  class Meteoid {
    @Test
    public void teleportingLogsTheChosenAction() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(488, html("request/test_arcade_meteoid_entry.html"));
                    var teleport =
                        Player.withChoice(
                            488, 1, null, html("request/test_arcade_meteoid_teleport.html"))) {}
              });

      assertThat(logs, equalTo("Action: Teleport to Underground Complex (5 charges remaining)"));
    }

    @Test
    public void blockedMoveLogsTheDirectionAndNothingElse() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(488, html("request/test_arcade_meteoid_entry.html"));
                    var teleport =
                        Player.withChoice(
                            488, 1, null, html("request/test_arcade_meteoid_teleport.html"));
                    var wall =
                        Player.withChoice(
                            490, 1, null, html("request/test_arcade_meteoid_wall.html"))) {}
              });

      assertThat(
          logs,
          equalTo("Action: Teleport to Underground Complex (5 charges remaining)\nAction: Up"));
    }

    @Test
    public void guardedRoomLogsTheMonster() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(488, html("request/test_arcade_meteoid_entry.html"));
                    var teleport =
                        Player.withChoice(
                            488, 1, null, html("request/test_arcade_meteoid_teleport.html"));
                    var guarded =
                        Player.withChoice(
                            490, 4, null, html("request/test_arcade_meteoid_guarded.html"))) {}
              });

      assertThat(
          logs,
          equalTo(
              "Action: Teleport to Underground Complex (5 charges remaining)\n"
                  + "Action: Right\n"
                  + "Encounter: Snugglecrawler"));
    }

    @Test
    public void shotLogsTheEnergyLoss() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(488, html("request/test_arcade_meteoid_entry.html"));
                    var teleport =
                        Player.withChoice(
                            488, 1, null, html("request/test_arcade_meteoid_teleport.html"));
                    var guarded =
                        Player.withChoice(
                            490, 4, null, html("request/test_arcade_meteoid_guarded.html"));
                    var fight =
                        Player.withChoice(
                            490, 5, null, html("request/test_arcade_meteoid_fight.html"))) {}
              });

      assertThat(
          logs,
          equalTo(
              "Action: Teleport to Underground Complex (5 charges remaining)\n"
                  + "Action: Right\n"
                  + "Encounter: Snugglecrawler\n"
                  + "Action: Shoot\n"
                  + "Energy: 95. Bombs: 1. Missiles: 1 Credcrystals: 0."));
    }

    @Test
    public void chargeStationIsLogged() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(488, html("request/test_arcade_meteoid_entry.html"));
                    var teleport =
                        Player.withChoice(
                            488, 1, null, html("request/test_arcade_meteoid_teleport.html"));
                    var station =
                        Player.withChoice(
                            490,
                            3,
                            null,
                            html("request/test_arcade_meteoid_charge_station.html"))) {}
              });

      assertThat(
          logs,
          equalTo(
              "Action: Teleport to Underground Complex (5 charges remaining)\nAction: Left\nEncounter: Charge Station\nEnergy: 50. Bombs: 0. Missiles: 0 Credcrystals: 55."));
    }

    @Test
    public void navigatingTheBridgeToSpaceMallIsNotLogged() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(488, html("request/test_arcade_meteoid_entry.html"));
                    var spacemall =
                        Player.withChoice(
                            488, 2, null, html("request/test_arcade_meteoid_spacemall.html"))) {}
              });

      assertThat(logs, equalTo(""));
    }

    @Test
    public void gameEndReportsTheTotalScore() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(488, html("request/test_arcade_meteoid_entry.html"));
                    var end =
                        Player.withChoice(
                            488, 6, null, html("request/test_arcade_meteoid_end.html"));
                    var score =
                        Player.withChoice(
                            491, 1, null, html("request/test_arcade_meteoid_score.html"))) {}
              });

      assertThat(
          logs,
          equalTo("Action: Self Destruct\nTotal Score: 1,186\nYou acquire Game Grid ticket (5)"));
    }
  }

  @Nested
  class DemonStar {
    @Test
    public void movingLogsTheDirection() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(471, html("request/test_arcade_demonstar_entry.html"));
                    var move =
                        Player.withChoice(
                            471,
                            1,
                            "mv=-1,-1",
                            html("request/test_arcade_demonstar_northwest.html"))) {}
              });

      assertThat(logs, equalTo("Action: Move northwest"));
    }

    @Test
    public void warriorShipIsLogged() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(471, html("request/test_arcade_demonstar_entry.html"));
                    var move =
                        Player.withChoice(
                            471,
                            1,
                            "mv=-1,1",
                            html("request/test_arcade_demonstar_warrior.html"))) {}
              });

      assertThat(logs, equalTo("Action: Move southwest\nEncounter: a warrior"));
    }

    @Test
    public void walkingAwayReportsTheFailedScore() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(471, html("request/test_arcade_demonstar_entry.html"));
                    var end =
                        Player.withChoice(
                            471, 6, null, html("request/test_arcade_demonstar_walk_away.html"))) {}
              });

      assertThat(logs, equalTo("Action: Walk Away\nYOU HAVE FAILED. FINAL SCORE 0"));
    }
  }

  @Nested
  class FightersOfFighting {
    @Test
    public void visitLogsTheMatchup() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                    Player.withChoice(485, html("request/test_arcade_fighters_entry.html"))) {}
              });

      assertThat(logs, equalTo("Match: gausie vs. Serenity"));
    }

    @Test
    public void roundLogsTheMovesAndHealth() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(485, html("request/test_arcade_fighters_entry.html"));
                    var round =
                        Player.withChoice(
                            485,
                            1,
                            "attack=hk",
                            html("request/test_arcade_fighters_round_two.html"))) {}
              });

      assertThat(
          logs,
          equalTo(
              "Match: gausie vs. Serenity\n"
                  + "Round 0: Head Kick gausie (229 HP) Serenity (250 HP) Score: 0 Threat: Leg Sweep"));
    }

    @Test
    public void walkingAwayEndsTheGameWithoutARoundLog() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(485, html("request/test_arcade_fighters_entry.html"));
                    var over =
                        Player.withChoice(
                            485, 3, null, html("request/test_arcade_fighters_game_over.html"))) {}
              });

      assertThat(logs, equalTo("Match: gausie vs. Serenity\nYou acquire Game Grid ticket (5)"));
    }
  }

  @Nested
  class DungeonFist {
    @Test
    public void northLogsTheGruntRoom() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(486, html("request/test_arcade_fist_entry.html"));
                    var north =
                        Player.withChoice(
                            486, 1, null, html("request/test_arcade_fist_north.html"))) {}
              });

      assertThat(logs, equalTo("Action: Go North\nEncounter: Grunts"));
    }

    @Test
    public void westLogsTheDemonRoom() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(486, html("request/test_arcade_fist_entry.html"));
                    var north =
                        Player.withChoice(
                            486, 1, null, html("request/test_arcade_fist_north.html"));
                    var south =
                        Player.withChoice(
                            486, 2, null, html("request/test_arcade_fist_back_south.html"));
                    var west =
                        Player.withChoice(
                            486, 3, null, html("request/test_arcade_fist_west.html"))) {}
              });

      assertThat(
          logs,
          equalTo(
              "Action: Go North\n"
                  + "Encounter: Grunts\n"
                  + "Action: Run Back South\n"
                  + "Action: Go West\n"
                  + "Encounter: Demons"));
    }

    @Test
    public void quittingReportsTheFinalScore() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(486, html("request/test_arcade_fist_fight.html"));
                    var quit =
                        Player.withChoice(
                            486, 2, null, html("request/test_arcade_fist_quit.html"))) {}
              });

      assertThat(
          logs,
          equalTo(
              "Action: Quit\n"
                  + "YOU HAVE DIED. FINAL SCORE: 5375\n"
                  + "You acquire Game Grid ticket (6)"));
    }
  }

  @Nested
  class SpaceTripRest {
    @Test
    public void starbaseVisitLogsTheAction() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(460, html("request/test_arcade_spacetrip_entry.html"));
                    var nav =
                        Player.withChoice(
                            460, 1, null, html("request/test_arcade_spacetrip_navigation.html"));
                    var starbase =
                        Player.withChoice(
                            461, 5, null, html("request/test_arcade_spacetrip_starbase.html"))) {}
              });

      assertThat(logs, equalTo("Action: Visit Reynaldo's Roost Starbase (10 gas)"));
    }

    @Test
    public void zoologicalSquadLogsTheCrewAndResources() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(460, html("request/test_arcade_spacetrip_entry.html"));
                    var nav =
                        Player.withChoice(
                            460, 1, null, html("request/test_arcade_spacetrip_navigation.html"));
                    var quadrant =
                        Player.withChoice(
                            461,
                            1,
                            null,
                            html("request/test_arcade_spacetrip_alpha_quadrant.html"));
                    var explore =
                        Player.withChoice(
                            463, 1, null, html("request/test_arcade_spacetrip_explore.html"));
                    var zoological =
                        Player.withChoice(
                            465, 2, null, html("request/test_arcade_spacetrip_zoological.html"))) {}
              });

      assertThat(
          logs,
          equalTo(
              "Action: Alpha Quadrant\n"
                  + "Week 1: Explore an uncharted Alpha Quadrant system (10 gas)\n"
                  + "Crew: 30. Gas: 90 gallons. Money: 0 Crabs. Time left: 52 weeks.\n"
                  + "Action: Send a Zoological Squad\n"
                  + "You gain 360 Crabs\n"
                  + "Crew: 30. Gas: 90 gallons. Money: 360 Crabs. Time left: 51 weeks."));
    }

    @Test
    public void scienceExpeditionLogsResources() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(460, html("request/test_arcade_spacetrip_entry.html"));
                    var nav =
                        Player.withChoice(
                            460, 1, null, html("request/test_arcade_spacetrip_navigation.html"));
                    var quadrant =
                        Player.withChoice(
                            461,
                            1,
                            null,
                            html("request/test_arcade_spacetrip_alpha_quadrant.html"));
                    var explore =
                        Player.withChoice(
                            463, 1, null, html("request/test_arcade_spacetrip_explore.html"));
                    var science =
                        Player.withChoice(
                            465, 3, null, html("request/test_arcade_spacetrip_science.html"))) {}
              });

      assertThat(
          logs,
          equalTo(
              "Action: Alpha Quadrant\n"
                  + "Week 1: Explore an uncharted Alpha Quadrant system (10 gas)\n"
                  + "Crew: 30. Gas: 90 gallons. Money: 0 Crabs. Time left: 52 weeks.\n"
                  + "Action: Send a Science Expedition\n"
                  + "Crew: 30. Gas: 90 gallons. Money: 360 Crabs. Time left: 51 weeks."));
    }

    @Test
    public void retiringShowsTheScore() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(460, html("request/test_arcade_spacetrip_entry.html"));
                    var retire =
                        Player.withChoice(
                            460,
                            6,
                            "confirmretire=on",
                            html("request/test_arcade_spacetrip_retired.html"));
                    var score =
                        Player.withChoice(
                            484, 1, null, html("request/test_arcade_spacetrip_score.html"))) {}
              });

      assertThat(
          logs,
          equalTo("Action: See your score\nTotal Score: -323\nYou acquire Game Grid ticket (5)"));
    }
  }

  @Nested
  class DungeonFistFights {
    @Test
    public void attackingLogsTheCombatPotion() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(486, html("request/test_arcade_fist_fight_two.html"));
                    var potion =
                        Player.withChoice(
                            486, 1, null, html("request/test_arcade_fist_combat_potion.html"))) {}
              });

      assertThat(logs, equalTo("Action: Attack!\nYou find a Combat Potion"));
    }

    @Test
    public void walkingIntoTheLockedDoorIsLogged() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(486, html("request/test_arcade_fist_before_door.html"));
                    var door =
                        Player.withChoice(
                            486, 3, null, html("request/test_arcade_fist_locked_door.html"))) {}
              });

      assertThat(logs, equalTo("Action: Go West\nYou find a locked door"));
    }

    @Test
    public void takingTheKeyIsLogged() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(486, html("request/test_arcade_fist_before_key.html"));
                    var key =
                        Player.withChoice(
                            486, 1, null, html("request/test_arcade_fist_key.html"))) {}
              });

      assertThat(logs, equalTo("Action: Attack!\nYou find a key"));
    }

    @Test
    public void escapingTheDungeonReportsTheFinalScore() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(
                            486, html("request/test_arcade_fist_before_escape.html"));
                    var escape =
                        Player.withChoice(
                            486, 3, null, html("request/test_arcade_fist_escape.html"))) {}
              });

      assertThat(
          logs,
          equalTo(
              "Action: Go West\n"
                  + "You find a locked door\n"
                  + "You unlock the door\n"
                  + "You find the exit\n"
                  + "YOU HAVE ESCAPED THE DUNGEON! FINAL SCORE: 9150\n"
                  + "You acquire Game Grid ticket (11)"));
    }
  }

  @Nested
  class SpaceTrip {
    @Test
    public void navigatingToTheConsoleIsNotLogged() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(460, html("request/test_arcade_spacetrip_entry.html"));
                    var nav =
                        Player.withChoice(
                            460, 1, null, html("request/test_arcade_spacetrip_navigation.html"))) {}
              });

      assertThat(logs, equalTo(""));
    }

    @Test
    public void exploringLogsTheWeek() {
      var logs =
          withCapturedLogs(
              () -> {
                try (var visit =
                        Player.withChoice(460, html("request/test_arcade_spacetrip_entry.html"));
                    var nav =
                        Player.withChoice(
                            460, 1, null, html("request/test_arcade_spacetrip_navigation.html"));
                    var quadrant =
                        Player.withChoice(
                            461,
                            1,
                            null,
                            html("request/test_arcade_spacetrip_alpha_quadrant.html"));
                    var explore =
                        Player.withChoice(
                            463, 1, null, html("request/test_arcade_spacetrip_explore.html"))) {}
              });

      assertThat(
          logs,
          equalTo(
              "Action: Alpha Quadrant\n"
                  + "Week 1: Explore an uncharted Alpha Quadrant system (10 gas)\n"
                  + "Crew: 30. Gas: 90 gallons. Money: 0 Crabs. Time left: 52 weeks."));
    }
  }
}
