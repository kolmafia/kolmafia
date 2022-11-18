package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;

public class ShoreGiftShopRequest extends CoinMasterRequest {
  public static final String master = "The Shore, Inc. Gift Shop";

  private static final Pattern SCRIP_PATTERN = Pattern.compile("(\\d+) Shore Inc. Ship Trip Scrip");
  public static final AdventureResult SHIP_TRIP_SCRIP = ItemPool.get(ItemPool.SHIP_TRIP_SCRIP, 1);

  public static final CoinmasterData SHORE_GIFT_SHOP =
      new CoinmasterData(master, "shore", ShoreGiftShopRequest.class)
          .withToken("Shore Inc. Ship Trip Scrip")
          .withTokenTest("no Shore Inc. Ship Trip Scrip")
          .withTokenPattern(SCRIP_PATTERN)
          .withItem(SHIP_TRIP_SCRIP)
          .withShopRowFields(master, "shore")
          .withNeedsPasswordHash(true)
          .withCanBuyItem(ShoreGiftShopRequest::canBuyItem)
          .withPurchasedItem(ShoreGiftShopRequest::purchasedItem);

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
      case ItemPool.TOASTER:
        Preferences.setBoolean("itemBoughtPerAscension637", true);
        break;
    }
  }

  public ShoreGiftShopRequest() {
    super(SHORE_GIFT_SHOP);
  }

  public ShoreGiftShopRequest(final boolean buying, final AdventureResult[] attachments) {
    super(SHORE_GIFT_SHOP, buying, attachments);
  }

  public ShoreGiftShopRequest(final boolean buying, final AdventureResult attachment) {
    super(SHORE_GIFT_SHOP, buying, attachment);
  }

  public ShoreGiftShopRequest(final boolean buying, final int itemId, final int quantity) {
    super(SHORE_GIFT_SHOP, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=shore")) {
      return;
    }

    CoinmasterData data = SHORE_GIFT_SHOP;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    Preferences.setBoolean("itemBoughtPerAscension637", !responseText.contains("cheap toaster"));

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (!KoLCharacter.desertBeachAccessible()) {
      return "You can't get to the desert beach";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=shore")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(SHORE_GIFT_SHOP, urlString, true);
  }
}
