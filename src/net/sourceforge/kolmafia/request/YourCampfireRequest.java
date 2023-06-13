package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class YourCampfireRequest extends CoinMasterRequest {
  public static final String master = "Your Campfire";

  private static final Pattern FIREWOOD_PATTERN = Pattern.compile("([\\d,]+) sticks? of firewood");
  public static final AdventureResult STICK_OF_FIREWOOD =
      ItemPool.get(ItemPool.STICK_OF_FIREWOOD, 1);

  public static final CoinmasterData YOUR_CAMPFIRE =
      new CoinmasterData(master, "campfire", YourCampfireRequest.class)
          .withToken("stick of firewood")
          .withTokenTest("no sticks of firewood")
          .withTokenPattern(FIREWOOD_PATTERN)
          .withItem(STICK_OF_FIREWOOD)
          .withShopRowFields(master, "campfire")
          .withNeedsPasswordHash(true);

  public YourCampfireRequest() {
    super(YOUR_CAMPFIRE);
  }

  public YourCampfireRequest(final boolean buying, final AdventureResult[] attachments) {
    super(YOUR_CAMPFIRE, buying, attachments);
  }

  public YourCampfireRequest(final boolean buying, final AdventureResult attachment) {
    super(YOUR_CAMPFIRE, buying, attachment);
  }

  public YourCampfireRequest(final boolean buying, final int itemId, final int quantity) {
    super(YOUR_CAMPFIRE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=campfire")) {
      return;
    }

    CoinmasterData data = YOUR_CAMPFIRE;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    return CampAwayRequest.campAwayTentAvailable() ? null : "Need access to your Getaway Campsite";
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=campfire")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(YOUR_CAMPFIRE, urlString, true);
  }
}
