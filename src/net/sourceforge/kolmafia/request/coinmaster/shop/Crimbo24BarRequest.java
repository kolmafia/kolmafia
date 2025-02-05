package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;
import net.sourceforge.kolmafia.shop.ShopRow;

public class Crimbo24BarRequest extends CoinMasterRequest {
  public static final String master = "Crimbo24 Bar";
  public static final String SHOPID = "crimbo24_bar";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo24_bar", Crimbo24BarRequest.class)
          .inZone("Crimbo24")
          .withNewShopRowFields(master, SHOPID);

  public Crimbo24BarRequest() {
    super(DATA);
  }

  public Crimbo24BarRequest(final ShopRow row, final int count) {
    super(DATA, row, count);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }
}
