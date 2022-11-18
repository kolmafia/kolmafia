package net.sourceforge.kolmafia.request;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.CoinmasterData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class HermitRequest extends CoinMasterRequest {
  public static final String master = "Hermit";

  private static final Pattern TOKEN_PATTERN = Pattern.compile("You have ([\\d,]+) tradable items");
  public static final AdventureResult WORTHLESS_ITEM = ItemPool.get(ItemPool.WORTHLESS_ITEM, 1);

  private static final Map<Integer, Integer> buyPrices = new TreeMap<Integer, Integer>();

  public static final CoinmasterData HERMIT =
      new CoinmasterData(master, "hermit", HermitRequest.class)
          .withToken("worthless item")
          .withTokenPattern(TOKEN_PATTERN)
          .withItem(WORTHLESS_ITEM)
          .withBuyURL("hermit.php")
          .withBuyAction("trade")
          .withBuyItems(KoLConstants.hermitItems)
          .withBuyPrices(buyPrices)
          .withItemField("whichitem")
          .withItemPattern(GenericRequest.WHICHITEM_PATTERN)
          .withCountField("quantity")
          .withCountPattern(GenericRequest.QUANTITY_PATTERN);

  private static final Pattern CLOVER_PATTERN = Pattern.compile("(\\d+) left in stock for today");

  public static final AdventureResult CLOVER = ItemPool.get(ItemPool.ELEVEN_LEAF_CLOVER, 1);
  public static final String CLOVER_FIELD = "whichitem=" + ItemPool.ELEVEN_LEAF_CLOVER;

  public static final AdventureResult PERMIT = ItemPool.get(ItemPool.HERMIT_PERMIT, 1);

  public static final AdventureResult TRINKET = ItemPool.get(ItemPool.WORTHLESS_TRINKET, 1);
  public static final AdventureResult GEWGAW = ItemPool.get(ItemPool.WORTHLESS_GEWGAW, 1);
  public static final AdventureResult KNICK_KNACK = ItemPool.get(ItemPool.WORTHLESS_KNICK_KNACK, 1);

  public static final AdventureResult HACK_SCROLL = ItemPool.get(ItemPool.HERMIT_SCRIPT, 1);
  public static final AdventureResult SUMMON_SCROLL = ItemPool.get(ItemPool.ELITE_SCROLL, 1);

  private static boolean checkedForClovers = false;
  private static final Integer ONE = 1;

  /**
   * Constructs a new <code>HermitRequest</code> that simply checks what items the hermit has
   * available.
   */
  public HermitRequest() {
    super(HERMIT);
  }

  public HermitRequest(final boolean buying, final AdventureResult[] attachments) {
    super(HERMIT, buying, attachments);
  }

  public HermitRequest(final boolean buying, final AdventureResult attachment) {
    super(HERMIT, buying, attachment);
  }

  public HermitRequest(final boolean buying, final int itemId, final int quantity) {
    super(HERMIT, buying, itemId, quantity);
  }

  public HermitRequest(final int itemId, final int quantity) {
    this(true, itemId, quantity);
  }

  private static void registerHermitItem(final int itemId, final int count) {
    AdventureResult item = ItemPool.get(itemId, count);
    KoLConstants.hermitItems.add(item);
    buyPrices.put(itemId, ONE);
  }

  public static final void initialize() {
    reset();

    if (KoLCharacter.inZombiecore() || KoLCharacter.inNuclearAutumn()) {
      resetPurchaseRequests();
      return;
    }

    registerHermitItem(ItemPool.SEAL_TOOTH, PurchaseRequest.MAX_QUANTITY);
    registerHermitItem(ItemPool.CHISEL, PurchaseRequest.MAX_QUANTITY);
    registerHermitItem(ItemPool.PETRIFIED_NOODLES, PurchaseRequest.MAX_QUANTITY);
    registerHermitItem(ItemPool.JABANERO_PEPPER, PurchaseRequest.MAX_QUANTITY);
    registerHermitItem(ItemPool.BANJO_STRINGS, PurchaseRequest.MAX_QUANTITY);
    registerHermitItem(ItemPool.BUTTERED_ROLL, PurchaseRequest.MAX_QUANTITY);
    registerHermitItem(ItemPool.WOODEN_FIGURINE, PurchaseRequest.MAX_QUANTITY);
    registerHermitItem(ItemPool.KETCHUP, PurchaseRequest.MAX_QUANTITY);
    registerHermitItem(ItemPool.CATSUP, PurchaseRequest.MAX_QUANTITY);
    registerHermitItem(ItemPool.VOLLEYBALL, PurchaseRequest.MAX_QUANTITY);
    if (KoLCharacter.isSealClubber()) {
      registerHermitItem(ItemPool.ANCIENT_SEAL, PurchaseRequest.MAX_QUANTITY);
    }

    registerHermitItem(ItemPool.ELEVEN_LEAF_CLOVER, -1);

    resetPurchaseRequests();
  }

  public static final void reset() {
    checkedForClovers = false;
    KoLConstants.hermitItems.clear();
    buyPrices.clear();
  }

  public static final void resetPurchaseRequests() {
    HERMIT.registerPurchaseRequests();
  }

  private int worthlessItemsNeeded() {
    if (this.attachments == null) {
      return 0;
    }

    int count = 0;
    for (AdventureResult attachment : this.attachments) {
      count += attachment.getCount();
    }

    return count;
  }

  private int cloversNeeded() {
    if (this.attachments == null) {
      return 0;
    }

    int count = 0;
    for (AdventureResult attachment : this.attachments) {
      if (attachment.getItemId() == ItemPool.ELEVEN_LEAF_CLOVER) {
        count += attachment.getCount();
      }
    }

    return count;
  }

  /**
   * Executes the <code>HermitRequest</code>. This will trade the item specified in the character's
   * <code>KoLSettings</code> for their worthless trinket; if the character has no worthless
   * trinkets, this method will report an error to the StaticEntity.getClient().
   */
  @Override
  public void run() {
    // If we are simply visiting, we need no worthless items
    if (this.attachments == null) {
      super.run();
      return;
    }

    int cloversWanted = this.cloversNeeded();
    int cloversAvailable = cloverCount();
    if (cloversWanted > cloversAvailable) {
      KoLmafia.updateDisplay(
          MafiaState.ERROR,
          "Asking for "
              + cloversWanted
              + " 11-leaf clover, but "
              + cloversAvailable
              + " left today.");
      return;
    }

    int count = this.worthlessItemsNeeded();

    // If we have a hermit script, read it now
    if (InventoryManager.hasItem(HACK_SCROLL)) {
      RequestThread.postRequest(UseItemRequest.getInstance(HACK_SCROLL));
    }

    int worthless = getWorthlessItemCount();

    // If we want to make a trade, fetch enough worthless items
    if (worthless < count) {
      InventoryManager.retrieveItem(WORTHLESS_ITEM.getInstance(count));
      worthless = getWorthlessItemCount();
    }

    if (worthless < count) {
      return;
    }

    super.run();
  }

  @Override
  public void processResults() {
    // The Hermit has left in the Nuclear Autumn path
    if (KoLCharacter.inNuclearAutumn()) {
      return;
    }

    // In Zombiecore we can get one clover per day on first visit
    if (KoLCharacter.inZombiecore()) {
      Preferences.setBoolean("_zombieClover", true);
      checkedForClovers = true;
      return;
    }

    if (!parseHermitTrade(this.getURLString(), this.responseText)) {
      // If we got here, the hermit wouldn't talk to us.
      if (InventoryManager.retrieveItem(PERMIT)) {
        this.run();
        return;
      }

      KoLmafia.updateDisplay(MafiaState.ERROR, "You're not allowed to visit the Hermit.");
      return;
    }

    if (this.attachments == null) {
      return;
    }

    // If you don't have any hermit permits, get one
    // The Hermit looks at you expectantly, and when you don't respond, he points to a
    // crudely-chalked
    // sign on the wall reading "Hermit Permit required, pursuant to Seaside Town Ordinance #3769"

    if (this.responseText.indexOf("Hermit Permit required") != -1) {
      if (InventoryManager.retrieveItem(PERMIT)) {
        this.run();
      }

      return;
    }

    // If the item is unavailable, assume he was asking for clover

    if (this.responseText.indexOf("doesn't have that item.") != -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Today is not a clover day.");
      return;
    }

    // If you still didn't acquire items, what went wrong?

    if (this.responseText.indexOf("You acquire") == -1) {
      KoLmafia.updateDisplay(MafiaState.ERROR, "The hermit kept his stuff.");
      return;
    }
  }

  public static final boolean parseHermitTrade(final String urlString, final String responseText) {
    // Nothing special to do if the Hermit has departed
    if (KoLCharacter.inZombiecore() || KoLCharacter.inNuclearAutumn()) {
      return true;
    }

    // There should be a form, or an indication of item receipt,
    // for all valid hermit requests.

    if (responseText.indexOf("hermit.php") == -1 && responseText.indexOf("You acquire") == -1) {
      return false;
    }

    // If you don't have enough Hermit Permits, failure

    if (responseText.indexOf("You don't have enough Hermit Permits") != -1) {
      checkedForClovers = false;
      return true;
    }

    // If the item is unavailable, assume he was asking for clover
    // If asked for too many, you get no items

    if (responseText.indexOf("doesn't have that item.") != -1
        || responseText.indexOf("You acquire") == -1) {
      parseHermitStock(responseText);
      return true;
    }

    Matcher quantityMatcher = GenericRequest.QUANTITY_PATTERN.matcher(urlString);
    if (!quantityMatcher.find()) {
      // We simply visited the hermit
      parseHermitStock(responseText);
      return true;
    }

    int quantity = StringUtilities.parseInt(quantityMatcher.group(1));

    // Subtract the worthless items in order of their priority;
    // as far as we know, the priority is the item Id.

    int used = subtractWorthlessItems(TRINKET, quantity);
    if (used > 0) {
      quantity -= used;
    }
    used = subtractWorthlessItems(GEWGAW, quantity);
    if (used > 0) {
      quantity -= used;
    }
    used = subtractWorthlessItems(KNICK_KNACK, quantity);
    if (used > 0) {
      quantity -= used;
    }

    if (responseText.indexOf("he sends you packing") != -1) {
      // No worthless items in inventory, so we can't tell if
      // clovers remain in stock
      checkedForClovers = false;
      return true;
    }

    parseHermitStock(responseText);

    return true;
  }

  private static int subtractWorthlessItems(final AdventureResult item, final int total) {
    int count = 0 - Math.min(total, item.getCount(KoLConstants.inventory));
    if (count != 0) {
      ResultProcessor.processResult(item.getInstance(count));
    }
    return 0 - count;
  }

  // <td valign=center><img src="http://images.kingdomofloathing.com/itemimages/tooth.gif"
  // class=hand onClick='javascript:item(617818041)'></td><td valign=center><b>seal
  // tooth</b></td></tr>
  private static final Pattern ITEM_PATTERN =
      Pattern.compile("javascript:item\\(([\\d]+)\\).*?<b>([^<]*)</b>", Pattern.DOTALL);

  private static void parseHermitStock(final String responseText) {
    // Refresh the Coin Master inventory every time we visit.
    Matcher matcher = ITEM_PATTERN.matcher(responseText);
    if (!matcher.find()) {
      return;
    }

    KoLConstants.hermitItems.clear();
    buyPrices.clear();

    do {
      String descId = matcher.group(1);
      String itemName = matcher.group(2);
      int itemId = ItemDatabase.getItemId(itemName);

      // Add it to the Hermit's inventory
      registerHermitItem(itemId, PurchaseRequest.MAX_QUANTITY);
    } while (matcher.find());

    Matcher cloverMatcher = CLOVER_PATTERN.matcher(responseText);
    int count = cloverMatcher.find() ? Integer.parseInt(cloverMatcher.group(1)) : 0;

    int index = KoLConstants.hermitItems.indexOf(CLOVER);
    if (index < 0) {
      registerHermitItem(ItemPool.ELEVEN_LEAF_CLOVER, count);
    } else {
      AdventureResult old = KoLConstants.hermitItems.get(index);
      int oldCount = old.getCount();
      if (oldCount != count) {
        KoLConstants.hermitItems.set(index, CLOVER.getInstance(count));
      }
    }

    checkedForClovers = true;

    // Register the purchase requests, now that we know what is available
    HERMIT.registerPurchaseRequests();
  }

  public static final boolean isWorthlessItem(final int itemId) {
    return switch (itemId) {
      case ItemPool.WORTHLESS_TRINKET,
          ItemPool.WORTHLESS_GEWGAW,
          ItemPool.WORTHLESS_KNICK_KNACK -> true;
      default -> false;
    };
  }

  public static final int getWorthlessItemCount() {
    return getWorthlessItemCount(KoLConstants.inventory);
  }

  public static final int getWorthlessItemCount(final List<AdventureResult> list) {
    return TRINKET.getCount(list) + GEWGAW.getCount(list) + KNICK_KNACK.getCount(list);
  }

  public static final int getAvailableWorthlessItemCount() {
    int count = getWorthlessItemCount(KoLConstants.inventory);

    if (InventoryManager.canUseCloset()) {
      count += getWorthlessItemCount(KoLConstants.closet);
    }

    if (InventoryManager.canUseStorage()) {
      count += getWorthlessItemCount(KoLConstants.storage);
    }

    return count;
  }

  public static final int getAcquirableWorthlessItemCount() {
    int count = getAvailableWorthlessItemCount();
    if (InventoryManager.canUseNPCStores()) {
      int cost = SewerRequest.currentWorthlessItemCost();
      count += ((int) KoLCharacter.getAvailableMeat()) / cost;
    }
    return count;
  }

  public static final int cloverCount() {
    // One clover a day available in Zombie path
    if (KoLCharacter.inZombiecore()) {
      return Preferences.getBoolean("_zombieClover0") ? 0 : 1;
    }

    if (!checkedForClovers) {
      RequestThread.postRequest(new HermitRequest());
    }

    int index = KoLConstants.hermitItems.indexOf(CLOVER);
    return index < 0 ? 0 : KoLConstants.hermitItems.get(index).getCount();
  }

  public static final boolean isCloverDay() {
    return cloverCount() > 0;
  }

  public static final void hackHermit() {
    int index = KoLConstants.hermitItems.indexOf(CLOVER);
    if (index != -1) {
      AdventureResult clover = KoLConstants.hermitItems.get(index);
      KoLConstants.hermitItems.set(index, CLOVER.getInstance(clover.getCount() + 1));
    } else {
      registerHermitItem(ItemPool.ELEVEN_LEAF_CLOVER, 1);
    }
  }

  public static String accessible() {
    if (KoLCharacter.isKingdomOfExploathing()) {
      return "The Hermitage exploded";
    }
    return null;
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("hermit.php")) {
      return false;
    }

    return CoinMasterRequest.registerRequest(HERMIT, urlString, true);
  }
}
