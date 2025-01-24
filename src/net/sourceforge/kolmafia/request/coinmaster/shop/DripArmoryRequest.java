package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class DripArmoryRequest extends CoinMasterRequest {
  public static final String master = "Drip Institute Armory";

  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.DRIPLET, 1);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Driplet");

  public static final CoinmasterData DRIP_ARMORY =
      new CoinmasterData(master, "driparmory", DripArmoryRequest.class)
          .withToken("Driplet")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, "driparmory")
          .withCanBuyItem(DripArmoryRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    AdventureResult item = ItemPool.get(itemId);
    return switch (itemId) {
      case ItemPool.DRIPPY_SHIELD -> Preferences.getBoolean("drippyShieldUnlocked")
          && !InventoryManager.hasItem(item);
      default -> item.getCount(DRIP_ARMORY.getBuyItems()) > 0;
    };
  }

  public DripArmoryRequest() {
    super(DRIP_ARMORY);
  }

  public DripArmoryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DRIP_ARMORY, buying, attachments);
  }

  public DripArmoryRequest(final boolean buying, final AdventureResult attachment) {
    super(DRIP_ARMORY, buying, attachment);
  }

  public DripArmoryRequest(final boolean buying, final int itemId, final int quantity) {
    super(DRIP_ARMORY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=driparmory")) {
      return;
    }

    // Check for item unlocks
    if (responseText.contains("drippy shield")) {
      Preferences.setBoolean("drippyShieldUnlocked", true);
    }

    CoinmasterData data = DRIP_ARMORY;
    int itemId = CoinMasterRequest.extractItemId(data, location);

    if (itemId == -1) {
      // Purchase for Meat or a simple visit
      CoinMasterRequest.parseBalance(data, responseText);
      return;
    }

    CoinMasterRequest.parseResponse(data, location, responseText);
  }

  public static String accessible() {
    return null;
  }

  public static final boolean registerRequest(final String urlString, final boolean noMeat) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=driparmory")) {
      return false;
    }

    Matcher m = GenericRequest.WHICHROW_PATTERN.matcher(urlString);
    if (!m.find()) {
      // Just a visit
      return true;
    }

    CoinmasterData data = DRIP_ARMORY;
    int itemId = CoinMasterRequest.extractItemId(data, urlString);

    if (itemId == -1) {
      // Presumably this is a purchase for Meat.
      // If we've already checked Meat, this is an unknown item
      if (noMeat) {
        return false;
      }
      return NPCPurchaseRequest.registerShopRequest(urlString, true);
    }

    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
