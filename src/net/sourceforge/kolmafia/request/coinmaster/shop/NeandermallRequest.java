package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.QuestManager;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class NeandermallRequest extends CoinMasterRequest {
  public static final String master = "The Neandermall";
  public static final String SHOPID = "caveshop";

  private static final Pattern CHRONER_PATTERN = Pattern.compile("([\\d,]+) Chroner");
  public static final AdventureResult CHRONER = ItemPool.get(ItemPool.CHRONER, 1);

  public static final CoinmasterData NEANDERMALL =
      new CoinmasterData(master, "caveshop", NeandermallRequest.class)
          .withToken("Chroner")
          .withTokenTest("no Chroner")
          .withTokenPattern(CHRONER_PATTERN)
          .withItem(CHRONER)
          .withShopRowFields(master, SHOPID)
          .withVisitShop(NeandermallRequest::visitShop)
          .withAccessible(NeandermallRequest::accessible);

  public NeandermallRequest() {
    super(NEANDERMALL);
  }

  public NeandermallRequest(final boolean buying, final AdventureResult[] attachments) {
    super(NEANDERMALL, buying, attachments);
  }

  public NeandermallRequest(final boolean buying, final AdventureResult attachment) {
    super(NEANDERMALL, buying, attachment);
  }

  public NeandermallRequest(final boolean buying, final int itemId, final int quantity) {
    super(NEANDERMALL, buying, itemId, quantity);
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
      return "You can't get to the Neandermall";
    }
    return null;
  }
}
