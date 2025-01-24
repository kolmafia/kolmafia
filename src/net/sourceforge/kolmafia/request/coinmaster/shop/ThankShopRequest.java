package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class ThankShopRequest extends CoinMasterRequest {
  public static final String master = "A traveling Thanksgiving salesman";

  private static final Pattern CASHEW_PATTERN = Pattern.compile("([\\d,]+) cashews");
  public static final AdventureResult CASHEW = ItemPool.get(ItemPool.CASHEW, 1);

  public static final CoinmasterData CASHEW_STORE =
      new CoinmasterData(master, "thankshop", ThankShopRequest.class)
          .withToken("cashew")
          .withTokenPattern(CASHEW_PATTERN)
          .withItem(CASHEW)
          .withShopRowFields(master, "thankshop")
          .withNeedsPasswordHash(true);

  public ThankShopRequest() {
    super(CASHEW_STORE);
  }

  public ThankShopRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CASHEW_STORE, buying, attachments);
  }

  public ThankShopRequest(final boolean buying, final AdventureResult attachment) {
    super(CASHEW_STORE, buying, attachment);
  }

  public ThankShopRequest(final boolean buying, final int itemId, final int quantity) {
    super(CASHEW_STORE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=thankshop")) {
      return;
    }

    CoinmasterData data = CASHEW_STORE;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=thankshop")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(CASHEW_STORE, urlString, true);
  }
}
