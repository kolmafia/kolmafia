package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class RumpleRequest extends CoinMasterShopRequest {
  public static final String master = "Rumplestiltskin's Workshop";
  public static final String SHOPID = "rumple";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, RumpleRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(RumpleRequest::accessible);

  public static String accessible() {
    if (Preferences.getString("grimstoneMaskPath").equals("gnome")) {
      return null;
    }
    return "You need access to Rumplestiltskin's Workshop to make that.";
  }
}
