package net.sourceforge.kolmafia.session;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingRequirements;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaASH;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.SpecialOutfit.Checkpoint;
import net.sourceforge.kolmafia.listener.ItemListenerRegistry;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.CoinmastersDatabase;
import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.NPCStoreDatabase;
import net.sourceforge.kolmafia.persistence.RestoresDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.ClanStashRequest;
import net.sourceforge.kolmafia.request.ClosetRequest;
import net.sourceforge.kolmafia.request.CombineMeatRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FamiliarRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.HermitRequest;
import net.sourceforge.kolmafia.request.PurchaseRequest;
import net.sourceforge.kolmafia.request.SewerRequest;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.request.UntinkerRequest;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.textui.ScriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class InventoryManager {
  private static final int BULK_PURCHASE_AMOUNT = 30;

  private static int askedAboutCrafting = 0;
  private static boolean cloverProtectionEnabled = true;

  public static void resetInventory() {
    KoLConstants.inventory.clear();
  }

  public static void refresh() {
    // Retrieve the contents of inventory via api.php
    ApiRequest.updateInventory();
  }

  public static final void parseInventory(final JSONObject JSON) {
    if (JSON == null) {
      return;
    }

    ArrayList<AdventureResult> items = new ArrayList<AdventureResult>();
    ArrayList<AdventureResult> unlimited = new ArrayList<AdventureResult>();

    try {
      // {"1":"1","2":"1" ... }
      Iterator<?> keys = JSON.keys();
      while (keys.hasNext()) {
        String key = (String) keys.next();
        int itemId = StringUtilities.parseInt(key);
        int count = JSON.getInt(key);
        String name = ItemDatabase.getItemDataName(itemId);
        if (name == null) {
          // Fetch descid from api.php?what=item
          // and register new item.
          ItemDatabase.registerItem(itemId);
        }

        if (Limitmode.limitItem(itemId)) {
          unlimited.add(ItemPool.get(itemId, count));
        } else {
          items.add(ItemPool.get(itemId, count));
          switch (itemId) {
            case ItemPool.BOOMBOX:
              if (!Preferences.getString("boomBoxSong").equals("")) {
                KoLCharacter.addAvailableSkill("Sing Along");
              }
              break;
          }
        }
      }
    } catch (JSONException e) {
      ApiRequest.reportParseError("inventory", JSON.toString(), e);
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
    return InventoryManager.getAccessibleCount(ItemPool.get(itemId, 1));
  }

  public static final int getAccessibleCount(final AdventureResult item) {
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
    if (!StandardRequest.isAllowed("Items", item.getName())) {
      return 0;
    }

    int count = item.getCount(KoLConstants.inventory);

    // Items in closet might be accessible, but if the user has
    // marked items in the closet as out-of-bounds, honor that.
    if (InventoryManager.canUseCloset()) {
      count += item.getCount(KoLConstants.closet);
    }

    // Free Pulls from Hagnk's are always accessible
    count += item.getCount(KoLConstants.freepulls);

    // Storage and your clan stash are always accessible
    // once you are out of Ronin or have freed the king,
    // but the user can mark either as out-of-bounds
    if (InventoryManager.canUseStorage()) {
      count += item.getCount(KoLConstants.storage);
    }

    if (InventoryManager.canUseClanStash()) {
      count += item.getCount(ClanManager.getStash());
    }

    count += InventoryManager.getEquippedCount(item);

    for (FamiliarData current : KoLCharacter.getFamiliarList()) {
      if (!current.equals(KoLCharacter.getFamiliar())
          && current.getItem() != null
          && current.getItem().equals(item)) {
        ++count;
      }
    }

    return count;
  }

  public static final int getEquippedCount(final int itemId) {
    return InventoryManager.getEquippedCount(ItemPool.get(itemId, 1));
  }

  public static final int getEquippedCount(final AdventureResult item) {
    int count = 0;
    for (int i = 0; i <= EquipmentManager.FAMILIAR; ++i) {
      AdventureResult equipment = EquipmentManager.getEquipment(i);
      if (equipment != null && equipment.getItemId() == item.getItemId()) {
        ++count;
      }
    }
    return count;
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
    // if we're simulating, we don't need to waste time disabling/enabling clover protection
    if (sim) {
      return InventoryManager.doRetrieveItem(item, isAutomated, useEquipped, sim, canCreate);
    }

    try {
      InventoryManager.setCloverProtection(false);
      return InventoryManager.doRetrieveItem(item, isAutomated, useEquipped, false, canCreate);
    } finally {
      // Restore clover protection
      InventoryManager.setCloverProtection(true);
    }
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

    boolean isRestricted = !StandardRequest.isAllowed("Items", item.getName());
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
      for (FamiliarData current : KoLCharacter.getFamiliarList()) {
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
      for (int i = EquipmentManager.HAT; i <= EquipmentManager.FAMILIAR; ++i) {
        // If you are dual-wielding the target item,
        // remove the one in the offhand slot first
        // since taking from the weapon slot will drop
        // the offhand weapon.
        int slot =
            i == EquipmentManager.WEAPON
                ? EquipmentManager.OFFHAND
                : i == EquipmentManager.OFFHAND ? EquipmentManager.WEAPON : i;

        if (EquipmentManager.getEquipment(slot).equals(item)) {
          if (sim) {
            return "remove";
          }

          SpecialOutfit.replaceEquipmentInSlot(EquipmentRequest.UNEQUIP, slot);

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
            new ClosetRequest(ClosetRequest.CLOSET_TO_INVENTORY, item.getInstance(retrieveCount)));
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
                StorageRequest.STORAGE_TO_INVENTORY, item.getInstance(retrieveCount)));
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
                StorageRequest.STORAGE_TO_INVENTORY, item.getInstance(retrieveCount)));
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
            new ClanStashRequest(item.getInstance(retrieveCount), ClanStashRequest.STASH_TO_ITEMS));
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
        int NPCPrice = NPCStoreDatabase.availablePrice(itemId);
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
    boolean haveBuyScript = Preferences.getString("buyScript").trim().length() > 0;
    boolean scriptSaysBuy = false;

    // Attempt to create the item from existing ingredients (if
    // possible).  The user's buyScript can kick in here and force
    // it to be purchased, rather than created

    Concoction concoction = ConcoctionPool.get(item);
    boolean asked = false;

    if (creator != null && creator.getQuantityPossible() > 0) {
      if (!forceNoMall) {
        boolean defaultBuy = shouldUseMall && InventoryManager.cheaperToBuy(item, missingCount);
        if (sim && haveBuyScript) {
          return defaultBuy ? "create or buy" : "create";
        }
        scriptSaysBuy = InventoryManager.invokeBuyScript(item, missingCount, 2, defaultBuy);
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

    // A ten-leaf clover can be created (by using a disassembled
    // clover) or purchased from the Hermit (if he has any in
    // stock. We tried the former above. Now try the latter.

    if (shouldUseCoinmasters
        && KoLConstants.hermitItems.contains(item)
        && (!shouldUseMall
            || SewerRequest.currentWorthlessItemCost() < StoreManager.getMallPrice(item))) {
      int itemCount =
          itemId == ItemPool.TEN_LEAF_CLOVER
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
      if (creator == null) {
        if (sim) {
          return "buy";
        }
        scriptSaysBuy = true;
      } else {
        boolean defaultBuy = InventoryManager.cheaperToBuy(item, missingCount);
        if (sim && haveBuyScript) {
          return defaultBuy ? "create or buy" : "create";
        }
        scriptSaysBuy = InventoryManager.invokeBuyScript(item, missingCount, 0, defaultBuy);
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
      boolean onlyNPC = forceNoMall || !InventoryManager.canUseMall();
      ArrayList<PurchaseRequest> results =
          onlyNPC ? StoreManager.searchNPCs(item) : StoreManager.searchMall(item);
      KoLmafia.makePurchases(
          results,
          results.toArray(new PurchaseRequest[0]),
          InventoryManager.getPurchaseCount(itemId, missingCount),
          isAutomated,
          0);
      if (!onlyNPC) {
        StoreManager.updateMallPrice(item, results);
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
            new StorageRequest(StorageRequest.STORAGE_TO_INVENTORY, item.getInstance(pullCount)));
        ConcoctionDatabase.setPullsBudgeted(newbudget);
        missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

        if (missingCount <= 0) {
          return "";
        }
      }
    }

    if (creator != null && mixingMethod != CraftingType.NOCREATE) {
      switch (itemId) {
        case ItemPool.DOUGH:
        case ItemPool.DISASSEMBLED_CLOVER:
        case ItemPool.JOLLY_BRACELET:
          scriptSaysBuy = true;
          break;
        default:
          scriptSaysBuy = false;
          break;
      }

      boolean defaultBuy =
          scriptSaysBuy || shouldUseMall && InventoryManager.cheaperToBuy(item, missingCount);
      if (sim && haveBuyScript) {
        return defaultBuy ? "create or buy" : "create";
      }
      scriptSaysBuy = InventoryManager.invokeBuyScript(item, missingCount, 1, defaultBuy);
      missingCount = item.getCount() - item.getCount(KoLConstants.inventory);

      if (missingCount <= 0) {
        return "";
      }
    }

    // If it's creatable, and you have at least one ingredient, see
    // if you can make it via recursion.

    if (creator != null && mixingMethod != CraftingType.NOCREATE && !scriptSaysBuy) {
      boolean makeFromComponents = true;
      if (isAutomated && creator.getQuantityPossible() > 0) {
        // Speculate on how much the items needed to make the creation would cost.
        // Do not retrieve if the average meat spend to make one of the item
        // exceeds the user's autoBuyPriceLimit.

        float meatSpend =
            InventoryManager.priceToMake(item, missingCount, 0, true, true) / missingCount;
        int autoBuyPriceLimit = Preferences.getInteger("autoBuyPriceLimit");
        if (meatSpend > autoBuyPriceLimit) {
          makeFromComponents = false;
          KoLmafia.updateDisplay(
              MafiaState.ERROR,
              "The average amount of meat spent on components ("
                  + KoLConstants.COMMA_FORMAT.format(meatSpend)
                  + ") for one "
                  + item.getName()
                  + " exceeds autoBuyPriceLimit ("
                  + KoLConstants.COMMA_FORMAT.format(autoBuyPriceLimit)
                  + ")");

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

      ArrayList<PurchaseRequest> results = StoreManager.searchMall(item);
      KoLmafia.makePurchases(
          results,
          results.toArray(new PurchaseRequest[0]),
          InventoryManager.getPurchaseCount(itemId, missingCount),
          isAutomated,
          0);
      StoreManager.updateMallPrice(item, results);
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
      final AdventureResult item,
      final int quantity,
      final int ingredientLevel,
      final boolean defaultBuy) {
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
                String.valueOf(quantity),
                String.valueOf(ingredientLevel),
                String.valueOf(defaultBuy)
              });
      KoLmafiaASH.logScriptExecution("Finished buy script: ", scriptFile.getName(), interpreter);
      return v != null && v.intValue() != 0;
    }
    return defaultBuy;
  }

  private static boolean cheaperToBuy(final AdventureResult item, final int quantity) {
    if (!ItemDatabase.isTradeable(item.getItemId())) {
      return false;
    }

    int mallPrice = StoreManager.getMallPrice(item, 7.0f) * quantity;
    if (mallPrice <= 0) {
      return false;
    }

    int makePrice = InventoryManager.priceToMake(item, quantity, 0, false);
    if (makePrice == Integer.MAX_VALUE) {
      return true;
    }

    if (mallPrice / 2 < makePrice && makePrice / 2 < mallPrice) {
      // Less than a 2:1 ratio, we should check more carefully
      mallPrice = StoreManager.getMallPrice(item) * quantity;
      if (mallPrice <= 0) {
        return false;
      }

      makePrice = InventoryManager.priceToMake(item, quantity, 0, true);
      if (makePrice == Integer.MAX_VALUE) {
        return true;
      }
    }

    if (Preferences.getBoolean("debugBuy")) {
      RequestLogger.printLine(
          "\u262F " + item.getInstance(quantity) + " mall=" + mallPrice + " make=" + makePrice);
    }

    return mallPrice < makePrice;
  }

  private static int itemValue(final AdventureResult item, final boolean exact) {
    float factor = Preferences.getFloat("valueOfInventory");
    if (factor <= 0.0f) {
      return 0;
    }

    int lower = 0;
    int autosell = ItemDatabase.getPriceById(item.getItemId());
    int upper = Math.max(0, autosell);

    if (factor <= 1.0f) {
      return lower + (int) ((upper - lower) * factor);
    }

    factor -= 1.0f;
    lower = upper;

    int mall = StoreManager.getMallPrice(item, exact ? 0.0f : 7.0f);
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

  private static int priceToAcquire(
      final AdventureResult item,
      int quantity,
      final int level,
      final boolean exact,
      final boolean mallPriceOnly) {
    int price = 0;
    int onhand = Math.min(quantity, item.getCount(KoLConstants.inventory));
    if (onhand > 0) {
      if (item.getItemId() != ItemPool.PLASTIC_SWORD) {
        price = mallPriceOnly ? 0 : InventoryManager.itemValue(item, exact);
      }

      price *= onhand;
      quantity -= onhand;

      if (quantity == 0) {
        if (Preferences.getBoolean("debugBuy")) {
          RequestLogger.printLine("\u262F " + item.getInstance(onhand) + " onhand=" + price);
        }

        return price;
      }
    }

    int mallPrice = StoreManager.getMallPrice(item, exact ? 0.0f : 7.0f) * quantity;
    if (mallPrice <= 0) {
      mallPrice = Integer.MAX_VALUE;
    } else {
      mallPrice += price;
    }

    int makePrice = InventoryManager.priceToMake(item, quantity, level, exact, mallPriceOnly);
    if (makePrice != Integer.MAX_VALUE) {
      makePrice += price;
    }

    if (!exact && mallPrice / 2 < makePrice && makePrice / 2 < mallPrice) {
      // Less than a 2:1 ratio, we should check more carefully
      return InventoryManager.priceToAcquire(item, quantity, level, true, mallPriceOnly);
    }

    if (Preferences.getBoolean("debugBuy")) {
      RequestLogger.printLine(
          "\u262F " + item.getInstance(quantity) + " mall=" + mallPrice + " make=" + makePrice);
    }

    return Math.min(mallPrice, makePrice);
  }

  private static int priceToMake(
      final AdventureResult item,
      final int quantity,
      final int level,
      final boolean exact,
      final boolean mallPriceOnly) {
    int id = item.getItemId();
    int meatCost = CombineMeatRequest.getCost(id);
    if (meatCost > 0) {
      return meatCost * quantity;
    }

    CraftingType method = ConcoctionDatabase.getMixingMethod(item);
    EnumSet<CraftingRequirements> requirements = ConcoctionDatabase.getRequirements(id);
    if (level > 10 || !ConcoctionDatabase.isPermittedMethod(method, requirements)) {
      return Integer.MAX_VALUE;
    }

    int price = ConcoctionDatabase.getCreationCost(method);
    int yield = ConcoctionDatabase.getYield(id);
    int madeQuantity = (quantity + yield - 1) / yield;

    AdventureResult[] ingredients = ConcoctionDatabase.getIngredients(id);

    for (int i = 0; i < ingredients.length; ++i) {
      AdventureResult ingredient = ingredients[i];
      int needed = ingredient.getCount() * madeQuantity;

      int ingredientPrice =
          InventoryManager.priceToAcquire(ingredient, needed, level + 1, exact, mallPriceOnly);

      if (ingredientPrice == Integer.MAX_VALUE) {
        return ingredientPrice;
      }

      price += ingredientPrice;
    }

    return price * quantity / (yield * madeQuantity);
  }

  private static int priceToMake(
      final AdventureResult item, final int qty, final int level, final boolean exact) {
    return InventoryManager.priceToMake(item, qty, level, exact, false);
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

  private static boolean hasAnyIngredient(final int itemId, HashSet<Integer> seen) {
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

    Integer key = IntegerPool.get(itemId);

    if (seen == null) {
      seen = new HashSet<Integer>();
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
        && !Limitmode.limitMall();
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
    return Preferences.getBoolean("autoSatisfyWithMall") && !Limitmode.limitMall();
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
    return Preferences.getBoolean("autoSatisfyWithNPCs") && !Limitmode.limitNPCStores();
  }

  public static boolean canUseCoinmasters(final AdventureResult item) {
    if (item == null) {
      return false;
    }
    return InventoryManager.canUseCoinmasters(item.getItemId());
  }

  public static boolean canUseCoinmasters(final int itemId) {
    return InventoryManager.canUseCoinmasters() && CoinmastersDatabase.contains(itemId);
  }

  public static boolean canUseCoinmasters() {
    return Preferences.getBoolean("autoSatisfyWithCoinmasters") && !Limitmode.limitCoinmasters();
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
        && !Limitmode.limitClan();
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
    return Preferences.getBoolean("autoSatisfyWithCloset") && !Limitmode.limitCampground();
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
        && !Limitmode.limitStorage();
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

  public static final void checkCrownOfThrones() {
    // If we are wearing the Crown of Thrones, we've already seen
    // which familiar is riding in it
    if (KoLCharacter.hasEquipped(InventoryManager.CROWN_OF_THRONES, EquipmentManager.HAT)) {
      return;
    }

    // The Crown of Thrones is not trendy, but double check anyway
    AdventureResult item = InventoryManager.CROWN_OF_THRONES;
    if (!StandardRequest.isAllowed("Items", item.getName())) {
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
    if (KoLCharacter.hasEquipped(InventoryManager.BUDDY_BJORN, EquipmentManager.CONTAINER)) {
      return;
    }

    // Check if the Buddy Bjorn is Trendy
    AdventureResult item = InventoryManager.BUDDY_BJORN;
    if (!StandardRequest.isAllowed("Items", item.getName())) {
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

  public static final void checkNoHat() {
    AdventureResult NO_HAT = ItemPool.get(ItemPool.NO_HAT, 1);
    String mod = Preferences.getString("_noHatModifier");
    if (!KoLCharacter.hasEquipped(NO_HAT, EquipmentManager.HAT)
        && !KoLConstants.inventory.contains(NO_HAT)) {
      return;
    }
    if (!mod.equals("")) {
      Modifiers.overrideModifier("Item:[" + ItemPool.NO_HAT + "]", mod);
      return;
    }

    checkItemDescription(ItemPool.NO_HAT);
  }

  public static final void checkJickSword() {
    AdventureResult JICK_SWORD = ItemPool.get(ItemPool.JICK_SWORD, 1);
    String mod = Preferences.getString("jickSwordModifier");
    if (!mod.equals("")) {
      Modifiers.overrideModifier("Item:[" + ItemPool.JICK_SWORD + "]", mod);
      return;
    }
    if (!KoLCharacter.hasEquipped(JICK_SWORD, EquipmentManager.WEAPON)
        && !KoLConstants.inventory.contains(JICK_SWORD)) {
      // There are other places it could be, but it only needs to be
      // checked once ever, and if the sword isn't being used then
      // it can be checked later
      return;
    }
    if (!mod.equals("")) {
      Modifiers.overrideModifier("Item:[" + ItemPool.JICK_SWORD + "]", mod);
      return;
    }

    checkItemDescription(ItemPool.JICK_SWORD);
  }

  public static final void checkPantogram() {
    AdventureResult PANTOGRAM_PANTS = ItemPool.get(ItemPool.PANTOGRAM_PANTS, 1);
    String mod = Preferences.getString("_pantogramModifier");
    if (!KoLCharacter.hasEquipped(PANTOGRAM_PANTS, EquipmentManager.PANTS)
        && !KoLConstants.inventory.contains(PANTOGRAM_PANTS)) {
      return;
    }
    if (!mod.equals("")) {
      Modifiers.overrideModifier("Item:[" + ItemPool.PANTOGRAM_PANTS + "]", mod);
      return;
    }

    checkItemDescription(ItemPool.PANTOGRAM_PANTS);
  }

  public static final void checkLatte() {
    AdventureResult LATTE_MUG = ItemPool.get(ItemPool.LATTE_MUG, 1);
    String mod = Preferences.getString("latteModifier");
    if (!KoLCharacter.hasEquipped(LATTE_MUG, EquipmentManager.OFFHAND)
        && !KoLConstants.inventory.contains(LATTE_MUG)) {
      return;
    }
    if (!mod.equals("")) {
      Modifiers.overrideModifier("Item:[" + ItemPool.LATTE_MUG + "]", mod);
      return;
    }

    checkItemDescription(ItemPool.LATTE_MUG);
  }

  public static final void checkSaber() {
    AdventureResult SABER = ItemPool.get(ItemPool.FOURTH_SABER, 1);
    if (!KoLCharacter.hasEquipped(SABER)
        && SABER.getCount(KoLConstants.inventory) == 0
        && SABER.getCount(KoLConstants.closet) == 0) {
      return;
    }
    if (!Preferences.getString("_saberMod").equals("0")) {
      return;
    }

    checkItemDescription(ItemPool.FOURTH_SABER);
  }

  public static final void checkKGB() {
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

  public static final void checkCoatOfPaint() {
    AdventureResult COAT_OF_PAINT = ItemPool.get(ItemPool.COAT_OF_PAINT, 1);
    String mod = Preferences.getString("_coatOfPaintModifier");
    if (!KoLCharacter.hasEquipped(COAT_OF_PAINT, EquipmentManager.SHIRT)
        && !KoLConstants.inventory.contains(COAT_OF_PAINT)) {
      return;
    }
    if (!mod.equals("")) {
      Modifiers.overrideModifier("Item:[" + ItemPool.COAT_OF_PAINT + "]", mod);
      return;
    }

    checkItemDescription(ItemPool.COAT_OF_PAINT);
  }

  public static Pattern BIRD_PATTERN = Pattern.compile("Seek out an? (.*)");

  public static final void checkBirdOfTheDay() {
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

    ResultProcessor.updateBirdModifiers(EffectPool.BLESSING_OF_THE_BIRD, "_birdOfTheDay");
    ResultProcessor.updateBirdModifiers(
        EffectPool.BLESSING_OF_YOUR_FAVORITE_BIRD, "yourFavoriteBird");
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

  public static void checkPowerfulGlove() {
    if (KoLCharacter.hasEquipped(UseSkillRequest.POWERFUL_GLOVE)
        || InventoryManager.hasItem(UseSkillRequest.POWERFUL_GLOVE, false)) {
      // *** Special case: the buffs are always available
      KoLCharacter.addAvailableSkill("CHEAT CODE: Invisible Avatar");
      KoLCharacter.addAvailableSkill("CHEAT CODE: Triple Size");
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

    if (mixingMethod == CraftingType.JEWELRY) {
      freeCrafts += ConcoctionDatabase.getFreeSmithJewelTurns();
    }

    if (mixingMethod == CraftingType.SMITH || mixingMethod == CraftingType.SSMITH) {
      freeCrafts += ConcoctionDatabase.getFreeSmithingTurns();
      freeCrafts += ConcoctionDatabase.getFreeSmithJewelTurns();
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

  public static boolean cloverProtectionActive() {
    return InventoryManager.cloverProtectionEnabled
        && Preferences.getBoolean("cloverProtectActive");
  }

  // Accessory function just to _temporarily_ disable clover protection so that messing with
  // preferences is unnecessary.

  private static void setCloverProtection(boolean enabled) {
    InventoryManager.cloverProtectionEnabled = enabled;
  }
}
