package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.TransferItemRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FDKOLRequest extends CoinMasterRequest {
  public static final String master = "FDKOL Tent";

  public static final AdventureResult FDKOL_TOKEN = ItemPool.get(ItemPool.FDKOL_COMMENDATION, 1);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) FDKOL commendation");

  public static final CoinmasterData FDKOL =
      new CoinmasterData(master, "FDKOL", FDKOLRequest.class)
          .withToken("FDKOL commendation")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(FDKOL_TOKEN)
          .withShopRowFields(master, "fdkol");

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

    Matcher m = TransferItemRequest.ITEMID_PATTERN.matcher(location);
    if (!m.find()) {
      CoinMasterRequest.parseBalance(data, responseText);
      return;
    }

    int itemId = StringUtilities.parseInt(m.group(1));
    AdventureResult item = AdventureResult.findItem(itemId, data.getBuyItems());
    if (item == null) {
      return;
    }

    CoinMasterRequest.parseResponse(data, location, responseText);
  }

  public static String accessible() {
    if (FDKOL_TOKEN.getCount(KoLConstants.inventory) == 0) {
      return "You do not have an FDKOL commendation in inventory";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString, final boolean noMeat) {
    if (urlString.startsWith("inv_use.php") && urlString.contains("whichitem=5707")) {
      // This is a simple visit to the FDKOL Requisitions Tent
      return true;
    }

    // shop.php?pwd&whichshop=fdkol
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=fdkol")) {
      return false;
    }

    Matcher m = TransferItemRequest.ITEMID_PATTERN.matcher(urlString);
    if (!m.find()) {
      // Just a visit
      return true;
    }

    CoinmasterData data = FDKOL;
    int itemId = StringUtilities.parseInt(m.group(1));
    AdventureResult item = AdventureResult.findItem(itemId, data.getBuyItems());
    if (item == null) {
      // Presumably this is a purchase for Meat.
      // If we've already checked Meat, this is an unknown item
      if (noMeat) {
        return false;
      }
      return NPCPurchaseRequest.registerShopRequest(urlString, true);
    }

    return CoinMasterRequest.registerRequest(data, urlString);
  }
}
