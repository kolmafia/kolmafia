package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public abstract class BlackMarketRequest extends CoinMasterShopRequest {
  public static final String master = "The Black Market";
  public static final String SHOPID = "blackmarket";

  public static final AdventureResult TOKEN = ItemPool.get(ItemPool.PRICELESS_DIAMOND, 1);
  private static final Pattern TOKEN_PATTERN = Pattern.compile("<td>([\\d,]+) priceless diamond");

  public static final CoinmasterData BLACK_MARKET =
      new CoinmasterData(master, "blackmarket", BlackMarketRequest.class)
          .withToken("priceless diamond")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(TOKEN)
          .withShopRowFields(master, SHOPID)
          .withCanBuyItem(BlackMarketRequest::canBuyItem)
          .withVisitShop(BlackMarketRequest::visitShop)
          .withAccessible(BlackMarketRequest::accessible);

  private static Boolean canBuyItem(final Integer itemId) {
    AdventureResult item = ItemPool.get(itemId);
    return switch (itemId) {
      case ItemPool.ZEPPELIN_TICKET -> InventoryManager.getCount(item) == 0;
      default -> item.getCount(BLACK_MARKET.getBuyItems()) > 0;
    };
  }

  public static void visitShop(final String responseText) {
    // If Black Market not already unlocked, unlock it
    if (!QuestLogRequest.isBlackMarketAvailable()) {
      QuestDatabase.setQuestProgress(Quest.MACGUFFIN, "step1");
      ConcoctionDatabase.setRefreshNeeded(true);
    }
  }

  public static String accessible() {
    if (!QuestLogRequest.isBlackMarketAvailable()) {
      return "The Black Market is not currently available";
    }
    return null;
  }
}
