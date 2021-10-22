package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.Test;

class UseItemRequestTest extends RequestTestBase {

  // We don't use @BeforeEach here because it's specific to milk-related tests.
  private void milkSetup() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("milk user");
    // Reset preferences to defaults.
    KoLCharacter.reset(true);
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.MILK_OF_MAGNESIUM));
  }

  private UseItemRequest getUseMilkRequest() {
    return spy(UseItemRequest.getInstance(ItemPool.MILK_OF_MAGNESIUM));
  }

  @Test
  void successfulMilkUsageSetsPreferences() {
    milkSetup();
    assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));

    UseItemRequest req = getUseMilkRequest();
    // Wiki claims that this message is indeed "You stomach ..."
    expectSuccess(req, "You stomach immediately begins to churn");
    req.run();

    assertTrue(Preferences.getBoolean("_milkOfMagnesiumUsed"));
    assertTrue(Preferences.getBoolean("milkOfMagnesiumActive"));
  }

  @Test
  void unsuccessfulMilkUsageSetsPreference() {
    milkSetup();
    // You might have used it outside of KoLmafia.
    assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));

    UseItemRequest req = getUseMilkRequest();
    expectSuccess(req, "it was pretty hard on the old gullet.");
    req.run();

    assertTrue(Preferences.getBoolean("_milkOfMagnesiumUsed"));
  }

  @Test
  void milkPreferencePreventsWastedServerHit() {
    milkSetup();
    Preferences.setBoolean("_milkOfMagnesiumUsed", true);

    UseItemRequest req = getUseMilkRequest();
    req.run();

    verify(req, never()).externalExecute();
    assertTrue(Preferences.getBoolean("_milkOfMagnesiumUsed"));
  }
}
