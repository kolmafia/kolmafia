package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class FDKOLRequest extends CoinMasterShopRequest {
  public static final String master = "FDKOL Tent";
  public static final String SHOPID = "fdkol";

  public static final AdventureResult FDKOL_TOKEN = ItemPool.get(ItemPool.FDKOL_COMMENDATION, 1);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) FDKOL commendation");

  public static final CoinmasterData FDKOL =
      new CoinmasterData(master, "FDKOL", FDKOLRequest.class)
          .withToken("FDKOL commendation")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(FDKOL_TOKEN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(FDKOLRequest::accessible);

  public static String accessible() {
    if (FDKOL_TOKEN.getCount(KoLConstants.inventory) == 0) {
      return "You do not have an FDKOL commendation in inventory";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (urlString.startsWith("inv_use.php") && urlString.contains("whichitem=5707")) {
      // This is a simple visit to the FDKOL Requisitions Tent
      return true;
    }

    return false;
  }
}
