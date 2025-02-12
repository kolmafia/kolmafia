package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class PrecinctRequest extends CoinMasterShopRequest {
  public static final String master = "Precinct Materiel Division";
  public static final String SHOPID = "detective";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) cop dollar");
  public static final AdventureResult DOLLAR = ItemPool.get(ItemPool.COP_DOLLAR, 1);

  public static final CoinmasterData PRECINCT =
      new CoinmasterData(master, "Precinct Materiel Division", PrecinctRequest.class)
          .withToken("cop dollar")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(DOLLAR)
          .withShopRowFields(master, SHOPID)
          .withAccessible(PrecinctRequest::accessible);

  public static String accessible() {
    // *** Finish this.
    return null;
  }
}
