package net.sourceforge.kolmafia.maximizer;

import java.util.EnumSet;
import java.util.List;
import java.util.ListIterator;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase.FoldGroup;
import net.sourceforge.kolmafia.preferences.Preferences;

final class CandidateShortlistCompiler {
  record Result(SlotList<CheckedItem> candidates, int candidateCount) {}

  private final List<FamiliarData> familiars;
  private final CharacterSnapshot character;
  private final EquipScope equipScope;
  private final long maxPrice;
  private final PriceLevel priceLevel;
  private final int dump;

  CandidateShortlistCompiler(
      List<FamiliarData> familiars,
      CharacterSnapshot character,
      EquipScope equipScope,
      long maxPrice,
      PriceLevel priceLevel,
      int dump) {
    this.familiars = familiars;
    this.character = character;
    this.equipScope = equipScope;
    this.maxPrice = maxPrice;
    this.priceLevel = priceLevel;
    this.dump = dump;
  }

  Result compile(
      SlotList<CheckedItem> ranked,
      SlotList<MaximizerLoadout> loadouts,
      boolean codpieceCanExpandAccessoryPool)
      throws MaximizerInterruptedException {
    SlotList<CheckedItem> candidates = new SlotList<>(this.familiars.size());
    for (var entry : ranked.entries()) {
      List<CheckedItem> rankedItems = ranked.get(entry);
      List<CheckedItem> selected = candidates.get(entry);

      if (this.dump > 0) {
        RequestLogger.printLine(
            "SLOT " + (entry.isSlot() ? entry.slot() : "BONUS FAMILIAR #" + entry.famIndex()));
      }
      if (this.dump > 1) {
        RequestLogger.printLine(loadouts.get(entry).toString());
      }

      int total = addRequired(rankedItems, selected);
      int useful = entry.isSlot() ? maxUseful(entry.slot()) : 1;
      if (useful > total
          || (codpieceCanExpandAccessoryPool
              && entry.isSlot()
              && entry.slot() == Slot.ACCESSORY1)) {
        addBestAvailable(entry, loadouts, candidates, selected, total, useful);
      }

      if (selected.size() == 1
          && ItemDatabase.getFoldGroup(selected.getFirst().getName()) != null) {
        selected.add(new CheckedItem(-1, this.equipScope, this.maxPrice, this.priceLevel));
      }
      if (this.dump > 0) {
        RequestLogger.printLine(selected.toString());
      }
    }

    int candidateCount =
        candidates.entries().stream().mapToInt(entry -> entry.value().size()).sum();
    candidates.get(Slot.WEAPON).addAll(candidates.get(Evaluator.WEAPON_1H));
    candidates.get(Evaluator.OFFHAND_MELEE).addAll(candidates.get(Slot.OFFHAND));
    candidates.get(Evaluator.OFFHAND_RANGED).addAll(candidates.get(Slot.OFFHAND));
    return new Result(candidates, candidateCount);
  }

  private int addRequired(List<CheckedItem> ranked, List<CheckedItem> selected) {
    int total = 0;
    for (CheckedItem item : ranked) {
      if (!item.requiredFlag) {
        continue;
      }
      selected.add(item);
      int itemId = item.getItemId();
      if (itemId != ItemPool.BROKEN_CHAMPAGNE && itemId != ItemPool.MAKESHIFT_GARBAGE_SHIRT) {
        total++;
      }
    }
    return total;
  }

  private void addBestAvailable(
      SlotList.Entry<CheckedItem> entry,
      SlotList<MaximizerLoadout> loadouts,
      SlotList<CheckedItem> candidates,
      List<CheckedItem> selected,
      int total,
      int useful)
      throws MaximizerInterruptedException {
    ListIterator<MaximizerLoadout> iterator =
        loadouts.get(entry).listIterator(loadouts.get(entry).size());
    int resourceCandidates = 0;
    ResourceUsage resourceUsage = this.character.resourceUsage("");

    while (iterator.hasPrevious()) {
      CheckedItem item = iterator.previous().attachment;
      item.validate(this.maxPrice, this.priceLevel);
      int foldItemsNeeded = foldItemsNeeded(entry, item, candidates, loadouts, useful);
      if (item.getCount() == 0) {
        continue;
      }

      boolean leavesSlotEmpty = item.getItemId() == -1;
      ResourceUsage itemResourceUsage = this.character.resourceUsage(item.getName());
      if (!itemResourceUsage.isZero()) {
        if (item.automaticFlag) {
          if (!selected.contains(item)) {
            selected.add(item);
          }
          if (!leavesSlotEmpty) {
            resourceCandidates += item.getCount();
            resourceUsage = resourceUsage.plus(itemResourceUsage.times(item.getCount()));
          }
        } else if (total < useful
            && resourceCandidates < useful
            && this.character.hasRemainingCapacityFor(resourceUsage, itemResourceUsage)) {
          if (!selected.contains(item)) {
            selected.add(item);
          }
          if (!leavesSlotEmpty) {
            resourceCandidates += item.getCount();
            resourceUsage = resourceUsage.plus(itemResourceUsage.times(item.getCount()));
          }
        }
      } else if (item.automaticFlag) {
        if (!selected.contains(item)) {
          selected.add(item);
          if (!leavesSlotEmpty && !item.conditionalFlag && item.getCount() >= foldItemsNeeded) {
            total += item.getCount();
          }
        }
      } else if ((entry.isSlot() && entry.slot() == Slot.CODPIECE1) || total < useful) {
        if (!selected.contains(item)) {
          selected.add(item);
          if (!leavesSlotEmpty && !item.conditionalFlag && item.getCount() >= foldItemsNeeded) {
            total += item.getCount();
          }
        }
      }
    }
  }

  private int foldItemsNeeded(
      SlotList.Entry<CheckedItem> entry,
      CheckedItem item,
      SlotList<CheckedItem> selected,
      SlotList<MaximizerLoadout> loadouts,
      int useful) {
    FoldGroup group = ItemDatabase.getFoldGroup(item.getName());
    if (group == null || !Preferences.getBoolean("maximizerFoldables")) {
      return 0;
    }

    int needed = Math.max(item.getCount(), useful);
    for (Slot checkSlot : SlotSet.SLOTS) {
      if (entry.isSlot() && checkSlot.ordinal() >= entry.slot().ordinal()) {
        break;
      }
      for (CheckedItem selectedItem : selected.get(checkSlot)) {
        FoldGroup selectedGroup = ItemDatabase.getFoldGroup(selectedItem.getName());
        if (sameGroup(group, selectedGroup)) {
          needed += Math.max(selectedItem.getCount(), maxUseful(checkSlot));
        }
      }
    }

    if (entry.isSlot() && entry.slot().ordinal() < Slot.FAMILIAR.ordinal()) {
      for (Slot checkSlot :
          EnumSet.range(Slot.byOrdinal(entry.slot().ordinal() + 1), Slot.FAMILIAR)) {
        ListIterator<MaximizerLoadout> iterator =
            loadouts.get(checkSlot).listIterator(loadouts.get(checkSlot).size());
        int usefulCheckCount = maxUseful(checkSlot);
        while (iterator.hasPrevious()) {
          CheckedItem checkItem = iterator.previous().attachment;
          FoldGroup checkGroup = ItemDatabase.getFoldGroup(checkItem.getName());
          if (sameGroup(group, checkGroup)) {
            if (usefulCheckCount > 0 || checkItem.requiredFlag) {
              needed += Math.max(checkItem.getCount(), maxUseful(checkSlot));
            }
          } else if (checkItem.automaticFlag || !checkItem.conditionalFlag) {
            usefulCheckCount--;
          }
        }
      }
    }
    return needed;
  }

  private static boolean sameGroup(FoldGroup group, FoldGroup other) {
    return other != null && group.names.getFirst().equals(other.names.getFirst());
  }

  private int maxUseful(Slot slot) {
    return switch (slot) {
      case /* Evaluator.WEAPON_1H */ STICKER3 ->
          1
              + relevantSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING)
              + relevantFamiliar(FamiliarPool.HAND);
      case OFFHAND -> 1 + relevantFamiliar(FamiliarPool.LEFT_HAND);
      case ACCESSORY1 -> 3;
      case CODPIECE1 -> 5;
      case FAMILIAR ->
          1
              + relevantFamiliar(FamiliarPool.SCARECROW)
              + relevantFamiliar(FamiliarPool.HAND)
              + relevantFamiliar(FamiliarPool.HATRACK);
      default -> 1;
    };
  }

  private static int relevantSkill(int skillId) {
    return KoLCharacter.hasSkill(skillId) ? 1 : 0;
  }

  private int relevantFamiliar(int familiarId) {
    if (KoLCharacter.getFamiliar().getId() == familiarId) {
      return 1;
    }
    return this.familiars.stream().anyMatch(familiar -> familiar.getId() == familiarId) ? 1 : 0;
  }
}
