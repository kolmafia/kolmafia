package net.sourceforge.kolmafia.session;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.listener.ItemListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.modifiers.Lookup;
import net.sourceforge.kolmafia.modifiers.MultiStringModifier;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.Attribute;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.RestoresDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest.ClanStashRequestType;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.ClosetRequest.ClosetRequestType;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.ManageStoreRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.StorageRequest.StorageRequestType;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.coinmaster.HermitRequest;
import net.sourceforge.kolmafia.request.concoction.CombineMeatRequest;
import net.sourceforge.kolmafia.request.concoction.CreateItemRequest;
import net.sourceforge.kolmafia.request.concoction.SewerRequest;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;

@SuppressWarnings("incomplete-switch")
public abstract class InventoryManager {
  private static final int BULK_PURCHASE_AMOUNT = 30;

  // Number of days which is considered "too old" for a cached mall price.
  public static final float MALL_PRICE_AGE = 7.0f;

  private static int askedAboutCrafting = 0;

  public static void resetInventory() {
    KoLConstants.inventory.clear();
  }

  public static void refresh() {
    // Retrieve the contents of inventory via api.php
    ApiRequest.updateInventory();
    // Items in inventory can grant modifiers.
    // For example, Cincho de Mayo gives 3 free rests.
    KoLCharacter.recalculateAdjustments();
  }

  public static final void parseInventory(final JSONObject json) {
    if (json == null) {
      return;
    }

    List<AdventureResult> items = new ArrayList<>();
    List<AdventureResult> unlimited = new ArrayList<>();

    try {
      // {"1":"1","2":"1" ... }
      for (String key : json.keySet()) {
        int itemId = StringUtilities.parseInt(key);
        int count = json.getIntValue(key);
        String name = ItemDatabase.getItemDataName(itemId);
        if (name == null) {
          // Fetch descid from api.php?what=item
          // and register new item.
          ItemDatabase.registerItem(itemId);
        }

        if (KoLCharacter.getLimitMode().limitItem(itemId)) {
          unlimited.add(ItemPool.get(itemId, count));
        } else {
          items.add(ItemPool.get(itemId, count));
          switch (itemId) {
            case ItemPool.BOOMBOX:
              if (!Preferences.getString("boomBoxSong").equals("")) {
                KoLCharacter.addAvailableSkill(SkillPool.SING_ALONG);
              }
              break;
          }
        }
      }
    } catch (JSONException e) {
      ApiRequest.reportParseError("inventory", json.toString(), e);
      return;
    }

    KoLConstants.inventory.clear();
    KoLConstants.inventory.addAll(items);
    KoLConstants.unlimited.clear();
    KoLConstants.unlimited.addAll(unlimited);
    EquipmentManager.updateEquipmentLists();
    ConcoctionDatabase.refreshConcoctions();
    PreferenceListenerRegistry.firePreferenceChanged("(hats)");
  }

  public static final int getCount(final int itemId) {
    return InventoryManager.getCount(ItemPool.get(itemId, 1));
  }

  public static final int getCount(final AdventureResult item) {
    return item.getCount(KoLConstants.inventory);
  }

  public static final boolean hasItem(final int itemId) {
    return InventoryManager.hasItem(itemId, false);
  }

  public static final boolean hasItem(final int itemId, final boolean shouldCreate) {
    return InventoryManager.hasItem(ItemPool.get(itemId, 1), shouldCreate);
  }

  public static final boolean hasItem(final AdventureResult item) {
    return InventoryManager.hasItem(item, false);
  }

  public static final boolean hasItem(final AdventureResult item, final boolean shouldCreate) {
    int count = InventoryManager.getAccessibleCount(item);

    if (shouldCreate) {
      CreateItemRequest creation = CreateItemRequest.getInstance(item);
      if (creation != null) {
        count += creation.getQuantityPossible();
      }
    }

    return count > 0 && count >= item.getCount();
  }

  public static final int getAccessibleCount(final int itemId) {
    return getAccessibleCount(itemId, true);
  }

  public static final int getAccessibleCount(final int itemId, final boolean includeStash) {
    return InventoryManager.getAccessibleCount(ItemPool.get(itemId, 1), includeStash);
  }

  public static final int getAccessibleCount(final AdventureResult item) {
    return getAccessibleCount(item, true);
  }

  public static final int getAccessibleCount(
      final AdventureResult item, final boolean includeStash) {
    if (item == null) {
      return 0;
    }

    int itemId = item.getItemId();

    if (itemId <= 0) {
      return 0;
    }

    // Agree with what retrieveItem looks at
    if (itemId == HermitRequest.WORTHLESS_ITEM.getItemId()) {
      return HermitRequest.getAvailableWorthlessItemCount();
    }

    // If this item is restricted, ignore it entirely.
    if (!ItemDatabase.isAllowed(item)) {
      return 0;
    }

    int count = item.getCount(KoLConstants.inventory);

    // Items in closet might be accessible, but if the user has
    // marked items in the closet as out-of-bounds, honor that.
    if (InventoryManager.canUseCloset()) {
      count += item.getCount(KoLConstants.closet);
    }

    if ((!KoLCharacter.inLegacyOfLoathing() || pullableInLoL(itemId))
        && (!KoLCharacter.inSeaPath() || pullableInSeaPath(itemId))) {
      // Free Pulls from Hagnk's are always accessible
      count += item.getCount(KoLConstants.freepulls);

      // Storage and your clan stash are always accessible
      // once you are out of Ronin or have freed the king,
      // but the user can mark either as out-of-bounds
      if (InventoryManager.canUseStorage()) {
        count += item.getCount(KoLConstants.storage);
      }
    }

    if (InventoryManager.canUseClanStash() && includeStash) {
      count += item.getCount(ClanManager.getStash());
    }

    count += InventoryManager.getEquippedCount(item, true);

    return count;
  }

  public static final int getEquippedCount(final int itemId) {
    return InventoryManager.getEquippedCount(ItemPool.get(itemId, 1));
  }

  public static final int getEquippedCount(final AdventureResult item) {
    return getEquippedCount(item, false);
  }

  public static final int getEquippedCount(
      final AdventureResult item, boolean includeAllFamiliars) {
    int count = 0;
    for (var slot : SlotSet.SLOTS) {
      AdventureResult equipment = EquipmentManager.getEquipment(slot);
      if (equipment != null && equipment.getItemId() == item.getItemId()) {
        ++count;
      }
    }
    if (KoLCharacter.inHatTrick()) {
      for (var hat : EquipmentManager.getHatTrickHats()) {
        if (hat == item.getItemId()) {
          ++count;
        }
      }
    }
    if (includeAllFamiliars) {
      for (FamiliarData current : KoLCharacter.ownedFamiliars()) {
        if (!current.equals(KoLCharacter.getFamiliar())
            && current.getItem() != null
            && current.getItem().equals(item)) {
          ++count;
        }
      }
    }
    return count;
  }

  public static boolean equippedOrInInventory(int itemId) {
    return equippedOrInInventory(ItemPool.get(itemId));
  }

  public static boolean equippedOrInInventory(AdventureResult equip) {
    return KoLCharacter.hasEquipped(equip) || InventoryManager.getCount(equip) > 0;
  }

  public static final boolean checkpointedRetrieveItem(final int itemId) {
    try (Checkpoint checkpoint = new Checkpoint()) {
      return InventoryManager.retrieveItem(ItemPool.get(itemId, 1), true, true, true);
    }
  }

  public static final boolean retrieveItem(final int itemId) {
    return InventoryManager.retrieveItem(ItemPool.get(itemId, 1), true, true, true);
  }

  public static final boolean retrieveItem(final int itemId, final boolean isAutomated) {
    return InventoryManager.retrieveItem(ItemPool.get(itemId, 1), isAutomated, true, true);
  }

  public static final boolean retrieveItem(
      final int itemId, final boolean isAutomated, final boolean useEquipped) {
    return InventoryManager.retrieveItem(ItemPool.get(itemId, 1), isAutomated, useEquipped, true);
  }

  public static final boolean retrieveItem(
      final int itemId,
      final boolean isAutomated,
      final boolean useEquipped,
      final boolean canCreate) {
    return InventoryManager.retrieveItem(
        ItemPool.get(itemId, 1), isAutomated, useEquipped, canCreate);
  }

  public static final boolean retrieveItem(final int itemId, final int count) {
    return InventoryManager.retrieveItem(ItemPool.get(itemId, count), true, true, true);
  }

  public static final boolean checkpointedRetrieveItem(final int itemId, final int count) {
    try (Checkpoint checkpoint = new Checkpoint()) {
      return InventoryManager.retrieveItem(itemId, count);
    }
  }

  public static final boolean retrieveItem(
      final int itemId, final int count, final boolean isAutomated) {
    return InventoryManager.retrieveItem(ItemPool.get(itemId, count), isAutomated, true, true);
  }

  public static final boolean retrieveItem(
      final int itemId, final int count, final boolean isAutomated, final boolean useEquipped) {
    return InventoryManager.retrieveItem(
        ItemPool.get(itemId, count), isAutomated, useEquipped, true);
  }

  public static final boolean retrieveItem(
      final int itemId,
      final int count,
      final boolean isAutomated,
      final boolean useEquipped,
      final boolean canCreate) {
    return InventoryManager.retrieveItem(
        ItemPool.get(itemId, count), isAutomated, useEquipped, canCreate);
  }

  public static final boolean retrieveItem(final String itemName) {
    return InventoryManager.retrieveItem(ItemPool.get(itemName, 1), true, true, true);
  }

  public static final boolean retrieveItem(final String itemName, final boolean isAutomated) {
    return InventoryManager.retrieveItem(ItemPool.get(itemName, 1), isAutomated, true, true);
  }

  public static final boolean retrieveItem(
      final String itemName, final boolean isAutomated, final boolean useEquipped) {
    return InventoryManager.retrieveItem(ItemPool.get(itemName, 1), isAutomated, useEquipped, true);
  }

  public static final boolean retrieveItem(
      final String itemName,
      final boolean isAutomated,
      final boolean useEquipped,
      final boolean canCreate) {
    return InventoryManager.retrieveItem(
        ItemPool.get(itemName, 1), isAutomated, useEquipped, canCreate);
  }

  public static final boolean retrieveItem(final String itemName, final int count) {
    return InventoryManager.retrieveItem(ItemPool.get(itemName, count), true, true, true);
  }

  public static final boolean retrieveItem(
      final String itemName, final int count, final boolean isAutomated) {
    return InventoryManager.retrieveItem(ItemPool.get(itemName, count), isAutomated, true, true);
  }

  public static final boolean retrieveItem(
      final String itemName,
      final int count,
      final boolean isAutomated,
      final boolean useEquipped) {
    return InventoryManager.retrieveItem(
        ItemPool.get(itemName, count), isAutomated, useEquipped, true);
  }

  public static final boolean retrieveItem(
      final String itemName,
      final int count,
      final boolean isAutomated,
      final boolean useEquipped,
      final boolean canCreate) {
    return InventoryManager.retrieveItem(
        ItemPool.get(itemName, count), isAutomated, useEquipped, canCreate);
  }

  public static final boolean retrieveItem(final AdventureResult item) {
    return InventoryManager.retrieveItem(item, true, true, true);
  }

  public static final boolean checkpointedRetrieveItem(final AdventureResult item) {
    try (Checkpoint checkpoint = new Checkpoint()) {
      return InventoryManager.retrieveItem(item);
    }
  }

  public static final boolean retrieveItem(final AdventureResult item, final boolean isAutomated) {
    return InventoryManager.retrieveItem(item, isAutomated, true, true);
  }

  public static final boolean retrieveItem(
      final AdventureResult item, final boolean isAutomated, final boolean useEquipped) {
    return InventoryManager.retrieveItem(item, isAutomated, useEquipped, true);
  }

  public static final boolean retrieveItem(
      final AdventureResult item,
      final boolean isAutomated,
      final boolean useEquipped,
      boolean canCreate) {
    String rv = InventoryManager.retrieveItem(item, isAutomated, useEquipped, canCreate, false);
    if (rv == null) {
      return false;
    }
    if (rv.equals("")) {
      if (EquipmentDatabase.isHat(item)) {
        PreferenceListenerRegistry.firePreferenceChanged("(hats)");
      }
      return true;
    }
    RequestLogger.printLine("INTERNAL ERROR: retrieveItem returned string when not simulating!");
    return true;
  }

  public static final String simRetrieveItem(final int itemId) {
    return InventoryManager.simRetrieveItem(ItemPool.get(itemId, 1), true);
  }

  public static final String simRetrieveItem(final int itemId, final boolean isAutomated) {
    return InventoryManager.simRetrieveItem(ItemPool.get(itemId, 1), isAutomated);
  }

  public static final String simRetrieveItem(final String itemName) {
    return InventoryManager.simRetrieveItem(ItemPool.get(itemName, 1), true);
  }

  public static final String simRetrieveItem(final String itemName, final boolean isAutomated) {
    return InventoryManager.simRetrieveItem(ItemPool.get(itemName, 1), isAutomated);
  }

  public static final String simRetrieveItem(final AdventureResult item) {
    return InventoryManager.simRetrieveItem(item, true, true, true);
  }

  public static final String simRetrieveItem(
      final AdventureResult item, final boolean isAutomated) {
    return InventoryManager.simRetrieveItem(item, isAutomated, true, true);
  }

  public static final String simRetrieveItem(
      final AdventureResult item, final boolean isAutomated, final boolean useEquipped) {
    return InventoryManager.simRetrieveItem(item, isAutomated, useEquipped, true);
  }

  public static final String simRetrieveItem(
      final AdventureResult item,
      final boolean isAutomated,
      final boolean useEquipped,
      final boolean canCreate) {
    String rv = InventoryManager.retrieveItem(item, isAutomated, useEquipped, canCreate, true);
    if (rv == null || rv.equals("")) {
      RequestLogger.printLine("INTERNAL ERROR: retrieveItem didn't return string when simulating!");
      return "buggy";
    }
    return rv;
  }

  private static String retrieveItem(
      final AdventureResult item,
      final boolean isAutomated,
      final boolean useEquipped,
      final boolean canCreate,
      final boolean sim) {
    return InventoryManager.doRetrieveItem(item, isAutomated, useEquipped, sim, canCreate);
  }

  // When called with sim=true, retrieveItem should return a non-empty string
  // indicating how at least some quantity of the item would be retrieved.
  // There are two distinguished return values: "have" indicates trivial
  // success, "fail" indicates unavoidable failure.  No side-effects, please!
  //
  // When called with sim=false, it should return "" for success (equivalent
  // to the previous return value of true), null for failure (previously false).

  private static String doRetrieveItem(
      final AdventureResult item,
      final boolean isAutomated,
      final boolean useEquipped,
      final boolean sim,
      final boolean canCreate) {
    if (item.isMeat()) {
      long available = KoLCharacter.getAvailableMeat();
      long needed = item.getLongCount();
      if (needed > available) {
        if (sim) {
          return "fail";
        }
        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You need " + (needed - available) + " more Meat to continue.");
        return null;
      }
      return sim ? "have" : "";
    }

    int itemId = item.getItemId();

    if (itemId < 0) {
      // See if it is a Coin Master token.
      Concoction concoction = ConcoctionPool.get(item);
      String property = concoction != null ? concoction.property : null;
      if (property == null) {
        if (sim) {
          return "fail";
        }

        KoLmafia.updateDisplay(MafiaState.ERROR, "Don't know how to retrieve a " + item.getName());
        return null;
      }

      int have = Preferences.getInteger(property);
      int need = item.getCount() - have;
      if (need > 0) {
        if (sim) {
          return "fail";
        }

        KoLmafia.updateDisplay(
            MafiaState.ERROR, "You need " + need + " more " + item.getName() + " to continue.");
        return null;
      }

      return sim ? "have" : "";
    }

    if (itemId == 0) {
      return sim ? "pretend to have" : "";
    }

    if (itemId == ItemPool.WORTHLESS_ITEM) {
      // Retrieve worthless items using special techniques.
      if (sim) {
        return "chewing gum";
      }

      boolean success =
          SewerRequest.retrieveSewerItems(
              item, InventoryManager.canUseCloset(), InventoryManager.canUseStorage());
      return success ? "" : null;
    }

    // If it is a virtual item, see if we already bought it
    if (ItemDatabase.isVirtualItem(itemId)) {
      if (ItemDatabase.haveVirtualItem(itemId)) {
        return sim ? "have" : "";
      }
    }

    int availableCount = item.getCount(KoLConstants.inventory);
    int missingCount = item.getCount() - availableCount;

    // If you already have enough of the given item, then return
    // from this method.

    if (missingCount <= 0) {
      return sim ? "have" : "";
    }

    // Handle the bridge by untinkering the abridged dictionary
    // You can have at most one of these.

    if (itemId == ItemPool.BRIDGE) {
      if (InventoryManager.hasItem(ItemPool.ABRIDGED)) {
        if (sim) {
          return "untinker";
        }

        RequestThread.postRequest(new UntinkerRequest(ItemPool.ABRIDGED, 1));
      }

      if (sim) {
        return "fail";
      }

      return item.getCount(KoLConstants.inventory) > 0 ? "" : null;
    }

    boolean isRestricted = !ItemDatabase.isAllowed(item);
    CraftingType mixingMethod = ConcoctionDatabase.getMixingMethod(item);
    boolean coinmasterCreation = mixingMethod == CraftingType.COINMASTER;
    boolean shouldUseCoinmasters = InventoryManager.canUseCoinmasters(itemId);
    CreateItemRequest creator =
        canCreate && (!coinmasterCreation || shouldUseCoinmasters)
            ? CreateItemRequest.getInstance(item)
            : null;

    // If this item is restricted, we might be able to create it.
    // If we can't, give up now; we cannot obtain it in any way.

    if (isRestricted && creator == null) {
      return sim ? "fail" : null;
    }

    // Don't waste time checking familiars and equipment for
    // restricted items or non-equipment.
    // In QT, we'll just unequip it in the next code block
    if (!isRestricted && ItemDatabase.isEquipment(itemId) && !KoLCharacter.inQuantum()) {
      for (FamiliarData current : KoLCharacter.ownedFamiliars()) {
        if (current.getItem() != null && current.getItem().equals(item)) {
          if (sim) {
            return "steal";
          }

          KoLmafia.updateDisplay(
              "Stealing "
                  + item.getName()
                  + " from "
                  + current.getName()
                  + " the "
                  + current.getRace()
                  + "...");
          FamiliarRequest request = new FamiliarRequest(current, EquipmentRequest.UNEQUIP);
          RequestThread.postRequest(request);

          if (--missingCount <= 0) {
            return "";
          }

          // Keep going; generic familiar equipment might
          // be retrievable from multiple familiars.
        }
      }
    }

    if (!isRestricted && ItemDatabase.isEquipment(itemId) && useEquipped) {
      for (var i : SlotSet.SLOTS) {
        // If you are dual-wielding the target item,
        // remove the one in the offhand slot first
        // since taking from the weapon slot will drop
        // the offhand weapon.
        Slot slot = i == Slot.WEAPON ? Slot.OFFHAND : i == Slot.OFFHAND ? Slot.WEAPON : i;

        if (EquipmentManager.getEquipment(slot).equals(item)) {
          if (sim) {
            return "remove";
          }

          RequestThread.postRequest(new EquipmentRequest(EquipmentRequest.UNEQUIP, slot));

          if (--missingCount <= 0) {
            return "";
          }
        }
      }
    }
    // Attempt to pull the item from the closet.

    boolean shouldUseCloset = InventoryManager.canUseCloset();
    if (shouldUseCloset) {
      int itemCount = item.getCount(KoLConstants.closet);
      if (itemCount > 0) {
        if (sim) {
          return "uncloset";
        }

        int retrieveCount = Math.min(itemCount, missingCount);
        RequestThread.postRequest(
            new ClosetRequest(
                ClosetRequestType.CLOSET_TO_INVENTORY, item.getInstance(retrieveCount)));
        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }
      }
    }

    // If the item is a free pull from Hagnk's, pull it

    if (!isRestricted) {
      int itemCount = item.getCount(KoLConstants.freepulls);
      if (itemCount > 0) {
        if (sim) {
          return "free pull";
        }

        int retrieveCount = Math.min(itemCount, missingCount);
        RequestThread.postRequest(
            new StorageRequest(
                StorageRequestType.STORAGE_TO_INVENTORY, item.getInstance(retrieveCount)));
        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }
      }
    }

    // Attempt to pull the items out of storage, if you are out of
    // ronin and the user wishes to use storage

    if (!isRestricted && InventoryManager.canUseStorage()) {
      int itemCount = item.getCount(KoLConstants.storage);

      if (itemCount > 0) {
        if (sim) {
          return "pull";
        }

        int retrieveCount = Math.min(itemCount, missingCount);
        RequestThread.postRequest(
            new StorageRequest(
                StorageRequestType.STORAGE_TO_INVENTORY, item.getInstance(retrieveCount)));
        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }
      }
    }

    // Attempt to pull the item from the clan stash, if it is
    // available there and the user wishes to use the stash

    if (!isRestricted && InventoryManager.canUseClanStash()) {
      int itemCount = item.getCount(ClanManager.getStash());

      if (itemCount > 0) {
        if (sim) {
          return "unstash";
        }

        int retrieveCount =
            Math.min(itemCount, InventoryManager.getPurchaseCount(itemId, missingCount));
        RequestThread.postRequest(
            new ClanStashRequest(
                item.getInstance(retrieveCount), ClanStashRequestType.STASH_TO_ITEMS));
        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }
      }
    }

    // From here on, we will consider buying the item. Decide if we
    // want to use only NPCs or if the mall is possible.

    boolean shouldUseNPCStore = InventoryManager.canUseNPCStores(item);

    boolean forceNoMall = isRestricted;

    if (!forceNoMall) {
      if (shouldUseNPCStore) {
        // If Price from NPC store is 100 or below and available, never try mall.
        long NPCPrice = NPCStoreDatabase.availablePrice(itemId);
        int autosellPrice = ItemDatabase.getPriceById(itemId);
        if (NPCPrice > 0 && NPCPrice <= Math.max(100, autosellPrice * 2)) {
          forceNoMall = true;
        }
      }

      // Things that we can construct out of pure Meat cannot
      // possibly be cheaper to buy.
      if (creator instanceof CombineMeatRequest) {
        forceNoMall = true;
      }
    }

    boolean shouldUseMall = !forceNoMall && InventoryManager.canUseMall(item);
    boolean shouldUseShop = shouldUseMall && InventoryManager.canUseShop(item);
    boolean haveBuyScript = Preferences.getString("buyScript").trim().length() > 0;
    boolean scriptSaysBuy = false;

    // Attempt to create the item from existing ingredients (if
    // possible).  The user's buyScript can kick in here and force
    // it to be purchased, rather than created

    Concoction concoction = ConcoctionPool.get(item);
    boolean asked = false;

    if (creator != null && creator.getQuantityPossible() > 0) {
      if (!forceNoMall) {
        AdventureResult instance = item.getInstance(missingCount);
        boolean defaultBuy = shouldUseMall && InventoryManager.cheaperToBuy(instance);
        if (sim && haveBuyScript) {
          return defaultBuy ? "create or buy" : "create";
        }
        scriptSaysBuy = InventoryManager.invokeBuyScript(instance, 2, defaultBuy);
        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }
      }

      if (sim) {
        return scriptSaysBuy ? "buy" : "create or buy";
      }

      if (!scriptSaysBuy) {
        // Prompt about adventures if we make it here.
        creator.setQuantityNeeded(Math.min(missingCount, creator.getQuantityPossible()));

        if (isAutomated
            && concoction != null
            && concoction.getAdventuresNeeded(missingCount, true) > 0) {
          if (!InventoryManager.allowTurnConsumption(creator)) {
            return null;
          }
          asked = true;
        }

        RequestThread.postRequest(creator);

        if (ItemDatabase.isVirtualItem(itemId) && ItemDatabase.haveVirtualItem(itemId)) {
          return "";
        }

        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }

        if (!KoLmafia.permitsContinue()) {
          return null;
        }
      }
    }

    // An 11-leaf clover can be purchased from the Hermit (if he has any in
    // stock.

    if (shouldUseCoinmasters
        && KoLConstants.hermitItems.contains(item)
        && (!shouldUseMall
            || SewerRequest.currentWorthlessItemCost() < MallPriceManager.getMallPrice(itemId))) {

      int itemCount =
          itemId == ItemPool.ELEVEN_LEAF_CLOVER
              ? HermitRequest.cloverCount()
              : PurchaseRequest.MAX_QUANTITY;

      if (itemCount > 0) {
        if (sim) {
          return "hermit";
        }

        int retrieveCount = Math.min(itemCount, missingCount);
        RequestThread.postRequest(new HermitRequest(itemId, retrieveCount));
      }

      missingCount = item.getCount() - item.getCount(KoLConstants.inventory);
      if (missingCount <= 0) {
        return "";
      }
    }

    // Try to purchase the item from the mall, if the user wishes
    // to autosatisfy through purchases, and we have none of the
    // ingredients needed to create the item

    if (shouldUseMall && !scriptSaysBuy && !InventoryManager.hasAnyIngredient(itemId)) {
      // On the dev server it's always useful to check the buyScript because items can be magicked
      // into existence.
      if (creator == null && !KoLmafia.usingDevServer()) {
        if (sim) {
          return "buy";
        }
        scriptSaysBuy = true;
      } else {
        AdventureResult instance = item.getInstance(missingCount);
        boolean defaultBuy = InventoryManager.cheaperToBuy(instance);
        if (sim && haveBuyScript) {
          return defaultBuy ? "create or buy" : "create";
        }
        scriptSaysBuy = InventoryManager.invokeBuyScript(instance, 0, defaultBuy);
        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }
      }
    }

    if (shouldUseNPCStore || scriptSaysBuy) {
      if (sim) {
        return shouldUseNPCStore ? "buy from NPC" : "buy";
      }

      // If buying from the mall will leave the item in storage, use only NPCs
      AdventureResult instance = item.getInstance(missingCount);
      boolean onlyNPC = forceNoMall || !InventoryManager.canUseMall();

      // If mall purchases are allowed, maybe take items from the mall shop first
      if (shouldUseShop && !onlyNPC) {
        ManageStoreRequest request = new ManageStoreRequest(itemId, missingCount);
        RequestThread.postRequest(request);

        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }
      }

      List<PurchaseRequest> results =
          onlyNPC ? MallPriceManager.searchNPCs(item) : MallPriceManager.searchMall(instance);
      KoLmafia.makePurchases(
          results,
          results.toArray(new PurchaseRequest[0]),
          InventoryManager.getPurchaseCount(itemId, missingCount),
          isAutomated,
          0);
      if (!onlyNPC) {
        MallPriceManager.updateMallPrice(instance, results);
      }

      missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

      if (missingCount <= 0) {
        return "";
      }
    }

    // Use budgeted pulls if the item is available from storage.

    if (!isRestricted && !KoLCharacter.canInteract() && !KoLCharacter.isHardcore()) {
      int pullCount =
          Math.min(item.getCount(KoLConstants.storage), ConcoctionDatabase.getPullsBudgeted());

      if (pullCount > 0) {
        if (sim) {
          return "pull";
        }

        pullCount = Math.min(pullCount, item.getCount());
        int newbudget = ConcoctionDatabase.getPullsBudgeted() - pullCount;

        RequestThread.postRequest(
            new StorageRequest(
                StorageRequestType.STORAGE_TO_INVENTORY, item.getInstance(pullCount)));
        ConcoctionDatabase.setPullsBudgeted(newbudget);
        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }
      }
    }

    if (creator != null && mixingMethod != CraftingType.NOCREATE) {
      scriptSaysBuy =
          switch (itemId) {
            case ItemPool.DOUGH, ItemPool.DISASSEMBLED_CLOVER, ItemPool.JOLLY_BRACELET -> true;
            default -> false;
          };

      AdventureResult instance = item.getInstance(missingCount);
      boolean defaultBuy =
          scriptSaysBuy || shouldUseMall && InventoryManager.cheaperToBuy(instance);
      if (sim && haveBuyScript) {
        return defaultBuy ? "create or buy" : "create";
      }
      scriptSaysBuy = InventoryManager.invokeBuyScript(instance, 1, defaultBuy);
      missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

      if (missingCount <= 0) {
        return "";
      }
    }

    // If it's creatable, and you have at least one ingredient, see
    // if you can make it via recursion.

    if (creator != null && mixingMethod != CraftingType.NOCREATE && !scriptSaysBuy) {
      boolean makeFromComponents = true;
      if (isAutomated) {
        // Speculate on how much the items needed to make the creation would cost.
        // Do not retrieve if the average meat spend to make one of the item
        // exceeds the user's autoBuyPriceLimit.

        AdventureResult instance = item.getInstance(missingCount);
        float meatSpend = InventoryManager.priceToMake(instance, true, true) / missingCount;
        int autoBuyPriceLimit = Preferences.getInteger("autoBuyPriceLimit");
        if (meatSpend > autoBuyPriceLimit) {
          // Print an informative message. It need not be an error, since we
          // will fail with another error almost immediately.
          //
          // It also need not be displayed by the maximizer when considering how to obtain an item.
          if (!sim) {
            KoLmafia.updateDisplay(
                "The average amount of meat spent on components ("
                    + KoLConstants.COMMA_FORMAT.format(meatSpend)
                    + ") for one "
                    + item.getName()
                    + " exceeds autoBuyPriceLimit ("
                    + KoLConstants.COMMA_FORMAT.format(autoBuyPriceLimit)
                    + ")");
          }

          // Too expensive to make
          makeFromComponents = false;

          // If making it from components was cheaper than buying the final product, and we
          // couldn't afford to make it, don't bother trying to buy the final product.
          shouldUseMall = false;
        }
      }

      // bundle of firewood = stick of firewood (10) - COINMASTER
      // stick of firewood (10) = bundle of firewood - SUSE
      //
      // This is the only concoction that creates a coinmaster token
      //
      // It is reasonable to use bundle of firewood to get sticks of
      // firewood to spend in the Coinmaster to make items - unless
      // you are making bundles of firewood.
      if (item.getItemId() == ItemPool.BUNDLE_OF_FIREWOOD && creator.getQuantityPossible() == 0) {
        makeFromComponents = false;
      }

      if (makeFromComponents) {
        if (sim) {
          return "create";
        }

        // Second place to check for adventure usage.  Make sure we didn't already ask above.
        creator.setQuantityNeeded(missingCount);

        if (!asked
            && isAutomated
            && concoction != null
            && concoction.getAdventuresNeeded(missingCount, true) > 0) {
          if (!InventoryManager.allowTurnConsumption(creator)) {
            return null;
          }
          asked = true;
        }

        RequestThread.postRequest(creator);
        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }

        if (!KoLmafia.permitsContinue() && isAutomated) {
          return null;
        }
      }
    }

    // All other options have been exhausted. Buy the remaining
    // items from the mall.

    if (shouldUseMall) {
      if (sim) {
        return "buy";
      }

      // If mall purchases are allowed, maybe take items from the mall shop first
      if (shouldUseShop) {
        ManageStoreRequest request = new ManageStoreRequest(itemId, missingCount);
        RequestThread.postRequest(request);

        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }
      }

      AdventureResult instance = item.getInstance(missingCount);
      List<PurchaseRequest> results = MallPriceManager.searchMall(instance);
      KoLmafia.makePurchases(
          results,
          results.toArray(new PurchaseRequest[0]),
          InventoryManager.getPurchaseCount(itemId, missingCount),
          isAutomated,
          0);
      MallPriceManager.updateMallPrice(instance, results);
      missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

      if (missingCount <= 0) {
        return "";
      }
    }

    // We were unable to obtain as many of the item as the user desired.
    // Fail now.

    if (sim) {
      return "fail";
    }

    KoLmafia.updateDisplay(
        MafiaState.ERROR, "You need " + missingCount + " more " + item.getName() + " to continue.");

    return null;
  }

  private static boolean invokeBuyScript(
      final AdventureResult item, final int ingredientLevel, final boolean defaultBuy) {
    String scriptName = Preferences.getString("buyScript").trim();
    if (scriptName.length() == 0) {
      return defaultBuy;
    }

    List<File> scriptFiles = KoLmafiaCLI.findScriptFile(scriptName);
    ScriptRuntime interpreter = KoLmafiaASH.getInterpreter(scriptFiles);
    if (interpreter != null) {
      File scriptFile = scriptFiles.get(0);
      KoLmafiaASH.logScriptExecution("Starting buy script: ", scriptFile.getName(), interpreter);
      Value v =
          interpreter.execute(
              "main",
              new String[] {
                item.getName(),
                String.valueOf(item.getCount()),
                String.valueOf(ingredientLevel),
                String.valueOf(defaultBuy)
              });
      KoLmafiaASH.logScriptExecution("Finished buy script: ", scriptFile.getName(), interpreter);
      return v != null && v.intValue() != 0;
    }
    return defaultBuy;
  }

  public static boolean cheaperToBuy(final AdventureResult item) {
    if (!ItemDatabase.isTradeable(item.getItemId())) {
      return false;
    }

    long mallPrice = MallPriceManager.getMallPrice(item, MALL_PRICE_AGE);
    if (mallPrice <= 0) {
      return false;
    }

    long makePrice = InventoryManager.priceToMake(item, false);
    if (makePrice == Long.MAX_VALUE) {
      return true;
    }

    if (mallPrice / 2 < makePrice && makePrice / 2 < mallPrice) {
      // Less than a 2:1 ratio, we should check more carefully
      mallPrice = MallPriceManager.getMallPrice(item);
      if (mallPrice <= 0) {
        return false;
      }

      makePrice = InventoryManager.priceToMake(item, true);
      if (makePrice == Long.MAX_VALUE) {
        return true;
      }
    }

    if (Preferences.getBoolean("debugBuy")) {
      RequestLogger.printLine(
          "\u262F " + item + " mall=" + priceString(mallPrice) + " make=" + priceString(makePrice));
    }

    return mallPrice < makePrice;
  }

  public static long itemValue(final AdventureResult item, final boolean exact) {

    // r9806 | jasonharper | 2011-09-05 00:04:24 -0400 (Mon, 05 Sep 2011) | 29 lines
    //
    // The decision to buy a completed item rather than creating it from ingredients
    // already in inventory requires assigning a value to those ingredients, which
    // really depends on play style.  Not everyone is going to put in the effort
    // needed to maximize their Mall profits; they might use only autosell to
    // dispose of excess items, or just hoard them.  Therefore, a new float
    // preference "valueOfInventory" allows players to indicate the worth of items,
    // with these key values:
    //
    // 0.0 - Items already in inventory are considered free.
    // 1.0 - Items are valued at their autosell price.
    // 2.0 - Items are valued at current Mall price, unless they are min-priced.
    // 3.0 - Items are always valued at Mall price (not really realistic).
    //
    // Intermediate values interpolate between integral values.  The default is 1.8,
    // reflecting the fact that items won't sell immediately in the Mall without
    // undercutting or advertising.  This preference, and several previously hidden
    // prefs affecting create vs. buy decisions, are now exposed on a new Creatable
    // -> Fine Tuning page in the Item Manager.

    // 0.0 - Items already owned are considered free.
    // 1.0 - Items are valued at autosell price.
    // 2.0 - Items are valued at autosell price if min-priced in Mall.
    // 2.0 - Items are valued at current Mall price, if not min-priced.
    // 3.0 - Items are always valued at Mall price (not really realistic).

    float factor = Preferences.getFloat("valueOfInventory");
    if (factor <= 0.0f) {
      return 0;
    }

    long lower = 0;
    int autosell = ItemDatabase.getPriceById(item.getItemId());
    long upper = Math.max(0, autosell);

    if (factor <= 1.0f) {
      return lower + (int) ((upper - lower) * factor);
    }

    factor -= 1.0f;
    lower = upper;

    long mall =
        exact
            ? MallPriceManager.getMallPrice(item)
            : MallPriceManager.getMallPrice(item, MALL_PRICE_AGE);
    if (mall > Math.max(100, 2 * Math.abs(autosell))) {
      upper = Math.max(lower, mall);
    }

    if (factor <= 1.0f) {
      return lower + (int) ((upper - lower) * factor);
    }

    factor -= 1.0f;
    upper = Math.max(lower, mall);
    return lower + (int) ((upper - lower) * factor);
  }

  public static final long priceToAcquire(final AdventureResult item, final boolean exact) {
    return InventoryManager.priceToAcquire(item, exact, false, 0);
  }

  public static final long priceToAcquire(
      final AdventureResult item, final boolean exact, final boolean mallPriceOnly) {
    return InventoryManager.priceToAcquire(item, exact, mallPriceOnly, 0);
  }

  private static final long priceToAcquire(
      final AdventureResult item,
      final boolean exact,
      final boolean mallPriceOnly,
      final int level) {

    int itemId = item.getItemId();
    int needed = item.getCount();

    // Not just inventory; include anything our setting allow to be retrieved
    int onhand = Math.min(needed, InventoryManager.getAccessibleCount(item));
    long price = 0;

    if (onhand > 0) {

      // r8873 | jasonharper | 2011-01-04 00:07:09 -0500 (Tue, 04 Jan 2011) | 9 lines
      //
      // Added a special case to the create vs. buy decision-making code: an on-hand
      // tiny plastic sword is valued at 0 meat, since you get it back from the drink.
      // This should avoid undesirable behavior if the historical price of the TPS is
      // ever greater than one of the items containing a TPS.

      if (itemId != ItemPool.PLASTIC_SWORD) {
        AdventureResult instance = item.getInstance(onhand);
        price = mallPriceOnly ? 0 : InventoryManager.itemValue(instance, exact);
      }

      needed -= onhand;

      if (needed == 0) {
        if (Preferences.getBoolean("debugBuy")) {
          RequestLogger.printLine(
              "\u262F "
                  + item.getInstance(onhand)
                  + " onhand="
                  + onhand
                  + " price = "
                  + priceString(price));
        }

        return price;
      }
    }

    AdventureResult instance = item.getInstance(needed);
    long mallPrice =
        (exact
            ? MallPriceManager.getMallPrice(instance)
            : MallPriceManager.getMallPrice(instance, MALL_PRICE_AGE));
    if (mallPrice <= 0) {
      mallPrice = Long.MAX_VALUE;
    } else {
      mallPrice += price;
    }

    long makePrice = InventoryManager.priceToMake(instance, exact, mallPriceOnly, level);
    if (makePrice != Long.MAX_VALUE) {
      makePrice += price;
    }

    if (!exact && mallPrice / 2 < makePrice && makePrice / 2 < mallPrice) {
      // Less than a 2:1 ratio, we should check more carefully
      return InventoryManager.priceToAcquire(item, true, mallPriceOnly, level);
    }

    if (Preferences.getBoolean("debugBuy")) {
      RequestLogger.printLine(
          "\u262F " + item + " mall=" + priceString(mallPrice) + " make=" + priceString(makePrice));
    }

    return Math.min(mallPrice, makePrice);
  }

  private static String priceString(long price) {
    return price == Long.MAX_VALUE ? "\u221E" : String.valueOf(price);
  }

  public static long priceToMake(final AdventureResult item, final boolean exact) {
    return InventoryManager.priceToMake(item, exact, false, 0);
  }

  public static long priceToMake(
      final AdventureResult item, final boolean exact, final boolean mallPriceOnly) {
    return InventoryManager.priceToMake(item, exact, mallPriceOnly, 0);
  }

  private static long priceToMake(
      final AdventureResult item,
      final boolean exact,
      final boolean mallPriceOnly,
      final int level) {
    int itemId = item.getItemId();
    int quantity = item.getCount();
    int meatCost = CombineMeatRequest.getCost(itemId);
    if (meatCost > 0) {
      return meatCost * quantity;
    }

    // Limit recursion depth
    if (level > 10) {
      return Long.MAX_VALUE;
    }

    if (!ConcoctionDatabase.isPermittedMethod(item)) {
      return Long.MAX_VALUE;
    }

    CraftingType method = ConcoctionDatabase.getMixingMethod(item);
    long price = ConcoctionDatabase.getCreationCost(method);
    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(itemId);

    if (ingredients.length == 0) {
      // This is a concoction with no ingredients, so if creatable == 0,
      // we can be sure that we cannot concoct it right now.
      var conc = ConcoctionPool.get(itemId);
      if (conc == null || conc.creatable == 0) {
        return Long.MAX_VALUE;
      }
      if (price == 0) {
        return 0;
      }
    }

    long yield = ConcoctionDatabase.getYield(itemId);
    long madeQuantity = (quantity + yield - 1) / yield;
    price *= madeQuantity;

    for (AdventureResult ingredient : ingredients) {
      long needed = ingredient.getCount() * madeQuantity;

      long ingredientPrice =
          ingredient.isMeat()
              ? needed
              : InventoryManager.priceToAcquire(
                  ingredient.getInstance(needed), exact, mallPriceOnly, level + 1);

      if (ingredientPrice == Long.MAX_VALUE) {
        return ingredientPrice;
      }

      price += ingredientPrice;
    }

    return price * quantity / (yield * madeQuantity);
  }

  private static int getPurchaseCount(final int itemId, final int missingCount) {
    if (missingCount >= InventoryManager.BULK_PURCHASE_AMOUNT
        || !KoLCharacter.canInteract()
        || KoLCharacter.getAvailableMeat() < 5000) {
      return missingCount;
    }

    if (InventoryManager.shouldBulkPurchase(itemId)) {
      return InventoryManager.BULK_PURCHASE_AMOUNT;
    }

    return missingCount;
  }

  private static boolean hasAnyIngredient(final int itemId) {
    return InventoryManager.hasAnyIngredient(itemId, null);
  }

  private static boolean hasAnyIngredient(final int itemId, Set<Integer> seen) {
    if (itemId < 0) {
      return false;
    }

    switch (itemId) {
      case ItemPool.MEAT_PASTE:
        return KoLCharacter.getAvailableMeat() >= 10;
      case ItemPool.MEAT_STACK:
        return KoLCharacter.getAvailableMeat() >= 100;
      case ItemPool.DENSE_STACK:
        return KoLCharacter.getAvailableMeat() >= 1000;
    }

    AdventureResult[] ingredients = ConcoctionDatabase.getStandardIngredients(itemId);
    boolean shouldUseCloset = InventoryManager.canUseCloset();

    for (int i = 0; i < ingredients.length; ++i) {
      AdventureResult ingredient = ingredients[i];
      // An item is immediately available if it is in your
      // inventory, or in your closet.

      if (KoLConstants.inventory.contains(ingredient)) {
        return true;
      }

      if (shouldUseCloset && KoLConstants.closet.contains(ingredient)) {
        return true;
      }
    }

    Integer key = itemId;

    if (seen == null) {
      seen = new HashSet<>();
    } else if (seen.contains(key)) {
      return false;
    }

    seen.add(key);

    for (int i = 0; i < ingredients.length; ++i) {
      // An item is immediately available if you have the
      // ingredients for a substep.

      if (InventoryManager.hasAnyIngredient(ingredients[i].getItemId(), seen)) {
        return true;
      }
    }

    return false;
  }

  private static boolean shouldBulkPurchase(final int itemId) {
    // We always bulk purchase certain specific items.

    switch (itemId) {
      case ItemPool.REMEDY: // soft green echo eyedrop antidote
      case ItemPool.TINY_HOUSE:
      case ItemPool.DRASTIC_HEALING:
      case ItemPool.ANTIDOTE:
        return true;
    }

    if (!KoLmafia.isAdventuring()) {
      return false;
    }

    // We bulk purchase consumable items if we are
    // auto-adventuring.

    if (RestoresDatabase.isRestore(itemId)) {
      return true;
    }

    return false;
  }

  public static boolean itemAvailable(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    return InventoryManager.itemAvailable(item.getItemId());
  }

  public static boolean itemAvailable(final int itemId) {
    return InventoryManager.hasItem(itemId)
        || InventoryManager.canUseStorage(itemId)
        || InventoryManager.canUseMall(itemId)
        || InventoryManager.canUseNPCStores(itemId)
        || InventoryManager.canUseCoinmasters(itemId)
        || InventoryManager.canUseClanStash(itemId)
        || InventoryManager.canUseCloset(itemId);
  }

  public static boolean canUseMall(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    return InventoryManager.canUseMall(item.getItemId());
  }

  public static boolean canUseMall(final int itemId) {
    return ItemDatabase.isTradeable(itemId) && InventoryManager.canUseMall();
  }

  public static boolean canUseMall() {
    return KoLCharacter.canInteract()
        && Preferences.getBoolean("autoSatisfyWithMall")
        && !KoLCharacter.getLimitMode().limitMall();
  }

  public static boolean canUseMallToStorage(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    return InventoryManager.canUseMallToStorage(item.getItemId());
  }

  public static boolean canUseMallToStorage(final int itemId) {
    return ItemDatabase.isTradeable(itemId) && InventoryManager.canUseMallToStorage();
  }

  public static boolean canUseMallToStorage() {
    return Preferences.getBoolean("autoSatisfyWithMall")
        && !KoLCharacter.getLimitMode().limitMall();
  }

  public static boolean canUseNPCStores(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    return InventoryManager.canUseNPCStores(item.getItemId());
  }

  public static boolean canUseNPCStores(final int itemId) {
    return InventoryManager.canUseNPCStores() && NPCStoreDatabase.contains(itemId);
  }

  public static boolean canUseNPCStores() {
    return Preferences.getBoolean("autoSatisfyWithNPCs")
        && !KoLCharacter.getLimitMode().limitNPCStores();
  }

  public static boolean canUseCoinmasters(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    return InventoryManager.canUseCoinmasters(item.getItemId());
  }

  public static boolean canUseCoinmasters(final int itemId) {
    if (itemId == ItemPool.ELEVEN_LEAF_CLOVER && HermitRequest.cloverCount() < 1) {
      return false;
    }
    return InventoryManager.canUseCoinmasters() && CoinmastersDatabase.contains(itemId);
  }

  public static boolean canUseCoinmasters() {
    return Preferences.getBoolean("autoSatisfyWithCoinmasters")
        && !KoLCharacter.getLimitMode().limitCoinmasters();
  }

  public static boolean canUseClanStash(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    boolean canUseStash = InventoryManager.canUseClanStash();
    return canUseStash && item.getCount(ClanManager.getStash()) > 0;
  }

  public static boolean canUseClanStash(final int itemId) {
    AdventureResult item = ItemPool.get(itemId, 1);
    return InventoryManager.canUseClanStash(item);
  }

  public static boolean canUseClanStash() {
    return KoLCharacter.canInteract()
        && Preferences.getBoolean("autoSatisfyWithStash")
        && KoLCharacter.hasClan()
        && !KoLCharacter.getLimitMode().limitClan();
  }

  public static boolean canUseCloset(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    return InventoryManager.canUseCloset() && item.getCount(KoLConstants.closet) > 0;
  }

  public static boolean canUseCloset(final int itemId) {
    AdventureResult item = ItemPool.get(itemId, 1);
    return InventoryManager.canUseCloset(item);
  }

  public static boolean canUseCloset() {
    return Preferences.getBoolean("autoSatisfyWithCloset")
        && !KoLCharacter.getLimitMode().limitCampground();
  }

  public static boolean canUseStorage(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    return InventoryManager.canUseStorage() && item.getCount(KoLConstants.storage) > 0;
  }

  public static boolean canUseStorage(final int itemId) {
    AdventureResult item = ItemPool.get(itemId, 1);
    return InventoryManager.canUseStorage(item);
  }

  public static boolean canUseStorage() {
    return KoLCharacter.canInteract()
        && Preferences.getBoolean("autoSatisfyWithStorage")
        && !KoLCharacter.getLimitMode().limitStorage();
  }

  public static boolean canUseShop(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    return InventoryManager.canUseShop(item.getItemId());
  }

  public static boolean canUseShop(final int itemId) {
    return InventoryManager.canUseShop()
        && ItemDatabase.isTradeable(itemId)
        && StoreManager.shopAmount(itemId) > 0;
  }

  public static boolean canUseShop() {
    return InventoryManager.canUseMall() && Preferences.getBoolean("autoSatisfyWithShop");
  }

  public static final void fireInventoryChanged(final int itemId) {
    ItemListenerRegistry.fireItemChanged(itemId);
  }

  public static final AdventureResult CROWN_OF_THRONES = ItemPool.get(ItemPool.HATSEAT, 1);

  public static final void checkItemDescription(final int itemId) {
    String descId = ItemDatabase.getDescriptionId(itemId);
    GenericRequest req = new GenericRequest("desc_item.php?whichitem=" + descId);
    RequestThread.postRequest(req);
  }

  public static final void checkEffectDescription(final int effectId) {
    String descId = EffectDatabase.getDescriptionId(effectId);
    GenericRequest req = new GenericRequest("desc_effect.php?whicheffect=" + descId);
    RequestThread.postRequest(req);
  }

  public static final void checkCrownOfThrones() {
    // If we are wearing the Crown of Thrones, we've already seen
    // which familiar is riding in it
    if (KoLCharacter.hasEquipped(InventoryManager.CROWN_OF_THRONES, Slot.HAT)) {
      return;
    }

    // The Crown of Thrones is not trendy, but double check anyway
    AdventureResult item = InventoryManager.CROWN_OF_THRONES;
    if (!ItemDatabase.isAllowed(item)) {
      return;
    }

    // See if we have a Crown of Thrones in inventory or closet
    int count = item.getCount(KoLConstants.inventory) + item.getCount(KoLConstants.closet);
    if (count == 0) {
      return;
    }

    // See which familiar is riding in it.
    checkItemDescription(ItemPool.HATSEAT);
  }

  public static final AdventureResult BUDDY_BJORN = ItemPool.get(ItemPool.BUDDY_BJORN, 1);

  public static final void checkBuddyBjorn() {
    // If we are wearing the Bjorn Buddy, we've already seen
    // which familiar is riding in it
    if (KoLCharacter.hasEquipped(InventoryManager.BUDDY_BJORN, Slot.CONTAINER)) {
      return;
    }

    // Check if the Buddy Bjorn is Trendy
    AdventureResult item = InventoryManager.BUDDY_BJORN;
    if (!ItemDatabase.isAllowed(item)) {
      return;
    }

    // See if we have a Buddy Bjorn in inventory or closet
    int count = item.getCount(KoLConstants.inventory) + item.getCount(KoLConstants.closet);
    if (count == 0) {
      return;
    }

    // See which familiar is riding in it.
    checkItemDescription(ItemPool.BUDDY_BJORN);
  }

  public static void checkMods() {
    checkNoHat();
    checkJickSword();
    checkPantogram();
    checkLatte();
    checkSaber();
    checkCoatOfPaint(false);
    checkUmbrella();
    checkBuzzedOnDistillate();
    checkVampireVintnerWine();
    checkCrimboTrainingManual();
    checkRing();
    checkFuturistic();
    checkZootomistMods();
  }

  public static void checkNoHat() {
    checkItem(ItemPool.NO_HAT, "_noHatModifier");
  }

  public static void checkJickSword() {
    AdventureResult JICK_SWORD = ItemPool.get(ItemPool.JICK_SWORD, 1);
    String mod = Preferences.getString("jickSwordModifier");
    if (!mod.equals("")) {
      ModifierDatabase.overrideModifier(ModifierType.ITEM, ItemPool.JICK_SWORD, mod);
      return;
    }
    if (!KoLCharacter.hasEquipped(JICK_SWORD, Slot.WEAPON)
        && !KoLConstants.inventory.contains(JICK_SWORD)) {
      // There are other places it could be, but it only needs to be
      // checked once ever, and if the sword isn't being used then
      // it can be checked later
      return;
    }

    checkItemDescription(ItemPool.JICK_SWORD);
  }

  public static void checkPantogram() {
    checkItem(ItemPool.PANTOGRAM_PANTS, "_pantogramModifier");
  }

  public static void checkLatte() {
    checkItem(ItemPool.LATTE_MUG, "latteModifier");
  }

  public static void checkSaber() {
    AdventureResult SABER = ItemPool.get(ItemPool.FOURTH_SABER, 1);
    if (!InventoryManager.equippedOrInInventory(SABER)
        && SABER.getCount(KoLConstants.closet) == 0
        && !(KoLCharacter.inLegacyOfLoathing()
            && InventoryManager.equippedOrInInventory(ItemPool.REPLICA_FOURTH_SABER))) {
      return;
    }
    if (!Preferences.getString("_saberMod").equals("0")) {
      return;
    }

    checkItemDescription(ItemPool.FOURTH_SABER);
  }

  public static void checkUmbrella() {
    AdventureResult UMBRELLA = ItemPool.get(ItemPool.UNBREAKABLE_UMBRELLA, 1);
    if (!KoLCharacter.hasEquipped(UMBRELLA)
        && UMBRELLA.getCount(KoLConstants.inventory) == 0
        && UMBRELLA.getCount(KoLConstants.closet) == 0) {
      return;
    }

    checkItemDescription(ItemPool.UNBREAKABLE_UMBRELLA);
  }

  public static void checkKGB() {
    AdventureResult KGB = ItemPool.get(ItemPool.KREMLIN_BRIEFCASE, 1);
    // See if we have a Kremlin's Greatest Briefcase
    // One sitting in storage, mall store, or display case probably hasn't had enchantments changed,
    // maybe
    if (!KoLCharacter.hasEquipped(KGB)
        && KGB.getCount(KoLConstants.inventory) == 0
        && KGB.getCount(KoLConstants.closet) == 0) {
      return;
    }

    checkItemDescription(ItemPool.KREMLIN_BRIEFCASE);
  }

  public static void checkVampireVintnerWine() {
    // 1950 Vampire Vintner Wine is a quest item. You can have at most
    // one in inventory - and nowhere else.
    if (InventoryManager.getCount(ItemPool.VAMPIRE_VINTNER_WINE) == 0) {
      return;
    }

    // ResultProcessor will parse the item description and set properties
    checkItemDescription(ItemPool.VAMPIRE_VINTNER_WINE);
  }

  public static void checkCoatOfPaint(boolean playerClassChanged) {
    AdventureResult COAT_OF_PAINT = ItemPool.get(ItemPool.COAT_OF_PAINT, 1);

    if (InventoryManager.getAccessibleCount(COAT_OF_PAINT) == 0) {
      return;
    }

    String mod = Preferences.getString("_coatOfPaintModifier");

    if (!playerClassChanged && !mod.equals("")) {
      ModifierDatabase.overrideModifier(ModifierType.ITEM, ItemPool.COAT_OF_PAINT, mod);
      return;
    }

    checkItemDescription(ItemPool.COAT_OF_PAINT);
  }

  public static void checkBuzzedOnDistillate() {
    var BUZZED = EffectPool.get(EffectPool.BUZZED_ON_DISTILLATE);
    String mod = Preferences.getString("currentDistillateMods");
    if (!KoLConstants.activeEffects.contains(BUZZED)) {
      return;
    }
    if (!mod.equals("")) {
      ModifierDatabase.overrideModifier(ModifierType.EFFECT, EffectPool.BUZZED_ON_DISTILLATE, mod);
      return;
    }

    DebugDatabase.readEffectDescriptionText(EffectPool.BUZZED_ON_DISTILLATE);
  }

  public static void checkCrimboTrainingManual() {
    AdventureResult CRIMBO_TRAINING_MANUAL = ItemPool.get(ItemPool.CRIMBO_TRAINING_MANUAL, 1);
    int skill = Preferences.getInteger("crimboTrainingSkill");
    if (skill >= 1 && skill <= 11) {
      // We have already recorded which skill we can train
      return;
    }

    if (InventoryManager.getAccessibleCount(CRIMBO_TRAINING_MANUAL, false) == 0) {
      // We don't have a Crimbo training manual
      return;
    }

    checkItemDescription(ItemPool.CRIMBO_TRAINING_MANUAL);
  }

  public static Pattern BIRD_PATTERN = Pattern.compile("Seek out an? (.*)");

  public static void checkBirdOfTheDay() {
    AdventureResult BOTD = ItemPool.get(ItemPool.BIRD_A_DAY_CALENDAR, 1);
    if (BOTD.getCount(KoLConstants.inventory) == 0 && BOTD.getCount(KoLConstants.closet) == 0) {
      return;
    }

    String text = DebugDatabase.readSkillDescriptionText(SkillPool.SEEK_OUT_A_BIRD);
    String skillName = DebugDatabase.parseName(text);
    Matcher birdMatcher = InventoryManager.BIRD_PATTERN.matcher(skillName);
    if (birdMatcher.find()) {
      // We have unlocked this skill today.
      String bird = birdMatcher.group(1);
      Preferences.setString("_birdOfTheDay", bird);
      if (!Preferences.getBoolean("_canSeekBirds")) {
        Preferences.setBoolean("_canSeekBirds", true);
        ResponseTextParser.learnSkill("Seek out a Bird");
      }
      // Calculate how many times we used it.
      long mp = DebugDatabase.parseSkillMPCost(text);
      int casts = (int) (Math.log(mp / 5) / Math.log(2));
      Preferences.setInteger("_birdsSoughtToday", casts);
    } else {
      // We have not unlocked this skill today.
      // Leave _birdOfTheDay intact; active turns of
      // Blessing of the Bid will still refer to it.
    }

    DebugDatabase.readEffectDescriptionText(EffectPool.BLESSING_OF_YOUR_FAVORITE_BIRD);
    DebugDatabase.readEffectDescriptionText(EffectPool.BLESSING_OF_THE_BIRD);
  }

  public static void checkFuturistic() {
    checkItem(ItemPool.FUTURISTIC_HAT, "_futuristicHatModifier");
    checkItem(ItemPool.FUTURISTIC_SHIRT, "_futuristicShirtModifier");
    checkItem(ItemPool.FUTURISTIC_COLLAR, "_futuristicCollarModifier");
  }

  public static void checkZootomistMods() {
    if (!KoLCharacter.inZootomist()) {
      // don't bother checking
      return;
    }
    checkEffectDescription(EffectPool.GRAFTED);
    checkEffectDescription(EffectPool.MILK_OF_FAMILIAR_KINDNESS);
    checkEffectDescription(EffectPool.MILK_OF_FAMILIAR_CRUELTY);
  }

  private static void checkItem(int id, String preference) {
    AdventureResult ITEM = ItemPool.get(id, 1);
    String mod = Preferences.getString(preference);
    if (!InventoryManager.equippedOrInInventory(ITEM)) {
      return;
    }
    if (!mod.isEmpty()) {
      ModifierDatabase.overrideModifier(ModifierType.ITEM, id, mod);
      return;
    }

    checkItemDescription(id);
  }

  public static void checkDartPerks() {
    if (!InventoryManager.equippedOrInInventory(ItemPool.get(ItemPool.EVERFULL_DART_HOLSTER, 1))) {
      return;
    }

    checkItemDescription(ItemPool.EVERFULL_DART_HOLSTER);
  }

  public static void checkMimicEgg() {
    if (!InventoryManager.equippedOrInInventory(ItemPool.get(ItemPool.MIMIC_EGG, 1))) {
      return;
    }

    checkItemDescription(ItemPool.MIMIC_EGG);
  }

  private static final AdventureResult GOLDEN_MR_ACCESSORY =
      ItemPool.get(ItemPool.GOLDEN_MR_ACCESSORY, 1);

  public static void countGoldenMrAccesories() {
    int oldCount = Preferences.getInteger("goldenMrAccessories");
    int newCount =
        InventoryManager.GOLDEN_MR_ACCESSORY.getCount(KoLConstants.inventory)
            + InventoryManager.GOLDEN_MR_ACCESSORY.getCount(KoLConstants.closet)
            + InventoryManager.GOLDEN_MR_ACCESSORY.getCount(KoLConstants.storage)
            + InventoryManager.GOLDEN_MR_ACCESSORY.getCount(KoLConstants.collection)
            + InventoryManager.getEquippedCount(InventoryManager.GOLDEN_MR_ACCESSORY);

    // A Golden Mr. Accessory cannot be traded or discarded. Once
    // you purchase one, it's yours forever more.

    if (newCount > oldCount) {
      if (oldCount == 0) {
        ResponseTextParser.learnSkill("The Smile of Mr. A.");
      }
      Preferences.setInteger("goldenMrAccessories", newCount);
    }
  }

  public static void checkSkillGrantingEquipment() {
    checkSkillGrantingEquipment(null);
  }

  public static void checkSkillGrantingEquipment(final Integer itemId) {
    ModifierDatabase.getInventorySkillProviders().stream()
        .filter(l -> l.getType() == ModifierType.ITEM)
        .map(Lookup::getIntKey)
        .filter(i -> itemId == null || i.equals(itemId))
        .filter(id -> KoLCharacter.hasEquipped(id) || InventoryManager.hasItem(id))
        .flatMap(
            id -> {
              var mods = ModifierDatabase.getItemModifiers(id);
              if (mods == null) return Stream.empty();
              return Stream.concat(
                  mods.getStrings(MultiStringModifier.CONDITIONAL_SKILL_INVENTORY).stream()
                      .map(s -> Map.entry(true, s)),
                  mods.getStrings(MultiStringModifier.CONDITIONAL_SKILL_EQUIPPED).stream()
                      .map(s -> Map.entry(false, s)));
            })
        .map(e -> Map.entry(e.getKey(), SkillDatabase.getSkillId(e.getValue())))
        .filter(
            e ->
                e.getKey()
                    || SkillDatabase.getSkillTags(e.getValue())
                        .contains(SkillDatabase.SkillTag.NONCOMBAT))
        .map(Map.Entry::getValue)
        .forEach(KoLCharacter::addAvailableSkill);
  }

  public static void checkRing() {
    if (InventoryManager.itemAvailable(ItemPool.RING)) {
      checkItemDescription(ItemPool.RING);
    }
  }

  private static boolean allowTurnConsumption(final CreateItemRequest creator) {
    if (!GenericFrame.instanceExists()) {
      return true;
    }

    if (!InventoryManager.askAboutCrafting(creator)) {
      return false;
    }

    return true;
  }

  public static boolean askAboutCrafting(final CreateItemRequest creator) {
    if (creator.getQuantityNeeded() < 1) {
      return true;
    }
    // Allow the user to permanently squash this prompt.
    if (Preferences.getInteger("promptAboutCrafting") < 1) {
      return true;
    }
    // If we've already nagged, don't nag. Unless the user wants us to nag. Then, nag.
    if (InventoryManager.askedAboutCrafting == KoLCharacter.getUserId()
        && Preferences.getInteger("promptAboutCrafting") < 2) {
      return true;
    }

    // See if we have enough free crafting turns available
    int freeCrafts = ConcoctionDatabase.getFreeCraftingTurns();
    int count = creator.getQuantityNeeded();
    int needed = creator.concoction.getAdventuresNeeded(count);

    CraftingType mixingMethod = creator.concoction.getMixingMethod();

    switch (mixingMethod) {
      case SMITH, SSMITH -> freeCrafts += ConcoctionDatabase.getFreeSmithingTurns();
      case COOK_FANCY -> freeCrafts += ConcoctionDatabase.getFreeCookingTurns();
      case MIX_FANCY -> freeCrafts += ConcoctionDatabase.getFreeCocktailcraftingTurns();
    }

    if (needed <= freeCrafts) {
      return true;
    }

    // We could cast Inigo's automatically here, but nah. Let the user do that.

    String itemName = creator.getName();
    StringBuilder message = new StringBuilder();
    if (freeCrafts > 0) {
      message.append("You will run out of free crafting turns before you finished crafting ");
    } else {
      int craftingAdvs = needed - freeCrafts;
      message.append("You are about to spend ");
      message.append(craftingAdvs);
      message.append(" adventure");
      if (craftingAdvs > 1) {
        message.append("s");
      }
      message.append(" crafting ");
    }
    message.append(itemName);
    message.append(" (");
    message.append(count - creator.concoction.getInitial());
    message.append("). Are you sure?");

    if (!InputFieldUtilities.confirm(message.toString())) {
      return false;
    }

    InventoryManager.askedAboutCrafting = KoLCharacter.getUserId();

    return true;
  }

  public static boolean pullableInLoL(int itemId) {
    // the Mayam Calendar is not pullable despite being a usable item & a free pull
    if (itemId == ItemPool.MAYAM_CALENDAR) {
      return false;
    }
    // Only food, booze, potions, combat and usable items may be pulled on this path.
    return switch (ItemDatabase.getConsumptionType(itemId)) {
      case
          // food, booze
          EAT,
          DRINK,
          // potions
          POTION,
          AVATAR_POTION,
          // usable
          USE,
          USE_MULTIPLE,
          USE_INFINITE,
          USE_MESSAGE_DISPLAY,
          FOOD_HELPER,
          DRINK_HELPER,
          CARD,
          FOLDER,
          BOOTSKIN,
          BOOTSPUR -> true;
        // combat
      case NONE -> ItemDatabase.getAttribute(
          itemId, EnumSet.of(Attribute.COMBAT, Attribute.COMBAT_REUSABLE));
      default -> false;
    };
  }

  public static boolean pullableInSeaPath(int itemId) {
    return switch (itemId) {
      case ItemPool.ROUGH_FISH_SCALE,
          ItemPool.PRISTINE_FISH_SCALE,
          ItemPool.RUSTY_BROKEN_DIVING_HELMET,
          ItemPool.AERATED_DIVING_HELMET,
          ItemPool.TEFLON_ORE,
          ItemPool.TEFLON_SWIM_FINS,
          ItemPool.SEA_LEATHER,
          ItemPool.SEA_COWBOY_HAT,
          ItemPool.MERKIN_BUNWIG,
          ItemPool.CRAPPY_MASK,
          ItemPool.CRAPPY_TAILPIECE,
          ItemPool.GLADIATOR_MASK,
          ItemPool.SCHOLAR_MASK,
          ItemPool.GLADIATOR_TAILPIECE,
          ItemPool.SCHOLAR_TAILPIECE,
          ItemPool.MERKIN_HEADGUARD,
          ItemPool.MERKIN_WAISTROPE,
          ItemPool.MERKIN_FACECOWL,
          ItemPool.MERKIN_THIGHGUARD,
          ItemPool.MERKIN_DODGEBALL,
          ItemPool.MERKIN_DRAGNET,
          ItemPool.MERKIN_SWITCHBLADE,
          ItemPool.SEA_CHAPS,
          ItemPool.UNBLEMISHED_PEARL -> false;
      default -> true;
    };
  }
}
