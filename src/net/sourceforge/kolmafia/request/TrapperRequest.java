package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;

public class TrapperRequest extends CoinMasterRequest {
  public static String master = "The Trapper";
  public static LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(TrapperRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(TrapperRequest.master);
  private static final Map<Integer, Integer> itemRows =
      CoinmastersDatabase.getRows(TrapperRequest.master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("([\\d,]+) yeti fur");
  public static final AdventureResult YETI_FUR = ItemPool.get(ItemPool.YETI_FUR, 1);
  public static final CoinmasterData TRAPPER =
      new CoinmasterData(
          TrapperRequest.master,
          "trapper",
          TrapperRequest.class,
          "yeti fur",
          "no yeti furs",
          false,
          TrapperRequest.TOKEN_PATTERN,
          TrapperRequest.YETI_FUR,
          null,
          TrapperRequest.itemRows,
          "shop.php?whichshop=trapper",
          "buyitem",
          TrapperRequest.buyItems,
          TrapperRequest.buyPrices,
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

  public TrapperRequest() {
    super(TrapperRequest.TRAPPER);
  }

  public TrapperRequest(final boolean buying, final AdventureResult[] attachments) {
    super(TrapperRequest.TRAPPER, buying, attachments);
  }

  public TrapperRequest(final boolean buying, final AdventureResult attachment) {
    super(TrapperRequest.TRAPPER, buying, attachment);
  }

  public TrapperRequest(final boolean buying, final int itemId, final int quantity) {
    super(TrapperRequest.TRAPPER, buying, itemId, quantity);
  }

  public TrapperRequest(final int itemId, final int quantity) {
    this(true, itemId, quantity);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    // I'm plumb stocked up on everythin' 'cept yeti furs, Adventurer.
    // If you've got any to trade, I'd be much obliged."
    if (responseText.contains("yeti furs")) {
      Preferences.setInteger("lastTr4pz0rQuest", KoLCharacter.getAscensions());
      QuestDatabase.setQuestProgress(Quest.TRAPPER, QuestDatabase.FINISHED);
    }
    CoinMasterRequest.parseResponse(TrapperRequest.TRAPPER, urlString, responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    // shop.php?pwd&whichshop=trapper
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=trapper")) {
      return false;
    }

    CoinmasterData data = TrapperRequest.TRAPPER;
    return CoinMasterRequest.registerRequest(data, urlString);
  }

  public static String accessible() {
    if (KoLCharacter.getLevel() < 8) {
      return "You haven't met the Trapper yet";
    }
    if (!KoLCharacter.getTrapperQuestCompleted()) {
      return "You have unfinished business with the Trapper";
    }
    if (KoLCharacter.inZombiecore()) {
      return "The trapper won't be back for quite a while";
    }
    return null;
  }
}
