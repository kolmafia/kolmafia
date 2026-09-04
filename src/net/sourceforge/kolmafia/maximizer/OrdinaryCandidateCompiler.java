package net.sourceforge.kolmafia.maximizer;

import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;

/**
 * Discovers ordinary equipment candidates and builds full and ranked catalogs.
 *
 * <p>Database iteration lives here; per-item legality and relevance belong to {@link
 * OrdinaryCandidateEvaluator}, slot placement to {@link EquipmentCandidateSlotter}, and familiar
 * equipment to {@link FamiliarEquipmentCompiler}. The full catalog is retained for exhaustive
 * verification even when interactive search uses a shortlist.
 */
final class OrdinaryCandidateCompiler {
  record Options(
      List<FamiliarData> familiars,
      Map<Slot, Integer> slots,
      Map<Modeable, String> forcedModeables,
      boolean current,
      int clownosity,
      int raveosity,
      int surgeonosity,
      int stinkycheese,
      EquipmentCandidateSlotter.Requirements placement,
      boolean itemDropUseful,
      boolean experienceUseful,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel,
      double nullScore) {}

  record Result(
      SlotList<CheckedItem> catalog,
      SlotList<CheckedItem> ranked,
      int carriedFamiliarsNeeded,
      boolean cardNeeded,
      Map<Modeable, Boolean> modeablesNeeded) {}

  private final Evaluator evaluator;
  private final CharacterSnapshot character;
  private final EquipmentSetEvaluator setEvaluator;
  private final Options options;

  OrdinaryCandidateCompiler(
      Evaluator evaluator,
      CharacterSnapshot character,
      EquipmentSetEvaluator setEvaluator,
      Options options) {
    this.evaluator = evaluator;
    this.character = character;
    this.setEvaluator = setEvaluator;
    this.options = options;
  }

  Result compile() throws MaximizerInterruptedException {
    SlotList<CheckedItem> catalog = new SlotList<>(this.options.familiars().size());
    SlotList<CheckedItem> ranked = new SlotList<>(this.options.familiars().size());
    OrdinaryCandidateEvaluator candidateEvaluator =
        new OrdinaryCandidateEvaluator(
            this.evaluator,
            this.setEvaluator,
            new OrdinaryCandidateEvaluator.Requirements(
                this.options.slots(),
                this.options.forcedModeables(),
                this.options.current(),
                this.options.clownosity(),
                this.options.raveosity(),
                this.options.surgeonosity(),
                this.options.stinkycheese()),
            this.options.nullScore());
    FamiliarEquipmentCompiler familiarCompiler =
        new FamiliarEquipmentCompiler(
            this.evaluator,
            this.options.familiars(),
            catalog,
            ranked,
            this.options.equipScope(),
            this.options.maxPrice(),
            this.options.priceLevel(),
            this.options.nullScore());
    EquipmentCandidateSlotter slotter =
        new EquipmentCandidateSlotter(
            this.options.placement(),
            candidateEvaluator.hoboPowerUseful(),
            this.options.itemDropUseful(),
            this.options.experienceUseful(),
            this.options.maxPrice(),
            this.options.priceLevel());

    int itemId = 0;
    while ((itemId = EquipmentDatabase.nextEquipmentItemId(itemId)) != -1) {
      Slot slot = EquipmentManager.itemIdToEquipmentType(itemId);
      if (slot == Slot.NONE) {
        continue;
      }

      AdventureResult itemResult = ItemPool.get(itemId, 1);
      String name = itemResult.getName();
      if (this.evaluator.excludesEquipment(itemResult)
          || this.character.resourcesExceeded(this.character.resourceUsage(name))) {
        continue;
      }

      Modeable modeable = Modeable.find(itemId);
      boolean familiarCanEquip = KoLCharacter.getFamiliar().canEquip(itemResult);
      var familiarResult = familiarCompiler.compile(itemId, itemResult, slot, modeable);
      if (familiarResult.rejected()) {
        continue;
      }

      CheckedItem item = familiarResult.item();
      if (!EquipmentManager.canEquip(itemId) && !KoLCharacter.hasEquipped(itemId)) {
        continue;
      }
      if (item == null) {
        item =
            new CheckedItem(
                itemId,
                this.options.equipScope(),
                this.options.maxPrice(),
                this.options.priceLevel());
      }
      if (item.getCount() == 0) {
        continue;
      }
      if (!StandardRequest.isAllowed(RestrictedItemType.ITEMS, item.getName())) {
        continue;
      }

      var placement = slotter.place(itemId, name, slot, item, familiarCanEquip);
      if (!placement.accepted()) {
        continue;
      }
      slot = placement.slot();
      Slot auxiliarySlot = placement.auxiliarySlot();
      if (!placement.skipScoring()) {
        switch (candidateEvaluator.evaluate(itemId, item, modeable)) {
          case REJECT:
            continue;
          case CATALOG_ONLY:
            addCandidate(catalog, slot, auxiliarySlot, item);
            continue;
          case RANKED:
            break;
        }
      }
      addCandidate(catalog, slot, auxiliarySlot, item);
      addCandidate(ranked, slot, auxiliarySlot, item);
    }

    return new Result(
        catalog,
        ranked,
        candidateEvaluator.carriedFamiliarsNeeded(),
        candidateEvaluator.cardNeeded(),
        candidateEvaluator.modeablesNeeded());
  }

  private static void addCandidate(
      SlotList<CheckedItem> candidates, Slot slot, Slot auxiliarySlot, CheckedItem item) {
    if (slot != Slot.NONE) {
      candidates.get(slot).add(item);
    }
    if (auxiliarySlot != Slot.NONE) {
      candidates.get(auxiliarySlot).add(item);
    }
  }
}
