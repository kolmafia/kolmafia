package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LeprecondoManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("LeprecondoManagerTest");
    Preferences.reset("LeprecondoManagerTest");
  }

  @ParameterizedTest
  @CsvSource(
      value = {
        "booze|''|booze",
        "food|sleep,exercise,mental stimulation|sleep,exercise,mental stimulation,food",
        "food|sleep,food,booze,exercise|food",
        "food|sleep,mental stimulation,food,dumb entertainment,booze,exercise,booze|food",
        "exercise|sleep,mental stimulation,food,dumb entertainment,exercise,booze|sleep,mental stimulation,food,dumb entertainment,exercise,booze",
      },
      delimiter = '|')
  void calculatesNeedOrder(final String need, final String order, final String expected) {
    var cleanups =
        new Cleanups(
            withItem("Leprecondo"),
            withProperty("leprecondoDiscovered"),
            withProperty("leprecondoCurrentNeed", ""),
            withProperty("leprecondoNeedOrder", order),
            withFight(0));
    try (cleanups) {
      LeprecondoManager.processNeedChange(need);
      assertThat("leprecondoNeedOrder", isSetTo(expected));
    }
  }

  @ParameterizedTest
  @CsvSource(value = {"The Marinara Trench,sensory deprivation tank", "An Octopus's Garden,''"})
  void getsUndiscoveredFurnitureForLocation(final String zone, final String expected) {
    var cleanups =
        new Cleanups(withItem("Leprecondo"), withProperty("leprecondoDiscovered", "1,2,3,4,5,6,7"));
    try (cleanups) {
      var furniture = LeprecondoManager.getUndiscoveredFurnitureForLocation(zone);
      assertThat(furniture, equalTo(expected));
    }
  }
}
