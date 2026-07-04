package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withLastLocation;
import static internal.helpers.Player.withNextMonster;
import static internal.helpers.Player.withPostChoice1;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class DailyDungeonManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("DailyDungeonManagerTest");
    Preferences.reset("DailyDungeonManagerTest");
  }

  @Nested
  class RoomEnter {
    @ParameterizedTest
    @CsvSource({
      "2,DT??_????_????,c02_enter_trap",
      "4,D??D_????_????,c04_enter_door",
      "5,D???_????_????,c05_enter_treasure",
      "10,D???_????_????,c10_enter_treasure",
      "15,D???_????_????,c15_enter_treasure",
    })
    public void enterDailyDungeonNcRoom(int chamber, String rooms, String responseFile) {
      var cleanups =
          new Cleanups(
              withProperty("_lastDailyDungeonRoom", 0),
              withProperty("dailyDungeonRooms", "D???_????_????"));

      try (cleanups) {
        var req = new GenericRequest("choice.php?forceoption=0");
        req.responseText = html("request/test_dailydungeon_" + responseFile + ".html");
        ChoiceManager.visitChoice(req);

        assertThat("_lastDailyDungeonRoom", isSetTo(chamber - 1));
        assertThat("dailyDungeonRooms", isSetTo(rooms));
      }
    }

    @Test
    public void enterDailyDungeonCombat() {
      var cleanups =
          new Cleanups(
              withLastLocation("The Daily Dungeon"),
              withProperty("_lastDailyDungeonRoom", 0),
              withProperty("dailyDungeonRooms", "DTTD_??T?_????"));

      try (cleanups) {
        var resp = html("request/test_dailydungeon_c09_enter_combat.html");
        FightRequest.updateCombatData(null, null, resp);

        assertThat("_lastDailyDungeonRoom", isSetTo(8));
        assertThat("dailyDungeonRooms", isSetTo("DTTD_??TM_????"));
      }
    }
  }

  @Nested
  class RoomExit {
    @ParameterizedTest
    @CsvSource({
      "693,2,6,7", // exit trap
      "693,3,6,6", // retreat trap
      "692,3,6,7", // exit door
      "692,8,6,6", // retreat door
      "690,1,4,5", // treasure
      "690,2,4,7", // treasure skip
    })
    void tracksDailyDungeonNcRoomExit(int choiceNum, int choice, int lastRoom, int currentRoom) {
      var cleanups =
          new Cleanups(
              withProperty("_lastDailyDungeonRoom", lastRoom), withPostChoice1(choiceNum, choice));

      try (cleanups) {
        assertThat("_lastDailyDungeonRoom", isSetTo(currentRoom));
      }
    }

    @Test
    void tracksDailyDungeonCompletion() {
      var cleanups =
          new Cleanups(
              withProperty("_lastDailyDungeonRoom", 0),
              withProperty("dailyDungeonDone", false),
              withPostChoice1(689, 1, html("request/test_dailydungeon_c15_exit_treasure.html")));

      try (cleanups) {
        assertThat("_lastDailyDungeonRoom", isSetTo(15));
        assertThat("dailyDungeonDone", isSetTo(true));
      }
    }

    @Test
    public void dailyDungeonMonsterVictory() {
      var cleanups =
          new Cleanups(
              withFight(),
              withNextMonster("dairy ooze"),
              withLastLocation("The Daily Dungeon"),
              withProperty("_lastDailyDungeonRoom", 1));
      try (cleanups) {
        FightRequest.updateFinalRoundData("", true, false);
        assertThat("_lastDailyDungeonRoom", isSetTo(2));
      }
    }

    @Test
    public void dailyDungeonMonsterLoss() {
      var cleanups =
          new Cleanups(
              withFight(),
              withNextMonster("dairy ooze"),
              withLastLocation("The Daily Dungeon"),
              withProperty("_lastDailyDungeonRoom", 1));
      try (cleanups) {
        FightRequest.updateFinalRoundData("", false, true);
        assertThat("_lastDailyDungeonRoom", isSetTo(1));
      }
    }
  }

  @Test
  public void roomUpdateSurvivesBrokenPref() {
    var cleanups =
        new Cleanups(
            withProperty("dailyDungeonRooms", "xyzabc"), withProperty("_lastDailyDungeonRoom", 8));

    try (cleanups) {
      DailyDungeonManager.handleRoomCompletion(9, DailyDungeonManager.RoomType.TRAP);
      assertThat("_lastDailyDungeonRoom", isSetTo(9));
      assertThat("dailyDungeonRooms", isSetTo("xyzabc??T_????"));
    }
  }
}
