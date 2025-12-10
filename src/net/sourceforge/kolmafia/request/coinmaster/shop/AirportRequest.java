package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class AirportRequest extends CoinMasterShopRequest {
  public static final String master = "Elemental Duty Free, Inc.";
  public static final String SHOPID = "airport";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, AirportRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(AirportRequest::accessible);

  public static String accessible() {
    if (Preferences.getBoolean("coldAirportAlways")
        || Preferences.getBoolean("hotAirportAlways")
        || Preferences.getBoolean("spookyAirportAlways")
        || Preferences.getBoolean("stenchAirportAlways")
        || Preferences.getBoolean("sleazeAirportAlways")
        || Preferences.getBoolean("_coldAirportToday")
        || Preferences.getBoolean("_hotAirportToday")
        || Preferences.getBoolean("_spookyAirportToday")
        || Preferences.getBoolean("_stenchAirportToday")
        || Preferences.getBoolean("_sleazeAirportToday")) {
      return null;
    }
    return "You cannot access the Elemental Airport";
  }
}
