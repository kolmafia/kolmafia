package net.sourceforge.kolmafia.maximizer;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants.WeaponType;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.SpecialOutfit;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

final class EquipmentSetEvaluator {
  private static final int[][] THREE_ITEM_SYNERGIES = {
    {
      ItemPool.MONSTROUS_MONOCLE, ItemPool.MUSTY_MOCCASINS, ItemPool.MOLTEN_MEDALLION,
    },
    {
      ItemPool.BRAZEN_BRACELET, ItemPool.BITTER_BOWTIE, ItemPool.BEWITCHING_BOOTS,
    },
  };

  private final Evaluator evaluator;
  private final Set<String> requiredOutfits;
  private final EquipScope equipScope;
  private final int maxPrice;
  private final PriceLevel priceLevel;
  private final int dump;
  private final Map<Integer, Boolean> usefulOutfits = new HashMap<>();
  private final Map<AdventureResult, AdventureResult> outfitPieces = new HashMap<>();
  private int usefulSynergies;

  EquipmentSetEvaluator(
      Evaluator evaluator,
      Set<String> requiredOutfits,
      Set<String> excludedOutfits,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel,
      int dump,
      double nullScore) {
    this.evaluator = evaluator;
    this.requiredOutfits = requiredOutfits;
    this.equipScope = equipScope;
    this.maxPrice = maxPrice;
    this.priceLevel = priceLevel;
    this.dump = dump;

    this.findUsefulOutfits(excludedOutfits, nullScore);
    this.findUsefulSynergies(nullScore);
  }

  Map<Integer, Boolean> usefulOutfits() {
    return this.usefulOutfits;
  }

  Map<AdventureResult, AdventureResult> outfitPieces() {
    return this.outfitPieces;
  }

  boolean isUsefulOutfitPiece(int itemId) {
    return this.usefulOutfits.getOrDefault(EquipmentDatabase.getOutfitWithItem(itemId), false);
  }

  boolean isUsefulSynergyPiece(Modifiers modifiers) {
    return (modifiers.getRawBitmap(net.sourceforge.kolmafia.modifiers.BitmapModifier.SYNERGETIC)
            & this.usefulSynergies)
        != 0;
  }

  void retainOutfitPiece(CheckedItem item) throws MaximizerInterruptedException {
    item.validate(this.maxPrice, this.priceLevel);
    if (item.getCount() > 0) {
      this.outfitPieces.put(item, item);
    }
  }

  void evaluate(SlotList<MaximizerSpeculation> speculations) throws MaximizerInterruptedException {
    this.pruneTwoItemSynergies(speculations);
    this.restoreThreeItemSynergies(speculations);
    this.pruneOutfits(speculations);
  }

  private void findUsefulOutfits(Set<String> excludedOutfits, double nullScore) {
    for (var entry : EquipmentDatabase.normalOutfits.entrySet()) {
      int id = entry.getKey();
      SpecialOutfit outfit = entry.getValue();
      if (outfit == null || excludedOutfits.contains(outfit.getName())) {
        continue;
      }
      if (this.requiredOutfits.contains(outfit.getName())) {
        this.usefulOutfits.put(id, true);
        continue;
      }

      Modifiers modifiers = ModifierDatabase.getModifiers(ModifierType.OUTFIT, outfit.getName());
      if (modifiers == null) {
        continue;
      }
      switch (this.evaluator.checkConstraints(modifiers)) {
        case VIOLATES:
          continue;
        case IRRELEVANT:
          if (this.evaluator.getScore(modifiers) - nullScore <= 0.0) {
            continue;
          }
          break;
      }
      this.usefulOutfits.put(id, true);
    }
  }

  private void findUsefulSynergies(double nullScore) {
    for (var entry : ModifierDatabase.getSynergies()) {
      Modifiers modifiers = ModifierDatabase.getModifiers(ModifierType.SYNERGY, entry.getKey());
      if (modifiers != null && this.evaluator.getScore(modifiers) - nullScore > 0.0) {
        this.usefulSynergies |= entry.getValue();
      }
    }
  }

  private void pruneTwoItemSynergies(SlotList<MaximizerSpeculation> speculations)
      throws MaximizerInterruptedException {
    for (var entry : ModifierDatabase.getSynergies()) {
      String synergy = entry.getKey();
      int separator = synergy.indexOf("/");
      String itemName1 = synergy.substring(0, separator);
      String itemName2 = synergy.substring(separator + 1);
      int itemId1 = ItemDatabase.getItemId(itemName1);
      int itemId2 = ItemDatabase.getItemId(itemName2);
      Slot slot1 = EquipmentManager.itemIdToEquipmentType(itemId1);
      Slot slot2 = EquipmentManager.itemIdToEquipmentType(itemId2);
      if (slot1 == Slot.NONE || slot2 == Slot.NONE) {
        continue;
      }

      Slot slot1Lookup =
          EquipmentDatabase.getHands(itemId1) == 1
                  && EquipmentDatabase.getWeaponType(itemId1) == WeaponType.MELEE
              ? Evaluator.WEAPON_1H
              : slot1;
      CheckedItem item1 = findByName(speculations.get(slot1Lookup), itemName1);
      CheckedItem item2 = findByName(speculations.get(slot2), itemName2);
      if (item1 == null || item2 == null) {
        continue;
      }

      MaximizerSpeculation synergySpec = new MaximizerSpeculation();
      MaximizerSpeculation compareSpec = new MaximizerSpeculation();
      Slot useSlot1 = slot1;
      CheckedItem compareItem1 =
          bestUnconditional(speculations.get(slot1Lookup), slot1 == Slot.ACCESSORY1 ? 2 : 0, null);
      compareSpec.equipment.put(
          useSlot1, compareItem1 == null ? EquipmentRequest.UNEQUIP : compareItem1);
      synergySpec.equipment.put(useSlot1, item1);

      Slot useSlot2 = accessorySlot(slot2, slot1 == Slot.ACCESSORY1 ? 1 : 0);
      CheckedItem compareItem2 =
          bestUnconditional(
              speculations.get(slot2),
              slot2 == Slot.ACCESSORY1 ? 1 : 0,
              compareSpec.equipment.get(useSlot1).getName());
      compareSpec.equipment.put(
          useSlot2, compareItem2 == null ? EquipmentRequest.UNEQUIP : compareItem2);
      synergySpec.equipment.put(useSlot2, item2);

      if (synergySpec.compareTo(compareSpec) <= 0 || synergySpec.failed()) {
        item1.automaticFlag = false;
        item2.automaticFlag = false;
      }
    }
  }

  private void restoreThreeItemSynergies(SlotList<MaximizerSpeculation> speculations)
      throws MaximizerInterruptedException {
    ListIterator<MaximizerSpeculation> iterator;
    for (int[] synergy : THREE_ITEM_SYNERGIES) {
      CheckedItem[] items = new CheckedItem[synergy.length];
      iterator =
          speculations.get(Slot.ACCESSORY1).listIterator(speculations.get(Slot.ACCESSORY1).size());
      while (iterator.hasPrevious()) {
        CheckedItem candidate = iterator.previous().attachment;
        candidate.validate(this.maxPrice, this.priceLevel);
        for (int i = 0; i < synergy.length; i++) {
          if (candidate.getItemId() == synergy[i]) {
            items[i] = candidate;
          }
        }
      }
      if (items[0] == null || items[1] == null || items[2] == null) {
        continue;
      }

      MaximizerSpeculation synergySpec = new MaximizerSpeculation();
      MaximizerSpeculation compareSpec = new MaximizerSpeculation();
      Slot slot = Slot.ACCESSORY1;
      for (int i = 0; i < items.length; i++) {
        CheckedItem item = items[i];
        synergySpec.equipment.put(slot, item);
        CheckedItem comparison = bestUnconditional(speculations.get(Slot.ACCESSORY1), i, null);
        compareSpec.equipment.put(slot, comparison == null ? EquipmentRequest.UNEQUIP : comparison);
        slot = nextAccessory(slot);
      }

      if (synergySpec.compareTo(compareSpec) > 0 && !synergySpec.failed()) {
        for (CheckedItem item : items) {
          item.automaticFlag = true;
        }
      }
    }
  }

  private void pruneOutfits(SlotList<MaximizerSpeculation> speculations) {
    StringBuilder summary = new StringBuilder("Outfits [");
    int outfitCount = 0;
    for (var entry : this.usefulOutfits.entrySet()) {
      if (!entry.getValue()) {
        continue;
      }

      MaximizerSpeculation outfitSpec = new MaximizerSpeculation();
      MaximizerSpeculation compareSpec = new MaximizerSpeculation();
      SpecialOutfit outfit = EquipmentDatabase.getOutfit(entry.getKey());
      int accessoryCount = 0;
      for (AdventureResult piece : outfit.getPieces()) {
        int itemId = piece.getItemId();
        Slot lookupSlot = EquipmentManager.itemIdToEquipmentType(itemId);
        if (EquipmentDatabase.getHands(itemId) == 1) {
          lookupSlot = Evaluator.WEAPON_1H;
        }
        Slot useSlot = accessorySlot(lookupSlot, accessoryCount);
        if (lookupSlot == Slot.ACCESSORY1) {
          accessoryCount++;
        }
        if (useSlot == Evaluator.WEAPON_1H) {
          useSlot = Slot.WEAPON;
        }

        CheckedItem comparison =
            bestUnconditional(
                speculations.get(lookupSlot),
                lookupSlot == Slot.ACCESSORY1 ? 3 - accessoryCount : 0,
                null);
        compareSpec.equipment.put(
            useSlot, comparison == null ? EquipmentRequest.UNEQUIP : comparison);
        outfitSpec.equipment.put(
            useSlot, new CheckedItem(itemId, this.equipScope, this.maxPrice, this.priceLevel));
      }

      if (outfitSpec.compareTo(compareSpec) <= 0
          && !this.requiredOutfits.contains(outfit.getName())) {
        entry.setValue(false);
        continue;
      }
      if (outfitCount++ > 0) {
        summary.append(", ");
      }
      summary.append(outfit);
    }
    if (this.dump > 0) {
      RequestLogger.printLine(summary.append("]").toString());
    }
  }

  private CheckedItem findByName(ListIterator<MaximizerSpeculation> iterator, String name)
      throws MaximizerInterruptedException {
    while (iterator.hasPrevious()) {
      CheckedItem item = iterator.previous().attachment;
      item.validate(this.maxPrice, this.priceLevel);
      if (item.getName().equals(name)) {
        return item;
      }
    }
    return null;
  }

  private CheckedItem findByName(List<MaximizerSpeculation> speculations, String name)
      throws MaximizerInterruptedException {
    return findByName(speculations.listIterator(speculations.size()), name);
  }

  private static CheckedItem bestUnconditional(
      List<MaximizerSpeculation> speculations, int skip, String excludedName) {
    ListIterator<MaximizerSpeculation> iterator = speculations.listIterator(speculations.size());
    while (iterator.hasPrevious()) {
      CheckedItem item = iterator.previous().attachment;
      if (item.conditionalFlag || (excludedName != null && item.getName().equals(excludedName))) {
        continue;
      }
      if (skip-- > 0) {
        continue;
      }
      return item;
    }
    return null;
  }

  private static Slot accessorySlot(Slot slot, int offset) {
    if (slot != Slot.ACCESSORY1) {
      return slot;
    }
    return switch (offset) {
      case 0 -> Slot.ACCESSORY1;
      case 1 -> Slot.ACCESSORY2;
      default -> Slot.ACCESSORY3;
    };
  }

  private static Slot nextAccessory(Slot slot) {
    return switch (slot) {
      case ACCESSORY1 -> Slot.ACCESSORY2;
      case ACCESSORY2 -> Slot.ACCESSORY3;
      case ACCESSORY3 -> Slot.NONE;
      default -> throw new IllegalStateException("Unexpected value: " + slot);
    };
  }
}
