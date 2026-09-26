package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class Crimbo16Request extends CoinMasterShopRequest {
  public static final String master = "Crimbo Lumps Shop";
  public static final String SHOPID = "crimbo16";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, Crimbo16Request.class)
          .inZone("Crimbo16")
          .withNewShopRowFields(master, SHOPID);
}
