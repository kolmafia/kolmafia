package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
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

  private static int catalogToUse() {
    if (InventoryManager.hasItem(ItemPool.MR_STORE_2002_CATALOG)) {
      return ItemPool.MR_STORE_2002_CATALOG;
    }
    if (KoLCharacter.inLegacyOfLoathing()
        && InventoryManager.hasItem(ItemPool.REPLICA_MR_STORE_2002_CATALOG)) {
      return ItemPool.REPLICA_MR_STORE_2002_CATALOG;
    }
    return 0;
  }

  // If we need to use the item in order to gain our daily store
  // credits, create a request to use it. That request will redirect to
  // shop.php. We do not want the request to automatically follow the
  // request, since we will submit our own request to do that, which
  // might include additional fields to buy an item.
  //
  // There are two options:
  //
  // 1) We could create a UseItemRequest. That does not follow
  //    redirects.  However, it comes with a bunch of additional
  //    overhead which we don't need or want.
  //
  // 2) We could create a GenericRequest, which has no extraneous
  //    overhead. However, as coded, it automatically follows redirects.
  //    We can override that behavior.

  private static GenericRequest useItemRequest(int itemId) {
    return new GenericRequest("inv_use.php?which=3&ajax=1&whichitem=" + itemId) {
      @Override
      protected boolean shouldFollowRedirect() {
        return false;
      }
    };
  }

  @Override
  public void run() {
    // Make sure we have a Mr. Store 2002 catalog
    int catalog = catalogToUse();
    if (catalog == 0) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "You have no 2002 Mr. Store Catalog available.");
      return;
    }

    // Make sure it is in inventory
    if (!InventoryManager.retrieveItem(catalog)) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Unable to put catalog into inventory.");
      return;
    }

    // If we have not yet obtained today's store credits, "visit" by
    // using the item (which redirects to the shop), rather than going
    // to the shop directly, since the latter will not collect credits.
    if (!Preferences.getBoolean("_2002MrStoreCreditsCollected")) {
      // Create a request.
      GenericRequest request = useItemRequest(catalog);
      // Run it.
      request.run();
      // Check that it redirected to shop.php
      String redirectLocation = request.redirectLocation;
      if (redirectLocation == null
          || !redirectLocation.startsWith("shop.php")
          || !redirectLocation.contains("whichshop=mrstore2002")) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Failed to redirect to shop.php.");
        return;
      }
      // Remember that we've collected credits today.
      Preferences.setBoolean("_2002MrStoreCreditsCollected", true);
    }

    // Now run the shop.php request
    super.run();
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
    if (catalogToUse() != 0) {
      return null;
    }
    return "You need a 2002 Mr. Store Catalog in order to shop here.";
  }
}
