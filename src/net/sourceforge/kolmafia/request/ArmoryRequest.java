package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public class ArmoryRequest extends CoinMasterRequest {
  public static final String master = "The Armory";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Coins-spiracy");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.COINSPIRACY, 1);

  public static final CoinmasterData ARMORY =
      new CoinmasterData(master, "armory", ArmoryRequest.class)
          .withToken("Coinspiracy")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "si_shop3");

  public ArmoryRequest() {
    super(ARMORY);
  }

  public ArmoryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(ARMORY, buying, attachments);
  }

  public ArmoryRequest(final boolean buying, final AdventureResult attachment) {
    super(ARMORY, buying, attachment);
  }

  public ArmoryRequest(final boolean buying, final int itemId, final int quantity) {
    super(ARMORY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=si_shop3")) {
      return;
    }

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(ARMORY, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(ARMORY, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=si_shop3")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(ARMORY, urlString, true);
  }

  public static String accessible() {
    if (!Preferences.getBoolean("_spookyAirportToday")
        && !Preferences.getBoolean("spookyAirportAlways")) {
      return "You don't have access to Conspiracy Island";
    }
    if (KoLCharacter.getLimitMode().limitZone("Conspiracy Island")) {
      return "You cannot currently access Conspiracy Island";
    }
    if (!Preferences.getBoolean("armoryUnlocked")) {
      return "The Armory is locked";
    }
    return null;
  }
}
