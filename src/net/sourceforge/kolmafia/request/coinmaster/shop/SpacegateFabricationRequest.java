package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class SpacegateFabricationRequest extends CoinMasterRequest {
  public static final String master = "Spacegate Fabrication Facility";

  private static final Pattern RESEARCH_PATTERN =
      Pattern.compile("([\\d,]+) pages? of Spacegate Research");
  public static final AdventureResult RESEARCH = ItemPool.get(ItemPool.SPACEGATE_RESEARCH, 1);

  public static final CoinmasterData SPACEGATE_STORE =
      new CoinmasterData(master, "spacegate", SpacegateFabricationRequest.class)
          .withToken("Spacegate Research")
          .withTokenTest("no pages of Spacegate Research")
          .withTokenPattern(RESEARCH_PATTERN)
          .withItem(RESEARCH)
          .withShopRowFields(master, "spacegate")
          .withNeedsPasswordHash(true);

  public SpacegateFabricationRequest() {
    super(SPACEGATE_STORE);
  }

  public SpacegateFabricationRequest(final boolean buying, final AdventureResult[] attachments) {
    super(SPACEGATE_STORE, buying, attachments);
  }

  public SpacegateFabricationRequest(final boolean buying, final AdventureResult attachment) {
    super(SPACEGATE_STORE, buying, attachment);
  }

  public SpacegateFabricationRequest(final boolean buying, final int itemId, final int quantity) {
    super(SPACEGATE_STORE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=spacegate")) {
      return;
    }

    CoinmasterData data = SPACEGATE_STORE;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_spacegateToday") && !Preferences.getBoolean("spacegateAlways")) {
      return "You can't get to the Spacegate.";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=spacegate")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(SPACEGATE_STORE, urlString, true);
  }
}
