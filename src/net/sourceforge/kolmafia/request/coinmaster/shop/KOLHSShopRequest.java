package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;

public abstract class KOLHSShopRequest extends CoinMasterShopRequest {
  public static final String master = "Shop Class (After School)";
  public static final String SHOPID = "kolhs_shop";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, KOLHSShopRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(KOLHSShopRequest::accessible);

  public static String accessible() {
    if (KoLCharacter.inHighschool()) {
      return null;
    }
    return "You cannot make that as you are not at school.";
  }
}
