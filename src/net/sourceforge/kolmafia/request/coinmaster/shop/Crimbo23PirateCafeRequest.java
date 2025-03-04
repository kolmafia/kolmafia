package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class Crimbo23PirateCafeRequest extends CoinMasterShopRequest {
  public static final String master = "Crimbuccaneer Grub Hall";
  public static final String SHOPID = "crimbo23_pirate_cafe";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("([\\d,]+) Crimbuccaneer pieces? of 12");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.CRIMBUCCANEER_PIECE_OF_12, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo23_pirate_cafe", Crimbo23PirateCafeRequest.class)
          .inZone("Crimbo23")
          .withToken("Crimbuccaneer piece of 12")
          .withTokenTest("no Crimbuccaneer pieces of 12")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(Crimbo23PirateCafeRequest::accessible);

  public static String accessible() {
    return switch (Preferences.getString("crimbo23CafeControl")) {
      case "none" -> "CrimboTown is closed";
      case "elf" -> "The elves control the cafe";
      case "pirate" -> null;
      case "contested" -> "The elves and pirates are fighting for control of the cafe";
      default -> null;
    };
  }
}
