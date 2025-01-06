package net.sourceforge.kolmafia.request.coinmaster.shop;

import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.BatManager;
import net.sourceforge.kolmafia.session.LimitMode;

public class BatFabricatorRequest extends CoinMasterRequest {
  public static final String master = "Bat-Fabricator";

  public static final AdventureResult METAL = ItemPool.get(ItemPool.HIGH_GRADE_METAL, 1);
  public static final AdventureResult FIBERS =
      ItemPool.get(ItemPool.HIGH_TENSILE_STRENGTH_FIBERS, 1);
  public static final AdventureResult EXPLOSIVES = ItemPool.get(ItemPool.HIGH_GRADE_EXPLOSIVES, 1);

  public static final CoinmasterData BAT_FABRICATOR =
      new CoinmasterData(master, "Bat-Fabricator", BatFabricatorRequest.class)
          .withShopRowFields(master, "batman_cave")
          .withItemBuyPrice(BatFabricatorRequest::itemBuyPrice);

  public static AdventureResult itemBuyPrice(final Integer itemId) {
    int cost = BatManager.hasUpgrade(BatManager.IMPROVED_3D_BAT_PRINTER) ? 2 : 3;
    return switch (itemId) {
      case ItemPool.BAT_OOMERANG -> METAL.getInstance(cost);
      case ItemPool.BAT_JUTE -> FIBERS.getInstance(cost);
      case ItemPool.BAT_O_MITE -> EXPLOSIVES.getInstance(cost);
      default -> null;
    };
  }

  public BatFabricatorRequest() {
    super(BAT_FABRICATOR);
  }

  public BatFabricatorRequest(final boolean buying, final AdventureResult[] attachments) {
    super(BAT_FABRICATOR, buying, attachments);
  }

  public BatFabricatorRequest(final boolean buying, final AdventureResult attachment) {
    super(BAT_FABRICATOR, buying, attachment);
  }

  public BatFabricatorRequest(final boolean buying, final int itemId, final int quantity) {
    super(BAT_FABRICATOR, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=batman_cave")) {
      return;
    }

    CoinmasterData data = BAT_FABRICATOR;

    String action = GenericRequest.getAction(urlString);
    if (action != null) {
      CoinMasterRequest.parseResponse(data, urlString, responseText);
      return;
    }

    // Parse current coin balances
    CoinMasterRequest.parseBalance(data, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=batman_cave")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(BAT_FABRICATOR, urlString, true);
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
