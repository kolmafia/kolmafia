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
      CoinmastersDatabase.getBuyItems(QuartersmasterRequest.master);
  private static final Map<Integer, Integer> buyPrices =
      CoinmastersDatabase.getBuyPrices(QuartersmasterRequest.master);
  private static final LockableListModel<AdventureResult> sellItems =
      CoinmastersDatabase.getSellItems(QuartersmasterRequest.master);
  private static final Map<Integer, Integer> sellPrices =
      CoinmastersDatabase.getSellPrices(QuartersmasterRequest.master);

  private static final Pattern TOKEN_PATTERN = Pattern.compile("You've.*?got ([\\d,]+) quarter");
  public static final CoinmasterData FRATBOY =
      new CoinmasterData(
          "Quartersmaster",
          "quartersmaster",
          QuartersmasterRequest.class,
          "quarter",
          "You don't have any quarters",
          false,
          QuartersmasterRequest.TOKEN_PATTERN,
          null,
          "availableQuarters",
          null,
          "bigisland.php?place=camp&whichcamp=2",
          "getgear",
          QuartersmasterRequest.buyItems,
          QuartersmasterRequest.buyPrices,
          "bigisland.php?place=camp&whichcamp=2",
          "turnin",
          QuartersmasterRequest.sellItems,
          QuartersmasterRequest.sellPrices,
          "whichitem",
          GenericRequest.WHICHITEM_PATTERN,
          "quantity",
          GenericRequest.QUANTITY_PATTERN,
          null,
          null,
          true) {
        @Override
        public final boolean canBuyItem(final int itemId) {
          switch (itemId) {
            case ItemPool.TEQUILA_GRENADE:
            case ItemPool.MOLOTOV_COCKTAIL_COCKTAIL:
              return Preferences.getString("sidequestLighthouseCompleted").equals("fratboy");
          }
          return super.canBuyItem(itemId);
        }
      };

  static {
    ConcoctionPool.set(new Concoction("quarter", "availableQuarters"));
  }

  public QuartersmasterRequest() {
    super(QuartersmasterRequest.FRATBOY);
  }

  public QuartersmasterRequest(final boolean buying, final AdventureResult[] attachments) {
    super(QuartersmasterRequest.FRATBOY, buying, attachments);
  }

  public QuartersmasterRequest(final boolean buying, final AdventureResult attachment) {
    super(QuartersmasterRequest.FRATBOY, buying, attachment);
  }

  public QuartersmasterRequest(final boolean buying, final int itemId, final int quantity) {
    super(QuartersmasterRequest.FRATBOY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    CoinMasterRequest.parseResponse(
        QuartersmasterRequest.FRATBOY, this.getURLString(), this.responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("bigisland.php") || urlString.indexOf("whichcamp=2") == -1) {
      return false;
    }

    CoinmasterData data = QuartersmasterRequest.FRATBOY;
    IslandRequest.lastCampVisited = data;
    return CoinMasterRequest.registerRequest(data, urlString);
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
