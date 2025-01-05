package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class Crimbo23ElfCafeRequest extends CoinMasterRequest {
  public static final String master = "Elf Guard Mess Hall";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("([\\d,]+) Elf Guard MPCs");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.ELF_GUARD_MPC, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo23_elf_cafe", Crimbo23ElfCafeRequest.class)
          .withToken("Elf Guard MPC")
          .withTokenTest("no Elf Guard MPCs")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, "crimbo23_elf_cafe")
          .withNeedsPasswordHash(true);

  public Crimbo23ElfCafeRequest() {
    super(DATA);
  }

  public Crimbo23ElfCafeRequest(final boolean buying, final AdventureResult[] attachments) {
    super(DATA, buying, attachments);
  }

  public Crimbo23ElfCafeRequest(final boolean buying, final AdventureResult attachment) {
    super(DATA, buying, attachment);
  }

  public Crimbo23ElfCafeRequest(final boolean buying, final int itemId, final int quantity) {
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
    return switch (Preferences.getString("crimbo23CafeControl")) {
      case "none" -> "CrimboTown is closed";
      case "elf" -> null;
      case "pirate" -> "The pirates control the cafe";
      case "contested" -> "The elves and pirates are fighting for control of the cafe";
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
