package net.sourceforge.kolmafia.maximizer;

import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

final class CandidateSpeculationFactory {
  record Result(MaximizerSpeculation speculation, AdventureResult card) {}

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

  Result create(CheckedItem item, Slot slot, FamiliarData familiar) {
    MaximizerSpeculation speculation = new MaximizerSpeculation();
    speculation.attachment = item;
    if (familiar != null) {
      speculation.setFamiliar(familiar);
    }
    speculation.equipment.put(slot, item);

    if (slot == Slot.CODPIECE1) {
      for (Slot codpieceSlot : SlotSet.CODPIECE_SLOTS) {
        speculation.equipment.put(codpieceSlot, EquipmentRequest.UNEQUIP);
      }
      speculation.equipment.put(slot, item);
    }

    AdventureResult card = null;
    switch (item.getItemId()) {
      case ItemPool.HATSEAT -> configureCrown(speculation, item);
      case ItemPool.BUDDY_BJORN -> configureBjorn(speculation, item);
      case ItemPool.CARD_SLEEVE -> {
        card = this.bestCard != null ? this.bestCard : this.current.equipment.get(Slot.CARDSLEEVE);
        speculation.equipment.put(Slot.CARDSLEEVE, card);
      }
      case ItemPool.FOLDER_HOLDER, ItemPool.REPLICA_FOLDER_HOLDER -> {
        copyCurrent(speculation, Slot.FOLDER1);
        copyCurrent(speculation, Slot.FOLDER2);
        copyCurrent(speculation, Slot.FOLDER3);
        copyCurrent(speculation, Slot.FOLDER4);
        copyCurrent(speculation, Slot.FOLDER5);
      }
      case ItemPool.COWBOY_BOOTS -> {
        copyCurrent(speculation, Slot.BOOTSKIN);
        copyCurrent(speculation, Slot.BOOTSPUR);
      }
      default -> configureOther(speculation, item);
    }

    speculation.getScore();
    speculation.failed = false;
    return new Result(speculation, card);
  }

  private void configureCrown(MaximizerSpeculation speculation, CheckedItem item) {
    if (this.carriedFamiliars.lockedCrown() != null) {
      speculation.setEnthroned(this.carriedFamiliars.lockedCrown());
    } else if (this.carriedFamiliarsNeeded > 1) {
      item.automaticFlag = true;
      speculation.setEnthroned(this.carriedFamiliars.secondBest());
    } else {
      speculation.setEnthroned(this.carriedFamiliars.best());
    }
  }

  private void configureBjorn(MaximizerSpeculation speculation, CheckedItem item) {
    if (this.carriedFamiliars.lockedBjorn() != null) {
      speculation.setBjorned(this.carriedFamiliars.lockedBjorn());
    } else if (this.carriedFamiliarsNeeded > 1) {
      item.automaticFlag = true;
      speculation.setBjorned(this.carriedFamiliars.secondBest());
    } else {
      speculation.setBjorned(this.carriedFamiliars.best());
    }
  }

  private void configureOther(MaximizerSpeculation speculation, CheckedItem item) {
    if (EquipmentManager.isStickerWeapon(item)) {
      copyCurrent(speculation, Slot.STICKER1);
      copyCurrent(speculation, Slot.STICKER2);
      copyCurrent(speculation, Slot.STICKER3);
      return;
    }

    Modeable modeable = Modeable.find(item);
    if (modeable == null) {
      return;
    }
    String best = this.bestModes.getOrDefault(modeable, "");
    if (!best.isEmpty()) {
      speculation.setModeable(modeable, best);
    }
  }

  private void copyCurrent(MaximizerSpeculation speculation, Slot slot) {
    speculation.equipment.put(slot, this.current.equipment.get(slot));
  }
}
