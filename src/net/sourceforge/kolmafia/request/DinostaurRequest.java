package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class DinostaurRequest extends CoinMasterRequest {
  public static final String master = "Dino World Gift Shop (The Dinostaur)";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) Dinodollar");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.DINODOLLAR, 1);

  public static final CoinmasterData DINOSTAUR =
      new CoinmasterData(master, "Dinostaur", DinostaurRequest.class)
          .withToken("Dinodollar")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, "dino");

  public DinostaurRequest() {
    super(DINOSTAUR);
  }

  public DinostaurRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DINOSTAUR, buying, attachments);
  }

  public DinostaurRequest(final boolean buying, final AdventureResult attachment) {
    super(DINOSTAUR, buying, attachment);
  }

  public DinostaurRequest(final boolean buying, final int itemId, final int quantity) {
    super(DINOSTAUR, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=dino")) {
      return;
    }

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(DINOSTAUR, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(DINOSTAUR, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=dino")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(DINOSTAUR, urlString, true);
  }

  public static String accessible() {
    if (!KoLCharacter.inDinocore()) {
      return "Dino World is not available";
    }
    return null;
  }
}
