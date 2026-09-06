package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.InventoryManager;

/**
 * Eternity Codpiece gem support for the Modifier Maximizer.
 *
 * <p>Gems are inserted into the codpiece rather than equipped, so they have no ordinary equipment
 * slot and never reach the per-slot candidate loops in {@link Evaluator}. Candidates are discovered
 * once per maximization; the combination itself is searched separately for every fully decided
 * equipment branch, because a gem is only worth anything while the codpiece is worn and its value
 * can depend on that branch's other equipment and familiar.
 *
 * <p>The search writes only to the speculation's own equipment map. It never changes
 * EquipmentManager, and never posts a request.
 */
final class CodpieceMaximizer {
  /**
   * Modifiers whose score contribution is exactly the sum of each source's own raw value: {@link
   * Evaluator#getScore} reads them directly rather than deriving them, and Modifiers.addDouble
   * accumulates them without a cap, a floor, a conditional, or an "only the best applies" rule.
   * Anything outside this set makes the suffix bound below unusable, and the search falls back to
   * exact enumeration.
   */
  private static final EnumSet<DoubleModifier> SUMMABLE_MODIFIERS =
      EnumSet.of(
          DoubleModifier.ADVENTURES,
          DoubleModifier.BOOZEDROP,
          DoubleModifier.BUGBEAR_DAMAGE,
          DoubleModifier.CANDYDROP,
          DoubleModifier.DAMAGE_ABSORPTION,
          DoubleModifier.DAMAGE_REDUCTION,
          DoubleModifier.ENCHANTMENT_COUNT,
          DoubleModifier.FAMILIAR_DAMAGE,
          DoubleModifier.FAMILIAR_EXP,
          DoubleModifier.FISHING_SKILL,
          DoubleModifier.FOODDROP,
          DoubleModifier.GHOST_DAMAGE,
          DoubleModifier.HP_REGEN_MAX,
          DoubleModifier.HP_REGEN_MIN,
          DoubleModifier.MONSTER_LEVEL,
          DoubleModifier.MOX_PCT,
          DoubleModifier.MP_PCT,
          DoubleModifier.MP_REGEN_MAX,
          DoubleModifier.MP_REGEN_MIN,
          DoubleModifier.MUS_PCT,
          DoubleModifier.MYS_PCT,
          DoubleModifier.PICKPOCKET_CHANCE,
          DoubleModifier.POOL_SKILL,
          DoubleModifier.PVP_FIGHTS,
          DoubleModifier.SEAL_DAMAGE,
          DoubleModifier.VAMPIRE_DAMAGE,
          DoubleModifier.WEREWOLF_DAMAGE,
          DoubleModifier.ZOMBIE_DAMAGE);

  private record Gem(CheckedItem item, Modifiers mods, double delta, int count) {}

  private final Evaluator evaluator;

  private final List<Gem> catalogue = new ArrayList<>();

  /** Available copies, including gems already installed in a retrievable codpiece. */
  private final Map<Integer, Integer> countsByItemId = new HashMap<>();

  private int combinationsEvaluated;

  private List<Gem> candidates = List.of();
  private int[] available = new int[0];
  private List<Slot> freeSlots = List.of();
  private ScoreBound bound;

  CodpieceMaximizer(Evaluator evaluator) {
    this.evaluator = evaluator;
  }

  int getCombinationsEvaluated() {
    return this.combinationsEvaluated;
  }

  boolean hasCandidates() {
    return !this.catalogue.isEmpty();
  }

  static int copiesInGemSlots(Map<Slot, AdventureResult> equipment, AdventureResult item) {
    int count = 0;
    for (Slot slot : SlotSet.CODPIECE_SLOTS) {
      AdventureResult gem = equipment.get(slot);
      if (gem != null && gem.getItemId() == item.getItemId()) {
        count++;
      }
    }
    return count;
  }

  static boolean hasEquippedGem(Map<Slot, AdventureResult> equipment, AdventureResult item) {
    return isCodpieceWorn(equipment) && copiesInGemSlots(equipment, item) > 0;
  }

  /**
   * Finds every gem the character could use. A gem is judged by the same isolated delta as any
   * other candidate: one that cannot help the requested expression is dropped, one already in the
   * codpiece is kept so it can be left alone, and one the user demanded is kept so the requirement
   * can be met or reported as impossible.
   */
  void discoverGems(EquipScope equipScope, int maxPrice, PriceLevel priceLevel, double nullScore)
      throws MaximizerInterruptedException {
    for (var entry : ModifierDatabase.getAllModifiersOfType(ModifierType.ETERNITY_CODPIECE)) {
      if (!entry.getKey().isInt()) continue;
      int itemId = entry.getKey().getIntValue();
      AdventureResult gem = ItemPool.get(itemId, 1);
      if (this.evaluator.isForbidden(gem)) continue;
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ETERNITY_CODPIECE, itemId);
      if (mods == null) continue;

      int slotted = liveCopiesInGemSlots(gem);
      boolean required = this.evaluator.isRequired(gem);
      double delta = this.evaluator.getScore(mods, Map.of(Slot.NONE, gem), Map.of()) - nullScore;
      Evaluator.Constraint constraint = this.evaluator.checkConstraints(mods);
      boolean contributesToMinimum = this.evaluator.contributesToMinimum(mods);
      boolean contributesToCappedScore = this.evaluator.contributesToCappedScore(mods);
      if (constraint == Evaluator.Constraint.VIOLATES) continue;
      if (delta < 0.0
          && !required
          && constraint == Evaluator.Constraint.IRRELEVANT
          && !contributesToMinimum
          && !contributesToCappedScore
          && !this.contributesToInteractingScore(mods)) {
        continue;
      }
      if (delta == 0.0
          && slotted == 0
          && !required
          && constraint == Evaluator.Constraint.IRRELEVANT
          && !contributesToMinimum
          && !contributesToCappedScore
          && !this.contributesToInteractingScore(mods)
          && !hasNonNumericModifiers(mods)) {
        continue;
      }

      CheckedItem item = new CheckedItem(itemId, equipScope, maxPrice, priceLevel, true);
      item.validate(maxPrice, priceLevel);
      int count = item.getCount();
      if (!InventoryManager.equippedOrInInventory(ItemPool.THE_ETERNITY_CODPIECE)) {
        count += slotted;
      }
      if (count == 0) continue;
      this.countsByItemId.put(itemId, count);
      this.catalogue.add(new Gem(item, mods, delta, count));
    }
    this.catalogue.sort(
        Comparator.comparingDouble(Gem::delta)
            .reversed()
            .thenComparingInt(gem -> gem.item().getItemId()));
  }

  private boolean contributesToInteractingScore(Modifiers mods) {
    if (this.evaluator.contributesToNonlinearScore(mods, SUMMABLE_MODIFIERS)) return true;
    return this.evaluator.getActiveScoreModifiers().stream()
        .anyMatch(
            term ->
                term.modifier() == DoubleModifier.PRISMATIC_DAMAGE
                    && (mods.getDouble(DoubleModifier.HOT_DAMAGE) != 0.0
                        || mods.getDouble(DoubleModifier.COLD_DAMAGE) != 0.0
                        || mods.getDouble(DoubleModifier.SPOOKY_DAMAGE) != 0.0
                        || mods.getDouble(DoubleModifier.STENCH_DAMAGE) != 0.0
                        || mods.getDouble(DoubleModifier.SLEAZE_DAMAGE) != 0.0));
  }

  /**
   * Chooses gems for one fully decided equipment branch and scores every combination it reaches.
   * Exactly one {@link MaximizerSpeculation#checkBest} call happens for a branch with no gem
   * decisions to make, so this replaces, rather than supplements, the leaf scoring it stands in
   * for.
   */
  void search(MaximizerSpeculation spec) throws MaximizerInterruptedException {
    this.freeSlots = new ArrayList<>();
    for (Slot slot : SlotSet.CODPIECE_SLOTS) {
      if (spec.equipment.get(slot) == null) {
        this.freeSlots.add(slot);
      }
    }
    if (this.freeSlots.isEmpty()) {
      spec.checkBest();
      return;
    }

    if (!isCodpieceWorn(spec.equipment)) {
      // Gems only enchant while the codpiece is worn, and a codpiece that is not worn keeps
      // whatever it already holds, unless the ordinary equipment search has committed that copy
      // elsewhere.
      for (Slot slot : this.freeSlots) {
        AdventureResult gem = EquipmentManager.getEquipment(slot);
        int available = this.availableCopies(gem);
        spec.equipment.put(
            slot,
            gem != null
                    && gem != EquipmentRequest.UNEQUIP
                    && countInEquipment(spec.equipment, gem) < available
                ? gem
                : EquipmentRequest.UNEQUIP);
      }
      spec.checkBest();
      return;
    }

    for (Slot slot : this.freeSlots) {
      spec.equipment.put(slot, EquipmentRequest.UNEQUIP);
    }
    int filled = this.selectCandidates(spec);
    this.bound = this.buildBound(spec, this.freeSlots.size() - filled);
    this.searchGems(spec, filled, 0);
  }

  /**
   * Works out which gems this branch can still use and how many copies of each, then places any
   * required gem up front. Copies this branch has already committed elsewhere, including to an
   * excluded gem slot, are subtracted, so one physical gem is never used twice.
   *
   * @return how many free gem slots were filled with required gems
   */
  private int selectCandidates(MaximizerSpeculation spec) {
    this.candidates = new ArrayList<>();
    List<Integer> counts = new ArrayList<>();
    int filled = 0;
    for (Gem gem : this.catalogue) {
      AdventureResult item = gem.item();
      int count = gem.count() - countInEquipment(spec.equipment, item);
      if (count <= 0) continue;

      // Placing a required gem first is only a shortcut. One that does not fit still fails the
      // speculation, through Evaluator.checkEquipment, on every leaf this branch reaches.
      if (filled < this.freeSlots.size()
          && this.evaluator.isRequired(item)
          && !KoLCharacter.hasEquipped(spec.equipment, item)
          && copiesInGemSlots(spec.equipment, item) == 0) {
        spec.equipment.put(this.claimSlot(item, filled++), item);
        count--;
      }
      if (count <= 0) continue;

      this.candidates.add(gem);
      counts.add(Math.min(count, this.freeSlots.size()));
    }
    this.available = counts.stream().mapToInt(Integer::intValue).toArray();
    return filled;
  }

  /**
   * Enumerates gem multisets in canonical order: each candidate may fill progressively more of the
   * remaining slots before the search moves on, so no combination is reached twice. Every node,
   * including the one that adds no further gem, is scored by the same {@link
   * MaximizerSpeculation#checkBest} the rest of the equipment search uses, so the answer is exact
   * whether or not the bound is available.
   */
  private void searchGems(MaximizerSpeculation spec, int filled, int start)
      throws MaximizerInterruptedException {
    this.combinationsEvaluated++;
    spec.checkBest();

    int slotsLeft = this.freeSlots.size() - filled;
    if (slotsLeft == 0) return;

    var mark = spec.mark();
    for (int pos = start; pos < this.candidates.size(); pos++) {
      if (this.bound != null
          && !Maximizer.best.failed
          && this.bound.upperBound(pos, slotsLeft) < Maximizer.best.getScore()) {
        // Nothing candidates[pos..] can add reaches the incumbent, and later candidates are worth
        // no more than this one, so the rest of this node is dead.
        break;
      }
      AdventureResult item = this.candidates.get(pos).item();
      int copies = Math.min(this.available[pos], slotsLeft);
      for (int used = 1; used <= copies; used++) {
        spec.equipment.put(this.claimSlot(item, filled + used - 1), item);
        if (this.bound != null) this.bound.select(pos, 1);
        this.searchGems(spec, filled + used, pos + 1);
      }
      if (this.bound != null) this.bound.select(pos, -copies);
      spec.restore(mark);
    }
  }

  /**
   * Picks which free gem slot the next copy goes in, preferring one that already holds that gem so
   * that keeping a gem where it is reads as leaving equipment alone rather than as a removal plus
   * an insertion. Only slots the search has not assigned yet are considered, and no score depends
   * on which gem slot a gem occupies.
   */
  private Slot claimSlot(AdventureResult item, int index) {
    for (int i = index; i < this.freeSlots.size(); i++) {
      AdventureResult live = EquipmentManager.getEquipment(this.freeSlots.get(i));
      if (live != null && live.getItemId() == item.getItemId()) {
        Slot preferred = this.freeSlots.get(i);
        this.freeSlots.set(i, this.freeSlots.get(index));
        this.freeSlots.set(index, preferred);
        break;
      }
    }
    return this.freeSlots.get(index);
  }

  private ScoreBound buildBound(MaximizerSpeculation spec, int slotsLeft) {
    if (slotsLeft == 0 || this.candidates.isEmpty()) return null;
    if (!this.evaluator.scoreIsWeightedSumOfTerms()) return null;
    List<Evaluator.ScoreModifier> terms = this.evaluator.getActiveScoreModifiers();
    for (Evaluator.ScoreModifier term : terms) {
      if (!SUMMABLE_MODIFIERS.contains(term.modifier())) return null;
      if (term.modifier() == DoubleModifier.ADVENTURES
          && !KoLCharacter.canGainRolloverAdventures()) {
        return null;
      }
    }
    for (Gem gem : this.candidates) {
      if (!contributesLinearly(gem.mods())) return null;
    }
    spec.setUnscored();
    return new ScoreBound(terms, spec.calculate(), this.candidates, this.available, slotsLeft);
  }

  /**
   * Whether a gem's contribution to a summable modifier is exactly the raw value in its own
   * enchantments. A rollover effect is scored directly by {@link Evaluator#getScore} rather than
   * through a modifier, an intrinsic effect brings in modifiers that are not listed on the gem, and
   * a bitmap such as Brimstone feeds a non-linear late calculation.
   */
  private static boolean contributesLinearly(Modifiers mods) {
    return !hasNonNumericModifiers(mods);
  }

  private static boolean hasNonNumericModifiers(Modifiers mods) {
    return EnumSet.allOf(BitmapModifier.class).stream()
            .anyMatch(modifier -> mods.getRawBitmap(modifier) != 0)
        || EnumSet.allOf(BooleanModifier.class).stream().anyMatch(mods::getBoolean)
        || EnumSet.allOf(StringModifier.class).stream()
            .filter(
                modifier ->
                    modifier != StringModifier.MODIFIERS
                        && modifier != StringModifier.EVALUATED_MODIFIERS)
            .anyMatch(
                modifier ->
                    modifier.isMultiple()
                        ? !mods.getStrings(modifier).isEmpty()
                        : !mods.getString(modifier).isEmpty());
  }

  private static boolean isCodpieceWorn(Map<Slot, AdventureResult> equipment) {
    for (Slot slot : SlotSet.SLOTS) {
      AdventureResult item = equipment.get(slot);
      if (item != null && item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE) {
        return true;
      }
    }
    return false;
  }

  private static int countInEquipment(Map<Slot, AdventureResult> equipment, AdventureResult item) {
    int count = 0;
    for (AdventureResult equipped : equipment.values()) {
      if (equipped != null && equipped.getItemId() == item.getItemId()) {
        count++;
      }
    }
    return count;
  }

  private static int liveCopiesInGemSlots(AdventureResult item) {
    int count = 0;
    for (Slot slot : SlotSet.CODPIECE_SLOTS) {
      AdventureResult gem = EquipmentManager.getEquipment(slot);
      if (gem != null && gem.getItemId() == item.getItemId()) {
        count++;
      }
    }
    return count;
  }

  private int availableCopies(AdventureResult item) {
    if (item == null || item == EquipmentRequest.UNEQUIP) return 0;
    Integer count = this.countsByItemId.get(item.getItemId());
    if (count != null) return count;
    count =
        ItemDatabase.isAllowed(item)
            ? InventoryManager.getAccessibleCount(item)
            : InventoryManager.getCount(item) + InventoryManager.getEquippedCount(item);
    if (!InventoryManager.equippedOrInInventory(ItemPool.THE_ETERNITY_CODPIECE)) {
      count += liveCopiesInGemSlots(item);
    }
    return count;
  }

  /**
   * An upper bound on the score of any combination still reachable from a search node, so that a
   * node which cannot reach the incumbent need not be explored.
   *
   * <p>For each active term, {@code suffix[term][c][s]} is the most favourable raw value the gems
   * from index {@code c} onwards can contribute using at most {@code s} slots -- the largest when
   * the term's weight is positive, the smallest when it is negative, which is the direction that
   * makes the term's weighted contribution largest. Adding that to the branch's own baseline and to
   * what has already been selected therefore over-states every term, and hence the whole score.
   * Math.nextUp and Math.nextDown keep that true through floating point rounding.
   *
   * <p>A term minimum, a total minimum, or a boolean requirement needs no allowance here: those
   * only mark a combination as failed, and a failed combination already loses to the incumbent,
   * which is only compared against while it has not itself failed.
   */
  private static final class ScoreBound {
    private final List<Evaluator.ScoreModifier> terms;
    private final double constant;
    private final double[][] perCopy;
    private final double[] baseline;
    private final double[] selected;
    private final double[][][] suffix;

    ScoreBound(
        List<Evaluator.ScoreModifier> terms,
        Modifiers baseMods,
        List<Gem> candidates,
        int[] available,
        int slotsLeft) {
      this.terms = terms;
      this.constant = baseMods.hasString(StringModifier.ROLLOVER_EFFECT) ? 0.01f : 0.0;
      int size = terms.size();
      this.baseline = new double[size];
      this.selected = new double[size];
      this.perCopy = new double[candidates.size()][size];
      for (int t = 0; t < size; t++) {
        DoubleModifier modifier = (DoubleModifier) terms.get(t).modifier();
        this.baseline[t] = baseMods.getDouble(modifier);
        for (int c = 0; c < candidates.size(); c++) {
          this.perCopy[c][t] = candidates.get(c).mods().getDouble(modifier);
        }
      }
      this.suffix = new double[size][][];
      for (int t = 0; t < size; t++) {
        this.suffix[t] = this.buildSuffix(t, available, slotsLeft);
      }
    }

    private double[][] buildSuffix(int term, int[] available, int maxSlots) {
      int size = this.perCopy.length;
      boolean maximize = this.terms.get(term).weight() >= 0.0;
      // suffix[size][*] stays zero: with no candidates left nothing more can be contributed.
      double[][] suffix = new double[size + 1][maxSlots + 1];
      for (int c = size - 1; c >= 0; c--) {
        double value = this.perCopy[c][term];
        for (int slots = 1; slots <= maxSlots; slots++) {
          double best = suffix[c + 1][slots];
          for (int used = 1; used <= Math.min(available[c], slots); used++) {
            double total = used * value + suffix[c + 1][slots - used];
            total = maximize ? Math.nextUp(total) : Math.nextDown(total);
            best = maximize ? Math.max(best, total) : Math.min(best, total);
          }
          suffix[c][slots] = best;
        }
      }
      return suffix;
    }

    void select(int candidate, int copies) {
      for (int t = 0; t < this.terms.size(); t++) {
        this.selected[t] += copies * this.perCopy[candidate][t];
      }
    }

    double upperBound(int pos, int slotsLeft) {
      double score = this.constant;
      for (int t = 0; t < this.terms.size(); t++) {
        Evaluator.ScoreModifier term = this.terms.get(t);
        double value = this.baseline[t] + this.selected[t] + this.suffix[t][pos][slotsLeft];
        score = Math.nextUp(score + Math.nextUp(term.weight() * Math.min(value, term.max())));
      }
      return score;
    }
  }
}
