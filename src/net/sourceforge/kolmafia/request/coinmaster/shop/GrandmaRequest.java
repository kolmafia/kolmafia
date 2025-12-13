package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;

public abstract class GrandmaRequest extends CoinMasterShopRequest {
  public static final String master = "Grandma Sea Monkey";
  public static final String SHOPID = "grandma";

  public static final CoinmasterData DATA =
      new CoinmasterData(master, SHOPID, GrandmaRequest.class)
          .withNewShopRowFields(master, SHOPID)
          .withAccessible(GrandmaRequest::accessible);

  public static String accessible() {
    if (QuestDatabase.isQuestLaterThan(Quest.SEA_MONKEES, "step8")) {
      return null;
    }
    return "You must rescue Grandma first.";
  }
}
