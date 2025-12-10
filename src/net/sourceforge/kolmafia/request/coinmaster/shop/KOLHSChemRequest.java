package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;

public abstract class KOLHSChemRequest extends CoinMasterShopRequest {
  public static final String master = "Chemistry Class (After School)";
  public static final String SHOPID = "kolhs_chem";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, KOLHSChemRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(KOLHSChemRequest::accessible);

  public static String accessible() {
    if (KoLCharacter.inHighschool()) {
      return null;
    }
    return "You cannot make that as you are not at school.";
  }
}
