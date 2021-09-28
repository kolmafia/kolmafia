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

public class NinjaStoreRequest extends CoinMasterRequest {
  public static final String master = "Ni&ntilde;a Store";

  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(NinjaStoreRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(NinjaStoreRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(NinjaStoreRequest.master);
  private static final Pattern CHRONER_PATTERN = Pattern.compile("([\\d,]+) Chroner");
  public static final AdventureResult CHRONER = ItemPool.get(ItemPool.CHRONER, 1);

  public static final CoinmasterData NINJA_STORE =
      new CoinmasterData(
          NinjaStoreRequest.master,
          "nina",
          NinjaStoreRequest.class,
          "Chroner",
          "no Chroner",
          false,
          NinjaStoreRequest.CHRONER_PATTERN,
          NinjaStoreRequest.CHRONER,
          null,
          NinjaStoreRequest.itemRows,
          "shop.php?whichshop=nina",
          "buyitem",
          NinjaStoreRequest.buyItems,
          NinjaStoreRequest.buyPrices,
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

  public NinjaStoreRequest() {
    super(NinjaStoreRequest.NINJA_STORE);
  }

  public NinjaStoreRequest(final boolean buying, final AdventureResult[] attachments) {
    super(NinjaStoreRequest.NINJA_STORE, buying, attachments);
  }

  public NinjaStoreRequest(final boolean buying, final AdventureResult attachment) {
    super(NinjaStoreRequest.NINJA_STORE, buying, attachment);
  }

  public NinjaStoreRequest(final boolean buying, final int itemId, final int quantity) {
    super(NinjaStoreRequest.NINJA_STORE, buying, itemId, quantity);
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
    NinjaStoreRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=nina")) {
      return;
    }

    if (responseText.contains("That store isn't there anymore.")) {
      QuestManager.handleTimeTower(false);
      return;
    }

    QuestManager.handleTimeTower(true);

    CoinmasterData data = NinjaStoreRequest.NINJA_STORE;

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
      return "You can't get to Ni&ntilde;a Store";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=nina")) {
      return false;
    }

    CoinmasterData data = NinjaStoreRequest.NINJA_STORE;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
