package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class FDKOLRequest extends CoinMasterRequest {
  public static final String master = "FDKOL Tent";
  public static final AdventureResult FDKOL_TOKEN = ItemPool.get(ItemPool.FDKOL_COMMENDATION, 1);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) FDKOL commendation");
  public static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(FDKOLRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(FDKOLRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(FDKOLRequest.master);

  public static final CoinmasterData FDKOL =
      new CoinmasterData(
          FDKOLRequest.master,
          "FDKOL",
          FDKOLRequest.class,
          "FDKOL commendation",
          null,
          false,
          FDKOLRequest.TOKEN_PATTERN,
          FDKOLRequest.FDKOL_TOKEN,
          null,
          FDKOLRequest.itemRows,
          "shop.php?whichshop=fdkol",
          "buyitem",
          FDKOLRequest.buyItems,
          FDKOLRequest.buyPrices,
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
          true);

  public FDKOLRequest() {
    super(FDKOLRequest.FDKOL);
  }

  public FDKOLRequest(final boolean buying, final AdventureResult[] attachments) {
    super(FDKOLRequest.FDKOL, buying, attachments);
  }

  public FDKOLRequest(final boolean buying, final AdventureResult attachment) {
    super(FDKOLRequest.FDKOL, buying, attachment);
  }

  public FDKOLRequest(final boolean buying, final int itemId, final int quantity) {
    super(FDKOLRequest.FDKOL, buying, itemId, quantity);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.startsWith("shop.php") || !location.contains("whichshop=fdkol")) {
      return;
    }

    CoinmasterData data = FDKOLRequest.FDKOL;

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
    if (FDKOLRequest.FDKOL_TOKEN.getCount(KoLConstants.inventory) == 0) {
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

    CoinmasterData data = FDKOLRequest.FDKOL;
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
