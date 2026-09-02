package net.sourceforge.kolmafia.maximizer;

import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;

final class FamiliarEquipmentCompiler {
  record Result(CheckedItem item, boolean rejected) {}

  private final Evaluator evaluator;
  private final List<FamiliarData> familiars;
  private final SlotList<CheckedItem> catalog;
  private final SlotList<CheckedItem> ranked;
  private final EquipScope equipScope;
  private final long maxPrice;
  private final PriceLevel priceLevel;
  private final double nullScore;

  FamiliarEquipmentCompiler(
      Evaluator evaluator,
      List<FamiliarData> familiars,
      SlotList<CheckedItem> catalog,
      SlotList<CheckedItem> ranked,
      EquipScope equipScope,
      long maxPrice,
      PriceLevel priceLevel,
      double nullScore) {
    this.evaluator = evaluator;
    this.familiars = familiars;
    this.catalog = catalog;
    this.ranked = ranked;
    this.equipScope = equipScope;
    this.maxPrice = maxPrice;
    this.priceLevel = priceLevel;
    this.nullScore = nullScore;
  }

  Result compile(int itemId, AdventureResult itemResult, Slot slot, Modeable modeable) {
    CheckedItem item = null;
    FamiliarData activeFamiliar = KoLCharacter.getFamiliar();
    boolean activeCanEquip = activeFamiliar.canEquip(itemResult);

    if (activeCanEquip && slot != Slot.FAMILIAR) {
      Modifiers modifiers = familiarModifiers(activeFamiliar, itemResult, slot, true);
      item = checkedItem(itemId);

      Evaluator.Constraint constraint = this.evaluator.checkConstraints(modifiers);
      if (constraint == Evaluator.Constraint.VIOLATES) {
        return new Result(item, true);
      }
      markAutomatic(item, constraint, modeable);
      addCandidate(
          this.catalog.get(Slot.FAMILIAR), this.ranked.get(Slot.FAMILIAR), item, modifiers);
    }

    for (int index = this.familiars.size() - 1; index >= 0; index--) {
      FamiliarData familiar = this.familiars.get(index);
      if (!familiar.canEquip(itemResult)) {
        continue;
      }

      Modifiers modifiers = familiarModifiers(familiar, itemResult, slot, false);
      if (item == null) {
        item = checkedItem(itemId);
      }
      Evaluator.Constraint constraint = this.evaluator.checkConstraints(modifiers);
      if (constraint == Evaluator.Constraint.VIOLATES) {
        continue;
      }
      markAutomatic(item, constraint, modeable);
      addCandidate(
          this.catalog.getFamiliar(index), this.ranked.getFamiliar(index), item, modifiers);
    }

    return new Result(item, false);
  }

  private Modifiers familiarModifiers(
      FamiliarData familiar, AdventureResult item, Slot slot, boolean active) {
    int familiarId = familiar.getId();
    if ((familiarId == FamiliarPool.HATRACK && slot == Slot.HAT)
        || (familiarId == FamiliarPool.SCARECROW && slot == Slot.PANTS)) {
      Modifiers modifiers = new Modifiers();
      modifiers.applyFamiliarModifiers(familiar, item);
      return modifiers;
    }

    Modifiers modifiers =
        active
            ? ModifierDatabase.getItemModifiersInFamiliarSlot(item.getItemId())
            : ModifierDatabase.getItemModifiers(item.getItemId());
    if (active
        && familiarId == FamiliarPool.LEFT_HAND
        && (item.getItemId() == ItemPool.KOL_COL_13_SNOWGLOBE
            || item.getItemId() == ItemPool.GLOWING_ESCA)) {
      modifiers = null;
    }
    return modifiers == null ? new Modifiers() : modifiers;
  }

  private CheckedItem checkedItem(int itemId) {
    return new CheckedItem(itemId, this.equipScope, this.maxPrice, this.priceLevel);
  }

  private void markAutomatic(CheckedItem item, Evaluator.Constraint constraint, Modeable modeable) {
    if (constraint == Evaluator.Constraint.MEETS || modeable != null) {
      item.automaticFlag = true;
    }
  }

  private void addCandidate(
      List<CheckedItem> catalog, List<CheckedItem> ranked, CheckedItem item, Modifiers modifiers) {
    if (item.getCount() == 0) {
      return;
    }

    catalog.add(item);
    if (item.automaticFlag
        || this.evaluator.requiresEquipment(item)
        || this.evaluator.getScore(modifiers, Map.of(Slot.FAMILIAR, item), Map.of())
                - this.nullScore
            > 0.0) {
      ranked.add(item);
    }
  }
}
