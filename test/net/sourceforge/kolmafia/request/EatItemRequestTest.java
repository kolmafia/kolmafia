package net.sourceforge.kolmafia.request;

import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class EatItemRequestTest {
  @BeforeEach
  public void beforeEach() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("EatItemRequest");
    Preferences.reset("EatItemRequest");
  }

  @Nested
  class MilkOfMagnesium {
    private Cleanups cleanups = new Cleanups();

    @BeforeEach
    public void beforeEach() {
      cleanups.add(withItem(ItemPool.MILK_OF_MAGNESIUM));
      cleanups.add(withItem(ItemPool.TOMATO));
    }

    @AfterEach
    public void afterEach() {
      cleanups.close();
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

  @ParameterizedTest
  @CsvSource({
    "10991, pizzaOfLegendEaten",
    "10992, calzoneOfLegendEaten",
    "11000, deepDishOfLegendEaten"
  })
  public void canTrackCookbookbatFoodsSuccess(Integer itemId, String prefname) {
    var cleanups = withProperty(prefname, false);
    try (cleanups) {
      assertFalse(Preferences.getBoolean(prefname));
      var req = new EatItemRequest(ItemPool.get(itemId));
      req.responseText = "";
      req.processResults();
      assertTrue(Preferences.getBoolean(prefname));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "10991, pizzaOfLegendEaten",
    "10992, calzoneOfLegendEaten",
    "11000, deepDishOfLegendEaten"
  })
  public void canTrackCookbookbatFoodsFailure(Integer itemId, String prefname) {
    var cleanups = withProperty(prefname, false);
    try (cleanups) {
      assertFalse(Preferences.getBoolean(prefname));
      var req = new EatItemRequest(ItemPool.get(itemId));
      req.responseText = "You may only eat one of those per lifetime";
      req.processResults();
      assertTrue(Preferences.getBoolean(prefname));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "10991, pizzaOfLegendEaten",
    "10992, calzoneOfLegendEaten",
    "11000, deepDishOfLegendEaten"
  })
  public void canPredictCookbookbatFoodsLimit(Integer itemId, String prefname) {
    var cleanups = withProperty(prefname, false);
    try (cleanups) {
      assertEquals(1, EatItemRequest.maximumUses(itemId));
      Preferences.setBoolean(prefname, true);
      assertEquals(0, EatItemRequest.maximumUses(itemId));
    }
  }
}
