package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class AppleStoreRequest extends CoinMasterRequest {
  public static final String master = "The Applecalypse Store";
  public static final String SHOPID = "applestore";

  private static final Pattern CHRONER_PATTERN = Pattern.compile("([\\d,]+) Chroner");
  public static final AdventureResult CHRONER = ItemPool.get(ItemPool.CHRONER, 1);

  public static final CoinmasterData APPLE_STORE =
      new CoinmasterData(master, "applestore", AppleStoreRequest.class)
          .withToken("Chroner")
          .withTokenTest("no Chroner")
          .withTokenPattern(CHRONER_PATTERN)
          .withItem(CHRONER)
          .withShopRowFields(master, SHOPID)
          .withVisitShop(AppleStoreRequest::visitShop)
          .withAccessible(AppleStoreRequest::accessible);

  public AppleStoreRequest() {
    super(APPLE_STORE);
  }

  public AppleStoreRequest(final boolean buying, final AdventureResult[] attachments) {
    super(APPLE_STORE, buying, attachments);
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
      return "You can't get to The Applecalypse Store";
    }
    return null;
  }
}
