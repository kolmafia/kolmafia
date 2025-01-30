package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class PrecinctRequest extends CoinMasterRequest {
  public static final String master = "Precinct Materiel Division";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) cop dollar");
  public static final AdventureResult DOLLAR = ItemPool.get(ItemPool.COP_DOLLAR, 1);

  public static final CoinmasterData PRECINCT =
      new CoinmasterData(master, "Precinct Materiel Division", PrecinctRequest.class)
          .withToken("cop dollar")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(DOLLAR)
          .withShopRowFields(master, "detective");

  public PrecinctRequest() {
    super(PRECINCT);
  }

  public PrecinctRequest(final boolean buying, final AdventureResult[] attachments) {
    super(PRECINCT, buying, attachments);
  }

  public PrecinctRequest(final boolean buying, final AdventureResult attachment) {
    super(PRECINCT, buying, attachment);
  }

  public PrecinctRequest(final boolean buying, final int itemId, final int quantity) {
    super(PRECINCT, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=detective")) {
      return;
    }

    CoinmasterData data = PRECINCT;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=detective")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(PRECINCT, urlString, true);
  }

  public static String accessible() {
    return null;
  }
}
