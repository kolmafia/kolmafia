package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSign;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasToString;

import internal.helpers.Cleanups;
import java.util.EnumSet;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.MaximizerFrame;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class MaximizerIncludeAllTest {
  @BeforeAll
  static void resetCharacter() {
    KoLCharacter.reset("MaximizerIncludeAllTest");
    Preferences.reset("MaximizerIncludeAllTest");
  }

  private static void maximizeAll(String expression) {
    MaximizerFrame.expressionSelect.setSelectedItem(expression);
    Maximizer.maximize(
        EquipScope.SPECULATE_INVENTORY,
        0,
        PriceLevel.DONT_CHECK,
        true,
        EnumSet.allOf(KoLConstants.filterType.class));
  }

  @Test
  void explainsUnavailableGlobalOptions() {
    try (var cleanups =
        new Cleanups(withSign(ZodiacSign.NONE), withProperty("horseryAvailable", false))) {
      maximizeAll("-combat, meat drop, monster level");

      assertThat(
          getBoosts(), hasItem(hasToString(containsString("get a horsery and ride a dark horse"))));
      assertThat(
          getBoosts(),
          hasItem(hasToString(containsString("BoomBox and play Total Eclipse of Your Meat"))));
      assertThat(
          getBoosts(),
          hasItem(hasToString(containsString("ascend into a non-Bad Moon sign and mcd 10"))));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "muscle percent | get an Eight Days a Week Pill Keeper",
        "muscle percent | unlock Boxing Daycare and visit spa for Muddled",
        "all resistance | unlock Spacegate and vaccine 1 for Rainbow Vaccine",
        "initiative | get a Grim Brother familiar for Soles of Glass",
        "item drop | acquire a pair of Cargo Cultist Shorts for Finding Stuff",
        "item drop | acquire Deck of Every Card for Fortune of the Wheel",
        "item drop | install Source Terminal for items.enh",
        "item drop | acquire and equip Greatest American Pants for Super Vision",
        "item drop | install Asdon Martin for Driving Observantly",
        "muscle percent | acquire protonic accelerator pack and crossstreams for Total Protonic Reversal",
        "weapon damage percent | acquire a Beach Comb or a driftwood beach comb for Lack of Body-Building",
        "-combat | get access to the VIP lounge",
        "-combat | acquire a cursed monkey's paw"
      })
  void explainsUnavailableEffectSources(String expression, String explanation) {
    maximizeAll(expression);

    assertThat(getBoosts(), hasItem(hasToString(containsString(explanation))));
  }

  @Test
  void explainsUnavailableSkillSource() {
    try (var cleanups = new Cleanups(withClass(AscensionClass.SEAL_CLUBBER))) {
      maximizeAll("initiative");

      assertThat(
          getBoosts(), hasItem(hasToString(containsString("learn to cast 1 Silent Hunter"))));
    }
  }

  @Test
  void reportsWhenBeneficialEffectHasNoKnownSource() {
    maximizeAll("item drop");

    assertThat(
        getBoosts(), hasItem(hasToString(containsString("no known source of Shadow Waters"))));
  }
}
