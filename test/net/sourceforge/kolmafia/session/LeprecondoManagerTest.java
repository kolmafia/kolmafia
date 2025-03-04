package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withFight;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;

import internal.helpers.Cleanups;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LeprecondoManagerTest {
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
}
