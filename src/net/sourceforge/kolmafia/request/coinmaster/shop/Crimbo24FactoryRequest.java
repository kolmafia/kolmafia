package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class Crimbo24FactoryRequest extends CoinMasterShopRequest {
  public static final String master = "Crimbo24 Factory";
  public static final String SHOPID = "crimbo24_factory";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo24_factory", Crimbo24FactoryRequest.class)
          .inZone("Crimbo24")
          .withNewShopRowFields(master, SHOPID);
}
