package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class CanteenRequest extends CoinMasterRequest {
  public static final String master = "The Canteen";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Coins-spiracy");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.COINSPIRACY, 1);

  public static final CoinmasterData CANTEEN =
      new CoinmasterData(master, "canteen", CanteenRequest.class)
          .withToken("Coinspiracy")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "si_shop2");

  public CanteenRequest() {
    super(CANTEEN);
  }

  public CanteenRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CANTEEN, buying, attachments);
  }

  public CanteenRequest(final boolean buying, final AdventureResult attachment) {
    super(CANTEEN, buying, attachment);
  }

  public CanteenRequest(final boolean buying, final int itemId, final int quantity) {
    super(CANTEEN, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=si_shop2")) {
      return;
    }

    CoinmasterData data = CANTEEN;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=si_shop2")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(CANTEEN, urlString, true);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_spookyAirportToday")
        && !Preferences.getBoolean("spookyAirportAlways")) {
      return "You don't have access to Conspiracy Island";
    }
    if (KoLCharacter.getLimitMode().limitZone("Conspiracy Island")) {
      return "You cannot currently access Conspiracy Island";
    }
    if (!Preferences.getBoolean("canteenUnlocked")) {
      return "The Canteen is locked";
    }
    return null;
  }
}
