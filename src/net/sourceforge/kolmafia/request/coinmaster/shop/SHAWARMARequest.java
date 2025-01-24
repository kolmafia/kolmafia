package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class SHAWARMARequest extends CoinMasterRequest {
  public static final String master = "The SHAWARMA Initiative";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Coins-spiracy");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.COINSPIRACY, 1);

  public static final CoinmasterData SHAWARMA =
      new CoinmasterData(master, "SHAWARMA", SHAWARMARequest.class)
          .withToken("Coinspiracy")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "si_shop1");

  public SHAWARMARequest() {
    super(SHAWARMA);
  }

  public SHAWARMARequest(final boolean buying, final AdventureResult[] attachments) {
    super(SHAWARMA, buying, attachments);
  }

  public SHAWARMARequest(final boolean buying, final AdventureResult attachment) {
    super(SHAWARMA, buying, attachment);
  }

  public SHAWARMARequest(final boolean buying, final int itemId, final int quantity) {
    super(SHAWARMA, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=si_shop1")) {
      return;
    }

    CoinmasterData data = SHAWARMA;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=si_shop1")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(SHAWARMA, urlString, true);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_spookyAirportToday")
        && !Preferences.getBoolean("spookyAirportAlways")) {
      return "You don't have access to Conspiracy Island";
    }
    if (KoLCharacter.getLimitMode().limitZone("Conspiracy Island")) {
      return "You cannot currently access Conspiracy Island";
    }
    if (!Preferences.getBoolean("SHAWARMAInitiativeUnlocked")) {
      return "SHAWARMA Initiative is locked";
    }
    return null;
  }
}
