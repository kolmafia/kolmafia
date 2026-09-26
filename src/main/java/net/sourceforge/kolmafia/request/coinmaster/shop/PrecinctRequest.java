package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class PrecinctRequest extends CoinMasterShopRequest {
  public static final String master = "Precinct Materiel Division";
  public static final String SHOPID = "detective";

  public static final CoinmasterData PRECINCT =
      new CoinmasterData(master, "Precinct Materiel Division", PrecinctRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(PrecinctRequest::accessible);

  public static String accessible() {
    if (Preferences.getBoolean("hasDetectiveSchool")) {
      return null;
    }
    return "You cannot access the Precinct";
  }
}
