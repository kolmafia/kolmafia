package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class Crimbo23PirateArmoryRequest extends CoinMasterRequest {
  public static final String master = "Crimbuccaneer Junkworks";

  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.CRIMBUCCANEER_FLOTSAM, 1);
  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>([\\d,]+) piles? of Crimbuccaneer flotsam</td>");

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo23_pirate_armory", Crimbo23PirateArmoryRequest.class)
          .withToken("Crimbuccaneer flotsam")
          .withTokenTest("no piles of Crimbuccaneer flotsam")
          .withItem(TOKEN)
          .withTokenPattern(TOKEN_PATTERN)
          .withShopRowFields(master, "crimbo23_pirate_armory")
          // In order to sell 1 item to get 3 piles of Crimbuccaneer flotsam,
          // strangely enough, KoL uses "buyitem"
          // shop.php?whichshop=crimbo23_pirate_armory&action=buyitem&quantity=1&whichrow=1420&pwd
          .withSellURL("shop.php?whichshop=crimbo23_pirate_armory")
          .withSellAction("buyitem")
          .withSellItems(master)
          .withSellPrices(master);

  public Crimbo23PirateArmoryRequest() {
    super(DATA);
  }

  public Crimbo23PirateArmoryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DATA, buying, attachments);
  }

  public Crimbo23PirateArmoryRequest(final boolean buying, final AdventureResult attachment) {
    super(DATA, buying, attachment);
  }

  public Crimbo23PirateArmoryRequest(final boolean buying, final int itemId, final int quantity) {
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

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(DATA, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(DATA, responseText);
  }

  public static String accessible() {
    return switch (Preferences.getString("crimbo23ArmoryControl")) {
      case "none" -> "CrimboTown is closed";
      case "elf" -> "The elves control the armory";
      case "pirate" -> null;
      case "contested" -> "The elves and pirates are fighting for control of the armory";
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
