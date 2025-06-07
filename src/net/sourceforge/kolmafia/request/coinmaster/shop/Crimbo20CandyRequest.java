package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public abstract class Crimbo20CandyRequest extends CoinMasterShopRequest {
  public static final String master = "Elf Candy Drive";
  public static final String SHOPID = "crimbo20candy";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("([\\d,]+) (boxes of )?donated candy");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.DONATED_CANDY, 1);

  public static final CoinmasterData CRIMBO20CANDY =
      new CoinmasterData(master, "crimbo20candy", Crimbo20CandyRequest.class)
          .inZone("Crimbo20")
          .withToken("donated candy")
          .withTokenTest("no boxes of donated candy")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, SHOPID)
          .withCanBuyItem(Crimbo20CandyRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    AdventureResult item = ItemPool.get(itemId);
    return switch (itemId) {
      case ItemPool.CANDY_DRIVE_BUTTON, ItemPool.CANDY_MAILING_LIST -> item.getCount(
                  KoLConstants.closet)
              + item.getCount(KoLConstants.inventory)
          == 0;
      default -> item.getCount(CRIMBO20CANDY.getBuyItems()) > 0;
    };
  }
}
