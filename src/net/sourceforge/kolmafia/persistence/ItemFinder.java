package net.sourceforge.kolmafia.persistence;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.IntegerPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CombineMeatRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ItemFinder {
  public enum Match {
    ANY,
    FOOD,
    BOOZE,
    SPLEEN,
    USE,
    CREATE,
    UNTINKER,
    EQUIP,
    CANDY,
    ABSORB,
    ROBO,
    ASDON,
  }

  public static final List<String> getMatchingNames(String searchString) {
    return ItemDatabase.getMatchingNames(searchString);
  }

  public static final String getFirstMatchingItemName(List<String> nameList, String searchString) {
    return ItemFinder.getFirstMatchingItemName(nameList, searchString, Match.ANY);
  }

  public static final String getFirstMatchingItemName(
      List<String> nameList, String searchString, Match filterType) {
    if (nameList == null || nameList.isEmpty()) {
      return null;
    }

    // Filter the list
    ItemFinder.filterNameList(nameList, filterType);
    if (nameList.isEmpty()) {
      return null;
    }

    // If there are multiple matches, such that one is a substring of the
    // others, choose the shorter one, on the grounds that the user would
    // have included part of the unique section of the longer name if that
    // was the item they actually intended.	 This makes it easier to refer
    // to non-clockwork in-a-boxes, and DoD potions by flavor.
    while (nameList.size() >= 2) {
      String name0 = nameList.get(0);
      String name1 = nameList.get(1);
      if (name0.contains(name1)) {
        nameList.remove(0);
      } else if (name1.contains(name0)) {
        nameList.remove(1);
      } else break;
    }

    // If a single item remains, that's it!
    if (nameList.size() == 1) {
      return ItemDatabase.getCanonicalName(nameList.get(0));
    }

    // Remove duplicate names that all refer to the same item?
    Set<Integer> itemIdSet = new HashSet<Integer>();
    int pseudoItems = 0;

    for (int i = 0; i < nameList.size(); ++i) {
      int itemId = ItemDatabase.getItemId(nameList.get(i));
      if (itemId == -1) {
        pseudoItems += 1;
      } else {
        itemIdSet.add(IntegerPool.get(itemId));
      }
    }

    if ((pseudoItems + itemIdSet.size()) == 1) {
      return ItemDatabase.getCanonicalName(nameList.get(0));
    }

    String itemName;
    String rv = null;

    // Candy hearts, snowcones and cupcakes take precedence over
    // all the other items in the game, IF exactly one such item
    // matches.

    for (int i = 0; i < nameList.size(); ++i) {
      itemName = nameList.get(i);
      if (!itemName.startsWith("pix") && itemName.endsWith("candy heart")) {
        if (rv != null) return "";
        rv = ItemDatabase.getCanonicalName(itemName);
      }
    }

    for (int i = 0; i < nameList.size(); ++i) {
      itemName = nameList.get(i);
      if (!itemName.startsWith("abo")
          && !itemName.startsWith("yel")
          && itemName.endsWith("snowcone")) {
        if (rv != null) return "";
        rv = ItemDatabase.getCanonicalName(itemName);
      }
    }

    for (int i = 0; i < nameList.size(); ++i) {
      itemName = nameList.get(i);
      if (itemName.endsWith("cupcake")) {
        if (rv != null) return "";
        rv = ItemDatabase.getCanonicalName(itemName);
      }
    }

    if (rv != null) return rv;

    // If we get here, there is not a single matching item
    return "";
  }

  private static void filterNameList(List<String> nameList, Match filterType) {
    if (filterType != Match.FOOD
        && filterType != Match.BOOZE
        && filterType != Match.SPLEEN
        && filterType != Match.CANDY) {
      // First, check to see if there are an HP/MP restores
      // in the list of matches.  If there are, only return
      // the restorative items (the others are irrelevant).

      ArrayList<String> restoreList = new ArrayList<String>();

      for (int i = 0; i < nameList.size(); ++i) {
        String itemName = nameList.get(i);
        int itemId = ItemDatabase.getItemId(itemName);

        if (RestoresDatabase.isRestore(itemId)) {
          restoreList.add(itemName);
        }
      }

      if (!restoreList.isEmpty()) {
        nameList.clear();
        nameList.addAll(restoreList);
      }
    }

    // Check for consumption filters when matching against the
    // item name.

    Iterator<String> nameIterator = nameList.iterator();

    while (nameIterator.hasNext()) {
      String itemName = nameIterator.next();
      int itemId = ItemDatabase.getItemId(itemName);

      if (filterType == Match.CREATE || filterType == Match.UNTINKER) {
        CraftingType mixMethod = ConcoctionDatabase.getMixingMethod(itemId, itemName);
        boolean condition =
            (filterType == Match.CREATE)
                ? (mixMethod == CraftingType.NOCREATE && CombineMeatRequest.getCost(itemId) == 0)
                : (mixMethod != CraftingType.COMBINE && mixMethod != CraftingType.JEWELRY);
        ItemFinder.conditionalRemove(nameIterator, condition);
        continue;
      }

      int useType = ItemDatabase.getConsumptionType(itemId);

      switch (filterType) {
        case FOOD:
          ItemFinder.conditionalRemove(
              nameIterator,
              useType != KoLConstants.CONSUME_EAT && useType != KoLConstants.CONSUME_FOOD_HELPER);
          break;
        case BOOZE:
          ItemFinder.conditionalRemove(
              nameIterator,
              useType != KoLConstants.CONSUME_DRINK
                  && useType != KoLConstants.CONSUME_DRINK_HELPER);
          break;
        case SPLEEN:
          ItemFinder.conditionalRemove(nameIterator, useType != KoLConstants.CONSUME_SPLEEN);
          break;
        case EQUIP:
          switch (useType) {
            case KoLConstants.EQUIP_FAMILIAR:
            case KoLConstants.EQUIP_ACCESSORY:
            case KoLConstants.EQUIP_HAT:
            case KoLConstants.EQUIP_PANTS:
            case KoLConstants.EQUIP_SHIRT:
            case KoLConstants.EQUIP_WEAPON:
            case KoLConstants.EQUIP_OFFHAND:
            case KoLConstants.EQUIP_CONTAINER:
            case KoLConstants.CONSUME_STICKER:
            case KoLConstants.CONSUME_CARD:
            case KoLConstants.CONSUME_FOLDER:
            case KoLConstants.CONSUME_BOOTSKIN:
            case KoLConstants.CONSUME_BOOTSPUR:
            case KoLConstants.CONSUME_SIXGUN:
              break;

            default:
              nameIterator.remove();
          }

          break;
        case CANDY:
          ItemFinder.conditionalRemove(nameIterator, !ItemDatabase.isCandyItem(itemId));
          break;

        case ABSORB:
          ItemFinder.conditionalRemove(
              nameIterator,
              (ItemDatabase.getNoobSkillId(itemId) == -1
                  && !(ItemDatabase.isEquipment(itemId)
                      && !ItemDatabase.isFamiliarEquipment(itemId))));
          break;

        case ROBO:
          ItemFinder.conditionalRemove(
              nameIterator,
              itemId < ItemPool.LITERAL_GRASSHOPPER
                  || itemId > ItemPool.PHIL_COLLINS
                  || Preferences.getString("_roboDrinks").contains(itemName));
          break;

        case ASDON:
          ItemFinder.conditionalRemove(nameIterator, NPCStoreDatabase.contains(itemId, false));
          break;

        case USE:
          ItemFinder.conditionalRemove(nameIterator, !ItemDatabase.isUsable(itemId));
          break;
      }
    }

    if (nameList.size() == 1 || filterType == Match.CREATE || filterType == Match.UNTINKER) {
      return;
    }

    // Never match against (non-quest) untradeable items not available
    // in NPC stores when other items are possible.
    // This can be overridden by adding "matchable" as a secondary
    // use; this is needed for untradeables that do need to be
    // explicitly referred to, and have names similar to other items
    // (such as the NS Tower keys).

    // If this process results in filtering EVERYTHING in our list, that's not helpful.
    // Make a backup of nameList to restore from in such a case.
    List<String> nameListCopy = new ArrayList<String>(nameList);

    nameIterator = nameList.iterator();

    while (nameIterator.hasNext()) {
      String itemName = nameIterator.next();
      int itemId = ItemDatabase.getItemId(itemName);

      conditionalRemove(
          nameIterator,
          itemId != -1
              && !ItemDatabase.getAttribute(
                  itemId,
                  ItemDatabase.ATTR_TRADEABLE
                      | ItemDatabase.ATTR_MATCHABLE
                      | ItemDatabase.ATTR_QUEST)
              && !NPCStoreDatabase.contains(itemId));
    }

    // restore from last step iff we filtered _everything_
    if (nameList.isEmpty()) {
      nameList.addAll(nameListCopy);
    }
  }

  private static void conditionalRemove(Iterator<String> iterator, boolean condition) {
    if (condition) {
      iterator.remove();
    }
  }

  /**
   * Utility method which determines the first item which matches the given parameter string. Note
   * that the string may also specify an item quantity before the string.
   */
  public static final AdventureResult getFirstMatchingItem(String parameters) {
    return ItemFinder.getFirstMatchingItem(parameters, true, null, ItemFinder.Match.ANY);
  }

  public static final AdventureResult getFirstMatchingItem(String parameters, Match filterType) {
    return ItemFinder.getFirstMatchingItem(parameters, true, null, filterType);
  }

  public static final AdventureResult getFirstMatchingItem(
      String parameters, boolean errorOnFailure) {
    return ItemFinder.getFirstMatchingItem(parameters, errorOnFailure, null, Match.ANY);
  }

  public static final AdventureResult getFirstMatchingItem(
      String parameters, boolean errorOnFailure, Match filterType) {
    return getFirstMatchingItem(parameters, errorOnFailure, null, filterType);
  }

  public static final AdventureResult getFirstMatchingItem(
      String parameters,
      boolean errorOnFailure,
      List<AdventureResult> sourceList,
      Match filterType) {
    // Ignore spaces and tabs in front of the parameter string
    parameters = parameters.trim();

    // If there are no valid strings passed in, return
    if (parameters.length() == 0) {
      if (errorOnFailure) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Need to provide an item to match.");
      }

      return null;
    }

    // Find the item id

    int itemCount = 1;
    int itemId = -1;

    // Allow the person to ask for all of the item from the source
    if (parameters.charAt(0) == '*') {
      itemCount = 0;
      parameters = parameters.substring(1).trim();
    }

    List<String> matchList;

    if (parameters.contains("\u00B6") || parameters.contains("[")) {
      // At least one item is specified by item ID
      if (parameters.contains(",")) {
        // We can't parse multiple items of this sort
        if (errorOnFailure) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "More than one item specified by item ID.");
        }
        return null;
      }

      int spaceIndex = parameters.indexOf(' ');
      if (spaceIndex != -1) {
        String itemCountString = parameters.substring(0, spaceIndex);

        if (StringUtilities.isNumeric(itemCountString)) {
          itemCount = StringUtilities.parseInt(itemCountString);
          parameters = parameters.substring(spaceIndex + 1).trim();
        }
      }

      // KoL has an item whose name includes a pilcrow character.
      // Handle it
      String name = parameters;

      // If the pilcrow character is first, it is followed by an item ID
      if (name.startsWith("\u00B6")) {
        itemId = StringUtilities.parseInt(parameters.substring(1));
      } else if (name.startsWith("[")) {
        int index = name.indexOf("]");
        if (index == -1) {
          return null;
        }
        itemId = StringUtilities.parseInt(name.substring(1, index));
      } else if (ItemDatabase.getItemId(parameters, 1) == -1) {
        // This is not the item with a pilcrow character
        if (errorOnFailure) {
          KoLmafia.updateDisplay(MafiaState.ERROR, "Unknown item " + name);
        }
        return null;
      }

      matchList = new ArrayList<String>();
      if (itemId != -1) {
        matchList.add("[" + itemId + "]");
      } else {
        matchList.add(name);
      }
    } else if (ItemDatabase.getItemId(parameters, 1) != -1) {
      // The entire parameter is a single item
      itemId = ItemDatabase.getItemId(parameters, 1);
      matchList = new ArrayList<String>();
      matchList.add(ItemDatabase.getCanonicalName(itemId));
    } else {
      int spaceIndex = parameters.indexOf(' ');

      if (spaceIndex != -1) {
        String itemCountString = parameters.substring(0, spaceIndex);

        if (StringUtilities.isNumeric(itemCountString)) {
          itemCount = StringUtilities.parseInt(itemCountString);
          parameters = parameters.substring(spaceIndex + 1).trim();
        }
      }

      // This is not right for "1 seal tooth, 2 turtle totem, 3 stolen accordion"
      // since the first count is trimmed off
      matchList = ItemFinder.getMatchingNames(parameters);
    }

    String itemName = ItemFinder.getFirstMatchingItemName(matchList, parameters, filterType);

    if (itemName == null) {
      if (errorOnFailure) {
        String error;
        switch (filterType) {
          case ANY:
          default:
            error = " has no matches.";
            break;
          case FOOD:
            error = " cannot be eaten.";
            break;
          case BOOZE:
            error = " cannot be drunk.";
            break;
          case SPLEEN:
            error = " cannot be chewed.";
            break;
          case USE:
            error = " cannot be used.";
            break;
          case CREATE:
            error = " cannot be created.";
            break;
          case UNTINKER:
            error = " cannot be untinkered.";
            break;
          case EQUIP:
            error = " cannot be equipped.";
            break;
          case CANDY:
            error = " is not candy.";
            break;
          case ABSORB:
            error = " cannot be absorbed.";
            break;
          case ROBO:
            error = " cannot be fed.";
            break;
          case ASDON:
            error = " cannot be used as fuel.";
            break;
        }

        KoLmafia.updateDisplay(MafiaState.ERROR, "[" + parameters + "]" + error);
      }

      return null;
    }

    if (itemName.equals("")) {
      if (errorOnFailure) {
        RequestLogger.printList(matchList);
        RequestLogger.printLine();

        KoLmafia.updateDisplay(MafiaState.ERROR, "[" + parameters + "] has too many matches.");
      }

      return null;
    }

    AdventureResult firstMatch = null;
    if (itemId != -1) {
      firstMatch = ItemPool.get(itemId, itemCount);
    } else {
      firstMatch = ItemPool.get(itemName, itemCount);
    }

    // The result also depends on the number of items which
    // are available in the given match area.

    int matchCount;

    if (filterType == Match.CREATE) {
      boolean skipNPCs = Preferences.getBoolean("autoSatisfyWithNPCs") && itemCount <= 0;

      if (skipNPCs) {
        // Let '*' and negative counts be interpreted
        // relative to the quantity that can be created
        // with on-hand ingredients.

        Preferences.setBoolean("autoSatisfyWithNPCs", false);
        ConcoctionDatabase.refreshConcoctionsNow();
      }

      CreateItemRequest instance = CreateItemRequest.getInstance(firstMatch);
      matchCount = instance == null ? 0 : instance.getQuantityPossible();

      if (skipNPCs) {
        Preferences.setBoolean("autoSatisfyWithNPCs", true);
        ConcoctionDatabase.refreshConcoctionsNow();
      }
    } else if (sourceList == null) {
      // Default to number in inventory if count was "*" (all)
      // or negative (all but that many) and no list was given.
      matchCount = itemCount <= 0 ? firstMatch.getCount(KoLConstants.inventory) : 1;
    } else {
      matchCount = firstMatch.getCount(sourceList);
    }

    // If the person wants all except a certain quantity, update
    // the item count.

    if (itemCount <= 0) {
      if (sourceList == KoLConstants.storage && !KoLCharacter.canInteract()) {
        // Pulls are budgeted.
        itemCount = 0;
        return firstMatch.getInstance(itemCount);
      }

      itemCount = matchCount + itemCount;
      firstMatch = firstMatch.getInstance(itemCount);
    } else if (matchCount < itemCount && sourceList != null) {
      if (errorOnFailure) {
        String message = "";
        if (sourceList == KoLConstants.freepulls
            && !Modifiers.getBooleanModifier("Item", firstMatch.getItemId(), "Free Pull")) {
          message = "[" + firstMatch.getName() + "] requested, but it's not a Free Pull";
        } else {
          message =
              "["
                  + (itemCount > 1 ? itemCount + " " : "")
                  + firstMatch.getName()
                  + "] requested, but "
                  + (matchCount == 0 ? "none" : "only " + matchCount)
                  + " available.";
        }
        KoLmafia.updateDisplay(MafiaState.ERROR, message);
      }

      return null;
    }

    if (KoLmafiaCLI.isExecutingCheckOnlyCommand) {
      KoLmafia.updateDisplay(firstMatch == null ? "No match" : firstMatch.toString());
    }

    return itemCount <= 0 ? null : firstMatch;
  }

  public static AdventureResult[] getMatchingItemList(String itemList) {
    return ItemFinder.getMatchingItemList(itemList, true, null, Match.ANY);
  }

  public static AdventureResult[] getMatchingItemList(String itemList, boolean errorOnFailure) {
    return ItemFinder.getMatchingItemList(itemList, errorOnFailure, null, Match.ANY);
  }

  public static AdventureResult[] getMatchingItemList(
      String itemList, List<AdventureResult> sourceList) {
    return ItemFinder.getMatchingItemList(itemList, true, sourceList, Match.ANY);
  }

  public static AdventureResult[] getMatchingItemList(
      String itemList, boolean errorOnFailure, List<AdventureResult> sourceList) {
    return ItemFinder.getMatchingItemList(itemList, errorOnFailure, sourceList, Match.ANY);
  }

  public static AdventureResult[] getMatchingItemList(
      String itemList, boolean errorOnFailure, List<AdventureResult> sourceList, Match filterType) {
    AdventureResult firstMatch =
        ItemFinder.getFirstMatchingItem(itemList, false, sourceList, filterType);
    if (firstMatch != null) {
      AdventureResult[] items = new AdventureResult[1];
      items[0] = firstMatch;
      return items;
    }

    String[] itemNames = itemList.split("\\s*,\\s*");

    boolean isMeatMatch = false;
    ArrayList<AdventureResult> items = new ArrayList<AdventureResult>();

    for (String name : itemNames) {
      isMeatMatch = false;

      if (name.endsWith(" meat")) {
        if (sourceList == KoLConstants.freepulls) {
          continue;
        }

        String amountString = name.substring(0, name.length() - 5).trim();

        if (amountString.equals("*") || StringUtilities.isNumeric(amountString)) {
          isMeatMatch = true;

          long amount = 0;

          if (!amountString.equals("*")) {
            amount = StringUtilities.parseInt(amountString);
          }

          if (amount <= 0) {
            amount +=
                sourceList == KoLConstants.storage
                    ? KoLCharacter.getStorageMeat()
                    : sourceList == KoLConstants.closet
                        ? KoLCharacter.getClosetMeat()
                        : KoLCharacter.getAvailableMeat();
          }

          firstMatch = new AdventureLongCountResult(AdventureResult.MEAT, amount);
        }
      }

      if (!isMeatMatch) {
        firstMatch = ItemFinder.getFirstMatchingItem(name, errorOnFailure, sourceList, filterType);
      }

      if (firstMatch != null) {
        AdventureResult.addResultToList(items, firstMatch);
      }
    }

    AdventureResult[] result = new AdventureResult[items.size()];
    return items.toArray(result);
  }
}
