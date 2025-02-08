package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;

public abstract class MemeShopRequest extends CoinMasterShopRequest {
  public static final String master = "Internet Meme Shop";
  public static final String SHOPID = "bacon";

  private static final Pattern BACON_PATTERN = Pattern.compile("([\\d,]+) BACON");
  public static final AdventureResult BACON = ItemPool.get(ItemPool.BACON, 1);

  public static final CoinmasterData BACON_STORE =
      new CoinmasterData(master, "bacon", MemeShopRequest.class)
          .withToken("BACON")
          .withTokenTest("Where's the bacon?")
          .withTokenPattern(BACON_PATTERN)
          .withItem(BACON)
          .withShopRowFields(master, SHOPID)
          .withCanBuyItem(MemeShopRequest::canBuyItem)
          .withVisitShop(MemeShopRequest::visitShop)
          .withPurchasedItem(MemeShopRequest::purchasedItem);

  private static String itemProperty(final int itemId) {
    return switch (itemId) {
      case ItemPool.VIRAL_VIDEO -> "_internetViralVideoBought";
      case ItemPool.PLUS_ONE -> "_internetPlusOneBought";
      case ItemPool.GALLON_OF_MILK -> "_internetGallonOfMilkBought";
      case ItemPool.PRINT_SCREEN -> "_internetPrintScreenButtonBought";
      case ItemPool.DAILY_DUNGEON_MALWARE -> "_internetDailyDungeonMalwareBought";
      default -> null;
    };
  }

  private static Boolean canBuyItem(final Integer itemId) {
    String property = itemProperty(itemId);
    return (property != null)
        ? !Preferences.getBoolean(property)
        : ItemPool.get(itemId).getCount(BACON_STORE.getBuyItems()) > 0;
  }

  private static void purchasedItem(AdventureResult item, Boolean storage) {
    String property = itemProperty(item.getItemId());
    if (property != null) {
      Preferences.setBoolean(property, true);
    }
  }

  public static void visitShop(String responseText) {
    Preferences.setBoolean("_internetViralVideoBought", !responseText.contains("viral video"));
    Preferences.setBoolean("_internetPlusOneBought", !responseText.contains("plus one"));
    Preferences.setBoolean("_internetGallonOfMilkBought", !responseText.contains("gallon of milk"));
    Preferences.setBoolean(
        "_internetPrintScreenButtonBought", !responseText.contains("print screen button"));
    Preferences.setBoolean(
        "_internetDailyDungeonMalwareBought", !responseText.contains("daily dungeon malware"));
  }
}
