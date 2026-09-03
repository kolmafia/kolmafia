package net.sourceforge.kolmafia.maximizer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

final class CandidateSpeculationFactory {
  record Compilation(
      SlotList<MaximizerSpeculation> speculations, AdventureResult card, int catalogCount) {}

  private final int carriedFamiliarsNeeded;
  private final CarriedFamiliarSelector.Selection carriedFamiliars;
  private final CheckedItem bestCard;
  private final Map<Modeable, String> bestModes;
  private final MaximizerSpeculation current = new MaximizerSpeculation();

  CandidateSpeculationFactory(
      int carriedFamiliarsNeeded,
      CarriedFamiliarSelector.Selection carriedFamiliars,
      CheckedItem bestCard,
      Map<Modeable, String> bestModes) {
    this.carriedFamiliarsNeeded = carriedFamiliarsNeeded;
    this.carriedFamiliars = carriedFamiliars;
    this.bestCard = bestCard;
    this.bestModes = bestModes;
  }

  Compilation compile(
      SlotList<CheckedItem> ranked,
      SlotList<CheckedItem> catalog,
      List<FamiliarData> familiars,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel) {
    SlotList<MaximizerSpeculation> speculations = new SlotList<>(familiars.size());
    AdventureResult card = null;

    for (var entry : ranked.entries()) {
      List<CheckedItem> items = entry.value();
      if ((!entry.isSlot() || entry.slot() != Slot.CODPIECE1)
          && (!entry.isSlot()
              || EquipmentManager.getEquipment(Evaluator.toUseSlot(entry.slot()))
                  == EquipmentRequest.UNEQUIP)) {
        CheckedItem unequip = new CheckedItem(-1, equipScope, maxPrice, priceLevel);
        items.add(unequip);
        catalog.get(entry).add(unequip);
      }

      List<MaximizerSpeculation> slotSpeculations = speculations.get(entry);
      for (CheckedItem item : items) {
        FamiliarData familiar = entry.isSlot() ? null : familiars.get(entry.famIndex());
        Slot slot = entry.isSlot() ? Evaluator.toUseSlot(entry.slot()) : Slot.FAMILIAR;
        var result = this.create(item, slot, familiar);
        if (result.card() != null) {
          card = result.card();
        }
        slotSpeculations.add(result.speculation());
      }
      Collections.sort(slotSpeculations);
    }

    for (var entry : catalog.entries()) {
      if ((!entry.isSlot() || entry.slot() != Slot.CODPIECE1)
          && entry.value().stream().noneMatch(item -> item.getItemId() == -1)) {
        entry.value().add(new CheckedItem(-1, equipScope, maxPrice, priceLevel));
      }
    }
    int catalogCount = catalog.entries().stream().mapToInt(entry -> entry.value().size()).sum();
    return new Compilation(speculations, card, catalogCount);
  }

  private Result create(CheckedItem item, Slot slot, FamiliarData familiar) {
    MaximizerSpeculation speculation = new MaximizerSpeculation();
    speculation.attachment = item;
    if (familiar != null) {
      speculation.setFamiliar(familiar);
    }
    speculation.equipment.put(slot, item);

    if (slot == Slot.CODPIECE1) {
      for (Slot codpieceSlot : ItemSlotGroup.ETERNITY_CODPIECE.slots()) {
        ItemSlotGroup.ETERNITY_CODPIECE.put(speculation, codpieceSlot, EquipmentRequest.UNEQUIP);
      }
      ItemSlotGroup.ETERNITY_CODPIECE.put(speculation, slot, item);
    }

    AdventureResult card = null;
    FamiliarSlotGroup familiarSlots = FamiliarSlotGroup.find(item.getItemId());
    if (familiarSlots != null) {
      if (familiarSlots == FamiliarSlotGroup.CROWN) {
        configureCrown(speculation, item);
      } else {
        configureBjorn(speculation, item);
      }
    } else {
      ItemSlotGroup itemSlots = ItemSlotGroup.find(item.getItemId());
      switch (itemSlots) {
        case CARD_SLEEVE -> {
          card =
              this.bestCard != null ? this.bestCard : this.current.equipment.get(Slot.CARDSLEEVE);
          ItemSlotGroup.CARD_SLEEVE.put(speculation, Slot.CARDSLEEVE, card);
        }
        case STICKERS, FOLDERS, BOOTS -> copyCurrent(speculation, itemSlots);
        case ETERNITY_CODPIECE -> {}
        case null -> configureOther(speculation, item);
      }
    }

    speculation.getScore();
    speculation.clearFailure();
    return new Result(speculation, card);
  }

  private record Result(MaximizerSpeculation speculation, AdventureResult card) {}

  private void configureCrown(MaximizerSpeculation speculation, CheckedItem item) {
    if (this.carriedFamiliars.lockedCrown() != null) {
      FamiliarSlotGroup.CROWN.put(
          speculation, Slot.CROWNOFTHRONES, this.carriedFamiliars.lockedCrown());
    } else if (this.carriedFamiliarsNeeded > 1) {
      item.automaticFlag = true;
      FamiliarSlotGroup.CROWN.put(
          speculation, Slot.CROWNOFTHRONES, this.carriedFamiliars.secondBest());
    } else {
      FamiliarSlotGroup.CROWN.put(speculation, Slot.CROWNOFTHRONES, this.carriedFamiliars.best());
    }
  }

  private void configureBjorn(MaximizerSpeculation speculation, CheckedItem item) {
    if (this.carriedFamiliars.lockedBjorn() != null) {
      FamiliarSlotGroup.BJORN.put(
          speculation, Slot.BUDDYBJORN, this.carriedFamiliars.lockedBjorn());
    } else if (this.carriedFamiliarsNeeded > 1) {
      item.automaticFlag = true;
      FamiliarSlotGroup.BJORN.put(speculation, Slot.BUDDYBJORN, this.carriedFamiliars.secondBest());
    } else {
      FamiliarSlotGroup.BJORN.put(speculation, Slot.BUDDYBJORN, this.carriedFamiliars.best());
    }
  }

  private void configureOther(MaximizerSpeculation speculation, CheckedItem item) {
    Modeable modeable = Modeable.find(item);
    if (modeable == null) {
      return;
    }
    String best = this.bestModes.getOrDefault(modeable, "");
    if (!best.isEmpty()) {
      speculation.setModeable(modeable, best);
    }
  }

  private void copyCurrent(MaximizerSpeculation speculation, SlottedItem<AdventureResult> group) {
    for (Slot slot : group.slots()) {
      group.put(speculation, slot, group.get(this.current, slot));
    }
  }
}
