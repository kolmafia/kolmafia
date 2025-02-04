package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class Crimbo23ElfBarRequest extends CoinMasterRequest {
  public static final String master = "Elf Guard Officers' Club";
  public static final String SHOPID = "crimbo23_elf_bar";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("([\\d,]+) Elf Guard MPCs");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.ELF_GUARD_MPC, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo23_elf_bar", Crimbo23ElfBarRequest.class)
          .inZone("Crimbo23")
          .withToken("Elf Guard MPC")
          .withTokenTest("no Elf Guard MPCs")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(Crimbo23ElfBarRequest::accessible);

  public Crimbo23ElfBarRequest() {
    super(DATA);
  }

  public Crimbo23ElfBarRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DATA, buying, attachments);
  }

  @Override
  public void processResults() {
    String responseText = this.responseText;
    if (!responseText.contains("War has consumed this area.")) {
      ShopRequest.parseResponse(this.getURLString(), responseText);
    }
  }

  public static String accessible() {
    return switch (Preferences.getString("crimbo23BarControl")) {
      case "none" -> "CrimboTown is closed";
      case "elf" -> null;
      case "pirate" -> "The pirates control the bar";
      case "contested" -> "The elves and pirates are fighting for control of the bar";
      default -> null;
    };
  }
}
