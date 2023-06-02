package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.InventoryManager;

public class MrStore2002Request extends CoinMasterRequest {
  public static final String master = "Mr. Store 2002";

  // <b>You have 3 Mr. Store 2002 Credits.</b>
  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<b>You have ([\\d,]+) Mr. Store 2002 Credit");

  public static final CoinmasterData MR_STORE_2002 =
      new CoinmasterData(master, "Mr. Store 2002", MrStore2002Request.class)
          .withToken("Mr. Store 2002 Credit")
          .withTokenPattern(TOKEN_PATTERN)
          .withProperty("availableMrStore2002Credits")
          .withShopRowFields(master, "mrstore2002");

  public MrStore2002Request() {
    super(MR_STORE_2002);
  }

  public MrStore2002Request(final boolean buying, final AdventureResult[] attachments) {
    super(MR_STORE_2002, buying, attachments);
  }

  public MrStore2002Request(final boolean buying, final AdventureResult attachment) {
    super(MR_STORE_2002, buying, attachment);
  }

  public MrStore2002Request(final boolean buying, final int itemId, final int quantity) {
    super(MR_STORE_2002, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=mrstore2002")) {
      return;
    }

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(MR_STORE_2002, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(MR_STORE_2002, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=mrstore2002")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(MR_STORE_2002, urlString, true);
  }

  public static String accessible() {
    if (!InventoryManager.hasItem(ItemPool.MR_STORE_2002_CATALOG)) {
      return "You need a 2002 Mr. Store Catalog in order to shop here.";
    }
    return null;
  }
}
