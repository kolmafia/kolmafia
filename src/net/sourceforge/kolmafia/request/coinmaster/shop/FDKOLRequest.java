package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class FDKOLRequest extends CoinMasterRequest {
  public static final String master = "FDKOL Tent";

  public static final AdventureResult FDKOL_TOKEN = ItemPool.get(ItemPool.FDKOL_COMMENDATION, 1);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) FDKOL commendation");

  public static final CoinmasterData FDKOL =
      new CoinmasterData(master, "FDKOL", FDKOLRequest.class)
          .withToken("FDKOL commendation")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(FDKOL_TOKEN)
          .withShopRowFields(master, "fdkol")
          .withAccessible(FDKOLRequest::accessible);

  public FDKOLRequest() {
    super(FDKOL);
  }

  public FDKOLRequest(final boolean buying, final AdventureResult[] attachments) {
    super(FDKOL, buying, attachments);
  }

  public FDKOLRequest(final boolean buying, final AdventureResult attachment) {
    super(FDKOL, buying, attachment);
  }

  public FDKOLRequest(final boolean buying, final int itemId, final int quantity) {
    super(FDKOL, buying, itemId, quantity);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.startsWith("shop.php") || !location.contains("whichshop=fdkol")) {
      return;
    }

    CoinmasterData data = FDKOL;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

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
