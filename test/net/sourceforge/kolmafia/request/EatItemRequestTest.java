package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.Test;

class EatItemRequestTest extends RequestTestBase {

  // We don't use @BeforeEach here because it's specific to milk-related tests.
  private void milkSetup() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("milk user");
    // Reset preferences to defaults.
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.MILK_OF_MAGNESIUM));
    // A food item that we can eat.
    AdventureResult.addResultToList(KoLConstants.inventory, ItemPool.get(ItemPool.TOMATO));
  }

  @Test
  void skipMilkNagIfAlreadyUsedToday() {
    milkSetup();
    assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));
    assertTrue(EatItemRequest.askAboutMilk("tomato", 1));
  }

  @Test
  void skipMilkNagIfAlreadyActive() {
    milkSetup();
    Preferences.setBoolean("milkOfMagnesiumActive", true);
    assertTrue(EatItemRequest.askAboutMilk("tomato", 1));
  }

  @Test
  void milkResponseSetsPreference() {
    milkSetup();

    assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));
    Preferences.setBoolean("milkOfMagnesiumActive", true);

    EatItemRequest req = spy(new EatItemRequest(ItemPool.get(ItemPool.TOMATO)));
    // Wiki claims that this message is indeed "You stomach ..."
    expectSuccess(req, "Satisfied, you let loose a nasty magnesium-flavored belch.");
    req.run();

    assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));
    assertFalse(Preferences.getBoolean("milkOfMagnesiumActive"));
  }
}
