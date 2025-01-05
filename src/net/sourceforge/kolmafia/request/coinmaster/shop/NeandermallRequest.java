package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.QuestManager;

public class NeandermallRequest extends CoinMasterRequest {
  public static final String master = "The Neandermall";

  private static final Pattern CHRONER_PATTERN = Pattern.compile("([\\d,]+) Chroner");
  public static final AdventureResult CHRONER = ItemPool.get(ItemPool.CHRONER, 1);

  public static final CoinmasterData NEANDERMALL =
      new CoinmasterData(master, "caveshop", NeandermallRequest.class)
          .withToken("Chroner")
          .withTokenTest("no Chroner")
          .withTokenPattern(CHRONER_PATTERN)
          .withItem(CHRONER)
          .withShopRowFields(master, "caveshop")
          .withNeedsPasswordHash(true);

  public NeandermallRequest() {
    super(NEANDERMALL);
  }

  public NeandermallRequest(final boolean buying, final AdventureResult[] attachments) {
    super(NEANDERMALL, buying, attachments);
  }

  public NeandermallRequest(final boolean buying, final AdventureResult attachment) {
    super(NEANDERMALL, buying, attachment);
  }

  public NeandermallRequest(final boolean buying, final int itemId, final int quantity) {
    super(NEANDERMALL, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=caveshop")) {
      return;
    }

    if (responseText.contains("That store isn't there anymore.")) {
      QuestManager.handleTimeTower(false);
      return;
    }

    QuestManager.handleTimeTower(true);

    CoinmasterData data = NEANDERMALL;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("timeTowerAvailable")) {
      return "You can't get to the Neandermall";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=caveshop")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(NEANDERMALL, urlString, true);
  }
}
