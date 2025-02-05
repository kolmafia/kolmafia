package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class YeNeweSouvenirShoppeRequest extends CoinMasterRequest {
  public static final String master = "Ye Newe Souvenir Shoppe";
  public static final String SHOPID = "shakeshop";

  private static final Pattern CHRONER_PATTERN = Pattern.compile("([\\d,]+) Chroner");
  public static final AdventureResult CHRONER = ItemPool.get(ItemPool.CHRONER, 1);

  public static final CoinmasterData SHAKE_SHOP =
      new CoinmasterData(master, "shakeshop", YeNeweSouvenirShoppeRequest.class)
          .withToken("Chroner")
          .withTokenTest("no Chroner")
          .withTokenPattern(CHRONER_PATTERN)
          .withItem(CHRONER)
          .withShopRowFields(master, SHOPID)
          .withVisitShop(YeNeweSouvenirShoppeRequest::visitShop)
          .withAccessible(YeNeweSouvenirShoppeRequest::accessible);

  public YeNeweSouvenirShoppeRequest() {
    super(SHAKE_SHOP);
  }

  public YeNeweSouvenirShoppeRequest(final boolean buying, final AdventureResult[] attachments) {
    super(SHAKE_SHOP, buying, attachments);
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void visitShop(final String responseText) {
    QuestManager.handleTimeTower(!responseText.contains("That store isn't there anymore."));
  }

  public static String accessible() {
    if (!Preferences.getBoolean("timeTowerAvailable")) {
      return "You can't get to Ye Newe Souvenir Shoppe";
    }
    return null;
  }
}
