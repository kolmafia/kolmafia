package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.addItem;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EatItemRequestTest {
  @BeforeEach
  private void beforeEach() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("EatItemRequest");
    Preferences.reset("EatItemRequest");
  }

  @Nested
  class MilkOfMagnesium extends RequestTestBase {
    @BeforeEach
    private void milkSetup() {
      addItem(ItemPool.MILK_OF_MAGNESIUM);
      addItem(ItemPool.TOMATO);
    }

    @Test
    void skipMilkNagIfAlreadyUsedToday() {
      assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));
      assertTrue(EatItemRequest.askAboutMilk("tomato", 1));
    }

    @Test
    void skipMilkNagIfAlreadyActive() {
      Preferences.setBoolean("milkOfMagnesiumActive", true);
      assertTrue(EatItemRequest.askAboutMilk("tomato", 1));
    }

    @Test
    void milkResponseSetsPreference() {
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
}
