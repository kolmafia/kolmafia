package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;

public class DripArmoryRequest extends CoinMasterRequest {
  public static final String master = "Drip Institute Armory";
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.DRIPLET, 1);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Driplet");
  public static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(DripArmoryRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(DripArmoryRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(DripArmoryRequest.master);

  public static final CoinmasterData DRIP_ARMORY =
      new CoinmasterData(
          DripArmoryRequest.master,
          "driparmory",
          DripArmoryRequest.class,
          "Driplet",
          null,
          false,
          DripArmoryRequest.TOKEN_PATTERN,
          DripArmoryRequest.TOKEN,
          null,
          DripArmoryRequest.itemRows,
          "shop.php?whichshop=driparmory",
          "buyitem",
          DripArmoryRequest.buyItems,
          DripArmoryRequest.buyPrices,
          null,
          null,
          null,
          null,
          "whichrow",
          GenericRequest.WHICHROW_PATTERN,
          "quantity",
          GenericRequest.QUANTITY_PATTERN,
          null,
          null,
          true) {
        @Override
        public final boolean canBuyItem(final int itemId) {
          switch (itemId) {
            case ItemPool.DRIPPY_SHIELD:
              return Preferences.getBoolean("drippyShieldUnlocked")
                  && !InventoryManager.hasItem(ItemPool.DRIPPY_SHIELD);
          }
          return super.canBuyItem(itemId);
        }
      };

  public DripArmoryRequest() {
    super(DripArmoryRequest.DRIP_ARMORY);
  }

  public DripArmoryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DripArmoryRequest.DRIP_ARMORY, buying, attachments);
  }

  public DripArmoryRequest(final boolean buying, final AdventureResult attachment) {
    super(DripArmoryRequest.DRIP_ARMORY, buying, attachment);
  }

  public DripArmoryRequest(final boolean buying, final int itemId, final int quantity) {
    super(DripArmoryRequest.DRIP_ARMORY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    DripArmoryRequest.parseResponse(this.getURLString(), responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=driparmory")) {
      return;
    }

    // Check for item unlocks
    if (responseText.contains("drippy shield")) {
      Preferences.setBoolean("drippyShieldUnlocked", true);
    }

    CoinmasterData data = DripArmoryRequest.DRIP_ARMORY;
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

    CoinmasterData data = DripArmoryRequest.DRIP_ARMORY;
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
