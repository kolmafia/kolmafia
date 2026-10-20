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
}
