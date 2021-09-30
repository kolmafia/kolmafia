package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class CRIMBCOGiftShopRequest extends CoinMasterRequest {
  public static final String master = "CRIMBCO Gift Shop";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(CRIMBCOGiftShopRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(CRIMBCOGiftShopRequest.master);

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("You have <b>([\\d,]+)</b> CRIMBCO scrip");
  public static final AdventureResult CRIMBCO_SCRIP = ItemPool.get(ItemPool.CRIMBCO_SCRIP, 1);
  public static final CoinmasterData CRIMBCO_GIFT_SHOP =
      new CoinmasterData(
          CRIMBCOGiftShopRequest.master,
          "CRIMBCO",
          CRIMBCOGiftShopRequest.class,
          "CRIMBCO scrip",
          "You don't have any CRIMBCO scrip",
          false,
          CRIMBCOGiftShopRequest.TOKEN_PATTERN,
          CRIMBCOGiftShopRequest.CRIMBCO_SCRIP,
          null,
          null,
          "crimbo10.php",
          "buygift",
          CRIMBCOGiftShopRequest.buyItems,
          CRIMBCOGiftShopRequest.buyPrices,
          null,
          null,
          null,
          null,
          "whichitem",
          GenericRequest.WHICHITEM_PATTERN,
          "howmany",
          GenericRequest.HOWMANY_PATTERN,
          null,
          null,
          true);

  public CRIMBCOGiftShopRequest() {
    super(CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP);
  }

  public CRIMBCOGiftShopRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP, buying, attachments);
  }

  public CRIMBCOGiftShopRequest(final boolean buying, final AdventureResult attachment) {
    super(CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP, buying, attachment);
  }

  public CRIMBCOGiftShopRequest(final boolean buying, final int itemId, final int quantity) {
    super(CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    CRIMBCOGiftShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.startsWith("crimbo10.php")) {
      return;
    }

    CoinmasterData data = CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP;
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

    CoinmasterData data = CRIMBCOGiftShopRequest.CRIMBCO_GIFT_SHOP;
    return CoinMasterRequest.registerRequest(data, urlString);
  }

  public static String accessible() {
    return "The CRIMBCO Gift Shop is not available";
  }
}
