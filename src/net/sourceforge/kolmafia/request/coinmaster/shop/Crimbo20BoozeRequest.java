package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class Crimbo20BoozeRequest extends CoinMasterRequest {
  public static final String master = "Elf Booze Drive";
  public static final String SHOPID = "crimbo20booze";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("([\\d,]+) (boxes of )?donated booze");
  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.DONATED_BOOZE, 1);

  public static final CoinmasterData CRIMBO20BOOZE =
      new CoinmasterData(master, "crimbo20booze", Crimbo20BoozeRequest.class)
          .inZone("Crimbo20")
          .withToken("donated booze")
          .withTokenTest("no boxes of donated booze")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, SHOPID)
          .withCanBuyItem(Crimbo20BoozeRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    AdventureResult item = ItemPool.get(itemId);
    return switch (itemId) {
      case ItemPool.BOOZE_DRIVE_BUTTON, ItemPool.BOOZE_MAILING_LIST -> item.getCount(
                  KoLConstants.closet)
              + item.getCount(KoLConstants.inventory)
          == 0;
      default -> item.getCount(CRIMBO20BOOZE.getBuyItems()) > 0;
    };
  }

  public Crimbo20BoozeRequest() {
    super(CRIMBO20BOOZE);
  }

  public Crimbo20BoozeRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CRIMBO20BOOZE, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }
}
