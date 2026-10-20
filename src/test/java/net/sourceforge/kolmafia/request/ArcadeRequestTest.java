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
}
