package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;

public class SeptEmberCenserRequest extends CoinMasterRequest {
  public static final String master = "Sept-Ember Censer";

  // <b>You have 8 Embers.</b>
  private static final Pattern TOKEN_PATTERN = Pattern.compile("<b>You have ([\\d,]+) Ember");

  public static final CoinmasterData SEPTEMBER_CENSER =
      new CoinmasterData(master, "Sept-Ember Censer", SeptEmberCenserRequest.class)
          .withToken("Ember")
          .withTokenPattern(TOKEN_PATTERN)
          .withProperty("availableSeptEmbers")
          .withShopRowFields(master, "september");

  public SeptEmberCenserRequest() {
    super(SEPTEMBER_CENSER);
  }

  public SeptEmberCenserRequest(final boolean buying, final AdventureResult[] attachments) {
    super(SEPTEMBER_CENSER, buying, attachments);
  }

  public SeptEmberCenserRequest(final boolean buying, final AdventureResult attachment) {
    super(SEPTEMBER_CENSER, buying, attachment);
  }

  public SeptEmberCenserRequest(final boolean buying, final int itemId, final int quantity) {
    super(SEPTEMBER_CENSER, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=september")) {
      return;
    }

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(SEPTEMBER_CENSER, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(SEPTEMBER_CENSER, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=september")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(SEPTEMBER_CENSER, urlString, true);
  }

  public static String accessible() {
    if (InventoryManager.hasItem(ItemPool.SEPTEMBER_CENSER)) {
      return null;
    }
    return "You need a Sept-Ember Censer in order to shop here.";
  }
}
