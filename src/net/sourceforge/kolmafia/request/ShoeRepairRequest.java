package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.QuestManager;

public class ShoeRepairRequest extends CoinMasterRequest {
  public static final String master = "Legitimate Shoe Repair, Inc.";

  private static final Pattern CHRONER_PATTERN = Pattern.compile("([\\d,]+) Chroner");
  public static final AdventureResult CHRONER = ItemPool.get(ItemPool.CHRONER, 1);

  public static final CoinmasterData SHOE_REPAIR =
      new CoinmasterData(master, "shoeshop", ShoeRepairRequest.class)
          .withToken("Chroner")
          .withTokenTest("no Chroner")
          .withTokenPattern(CHRONER_PATTERN)
          .withItem(CHRONER)
          .withShopRowFields(master, "shoeshop")
          .withNeedsPasswordHash(true);

  public ShoeRepairRequest() {
    super(SHOE_REPAIR);
  }

  public ShoeRepairRequest(final boolean buying, final AdventureResult[] attachments) {
    super(SHOE_REPAIR, buying, attachments);
  }

  public ShoeRepairRequest(final boolean buying, final AdventureResult attachment) {
    super(SHOE_REPAIR, buying, attachment);
  }

  public ShoeRepairRequest(final boolean buying, final int itemId, final int quantity) {
    super(SHOE_REPAIR, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=shoeshop")) {
      return;
    }

    if (responseText.contains("That store isn't there anymore.")) {
      QuestManager.handleTimeTower(false);
      return;
    }

    QuestManager.handleTimeTower(true);

    CoinmasterData data = SHOE_REPAIR;

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
      return "You can't get to the Shoe Repair Shop";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=shoeshop")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(SHOE_REPAIR, urlString, true);
  }
}
