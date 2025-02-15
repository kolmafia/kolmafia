package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class Crimbo24BarRequest extends CoinMasterShopRequest {
  public static final String master = "Crimbo24 Bar";
  public static final String SHOPID = "crimbo24_bar";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo24_bar", Crimbo24BarRequest.class)
          .inZone("Crimbo24")
          .withNewShopRowFields(master, SHOPID);
}
