package net.sourceforge.kolmafia.maximizer;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.session.EquipmentManager;

final class EquipmentSearchRunner {
  record Options(
      Map<Slot, Integer> slots,
      Map<Modeable, String> modes,
      AdventureResult card,
      FamiliarData crownFamiliar,
      FamiliarData bjornFamiliar,
      int maxPrice,
      PriceLevel priceLevel,
      boolean exhaustive) {}

  private final List<FamiliarData> familiars;
  private final List<FamiliarData> carriedFamiliars;
  private final Map<Integer, Boolean> usefulOutfits;
  private final Map<AdventureResult, AdventureResult> outfitPieces;
  private final Options options;

  EquipmentSearchRunner(
      List<FamiliarData> familiars,
      List<FamiliarData> carriedFamiliars,
      Map<Integer, Boolean> usefulOutfits,
      Map<AdventureResult, AdventureResult> outfitPieces,
      Options options) {
    this.familiars = familiars;
    this.carriedFamiliars = carriedFamiliars;
    this.usefulOutfits = usefulOutfits;
    this.outfitPieces = outfitPieces;
    this.options = options;
  }

  void run(SlotList<CheckedItem> candidates, SlotList<CheckedItem> catalog)
      throws MaximizerInterruptedException {
    MaximizerSpeculation baseline = this.createBaseline();
    if (baseline == null) {
      return;
    }

    this.prepareOffhandCandidates(baseline, candidates, catalog);
    this.applyModes(baseline);

    MaximizerSpeculation exhaustiveBaseline = this.options.exhaustive() ? baseline.clone() : null;
    Maximizer.startSearch(this.options.exhaustive());
    this.search(baseline, candidates);

    if (this.options.exhaustive()) {
      this.validateCatalog(catalog);
      this.prepareExhaustiveCatalog(exhaustiveBaseline, catalog);
      this.search(exhaustiveBaseline, catalog);
    }
  }

  private MaximizerSpeculation createBaseline() {
    MaximizerSpeculation baseline = new MaximizerSpeculation();
    for (int threshold = 1; threshold >= 0; threshold--) {
      boolean anySlots = false;
      for (Slot slot : SlotSet.SLOTS) {
        if (this.options.slots().getOrDefault(slot, 0) >= threshold) {
          baseline.equipment.put(slot, null);
          anySlots = true;
        }
      }
      if (anySlots) {
        return baseline;
      }
    }
    return null;
  }

  private void prepareOffhandCandidates(
      MaximizerSpeculation baseline,
      SlotList<CheckedItem> candidates,
      SlotList<CheckedItem> catalog) {
    if (baseline.equipment.get(Slot.OFFHAND) == null) {
      return;
    }

    candidates.set(Slot.WEAPON, candidates.get(Evaluator.WEAPON_1H));
    if (this.options.exhaustive()) {
      catalog.set(Slot.WEAPON, catalog.get(Evaluator.WEAPON_1H));
    }

    Iterator<AdventureResult> iterator = this.outfitPieces.keySet().iterator();
    while (iterator.hasNext()) {
      int itemId = iterator.next().getItemId();
      if (EquipmentManager.itemIdToEquipmentType(itemId) == Slot.WEAPON
          && EquipmentDatabase.getHands(itemId) > 1) {
        iterator.remove();
      }
    }
  }

  private void applyModes(MaximizerSpeculation baseline) {
    this.options
        .modes()
        .forEach(
            (modeable, mode) -> {
              Set<Slot> backupSlots = EnumSet.of(modeable.getSlot());
              if (modeable.getSlot() == Slot.ACCESSORY1) {
                backupSlots.add(Slot.ACCESSORY2);
                backupSlots.add(Slot.ACCESSORY3);
              }
              if (this.familiars.stream().anyMatch(f -> f.canEquip(modeable.getItem()))) {
                backupSlots.add(Slot.FAMILIAR);
              }

              boolean itemInIgnoredSlot =
                  baseline.equipment.values().stream()
                      .anyMatch(item -> item != null && item.getItemId() == modeable.getItemId());
              if (!itemInIgnoredSlot
                  && backupSlots.stream().anyMatch(slot -> baseline.equipment.get(slot) == null)) {
                baseline.setModeable(modeable, mode);
              }
            });
  }

  private void validateCatalog(SlotList<CheckedItem> catalog) throws MaximizerInterruptedException {
    for (var entry : catalog.entries()) {
      for (CheckedItem item : entry.value()) {
        item.validate(this.options.maxPrice(), this.options.priceLevel());
      }
    }
  }

  private static void prepareExhaustiveCatalog(
      MaximizerSpeculation baseline, SlotList<CheckedItem> catalog) {
    if (baseline.equipment.get(Slot.OFFHAND) == null) {
      catalog.get(Slot.WEAPON).addAll(catalog.get(Evaluator.WEAPON_1H));
    }
    catalog.get(Evaluator.OFFHAND_MELEE).addAll(catalog.get(Slot.OFFHAND));
    catalog.get(Evaluator.OFFHAND_RANGED).addAll(catalog.get(Slot.OFFHAND));
  }

  private void search(MaximizerSpeculation baseline, SlotList<CheckedItem> candidates)
      throws MaximizerInterruptedException {
    baseline.tryAll(
        this.familiars,
        this.carriedFamiliars,
        this.usefulOutfits,
        this.outfitPieces,
        candidates,
        this.options.card(),
        this.options.crownFamiliar(),
        this.options.bjornFamiliar());
  }
}
