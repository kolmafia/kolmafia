package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class Crimbo23PirateBarRequest extends CoinMasterRequest {
  public static final String master = "Crimbuccaneer Bar";
  public static final String SHOPID = "crimbo23_pirate_bar";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("([\\d,]+) Crimbuccaneer pieces? of 12");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.CRIMBUCCANEER_PIECE_OF_12, 1);

  public static final CoinmasterData DATA =
      new CoinmasterData(master, "crimbo23_pirate_bar", Crimbo23PirateBarRequest.class)
          .inZone("Crimbo23")
          .withToken("Crimbuccaneer piece of 12")
          .withTokenTest("no Crimbuccaneer pieces of 12")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(Crimbo23PirateBarRequest::accessible);

  public Crimbo23PirateBarRequest() {
    super(DATA);
  }

  public Crimbo23PirateBarRequest(final boolean buying, final AdventureResult[] attachments) {
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
      case "none" -> "CrimboTown 2023 is closed";
      case "elf" -> "The elves control the bar";
      case "pirate" -> null;
      case "contested" -> "The elves and pirates are fighting for control of the bar";
      default -> null;
    };
  }
}
