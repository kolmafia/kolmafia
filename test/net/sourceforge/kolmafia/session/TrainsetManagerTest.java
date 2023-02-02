package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withChoice;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withWorkshedItem;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class TrainsetManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("TrainsetManagerTest");
    Preferences.reset("TrainsetManagerTest");
  }

  @Test
  public void canDetectTrainConfiguration() {
    var cleanups =
        new Cleanups(
            withProperty("trainsetConfiguration", ""),
            withProperty("trainsetPosition", 0),
            withChoice(1485, html("request/test_trainset_detects_configuration.html")));

    try (cleanups) {
      assertThat("trainsetPosition", isSetTo(0));
      assertThat(
          "trainsetConfiguration",
          isSetTo(
              "coal_hopper,meat_mine,brawn_silo,grain_silo,candy_factory,trackside_diner,logging_mill,viewing_platform"));
    }
  }

  @Test
  public void canDetectTrainStationMovement() {
    var cleanups =
        new Cleanups(
            withProperty("trainsetPosition", 42),
            withWorkshedItem(ItemPool.MODEL_TRAIN_SET),
            withFight(0));

    try (cleanups) {
      String html = html("request/test_trainset_fight_diner_food.html");

      FightRequest.updateCombatData(null, null, html);

      assertThat("trainsetPosition", isSetTo(43));
    }
  }

  @Test
  public void canDetectTrainStationMovementEmptyTrack() {
    var cleanups =
        new Cleanups(
            withProperty("trainsetPosition", 42),
            withWorkshedItem(ItemPool.MODEL_TRAIN_SET),
            withFight(0));

    try (cleanups) {
      String html = html("request/test_trainset_detects_movement_empty_track.html");

      FightRequest.updateCombatData(null, null, html);

      assertThat("trainsetPosition", isSetTo(43));
    }
  }

  @ParameterizedTest
  @CsvSource({"84, Trackside Diner, true", "83, Trackside Diner, false"})
  public void canDetectExpectedTrainpiece(
      int stationPosition, String stationName, boolean expectedResult) {
    var cleanups =
        new Cleanups(
            withProperty("trainsetPosition", stationPosition),
            withProperty(
                "trainsetConfiguration",
                "coal_hopper,meat_mine,brawn_silo,grain_silo,candy_factory,trackside_diner,logging_mill,viewing_platform"));

    try (cleanups) {
      boolean result = TrainsetManager.onTrainsetMove(stationName);

      assertEquals(expectedResult, result);
    }
  }

  @Test
  public void canHandleUnexpectedConfigurationCooldown() {
    // Test when we're not allowed to configure the trainset, but the properties said we could
    // An example is that a user played 20 fights, then switched configuration outside of mafia
    var cleanups =
        new Cleanups(
            withProperty("trainsetConfiguration", 0),
            withProperty("lastTrainsetConfiguration", 0),
            withProperty("trainsetPosition", 20),
            withChoice(
                1485, html("request/test_trainset_detects_configuration_bad_tracking.html")));

    try (cleanups) {
      // Disabled as we cannot check our last position due to missing information
      // assertThat("trainsetPosition", isSetTo(0));

      // As there are 5 laps remaining, we assume the configuration was changed last turn
      assertThat("lastTrainsetConfiguration", isSetTo(19));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "2, 17", // 2 is behind the expected position range, and should be reset to 17
    "11, 11", // 11, 16 & 17 is inside the expected position range, and should remain unchanged
    "16, 16", "17, 17",
    "18, 17", // 18 & 20 is ahead of the expected position range, and should be reset to 17
    "20, 17"
  })
  public void canHandleUnexpectedConfigurationCooldownChange(
      int lastConfigured, int expectedConfigured) {
    // Test when we're not allowed to configure the trainset, and expected 3 laps remaining. But 5
    // laps remained. Which is inaccurate for lastTrainsetConfiguration.
    // An example is that a user played in mafia, reconfigured, then played outside of mafia and
    // reconfigured again.

    // The trainset position in the html is 2, (8 x 2) + 2 = 18
    // With 5 laps remaining, the last configured should be -1 to -7 turns behind.
    // Therefore, lastTrainsetConfiguration is expected to be in the range 11 to 17

    var cleanups =
        new Cleanups(
            withProperty("trainsetConfiguration", ""),
            withProperty("lastTrainsetConfiguration", lastConfigured),
            withProperty("trainsetPosition", 18),
            withChoice(
                1485, html("request/test_trainset_detects_configuration_bad_tracking.html")));

    try (cleanups) {
      // Disabled as we cannot check our last position due to missing information
      // assertThat("trainsetPosition", isSetTo(0));

      // As there are 5 laps remaining, we assume the configuration was changed last turn
      assertThat("lastTrainsetConfiguration", isSetTo(expectedConfigured));
    }
  }

  @Test
  public void canHandleUnexpectedConfigurationCooldownMissing() {
    // Test when we're allowed to configure the trainset, but we expected otherwise
    var cleanups =
        new Cleanups(
            withProperty("lastTrainsetConfiguration", 10),
            withProperty("trainsetPosition", 24),
            withChoice(1485, html("request/test_trainset_detects_configuration.html")));

    try (cleanups) {
      assertThat("trainsetPosition", isSetTo(24));

      // Cooldown is expected to be trainsetPosition - 40
      assertThat("lastTrainsetConfiguration", isSetTo(-16));
    }
  }

  @Test
  public void canHandleUnexpectedPosition() {
    // trainsetPosition is set to 10, which translates to 2. But the trainset is actually in
    // position 0.
    // Test that the trainset position is correctly moved forwards to a correct position
    // Note that this could force lastTrainsetConfiguration to update as well if configuration state
    // is different from expected, but that's not a real
    // issue.
    var cleanups =
        new Cleanups(
            withProperty("lastTrainsetConfiguration", -40),
            withProperty("trainsetPosition", 10),
            withChoice(1485, html("request/test_trainset_detects_configuration.html")));

    try (cleanups) {
      // As 10 > 8, it's moved up to the next multiple of 8 which is 16
      assertThat("trainsetPosition", isSetTo(16));
      // Cooldown is expected to be at least trainsetPosition - 40 as we can configure this trainset
      assertThat("lastTrainsetConfiguration", isSetTo(-40));
    }
  }

  @Test
  public void canTrackConfigurationChanged() {
    // Track when we've just reconfigured our trainset
    var cleanups =
        new Cleanups(
            withProperty("trainsetConfiguration", ""),
            withProperty("lastTrainsetConfiguration", 0),
            withProperty("trainsetPosition", 20),
            withChoice(1485, html("request/test_trainset_detects_configuration_updated.html")));

    try (cleanups) {
      assertThat("trainsetPosition", isSetTo(20));
      assertThat(
          "trainsetConfiguration",
          isSetTo(
              "coal_hopper,meat_mine,logging_mill,ore_hopper,prawn_silo,trackside_diner,candy_factory,tower_fizzy"));
      assertThat("lastTrainsetConfiguration", isSetTo(20));
    }
  }

  @Test
  public void canHandleTrainsetReconfiguredThisTurn() {
    // If the trainset has been reconfigured this turn, kol does not inform the user of this if the
    // workshed is visited.
    // As such, we should be checking lastTrainsetConfiguration to determine if it has been
    // modified.
    // If lastTrainsetConfiguration is equal to trainsetPosition, it should not be updated.
    var cleanups =
        new Cleanups(
            withProperty("lastTrainsetConfiguration", 0),
            withProperty("trainsetPosition", 0),
            withChoice(1485, html("request/test_trainset_detects_configuration.html")));

    try (cleanups) {
      assertThat("lastTrainsetConfiguration", isSetTo(0));
    }
  }
}
