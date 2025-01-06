package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class Crimbo23PirateFactoryRequest extends CoinMasterRequest {
  public static final String master = "Crimbuccaneer Foundry";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("([\\d,]+) Crimbuccaneer pieces? of 12");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.CRIMBUCCANEER_PIECE_OF_12, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo23_pirate_factory", Crimbo23PirateFactoryRequest.class)
          .withToken("Crimbuccaneer piece of 12")
          .withTokenTest("no Crimbuccaneer pieces of 12")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, "crimbo23_pirate_factory")
          .withNeedsPasswordHash(true);

  public Crimbo23PirateFactoryRequest() {
    super(DATA);
  }

  public Crimbo23PirateFactoryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DATA, buying, attachments);
  }

  public Crimbo23PirateFactoryRequest(final boolean buying, final AdventureResult attachment) {
    super(DATA, buying, attachment);
  }

  public Crimbo23PirateFactoryRequest(final boolean buying, final int itemId, final int quantity) {
    super(DATA, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=" + DATA.getNickname())) {
      return;
    }

    if (responseText.contains("War has consumed this area.")) {
      return;
    }

    CoinmasterData data = DATA;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    return switch (Preferences.getString("crimbo23FoundryControl")) {
      case "none" -> "CrimboTown is closed";
      case "elf" -> "The elves control the factory";
      case "pirate" -> null;
      case "contested" -> "The elves and pirates are fighting for control of the factory";
      default -> null;
    };
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php")
        || !urlString.contains("whichshop=" + DATA.getNickname())) {
      return false;
    }

    return CoinMasterRequest.registerRequest(DATA, urlString, true);
  }
}
