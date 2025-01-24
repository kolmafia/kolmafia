package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.QuestManager;

public class AppleStoreRequest extends CoinMasterRequest {
  public static final String master = "The Applecalypse Store";

  private static final Pattern CHRONER_PATTERN = Pattern.compile("([\\d,]+) Chroner");
  public static final AdventureResult CHRONER = ItemPool.get(ItemPool.CHRONER, 1);

  public static final CoinmasterData APPLE_STORE =
      new CoinmasterData(master, "applestore", AppleStoreRequest.class)
          .withToken("Chroner")
          .withTokenTest("no Chroner")
          .withTokenPattern(CHRONER_PATTERN)
          .withItem(CHRONER)
          .withShopRowFields(master, "applestore")
          .withNeedsPasswordHash(true);

  public AppleStoreRequest() {
    super(APPLE_STORE);
  }

  public AppleStoreRequest(final boolean buying, final AdventureResult[] attachments) {
    super(APPLE_STORE, buying, attachments);
  }

  public AppleStoreRequest(final boolean buying, final AdventureResult attachment) {
    super(APPLE_STORE, buying, attachment);
  }

  public AppleStoreRequest(final boolean buying, final int itemId, final int quantity) {
    super(APPLE_STORE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=applestore")) {
      return;
    }

    if (responseText.contains("That store isn't there anymore.")) {
      QuestManager.handleTimeTower(false);
      return;
    }

    QuestManager.handleTimeTower(true);

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(APPLE_STORE, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(APPLE_STORE, responseText);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("timeTowerAvailable")) {
      return "You can't get to The Applecalypse Store";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=applestore")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(APPLE_STORE, urlString, true);
  }
}
