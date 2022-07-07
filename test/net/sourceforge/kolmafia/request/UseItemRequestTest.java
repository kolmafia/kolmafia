package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.isClass;
import static internal.helpers.Player.setupFakeResponse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.Test;

class UseItemRequestTest {

  // We don't use @BeforeEach here because it's specific to milk-related tests.
  private void milkSetup() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("milk user");
    // Reset preferences to defaults.
    KoLCharacter.reset(true);
    // This shouldn't be necessary if reset does what is expected but....
    Preferences.setBoolean("_milkOfMagnesiumUsed", false);
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.MILK_OF_MAGNESIUM));
  }

  private UseItemRequest getUseMilkRequest() {
    return UseItemRequest.getInstance(ItemPool.MILK_OF_MAGNESIUM);
  }

  @Test
  void successfulMilkUsageSetsPreferences() {
    milkSetup();
    assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));

    UseItemRequest req = getUseMilkRequest();
    // Wiki claims that this message is indeed "You stomach ..."
    var cleanups = setupFakeResponse(200, "You stomach immediately begins to churn");
    try (cleanups) {
      req.run();
    }

    assertTrue(Preferences.getBoolean("_milkOfMagnesiumUsed"));
    assertTrue(Preferences.getBoolean("milkOfMagnesiumActive"));
  }

  @Test
  void unsuccessfulMilkUsageSetsPreference() {
    milkSetup();
    // You might have used it outside of KoLmafia.
    assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));

    UseItemRequest req = getUseMilkRequest();
    var cleanups = setupFakeResponse(200, "it was pretty hard on the old gullet.");
    try (cleanups) {
      req.run();
    }

    assertTrue(Preferences.getBoolean("_milkOfMagnesiumUsed"));
  }

  @Test
  void milkPreferencePreventsWastedServerHit() {
    milkSetup();
    Preferences.setBoolean("_milkOfMagnesiumUsed", true);

    UseItemRequest req = getUseMilkRequest();
    req.run();

    assertTrue(Preferences.getBoolean("_milkOfMagnesiumUsed"));
  }

  @Test
  void allConsumablesAreMaxUseOneInGreyYou() {
    var cleanups = new Cleanups(isClass(AscensionClass.GREY_GOO));

    try (cleanups) {
      assertThat(UseItemRequest.maximumUses(ItemPool.GRAPEFRUIT), equalTo(1));
      assertThat(UseItemRequest.maximumUses(ItemPool.COLD_WAD), equalTo(1));
      assertThat(UseItemRequest.maximumUses(ItemPool.MUSCHAT), equalTo(1));
    }
  }

  @Test
  void greyYouGivesWarningOnGcliWhenAlreadyAbsorbed() {
    // Lemon
    var cleanups = new Cleanups(isClass(AscensionClass.GREY_GOO));

    try (cleanups) {
      var req = UseItemRequest.getInstance(332);
      req.responseText = html("request/test_eat_already_absorbed.html");
      req.processResults();

      assertThat(UseItemRequest.lastUpdate, containsString("already absorbed"));
      assertThat(StaticEntity.getContinuationState(), equalTo(KoLConstants.MafiaState.ERROR));
    }
  }

  @Test
  void greyYouGivesNoWarningWhenAbsorbed() {
    // Lemon
    var cleanups = new Cleanups(isClass(AscensionClass.GREY_GOO));

    try (cleanups) {
      var req = UseItemRequest.getInstance(332);
      req.responseText = html("request/test_eat_absorb_lemon.html");
      req.processResults();

      assertThat(UseItemRequest.lastUpdate, equalTo(""));
      assertThat(StaticEntity.getContinuationState(), equalTo(KoLConstants.MafiaState.CONTINUE));
    }
  }
}
