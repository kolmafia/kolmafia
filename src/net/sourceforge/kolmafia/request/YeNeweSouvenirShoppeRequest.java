package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.QuestManager;

public class YeNeweSouvenirShoppeRequest extends CoinMasterRequest {
  public static final String master = "Ye Newe Souvenir Shoppe";

  private static final Pattern CHRONER_PATTERN = Pattern.compile("([\\d,]+) Chroner");
  public static final AdventureResult CHRONER = ItemPool.get(ItemPool.CHRONER, 1);

  public static final CoinmasterData SHAKE_SHOP =
      new CoinmasterData(master, "shakeshop", YeNeweSouvenirShoppeRequest.class)
          .withToken("Chroner")
          .withTokenTest("no Chroner")
          .withTokenPattern(CHRONER_PATTERN)
          .withItem(CHRONER)
          .withNeedsPasswordHash(true)
          .withShopRowFields(master, "shakeshop");

  public YeNeweSouvenirShoppeRequest() {
    super(SHAKE_SHOP);
  }

  public YeNeweSouvenirShoppeRequest(final boolean buying, final AdventureResult[] attachments) {
    super(SHAKE_SHOP, buying, attachments);
  }

  public YeNeweSouvenirShoppeRequest(final boolean buying, final AdventureResult attachment) {
    super(SHAKE_SHOP, buying, attachment);
  }

  public YeNeweSouvenirShoppeRequest(final boolean buying, final int itemId, final int quantity) {
    super(SHAKE_SHOP, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=shakeshop")) {
      return;
    }

    if (responseText.contains("That store isn't there anymore.")) {
      QuestManager.handleTimeTower(false);
      return;
    }

    QuestManager.handleTimeTower(true);

    CoinmasterData data = SHAKE_SHOP;

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
      return "You can't get to Ye Newe Souvenir Shoppe";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=shakeshop")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(SHAKE_SHOP, urlString, true);
  }
}
