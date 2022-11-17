package net.sourceforge.kolmafia.request;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;

public class FreeSnackRequest extends CoinMasterRequest {
  public static final String master = "Game Shoppe Snacks";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("You have ([\\d,]+) free snack voucher");
  private static final Pattern SNACK_PATTERN = Pattern.compile("whichsnack=(\\d+)");
  public static final AdventureResult VOUCHER = ItemPool.get(ItemPool.SNACK_VOUCHER, 1);

  public static final CoinmasterData FREESNACKS =
      new CoinmasterData(master, "snacks", FreeSnackRequest.class)
          .withToken("snack voucher")
          .withTokenTest("The teen glances at your snack voucher")
          .withPositiveTest(true)
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(VOUCHER)
          .withBuyURL("gamestore.php")
          .withBuyAction("buysnack")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withItemField("whichsnack")
          .withItemPattern(SNACK_PATTERN);

  public FreeSnackRequest() {
    super(FREESNACKS);
  }

  public FreeSnackRequest(final boolean buying, final AdventureResult[] attachments) {
    super(FREESNACKS, buying, attachments);
  }

  public FreeSnackRequest(final boolean buying, final AdventureResult attachment) {
    super(FREESNACKS, buying, attachment);
  }

  public FreeSnackRequest(final boolean buying, final int itemId, final int quantity) {
    super(FREESNACKS, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    GameShoppeRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseFreeSnackVisit(final String location, final String responseText) {
    if (responseText.indexOf("You acquire") != -1) {
      CoinmasterData data = FREESNACKS;
      CoinMasterRequest.completePurchase(data, location);
    }
  }

  public static final void buy(final int itemId, final int count) {
    RequestThread.postRequest(new FreeSnackRequest(true, itemId, count));
  }

  public static String accessible() {
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    // We only claim gamestore.php?action=buysnack
    if (!urlString.startsWith("gamestore.php")) {
      return false;
    }

    CoinmasterData data = FREESNACKS;
    return CoinMasterRequest.registerRequest(data, urlString);
  }
}
