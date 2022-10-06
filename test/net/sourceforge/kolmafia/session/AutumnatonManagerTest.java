package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withChoice;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AutumnatonManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("AutumnatonManagerTest");
    Preferences.reset("AutumnatonManagerTest");
  }

  @Test
  public void canDetectUpgrades() {
    var cleanups =
        new Cleanups(
            withProperty("autumnatonUpgrades", ""),
            withChoice(1483, html("request/test_choice_autumnaton_many_upgrades.html")));

    try (cleanups) {
      assertThat(
          "autumnatonUpgrades",
          isSetTo("dualexhaust,leftleg1,periscope,radardish,rightarm1,rightleg1"));
    }
  }
}
