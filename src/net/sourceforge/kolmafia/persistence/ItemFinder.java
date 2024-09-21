package net.sourceforge.kolmafia.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AdventureResult.AdventureLongCountResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLConstants.CraftingType;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase.Attribute;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CombineMeatRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.StorageRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class ItemFinder {
  private ItemFinder() {}

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

  public static class SingleResult {
    public enum Type {
      NO_MATCH,
      SINGLE_MATCH,
      MULTIPLE_MATCHES
    }

    public final Type type;
    public final String value;

    private SingleResult(Type type, String value) {
      this.type = type;
      this.value = value;
    }

    public static final SingleResult NO_MATCH = new SingleResult(Type.NO_MATCH, null);
    public static final SingleResult MULTIPLE_MATCHES =
        new SingleResult(Type.MULTIPLE_MATCHES, null);

    public static SingleResult match(String value) {
      return new SingleResult(Type.SINGLE_MATCH, value);
    }
  }

  public static List<String> getMatchingNames(String searchString) {
    return ItemDatabase.getMatchingNames(searchString);
  }

  public static SingleResult getFirstMatchingItemName(List<String> nameList, String searchString) {
    return ItemFinder.getFirstMatchingItemName(nameList, searchString, Match.ANY);
  }

  /**
   * Get the first matching item name out of a list of potential matches.
   *
   * @param nameList List of potential matches
   * @param searchString Query used to search for matches
   * @param filterType Type of item being sought
   * @return NO_MATCH if no valid matches, MULTIPLE_MATCHES if too many matches, SINGLE_MATCH with
   *     an item name otherwise.
   */
  public static SingleResult getFirstMatchingItemName(
      List<String> nameList, String searchString, Match filterType) {
    if (nameList == null || nameList.isEmpty()) {
      return SingleResult.NO_MATCH;
    }

    ItemFinder.prioritizeRestores(nameList, filterType);
    ItemFinder.removeInappropriateMatchTypes(nameList, filterType);

    // If one left, return.
    if (nameList.size() == 1) {
      return SingleResult.match(nameList.get(0));
    }

    // If query is unique initialism, return.
    if (!searchString.contains(" ")) {
      var initialismName = ItemDatabase.getNameByInitialismIfUnique(searchString);
      if (initialismName != null) return SingleResult.match(initialismName);
    }

    var singleAvailable = ItemFinder.getSingleAvailableItem(nameList, filterType);
    if (singleAvailable != null) return SingleResult.match(singleAvailable);

    ItemFinder.removeInaccessibleItems(nameList, filterType);
    ItemFinder.removeSuperstringMatches(nameList);

    singleAvailable = ItemFinder.getSingleAvailableItem(nameList, filterType);
    if (singleAvailable != null) return SingleResult.match(singleAvailable);

    if (nameList.isEmpty()) {
      return SingleResult.NO_MATCH;
    }

    // Remove duplicates (?). If there's one item left, that's it.
    var singleItem = ItemFinder.getSingleItem(nameList);
    if (singleItem != null) return SingleResult.match(singleItem);

    // Too many matches.
    return SingleResult.MULTIPLE_MATCHES;
  }

  private static String getSingleAvailableItem(List<String> nameList, Match filterType) {
    // If only one available per user settings, return.
    if (filterType != Match.CREATE && filterType != Match.ANY) {
      var available =
          nameList.stream()
              .filter(
                  name ->
                      Arrays.stream(ItemDatabase.getItemIds(name))
                          .anyMatch(InventoryManager::itemAvailable))
              .collect(Collectors.toList());
      if (available.size() == 1) {
        return available.get(0);
      }
    }
    return null;
  }

  private static String getSingleItem(List<String> nameList) {
    // Remove duplicate names that all refer to the same item?
    Set<Integer> itemIdSet = new HashSet<>();
    int pseudoItems = 0;

    for (String s : nameList) {
      int itemId = ItemDatabase.getItemId(s);
      if (itemId == -1) {
        pseudoItems += 1;
      } else {
        itemIdSet.add(itemId);
      }
    }

    if ((pseudoItems + itemIdSet.size()) == 1) {
      return ItemDatabase.getCanonicalName(nameList.get(0));
    }
    return null;
  }

  private static void removeSuperstringMatches(List<String> nameList) {
    // If there are multiple matches, such that one is a substring of the
    // others, choose the shorter one, on the grounds that the user would have
    // included part of the unique section of the longer name if that was the
    // item they actually intended.  This makes it easier to refer to
    // non-clockwork in-a-boxes, and DoD potions by flavor.
    // NB: this only removes superstrings of the first string.
    if (nameList.isEmpty()) return;
    nameList.sort(Comparator.comparingInt(String::length));
    var name0 = nameList.get(0);
    nameList.removeIf(s -> !s.equals(name0) && s.contains(name0));
  }

  private static void prioritizeRestores(List<String> nameList, Match filterType) {
    if (filterType != Match.FOOD
        && filterType != Match.BOOZE
        && filterType != Match.SPLEEN
        && filterType != Match.CANDY) {
      // First, check to see if there are an HP/MP restores
      // in the list of matches.  If there are, only return
      // the restorative items (the others are irrelevant).

      ArrayList<String> restoreList = new ArrayList<>();

      for (String itemName : nameList) {
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
  }

  private static void removeInappropriateMatchTypes(List<String> nameList, Match filterType) {
    // Check for consumption filters when matching against the
    // item name.

    Iterator<String> nameIterator = nameList.iterator();

    while (nameIterator.hasNext()) {
      String itemName = nameIterator.next();
      int itemId = ItemDatabase.getItemId(itemName);

      CraftingType mixMethod = ConcoctionDatabase.getMixingMethod(itemId, itemName);
      if (filterType == Match.CREATE
          && mixMethod == CraftingType.NOCREATE
          && CombineMeatRequest.getCost(itemId) == 0) {
        nameIterator.remove();
        continue;
      } else if (filterType == Match.UNTINKER
          && mixMethod != CraftingType.COMBINE
          && mixMethod != CraftingType.JEWELRY) {
        nameIterator.remove();
        continue;
      }

      ConsumptionType useType = ItemDatabase.getConsumptionType(itemId);

      switch (filterType) {
        case FOOD:
          ItemFinder.conditionalRemove(
              nameIterator,
              useType != ConsumptionType.EAT && useType != ConsumptionType.FOOD_HELPER);
          break;
        case BOOZE:
          ItemFinder.conditionalRemove(
              nameIterator,
              useType != ConsumptionType.DRINK && useType != ConsumptionType.DRINK_HELPER);
          break;
        case SPLEEN:
          ItemFinder.conditionalRemove(nameIterator, useType != ConsumptionType.SPLEEN);
          break;
        case EQUIP:
          switch (useType) {
            case FAMILIAR_EQUIPMENT:
            case ACCESSORY:
            case HAT:
            case PANTS:
            case SHIRT:
            case WEAPON:
            case OFFHAND:
            case CONTAINER:
            case STICKER:
            case CARD:
            case FOLDER:
            case BOOTSKIN:
            case BOOTSPUR:
            case SIXGUN:
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
              (ItemDatabase.getNoobSkillId(itemId) == 0
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
  }

  private static void removeInaccessibleItems(List<String> nameList, Match filterType) {
    if (nameList.size() == 1 || filterType == Match.CREATE) {
      return;
    }

    // Never match against (non-quest) untradeable items not
    // in NPC stores or otherwise available per user settings when other items are possible.
    // This can be overridden by adding "matchable" as a secondary
    // use; this is needed for untradeables that do need to be
    // explicitly referred to, and have names similar to other items
    // (such as the NS Tower keys).

    // If this process results in filtering EVERYTHING in our list, that's not helpful.
    // Make a backup of nameList to restore from in such a case.
    var nameListCopy = new ArrayList<>(nameList);
    var nameIterator = nameList.iterator();

    while (nameIterator.hasNext()) {
      String itemName = nameIterator.next();
      int itemId = ItemDatabase.getItemId(itemName);

      conditionalRemove(
          nameIterator,
          itemId != -1
              && !ItemDatabase.getAttribute(
                  itemId, EnumSet.of(Attribute.TRADEABLE, Attribute.MATCHABLE, Attribute.QUEST))
              && !NPCStoreDatabase.contains(itemId)
              && !InventoryManager.itemAvailable(itemId)
              && ConcoctionDatabase.getIngredients(itemId).length == 0);
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
  public static AdventureResult getFirstMatchingItem(String parameters) {
    return ItemFinder.getFirstMatchingItem(parameters, true, null, ItemFinder.Match.ANY);
  }

  public static AdventureResult getFirstMatchingItem(String parameters, Match filterType) {
    return ItemFinder.getFirstMatchingItem(parameters, true, null, filterType);
  }

  public static AdventureResult getFirstMatchingItem(String parameters, boolean errorOnFailure) {
    return ItemFinder.getFirstMatchingItem(parameters, errorOnFailure, null, Match.ANY);
  }

  public static AdventureResult getFirstMatchingItem(
      String parameters, boolean errorOnFailure, Match filterType) {
    return getFirstMatchingItem(parameters, errorOnFailure, null, filterType);
  }

  public static AdventureResult getFirstMatchingItem(
      String parameters,
      boolean errorOnFailure,
      List<AdventureResult> sourceList,
      Match filterType) {
    // Ignore spaces and tabs at ends of the parameter string
    parameters = parameters.trim();

    // If there are no valid strings passed in, return
    if (parameters.length() == 0) {
      if (errorOnFailure) {
        KoLmafia.updateDisplay(MafiaState.ERROR, "Need to provide an item to match.");
      }

      return null;
    }

    // Find the item id

    boolean returnAll = false;
    int itemCount = 1;
    int itemId = -1;

    // Allow the person to ask for all of the item from the source
    if (parameters.charAt(0) == '*') {
      returnAll = true;
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

      matchList = new ArrayList<>();
      if (itemId != -1) {
        matchList.add("[" + itemId + "]");
      } else {
        matchList.add(name);
      }
    } else if (wrapHelper(parameters) != -1) {
      // The entire parameter is a single item
      itemId = ItemDatabase.getItemId(parameters, 1);
      matchList = new ArrayList<>();
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

      matchList = ItemFinder.getMatchingNames(parameters);
    }

    SingleResult match = ItemFinder.getFirstMatchingItemName(matchList, parameters, filterType);

    String itemName = null;
    switch (match.type) {
      case NO_MATCH -> {
        if (errorOnFailure) {
          String error =
              switch (filterType) {
                case ANY -> " has no matches.";
                case FOOD -> " cannot be eaten.";
                case BOOZE -> " cannot be drunk.";
                case SPLEEN -> " cannot be chewed.";
                case USE -> " cannot be used.";
                case CREATE -> " cannot be created.";
                case UNTINKER -> " cannot be untinkered.";
                case EQUIP -> " cannot be equipped.";
                case CANDY -> " is not candy.";
                case ABSORB -> " cannot be absorbed.";
                case ROBO -> " cannot be fed.";
                case ASDON -> " cannot be used as fuel.";
              };

          KoLmafia.updateDisplay(MafiaState.ERROR, "[" + parameters + "]" + error);
        }

        return null;
      }
      case MULTIPLE_MATCHES -> {
        if (errorOnFailure) {
          RequestLogger.printList(matchList);
          RequestLogger.printLine();

          KoLmafia.updateDisplay(MafiaState.ERROR, "[" + parameters + "] has too many matches.");
        }

        return null;
      }
      case SINGLE_MATCH -> itemName = match.value;
    }

    AdventureResult firstMatch;
    if (itemId != -1) {
      firstMatch = ItemPool.get(itemId, itemCount);
    } else {
      firstMatch = ItemPool.get(itemName, itemCount);
    }

    // if the user asked for zero, give them zero
    if (itemCount == 0) return firstMatch;

    if (returnAll) itemCount = 0;

    // The result also depends on the number of items which
    // are available in the given match area.

    int matchCount;

    if (filterType == Match.CREATE) {
      boolean skipNPCs = Preferences.getBoolean("autoSatisfyWithNPCs") && itemCount <= 0;

      try {
        if (skipNPCs) {
          // Let '*' and negative counts be interpreted
          // relative to the quantity that can be created
          // with on-hand ingredients.

          Preferences.setBoolean("autoSatisfyWithNPCs", false);
          ConcoctionDatabase.refreshConcoctionsNow();
        }

        CreateItemRequest instance = CreateItemRequest.getInstance(firstMatch);
        matchCount = instance == null ? 0 : instance.getQuantityPossible();
      } finally {
        if (skipNPCs) {
          Preferences.setBoolean("autoSatisfyWithNPCs", true);
          ConcoctionDatabase.refreshConcoctionsNow();
        }
      }
    } else if (sourceList == null) {
      // Default to number in inventory if count was "*" (all)
      // or negative (all but that many) and no list was given.
      matchCount = itemCount <= 0 ? firstMatch.getCount(KoLConstants.inventory) : 1;
    } else if (sourceList == KoLConstants.storage) {
      // Either storage or freepulls; if we can interact, both are on
      // storage. Otherwise, they are split as appropriate.
      matchCount =
          firstMatch.getCount(KoLConstants.storage) + firstMatch.getCount(KoLConstants.freepulls);
    } else {
      matchCount = firstMatch.getCount(sourceList);
    }

    // If the person wants all except a certain quantity, update
    // the item count.

    if (itemCount <= 0) {
      if (sourceList == KoLConstants.storage
          && !KoLCharacter.canInteract()
          && !StorageRequest.isFreePull(firstMatch)) {
        // Pulls are budgeted.
        itemCount = 0;
        return firstMatch.getInstance(itemCount);
      }

      itemCount = matchCount + itemCount;
      firstMatch = firstMatch.getInstance(itemCount);
    } else if (matchCount < itemCount && sourceList != null) {
      if (errorOnFailure) {
        String message;
        if (sourceList == KoLConstants.freepulls && !StorageRequest.isFreePull(firstMatch)) {
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

    boolean isMeatMatch;
    ArrayList<AdventureResult> items = new ArrayList<>();

    for (String name : itemNames) {
      isMeatMatch = false;

      if (name.toLowerCase().endsWith(" meat")) {
        if (sourceList == KoLConstants.freepulls) {
          continue;
        }

        String amountString = name.substring(0, name.length() - 5).trim();

        if (amountString.equals("*") || StringUtilities.isNumeric(amountString)) {
          isMeatMatch = true;

          long amount = 0;

          if (!amountString.equals("*")) {
            amount = StringUtilities.parseLong(amountString);
          }

          if (amount < 0 || amountString.equals("*")) {
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

  private static int wrapHelper(String parameters) {
    int spaceIndex = parameters.indexOf(' ');
    if (spaceIndex == -1) return -1;
    String itemCountString = parameters.substring(0, spaceIndex);
    if (!StringUtilities.isNumeric(itemCountString)) return -1;
    String possibleItem = parameters.substring(spaceIndex + 1).trim();
    int possibleId = ItemDatabase.getExactItemId(possibleItem);
    if (possibleId != -1) return -1;
    return ItemDatabase.getItemId(parameters, 1);
  }
}
