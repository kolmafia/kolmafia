package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class WinterGardenRequest extends CoinMasterShopRequest {
  public static final String master = "Winter Gardening";
  public static final String SHOPID = "snowgarden";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, WinterGardenRequest.class)
          .withNewShopRowFields(master, SHOPID);
}
