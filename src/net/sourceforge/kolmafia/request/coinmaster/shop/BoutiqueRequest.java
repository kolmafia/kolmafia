package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class BoutiqueRequest extends CoinMasterShopRequest {
  public static final String master = "Paul's Boutique";
  public static final String SHOPID = "cindy";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) odd silver coin");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.ODD_SILVER_COIN, 1);

  public static final CoinmasterData BOUTIQUE =
      new CoinmasterData(master, "boutique", BoutiqueRequest.class)
          .withToken("odd silver coin")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(BoutiqueRequest::accessible);

  public static String accessible() {
    int coins = COIN.getCount(KoLConstants.inventory);
    if (coins == 0) {
      return "You don't have an odd silver coin.";
    }
    return null;
  }
}
