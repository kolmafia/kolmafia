package net.sourceforge.kolmafia.request;

import java.util.Map;
import java.util.regex.Pattern;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.IslandManager;

public class QuartersmasterRequest extends CoinMasterRequest {
  public static final String master = "Quartersmaster";

  private static final LockableListModel<AdventureResult> buyItems =
      CoinmastersDatabase.getBuyItems(master);
  private static final Map<Integer, Integer> buyPrices = CoinmastersDatabase.getBuyPrices(master);
  private static final LockableListModel<AdventureResult> sellItems =
      CoinmastersDatabase.getSellItems(master);
  private static final Map<Integer, Integer> sellPrices = CoinmastersDatabase.getSellPrices(master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("You've.*?got ([\\d,]+) quarter");
  public static final CoinmasterData FRATBOY =
      new CoinmasterData(master, "quartersmaster", QuartersmasterRequest.class) {
        @Override
        public final boolean canBuyItem(final int itemId) {
          return switch (itemId) {
            case ItemPool.TEQUILA_GRENADE, ItemPool.MOLOTOV_COCKTAIL_COCKTAIL -> Preferences
                .getString("sidequestLighthouseCompleted")
                .equals("fratboy");
            default -> super.canBuyItem(itemId);
          };
        }
      }.withToken("quarter")
          .withTokenTest("You don't have any quarters")
          .withTokenPattern(TOKEN_PATTERN)
          .withProperty("availableQuarters")
          .withBuyURL("bigisland.php?place=camp&whichcamp=2")
          .withBuyAction("getgear")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withSellURL("bigisland.php?place=camp&whichcamp=2")
          .withSellAction("turnin")
          .withSellItems(sellItems)
          .withSellPrices(sellPrices)
          .withItemField("whichitem")
          .withItemPattern(GenericRequest.WHICHITEM_PATTERN)
          .withCountField("quantity")
          .withCountPattern(GenericRequest.QUANTITY_PATTERN);

  static {
    ConcoctionPool.set(new Concoction("quarter", "availableQuarters"));
  }

  public QuartersmasterRequest() {
    super(FRATBOY);
  }

  public QuartersmasterRequest(final boolean buying, final AdventureResult[] attachments) {
    super(FRATBOY, buying, attachments);
  }

  public QuartersmasterRequest(final boolean buying, final AdventureResult attachment) {
    super(FRATBOY, buying, attachment);
  }

  public QuartersmasterRequest(final boolean buying, final int itemId, final int quantity) {
    super(FRATBOY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    CoinMasterRequest.parseResponse(FRATBOY, this.getURLString(), this.responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("bigisland.php") || urlString.indexOf("whichcamp=2") == -1) {
      return false;
    }

    IslandRequest.lastCampVisited = FRATBOY;
    return CoinMasterRequest.registerRequest(FRATBOY, urlString);
  }

  public static String accessible() {
    if (!IslandManager.warProgress().equals("started")) {
      return "You're not at war.";
    }

    if (!EquipmentManager.hasOutfit(OutfitPool.WAR_FRAT_OUTFIT)) {
      return "You don't have the Frat Warrior Fatigues";
    }

    return null;
  }

  @Override
  public void equip() {
    if (!EquipmentManager.isWearingOutfit(OutfitPool.WAR_FRAT_OUTFIT)) {
      SpecialOutfit outfit = EquipmentDatabase.getOutfit(OutfitPool.WAR_FRAT_OUTFIT);
      EquipmentRequest request = new EquipmentRequest(outfit);
      RequestThread.postRequest(request);
    }
  }
}
