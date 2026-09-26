package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class KringleRequest extends CoinMasterShopRequest {
  public static final String master = "The H. M. S. Kringle's Workshop";
  public static final String SHOPID = "crimbo19toys";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, KringleRequest.class)
          .inZone("Crimbo19")
          .withNewShopRowFields(master, SHOPID);
}
