package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class Crimbo20CandyRequest extends CoinMasterRequest {
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

  public Crimbo20CandyRequest() {
    super(CRIMBO20CANDY);
  }

  public Crimbo20CandyRequest(final boolean buying, final AdventureResult[] attachments) {
    super(CRIMBO20CANDY, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }
}
