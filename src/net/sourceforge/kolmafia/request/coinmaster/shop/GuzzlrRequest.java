package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;

public abstract class GuzzlrRequest extends CoinMasterShopRequest {
  public static final String master = "Guzzlr Company Store Website";
  public static final String SHOPID = "guzzlr";

  private static final Pattern GUZZLR_PATTERN = Pattern.compile("([\\d,]+) Guzzlrbuck");
  public static final AdventureResult GUZZLRBUCK = ItemPool.get(ItemPool.GUZZLRBUCK, 1);

  public static final CoinmasterData GUZZLR =
      new CoinmasterData(master, "guzzlr", GuzzlrRequest.class)
          .withToken("Guzzlrbuck")
          .withTokenPattern(GUZZLR_PATTERN)
          .withItem(GUZZLRBUCK)
          .withShopRowFields(master, SHOPID)
          .withAccessible(GuzzlrRequest::accessible);

  public static String accessible() {
    if (InventoryManager.getAccessibleCount(GUZZLRBUCK) > 0) {
      return null;
    }
    return "You have no Guzzlrbucks to spend";
  }
}
