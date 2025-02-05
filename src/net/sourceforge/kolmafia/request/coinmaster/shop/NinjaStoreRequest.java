package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class NinjaStoreRequest extends CoinMasterRequest {
  public static final String master = "Ni&ntilde;a Store";
  public static final String SHOPID = "nina";

  private static final Pattern CHRONER_PATTERN = Pattern.compile("([\\d,]+) Chroner");
  public static final AdventureResult CHRONER = ItemPool.get(ItemPool.CHRONER, 1);

  public static final CoinmasterData NINJA_STORE =
      new CoinmasterData(master, "nina", NinjaStoreRequest.class)
          .withToken("Chroner")
          .withTokenTest("no Chroner")
          .withTokenPattern(CHRONER_PATTERN)
          .withItem(CHRONER)
          .withShopRowFields(master, SHOPID)
          .withVisitShop(NinjaStoreRequest::visitShop)
          .withAccessible(NinjaStoreRequest::accessible);

  public NinjaStoreRequest() {
    super(NINJA_STORE);
  }

  public NinjaStoreRequest(final boolean buying, final AdventureResult[] attachments) {
    super(NINJA_STORE, buying, attachments);
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
      return "You can't get to Ni&ntilde;a Store";
    }
    return null;
  }
}
