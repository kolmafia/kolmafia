package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class InterestingCoinRequest extends CoinMasterShopRequest {
  public static final String master = "Spend your Interesting Coins";
  public static final String SHOPID = "interesting";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, InterestingCoinRequest.class)
          .withNewShopRowFields(master, SHOPID);
}
