package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class BeerGardenRequest extends CoinMasterShopRequest {
  public static final String master = "Beer Garden";
  public static final String SHOPID = "beergarden";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, BeerGardenRequest.class)
          .withNewShopRowFields(master, SHOPID);
}
