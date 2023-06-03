package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
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

  // True if accessing the shop via using the item and redirecting. The
  // first time you do this per day, KoL adds your daily store credits.
  // False if going directly to the shop.
  private boolean using = false;

  public MrStore2002Request() {
    super(MR_STORE_2002);
    // If we have not yet obtained today's store credits, "visit" by
    // using the item (which redirects to the shop), rather than going
    // to the shop directly, since the latter will not collect credits.
    if (!Preferences.getBoolean("_2002MrStoreCreditsCollected")) {
      int itemId =
          InventoryManager.hasItem(ItemPool.MR_STORE_2002_CATALOG)
              ? ItemPool.MR_STORE_2002_CATALOG
              : InventoryManager.hasItem(ItemPool.REPLICA_MR_STORE_2002_CATALOG)
                  ? ItemPool.REPLICA_MR_STORE_2002_CATALOG
                  // Don't have either catalog? Huh. run() will fail.
                  : ItemPool.MR_STORE_2002_CATALOG;
      this.constructURLString("inv_use.php?which=3&ajax=1&whichitem=" + itemId);
      this.using = true;
    }
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
  protected boolean shouldFollowRedirect() {
    return this.using;
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();
    // If we do not have a result from the shop, fail
    if (!urlString.contains("whichshop=mrstore2002")) {
      return;
    }

    // If we used the item - and redirected to the shop - we collected credits
    if (this.using) {
      Preferences.setBoolean("_2002MrStoreCreditsCollected", true);
    }

    // Perform standard show.php Coinmaster processing
    parseResponse(urlString, this.responseText);
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
    if (InventoryManager.hasItem(ItemPool.MR_STORE_2002_CATALOG)) {
      return null;
    }
    if (KoLCharacter.inLegacyOfLoathing()
        && InventoryManager.hasItem(ItemPool.REPLICA_MR_STORE_2002_CATALOG)) {
      return null;
    }
    return "You need a 2002 Mr. Store Catalog in order to shop here.";
  }
}
