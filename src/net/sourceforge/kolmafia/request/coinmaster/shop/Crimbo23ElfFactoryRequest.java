package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class Crimbo23ElfFactoryRequest extends CoinMasterShopRequest {
  public static final String master = "Elf Guard Toy and Munitions Factory";
  public static final String SHOPID = "crimbo23_elf_factory";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("([\\d,]+) Elf Guard MPCs");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.ELF_GUARD_MPC, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo23_elf_factory", Crimbo23ElfFactoryRequest.class)
          .inZone("Crimbo23")
          .withToken("Elf Guard MPC")
          .withTokenTest("no Elf Guard MPCs")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(Crimbo23ElfFactoryRequest::accessible);

  public static String accessible() {
    return switch (Preferences.getString("crimbo23FoundryControl")) {
      case "none" -> "CrimboTown is closed";
      case "elf" -> null;
      case "pirate" -> "The pirates control the factory";
      case "contested" -> "The elves and pirates are fighting for control of the factory";
      default -> null;
    };
  }
}
