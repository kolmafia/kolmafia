package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class MemeShopRequest extends CoinMasterRequest {
  public static final String master = "Internet Meme Shop";

  private static final Pattern BACON_PATTERN = Pattern.compile("([\\d,]+) BACON");
  public static final AdventureResult BACON = ItemPool.get(ItemPool.BACON, 1);

  public static final CoinmasterData BACON_STORE =
      new CoinmasterData(master, "bacon", MemeShopRequest.class) {}.withToken("BACON")
          .withTokenTest("Where's the bacon?")
          .withTokenPattern(BACON_PATTERN)
          .withItem(BACON)
          .withShopRowFields(master, "bacon")
          .withNeedsPasswordHash(true)
          .withCanBuyItem(MemeShopRequest::canBuyItem)
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

  public MemeShopRequest() {
    super(BACON_STORE);
  }

  public MemeShopRequest(final boolean buying, final AdventureResult[] attachments) {
    super(BACON_STORE, buying, attachments);
  }

  public MemeShopRequest(final boolean buying, final AdventureResult attachment) {
    super(BACON_STORE, buying, attachment);
  }

  public MemeShopRequest(final boolean buying, final int itemId, final int quantity) {
    super(BACON_STORE, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=bacon")) {
      return;
    }

    CoinmasterData data = BACON_STORE;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    Preferences.setBoolean("_internetViralVideoBought", !responseText.contains("viral video"));
    Preferences.setBoolean("_internetPlusOneBought", !responseText.contains("plus one"));
    Preferences.setBoolean("_internetGallonOfMilkBought", !responseText.contains("gallon of milk"));
    Preferences.setBoolean(
        "_internetPrintScreenButtonBought", !responseText.contains("print screen button"));
    Preferences.setBoolean(
        "_internetDailyDungeonMalwareBought", !responseText.contains("daily dungeon malware"));

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=bacon")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(BACON_STORE, urlString, true);
  }
}
