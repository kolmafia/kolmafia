package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class ToxicChemistryRequest extends CoinMasterRequest {
  public static final String master = "Toxic Chemistry";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) toxic globule");
  public static final AdventureResult TOXIC_GLOBULE = ItemPool.get(ItemPool.TOXIC_GLOBULE, 1);

  public static final CoinmasterData TOXIC_CHEMISTRY =
      new CoinmasterData(master, "ToxicChemistry", ToxicChemistryRequest.class)
          .withToken("toxic globule")
          .withTokenTest("no toxic globules")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOXIC_GLOBULE)
          .withShopRowFields(master, "toxic");

  public ToxicChemistryRequest() {
    super(TOXIC_CHEMISTRY);
  }

  public ToxicChemistryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(TOXIC_CHEMISTRY, buying, attachments);
  }

  public ToxicChemistryRequest(final boolean buying, final AdventureResult attachment) {
    super(TOXIC_CHEMISTRY, buying, attachment);
  }

  public ToxicChemistryRequest(final boolean buying, final int itemId, final int quantity) {
    super(TOXIC_CHEMISTRY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.startsWith("shop.php") || !location.contains("whichshop=toxic")) {
      return;
    }

    CoinmasterData data = TOXIC_CHEMISTRY;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (TOXIC_GLOBULE.getCount(KoLConstants.inventory) == 0) {
      return "You do not have a toxic globule in inventory";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    // shop.php?pwd&whichshop=toxic
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=toxic")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(TOXIC_CHEMISTRY, urlString, true);
  }
}
