package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.request.EquipmentRequest;

/**
 * Codpiece-specific plan/cache state and canonical gem search for a single {@link
 * MaximizerLoadout}. Reused across the outer equipment search so expensive per-item analysis (gem
 * modifiers, familiar contributions, late-adjustment prefixes) happens at most once.
 */
final class CodpieceSearchState {
  private static final ItemSlotGroup CODPIECE = ItemSlotGroup.ETERNITY_CODPIECE;

  private final MaximizerLoadout owner;
  private final Map<Integer, Modifiers> codpieceGemModifiers = new HashMap<>();
  private final Map<Integer, Boolean> safeLateCodpieceGems = new HashMap<>();
  private CodpiecePlan codpiecePlan;
  private CodpieceSearch search;

  CodpieceSearchState(MaximizerLoadout owner) {
    this.owner = owner;
  }

  private record CodpiecePlan(
      List<CheckedItem> gems, Modifiers[] gemModifiers, boolean[] familiarDependentGems) {}

  /** Modifiers built from the active search's cache, applied via {@code applyAdjustmentSuffix}. */
  record CalculatedModifiers(Modifiers modifiers, Modifiers fightMods) {}

  /** Reuses the expensive adjustment prefix while the search changes only late-safe gem effects. */
  private static final class LateCodpieceCache {
    private final Modifiers baseline;
    private final Modifiers fightMods;
    private final List<Slot> slots;
    private final Modifiers[] gemModifiers;
    private final boolean[] familiarDependentGems;
    private final Modifiers[] slotModifiers;
    private final int[] slotGemIndexes;
    private final Map<List<Integer>, KoLCharacter.AdjustmentPrefix> prefixes;
    private int familiarSlots;

    private LateCodpieceCache(
        Modifiers baseline,
        Modifiers fightMods,
        List<Slot> slots,
        Modifiers[] gemModifiers,
        boolean[] familiarDependentGems,
        Map<List<Integer>, KoLCharacter.AdjustmentPrefix> prefixes) {
      this.baseline = baseline;
      this.fightMods = fightMods;
      this.slots = slots;
      this.gemModifiers = gemModifiers;
      this.familiarDependentGems = familiarDependentGems;
      this.slotModifiers = new Modifiers[slots.size()];
      this.slotGemIndexes = new int[slots.size()];
      this.prefixes = prefixes;
    }

    private void select(int slotIndex, int gemIndex) {
      this.slotModifiers[slotIndex] = this.gemModifiers[gemIndex];
      this.slotGemIndexes[slotIndex] = gemIndex + 1;
      if (this.familiarDependentGems[gemIndex]) {
        this.familiarSlots++;
      }
    }

    private void deselect(int slotIndex) {
      int gemIndex = this.slotGemIndexes[slotIndex] - 1;
      if (gemIndex >= 0 && this.familiarDependentGems[gemIndex]) {
        this.familiarSlots--;
      }
      this.slotModifiers[slotIndex] = null;
      this.slotGemIndexes[slotIndex] = 0;
    }

    private KoLCharacter.AdjustmentPrefix getPrefix(
        Supplier<KoLCharacter.AdjustmentPrefix> factory) {
      List<Integer> familiarGemIndexes = new ArrayList<>();
      for (int encodedGemIndex : this.slotGemIndexes) {
        if (encodedGemIndex == 0) {
          continue;
        }
        int gemIndex = encodedGemIndex - 1;
        if (this.familiarDependentGems[gemIndex]) {
          familiarGemIndexes.add(gemIndex);
        }
      }
      return this.prefixes.computeIfAbsent(
          List.copyOf(familiarGemIndexes), ignored -> factory.get());
    }
  }

  /**
   * Builds the modifiers contributed by the active Codpiece search's cache, or {@code null} if no
   * search is currently active (in which case the owner should fall back to its default
   * calculation).
   */
  CalculatedModifiers calculateModifiers() {
    LateCodpieceCache cache = this.search == null ? null : this.search.cache;
    if (cache == null) {
      return null;
    }

    Modifiers newModifiers;
    Modifiers fightMods;
    if (cache.familiarSlots == 0) {
      newModifiers = new Modifiers(cache.baseline);
      for (Modifiers gemModifiers : cache.slotModifiers) {
        newModifiers.add(gemModifiers);
      }
      fightMods = cache.fightMods;
    } else {
      // Familiar-dependent gems must be present when familiar effects are calculated.
      var prefix = cache.getPrefix(this::primeFamiliarCodpiecePrefix);
      newModifiers = new Modifiers(prefix.modifiers());
      for (int encodedGemIndex : cache.slotGemIndexes) {
        if (encodedGemIndex == 0) {
          continue;
        }
        int gemIndex = encodedGemIndex - 1;
        if (!cache.familiarDependentGems[gemIndex]) {
          newModifiers.add(cache.gemModifiers[gemIndex]);
        }
      }
      fightMods = prefix.fightMods();
    }
    return new CalculatedModifiers(newModifiers, fightMods);
  }

  /** What the outer equipment search should do next about the Codpiece's gem slots. */
  enum Readiness {
    /** Not wearing a Codpiece: nothing further to search. */
    NOT_WEARING,
    /** Wearing a Codpiece, but the equipped gems already over-commit some item's count. */
    INFEASIBLE,
    /** Wearing a Codpiece with a feasible gem search to explore. */
    READY
  }

  record PreparedSearch(Readiness readiness, CodpieceSearch chooser) {}

  /**
   * Tail of {@code EquipmentSearchProblem}'s offhand choice once the weapon/offhand slots are
   * settled: figures out whether/how to search the Codpiece's gem slots. The caller owns restoring
   * equipment (and calling {@link #forget}) once it is done with the result.
   */
  PreparedSearch prepare(List<CheckedItem> possibles) {
    boolean wearingCodpiece =
        this.owner.equipment.values().stream()
            .anyMatch(item -> item != null && CODPIECE.isParent(item.getItemId()));
    if (!wearingCodpiece) {
      this.releaseCodpieceGemsNeededElsewhere();
      return new PreparedSearch(Readiness.NOT_WEARING, null);
    }

    List<Slot> codpieceSlots =
        CODPIECE.slots().stream().filter(Maximizer.evaluator()::slotEnabled).toList();
    for (Slot slot : codpieceSlots) {
      this.owner.equipment.put(slot, EquipmentRequest.UNEQUIP);
    }
    if (!this.hasEnoughCodpieceGems()) {
      return new PreparedSearch(Readiness.INFEASIBLE, null);
    }

    CodpiecePlan plan = this.getCodpiecePlan(possibles);
    List<CheckedItem> codpieceGems = plan.gems();
    LateCodpieceCache cache =
        this.canUseLateCodpieceCache(codpieceGems)
            ? this.primeLateCodpieceCache(plan, codpieceSlots)
            : null;
    CodpieceSearch search = new CodpieceSearch(codpieceGems, codpieceSlots, cache);
    this.search = search;
    this.owner.setUnscored();
    search.prepareIncrementalScore();
    return new PreparedSearch(Readiness.READY, search);
  }

  /** Releases the active {@link CodpieceSearch}, once the outer search leaves this branch. */
  void forget() {
    this.search = null;
  }

  private void releaseCodpieceGemsNeededElsewhere() {
    for (Slot slot : CODPIECE.slots()) {
      if (!Maximizer.evaluator().slotEnabled(slot)) {
        continue;
      }

      AdventureResult gem = this.owner.equipment.get(slot);
      if (gem == null || gem.equals(EquipmentRequest.UNEQUIP)) {
        continue;
      }

      CheckedItem equippedElsewhere =
          this.owner.equipment.entrySet().stream()
              .filter(entry -> !CODPIECE.slots().contains(entry.getKey()))
              .map(Map.Entry::getValue)
              .filter(gem::equals)
              .filter(CheckedItem.class::isInstance)
              .map(CheckedItem.class::cast)
              .findFirst()
              .orElse(null);
      if (equippedElsewhere == null) {
        continue;
      }

      long used = this.owner.equipment.values().stream().filter(gem::equals).count();
      if (used > equippedElsewhere.getAvailableCount()) {
        this.owner.equipment.put(slot, EquipmentRequest.UNEQUIP);
      }
    }
  }

  private boolean canUseLateCodpieceCache(List<CheckedItem> possibles) {
    for (CheckedItem possible : possibles) {
      if (!this.isSafeLateCodpieceGem(possible.getItemId())) {
        return false;
      }
    }

    return true;
  }

  private CodpiecePlan getCodpiecePlan(List<CheckedItem> possibles) {
    if (this.codpiecePlan == null) {
      this.codpiecePlan =
          this.createCodpiecePlan(
              Maximizer.evaluator()
                  .prioritizeCodpieceGems(
                      possibles.stream()
                          .filter(gem -> gem.getCount() > 0 && CODPIECE.accepts(gem.getItemId()))
                          .toList()));
    }
    return this.codpiecePlan;
  }

  private CodpiecePlan createCodpiecePlan(List<CheckedItem> gems) {
    Modifiers[] gemModifiers = new Modifiers[gems.size()];
    boolean[] familiarDependentGems = new boolean[gems.size()];
    for (int i = 0; i < gems.size(); i++) {
      gemModifiers[i] = this.getCodpieceGemModifiers(gems.get(i).getItemId());
      familiarDependentGems[i] = CodpieceModifierSafety.affectsFamiliarCalculation(gemModifiers[i]);
    }
    return new CodpiecePlan(gems, gemModifiers, familiarDependentGems);
  }

  private LateCodpieceCache primeLateCodpieceCache(CodpiecePlan plan, List<Slot> slots) {
    var mark = this.owner.mark();
    try {
      for (Slot slot : slots) {
        this.owner.equipment.put(slot, EquipmentRequest.UNEQUIP);
      }

      var prefix = this.owner.recalculateCodpiecePrefix(this.owner.equipment);
      Map<List<Integer>, KoLCharacter.AdjustmentPrefix> prefixes = new HashMap<>();
      prefixes.put(List.of(), prefix);
      return new LateCodpieceCache(
          prefix.modifiers(),
          prefix.fightMods(),
          slots,
          plan.gemModifiers(),
          plan.familiarDependentGems(),
          prefixes);
    } finally {
      this.owner.restore(mark);
    }
  }

  private KoLCharacter.AdjustmentPrefix primeFamiliarCodpiecePrefix() {
    LateCodpieceCache cache = this.search.cache;
    Map<Slot, AdventureResult> equipment = new EnumMap<>(this.owner.equipment);
    for (int slotIndex = 0; slotIndex < cache.slots.size(); slotIndex++) {
      int encodedGemIndex = cache.slotGemIndexes[slotIndex];
      if (encodedGemIndex == 0 || !cache.familiarDependentGems[encodedGemIndex - 1]) {
        equipment.put(cache.slots.get(slotIndex), EquipmentRequest.UNEQUIP);
      }
    }

    return this.owner.recalculateCodpiecePrefix(equipment);
  }

  private boolean isSafeLateCodpieceGem(int itemId) {
    return this.safeLateCodpieceGems.computeIfAbsent(
        itemId,
        id ->
            CodpieceModifierSafety.hasOnlySupportedLateCalculationModifiers(
                this.getCodpieceGemModifiers(id)));
  }

  private Modifiers getCodpieceGemModifiers(int itemId) {
    return this.codpieceGemModifiers.computeIfAbsent(itemId, CODPIECE::modifiers);
  }

  /** Gem-count validation: rejects branches that over-commit a gem beyond its available count. */
  boolean hasEnoughCodpieceGems() {
    boolean hasSlottedGem = false;
    for (Slot slot : CODPIECE.slots()) {
      AdventureResult item = this.owner.equipment.get(slot);
      if (item != null && !item.equals(EquipmentRequest.UNEQUIP)) {
        hasSlottedGem = true;
        break;
      }
    }
    if (!hasSlottedGem) {
      return true;
    }

    for (AdventureResult item : this.owner.equipment.values()) {
      if (!(item instanceof CheckedItem checked) || !CODPIECE.accepts(item.getItemId())) {
        continue;
      }

      int itemId = item.getItemId();
      int used = (int) this.countEquipped(itemId);
      if (used > checked.getAvailableCount()) {
        return false;
      }
    }
    return true;
  }

  private long countEquipped(int itemId) {
    return this.owner.equipment.values().stream()
        .filter(item -> item != null && item.getItemId() == itemId)
        .count();
  }

  /**
   * Enumerates canonical gem multisets, deduplicating by treating a branch as a multiset.
   *
   * <p>This is a plain state machine ({@link #complete}/{@link #choices}/{@link #choose}/{@link
   * #undo}), not an {@code AnytimeSearch.Problem} of its own: {@code EquipmentSearchProblem}
   * delegates to it directly as the tail of its own search tree.
   */
  final class CodpieceSearch {
    private final List<CheckedItem> gems;
    private final List<Slot> slots;
    private final int[] remaining;
    private final boolean[] required;
    private final int[] previousStarts;
    private final boolean[] satisfiedRequirement;
    private final LateCodpieceCache cache;
    private List<Evaluator.ScoreTerm> scoreTerms;
    private double[] baselineValues;
    private double[] selectedValues;
    private double[][] scoreContributions;
    private double[][][] scoreSuffixes;
    private boolean baselineRollover;
    private int start;
    private int slotIndex;
    private int remainingRequired;

    private CodpieceSearch(List<CheckedItem> gems, List<Slot> slots, LateCodpieceCache cache) {
      this.gems = gems;
      this.slots = slots;
      this.remaining = new int[gems.size()];
      this.required = new boolean[gems.size()];
      this.previousStarts = new int[slots.size()];
      this.satisfiedRequirement = new boolean[slots.size()];
      this.cache = cache;

      int requiredCount = 0;
      for (int i = 0; i < gems.size(); i++) {
        CheckedItem gem = gems.get(i);
        long used = countEquipped(gem.getItemId());
        this.remaining[i] = gem.getCount() - (int) used;
        if (gem.requiredFlag && used == 0) {
          this.required[i] = true;
          requiredCount++;
        }
      }
      this.remainingRequired = requiredCount;
    }

    private void prepareIncrementalScore() {
      Evaluator evaluator = Maximizer.evaluator();
      List<Evaluator.ScoreTerm> terms = evaluator.incrementalCodpieceScoreTerms();
      if (terms == null) return;

      double[][] contributions = new double[this.gems.size()][terms.size()];
      for (int i = 0; i < this.gems.size(); i++) {
        Modifiers modifiers = getCodpieceGemModifiers(this.gems.get(i).getItemId());
        if (modifiers != null && modifiers.hasString(StringModifier.ROLLOVER_EFFECT)) return;
        if (modifiers == null) continue;
        for (int j = 0; j < terms.size(); j++) {
          contributions[i][j] = modifiers.getDouble(terms.get(j).modifier());
        }
      }

      Modifiers baseline = owner.calculate();
      this.baselineValues = new double[terms.size()];
      this.selectedValues = new double[terms.size()];
      for (int i = 0; i < terms.size(); i++) {
        this.baselineValues[i] = baseline.getDouble(terms.get(i).modifier());
      }
      this.baselineRollover = baseline.hasString(StringModifier.ROLLOVER_EFFECT);
      this.scoreTerms = terms;
      this.scoreContributions = contributions;
      this.scoreSuffixes = this.buildScoreSuffixes();
    }

    boolean currentCanBeat(SolutionQuality incumbent) {
      if (this.scoreTerms == null || incumbent == null || !incumbent.feasible()) return true;
      return this.score(0) >= incumbent.score();
    }

    boolean canBeat(SolutionQuality incumbent) {
      if (this.scoreTerms == null || !incumbent.feasible()) return true;
      return this.score(this.slots.size() - this.slotIndex) >= incumbent.score();
    }

    private double score(int slotsLeft) {
      double score = this.baselineRollover ? 0.01f : 0.0;
      for (int i = 0; i < this.scoreTerms.size(); i++) {
        Evaluator.ScoreTerm term = this.scoreTerms.get(i);
        double suffix = slotsLeft == 0 ? 0.0 : this.scoreSuffixes[i][this.start][slotsLeft];
        double value =
            Math.min(this.baselineValues[i] + this.selectedValues[i] + suffix, term.max());
        score = Math.nextUp(score + Math.nextUp(term.weight() * value));
      }
      return score;
    }

    private double[][][] buildScoreSuffixes() {
      int candidates = this.gems.size();
      int slots = this.slots.size();
      double[][][] suffixes = new double[this.scoreTerms.size()][candidates + 1][slots + 1];
      for (int term = 0; term < this.scoreTerms.size(); term++) {
        boolean maximize = this.scoreTerms.get(term).weight() >= 0.0;
        for (int candidate = candidates - 1; candidate >= 0; candidate--) {
          double contribution = this.scoreContributions[candidate][term];
          for (int count = 1; count <= slots; count++) {
            double best = suffixes[term][candidate + 1][count];
            for (int copies = 1; copies <= Math.min(this.remaining[candidate], count); copies++) {
              double value = copies * contribution + suffixes[term][candidate + 1][count - copies];
              value = maximize ? Math.nextUp(value) : Math.nextDown(value);
              best = maximize ? Math.max(best, value) : Math.min(best, value);
            }
            suffixes[term][candidate][count] = best;
          }
        }
      }
      return suffixes;
    }

    /** Whether every required gem has been placed (a precondition for a valid candidate). */
    boolean requirementsSatisfied() {
      return this.remainingRequired == 0;
    }

    boolean complete() {
      return this.slotIndex == this.slots.size()
          || this.remainingRequired > this.slots.size() - this.slotIndex;
    }

    List<Integer> choices() {
      List<Integer> choices = new ArrayList<>();
      int firstRequired = -1;
      for (int i = this.start; i < this.required.length; i++) {
        if (this.required[i]) {
          firstRequired = i;
          break;
        }
      }

      for (int i = this.start; i < this.gems.size(); i++) {
        if (firstRequired != -1 && i > firstRequired) {
          break;
        }
        if (this.remaining[i] > 0) choices.add(i);
      }
      return choices;
    }

    boolean choose(Integer choice) {
      int depth = this.slotIndex;
      this.previousStarts[depth] = this.start;
      this.satisfiedRequirement[depth] = this.required[choice];
      this.remaining[choice]--;
      this.required[choice] = false;
      owner.equipment.put(this.slots.get(depth), this.gems.get(choice));
      if (this.cache != null) {
        this.cache.select(depth, choice);
      }
      this.start = choice;
      this.slotIndex++;
      if (this.satisfiedRequirement[depth]) this.remainingRequired--;
      if (this.scoreTerms != null) {
        for (int i = 0; i < this.scoreTerms.size(); i++) {
          this.selectedValues[i] += this.scoreContributions[choice][i];
        }
      }
      owner.setUnscored();
      return true;
    }

    void undo(Integer choice) {
      this.slotIndex--;
      int depth = this.slotIndex;
      this.start = this.previousStarts[depth];
      if (this.satisfiedRequirement[depth]) this.remainingRequired++;
      owner.equipment.put(this.slots.get(depth), EquipmentRequest.UNEQUIP);
      if (this.cache != null) {
        this.cache.deselect(depth);
      }
      this.required[choice] = this.satisfiedRequirement[depth];
      this.remaining[choice]++;
      if (this.scoreTerms != null) {
        for (int i = 0; i < this.scoreTerms.size(); i++) {
          this.selectedValues[i] -= this.scoreContributions[choice][i];
        }
      }
      owner.setUnscored();
    }
  }
}
