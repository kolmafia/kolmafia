package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class SpantRequest extends CoinMasterShopRequest {
  public static final String master = "Spant Bit Assembly";
  public static final String SHOPID = "spant";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, SpantRequest.class).withNewShopRowFields(master, SHOPID);
}
