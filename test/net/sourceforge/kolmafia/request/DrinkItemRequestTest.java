package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;

class DrinkItemRequestTest {
  @Test
  public void drinkingVintnerWineResetsVinterCharges() {
    var cleanups =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.VAMPIRE_VINTNER),
            withProperty("vintnerCharge", 13));
    try (cleanups) {
      DrinkItemRequest.parseConsumption(
          ItemPool.get(ItemPool.VAMPIRE_VINTNER_WINE),
          null,
          html("request/test_drink_vintner_wine.html"));
      assertThat("vintnerCharge", isSetTo(0));

      var vintner = new FamiliarData(FamiliarPool.VAMPIRE_VINTNER);
      assertThat(vintner.getCharges(), equalTo(0));
    }
  }
}
