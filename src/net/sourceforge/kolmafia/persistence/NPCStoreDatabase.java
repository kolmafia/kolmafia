package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
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
  private static final HashMultimap<NPCPurchaseRequest> NPC_ITEMS = new HashMultimap<>();
  private static final HashMultimap<NPCPurchaseRequest> ROW_ITEMS = new HashMultimap<>();
  private static final AdventureResult RABBIT_HOLE =
      new AdventureResult("Down the Rabbit Hole", 1, true);
  private static final Map<String, String> storeNameById = new TreeMap<>();

  static {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("npcstores.txt", KoLConstants.NPCSTORES_VERSION)) {

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
                ? StringUtilities.parseInt(data[4].substring(3))
                : 0;

        if (row != 0) {
          ShopRowDatabase.registerShopRow(
              row, "npc", new AdventureResult(itemName, 1, false), storeName);
        }

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
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private NPCStoreDatabase() {}

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

  public static Optional<Integer> getQuantity(int itemId) {
    return switch (itemId) {
      case ItemPool.MIRACLE_WHIP -> Optional.of(
          Preferences.getBoolean("_mayoDeviceRented")
                  || Preferences.getBoolean("itemBoughtPerAscension8266")
              ? 0
              : 1);
      case ItemPool.SPHYGMAYOMANOMETER, ItemPool.REFLEX_HAMMER, ItemPool.MAYO_LANCE -> Optional.of(
          Preferences.getBoolean("_mayoDeviceRented") ? 0 : 1);
      case ItemPool.FEDORA_MOUNTED_FOUNTAIN,
          ItemPool.PORKPIE_MOUNTED_POPPER,
          ItemPool.SOMBRERO_MOUNTED_SPARKLER -> Optional.of(
          Preferences.getBoolean("_fireworksShopHatBought") ? 0 : 1);
      case ItemPool.CATHERINE_WHEEL, ItemPool.ROCKET_BOOTS, ItemPool.OVERSIZED_SPARKLER -> Optional
          .of(Preferences.getBoolean("_fireworksShopEquipmentBought") ? 0 : 1);
      case ItemPool.BLART, ItemPool.RAINPROOF_BARREL_CAULK, ItemPool.PUMP_GREASE -> Optional.of(1);
      default -> Optional.empty();
    };
  }

  private static int limitQuantity(int itemId) {
    return switch (itemId) {
      case ItemPool.ABRIDGED,
          ItemPool.ZEPPELIN_TICKET,
          ItemPool.FORGED_ID_DOCUMENTS,
          ItemPool.SPARE_KIDNEY,
          ItemPool.MIRACLE_WHIP,
          ItemPool.SPHYGMAYOMANOMETER,
          ItemPool.REFLEX_HAMMER,
          ItemPool.MAYO_LANCE,
          ItemPool.FEDORA_MOUNTED_FOUNTAIN,
          ItemPool.PORKPIE_MOUNTED_POPPER,
          ItemPool.SOMBRERO_MOUNTED_SPARKLER,
          ItemPool.CATHERINE_WHEEL,
          ItemPool.ROCKET_BOOTS,
          ItemPool.OVERSIZED_SPARKLER -> 1;
      default -> PurchaseRequest.MAX_QUANTITY;
    };
  }

  private static boolean canPurchase(
      final String storeId, final String shopName, final int itemId) {
    if (storeId == null) {
      return false;
    }

    if (KoLCharacter.isSavageBeast()) {
      return false;
    }

    switch (storeId) {
      case "armory" -> {
        // Armory and Leggery
        if (KoLCharacter.inZombiecore()
            || KoLCharacter.inNuclearAutumn()
            || KoLCharacter.isKingdomOfExploathing()) {
          return false;
        }
        if (itemId == ItemPool.FISHING_HAT) {
          return InventoryManager.hasItem(ItemPool.FISHING_POLE);
        }
        return true;
      }
      case "bartender" -> {
        // The Typical Tavern
        return !KoLCharacter.inZombiecore() && QuestLogRequest.isTavernAvailable();
      }
      case "bartlebys" -> {
        // Barrrtleby's Barrrgain Books
        boolean available = shopName.equals(NPCStoreDatabase.getStoreName(storeId));
        if (!available) {
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
      }
      case "blackmarket" -> {
        // The Black Market
        if (!QuestLogRequest.isBlackMarketAvailable()) {
          return false;
        }
        return switch (itemId) {
          case ItemPool.ZEPPELIN_TICKET -> !InventoryManager.hasItem(itemId);
          case ItemPool.SPARE_KIDNEY ->
          // Should check for whether your kidney has been stolen
          KoLCharacter.inBadMoon() && !InventoryManager.hasItem(itemId);
          case ItemPool.FORGED_ID_DOCUMENTS -> !QuestDatabase.isQuestLaterThan(
              Quest.MACGUFFIN, "step1");
          default -> true;
        };
      }
      case "bugbear" -> {
        // Bugbear Bakery
        if (KoLCharacter.inNuclearAutumn()) {
          return false;
        }
        return EquipmentManager.hasOutfit(OutfitPool.BUGBEAR_COSTUME);
      }
      case "chateau" -> {
        // Chateau Mantenga Gift Shop
        return ChateauRequest.chateauAvailable();
      }
      case "chinatown" -> {
        // Chinatown Shops
        return InventoryManager.getCount(ItemPool.STRANGE_GOGGLES) > 0
            && KoLConstants.campground.contains(ItemPool.get(ItemPool.SUSPICIOUS_JAR, 1));
      }
      case "crimbo18", "crimbo18giftomat" -> {
        // The Crimbo Cafe
        // Crimbo Town Gift-O-Mat
        return false;
      }
      case "crimbo19" -> {
        // The Crimbo Cafe
        return false;
      }
      case "crimbo20cafe", "crimbo20blackmarket" -> {
        // The Crimbo Cafe
        // The Black and White and Red All Over Market
        return false;
      }
      case "crimbo21cafe", "crimbo21ornaments" -> {
        // The Crimbo Cafe
        // Ornament Stand
        return false;
      }
      case "doc" -> {
        // Doc Galaktik's Medicine Show
        if (KoLCharacter.inZombiecore()
            || KoLCharacter.inNuclearAutumn()
            || KoLCharacter.isKingdomOfExploathing()) {
          return false;
        }
        if (itemId == ItemPool.DOC_VITALITY_SERUM) {
          return QuestDatabase.isQuestFinished(Quest.DOC);
        }
        return true;
      }
      case "fdkol" -> {
        // FDKOL Requisitions Tent
        return false;
      }
      case "fwshop" -> {
        // Clan Underground Fireworks Shop
        if (Preferences.getBoolean("_fireworksShop") == false) {
          return false;
        }

        return switch (itemId) {
          case ItemPool.FEDORA_MOUNTED_FOUNTAIN,
              ItemPool.PORKPIE_MOUNTED_POPPER,
              ItemPool.SOMBRERO_MOUNTED_SPARKLER -> Preferences.getBoolean(
                  "_fireworksShopHatBought")
              == false;
          case ItemPool.CATHERINE_WHEEL,
              ItemPool.ROCKET_BOOTS,
              ItemPool.OVERSIZED_SPARKLER -> Preferences.getBoolean("_fireworksShopEquipmentBought")
              == false;
          default -> true;
        };
      }
      case "generalstore" -> {
        // The General Store

        if (KoLCharacter.inNuclearAutumn()) {
          return false;
        }

        // Some items restricted, often because of holidays
        String holiday = HolidayDatabase.getHoliday();

        return switch (itemId) {
          case ItemPool.MARSHMALLOW -> holiday.contains("Yuletide");
          case ItemPool.OYSTER_BASKET -> holiday.contains("Oyster Egg Day");
          case ItemPool.PARTY_HAT -> holiday.contains("Festival of Jarlsberg");
          case ItemPool.M282, ItemPool.SNAKE, ItemPool.SPARKLER -> holiday.contains(
              "Dependence Day");
          case ItemPool.GREEN_ROCKET -> holiday.contains("Dependence Day")
              && holiday.contains("St. Sneaky Pete's Day");
          case ItemPool.FOAM_NOODLE, ItemPool.INFLATABLE_DUCK, ItemPool.WATER_WINGS -> holiday
              .contains("Generic Summer Holiday");
          case ItemPool.DESERT_BUS_PASS -> !KoLCharacter.desertBeachAccessible();
          case ItemPool.FOLDER_01, ItemPool.FOLDER_02, ItemPool.FOLDER_03 -> {
            AdventureResult folderHolder = ItemPool.get(ItemPool.FOLDER_HOLDER);
            if (folderHolder.getCount(KoLConstants.inventory) > 0
                || folderHolder.getCount(KoLConstants.closet) > 0
                || folderHolder.getCount(KoLConstants.collection) > 0
                || KoLCharacter.hasEquipped(folderHolder)) {
              yield true;
            }
            if (KoLCharacter.inLegacyOfLoathing()) {
              AdventureResult replicaFolderHolder = ItemPool.get(ItemPool.REPLICA_FOLDER_HOLDER);
              yield replicaFolderHolder.getCount(KoLConstants.inventory) > 0
                  || KoLCharacter.hasEquipped(replicaFolderHolder);
            }
            yield false;
          }
          case ItemPool.WATER_WINGS_FOR_BABIES,
              ItemPool.MINI_LIFE_PRESERVER,
              ItemPool.HEAVY_DUTY_UMBRELLA,
              ItemPool.POOL_SKIMMER -> KoLCharacter.inRaincore();
          case ItemPool.FISHING_LINE -> InventoryManager.hasItem(ItemPool.FISHING_POLE);
          case ItemPool.TRICK_TOT_UNICORN, ItemPool.TRICK_TOT_CANDY -> KoLCharacter.usableFamiliar(
                  FamiliarPool.TRICK_TOT)
              != null;
          default -> true;
        };
      }
      case "gnoll" -> {
        // Degrassi Knoll Bakery and Hardware Store
        return KoLCharacter.knollAvailable();
      }
      case "gnomart" -> {
        // Gno-Mart
        return !KoLCharacter.inZombiecore() && KoLCharacter.gnomadsAvailable();
      }
      case "guildstore1" -> {
        // Shadowy Store
        return KoLCharacter.isMoxieClass() && KoLCharacter.getGuildStoreOpen();
      }
      case "guildstore2" -> {
        // Gouda's Grimoire and Grocery
        return (KoLCharacter.isMysticalityClass()
                || (KoLCharacter.isAccordionThief() && KoLCharacter.getLevel() >= 9))
            && KoLCharacter.getGuildStoreOpen();
      }
      case "guildstore3" -> {
        // Smacketeria
        return ((KoLCharacter.isMuscleClass() && !KoLCharacter.isAvatarOfBoris())
                || (KoLCharacter.isAccordionThief() && KoLCharacter.getLevel() >= 9))
            && KoLCharacter.getGuildStoreOpen();
      }
      case "hiddentavern" -> {
        // The Hidden Tavern
        return Preferences.getInteger("hiddenTavernUnlock") == KoLCharacter.getAscensions();
      }
      case "hippy" -> {
        // Hippy Store (Pre-War)
        // Hippy Store (Hippy)
        // Hippy Store (Fratboy)

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
        switch (shopName) {
          case "Hippy Store (Hippy)" -> {
            if (!Preferences.getString("currentHippyStore").equals("hippy")) {
              return false;
            }

            outfit = OutfitPool.WAR_HIPPY_OUTFIT;
          }
          case "Hippy Store (Fratboy)" -> {
            if (!Preferences.getString("currentHippyStore").equals("fratboy")) {
              return false;
            }

            outfit = OutfitPool.WAR_FRAT_OUTFIT;
          }
          default -> {
            // What is this?
            return false;
          }
        }

        return QuestLogRequest.isHippyStoreAvailable() || EquipmentManager.hasOutfit(outfit);
      }
      case "jewelers" -> {
        // Little Canadia Jewelers
        return !KoLCharacter.inZombiecore() && KoLCharacter.canadiaAvailable();
      }
      case "knobdisp" -> {
        // The Knob Dispensary
        return KoLCharacter.getDispensaryOpen();
      }
      case "madeline" -> {
        // Madeline's Baking Supply
        return QuestDatabase.isQuestFinished(Quest.ARMORER);
      }
      case "mayoclinic" -> {
        // The Mayo Clinic
        boolean available = false;
        AdventureResult workshedItem = CampgroundRequest.getCurrentWorkshedItem();
        if (workshedItem != null) {
          available =
              workshedItem.getItemId() == ItemPool.MAYO_CLINIC
                  && StandardRequest.isAllowed(RestrictedItemType.ITEMS, "portable Mayo Clinic");
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
      }
      case "meatsmith" -> {
        // Meatsmith's Shop
        return !KoLCharacter.inZombiecore()
            && !KoLCharacter.inNuclearAutumn()
            && !KoLCharacter.isKingdomOfExploathing();
      }
      case "mystic" -> {
        // The Crackpot Mystic's Shed
        if (KoLCharacter.isKingdomOfExploathing()) {
          return false;
        }
        // The following is not complete:
        // Items unlocked by beating all four bosses in the Crackpot Mystic's Psychoses:
        //    pixel energy tank
        //    pixel grappling hook
        //    pixel pill
        return switch (itemId) {
          case ItemPool.YELLOW_SUBMARINE -> !KoLCharacter.desertBeachAccessible();
          default -> true;
        };
      }
      case "nerve" -> {
        // Nervewrecker's Store
        return KoLCharacter.inBadMoon();
      }
      case "town_giftshop.php" -> {
        // Gift Shop
        if (KoLCharacter.inBadMoon() || KoLCharacter.isKingdomOfExploathing()) {
          return false;
        }

        // Some items restricted, because of holidays or number of ascensions
        String holiday = HolidayDatabase.getHoliday();
        int asc = KoLCharacter.getAscensions();

        return switch (itemId) {
          case ItemPool.VALENTINE,
              ItemPool.CHOCOLATE_COVERED_DIAMOND_STUDDED_ROSES,
              ItemPool.BOUQUET_OF_CIRCULAR_SAW_BLADES,
              ItemPool.BETTER_THAN_CUDDLING_CAKE,
              ItemPool.STUFFED_NINJA_SNOWMAN -> holiday.contains("Valentine's Day");
          case ItemPool.POTTED_FERN, ItemPool.HAPPY_BIRTHDAY_CLAUDE_CAKE -> asc >= 1;
          case ItemPool.STUFFED_GHUOL_WHELP, ItemPool.HEART_SHAPED_BALLOON -> asc >= 2;
          case ItemPool.TULIP, ItemPool.PERSONALIZED_BIRTHDAY_CAKE -> asc >= 4;
          case ItemPool.STUFFED_ZMOBIE, ItemPool.ANNIVERSARY_BALLOON -> asc >= 5;
          case ItemPool.VENUS_FLYTRAP, ItemPool.THREE_TIERED_WEDDING_CAKE -> asc >= 7;
          case ItemPool.RAGGEDY_HIPPY_DOLL, ItemPool.MYLAR_BALLOON -> asc >= 8;
          case ItemPool.ALL_PURPOSE_FLOWER, ItemPool.BABYCAKES -> asc >= 10;
          case ItemPool.STUFFED_STAB_BAT, ItemPool.KEVLAR_BALLOON -> asc >= 11;
          case ItemPool.EXOTIC_ORCHID, ItemPool.BLUE_VELVET_CAKE -> asc >= 13;
          case ItemPool.APATHETIC_LIZARDMAN_DOLL, ItemPool.THOUGHT_BALLOON -> asc >= 14;
          case ItemPool.LONG_STEMMED_ROSE, ItemPool.CONGRATULATORY_CAKE -> asc >= 16;
          case ItemPool.STUFFED_YETI, ItemPool.RAT_BALLOON -> asc >= 17;
          case ItemPool.GILDED_LILY, ItemPool.ANGEL_FOOD_CAKE -> asc >= 19;
          case ItemPool.STUFFED_MOB_PENGUIN, ItemPool.MINI_ZEPPELIN -> asc >= 20;
          case ItemPool.DEADLY_NIGHTSHADE, ItemPool.DEVILS_FOOD_CAKE -> asc >= 22;
          case ItemPool.STUFFED_SABRE_TOOTHED_LIME, ItemPool.MR_BALLOON -> asc >= 23;
          case ItemPool.BLACK_LOTUS, ItemPool.BIRTHDAY_PARTY_JELLYBEAN_CHEESECAKE -> asc >= 25;
          case ItemPool.GIANT_STUFFED_BUGBEAR, ItemPool.RED_BALLOON -> asc >= 26;
          default -> true;
        };
      }
      case "tweedle" -> {
        // The Tweedleporium
        return KoLConstants.activeEffects.contains(NPCStoreDatabase.RABBIT_HOLE);
      }
      case "unclep" -> {
        // Uncle P's Antiques
        return !KoLCharacter.inZombiecore()
            && !KoLCharacter.inNuclearAutumn()
            && KoLCharacter.desertBeachAccessible();
      }
      case "whitecitadel" -> {
        // White Citadel
        return QuestLogRequest.isWhiteCitadelAvailable();
      }
      case "vault1" -> {
        // Fallout Shelter Medical Supply
        if (!KoLCharacter.inNuclearAutumn() || Preferences.getInteger("falloutShelterLevel") < 2) {
          return false;
        }
        return switch (itemId) {
          case ItemPool.TRICK_TOT_CANDY, ItemPool.TRICK_TOT_EYEBALL -> KoLCharacter.usableFamiliar(
                  FamiliarPool.TRICK_TOT)
              != null;
          default -> true;
        };
      }
      case "vault2" -> {
        // Fallout Shelter Electronics Supply
        if (!KoLCharacter.inNuclearAutumn() || Preferences.getInteger("falloutShelterLevel") < 4) {
          return false;
        }
        return switch (itemId) {
          case ItemPool.TRICK_TOT_KNIGHT, ItemPool.TRICK_TOT_ROBOT -> KoLCharacter.usableFamiliar(
                  FamiliarPool.TRICK_TOT)
              != null;
          default -> true;
        };
      }
      case "vault3" -> {
        // Underground Record Store
        if (!KoLCharacter.inNuclearAutumn() || Preferences.getInteger("falloutShelterLevel") < 7) {
          return false;
        }
        return switch (itemId) {
          case ItemPool.TRICK_TOT_LIBERTY, ItemPool.TRICK_TOT_UNICORN -> KoLCharacter
                  .usableFamiliar(FamiliarPool.TRICK_TOT)
              != null;
          default -> true;
        };
      }
      case "wildfire" -> {
        // FDKOL Auxiliary
        if (!KoLCharacter.inFirecore()) {
          return false;
        }

        return switch (itemId) {
          case ItemPool.BLART -> !Preferences.getBoolean("itemBoughtPerAscension10790");
          case ItemPool.RAINPROOF_BARREL_CAULK -> !Preferences.getBoolean(
              "itemBoughtPerAscension10794");
          case ItemPool.PUMP_GREASE -> !Preferences.getBoolean("itemBoughtPerAscension10795");
          default -> true;
        };
      }
    }

    // If we get here, we don't recognize the shopId.
    // Assume the item is available for purchase.

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

  public static final long price(final int itemId) {
    PurchaseRequest request = NPCStoreDatabase.getPurchaseRequest(itemId);
    return request == null ? 0 : request.getPrice();
  }

  public static final long availablePrice(final int itemId) {
    PurchaseRequest request = NPCStoreDatabase.getPurchaseRequest(itemId);
    return request == null || !request.canPurchase() ? 0 : request.getPrice();
  }

  public static final boolean contains(final int itemId, boolean validate) {
    PurchaseRequest item = NPCStoreDatabase.getPurchaseRequest(itemId);
    return item != null && (!validate || item.canPurchaseIgnoringMeat());
  }

  public static void reset() {
    for (int itemId : NPCStoreDatabase.NPC_ITEMS.keySet()) {
      for (NPCPurchaseRequest request : NPCStoreDatabase.NPC_ITEMS.get(itemId)) {
        int quantity = NPCStoreDatabase.limitQuantity(itemId);

        request.setQuantity(quantity);
        request.setLimit(quantity);
        request.setCanPurchase(true);
      }
    }
  }
}
