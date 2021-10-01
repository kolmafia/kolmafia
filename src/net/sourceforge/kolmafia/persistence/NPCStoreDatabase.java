package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.request.NPCPurchaseRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.QuestLogRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.HashMultimap;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class NPCStoreDatabase {
  private static final HashMultimap<NPCPurchaseRequest> NPC_ITEMS =
      new HashMultimap<NPCPurchaseRequest>();
  private static final HashMultimap<NPCPurchaseRequest> ROW_ITEMS =
      new HashMultimap<NPCPurchaseRequest>();
  private static final AdventureResult RABBIT_HOLE =
      new AdventureResult("Down the Rabbit Hole", 1, true);
  private static final Map<String, String> storeNameById = new TreeMap<String, String>();

  static {
    BufferedReader reader =
        FileUtilities.getVersionedReader("npcstores.txt", KoLConstants.NPCSTORES_VERSION);

    String[] data;

    while ((data = FileUtilities.readData(reader)) != null) {
      if (data.length < 4) {
        continue;
      }

      String storeName = data[0];
      String storeId = data[1];
      if (!storeId.equals("bartlebys")) {
        NPCStoreDatabase.storeNameById.put(storeId, storeName);
      }

      String itemName = data[2];
      int itemId = ItemDatabase.getItemId(itemName);
      if (itemId == -1) {
        RequestLogger.printLine("Unknown item in store \"" + data[0] + "\": " + itemName);
        continue;
      }

      int price = StringUtilities.parseInt(data[3]);
      int row =
          (data.length > 4 && data[4].startsWith("ROW"))
              ? IntegerPool.get(StringUtilities.parseInt(data[4].substring(3)))
              : 0;

      // Make the purchase request for this item
      int quantity = NPCStoreDatabase.limitQuantity(itemId);
      NPCPurchaseRequest purchaseRequest =
          new NPCPurchaseRequest(storeName, storeId, itemId, row, price, quantity);

      // Map from item id -> purchase request
      NPCStoreDatabase.NPC_ITEMS.put(itemId, purchaseRequest);

      // Map from row -> purchase request
      if (row != 0) {
        NPCStoreDatabase.ROW_ITEMS.put(row, purchaseRequest);
      }
    }

    try {
      reader.close();
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
    }
  }

  public static final String getStoreName(final String storeId) {
    return storeId.equals("bartlebys")
        ? (KoLCharacter.inBeecore()
            ? "Barrrtleby's Barrrgain Books (Bees Hate You)"
            : "Barrrtleby's Barrrgain Books")
        : NPCStoreDatabase.storeNameById.get(storeId);
  }

  public static final PurchaseRequest getPurchaseRequest(final int itemId) {
    NPCPurchaseRequest foundItem = null;

    List<NPCPurchaseRequest> items = NPCStoreDatabase.NPC_ITEMS.get(itemId);
    if (items == null) {
      return null;
    }

    for (NPCPurchaseRequest item : items) {
      foundItem = item;

      if (!NPCStoreDatabase.canPurchase(item.getStoreId(), item.getShopName(), itemId)) {
        continue;
      }

      item.setCanPurchase(true);
      return item;
    }

    if (foundItem == null) {
      return null;
    }

    foundItem.setCanPurchase(false);
    return foundItem;
  }

  private static int limitQuantity(int itemId) {
    switch (itemId) {
      case ItemPool.ABRIDGED:
      case ItemPool.ZEPPELIN_TICKET:
      case ItemPool.FORGED_ID_DOCUMENTS:
      case ItemPool.SPARE_KIDNEY:
      case ItemPool.FEDORA_MOUNTED_FOUNTAIN:
      case ItemPool.PORKPIE_MOUNTED_POPPER:
      case ItemPool.SOMBRERO_MOUNTED_SPARKLER:
      case ItemPool.CATHERINE_WHEEL:
      case ItemPool.ROCKET_BOOTS:
      case ItemPool.OVERSIZED_SPARKLER:
        return 1;
    }
    return PurchaseRequest.MAX_QUANTITY;
  }

  private static boolean canPurchase(
      final String storeId, final String shopName, final int itemId) {
    if (storeId == null) {
      return false;
    }

    // Check for whether or not the purchase can be made from a
    // guild store.	 Store #1 is moxie classes, store #2 is for
    // mysticality classes, and store #3 is for muscle classes.

    String classType = KoLCharacter.getClassType();

    if (storeId.equals("gnoll")) {
      // Degrassi Knoll Bakery and Hardware Store
      return KoLCharacter.knollAvailable();
    } else if (storeId.equals("tweedle")) {
      // The Tweedleporium
      return KoLConstants.activeEffects.contains(NPCStoreDatabase.RABBIT_HOLE);
    } else if (storeId.equals("bugbear")) {
      if (KoLCharacter.inNuclearAutumn()) {
        return false;
      }
      // Bugbear Bakery
      return EquipmentManager.hasOutfit(OutfitPool.BUGBEAR_COSTUME);
    } else if (storeId.equals("madeline")) {
      // Bugbear Bakery
      return QuestDatabase.isQuestFinished(Quest.ARMORER);
    } else if (storeId.equals("bartender")) {
      // The Typical Tavern
      return !KoLCharacter.inZombiecore() && QuestLogRequest.isTavernAvailable();
    } else if (storeId.equals("blackmarket")) {
      // Black Market
      if (!QuestLogRequest.isBlackMarketAvailable()) {
        return false;
      }
      switch (itemId) {
        case ItemPool.ZEPPELIN_TICKET:
          return !InventoryManager.hasItem(itemId);
        case ItemPool.SPARE_KIDNEY:
          // Should check for whether your kidney has been stolen
          return KoLCharacter.inBadMoon() && !InventoryManager.hasItem(itemId);
        case ItemPool.FORGED_ID_DOCUMENTS:
          return !QuestDatabase.isQuestLaterThan(Quest.MACGUFFIN, "step1");
      }
      return true;
    } else if (storeId.equals("chateau")) {
      // Chateau Mantenga
      return ChateauRequest.chateauAvailable();
    } else if (storeId.equals("chinatown")) {
      // Chinatown Shops
      return KoLConstants.inventory.contains(ItemPool.get(ItemPool.STRANGE_GOGGLES, 1))
          && KoLConstants.campground.contains(ItemPool.get(ItemPool.SUSPICIOUS_JAR, 1));
    } else if (storeId.startsWith("crimbo18")) {
      return false;
    } else if (storeId.startsWith("crimbo19")) {
      return false;
    } else if (storeId.startsWith("crimbo20")) {
      return false;
    } else if (storeId.equals("guildstore1")) {
      // Shadowy Store
      return KoLCharacter.isMoxieClass() && KoLCharacter.getGuildStoreOpen();
    } else if (storeId.equals("guildstore2")) {
      // Gouda's Grimoire and Grocery
      return (KoLCharacter.isMysticalityClass()
              || (classType.equals(KoLCharacter.ACCORDION_THIEF) && KoLCharacter.getLevel() >= 9))
          && KoLCharacter.getGuildStoreOpen();
    } else if (storeId.equals("guildstore3")) {
      // Smacketeria
      return ((KoLCharacter.isMuscleClass() && !KoLCharacter.isAvatarOfBoris())
              || (classType.equals(KoLCharacter.ACCORDION_THIEF) && KoLCharacter.getLevel() >= 9))
          && KoLCharacter.getGuildStoreOpen();
    } else if (storeId.equals("hippy")) {
      if (KoLCharacter.isKingdomOfExploathing()) {
        return false;
      }

      int level = KoLCharacter.getLevel();

      if (shopName.equals("Hippy Store (Pre-War)")) {
        if (!KoLCharacter.mysteriousIslandAccessible()
            || !EquipmentManager.hasOutfit(OutfitPool.HIPPY_OUTFIT)) {
          return false;
        }

        if (Preferences.getInteger("lastFilthClearance") == KoLCharacter.getAscensions()) {
          return false;
        }

        if (level < 12) {
          return true;
        }

        return QuestLogRequest.isHippyStoreAvailable();
      }

      // Here, you insert any logic which is able to detect
      // the completion of the filthworm infestation and
      // which outfit was used to complete it.

      if (Preferences.getInteger("lastFilthClearance") != KoLCharacter.getAscensions()) {
        return false;
      }

      int outfit = OutfitPool.NONE;
      if (shopName.equals("Hippy Store (Hippy)")) {
        if (!Preferences.getString("currentHippyStore").equals("hippy")) {
          return false;
        }

        outfit = OutfitPool.WAR_HIPPY_OUTFIT;
      } else if (shopName.equals("Hippy Store (Fratboy)")) {
        if (!Preferences.getString("currentHippyStore").equals("fratboy")) {
          return false;
        }

        outfit = OutfitPool.WAR_FRAT_OUTFIT;
      } else {
        // What is this?
        return false;
      }

      return QuestLogRequest.isHippyStoreAvailable() || EquipmentManager.hasOutfit(outfit);
    } else if (storeId.equals("knobdisp")) {
      // The Knob Dispensary
      return KoLCharacter.getDispensaryOpen();
    } else if (storeId.equals("jewelers")) {
      // Little Canadia Jewelers
      return !KoLCharacter.inZombiecore() && KoLCharacter.canadiaAvailable();
    } else if (storeId.equals("generalstore")) {
      // General Store

      if (KoLCharacter.inNuclearAutumn()) {
        return false;
      }

      // Some items restricted, often because of holidays
      String holiday = HolidayDatabase.getHoliday();

      switch (itemId) {
        case ItemPool.MARSHMALLOW:
          return holiday.contains("Yuletide");
        case ItemPool.OYSTER_BASKET:
          return holiday.contains("Oyster Egg Day");
        case ItemPool.PARTY_HAT:
          return holiday.contains("Festival of Jarlsberg");
        case ItemPool.M282:
        case ItemPool.SNAKE:
        case ItemPool.SPARKLER:
          return holiday.contains("Dependence Day");
        case ItemPool.GREEN_ROCKET:
          return holiday.contains("Dependence Day") && holiday.contains("St. Sneaky Pete's Day");
        case ItemPool.FOAM_NOODLE:
        case ItemPool.INFLATABLE_DUCK:
        case ItemPool.WATER_WINGS:
          return holiday.contains("Generic Summer Holiday");
        case ItemPool.DESERT_BUS_PASS:
          return !KoLCharacter.desertBeachAccessible();
        case ItemPool.FOLDER_01:
        case ItemPool.FOLDER_02:
        case ItemPool.FOLDER_03:
          {
            AdventureResult folderHolder = ItemPool.get(ItemPool.FOLDER_HOLDER);
            return folderHolder.getCount(KoLConstants.inventory) > 0
                || folderHolder.getCount(KoLConstants.closet) > 0
                || folderHolder.getCount(KoLConstants.collection) > 0
                || KoLCharacter.hasEquipped(folderHolder);
          }
        case ItemPool.WATER_WINGS_FOR_BABIES:
        case ItemPool.MINI_LIFE_PRESERVER:
        case ItemPool.HEAVY_DUTY_UMBRELLA:
        case ItemPool.POOL_SKIMMER:
          return KoLCharacter.inRaincore();
        case ItemPool.FISHING_LINE:
          return InventoryManager.hasItem(ItemPool.FISHING_POLE);
        case ItemPool.TRICK_TOT_UNICORN:
        case ItemPool.TRICK_TOT_CANDY:
          return KoLCharacter.findFamiliar(FamiliarPool.TRICK_TOT) != null;
      }
    } else if (storeId.equals("town_giftshop.php")) {
      // Gift Shop
      if (KoLCharacter.inBadMoon() || KoLCharacter.isKingdomOfExploathing()) {
        return false;
      }

      // Some items restricted, because of holidays or number of ascensions
      String holiday = HolidayDatabase.getHoliday();
      int asc = KoLCharacter.getAscensions();

      switch (itemId) {
        case ItemPool.VALENTINE:
        case ItemPool.CHOCOLATE_COVERED_DIAMOND_STUDDED_ROSES:
        case ItemPool.BOUQUET_OF_CIRCULAR_SAW_BLADES:
        case ItemPool.BETTER_THAN_CUDDLING_CAKE:
        case ItemPool.STUFFED_NINJA_SNOWMAN:
          return holiday.contains("Valentine's Day");
        case ItemPool.POTTED_FERN:
        case ItemPool.HAPPY_BIRTHDAY_CLAUDE_CAKE:
          return asc >= 1;
        case ItemPool.STUFFED_GHUOL_WHELP:
        case ItemPool.HEART_SHAPED_BALLOON:
          return asc >= 2;
        case ItemPool.TULIP:
        case ItemPool.PERSONALIZED_BIRTHDAY_CAKE:
          return asc >= 4;
        case ItemPool.STUFFED_ZMOBIE:
        case ItemPool.ANNIVERSARY_BALLOON:
          return asc >= 5;
        case ItemPool.VENUS_FLYTRAP:
        case ItemPool.THREE_TIERED_WEDDING_CAKE:
          return asc >= 7;
        case ItemPool.RAGGEDY_HIPPY_DOLL:
        case ItemPool.MYLAR_BALLOON:
          return asc >= 8;
        case ItemPool.ALL_PURPOSE_FLOWER:
        case ItemPool.BABYCAKES:
          return asc >= 10;
        case ItemPool.STUFFED_STAB_BAT:
        case ItemPool.KEVLAR_BALLOON:
          return asc >= 11;
        case ItemPool.EXOTIC_ORCHID:
        case ItemPool.BLUE_VELVET_CAKE:
          return asc >= 13;
        case ItemPool.APATHETIC_LIZARDMAN_DOLL:
        case ItemPool.THOUGHT_BALLOON:
          return asc >= 14;
        case ItemPool.LONG_STEMMED_ROSE:
        case ItemPool.CONGRATULATORY_CAKE:
          return asc >= 16;
        case ItemPool.STUFFED_YETI:
        case ItemPool.RAT_BALLOON:
          return asc >= 17;
        case ItemPool.GILDED_LILY:
        case ItemPool.ANGEL_FOOD_CAKE:
          return asc >= 19;
        case ItemPool.STUFFED_MOB_PENGUIN:
        case ItemPool.MINI_ZEPPELIN:
          return asc >= 20;
        case ItemPool.DEADLY_NIGHTSHADE:
        case ItemPool.DEVILS_FOOD_CAKE:
          return asc >= 22;
        case ItemPool.STUFFED_SABRE_TOOTHED_LIME:
        case ItemPool.MR_BALLOON:
          return asc >= 23;
        case ItemPool.BLACK_LOTUS:
        case ItemPool.BIRTHDAY_PARTY_JELLYBEAN_CHEESECAKE:
          return asc >= 25;
        case ItemPool.GIANT_STUFFED_BUGBEAR:
        case ItemPool.RED_BALLOON:
          return asc >= 26;
      }
    } else if (storeId.equals("gnomart")) {
      // Gno-Mart
      return !KoLCharacter.inZombiecore() && KoLCharacter.gnomadsAvailable();
    } else if (storeId.equals("mayoclinic")) {
      // The Mayo Clinic
      boolean available = false;
      AdventureResult workshedItem = CampgroundRequest.getCurrentWorkshedItem();
      if (workshedItem != null) {
        available =
            workshedItem.getItemId() == ItemPool.MAYO_CLINIC
                && StandardRequest.isAllowed("Items", "portable Mayo Clinic");
        if (itemId == ItemPool.MIRACLE_WHIP) {
          return available
              && !Preferences.getBoolean("_mayoDeviceRented")
              && !Preferences.getBoolean("itemBoughtPerAscension8266");
        }
        if (itemId == ItemPool.SPHYGMAYOMANOMETER
            || itemId == ItemPool.REFLEX_HAMMER
            || itemId == ItemPool.MAYO_LANCE) {
          return available && !Preferences.getBoolean("_mayoDeviceRented");
        }
      }
      return available;
    } else if (storeId.equals("unclep")) {
      // Uncle P's Antiques
      return !KoLCharacter.inZombiecore()
          && !KoLCharacter.inNuclearAutumn()
          && KoLCharacter.desertBeachAccessible();
    } else if (storeId.equals("bartlebys")) {
      boolean available = shopName.equals(NPCStoreDatabase.getStoreName(storeId));
      if (!available) {
        return false;
      }

      if (itemId == ItemPool.ABRIDGED
          && (InventoryManager.hasItem(ItemPool.ABRIDGED)
              || InventoryManager.hasItem(ItemPool.DICTIONARY)
              || QuestDatabase.isQuestFinished(Quest.LOL))) {
        return false;
      }

      String itemName = ItemDatabase.getItemName(itemId);
      if (Preferences.getInteger("lastPirateEphemeraReset") == KoLCharacter.getAscensions()
          && !Preferences.getString("lastPirateEphemera").equals(itemName)) {
        if (NPCPurchaseRequest.PIRATE_EPHEMERA_PATTERN.matcher(itemName).matches()) {
          return false;
        }
      }
      return EquipmentManager.hasOutfit(OutfitPool.SWASHBUCKLING_GETUP)
          || InventoryManager.hasItem(ItemPool.PIRATE_FLEDGES);
    } else if (storeId.equals("meatsmith")) {
      // Meatsmith's Shop
      return !KoLCharacter.inZombiecore()
          && !KoLCharacter.inNuclearAutumn()
          && !KoLCharacter.isKingdomOfExploathing();
    } else if (storeId.equals("whitecitadel")) {
      return QuestLogRequest.isWhiteCitadelAvailable();
    } else if (storeId.equals("nerve")) {
      // Nervewrecker's Store
      return KoLCharacter.inBadMoon();
    } else if (storeId.equals("armory")) {
      if (KoLCharacter.inZombiecore()
          || KoLCharacter.inNuclearAutumn()
          || KoLCharacter.isKingdomOfExploathing()) {
        return false;
      }
      // Armory and Leggery
      if (itemId == ItemPool.FISHING_HAT) {
        return InventoryManager.hasItem(ItemPool.FISHING_POLE);
      }
    } else if (storeId.equals("fwshop")) {
      if (Preferences.getBoolean("_fireworksShop") == false) {
        return false;
      }

      switch (itemId) {
        case ItemPool.FEDORA_MOUNTED_FOUNTAIN:
        case ItemPool.PORKPIE_MOUNTED_POPPER:
        case ItemPool.SOMBRERO_MOUNTED_SPARKLER:
          return Preferences.getBoolean("_fireworksShopHatBought") == false;
        case ItemPool.CATHERINE_WHEEL:
        case ItemPool.ROCKET_BOOTS:
        case ItemPool.OVERSIZED_SPARKLER:
          return Preferences.getBoolean("_fireworksShopEquipmentBought") == false;
      }

      return true;
    } else if (storeId.equals("fdkol")) {
      return false;
    } else if (storeId.equals("hiddentavern")) {
      return Preferences.getInteger("hiddenTavernUnlock") == KoLCharacter.getAscensions();
    } else if (storeId.equals("doc")) {
      if (KoLCharacter.inZombiecore()
          || KoLCharacter.inNuclearAutumn()
          || KoLCharacter.isKingdomOfExploathing()) {
        return false;
      }
      if (itemId == ItemPool.DOC_VITALITY_SERUM) {
        return QuestDatabase.isQuestFinished(Quest.DOC);
      }
    } else if (storeId.equals("mystic")) {
      if (KoLCharacter.isKingdomOfExploathing()) {
        return false;
      }
      if (itemId == ItemPool.YELLOW_SUBMARINE) {
        return !KoLCharacter.desertBeachAccessible();
      } else if (itemId == ItemPool.DIGITAL_KEY) {
        return !InventoryManager.hasItem(ItemPool.DIGITAL_KEY);
      }
    } else if (storeId.equals("vault1")) {
      // Fallout Shelter Medical Supply
      if (!KoLCharacter.inNuclearAutumn() || Preferences.getInteger("falloutShelterLevel") < 2) {
        return false;
      }
      if (itemId == ItemPool.TRICK_TOT_CANDY || itemId == ItemPool.TRICK_TOT_EYEBALL) {
        return KoLCharacter.findFamiliar(FamiliarPool.TRICK_TOT) != null;
      }
    } else if (storeId.equals("vault2")) {

      if (!KoLCharacter.inNuclearAutumn() || Preferences.getInteger("falloutShelterLevel") < 4) {
        return false;
      }
      if (itemId == ItemPool.TRICK_TOT_KNIGHT || itemId == ItemPool.TRICK_TOT_ROBOT) {
        return KoLCharacter.findFamiliar(FamiliarPool.TRICK_TOT) != null;
      }
    } else if (storeId.equals("vault3")) {
      if (!KoLCharacter.inNuclearAutumn() || Preferences.getInteger("falloutShelterLevel") < 7) {
        return false;
      }
      if (itemId == ItemPool.TRICK_TOT_LIBERTY || itemId == ItemPool.TRICK_TOT_UNICORN) {
        return KoLCharacter.findFamiliar(FamiliarPool.TRICK_TOT) != null;
      }
    } else if (storeId.equals("wildfire")) {
      if (!KoLCharacter.inFirecore()) {
        return false;
      }

      switch (itemId) {
        case ItemPool.BLART:
          return !Preferences.getBoolean("itemBoughtPerAscension10790");
        case ItemPool.RAINPROOF_BARREL_CAULK:
          return !Preferences.getBoolean("itemBoughtPerAscension10794");
        case ItemPool.PUMP_GREASE:
          return !Preferences.getBoolean("itemBoughtPerAscension10795");
      }
    }

    // If it gets this far, then the item is definitely available
    // for purchase from the NPC store.

    return true;
  }

  public static final int itemIdByRow(final String shopId, final int row) {
    List<NPCPurchaseRequest> items = NPCStoreDatabase.ROW_ITEMS.get(row);
    if (items == null) {
      // Worth a shot...
      return ConcoctionPool.rowToId(row);
    }

    for (NPCPurchaseRequest item : items) {
      if (shopId.equals(item.getStoreId())) {
        return item.getItemId();
      }
    }

    return -1;
  }

  public static final boolean contains(final int itemId) {
    return NPCStoreDatabase.contains(itemId, true);
  }

  public static final int price(final int itemId) {
    PurchaseRequest request = NPCStoreDatabase.getPurchaseRequest(itemId);
    return request == null ? 0 : request.getPrice();
  }

  public static final int availablePrice(final int itemId) {
    PurchaseRequest request = NPCStoreDatabase.getPurchaseRequest(itemId);
    return request == null || !request.canPurchase() ? 0 : request.getPrice();
  }

  public static final boolean contains(final int itemId, boolean validate) {
    PurchaseRequest item = NPCStoreDatabase.getPurchaseRequest(itemId);
    return item != null && (!validate || item.canPurchaseIgnoringMeat());
  }
}
