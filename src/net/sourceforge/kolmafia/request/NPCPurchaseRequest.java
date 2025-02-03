package net.sourceforge.kolmafia.request;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.moods.RecoveryManager;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.session.ResultProcessor;
import net.sourceforge.kolmafia.shop.ShopRequest;
import net.sourceforge.kolmafia.shop.ShopRow;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NPCPurchaseRequest extends PurchaseRequest {
  private static final List<AdventureResult> DISCOUNT_TROUSERS =
      List.of(
          ItemPool.get(ItemPool.TRAVOLTAN_TROUSERS),
          ItemPool.get(ItemPool.DESIGNER_SWEATPANTS),
          ItemPool.get(ItemPool.REPLICA_DESIGNER_SWEATPANTS));
  private static final AdventureResult FLEDGES = ItemPool.get(ItemPool.PIRATE_FLEDGES);
  private static final AdventureResult SUPER_SKILL = EffectPool.get(EffectPool.SUPER_SKILL);
  private static final AdventureResult SUPER_STRUCTURE = EffectPool.get(EffectPool.SUPER_STRUCTURE);
  private static final AdventureResult SUPER_VISION = EffectPool.get(EffectPool.SUPER_VISION);
  private static final AdventureResult SUPER_SPEED = EffectPool.get(EffectPool.SUPER_SPEED);
  private static final AdventureResult SUPER_ACCURACY = EffectPool.get(EffectPool.SUPER_ACCURACY);

  public static final Pattern PIRATE_EPHEMERA_PATTERN =
      Pattern.compile("pirate (?:brochure|pamphlet|tract)");

  private static final Pattern NPCSHOPID_PATTERN = Pattern.compile("whichshop=([^&]*)");

  private final String npcStoreId;
  private final String quantityField;
  private final int row;

  /** Constructs a new <code>NPCPurchaseRequest</code> which retrieves things from NPC stores. */
  public NPCPurchaseRequest(
      final String storeName,
      final String storeId,
      final int itemId,
      final int row,
      final int price) {
    this(storeName, storeId, itemId, row, price, PurchaseRequest.MAX_QUANTITY);
  }

  public NPCPurchaseRequest(
      final String storeName,
      final String storeId,
      final int itemId,
      final int row,
      final int price,
      final int quantity) {
    super(NPCPurchaseRequest.pickForm(storeId));

    this.item = ItemPool.get(itemId);

    this.shopName = storeName;
    this.npcStoreId = storeId;
    this.row = row;

    this.quantity = quantity;
    this.price = price;
    this.limit = this.quantity;
    this.canPurchase = true;

    this.timestamp = 0L;

    if (this.row != 0) {
      this.addFormField("whichshop", storeId);
      this.addFormField("action", "buyitem");
      this.addFormField("whichrow", String.valueOf(row));
      this.addFormField("ajax", "1");
      this.hashField = "pwd";
      this.quantityField = "quantity";
      return;
    }

    this.addFormField("whichitem", String.valueOf(itemId));

    if (storeId.equals("town_giftshop.php")) {
      this.addFormField("action", "buy");
      this.hashField = "pwd";
      this.quantityField = "howmany";
    } else if (storeId.equals("fdkol")) {
      this.addFormField("whichshop", storeId);
      this.addFormField("action", "buyitem");
      this.addFormField("ajax", "1");
      this.hashField = "pwd";
      this.quantityField = "quantity";
    } else {
      this.addFormField("whichstore", storeId);
      this.addFormField("buying", "1");
      this.addFormField("ajax", "1");
      this.hashField = "phash";
      this.quantityField = "howmany";
    }
  }

  @Override
  public boolean isMallStore() {
    return false;
  }

  public static String pickForm(final String shopId) {
    if (shopId.contains(".")) {
      return shopId;
    }
    return "shop.php";
  }

  public static String getShopId(final String urlString) {
    Matcher m = NPCPurchaseRequest.NPCSHOPID_PATTERN.matcher(urlString);
    return m.find() ? m.group(1) : null;
  }

  public String getStoreId() {
    return this.npcStoreId;
  }

  @Override
  public int getQuantity() {
    var possibleQuantity = NPCStoreDatabase.getQuantity(this.getItemId());
    return possibleQuantity.orElseGet(super::getQuantity);
  }

  /**
   * Retrieves the price of the item being purchased.
   *
   * @return The price of the item being purchased
   */
  @Override
  public long getPrice() {
    long factor = 100;
    if (this.shopName.equals("Doc Galaktik's Medicine Show")
        && QuestDatabase.isQuestFinished(Quest.DOC)) {
      // This is before the subtractions on purpose. It is possible that KoL
      // will change and this will need to be moved down.
      // The exact multiplier is 2/3 but with rounding this will give
      // the desired result.
      factor = 67;
    }
    if (NPCPurchaseRequest.usingTrousers(this.npcStoreId)) factor -= 5;
    if (KoLCharacter.hasSkill(SkillPool.FIVE_FINGER_DISCOUNT)) factor -= 5;
    return (this.price * factor) / 100;
  }

  public static int priceFactor(String npcStoreId) {
    int factor = 100;
    if (NPCPurchaseRequest.usingTrousers(npcStoreId)) factor -= 5;
    if (KoLCharacter.hasSkill(SkillPool.FIVE_FINGER_DISCOUNT)) factor -= 5;
    return factor;
  }

  public static int currentDiscountedPrice(long price) {
    return currentDiscountedPrice(null, price);
  }

  public static int currentDiscountedPrice(String npcStoreId, long price) {
    int factor = priceFactor(npcStoreId);
    if (factor == 100) {
      return (int) price;
    }
    return (int) ((price * factor) / 100);
  }

  public static int currentUnDiscountedPrice(long price) {
    return currentUnDiscountedPrice(null, price);
  }

  public static int currentUnDiscountedPrice(String npcStoreId, long price) {
    int factor = priceFactor(npcStoreId);
    if (factor == 100) {
      return (int) price;
    }
    return (int) Math.ceil(price / (factor / 100.0));
  }

  private static boolean usingTrousers(String npcStoreId) {
    if ("fdkol".equals(npcStoreId)) {
      return false;
    }

    var trousers = EquipmentManager.getEquipment(Slot.PANTS);

    if (trousers == null) {
      return false;
    }

    // Designer sweatpants discount does not apply to the gift shop
    if ("town_giftshop.php".equals(npcStoreId)
        && (trousers.getItemId() == ItemPool.DESIGNER_SWEATPANTS
            || trousers.getItemId() == ItemPool.REPLICA_DESIGNER_SWEATPANTS)) {
      return false;
    }

    return DISCOUNT_TROUSERS.contains(trousers);
  }

  private static AdventureResult getEquippableTrousers(String npcStoreId) {
    AdventureResult trousers =
        DISCOUNT_TROUSERS.stream()
            .filter(InventoryManager::hasItem)
            .filter(EquipmentManager::canEquip)
            .findFirst()
            .orElse(null);

    if (trousers == null) {
      return null;
    }

    // Designer sweatpants discount does not apply to the gift shop
    if ("town_giftshop.php".equals(npcStoreId)
        && (trousers.getItemId() == ItemPool.DESIGNER_SWEATPANTS
            || trousers.getItemId() == ItemPool.REPLICA_DESIGNER_SWEATPANTS)) {
      return null;
    }

    return trousers;
  }

  @Override
  public void run() {
    this.addFormField(this.quantityField, String.valueOf(this.limit));

    super.run();
  }

  @Override
  public boolean ensureProperAttire() {
    if (this.npcStoreId.equals("fdkol")) {
      // Travoltan trousers do not give a discount
      return true;
    }

    int neededOutfit = OutfitPool.NONE;

    switch (this.npcStoreId) {
      case "bugbear":
        neededOutfit = OutfitPool.BUGBEAR_COSTUME;
        break;
      case "bartlebys":
        if (!KoLCharacter.hasEquipped(NPCPurchaseRequest.FLEDGES)) {
          neededOutfit = OutfitPool.SWASHBUCKLING_GETUP;
        }
        break;
      case "hippy":
        if (this.shopName.equals("Hippy Store (Pre-War)")) {
          neededOutfit = OutfitPool.HIPPY_OUTFIT;
        } else if (QuestLogRequest.isHippyStoreAvailable()) {
          neededOutfit = OutfitPool.NONE;
        } else if (this.shopName.equals("Hippy Store (Hippy)")) {
          neededOutfit = OutfitPool.WAR_HIPPY_OUTFIT;
        } else if (this.shopName.equals("Hippy Store (Fratboy)")) {
          neededOutfit = OutfitPool.WAR_FRAT_OUTFIT;
        }
        break;
    }

    // Only switch outfits if the person is not currently wearing the outfit and if they
    // have that outfit.

    if (neededOutfit != OutfitPool.NONE) {
      if (EquipmentManager.isWearingOutfit(neededOutfit)) {
        return true;
      }

      if (!EquipmentManager.hasOutfit(neededOutfit)) {
        return false;
      }
    }

    // If you have a buff from Greatest American Pants and have it set to keep the buffs,
    // disallow outfit changes.

    if (Preferences.getBoolean("gapProtection")) {
      if (KoLConstants.activeEffects.contains(NPCPurchaseRequest.SUPER_SKILL)
          || KoLConstants.activeEffects.contains(NPCPurchaseRequest.SUPER_STRUCTURE)
          || KoLConstants.activeEffects.contains(NPCPurchaseRequest.SUPER_VISION)
          || KoLConstants.activeEffects.contains(NPCPurchaseRequest.SUPER_SPEED)
          || KoLConstants.activeEffects.contains(NPCPurchaseRequest.SUPER_ACCURACY)) {
        if (neededOutfit != OutfitPool.NONE) {
          KoLmafia.updateDisplay(
              MafiaState.ERROR,
              "You have a Greatest American Pants buff and buying the necessary "
                  + getItemName()
                  + " would cause you to lose it.");

          return false;
        }

        return true;
      }
    }

    // If the recovery manager is running, do not change equipment as this has the potential
    // for an infinite loop.

    if (RecoveryManager.isRecoveryActive() && !MoodManager.isExecuting()) {
      if (neededOutfit != OutfitPool.NONE) {
        KoLmafia.updateDisplay(
            MafiaState.ERROR,
            "Aborting implicit outfit change due to potential infinite loop in auto-recovery. Please buy the necessary "
                + getItemName()
                + " manually.");

        return false;
      }

      return true;
    }

    // If there's an outfit that you need to use, change into it.

    if (neededOutfit != OutfitPool.NONE) {
      (new EquipmentRequest(EquipmentDatabase.getOutfit(neededOutfit))).run();

      return true;
    }

    // Otherwise, maybe you can put on some discount-providing trousers to decrease the cost of the
    // purchase, but only if auto-recovery isn't running.

    if (!usingTrousers(this.npcStoreId)) {
      var trousers = getEquippableTrousers(this.npcStoreId);
      if (trousers != null) {
        (new EquipmentRequest(trousers, Slot.PANTS)).run();
      }
    }

    return true;
  }

  @Override
  public void processResults() {
    String urlString = this.getURLString();

    if (urlString.startsWith("shop.php")) {
      ShopRequest.parseResponse(urlString, this.responseText);
      // shop.php stores say "You spent xxx Meat" and that's already parsed.
      return;
    }

    int quantityAcquired = this.item.getCount(KoLConstants.inventory) - this.initialCount;

    if (quantityAcquired > 0) {
      ResultProcessor.processMeat(-1 * this.getPrice() * quantityAcquired);
      KoLCharacter.updateStatus();
    }
  }

  public static final boolean registerRequest(final String urlString) {
    if (!urlString.startsWith("town_giftshop.php")) {
      return false;
    }

    Matcher itemMatcher = TransferItemRequest.ITEMID_PATTERN.matcher(urlString);
    if (!itemMatcher.find()) {
      return true;
    }

    Matcher quantityMatcher = TransferItemRequest.HOWMANY_PATTERN.matcher(urlString);
    if (!quantityMatcher.find()) {
      return true;
    }

    int itemId = StringUtilities.parseInt(itemMatcher.group(1));
    String itemName = ItemDatabase.getItemName(itemId);
    int quantity = StringUtilities.parseInt(quantityMatcher.group(1));
    long priceVal = NPCStoreDatabase.price(itemId);

    Matcher m = NPCPurchaseRequest.NPCSHOPID_PATTERN.matcher(urlString);
    String shopId = m.find() ? NPCStoreDatabase.getStoreName(m.group(1)) : null;
    String shopName = shopId != null ? shopId : "an NPC Store";

    RequestLogger.updateSessionLog();
    RequestLogger.updateSessionLog(
        "buy " + quantity + " " + itemName + " for " + priceVal + " each from " + shopName);

    return true;
  }

  private static final Pattern BLOOD_MAYO_PATTERN =
      Pattern.compile("blood mayonnaise concentration: (\\d+) mayograms");

  public static final void parseShopResponse(
      final String shopId,
      final ShopRow shopRow,
      final String urlString,
      final String responseText) {

    // This is called from ShopRequest.parseResponse to handle things it can't.
    // It has already validated the shopId and parsed the inventory.

    // The following are normal NPC shops

    if (shopId.equals("chateau")) {
      ChateauRequest.parseShopResponse(urlString, responseText);
      return;
    }

    if (shopId.equals("mayoclinic")) {
      if (!responseText.contains("Mayo")) {
        // We don't have it installed, maybe got here through URL manipulation?
        return;
      }
      boolean refreshConcoctions = false;
      AdventureResult currentWorkshed = CampgroundRequest.getCurrentWorkshedItem();
      if (currentWorkshed == null || currentWorkshed.getItemId() != ItemPool.MAYO_CLINIC) {
        refreshConcoctions = true;
      }
      CampgroundRequest.setCurrentWorkshedItem(ItemPool.MAYO_CLINIC);
      if (urlString.contains("ajax=1")) {
        return;
      }
      Matcher mayoMatcher = BLOOD_MAYO_PATTERN.matcher(responseText);
      if (mayoMatcher.find()) {
        Preferences.setString("mayoLevel", mayoMatcher.group(1));
      }
      if (!urlString.contains("buyitem")) {
        if (responseText.contains("miracle whip")) {
          Preferences.setBoolean("_mayoDeviceRented", false);
          Preferences.setBoolean("itemBoughtPerAscension8266", false);
        } else if (responseText.contains("mayo lance")) {
          Preferences.setBoolean("_mayoDeviceRented", false);
          Preferences.setBoolean("itemBoughtPerAscension8266", true);
        } else {
          Preferences.setBoolean("_mayoDeviceRented", true);
          Preferences.setBoolean("itemBoughtPerAscension8266", true);
        }
      }
      Preferences.setBoolean("_mayoTankSoaked", !responseText.contains("Soak in the Mayo Tank"));
      if (refreshConcoctions) {
        ConcoctionDatabase.refreshConcoctions();
      }
      return;
    }

    if (shopId.equals("hiddentavern")) {
      // If Hidden Tavern not already unlocked, new items available
      if (Preferences.getInteger("hiddenTavernUnlock") != KoLCharacter.getAscensions()) {
        // Unlock Hidden Tavern
        Preferences.setInteger("hiddenTavernUnlock", KoLCharacter.getAscensions());
        ConcoctionDatabase.setRefreshNeeded(true);
      }
      return;
    }

    if (shopId.equals("fwshop")) {
      if (responseText.contains("<b>Combat Explosives")) {
        Preferences.setBoolean("_fireworksShop", true);
        Preferences.setBoolean(
            "_fireworksShopHatBought", !responseText.contains("<b>Dangerous Hats"));
        Preferences.setBoolean(
            "_fireworksShopEquipmentBought", !responseText.contains("<b>Explosive Equipment"));
      }
      return;
    }

    if (shopId.equals("wildfire")) {
      if (responseText.contains("You acquire an item")) {
        int itemId = shopRow.getItem().getItemId();
        switch (itemId) {
          case ItemPool.BLART -> Preferences.setBoolean("itemBoughtPerAscension10790", true);
          case ItemPool.RAINPROOF_BARREL_CAULK -> Preferences.setBoolean(
              "itemBoughtPerAscension10794", true);
          case ItemPool.PUMP_GREASE -> Preferences.setBoolean("itemBoughtPerAscension10795", true);
        }
      }

      if (!urlString.contains("ajax=1")) {
        // B. L. A. R. T.
        Preferences.setBoolean(
            "itemBoughtPerAscension10790", !responseText.contains("<tr rel=\"10790\">"));
        // rainproof barrel caulk
        Preferences.setBoolean(
            "itemBoughtPerAscension10794", !responseText.contains("<tr rel=\"10794\">"));
        // pump grease
        Preferences.setBoolean(
            "itemBoughtPerAscension10795", !responseText.contains("<tr rel=\"10795\">"));
        return;
      }
    }

    // When we purchase items from NPC stores using ajax, the
    // response tells us nothing about the contents of the store.
    if (urlString.contains("ajax=1")) {
      return;
    }

    // These shops have variable offerings
    if (shopId.equals("bartlebys")) {
      Matcher m = PIRATE_EPHEMERA_PATTERN.matcher(responseText);
      if (m.find()) {
        Preferences.setInteger("lastPirateEphemeraReset", KoLCharacter.getAscensions());
        Preferences.setString("lastPirateEphemera", m.group(0));
      }
      return;
    }

    if (shopId.equals("hippy")) {
      // Check to see if any of the items offered in the
      // hippy store are special.

      String side = "none";

      if (responseText.contains("peach")
          && responseText.contains("pear")
          && responseText.contains("plum")) {
        Preferences.setInteger("lastFilthClearance", KoLCharacter.getAscensions());
        side = "hippy";
      } else if (responseText.contains("bowl of rye sprouts")
          && responseText.contains("cob of corn")
          && responseText.contains("juniper berries")) {
        Preferences.setInteger("lastFilthClearance", KoLCharacter.getAscensions());
        side = "fratboy";
      }

      Preferences.setString("currentHippyStore", side);
      Preferences.setString("sidequestOrchardCompleted", side);

      if (responseText.contains("Oh, hey, boss!  Welcome back!")) {
        Preferences.setBoolean("_hippyMeatCollected", true);
      }
      return;
    }
  }
}
