package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;

public abstract class JarlsbergRequest extends CoinMasterShopRequest {
  public static final String master = "Jarlsberg's Cosmic Kitchen";
  public static final String SHOPID = "jarl";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, JarlsbergRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(JarlsbergRequest::accessible)
          .withCanBuyItem(JarlsbergRequest::canBuyItem)
          .withPurchasedItem(JarlsbergRequest::purchasedItem);

  public static String accessible() {
    if (!KoLCharacter.isJarlsberg()) {
      return "You are not an Avatar of Jarlsberg";
    }
    return null;
  }

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.CONSUMMATE_BROWNIE,
          ItemPool.CONSUMMATE_MELTED_CHEESE,
          ItemPool.CONSUMMATE_MEATLOAF,
          ItemPool.CONSUMMATE_BAKED_POTATO ->
          KoLCharacter.hasSkill(SkillPool.BAKE);
      case ItemPool.CONSUMMATE_EGG_SALAD,
          ItemPool.CONSUMMATE_SALSA,
          ItemPool.CONSUMMATE_FRANKFURTER,
          ItemPool.CONSUMMATE_WHIPPED_CREAM ->
          KoLCharacter.hasSkill(SkillPool.BLEND);
      case ItemPool.CONSUMMATE_HARD_BOILED_EGG,
          ItemPool.CONSUMMATE_BAGEL,
          ItemPool.CONSUMMATE_SOUP ->
          KoLCharacter.hasSkill(SkillPool.BOIL);
      case ItemPool.CONSUMMATE_HOT_DOG_BUN, ItemPool.CONSUMMATE_SALAD ->
          KoLCharacter.hasSkill(SkillPool.CHOP);
      case ItemPool.PASSABLE_STOUT,
          ItemPool.CONSUMMATE_SAUERKRAUT,
          ItemPool.ACCEPTABLE_VODKA,
          ItemPool.CONSUMMATE_SOUR_CREAM,
          ItemPool.ADEQUATE_RUM ->
          KoLCharacter.hasSkill(SkillPool.CURDLE);
      case ItemPool.CONSUMMATE_COLD_CUTS,
          ItemPool.CONSUMMATE_ICE_CREAM,
          ItemPool.CONSUMMATE_SORBET ->
          KoLCharacter.hasSkill(SkillPool.FREEZE);
      case ItemPool.CONSUMMATE_FRIED_EGG,
          ItemPool.CONSUMMATE_BACON,
          ItemPool.CONSUMMATE_FRENCH_FRIES ->
          KoLCharacter.hasSkill(SkillPool.FRY);
      case ItemPool.CONSUMMATE_TOAST, ItemPool.CONSUMMATE_STEAK ->
          KoLCharacter.hasSkill(SkillPool.GRILL);
      case ItemPool.CONSUMMATE_SLICED_BREAD,
          ItemPool.CONSUMMATE_CORN_CHIPS,
          ItemPool.CONSUMMATE_CHEESE_SLICE,
          ItemPool.CONSUMMATE_STRAWBERRIES ->
          KoLCharacter.hasSkill(SkillPool.SLICE);
      case ItemPool.COSMIC_SIX_PACK -> !Preferences.getBoolean("_cosmicSixPackConjured");
      case ItemPool.STAFF_OF_BREAKFAST,
          ItemPool.STAFF_OF_LIFE,
          ItemPool.STAFF_OF_LUNCH,
          ItemPool.STAFF_OF_CHEESE,
          ItemPool.STAFF_OF_DINNER,
          ItemPool.STAFF_OF_STEAK,
          ItemPool.STAFF_OF_FRUIT,
          ItemPool.STAFF_OF_CREAM ->
          !InventoryManager.equippedOrInInventory(itemId);
      default -> DATA.availableItem(itemId);
    };
  }

  public static void purchasedItem(final AdventureResult item, final Boolean storage) {
    // Purchasing certain items makes them unavailable
    if (item.getItemId() == ItemPool.COSMIC_SIX_PACK) {
      Preferences.setBoolean("_cosmicSixPackConjured", true);
    }
  }

  public static boolean isJarlsbergian(final int itemId) {
    return DATA.availableItem(itemId) || itemId == ItemPool.MEDIOCRE_LAGER;
  }
}
