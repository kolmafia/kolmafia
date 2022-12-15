package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withChoice;
import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.FightRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    var cleanups = new Cleanups(withProperty("trainsetPosition", 42), withFight(0));

    try (cleanups) {
      String html = html("request/test_trainset_fight_diner_food.html");

      FightRequest.updateCombatData(null, null, html);

      assertThat("trainsetPosition", isSetTo(43));
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

  @Test
  public void canHandleUnexpectedConfigurationCooldownChange() {
    // Test when we're not allowed to configure the trainset, and expected 3 laps remaining. But 5
    // laps remained.
    // An example is that a user played in mafia, reconfigured, then played outside of mafia and
    // reconfigured again.
    var cleanups =
        new Cleanups(
            withProperty("trainsetConfiguration", ""),
            withProperty("lastTrainsetConfiguration", 2),
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
  public void canTrackFightDroppedFood() {
    // Track when we have a valid target for the diner food drop
    var cleanups = new Cleanups(withProperty("lastFoodDropped", 11), withFight(0));

    try (cleanups) {
      String html = html("request/test_trainset_fight_dropped_food.html");

      FightRequest.updateCombatData(null, null, html);

      assertThat("lastFoodDropped", isSetTo(49));
    }
  }

  @Test
  public void canTrackDinerDroppedFood() {
    // Track when the diner dupes our food
    var cleanups = new Cleanups(withProperty("lastFoodDropped", 49), withFight(0));

    try (cleanups) {
      String html = html("request/test_trainset_fight_diner_food.html");

      FightRequest.updateCombatData(null, null, html);

      assertThat("lastFoodDropped", isSetTo(-1));
    }
  }

  @Test
  public void doesNotSetLastFoodDroppedIfDinerDropped() {
    // Ensure that lastFoodDropped is not updated to the food that the diner dropped
    var cleanups = new Cleanups(withProperty("lastFoodDropped", -1), withFight(0));

    try (cleanups) {
      String html = html("request/test_trainset_fight_diner_food.html");

      FightRequest.updateCombatData(null, null, html);

      assertThat("lastFoodDropped", isSetTo(-1));
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
}
