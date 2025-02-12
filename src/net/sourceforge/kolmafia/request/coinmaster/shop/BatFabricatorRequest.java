package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.LimitMode;

public abstract class BatFabricatorRequest extends CoinMasterShopRequest {
  public static final String master = "Bat-Fabricator";
  public static final String SHOPID = "batman_cave";

  public static final AdventureResult METAL = ItemPool.get(ItemPool.HIGH_GRADE_METAL, 1);
  public static final AdventureResult FIBERS =
      ItemPool.get(ItemPool.HIGH_TENSILE_STRENGTH_FIBERS, 1);
  public static final AdventureResult EXPLOSIVES = ItemPool.get(ItemPool.HIGH_GRADE_EXPLOSIVES, 1);

  public static final CoinmasterData BAT_FABRICATOR =
      new CoinmasterData(master, "batman_cave", BatFabricatorRequest.class)
          .withShopRowFields(master, SHOPID)
          .withItemBuyPrice(BatFabricatorRequest::itemBuyPrice)
          .withAccessible(BatFabricatorRequest::accessible);

  public static AdventureResult itemBuyPrice(final Integer itemId) {
    int cost = BatManager.hasUpgrade(BatManager.IMPROVED_3D_BAT_PRINTER) ? 2 : 3;
    return switch (itemId) {
      case ItemPool.BAT_OOMERANG -> METAL.getInstance(cost);
      case ItemPool.BAT_JUTE -> FIBERS.getInstance(cost);
      case ItemPool.BAT_O_MITE -> EXPLOSIVES.getInstance(cost);
      default -> null;
    };
  }

  public static String accessible() {
    if (KoLCharacter.getLimitMode() != LimitMode.BATMAN) {
      return "Only Batfellow can use the Bat-Fabricator.";
    }
    if (BatManager.currentBatZone() != BatManager.BAT_CAVERN) {
      return "Batfellow can only use the Bat-Fabricator in the BatCavern.";
    }
    return null;
  }
}
