package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;
import net.sourceforge.kolmafia.shop.ShopRow;

public class Crimbo24FactoryRequest extends CoinMasterRequest {
  public static final String master = "Crimbo24 Factory";
  public static final String SHOPID = "crimbo24_factory";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo24_factory", Crimbo24FactoryRequest.class)
          .inZone("Crimbo24")
          .withNewShopRowFields(master, SHOPID);

  public Crimbo24FactoryRequest() {
    super(DATA);
  }

  public Crimbo24FactoryRequest(final ShopRow row, final int count) {
    super(DATA, row, count);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }
}
