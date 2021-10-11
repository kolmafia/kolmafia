package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class ToxicChemistryRequest extends CoinMasterRequest {
  public static final String master = "Toxic Chemistry";

  public static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(ToxicChemistryRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(ToxicChemistryRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(ToxicChemistryRequest.master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) toxic globule");
  public static final AdventureResult TOXIC_GLOBULE = ItemPool.get(ItemPool.TOXIC_GLOBULE, 1);

  public static final CoinmasterData TOXIC_CHEMISTRY =
      new CoinmasterData(
          ToxicChemistryRequest.master,
          "ToxicChemistry",
          ToxicChemistryRequest.class,
          "toxic globule",
          "no toxic globules",
          false,
          ToxicChemistryRequest.TOKEN_PATTERN,
          ToxicChemistryRequest.TOXIC_GLOBULE,
          null,
          ToxicChemistryRequest.itemRows,
          "shop.php?whichshop=toxic",
          "buyitem",
          ToxicChemistryRequest.buyItems,
          ToxicChemistryRequest.buyPrices,
          null,
          null,
          null,
          null,
          "whichrow",
          GenericRequest.WHICHROW_PATTERN,
          "quantity",
          GenericRequest.QUANTITY_PATTERN,
          null,
          null,
          true);

  public ToxicChemistryRequest() {
    super(ToxicChemistryRequest.TOXIC_CHEMISTRY);
  }

  public ToxicChemistryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(ToxicChemistryRequest.TOXIC_CHEMISTRY, buying, attachments);
  }

  public ToxicChemistryRequest(final boolean buying, final AdventureResult attachment) {
    super(ToxicChemistryRequest.TOXIC_CHEMISTRY, buying, attachment);
  }

  public ToxicChemistryRequest(final boolean buying, final int itemId, final int quantity) {
    super(ToxicChemistryRequest.TOXIC_CHEMISTRY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    ToxicChemistryRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.startsWith("shop.php") || !location.contains("whichshop=toxic")) {
      return;
    }

    CoinmasterData data = ToxicChemistryRequest.TOXIC_CHEMISTRY;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    if (ToxicChemistryRequest.TOXIC_GLOBULE.getCount(KoLConstants.inventory) == 0) {
      return "You do not have a toxic globule in inventory";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    // shop.php?pwd&whichshop=toxic
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=toxic")) {
      return false;
    }

    CoinmasterData data = ToxicChemistryRequest.TOXIC_CHEMISTRY;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
