package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class WalMartRequest extends CoinMasterRequest {
  public static final String master = "Wal-Mart";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>([\\d,]+) Wal-Mart gift certificates");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.WALMART_GIFT_CERTIFICATE, 1);

  public static final CoinmasterData WALMART =
      new CoinmasterData(master, "Wal-Mart", WalMartRequest.class)
          .withToken("Wal-Mart gift certificate")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "glaciest");

  public WalMartRequest() {
    super(WALMART);
  }

  public WalMartRequest(final boolean buying, final AdventureResult[] attachments) {
    super(WALMART, buying, attachments);
  }

  public WalMartRequest(final boolean buying, final AdventureResult attachment) {
    super(WALMART, buying, attachment);
  }

  public WalMartRequest(final boolean buying, final int itemId, final int quantity) {
    super(WALMART, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=glaciest")) {
      return;
    }

    CoinmasterData data = WALMART;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=glaciest")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(WALMART, urlString, true);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_coldAirportToday")
        && !Preferences.getBoolean("coldAirportAlways")) {
      return "You don't have access to The Glaciest";
    }
    if (KoLCharacter.getLimitMode().limitZone("The Glaciest")) {
      return "You cannot currently access The Glaciest";
    }
    return null;
  }
}
