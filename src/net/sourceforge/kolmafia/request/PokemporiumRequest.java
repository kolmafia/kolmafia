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
      CoinmastersDatabase.getBuyItems(master);
  private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices(master);
  private static final Map<Integer, Integer> itemRows = CoinmastersDatabase.getRows(master);
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
      new CoinmasterData(master, "pokefam", PokemporiumRequest.class) {
        @Override
        public final boolean canBuyItem(final int itemId) {
          return KoLCharacter.inPokefam();
        }
      }.withToken("pok&eacute;dollar bills")
          .withPluralToken("pok&eacute;dollar bills")
          .withTokenTest("no pok&eacute;dollar bills")
          .withTokenPattern(POKEDOLLAR_PATTERN)
          .withItem(POKEDOLLAR)
          .withRowShopFields(master, "pokefam");

  public PokemporiumRequest() {
    super(POKEMPORIUM);
  }

  public PokemporiumRequest(final boolean buying, final AdventureResult[] attachments) {
    super(POKEMPORIUM, buying, attachments);
  }

  public PokemporiumRequest(final boolean buying, final AdventureResult attachment) {
    super(POKEMPORIUM, buying, attachment);
  }

  public PokemporiumRequest(final boolean buying, final int itemId, final int quantity) {
    super(POKEMPORIUM, buying, itemId, quantity);
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
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=pokefam")) {
      return;
    }

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(POKEMPORIUM, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(POKEMPORIUM, responseText);
  }

  public static String accessible() {
    // Change after it closes
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=pokefam")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(POKEMPORIUM, urlString, true);
  }
}
