package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class SliemceRequest extends CoinMasterShopRequest {
  public static final String master = "Mad Sliemce";
  public static final String SHOPID = "voteslime";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, SliemceRequest.class).withNewShopRowFields(master, SHOPID);
}
