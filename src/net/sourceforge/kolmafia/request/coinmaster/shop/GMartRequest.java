package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class GMartRequest extends CoinMasterShopRequest {
  public static final String master = "G-Mart";
  public static final String SHOPID = "glover";

  private static final Pattern G_PATTERN = Pattern.compile("([\\d,]+) G");
  public static final AdventureResult G = ItemPool.get(ItemPool.G, 1);

  public static final CoinmasterData GMART =
      new CoinmasterData(master, "glover", GMartRequest.class)
          .withToken("G")
          .withTokenTest("no Gs")
          .withTokenPattern(G_PATTERN)
          .withItem(G)
          .withShopRowFields(master, SHOPID)
          .withAccessible(GMartRequest::accessible);

  public static String accessible() {
    // *** Finish this.
    return null;
  }
}
