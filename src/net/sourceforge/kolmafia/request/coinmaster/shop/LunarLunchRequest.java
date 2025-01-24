package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.request.SpaaaceRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;

public class LunarLunchRequest extends CoinMasterRequest {
  public static final String master = "Lunar Lunch-o-Mat";

  public static final CoinmasterData LUNAR_LUNCH =
      new CoinmasterData(master, "lunarlunch", LunarLunchRequest.class)
          .withToken("isotope")
          .withTokenTest("You have 0 lunar isotopes")
          .withTokenPattern(SpaaaceRequest.TOKEN_PATTERN)
          .withItem(SpaaaceRequest.ISOTOPE)
          .withShopRowFields(master, "elvishp3");

  public LunarLunchRequest() {
    super(LUNAR_LUNCH);
  }

  public LunarLunchRequest(final boolean buying, final AdventureResult[] attachments) {
    super(LUNAR_LUNCH, buying, attachments);
  }

  public LunarLunchRequest(final boolean buying, final AdventureResult attachment) {
    super(LUNAR_LUNCH, buying, attachment);
  }

  public LunarLunchRequest(final boolean buying, final int itemId, final int quantity) {
    super(LUNAR_LUNCH, buying, itemId, quantity);
  }

  public static final void buy(final int itemId, final int count) {
    RequestThread.postRequest(new LunarLunchRequest(true, itemId, count));
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || urlString.indexOf("whichshop=elvishp3") == -1) {
      return false;
    }

    return CoinMasterRequest.registerRequest(LUNAR_LUNCH, urlString, true);
  }

  public static String accessible() {
    if (!QuestDatabase.isQuestFinished(Quest.GENERATOR)) {
      return "You need to repair the Elves' Shield Generator to shop at the Lunar Lunch-o-Mat.";
    }
    return SpaaaceRequest.accessible();
  }

  @Override
  public void equip() {
    SpaaaceRequest.equip();
  }
}
