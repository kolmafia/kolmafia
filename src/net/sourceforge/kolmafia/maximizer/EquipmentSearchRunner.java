package net.sourceforge.kolmafia.maximizer;

import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.CLOWNOSITY;
import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.DUMP;
import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.RAVEOSITY;
import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.STINKYCHEESE;
import static net.sourceforge.kolmafia.maximizer.MaximizerTermRegistry.IntegerSetting.SURGEONOSITY;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
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

  static void compileAndRun(
      Evaluator evaluator,
      MaximizerTermRegistry terms,
      CodpieceEvaluator codpieceEvaluator,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel,
      boolean exhaustive)
      throws MaximizerInterruptedException {
    CharacterSnapshot character = Maximizer.character();
    double nullScore = evaluator.getScore(new Modifiers());
    double nullTiebreaker = evaluator.getTiebreaker(new Modifiers());
    var setEvaluator =
        new EquipmentSetEvaluator(
            evaluator,
            terms.posOutfits(),
            terms.negOutfits(),
            equipScope,
            maxPrice,
            priceLevel,
            terms.integer(DUMP),
            nullScore);
    var ordinaryCandidates =
        new OrdinaryCandidateCompiler(
                evaluator,
                character,
                setEvaluator,
                new OrdinaryCandidateCompiler.Options(
                    terms.familiars(),
                    terms.slots(),
                    terms.forcedModeables(),
                    terms.currentOnly(),
                    terms.integer(CLOWNOSITY),
                    terms.integer(RAVEOSITY),
                    terms.integer(SURGEONOSITY),
                    terms.integer(STINKYCHEESE),
                    terms.requirements(),
                    terms.itemDropUseful(),
                    terms.experienceUseful(),
                    equipScope,
                    maxPrice,
                    priceLevel,
                    nullScore))
            .compile();
    SlotList<CheckedItem> catalog = ordinaryCandidates.catalog();
    SlotList<CheckedItem> ranked = ordinaryCandidates.ranked();

    var codpieceCandidates =
        codpieceEvaluator.compileCandidates(
            equipScope, maxPrice, priceLevel, nullScore, nullTiebreaker);
    catalog.get(Slot.CODPIECE1).addAll(codpieceCandidates.catalog());
    ranked.get(Slot.CODPIECE1).addAll(codpieceCandidates.ranked());
    boolean codpieceCanExpandAccessoryPool =
        codpieceEvaluator.prepareAccessoryCandidates(
            codpieceCandidates.ranked(),
            ranked.get(Slot.ACCESSORY1),
            ItemSlotGroup.ETERNITY_CODPIECE.slots().stream().anyMatch(evaluator::slotEnabled));

    var carriedFamiliars =
        CarriedFamiliarSelector.select(
            ordinaryCandidates.carriedFamiliarsNeeded(),
            terms.slots().getOrDefault(Slot.CROWNOFTHRONES, 0) < 0,
            terms.slots().getOrDefault(Slot.BUDDYBJORN, 0) < 0,
            character,
            equipScope,
            maxPrice,
            priceLevel);
    CheckedItem bestCard =
        CardSleeveSelector.select(
            ordinaryCandidates.cardNeeded(), equipScope, maxPrice, priceLevel);
    Map<Modeable, String> bestModes =
        ModeableSelector.select(
            ordinaryCandidates.modeablesNeeded(),
            terms.forcedModeables(),
            equipScope,
            maxPrice,
            priceLevel);
    var speculationCompilation =
        new CandidateSpeculationFactory(
                ordinaryCandidates.carriedFamiliarsNeeded(), carriedFamiliars, bestCard, bestModes)
            .compile(ranked, catalog, terms.familiars(), equipScope, maxPrice, priceLevel);
    var speculations = speculationCompilation.speculations();
    setEvaluator.evaluate(speculations);

    var shortlist =
        new CandidateShortlistCompiler(
                terms.familiars(), character, equipScope, maxPrice, priceLevel, terms.integer(DUMP))
            .compile(ranked, speculations, codpieceCanExpandAccessoryPool);
    Maximizer.recordCandidateCounts(
        speculationCompilation.catalogCount(), shortlist.candidateCount());

    new EquipmentSearchRunner(
            terms.familiars(),
            carriedFamiliars.candidates(),
            setEvaluator.usefulOutfits(),
            setEvaluator.outfitPieces(),
            new Options(
                terms.slots(),
                bestModes,
                speculationCompilation.card(),
                carriedFamiliars.lockedCrown(),
                carriedFamiliars.lockedBjorn(),
                maxPrice,
                priceLevel,
                exhaustive))
        .run(shortlist.candidates(), catalog);
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
    var problem =
        new EquipmentSearchProblem(
            baseline,
            this.familiars,
            this.carriedFamiliars,
            this.usefulOutfits,
            this.outfitPieces,
            candidates,
            this.options.card(),
            this.options.crownFamiliar(),
            this.options.bjornFamiliar(),
            Preferences.getBoolean("maximizerFoldables"));
    AnytimeSearch.maximize(
        problem,
        new AnytimeSearch.Candidate<>(Maximizer.best().quality(), null),
        Maximizer::keepSearching);
  }
}
