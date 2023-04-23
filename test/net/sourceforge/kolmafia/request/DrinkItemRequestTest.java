package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DrinkItemRequestTest {
  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("DrinkItemRequest");
  }

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

  @Nested
  class GetsYouDrunk {
    @Test
    public void tracksSuccessfulConsumption() {
      var cleanups =
          new Cleanups(withProperty("getsYouDrunkTurnsLeft"), withItem(ItemPool.GETS_YOU_DRUNK));
      try (cleanups) {
        var req = new DrinkItemRequest(ItemPool.get(ItemPool.GETS_YOU_DRUNK));
        req.responseText = html("request/test_drink_gets_you_drunk_success.html");
        req.processResults();
        assertThat("getsYouDrunkTurnsLeft", isSetTo(4));
        assertThat(InventoryManager.getCount(ItemPool.GETS_YOU_DRUNK), is(0));
      }
    }

    @Test
    public void tracksUnsuccessfulConsumption() {
      var cleanups =
          new Cleanups(withProperty("getsYouDrunkTurnsLeft", 3), withItem(ItemPool.GETS_YOU_DRUNK));
      try (cleanups) {
        var req = new DrinkItemRequest(ItemPool.get(ItemPool.GETS_YOU_DRUNK));
        req.responseText = html("request/test_drink_gets_you_drunk_failure.html");
        req.processResults();
        assertThat("getsYouDrunkTurnsLeft", isSetTo(3));
        assertThat(InventoryManager.getCount(ItemPool.GETS_YOU_DRUNK), is(1));
      }
    }

    @Test
    public void guessesOldTimer() {
      var cleanups = withProperty("getsYouDrunkTurnsLeft", 0);
      try (cleanups) {
        var req = new DrinkItemRequest(ItemPool.get(ItemPool.GETS_YOU_DRUNK));
        req.responseText = html("request/test_drink_gets_you_drunk_failure.html");
        req.processResults();
        assertThat("getsYouDrunkTurnsLeft", isSetTo(4));
      }
    }

    @Test
    public void maximumUsesOneNormally() {
      var cleanups = withProperty("getsYouDrunkTurnsLeft", 0);
      try (cleanups) {
        var uses = DrinkItemRequest.maximumUses(ItemPool.GETS_YOU_DRUNK);
        assertThat(uses, is(1));
      }
    }

    @Test
    public void maximumUsesZeroWhenTimerGoing() {
      var cleanups = withProperty("getsYouDrunkTurnsLeft", 1);
      try (cleanups) {
        var uses = DrinkItemRequest.maximumUses(ItemPool.GETS_YOU_DRUNK);
        assertThat(uses, is(0));
      }
    }
  }
}
