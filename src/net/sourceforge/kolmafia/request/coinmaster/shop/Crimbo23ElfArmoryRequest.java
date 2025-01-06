package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class Crimbo23ElfArmoryRequest extends CoinMasterRequest {
  public static final String master = "Elf Guard Armory";

  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.ELF_ARMY_MACHINE_PARTS, 1);
  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>([\\d,]+) piles? of Elf Army machine parts</td>");

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo23_elf_armory", Crimbo23ElfArmoryRequest.class)
          .withToken("Elf Army machine parts")
          .withTokenTest("no piles of Elf Army machine parts")
          .withItem(TOKEN)
          .withTokenPattern(TOKEN_PATTERN)
          .withShopRowFields(master, "crimbo23_elf_armory")
          // In order to sell 1 item to get 3 piles of Elf Army machine parts,
          // strangely enough, KoL uses "buyitem"
          // shop.php?whichshop=crimbo23_elf_armory&action=buyitem&quantity=1&whichrow=1412&pwd
          .withSellURL("shop.php?whichshop=crimbo23_elf_armory")
          .withSellAction("buyitem")
          .withSellItems(master)
          .withSellPrices(master);

  public Crimbo23ElfArmoryRequest() {
    super(DATA);
  }

  public Crimbo23ElfArmoryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DATA, buying, attachments);
  }

  public Crimbo23ElfArmoryRequest(final boolean buying, final AdventureResult attachment) {
    super(DATA, buying, attachment);
  }

  public Crimbo23ElfArmoryRequest(final boolean buying, final int itemId, final int quantity) {
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
      case "elf" -> null;
      case "pirate" -> "The pirates control the armory";
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
