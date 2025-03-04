package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class WarbearBoxRequest extends CoinMasterShopRequest {
  public static final String master = "Warbear Black Box";
  public static final String SHOPID = "warbear";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) warbear whosit");
  public static final AdventureResult WHOSIT = ItemPool.get(ItemPool.WARBEAR_WHOSIT, 1);
  public static final AdventureResult BLACKBOX = ItemPool.get(ItemPool.WARBEAR_BLACK_BOX, 1);

  public static final CoinmasterData WARBEARBOX =
      new CoinmasterData(master, "warbear", WarbearBoxRequest.class)
          .withToken("warbear whosit")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(WHOSIT)
          .withShopRowFields(master, SHOPID)
          .withAccessible(WarbearBoxRequest::accessible);

  public static String accessible() {
    int wand = BLACKBOX.getCount(KoLConstants.inventory);
    if (wand == 0) {
      return "You don't have a warbear black box";
    }
    return null;
  }
}
