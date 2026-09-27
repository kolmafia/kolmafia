package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;

public abstract class ShoreGiftShopRequest extends CoinMasterShopRequest {
  public static final String master = "The Shore, Inc. Gift Shop";
  public static final String SHOPID = "shore";

  private static final Pattern SCRIP_PATTERN = Pattern.compile("(\\d+) Shore Inc. Ship Trip Scrip");
  public static final AdventureResult SHIP_TRIP_SCRIP = ItemPool.get(ItemPool.SHIP_TRIP_SCRIP, 1);

  public static final CoinmasterData SHORE_GIFT_SHOP =
      new CoinmasterData(master, "shore", ShoreGiftShopRequest.class)
          .withToken("Shore Inc. Ship Trip Scrip")
          .withTokenTest("no Shore Inc. Ship Trip Scrip")
          .withTokenPattern(SCRIP_PATTERN)
          .withItem(SHIP_TRIP_SCRIP)
          .withShopRowFields(master, SHOPID)
          .withCanBuyItem(ShoreGiftShopRequest::canBuyItem)
          .withPurchasedItem(ShoreGiftShopRequest::purchasedItem)
          .withVisitShop(ShoreGiftShopRequest::visitShop)
          .withAccessible(ShoreGiftShopRequest::accessible);

  private static Boolean canBuyItem(final Integer itemId) {
    AdventureResult item = ItemPool.get(itemId);
    return switch (itemId) {
      case ItemPool.TOASTER -> !Preferences.getBoolean("itemBoughtPerAscension637");
      case ItemPool.UV_RESISTANT_COMPASS -> !InventoryManager.hasItem(itemId);
      default -> item.getCount(SHORE_GIFT_SHOP.getBuyItems()) > 0;
    };
  }

  private static void purchasedItem(AdventureResult item, Boolean storage) {
    int itemId = item.getItemId();
    switch (itemId) {
      case ItemPool.TOASTER -> Preferences.setBoolean("itemBoughtPerAscension637", true);
    }
  }

  public static void visitShop(final String responseText) {
    Preferences.setBoolean("itemBoughtPerAscension637", !responseText.contains("cheap toaster"));
  }

  public static String accessible() {
    if (!KoLCharacter.desertBeachAccessible()) {
      return "You can't get to the desert beach";
    }
    return null;
  }
}
