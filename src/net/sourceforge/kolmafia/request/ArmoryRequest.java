package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.Limitmode;

public class ArmoryRequest extends CoinMasterRequest {
  public static final String master = "The Armory";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(ArmoryRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(ArmoryRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(ArmoryRequest.master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Coins-spiracy");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.COINSPIRACY, 1);
  public static final CoinmasterData ARMORY =
      new CoinmasterData(
          ArmoryRequest.master,
          "armory",
          ArmoryRequest.class,
          "Coinspiracy",
          null,
          false,
          ArmoryRequest.TOKEN_PATTERN,
          ArmoryRequest.COIN,
          null,
          ArmoryRequest.itemRows,
          "shop.php?whichshop=si_shop3",
          "buyitem",
          ArmoryRequest.buyItems,
          ArmoryRequest.buyPrices,
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

  public ArmoryRequest() {
    super(ArmoryRequest.ARMORY);
  }

  public ArmoryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(ArmoryRequest.ARMORY, buying, attachments);
  }

  public ArmoryRequest(final boolean buying, final AdventureResult attachment) {
    super(ArmoryRequest.ARMORY, buying, attachment);
  }

  public ArmoryRequest(final boolean buying, final int itemId, final int quantity) {
    super(ArmoryRequest.ARMORY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    ArmoryRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=si_shop3")) {
      return;
    }

    CoinmasterData data = ArmoryRequest.ARMORY;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=si_shop3")) {
      return false;
    }

    CoinmasterData data = ArmoryRequest.ARMORY;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_spookyAirportToday")
        && !Preferences.getBoolean("spookyAirportAlways")) {
      return "You don't have access to Conspiracy Island";
    }
    if (Limitmode.limitZone("Conspiracy Island")) {
      return "You cannot currently access Conspiracy Island";
    }
    if (!Preferences.getBoolean("armoryUnlocked")) {
      return "The Armory is locked";
    }
    return null;
  }
}
