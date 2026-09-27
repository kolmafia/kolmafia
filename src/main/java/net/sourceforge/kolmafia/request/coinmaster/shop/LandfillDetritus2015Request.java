package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class LandfillDetritus2015Request extends CoinMasterShopRequest {
  public static final String master = "Landfill Detritus from 2015 Assembly";
  public static final String SHOPID = "detritus2015";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, LandfillDetritus2015Request.class)
          .withNewShopRowFields(master, SHOPID);
}
