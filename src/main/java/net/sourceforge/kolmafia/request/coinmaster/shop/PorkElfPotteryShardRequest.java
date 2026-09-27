package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class PorkElfPotteryShardRequest extends CoinMasterShopRequest {
  public static final String master = "Pork Elf Pottery Shard Assembly";
  public static final String SHOPID = "potsherd";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, PorkElfPotteryShardRequest.class)
          .withNewShopRowFields(master, SHOPID);
}
