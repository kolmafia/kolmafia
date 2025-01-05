package net.sourceforge.kolmafia.request.coinmaster.shop;

import java.util.Map;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.coinmaster.CoinMasterRequest;
import net.sourceforge.kolmafia.session.InventoryManager;

public class FunALogRequest extends CoinMasterRequest {
  public static final String master = "PirateRealm Fun-a-Log";

  private static final Pattern TOKEN_PATTERN =
      Pattern.compile("<b>You have ([\\d,]+) FunPoints?\\.</b>");

  public static final CoinmasterData FUN_A_LOG =
      new CoinmasterData(master, "Fun-a-Log", FunALogRequest.class)
          .withToken("FunPoint")
          .withTokenTest("You have no FunPoints")
          .withTokenPattern(TOKEN_PATTERN)
          .withProperty("availableFunPoints")
          .withShopRowFields(master, "piraterealm")
          .withAvailableItem(FunALogRequest::availableItem);

  private static final Map<Integer, String> ITEM_TO_UNLOCK_PREF =
      Map.ofEntries(
          Map.entry(ItemPool.PR_CRABSICLE, "pirateRealmUnlockedCrabsicle"),
          Map.entry(ItemPool.PR_DARK_RHUM, "pirateRealmUnlockedRhum"),
          Map.entry(ItemPool.PR_EXTRA_DARK_RHUM, "pirateRealmUnlockedRhum"),
          Map.entry(ItemPool.PR_SUPER_EXTRA_DARK_RHUM, "pirateRealmUnlockedRhum"),
          Map.entry(ItemPool.PR_SHAVING_CREAM, "pirateRealmUnlockedShavingCream"),
          Map.entry(ItemPool.PR_BREASTPLATE, "pirateRealmUnlockedBreastplate"),
          Map.entry(ItemPool.PR_RADIO_RING, "pirateRealmUnlockedRadioRing"),
          Map.entry(ItemPool.ISLAND_DRINKIN, "pirateRealmUnlockedTikiSkillbook"),
          Map.entry(ItemPool.PR_TATTOO, "pirateRealmUnlockedTattoo"),
          Map.entry(ItemPool.PR_PLUSHIE, "pirateRealmUnlockedPlushie"),
          Map.entry(ItemPool.PIRATE_FORK, "pirateRealmUnlockedFork"),
          Map.entry(ItemPool.SCURVY_AND_SOBRIETY_PREVENTION, "pirateRealmUnlockedScurvySkillbook"),
          Map.entry(ItemPool.LUCKY_GOLD_RING, "pirateRealmUnlockedGoldRing"),
          Map.entry(ItemPool.PR_BLUNDERBUSS, "pirateRealmUnlockedBlunderbuss"));

  private static Boolean availableItem(final Integer itemId) {
    var pref = ITEM_TO_UNLOCK_PREF.getOrDefault(itemId, null);
    return pref == null || Preferences.getBoolean(pref);
  }

  public FunALogRequest() {
    super(FUN_A_LOG);
  }

  public FunALogRequest(final boolean buying, final AdventureResult[] attachments) {
    super(FUN_A_LOG, buying, attachments);
  }

  public FunALogRequest(final boolean buying, final AdventureResult attachment) {
    super(FUN_A_LOG, buying, attachment);
  }

  public FunALogRequest(final boolean buying, final int itemId, final int quantity) {
    super(FUN_A_LOG, buying, itemId, quantity);
  }

  @Override
  public void processResults() {
    parseResponse(this.getURLString(), this.responseText);
  }

  // <tr rel="10231"><td valign=center><input type=radio name=whichrow value=1064></td><td><img
  // src="https://s3.amazonaws.com/images.kingdomofloathing.com/itemimages/pr_partyhat.gif"
  // class="hand pop" rel="desc_item.php?whichitem=971293634"
  // onClick='javascript:descitem(971293634)'></td><td valign=center><a
  // onClick='javascript:descitem(971293634)'><b>PirateRealm party
  // hat</b>&nbsp;&nbsp;&nbsp;&nbsp;</a></td><td>F</td><td><b>20</b>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td></td><td>&nbsp;&nbsp;</td><td valign=center><input class="button doit multibuy "  type=button rel='shop.php?whichshop=piraterealm&action=buyitem&quantity=1&whichrow=1064&pwd=5f195b385cbe62956e089308af45f544' value='Buy'></td></tr>

  private static final Pattern ITEM_PATTERN =
      Pattern.compile(
          "<tr rel=\"(\\d+)\">.*?whichrow value=(\\d+)>.*?desc_item.php\\?whichitem=(\\d+).*?<b>(.*?)</b>.*?<td>F</td><td><b>([,\\d]+)</b>");

  public static void parseResponse(final String urlString, final String responseText) {
    if (!urlString.contains("whichshop=piraterealm")) {
      return;
    }

    // Check Fun-a-Log item unlock status from visiting the shop
    var unlocked =
        ITEM_PATTERN
            .matcher(responseText)
            .results()
            .map(m -> Integer.parseInt(m.group(1)))
            .toList();
    ITEM_TO_UNLOCK_PREF.forEach(
        (itemId, pref) -> Preferences.setBoolean(pref, unlocked.contains(itemId)));

    // Register the purchase requests, now that we know what is available
    FUN_A_LOG.registerPurchaseRequests();

    CoinMasterRequest.parseResponse(FUN_A_LOG, urlString, responseText);
  }

  public static boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("shop.php") || !urlString.contains("whichshop=piraterealm")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(FUN_A_LOG, urlString, true);
  }

  public static String accessible() {
    // You have to have the Fun-A-Log in your inventory in order to
    // purchase from it.  It is a quest item, so if you have it, it
    // will be there.  You get it the first time you complete a
    // PirateRealm adventure.  Therefore, you needed access to the
    // PirateRealm at least once to get it, but you do not need
    // current access to PirateRealm to use it.

    return InventoryManager.hasItem(ItemPool.PIRATE_REALM_FUN_LOG)
        ? null
        : "Need PirateRealm fun-a-log";
  }
}
