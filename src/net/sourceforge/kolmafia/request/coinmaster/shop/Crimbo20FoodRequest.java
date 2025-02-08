package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class Crimbo20FoodRequest extends CoinMasterShopRequest {
  public static final String master = "Elf Food Drive";
  public static final String SHOPID = "crimbo20food";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("([\\d,]+) (piles of )?donated food");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.DONATED_FOOD, 1);

  public static final CoinmasterData CRIMBO20FOOD =
      new CoinmasterData(master, "crimbo20food", Crimbo20FoodRequest.class)
          .inZone("Crimbo20")
          .withToken("donated food")
          .withTokenTest("no piles of donated food")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, SHOPID)
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
}
