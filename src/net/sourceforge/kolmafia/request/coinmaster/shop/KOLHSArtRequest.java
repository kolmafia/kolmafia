package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;

public abstract class KOLHSArtRequest extends CoinMasterShopRequest {
  public static final String master = "Art Class (After School)";
  public static final String SHOPID = "kolhs_art";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, KOLHSArtRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(KOLHSArtRequest::accessible);

  public static String accessible() {
    if (KoLCharacter.inHighschool()) {
      return null;
    }
    return "You cannot make that as you are not at school.";
  }
}
