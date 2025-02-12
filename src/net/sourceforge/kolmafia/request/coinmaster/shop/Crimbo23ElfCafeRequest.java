package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class Crimbo23ElfCafeRequest extends CoinMasterShopRequest {
  public static final String master = "Elf Guard Mess Hall";
  public static final String SHOPID = "crimbo23_elf_cafe";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("([\\d,]+) Elf Guard MPCs");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.ELF_GUARD_MPC, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo23_elf_cafe", Crimbo23ElfCafeRequest.class)
          .inZone("Crimbo23")
          .withToken("Elf Guard MPC")
          .withTokenTest("no Elf Guard MPCs")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(Crimbo23ElfCafeRequest::accessible);

  public static String accessible() {
    return switch (Preferences.getString("crimbo23CafeControl")) {
      case "none" -> "CrimboTown is closed";
      case "elf" -> null;
      case "pirate" -> "The pirates control the cafe";
      case "contested" -> "The elves and pirates are fighting for control of the cafe";
      default -> null;
    };
  }
}
