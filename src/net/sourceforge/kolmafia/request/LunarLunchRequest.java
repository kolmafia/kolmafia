package net.sourceforge.kolmafia.request;

import java.util.Map;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;

public class LunarLunchRequest extends CoinMasterRequest {
  public static final String master = "Lunar Lunch-o-Mat";
  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(LunarLunchRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(LunarLunchRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(LunarLunchRequest.master);

  public static final CoinmasterData LUNAR_LUNCH =
      new CoinmasterData(
          LunarLunchRequest.master,
          "lunarlunch",
          LunarLunchRequest.class,
          "isotope",
          "You have 0 lunar isotopes",
          false,
          SpaaaceRequest.TOKEN_PATTERN,
          SpaaaceRequest.ISOTOPE,
          null,
          LunarLunchRequest.itemRows,
          "shop.php?whichshop=elvishp3",
          "buyitem",
          LunarLunchRequest.buyItems,
          LunarLunchRequest.buyPrices,
          null,
          null,
          null,
          null,
          "whichrow",
          GenericRequest.WHICHROW_PATTERN,
          "quantity",
          GenericRequest.QUANTITY_PATTERN,
          null,
          null,
          true);

  public LunarLunchRequest() {
    super(LunarLunchRequest.LUNAR_LUNCH);
  }

  public LunarLunchRequest(final boolean buying, final AdventureResult[] attachments) {
    super(LunarLunchRequest.LUNAR_LUNCH, buying, attachments);
  }

  public LunarLunchRequest(final boolean buying, final AdventureResult attachment) {
    super(LunarLunchRequest.LUNAR_LUNCH, buying, attachment);
  }

  public LunarLunchRequest(final boolean buying, final int itemId, final int quantity) {
    super(LunarLunchRequest.LUNAR_LUNCH, buying, itemId, quantity);
  }

  public static final void buy(final int itemId, final int count) {
    RequestThread.postRequest(new LunarLunchRequest(true, itemId, count));
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || urlString.indexOf("whichshop=elvishp3") == -1) {
      return false;
    }

    CoinmasterData data = LunarLunchRequest.LUNAR_LUNCH;
    return CoinMasterRequest.registerRequest(data, urlString, true);
  }

  public static String accessible() {
    if (!Preferences.getString(Quest.GENERATOR.getPref()).equals(QuestDatabase.FINISHED)) {
      return "You need to repair the Elves' Shield Generator to shop at the Lunar Lunch-o-Mat.";
    }
    return SpaaaceRequest.accessible();
  }

  @Override
  public void equip() {
    SpaaaceRequest.equip();
  }
}
