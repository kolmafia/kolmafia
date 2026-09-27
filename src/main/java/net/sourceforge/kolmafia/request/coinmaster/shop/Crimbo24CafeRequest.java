package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class Crimbo24CafeRequest extends CoinMasterShopRequest {
  public static final String master = "Crimbo24 Cafe";
  public static final String SHOPID = "crimbo24_cafe";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo24_cafe", Crimbo24CafeRequest.class)
          .inZone("Crimbo24")
          .withNewShopRowFields(master, SHOPID);
}
