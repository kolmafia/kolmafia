package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class KOLHSArtRequest extends CoinMasterShopRequest {
  public static final String master = "Art Class (After School)";
  public static final String SHOPID = "kolhs_art";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, KOLHSArtRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(KOLHSArtRequest::accessible);

  public static String accessible() {
    if (KoLCharacter.inHighschool()
        && Preferences.getInteger("lastKOLHSArtClassUnlockAdventure")
            == KoLCharacter.getCurrentRun()) {
      return null;
    }
    return "You need to be in Art Class to make that.";
  }
}
