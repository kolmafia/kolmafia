package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class PlumberGearRequest extends CoinMasterRequest {
  public static final String master = "Mushroom District Gear Shop";
  public static final String SHOPID = "mariogear";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("([\\d,]+) coin");
  public static final AdventureResult COIN = ItemPool.get(ItemPool.COIN, 1);

  public static final CoinmasterData PLUMBER_GEAR =
      new CoinmasterData(master, "mariogear", PlumberGearRequest.class)
          .withToken("coin")
          .withTokenTest("no coins")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(COIN)
          .withShopRowFields(master, SHOPID)
          .withAccessible(PlumberGearRequest::accessible);

  public PlumberGearRequest() {
    super(PLUMBER_GEAR);
  }

  public PlumberGearRequest(final boolean buying, final AdventureResult[] attachments) {
    super(PLUMBER_GEAR, buying, attachments);
  }

  public PlumberGearRequest(final boolean buying, final AdventureResult attachment) {
    super(PLUMBER_GEAR, buying, attachment);
  }

  public PlumberGearRequest(final boolean buying, final int itemId, final int quantity) {
    super(PLUMBER_GEAR, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static String accessible() {
    if (!KoLCharacter.isPlumber()) {
      return "You are not a plumber.";
    }
    return null;
  }
}
