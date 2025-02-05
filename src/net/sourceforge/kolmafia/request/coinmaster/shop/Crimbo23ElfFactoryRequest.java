package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class Crimbo23ElfFactoryRequest extends CoinMasterRequest {
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

  public Crimbo23ElfFactoryRequest() {
    super(DATA);
  }

  public Crimbo23ElfFactoryRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DATA, buying, attachments);
  }

  @Override
  public void processResults() {
    String responseText = this.responseText;
    if (!responseText.contains("War has consumed this area.")) {
      ShopRequest.parseResponse(this.getURLString(), this.responseText);
    }
  }

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
