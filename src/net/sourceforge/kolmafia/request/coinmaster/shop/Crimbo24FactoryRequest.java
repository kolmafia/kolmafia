package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRow;

public class Crimbo24FactoryRequest extends CoinMasterRequest {
  public static final String master = "Crimbo24 Factory";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo24_factory", Crimbo24FactoryRequest.class)
          .inZone("Crimbo24")
          .withNewShopRowFields(master, "crimbo24_factory")
          .withNeedsPasswordHash(true);

  public Crimbo24FactoryRequest() {
    super(DATA);
  }

  public Crimbo24FactoryRequest(final ShopRow row, final int count) {
    super(DATA, row, count);
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

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php")
        || !urlString.contains("whichshop=" + DATA.getNickname())) {
      return false;
    }

    return CoinMasterRequest.registerRequest(DATA, urlString, true);
  }
}
