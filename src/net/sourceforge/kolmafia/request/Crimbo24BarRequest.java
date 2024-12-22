package net.sourceforge.kolmafia.request;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;

public class Crimbo24BarRequest extends CoinMasterRequest {
  public static final String master = "Crimbo24 Bar";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo24_bar", Crimbo24BarRequest.class)
          .withNewShopRowFields(master, "crimbo24_bar")
          .withNeedsPasswordHash(true);

  public Crimbo24BarRequest() {
    super(DATA);
  }

  public Crimbo24BarRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DATA, buying, attachments);
  }

  public Crimbo24BarRequest(final boolean buying, final AdventureResult attachment) {
    super(DATA, buying, attachment);
  }

  public Crimbo24BarRequest(final boolean buying, final int itemId, final int quantity) {
    super(DATA, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=" + DATA.getNickname())) {
      return;
    }

    CoinmasterData data = DATA;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    return "";
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php")
        || !urlString.contains("whichshop=" + DATA.getNickname())) {
      return false;
    }

    return CoinMasterRequest.registerRequest(DATA, urlString, true);
  }
}
