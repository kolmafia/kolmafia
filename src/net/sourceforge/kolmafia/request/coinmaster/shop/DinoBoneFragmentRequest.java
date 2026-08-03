package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;

public abstract class DinoBoneFragmentRequest extends CoinMasterShopRequest {
  public static final String master = "Dino Bone Fragment Assembly";
  public static final String SHOPID = "dinobone";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, DinoBoneFragmentRequest.class)
          .withNewShopRowFields(master, SHOPID);
}
