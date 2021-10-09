package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class PlumberGearRequest extends CoinMasterRequest {
  public static final String master = "Mushroom District Gear Shop";

  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(PlumberGearRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(PlumberGearRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(PlumberGearRequest.master);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("([\\d,]+) coin");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.COIN, 1);

  public static final CoinmasterData PLUMBER_GEAR =
      new CoinmasterData(
          PlumberGearRequest.master,
          "mariogear",
          PlumberGearRequest.class,
          "coin",
          "no coins",
          false,
          PlumberGearRequest.TOKEN_PATTERN,
          PlumberGearRequest.COIN,
          null,
          PlumberGearRequest.itemRows,
          "shop.php?whichshop=mariogear",
          "buyitem",
          PlumberGearRequest.buyItems,
          PlumberGearRequest.buyPrices,
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

  public PlumberGearRequest() {
    super(PlumberGearRequest.PLUMBER_GEAR);
  }

  public PlumberGearRequest(final boolean buying, final AdventureResult[] attachments) {
    super(PlumberGearRequest.PLUMBER_GEAR, buying, attachments);
  }

  public PlumberGearRequest(final boolean buying, final AdventureResult attachment) {
    super(PlumberGearRequest.PLUMBER_GEAR, buying, attachment);
  }

  public PlumberGearRequest(final boolean buying, final int itemId, final int quantity) {
    super(PlumberGearRequest.PLUMBER_GEAR, buying, itemId, quantity);
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
    PlumberGearRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    CoinmasterData data = PlumberGearRequest.PLUMBER_GEAR;
    String action = GenericRequest.getAction(location);
    if (action == null) {
      if (location.contains("whichshop=mariogear")) {
        // Parse current coin balances
        CoinMasterRequest.parseBalance(data, responseText);
      }

      return;
    }

    CoinMasterRequest.parseResponse(data, location, responseText);
  }

  public static String accessible() {
    if (!KoLCharacter.isPlumber()) {
      return "You are not a plumber.";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=mariogear")) {
      return false;
    }

    CoinmasterData data = PlumberGearRequest.PLUMBER_GEAR;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
