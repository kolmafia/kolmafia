package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.QuestManager;

public class NeandermallRequest extends CoinMasterRequest {
  public static final String master = "The Neandermall";

  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(NeandermallRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(NeandermallRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(NeandermallRequest.master);
  private static final Pattern CHRONER_PATTERN = Pattern.compile("([\\d,]+) Chroner");
  public static final AdventureResult CHRONER = ItemPool.get(ItemPool.CHRONER, 1);

  public static final CoinmasterData NEANDERMALL =
      new CoinmasterData(
          NeandermallRequest.master,
          "caveshop",
          NeandermallRequest.class,
          "Chroner",
          "no Chroner",
          false,
          NeandermallRequest.CHRONER_PATTERN,
          NeandermallRequest.CHRONER,
          null,
          NeandermallRequest.itemRows,
          "shop.php?whichshop=caveshop",
          "buyitem",
          NeandermallRequest.buyItems,
          NeandermallRequest.buyPrices,
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

  public NeandermallRequest() {
    super(NeandermallRequest.NEANDERMALL);
  }

  public NeandermallRequest(final boolean buying, final AdventureResult[] attachments) {
    super(NeandermallRequest.NEANDERMALL, buying, attachments);
  }

  public NeandermallRequest(final boolean buying, final AdventureResult attachment) {
    super(NeandermallRequest.NEANDERMALL, buying, attachment);
  }

  public NeandermallRequest(final boolean buying, final int itemId, final int quantity) {
    super(NeandermallRequest.NEANDERMALL, buying, itemId, quantity);
  }

  @Override
  public void run() {
    if (this.action != null) {
      this.addFormField("pwd");
    }

    super.run();
  }

  @Override
  public void processResults() {
    NeandermallRequest.parseResponse(this.getURLString(), this.responseText);
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

    CoinmasterData data = NeandermallRequest.NEANDERMALL;

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

    CoinmasterData data = NeandermallRequest.NEANDERMALL;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
