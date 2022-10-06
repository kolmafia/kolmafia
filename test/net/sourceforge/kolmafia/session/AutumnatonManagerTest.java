package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withChoice;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withTurnsPlayed;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AutumnatonManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("AutumnatonManagerTest");
    Preferences.reset("AutumnatonManagerTest");
  }

  @ParameterizedTest
  @CsvSource({
    "many_upgrades, 'dualexhaust,leftleg1,periscope,radardish,rightarm1,rightleg1'",
    "with_hat, 'base_blackhat,cowcatcher,leftleg1,rightleg1'",
  })
  public void canDetectUpgrades(final String fixture, final String upgrades) {
    var cleanups =
        new Cleanups(
            withProperty("autumnatonUpgrades", ""),
            withChoice(1483, html("request/test_choice_autumnaton_" + fixture + ".html")));

    try (cleanups) {
      assertThat("autumnatonUpgrades", isSetTo(upgrades));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "0, '', 11",
    "0, 'leftleg1,rightleg1', 11",
    "2, 'leftleg1', 22",
    "2, 'leftleg1,rightleg1', 11",
    "4, '', 55",
    "4, 'rightleg1', 44",
  })
  public void canDetectQuest(final int questsToday, final String upgrades, final int expected) {
    var cleanups =
        new Cleanups(
            withTurnsPlayed(0),
            withProperty("autumnatonUpgrades", upgrades),
            withProperty("_autumnatonQuests", questsToday));

    try (cleanups) {
      AutumnatonManager.postChoice(
          2,
          html("request/test_choice_autumnaton_quest_kitchen.html"),
          AdventurePool.HAUNTED_KITCHEN);
      assertThat("autumnatonQuestLocation", isSetTo(AdventurePool.HAUNTED_KITCHEN));
      assertThat("autumnatonQuestTurn", isSetTo(expected));
    }
  }
}
