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

final class CandidateLoadoutFactory {
  record Compilation(SlotList<MaximizerLoadout> loadouts, AdventureResult card, int catalogCount) {}

  private final int carriedFamiliarsNeeded;
  private final CarriedFamiliarSelector.Selection carriedFamiliars;
  private final CheckedItem bestCard;
  private final Map<Modeable, String> bestModes;
  private final MaximizerLoadout current = new MaximizerLoadout();

  CandidateLoadoutFactory(
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
    SlotList<MaximizerLoadout> loadouts = new SlotList<>(familiars.size());
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

      List<MaximizerLoadout> slotLoadouts = loadouts.get(entry);
      for (CheckedItem item : items) {
        FamiliarData familiar = entry.isSlot() ? null : familiars.get(entry.famIndex());
        Slot slot = entry.isSlot() ? Evaluator.toUseSlot(entry.slot()) : Slot.FAMILIAR;
        var result = this.create(item, slot, familiar);
        if (result.card() != null) {
          card = result.card();
        }
        slotLoadouts.add(result.loadout());
      }
      Collections.sort(slotLoadouts);
    }

    for (var entry : catalog.entries()) {
      if ((!entry.isSlot() || entry.slot() != Slot.CODPIECE1)
          && entry.value().stream().noneMatch(item -> item.getItemId() == -1)) {
        entry.value().add(new CheckedItem(-1, equipScope, maxPrice, priceLevel));
      }
    }
    int catalogCount = catalog.entries().stream().mapToInt(entry -> entry.value().size()).sum();
    return new Compilation(loadouts, card, catalogCount);
  }

  private Result create(CheckedItem item, Slot slot, FamiliarData familiar) {
    MaximizerLoadout loadout = new MaximizerLoadout();
    loadout.attachment = item;
    if (familiar != null) {
      loadout.setFamiliar(familiar);
    }
    loadout.equipment.put(slot, item);

    if (slot == Slot.CODPIECE1) {
      for (Slot codpieceSlot : ItemSlotGroup.ETERNITY_CODPIECE.slots()) {
        ItemSlotGroup.ETERNITY_CODPIECE.put(loadout, codpieceSlot, EquipmentRequest.UNEQUIP);
      }
      ItemSlotGroup.ETERNITY_CODPIECE.put(loadout, slot, item);
    }

    AdventureResult card = null;
    FamiliarSlotGroup familiarSlots = FamiliarSlotGroup.find(item.getItemId());
    if (familiarSlots != null) {
      if (familiarSlots == FamiliarSlotGroup.CROWN) {
        configureCrown(loadout, item);
      } else {
        configureBjorn(loadout, item);
      }
    } else {
      ItemSlotGroup itemSlots = ItemSlotGroup.find(item.getItemId());
      switch (itemSlots) {
        case CARD_SLEEVE -> {
          card =
              this.bestCard != null ? this.bestCard : this.current.equipment.get(Slot.CARDSLEEVE);
          ItemSlotGroup.CARD_SLEEVE.put(loadout, Slot.CARDSLEEVE, card);
        }
        case STICKERS, FOLDERS, BOOTS -> copyCurrent(loadout, itemSlots);
        case ETERNITY_CODPIECE -> {}
        case null -> configureOther(loadout, item);
      }
    }

    loadout.getScore();
    loadout.clearFailure();
    return new Result(loadout, card);
  }

  private record Result(MaximizerLoadout loadout, AdventureResult card) {}

  private void configureCrown(MaximizerLoadout loadout, CheckedItem item) {
    if (this.carriedFamiliars.lockedCrown() != null) {
      FamiliarSlotGroup.CROWN.put(
          loadout, Slot.CROWNOFTHRONES, this.carriedFamiliars.lockedCrown());
    } else if (this.carriedFamiliarsNeeded > 1) {
      item.automaticFlag = true;
      FamiliarSlotGroup.CROWN.put(loadout, Slot.CROWNOFTHRONES, this.carriedFamiliars.secondBest());
    } else {
      FamiliarSlotGroup.CROWN.put(loadout, Slot.CROWNOFTHRONES, this.carriedFamiliars.best());
    }
  }

  private void configureBjorn(MaximizerLoadout loadout, CheckedItem item) {
    if (this.carriedFamiliars.lockedBjorn() != null) {
      FamiliarSlotGroup.BJORN.put(loadout, Slot.BUDDYBJORN, this.carriedFamiliars.lockedBjorn());
    } else if (this.carriedFamiliarsNeeded > 1) {
      item.automaticFlag = true;
      FamiliarSlotGroup.BJORN.put(loadout, Slot.BUDDYBJORN, this.carriedFamiliars.secondBest());
    } else {
      FamiliarSlotGroup.BJORN.put(loadout, Slot.BUDDYBJORN, this.carriedFamiliars.best());
    }
  }

  private void configureOther(MaximizerLoadout loadout, CheckedItem item) {
    Modeable modeable = Modeable.find(item);
    if (modeable == null) {
      return;
    }
    String best = this.bestModes.getOrDefault(modeable, "");
    if (!best.isEmpty()) {
      loadout.setModeable(modeable, best);
    }
  }

  private void copyCurrent(MaximizerLoadout loadout, SlottedItem<AdventureResult> group) {
    for (Slot slot : group.slots()) {
      group.put(loadout, slot, group.get(this.current, slot));
    }
  }
}
