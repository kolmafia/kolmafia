package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class ShadowForgeRequest extends CoinMasterShopRequest {
  public static final String master = "The Shadow Forge";
  public static final String SHOPID = "shadowforge";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, ShadowForgeRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(ShadowForgeRequest::accessible);

  public static String accessible() {
    if (Preferences.getInteger("lastShadowForgeUnlockAdventure") == KoLCharacter.getCurrentRun()) {
      return null;
    }
    return "You need to be at The Shadow Forge to make that.";
  }
}
