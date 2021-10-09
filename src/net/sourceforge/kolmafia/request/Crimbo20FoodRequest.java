package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;

public class Crimbo20FoodRequest extends CoinMasterRequest {
  public static final String master = "Elf Food Drive";

  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(Crimbo20FoodRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(Crimbo20FoodRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(Crimbo20FoodRequest.master);
  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("([\\d,]+) (piles of )?donated food");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.DONATED_FOOD, 1);

  public static final CoinmasterData CRIMBO20FOOD =
      new CoinmasterData(
          Crimbo20FoodRequest.master,
          "crimbo20food",
          Crimbo20FoodRequest.class,
          "donated food",
          "no piles of donated food",
          false,
          Crimbo20FoodRequest.TOKEN_PATTERN,
          Crimbo20FoodRequest.TOKEN,
          null,
          Crimbo20FoodRequest.itemRows,
          "shop.php?whichshop=crimbo20food",
          "buyitem",
          Crimbo20FoodRequest.buyItems,
          Crimbo20FoodRequest.buyPrices,
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
            case ItemPool.FOOD_DRIVE_BUTTON:
            case ItemPool.FOOD_MAILING_LIST:
              AdventureResult item = ItemPool.get(itemId);
              return item.getCount(KoLConstants.closet) + item.getCount(KoLConstants.inventory)
                  == 0;
          }
          return super.canBuyItem(itemId);
        }
      };

  public Crimbo20FoodRequest() {
    super(Crimbo20FoodRequest.CRIMBO20FOOD);
  }

  public Crimbo20FoodRequest(final boolean buying, final AdventureResult[] attachments) {
    super(Crimbo20FoodRequest.CRIMBO20FOOD, buying, attachments);
  }

  public Crimbo20FoodRequest(final boolean buying, final AdventureResult attachment) {
    super(Crimbo20FoodRequest.CRIMBO20FOOD, buying, attachment);
  }

  public Crimbo20FoodRequest(final boolean buying, final int itemId, final int quantity) {
    super(Crimbo20FoodRequest.CRIMBO20FOOD, buying, itemId, quantity);
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
    Crimbo20FoodRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=crimbo20food")) {
      return;
    }

    CoinmasterData data = Crimbo20FoodRequest.CRIMBO20FOOD;

    String action = GenericRequest.getAction(location);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, location, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static String accessible() {
    return "Crimbo is gone";
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=crimbo20food")) {
      return false;
    }

    CoinmasterData data = Crimbo20FoodRequest.CRIMBO20FOOD;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }
}
