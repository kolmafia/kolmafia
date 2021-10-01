package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

public class MemeShopRequest extends CoinMasterRequest {
  public static final String master = "Internet Meme Shop";

  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(MemeShopRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(MemeShopRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(MemeShopRequest.master);
  private static final Pattern BACON_PATTERN = Pattern.compile("([\\d,]+) BACON");
  public static final AdventureResult BACON = ItemPool.get(ItemPool.BACON, 1);

  public static final CoinmasterData BACON_STORE =
      new CoinmasterData(
          MemeShopRequest.master,
          "bacon",
          MemeShopRequest.class,
          "BACON",
          "Where's the bacon?",
          false,
          MemeShopRequest.BACON_PATTERN,
          MemeShopRequest.BACON,
          null,
          MemeShopRequest.itemRows,
          "shop.php?whichshop=bacon",
          "buyitem",
          MemeShopRequest.buyItems,
          MemeShopRequest.buyPrices,
          null,
          null,
          null,
          null,
          "whichrow",
          GenericRequest.WHICHROW_PATTERN,
          "quantity",
          GenericRequest.QUANTITY_PATTERN,
          null,
          null,
          true) {
        @Override
        public final boolean canBuyItem(final int itemId) {
          switch (itemId) {
            case ItemPool.VIRAL_VIDEO:
              return !Preferences.getBoolean("_internetViralVideoBought");
            case ItemPool.PLUS_ONE:
              return !Preferences.getBoolean("_internetPlusOneBought");
            case ItemPool.GALLON_OF_MILK:
              return !Preferences.getBoolean("_internetGallonOfMilkBought");
            case ItemPool.PRINT_SCREEN:
              return !Preferences.getBoolean("_internetPrintScreenButtonBought");
            case ItemPool.DAILY_DUNGEON_MALWARE:
              return !Preferences.getBoolean("_internetDailyDungeonMalwareBought");
          }
          return super.canBuyItem(itemId);
        }

        @Override
        public void purchaseItem(AdventureResult item, boolean storage) {
          int itemId = item.getItemId();
          switch (itemId) {
            case ItemPool.VIRAL_VIDEO:
              Preferences.setBoolean("_internetViralVideoBought", true);
              break;
            case ItemPool.PLUS_ONE:
              Preferences.setBoolean("_internetPlusOneBought", true);
              break;
            case ItemPool.GALLON_OF_MILK:
              Preferences.setBoolean("_internetGallonOfMilkBought", true);
              break;
            case ItemPool.PRINT_SCREEN:
              Preferences.setBoolean("_internetPrintScreenButtonBought", true);
              break;
            case ItemPool.DAILY_DUNGEON_MALWARE:
              Preferences.setBoolean("_internetDailyDungeonMalwareBought", true);
              break;
          }
        }
      };

  public MemeShopRequest() {
    super(MemeShopRequest.BACON_STORE);
  }

  public MemeShopRequest(final boolean buying, final AdventureResult[] attachments) {
    super(MemeShopRequest.BACON_STORE, buying, attachments);
  }

  public MemeShopRequest(final boolean buying, final AdventureResult attachment) {
    super(MemeShopRequest.BACON_STORE, buying, attachment);
  }

  public MemeShopRequest(final boolean buying, final int itemId, final int quantity) {
    super(MemeShopRequest.BACON_STORE, buying, itemId, quantity);
  }

  @Override
  public void run() {
    if (this.action != null) {
      this.addFormField("pwd");
    }

    super.run();
  }

  @Override
  public void processResults() {
    MemeShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=bacon")) {
      return;
    }

    CoinmasterData data = MemeShopRequest.BACON_STORE;

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

    CoinmasterData data = MemeShopRequest.BACON_STORE;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
