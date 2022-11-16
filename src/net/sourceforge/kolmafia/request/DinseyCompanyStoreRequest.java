package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class DinseyCompanyStoreRequest extends CoinMasterRequest {
  public static final String master = "The Dinsey Company Store";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) FunFunds");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.FUNFUNDS, 1);

  public static final CoinmasterData DINSEY_COMPANY_STORE =
      new CoinmasterData(master, "DinsyStore", DinseyCompanyStoreRequest.class)
          .withToken("FunFunds&trade;")
          .withPluralToken("FunFunds&trade;")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "landfillstore");

  public DinseyCompanyStoreRequest() {
    super(DINSEY_COMPANY_STORE);
  }

  public DinseyCompanyStoreRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DINSEY_COMPANY_STORE, buying, attachments);
  }

  public DinseyCompanyStoreRequest(final boolean buying, final AdventureResult attachment) {
    super(DINSEY_COMPANY_STORE, buying, attachment);
  }

  public DinseyCompanyStoreRequest(final boolean buying, final int itemId, final int quantity) {
    super(DINSEY_COMPANY_STORE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=landfillstore")) {
      return;
    }

    CoinmasterData data = DINSEY_COMPANY_STORE;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=landfillstore")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(DINSEY_COMPANY_STORE, urlString, true);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_stenchAirportToday")
        && !Preferences.getBoolean("stenchAirportAlways")) {
      return "You don't have access to Dinseylandfill";
    }
    if (KoLCharacter.getLimitMode().limitZone("Dinseylandfill")) {
      return "You cannot currently access Dinseylandfill";
    }
    return null;
  }
}
