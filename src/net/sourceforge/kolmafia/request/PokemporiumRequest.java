package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class PokemporiumRequest extends CoinMasterRequest {
  public static final String master = "The Pok&eacute;mporium";

  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(PokemporiumRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(PokemporiumRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(PokemporiumRequest.master);
  private static final Pattern POKEDOLLAR_PATTERN =
      Pattern.compile("([\\d,]+) 1,960 pok&eacute;dollar bills");
  public static final AdventureResult POKEDOLLAR =
      new AdventureResult(ItemPool.POKEDOLLAR_BILLS, 1, false) {
        @Override
        public String getPluralName(int price) {
          return price == 1 ? "pok&eacute;dollar bill" : "pok&eacute;dollar bills";
        }
      };

  public static final CoinmasterData POKEMPORIUM =
      new CoinmasterData(
          PokemporiumRequest.master,
          "pokefam",
          PokemporiumRequest.class,
          "pok&eacute;dollar bills",
          "no pok&eacute;dollar bills",
          false,
          PokemporiumRequest.POKEDOLLAR_PATTERN,
          PokemporiumRequest.POKEDOLLAR,
          null,
          PokemporiumRequest.itemRows,
          "shop.php?whichshop=pokefam",
          "buyitem",
          PokemporiumRequest.buyItems,
          PokemporiumRequest.buyPrices,
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
          true) {
        @Override
        public final boolean canBuyItem(final int itemId) {
          return KoLCharacter.inPokefam();
        }
      };

  static {
    POKEMPORIUM.plural = "pok&eacute;dollar bills";
  }

  public PokemporiumRequest() {
    super(PokemporiumRequest.POKEMPORIUM);
  }

  public PokemporiumRequest(final boolean buying, final AdventureResult[] attachments) {
    super(PokemporiumRequest.POKEMPORIUM, buying, attachments);
  }

  public PokemporiumRequest(final boolean buying, final AdventureResult attachment) {
    super(PokemporiumRequest.POKEMPORIUM, buying, attachment);
  }

  public PokemporiumRequest(final boolean buying, final int itemId, final int quantity) {
    super(PokemporiumRequest.POKEMPORIUM, buying, itemId, quantity);
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
    PokemporiumRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=pokefam")) {
      return;
    }

    CoinmasterData data = PokemporiumRequest.POKEMPORIUM;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    // Change after it closes
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=pokefam")) {
      return false;
    }

    CoinmasterData data = PokemporiumRequest.POKEMPORIUM;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
