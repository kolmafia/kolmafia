package net.sourceforge.kolmafia.swingui.panel;

import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withLevel;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withSubStats;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import org.junit.jupiter.api.Test;

class CompactSidePaneTest {
  @Test
  void levelProgressBarIsShown() {
    var cleanups =
        new Cleanups(
            withClass(AscensionClass.SEAL_CLUBBER), withLevel(3), withSubStats(116, 100, 100));

    try (cleanups) {
      var pane = new CompactSidePane();
      pane.run();
      assertThat(pane.levelMeter.isVisible(), is(true));
      assertThat(pane.levelMeter.getPercentComplete(), closeTo(0.495, 0.001));
    }
  }

  @Test
  void levelProgressBarIsNotShownInZootomist() {
    var cleanups =
        new Cleanups(
            withPath(AscensionPath.Path.Z_IS_FOR_ZOOTOMIST),
            withLevel(3),
            withSubStats(116, 100, 100));

    try (cleanups) {
      var pane = new CompactSidePane();
      pane.run();
      assertThat(pane.levelMeter.isVisible(), is(false));
    }
  }

  @Test
  void rolloverAdventuresAndFightsAreNotShownAsBonuses() {
    var cleanups = new Cleanups(withClass(AscensionClass.SEAL_CLUBBER));

    try (cleanups) {
      KoLCharacter.recalculateAdjustments();
      var text = CompactSidePane.modifierPopupText();
      assertThat(text, containsString("Adv 40<br>PvP 10<br>"));
    }
  }

  @Test
  void criticalHitChanceIsNotShownAsABonus() {
    var cleanups = new Cleanups(withClass(AscensionClass.SEAL_CLUBBER));

    try (cleanups) {
      KoLCharacter.recalculateAdjustments();
      var text = CompactSidePane.modifierPopupText();
      assertThat(text, containsString("<td>Critical</td><td>9%"));
    }
  }
}
