package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class DinostaurRequest extends CoinMasterRequest {
  public static final String master = "Dino World Gift Shop (The Dinostaur)";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(DinostaurRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(DinostaurRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(DinostaurRequest.master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Dinodollars");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.DINODOLLAR, 1);
  public static final CoinmasterData DINOSTAUR =
      new CoinmasterData(
          DinostaurRequest.master,
          "Dinostaur",
          DinostaurRequest.class,
          "Dinodollars",
          null,
          false,
          DinostaurRequest.TOKEN_PATTERN,
          DinostaurRequest.COIN,
          null,
          DinostaurRequest.itemRows,
          "shop.php?whichshop=dino",
          "buyitem",
          DinostaurRequest.buyItems,
          DinostaurRequest.buyPrices,
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

  public DinostaurRequest() {
    super(DinostaurRequest.DINOSTAUR);
  }

  public DinostaurRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DinostaurRequest.DINOSTAUR, buying, attachments);
  }

  public DinostaurRequest(final boolean buying, final AdventureResult attachment) {
    super(DinostaurRequest.DINOSTAUR, buying, attachment);
  }

  public DinostaurRequest(final boolean buying, final int itemId, final int quantity) {
    super(DinostaurRequest.DINOSTAUR, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    DinostaurRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=dino")) {
      return;
    }

    CoinmasterData data = DinostaurRequest.DINOSTAUR;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=dino")) {
      return false;
    }

    CoinmasterData data = DinostaurRequest.DINOSTAUR;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }

  public static String accessible() {
    if (!KoLCharacter.inDinocore()) {
      return "Dino World is not available";
    }
    return null;
  }
}
