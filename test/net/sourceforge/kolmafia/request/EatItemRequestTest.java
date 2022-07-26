package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Player;
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
  class MilkOfMagnesium {
    @BeforeEach
    private void milkSetup() {
      Player.withItem(ItemPool.MILK_OF_MAGNESIUM);
      Player.withItem(ItemPool.TOMATO);
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

      var req = new EatItemRequest(ItemPool.get(ItemPool.TOMATO));
      req.responseText = "Satisfied, you let loose a nasty magnesium-flavored belch.";
      req.processResults();

      assertFalse(Preferences.getBoolean("_milkOfMagnesiumUsed"));
      assertFalse(Preferences.getBoolean("milkOfMagnesiumActive"));
    }
  }
}
