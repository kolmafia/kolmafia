package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.shop.ShopRequest;

public class TrapperRequest extends CoinMasterRequest {
  public static final String master = "The Trapper";
  public static final String SHOPID = "trapper";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("([\\d,]+) yeti fur");
  public static final AdventureResult YETI_FUR = ItemPool.get(ItemPool.YETI_FUR, 1);

  public static final CoinmasterData TRAPPER =
      new CoinmasterData(master, "trapper", TrapperRequest.class)
          .withToken("yeti fur")
          .withTokenTest("no yeti furs")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(YETI_FUR)
          .withShopRowFields(master, SHOPID)
          .withBuyItems(master)
          .withVisitShop(TrapperRequest::visitShop)
          .withAccessible(TrapperRequest::accessible);

  public TrapperRequest() {
    super(TRAPPER);
  }

  public TrapperRequest(final boolean buying, final AdventureResult[] attachments) {
    super(TRAPPER, buying, attachments);
  }

  public TrapperRequest(final int itemId, final int quantity) {
    this(true, new AdventureResult[] {ItemPool.get(itemId, quantity)});
  }

  @Override
  public void processResults() {
    ShopRequest.parseResponse(this.getURLString(), this.responseText);
  }

  public static void visitShop(String responseText) {
    // I'm plumb stocked up on everythin' 'cept yeti furs, Adventurer.
    // If you've got any to trade, I'd be much obliged."

    if (responseText.contains("yeti furs")) {
      Preferences.setInteger("lastTr4pz0rQuest", KoLCharacter.getAscensions());
      QuestDatabase.setQuestProgress(Quest.TRAPPER, QuestDatabase.FINISHED);
    }
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
