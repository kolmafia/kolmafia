package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class Crimbo20FoodRequest extends CoinMasterRequest {
  public static final String master = "Elf Food Drive";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("([\\d,]+) (piles of )?donated food");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.DONATED_FOOD, 1);

  public static final CoinmasterData CRIMBO20FOOD =
      new CoinmasterData(master, "crimbo20food", Crimbo20FoodRequest.class)
          .withToken("donated food")
          .withTokenTest("no piles of donated food")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, "crimbo20food")
          .withNeedsPasswordHash(true)
          .withCanBuyItem(Crimbo20FoodRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    AdventureResult item = ItemPool.get(itemId);
    return switch (itemId) {
      case ItemPool.FOOD_DRIVE_BUTTON, ItemPool.FOOD_MAILING_LIST -> item.getCount(
                  KoLConstants.closet)
              + item.getCount(KoLConstants.inventory)
          == 0;
      default -> item.getCount(CRIMBO20FOOD.getBuyItems()) > 0;
    };
  }

  public Crimbo20FoodRequest() {
    super(CRIMBO20FOOD);
  }

  public Crimbo20FoodRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CRIMBO20FOOD, buying, attachments);
  }

  public Crimbo20FoodRequest(final boolean buying, final AdventureResult attachment) {
    super(CRIMBO20FOOD, buying, attachment);
  }

  public Crimbo20FoodRequest(final boolean buying, final int itemId, final int quantity) {
    super(CRIMBO20FOOD, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String location, final String responseText) {
    if (!location.contains("whichshop=crimbo20food")) {
      return;
    }

    CoinmasterData data = CRIMBO20FOOD;

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

    return CoinMasterRequest.registerRequest(CRIMBO20FOOD, urlString, true);
  }
}
