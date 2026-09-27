package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;

public abstract class FiveDPrinterRequest extends CoinMasterShopRequest {
  public static final String master = "Xiblaxian 5D printer";
  public static final String SHOPID = "5dprinter";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, FiveDPrinterRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(FiveDPrinterRequest::accessible)
          .withCanBuyItem(FiveDPrinterRequest::canBuyItem)
          .withVisitShop(FiveDPrinterRequest::visitShop);

  public static String accessible() {
    if (InventoryManager.getCount(ItemPool.FIVE_D_PRINTER) == 0) {
      return "You do not have a Xiblaxian 5D printer.";
    }
    return null;
  }

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.XIBLAXIAN_STEALTH_COWL,
          ItemPool.XIBLAXIAN_STEALTH_TROUSERS,
          ItemPool.XIBLAXIAN_STEALTH_VEST,
          ItemPool.XIBLAXIAN_ULTRABURRITO,
          ItemPool.XIBLAXIAN_SPACE_WHISKEY,
          ItemPool.XIBLAXIAN_RESIDENCE_CUBE,
          ItemPool.XIBLAXIAN_XENO_GOGGLES ->
          !Preferences.getBoolean("unknownRecipe" + itemId);
      default -> DATA.availableItem(itemId);
    };
  }

  private static final Pattern DISCOVERY_PATTERN = Pattern.compile("descitem\\((\\d+)\\)");

  public static void visitShop(final String responseText) {
    Matcher matcher = FiveDPrinterRequest.DISCOVERY_PATTERN.matcher(responseText);
    while (matcher.find()) {
      int id = ItemDatabase.getItemIdFromDescription(matcher.group(1));
      String pref = "unknownRecipe" + id;
      if (id > 0 && Preferences.getBoolean(pref)) {
        KoLmafia.updateDisplay("You know the recipe for " + ItemDatabase.getItemName(id));
        Preferences.setBoolean(pref, false);
      }
    }
  }
}
