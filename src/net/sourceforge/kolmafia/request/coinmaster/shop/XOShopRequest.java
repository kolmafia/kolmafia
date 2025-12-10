package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class XOShopRequest extends CoinMasterShopRequest {
  public static final String master = "XO Shop";
  public static final String SHOPID = "xo";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, XOShopRequest.class).withNewShopRowFields(master, SHOPID);
}
