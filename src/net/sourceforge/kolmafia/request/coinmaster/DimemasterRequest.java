package net.sourceforge.kolmafia.request.coinmaster;

import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.IslandRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.IslandManager;

public class DimemasterRequest extends CoinMasterRequest {
  public static final String master = "Dimemaster";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("You've.*?got ([\\d,]+) dime");

  public static final CoinmasterData HIPPY =
      new CoinmasterData(master, "dimemaster", DimemasterRequest.class)
          .withToken("dime")
          .withTokenTest("You don't have any dimes")
          .withTokenPattern(TOKEN_PATTERN)
          .withProperty("availableDimes")
          .withBuyURL("bigisland.php?place=camp&whichcamp=1")
          .withBuyAction("getgear")
          .withBuyItems(master)
          .withBuyPrices(master)
          .withSellURL("bigisland.php?place=camp&whichcamp=1")
          .withSellAction("turnin")
          .withSellItems(master)
          .withSellPrices(master)
          .withItemField("whichitem")
          .withItemPattern(GenericRequest.WHICHITEM_PATTERN)
          .withCountField("quantity")
          .withCountPattern(GenericRequest.QUANTITY_PATTERN)
          .withCanBuyItem(DimemasterRequest::canBuyItem);

  private static Boolean canBuyItem(final Integer itemId) {
    return switch (itemId) {
      case ItemPool.PATCHOULI_OIL_BOMB, ItemPool.EXPLODING_HACKY_SACK -> Preferences.getString(
              "sidequestLighthouseCompleted")
          .equals("hippy");
      default -> ItemPool.get(itemId).getCount(HIPPY.getBuyItems()) > 0;
    };
  }

  public DimemasterRequest() {
    super(HIPPY);
  }

  public DimemasterRequest(final boolean buying, final AdventureResult[] attachments) {
    super(HIPPY, buying, attachments);
  }

  public DimemasterRequest(final boolean buying, final AdventureResult attachment) {
    super(HIPPY, buying, attachment);
  }

  public DimemasterRequest(final boolean buying, final int itemId, final int quantity) {
    super(HIPPY, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    CoinMasterRequest.parseResponse(HIPPY, this.getURLString(), this.responseText);
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("bigisland.php") || urlString.indexOf("whichcamp=1") == -1) {
      return false;
    }

    IslandRequest.lastCampVisited = HIPPY;
    return CoinMasterRequest.registerRequest(HIPPY, urlString);
  }

  public static String accessible() {
    if (!IslandManager.warProgress().equals("started")) {
      return "You're not at war.";
    }

    if (!EquipmentManager.hasOutfit(OutfitPool.WAR_HIPPY_OUTFIT)) {
      return "You don't have the War Hippy Fatigues";
    }

    return null;
  }

  @Override
  public void equip() {
    if (!EquipmentManager.isWearingOutfit(OutfitPool.WAR_HIPPY_OUTFIT)) {
      SpecialOutfit outfit = EquipmentDatabase.getOutfit(OutfitPool.WAR_HIPPY_OUTFIT);
      EquipmentRequest request = new EquipmentRequest(outfit);
      RequestThread.postRequest(request);
    }
  }
}
