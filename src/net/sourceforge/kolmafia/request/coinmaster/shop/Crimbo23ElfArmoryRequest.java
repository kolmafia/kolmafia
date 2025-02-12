package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class Crimbo23ElfArmoryRequest extends CoinMasterShopRequest {
  public static final String master = "Elf Guard Armory";
  public static final String SHOPID = "crimbo23_elf_armory";

  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.ELF_ARMY_MACHINE_PARTS, 1);
  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<td>([\\d,]+) piles? of Elf Army machine parts</td>");

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo23_elf_armory", Crimbo23ElfArmoryRequest.class)
          .inZone("Crimbo23")
          .withToken("Elf Army machine parts")
          .withTokenTest("no piles of Elf Army machine parts")
          .withItem(TOKEN)
          .withTokenPattern(TOKEN_PATTERN)
          .withShopRowFields(master, SHOPID)
          // In order to sell 1 item to get 3 piles of Elf Army machine parts,
          // strangely enough, KoL uses "buyitem"
          // shop.php?whichshop=crimbo23_elf_armory&action=buyitem&quantity=1&whichrow=1412&pwd
          .withSellURL("shop.php?whichshop=crimbo23_elf_armory")
          .withSellAction("buyitem")
          .withSellItems(master)
          .withSellPrices(master)
          .withAccessible(Crimbo23ElfArmoryRequest::accessible);

  public static String accessible() {
    return switch (Preferences.getString("crimbo23ArmoryControl")) {
      case "none" -> "CrimboTown is closed";
      case "elf" -> null;
      case "pirate" -> "The pirates control the armory";
      case "contested" -> "The elves and pirates are fighting for control of the armory";
      default -> null;
    };
  }
}
