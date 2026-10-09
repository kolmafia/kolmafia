package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.Modifier;

/**
 * Evaluates a conservative upper score for the numeric portion of a Maximizer expression.
 *
 * <p>Candidates are represented as vectors of score-relevant modifier deltas, then combined by
 * equipment role and copy-limited interchangeable slot group. Lower and upper modifier bounds are
 * retained because signed weights can make either endpoint maximize a term. The resulting score may
 * overestimate a branch, but must never underestimate one that could contain the best result.
 */
final class MaximizerNumericBound {
  record BoundResult(double upper, boolean minimumFailed) {}

  private record CandidateVector(double[] values) {}

  private record RoleVectors(
      Map<Integer, CandidateVector> candidates, CandidateVector minimum, CandidateVector maximum) {}

  private record CapacityBounds(double[][] lower, double[][] upper) {}

  private record GroupVectors(
      List<Slot> slots,
      Map<Integer, CandidateVector> candidates,
      double[][] lowerByCapacity,
      double[][] upperByCapacity,
      MaximizerBranchBound.CapacityMode capacityMode) {}

  private final List<Evaluator.NumericScoreTerm> terms;
  private final DoubleModifier[] modifiers;
  private final int[] indices;
  private final CandidateVector baseline;
  private final Map<Slot, RoleVectors> roles;
  private final List<GroupVectors> groups;
  private final double interactionUpper;
  private final double[] lowerScratch;
  private final double[] upperScratch;

  private MaximizerNumericBound(
      List<Evaluator.NumericScoreTerm> terms,
      DoubleModifier[] modifiers,
      CandidateVector baseline,
      Map<Slot, RoleVectors> roles,
      List<GroupVectors> groups,
      double interactionUpper) {
    this.terms = terms;
    this.modifiers = modifiers;
    this.indices = new int[DoubleModifier.values().length];
    Arrays.fill(this.indices, -1);
    for (int index = 0; index < modifiers.length; index++) {
      this.indices[modifiers[index].ordinal()] = index;
    }
    this.baseline = baseline;
    this.roles = roles;
    this.groups = groups;
    this.interactionUpper = interactionUpper;
    this.lowerScratch = new double[baseline.values().length];
    this.upperScratch = new double[baseline.values().length];
  }

  static MaximizerNumericBound create(
      Evaluator evaluator,
      Modifiers baseline,
      Map<Slot, AdventureResult> baselineEquipment,
      Map<Modeable, String> modeables,
      Map<Slot, Map<Integer, Modifiers>> projections,
      List<MaximizerBranchBound.InterchangeableSlotGroup> groups,
      double interactionUpper) {
    var terms = evaluator.numericScoreTerms();
    var modifiers = dependencies(terms);
    double baselineItemScore = evaluator.numericEquipmentScore(baselineEquipment, modeables);
    var baselineVector = vector(modifiers, baseline, baselineItemScore);
    var roleVectors = new EnumMap<Slot, RoleVectors>(Slot.class);
    for (var role : projections.entrySet()) {
      var candidates = new HashMap<Integer, CandidateVector>();
      var minimumValues = new double[baselineVector.values().length];
      var maximumValues = new double[baselineVector.values().length];
      for (var candidate : role.getValue().entrySet()) {
        double itemScore =
            baselineItemScore
                + evaluator.numericItemScore(
                    net.sourceforge.kolmafia.objectpool.ItemPool.get(candidate.getKey()),
                    modeables);
        var delta = delta(baselineVector, vector(modifiers, candidate.getValue(), itemScore));
        candidates.put(candidate.getKey(), delta);
        minimize(minimumValues, delta.values());
        maximize(maximumValues, delta.values());
      }
      // Each modifier gets its own extrema. They need not come from the same candidate: combining
      // them only makes the eventual score more optimistic, which is safe for pruning.
      roleVectors.put(
          role.getKey(),
          new RoleVectors(
              Map.copyOf(candidates),
              new CandidateVector(minimumValues),
              new CandidateVector(maximumValues)));
    }

    var groupVectors = new ArrayList<GroupVectors>();
    for (var group : groups) {
      var candidates = new HashMap<Integer, CandidateVector>();
      for (var candidate : group.projectionDeltas().entrySet()) {
        candidates.put(
            candidate.getKey(),
            vector(
                modifiers,
                candidate.getValue(),
                evaluator.numericItemScore(
                    net.sourceforge.kolmafia.objectpool.ItemPool.get(candidate.getKey()),
                    modeables)));
      }
      var bounds =
          groupBounds(
              candidates, group.copiesByItem(), group.capacity(), baselineVector.values().length);
      groupVectors.add(
          new GroupVectors(
              group.slots(),
              Map.copyOf(candidates),
              bounds.lower(),
              bounds.upper(),
              group.capacityMode()));
    }

    return new MaximizerNumericBound(
        terms,
        modifiers,
        baselineVector,
        Map.copyOf(roleVectors),
        List.copyOf(groupVectors),
        interactionUpper);
  }

  BoundResult evaluate(MaximizerSpeculation speculation) {
    double[] lower = this.lowerScratch;
    double[] upper = this.upperScratch;
    System.arraycopy(this.baseline.values(), 0, lower, 0, lower.length);
    System.arraycopy(this.baseline.values(), 0, upper, 0, upper.length);

    for (var role : this.roles.entrySet()) {
      AdventureResult item = speculation.equipment.get(role.getKey());
      if (item == null) {
        add(lower, role.getValue().minimum().values());
        add(upper, role.getValue().maximum().values());
      } else {
        CandidateVector vector = role.getValue().candidates().get(item.getItemId());
        if (vector != null) {
          add(lower, vector.values());
          add(upper, vector.values());
        }
      }
    }

    for (var group : this.groups) {
      int remaining =
          group.capacityMode() == MaximizerBranchBound.CapacityMode.FIXED
              ? group.slots().size()
              : 0;
      for (var slot : group.slots()) {
        AdventureResult item = speculation.equipment.get(slot);
        if (item == null) {
          if (group.capacityMode() == MaximizerBranchBound.CapacityMode.UNRESOLVED_SLOTS) {
            remaining++;
          }
          continue;
        }
        CandidateVector vector = group.candidates().get(item.getItemId());
        if (vector != null) {
          add(lower, vector.values());
          add(upper, vector.values());
        }
      }

      // A copy-limited group contributes the best and worst attainable sum at each remaining
      // capacity, independently for every modifier dimension.
      for (int index = 0; index < lower.length; index++) {
        lower[index] += group.lowerByCapacity()[index][remaining];
        upper[index] += group.upperByCapacity()[index][remaining];
      }
    }

    double score = this.interactionUpper + upper[this.modifiers.length];
    boolean minimumFailed = false;
    for (var term : this.terms) {
      var modifier = (DoubleModifier) term.modifier();
      double maximum = this.scoreValue(upper, modifier);
      if (maximum < term.min()) minimumFailed = true;
      double value = term.weight() >= 0.0 ? maximum : this.scoreValue(lower, modifier);
      score += term.weight() * Math.min(value, term.max());
    }
    return new BoundResult(Math.nextUp(score), minimumFailed);
  }

  private double scoreValue(double[] values, DoubleModifier modifier) {
    return switch (modifier) {
      case ITEMDROP ->
          this.value(values, DoubleModifier.ITEMDROP)
              + 100.0
              + Math.min(0.0, this.value(values, DoubleModifier.ITEMDROP_PENALTY))
              + this.value(values, DoubleModifier.SPORADIC_ITEMDROP);
      case MEATDROP ->
          this.value(values, DoubleModifier.MEATDROP)
              + 100.0
              + Math.min(0.0, this.value(values, DoubleModifier.MEATDROP_PENALTY))
              + this.value(values, DoubleModifier.SPORADIC_MEATDROP)
              + this.value(values, DoubleModifier.MEAT_BONUS) / 10000.0;
      case MANA_COST ->
          this.value(values, DoubleModifier.MANA_COST)
              + this.value(values, DoubleModifier.STACKABLE_MANA_COST);
      case INITIATIVE ->
          this.value(values, DoubleModifier.INITIATIVE)
              + Math.min(0.0, this.value(values, DoubleModifier.INITIATIVE_PENALTY));
      case WEAPON_DAMAGE ->
          this.value(values, DoubleModifier.WEAPON_DAMAGE)
              + this.value(values, DoubleModifier.WEAPON_DAMAGE_PCT);
      case RANGED_DAMAGE ->
          this.value(values, DoubleModifier.RANGED_DAMAGE)
              + this.value(values, DoubleModifier.RANGED_DAMAGE_PCT);
      case SPELL_DAMAGE ->
          this.value(values, DoubleModifier.SPELL_DAMAGE)
              + this.value(values, DoubleModifier.SPELL_DAMAGE_PCT);
      case DAMAGE_AURA ->
          this.value(values, DoubleModifier.DAMAGE_AURA)
              + this.value(values, DoubleModifier.SPORADIC_DAMAGE_AURA);
      case THORNS ->
          this.value(values, DoubleModifier.THORNS)
              + this.value(values, DoubleModifier.SPORADIC_THORNS);
      default -> this.value(values, modifier);
    };
  }

  private static CandidateVector vector(
      DoubleModifier[] modifiers, Modifiers source, double itemScore) {
    var values = new double[modifiers.length + 1];
    for (int index = 0; index < modifiers.length; index++) {
      values[index] = source.getDouble(modifiers[index]);
    }
    values[modifiers.length] = itemScore;
    return new CandidateVector(values);
  }

  private static CandidateVector delta(CandidateVector baseline, CandidateVector candidate) {
    var values = candidate.values().clone();
    for (int index = 0; index < values.length; index++) {
      values[index] -= baseline.values()[index];
    }
    return new CandidateVector(values);
  }

  private static CapacityBounds groupBounds(
      Map<Integer, CandidateVector> candidates,
      Map<Integer, Integer> copiesByItem,
      int capacity,
      int valueCount) {
    double[][] lower = new double[valueCount][capacity + 1];
    double[][] upper = new double[valueCount][capacity + 1];
    for (int value = 0; value < valueCount; value++) {
      var contributions = new ArrayList<Double>();
      for (var entry : copiesByItem.entrySet()) {
        CandidateVector candidate = candidates.get(entry.getKey());
        if (candidate == null) continue;
        for (int copy = 0; copy < Math.min(entry.getValue(), capacity); copy++) {
          contributions.add(candidate.values()[value]);
        }
      }
      contributions.sort(java.util.Comparator.reverseOrder());
      for (int selected = 1; selected <= capacity; selected++) {
        upper[value][selected] = upper[value][selected - 1];
        if (selected <= contributions.size()) {
          upper[value][selected] += Math.max(0.0, contributions.get(selected - 1));
        }
      }
      contributions.sort(java.util.Comparator.naturalOrder());
      for (int selected = 1; selected <= capacity; selected++) {
        lower[value][selected] = lower[value][selected - 1];
        if (selected <= contributions.size()) {
          lower[value][selected] += Math.min(0.0, contributions.get(selected - 1));
        }
      }
    }
    return new CapacityBounds(lower, upper);
  }

  private double value(double[] values, DoubleModifier modifier) {
    int index = this.indices[modifier.ordinal()];
    return index < 0 ? 0.0 : values[index];
  }

  private static void maximize(double[] target, double[] candidate) {
    for (int index = 0; index < target.length; index++) {
      target[index] = Math.max(target[index], candidate[index]);
    }
  }

  private static void minimize(double[] target, double[] candidate) {
    for (int index = 0; index < target.length; index++) {
      target[index] = Math.min(target[index], candidate[index]);
    }
  }

  private static void add(double[] target, double[] addition) {
    for (int index = 0; index < target.length; index++) target[index] += addition[index];
  }

  private static DoubleModifier[] dependencies(List<Evaluator.NumericScoreTerm> terms) {
    var dependencies = EnumSet.noneOf(DoubleModifier.class);
    for (var term : terms) {
      Modifier scoreModifier = term.modifier();
      if (!(scoreModifier instanceof DoubleModifier modifier)) continue;
      dependencies.add(modifier);
      switch (modifier) {
        case MANA_COST -> dependencies.add(DoubleModifier.STACKABLE_MANA_COST);
        case INITIATIVE -> dependencies.add(DoubleModifier.INITIATIVE_PENALTY);
        case MEATDROP ->
            dependencies.addAll(
                List.of(
                    DoubleModifier.MEATDROP_PENALTY,
                    DoubleModifier.SPORADIC_MEATDROP,
                    DoubleModifier.MEAT_BONUS));
        case ITEMDROP ->
            dependencies.addAll(
                List.of(DoubleModifier.ITEMDROP_PENALTY, DoubleModifier.SPORADIC_ITEMDROP));
        case WEAPON_DAMAGE -> dependencies.add(DoubleModifier.WEAPON_DAMAGE_PCT);
        case RANGED_DAMAGE -> dependencies.add(DoubleModifier.RANGED_DAMAGE_PCT);
        case SPELL_DAMAGE -> dependencies.add(DoubleModifier.SPELL_DAMAGE_PCT);
        case DAMAGE_AURA -> dependencies.add(DoubleModifier.SPORADIC_DAMAGE_AURA);
        case THORNS -> dependencies.add(DoubleModifier.SPORADIC_THORNS);
        default -> {}
      }
    }
    return dependencies.toArray(DoubleModifier[]::new);
  }
}
