package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class CRIMBCOGiftShopRequest extends CoinMasterRequest {
  public static final String master = "CRIMBCO Gift Shop";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("You have <b>([\\d,]+)</b> CRIMBCO scrip");
  public static final AdventureResult CRIMBCO_SCRIP = ItemPool.get(ItemPool.CRIMBCO_SCRIP, 1);

  public static final CoinmasterData CRIMBCO_GIFT_SHOP =
      new CoinmasterData(master, "CRIMBCO", CRIMBCOGiftShopRequest.class)
          .withToken("CRIMBCO scrip")
          .withTokenTest("You don't have any CRIMBCO scrip")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(CRIMBCO_SCRIP)
          .withBuyURL("crimbo10.php")
          .withBuyAction("buygift")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withItemField("whichitem")
          .withItemPattern(GenericRequest.WHICHITEM_PATTERN)
          .withCountField("quantity")
          .withCountPattern(GenericRequest.QUANTITY_PATTERN);

  public CRIMBCOGiftShopRequest() {
    super(CRIMBCO_GIFT_SHOP);
  }

  public CRIMBCOGiftShopRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CRIMBCO_GIFT_SHOP, buying, attachments);
  }

  public CRIMBCOGiftShopRequest(final boolean buying, final AdventureResult attachment) {
    super(CRIMBCO_GIFT_SHOP, buying, attachment);
  }

  public CRIMBCOGiftShopRequest(final boolean buying, final int itemId, final int quantity) {
    super(CRIMBCO_GIFT_SHOP, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.startsWith("crimbo10.php")) {
      return;
    }

    CoinmasterData data = CRIMBCO_GIFT_SHOP;
    String action = GenericRequest.getAction(location);
    if (action == null) {
      if (location.indexOf("place=giftshop") != -1) {
        // Parse current coin balances
        CoinMasterRequest.parseBalance(data, responseText);
      }

      return;
    }

    CoinMasterRequest.parseResponse(data, location, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    // We only claim crimbo10.php?action=buygift
    if (!urlString.startsWith("crimbo10.php")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(CRIMBCO_GIFT_SHOP, urlString);
  }

  public static String accessible() {
    return "The CRIMBCO Gift Shop is not available";
  }
}
