package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class DripArmoryRequest extends CoinMasterRequest {
  public static final String master = "Drip Institute Armory";
  public static final String SHOPID = "driparmory";

  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.DRIPLET, 1);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Driplet");

  public static final CoinmasterData DRIP_ARMORY =
      new CoinmasterData(master, "driparmory", DripArmoryRequest.class)
          .withToken("Driplet")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, SHOPID)
          .withCanBuyItem(DripArmoryRequest::canBuyItem)
          .withVisitShop(DripArmoryRequest::visitShop);

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

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), responseText);
  }

  public static void visitShop(String responseText) {
    if (responseText.contains("drippy shield")) {
      Preferences.setBoolean("drippyShieldUnlocked", true);
    }
  }
}
