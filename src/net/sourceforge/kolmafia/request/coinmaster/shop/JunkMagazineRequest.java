package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.session.InventoryManager;

public abstract class JunkMagazineRequest extends CoinMasterShopRequest {
  public static final String master = "Worse Homes and Gardens";
  public static final String SHOPID = "junkmagazine";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, JunkMagazineRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withVisitShop(JunkMagazineRequest::visitShop)
          .withAccessible(JunkMagazineRequest::accessible);

  public static String accessible() {
    if (InventoryManager.hasItem(ItemPool.WORSE_HOMES_GARDENS)) {
      return null;
    }
    return "You can't make that without a copy of Worse Homes and Gardens.";
  }

  public static void visitShop(final String responseText) {
    if (!QuestDatabase.isQuestLaterThan(Quest.HIPPY, "step1")) {
      QuestDatabase.setQuestProgress(Quest.HIPPY, "step2");
    }
  }
}
