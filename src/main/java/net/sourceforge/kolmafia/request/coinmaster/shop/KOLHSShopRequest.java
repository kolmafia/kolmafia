package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class KOLHSShopRequest extends CoinMasterShopRequest {
  public static final String master = "Shop Class (After School)";
  public static final String SHOPID = "kolhs_shop";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, KOLHSShopRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(KOLHSShopRequest::accessible);

  public static String accessible() {
    if (KoLCharacter.inHighschool()
        && Preferences.getInteger("lastKOLHSShopClassUnlockAdventure")
            == KoLCharacter.getCurrentRun()) {
      return null;
    }
    return "You need to be in Shop Class to make that.";
  }
}
