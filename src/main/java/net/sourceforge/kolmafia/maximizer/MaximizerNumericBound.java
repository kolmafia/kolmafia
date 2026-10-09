package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modeable;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;

/**
 * Evaluates a conservative upper score for the numeric portion of a Maximizer expression.
 *
 * <p>Candidates are represented as vectors of score-relevant modifier deltas, then combined by
 * equipment role and copy-limited interchangeable slot group. Lower and upper modifier bounds are
 * retained because negative weights and transformed modifiers can make either endpoint maximize a
 * term. The resulting score may overestimate a branch, but must never underestimate one that could
 * contain the best result.
 */
final class MaximizerNumericBound {
  record BoundResult(double upper, boolean minimumFailed) {}

  private static final int CHAMPAGNE = 1;
  private static final int GARBAGE_SHIRT = 1 << 1;
  private static final int EQUALIZE = 0;
  private static final int EQUALIZE_MUS = 1;
  private static final int EQUALIZE_MYS = 2;
  private static final int EQUALIZE_MOX = 3;
  private static final int FLOOR_MUS = 4;
  private static final int FLOOR_MYS = 5;
  private static final int FLOOR_MOX = 6;
  private static final int CONTROL_COUNT = 7;
  private static final int NONE = 1;
  private static final int MUS = 1 << 1;
  private static final int MYS = 1 << 2;
  private static final int MOX = 1 << 3;
  private static final int HIGH = 1 << 4;
  private static final int CONTROLS_MP = 1;
  private static final int MAY_CONTROL_MP = 1 << 1;
  private static final int MAX_INDEX_SPARSITY = 4;

  private record CandidateVector(
      double[] values,
      int[] bitmaps,
      int immunities,
      int vulnerabilities,
      boolean rolloverEffect,
      int dynamics,
      DerivedState derived) {}

  private record DerivedState(int[] controls, int[] limits, int mpControls) {}

  private record DerivedBounds(double[][] baseStats, double[][] buffedStats) {}

  private record RoleVectors(
      Map<Integer, CandidateVector> candidates,
      CandidateVector[] indexedCandidates,
      CandidateVector minimum,
      CandidateVector maximum) {
    CandidateVector candidate(int itemId) {
      return itemId >= 0 && itemId < this.indexedCandidates.length
          ? this.indexedCandidates[itemId]
          : this.candidates.get(itemId);
    }
  }

  private record PreparedRole(Slot slot, RoleVectors vectors) {}

  private record CapacityBounds(double[][] lower, double[][] upper) {}

  private record PreparedScoreTerm(Evaluator.NumericScoreTerm term, int directIndex) {}

  private record GroupVectors(
      List<Slot> slots,
      Map<Integer, CandidateVector> candidates,
      CandidateVector[] indexedCandidates,
      int capacity,
      double[][] lowerByCapacity,
      double[][] upperByCapacity,
      int[] possibleBitmaps,
      int possibleImmunities,
      int possibleVulnerabilities,
      boolean possibleRolloverEffect,
      int possibleDynamics,
      DerivedState possibleDerived,
      MaximizerBranchBound.CapacityMode capacityMode) {
    CandidateVector candidate(int itemId) {
      return itemId >= 0 && itemId < this.indexedCandidates.length
          ? this.indexedCandidates[itemId]
          : this.candidates.get(itemId);
    }
  }

  private final PreparedScoreTerm[] preparedTerms;
  private final int derivedTermCount;
  private final DoubleModifier[] modifiers;
  private final int[] indices;
  private final CandidateVector baseline;
  private final Map<Slot, RoleVectors> roles;
  private final List<GroupVectors> groups;
  private final PreparedRole[] preparedRoles;
  private final BitSet twoHandedWeapons;
  private final double interactionUpper;
  // Separate category maxima can describe incompatible outfits. These retain the combined
  // Brimstone/Cloathing residual at the root and for each pair of possible piece-count limits.
  private final double jointBrimstoneCloathingRootUpper;
  private final double[][] jointBrimstoneCloathingUpper;
  private final double[] lowerScratch;
  private final double[] upperScratch;
  private final int[] lowerBitmapScratch = new int[BitmapModifier.values().length];
  private final int[] upperBitmapScratch = new int[BitmapModifier.values().length];
  private int lowerImmunities;
  private int upperImmunities;
  private int lowerVulnerabilities;
  private int upperVulnerabilities;
  private boolean rolloverEffect;
  private int selectedDynamics;
  private int possibleDynamics;
  private final int[] selectedControls = new int[CONTROL_COUNT];
  private final int[] possibleControls = new int[CONTROL_COUNT];
  private final int[] selectedLimits = new int[3];
  private final int[] possibleLimits = new int[3];
  private int selectedMpControls;
  private int possibleMpControls;
  private boolean minimumFailed;

  private MaximizerNumericBound(
      List<Evaluator.NumericScoreTerm> terms,
      DoubleModifier[] modifiers,
      CandidateVector baseline,
      Map<Slot, RoleVectors> roles,
      List<GroupVectors> groups,
      BitSet twoHandedWeapons,
      double interactionUpper,
      double jointBrimstoneCloathingRootUpper,
      double[][] jointBrimstoneCloathingUpper) {
    this.modifiers = modifiers;
    this.indices = new int[DoubleModifier.values().length];
    Arrays.fill(this.indices, -1);
    for (int index = 0; index < modifiers.length; index++) {
      this.indices[modifiers[index].ordinal()] = index;
    }
    this.preparedTerms =
        terms.stream()
            .map(
                term ->
                    new PreparedScoreTerm(
                        term,
                        term.modifier() instanceof DoubleModifier modifier
                            ? this.indices[modifier.ordinal()]
                            : -1))
            .toArray(PreparedScoreTerm[]::new);
    this.derivedTermCount =
        (int)
            terms.stream()
                .filter(
                    term ->
                        term.modifier() == DoubleModifier.MUS
                            || term.modifier() == DoubleModifier.MYS
                            || term.modifier() == DoubleModifier.MOX
                            || term.modifier() == DoubleModifier.HP
                            || term.modifier() == DoubleModifier.MP)
                .count();
    this.baseline = baseline;
    this.roles = roles;
    this.groups = groups;
    this.preparedRoles =
        roles.entrySet().stream()
            .map(role -> new PreparedRole(role.getKey(), role.getValue()))
            .toArray(PreparedRole[]::new);
    this.twoHandedWeapons = twoHandedWeapons;
    this.interactionUpper = interactionUpper;
    this.jointBrimstoneCloathingRootUpper = jointBrimstoneCloathingRootUpper;
    this.jointBrimstoneCloathingUpper = jointBrimstoneCloathingUpper;
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
      double interactionUpper,
      double jointBrimstoneCloathingRootUpper,
      double[][] jointBrimstoneCloathingUpper) {
    var terms = evaluator.numericScoreTerms();

    var modifiers = dependencies(terms);
    double baselineItemScore = evaluator.numericEquipmentScore(baselineEquipment, modeables);
    var baselineVector =
        withDynamics(
            vector(modifiers, baseline, baselineItemScore), dynamics(baselineEquipment.values()));
    var twoHandedWeapons = new BitSet();
    AdventureResult baselineWeapon = baselineEquipment.get(Slot.WEAPON);
    if (baselineWeapon != null && EquipmentDatabase.getHands(baselineWeapon.getItemId()) > 1) {
      twoHandedWeapons.set(baselineWeapon.getItemId());
    }
    var roleVectors = new EnumMap<Slot, RoleVectors>(Slot.class);
    for (var role : projections.entrySet()) {
      var candidates = new HashMap<Integer, CandidateVector>();
      var minimumValues = new double[baselineVector.values().length];
      var maximumValues = new double[baselineVector.values().length];
      var possibleBitmaps = new int[BitmapModifier.values().length];
      int possibleImmunities = 0;
      int possibleVulnerabilities = 0;
      boolean possibleRolloverEffect = false;
      int possibleDynamics = 0;
      var possibleDerived = emptyDerivedState();
      for (var candidate : role.getValue().entrySet()) {
        if (role.getKey() == Slot.WEAPON && EquipmentDatabase.getHands(candidate.getKey()) > 1) {
          twoHandedWeapons.set(candidate.getKey());
        }
        double itemScore =
            baselineItemScore
                + evaluator.numericItemScore(ItemPool.get(candidate.getKey()), modeables);
        var candidateVector = vector(modifiers, candidate.getValue(), itemScore);
        var delta = delta(baselineVector, candidateVector);
        delta = normalizeDynamicDelta(delta, baselineVector, modifiers, candidate.getKey());
        candidates.put(candidate.getKey(), delta);
        minimize(minimumValues, delta.values());
        maximize(maximumValues, delta.values());
        merge(possibleBitmaps, delta.bitmaps());
        possibleImmunities |= delta.immunities();
        possibleVulnerabilities |= delta.vulnerabilities();
        possibleRolloverEffect |= delta.rolloverEffect();
        possibleDynamics |= delta.dynamics();
        possibleDerived = mergeDerived(possibleDerived, delta.derived());
      }
      // Each modifier gets its own extrema. They need not come from the same candidate: combining
      // them only makes the eventual score more optimistic, which is safe for pruning.
      roleVectors.put(
          role.getKey(),
          new RoleVectors(
              Map.copyOf(candidates),
              indexedCandidates(candidates),
              new CandidateVector(
                  minimumValues,
                  new int[BitmapModifier.values().length],
                  0,
                  possibleVulnerabilities,
                  false,
                  0,
                  emptyDerivedState()),
              new CandidateVector(
                  maximumValues,
                  possibleBitmaps,
                  possibleImmunities,
                  0,
                  possibleRolloverEffect,
                  possibleDynamics,
                  possibleDerived)));
    }

    var groupVectors = new ArrayList<GroupVectors>();
    for (var group : groups) {
      var candidates = new HashMap<Integer, CandidateVector>();
      var possibleBitmaps = new int[BitmapModifier.values().length];
      int possibleImmunities = 0;
      int possibleVulnerabilities = 0;
      boolean possibleRolloverEffect = false;
      int possibleDynamics = 0;
      var possibleDerived = emptyDerivedState();
      for (var candidate : group.projectionDeltas().entrySet()) {
        var vector =
            vector(
                modifiers,
                candidate.getValue(),
                evaluator.numericItemScore(ItemPool.get(candidate.getKey()), modeables));
        vector =
            withDerived(
                vector, derivedState(ModifierDatabase.getItemModifiers(candidate.getKey())));
        Modifiers itemModifiers = ModifierDatabase.getItemModifiers(candidate.getKey());
        if (itemModifiers != null && itemModifiers.hasString(StringModifier.ROLLOVER_EFFECT)) {
          vector =
              new CandidateVector(
                  vector.values(),
                  vector.bitmaps(),
                  vector.immunities(),
                  vector.vulnerabilities(),
                  true,
                  vector.dynamics(),
                  vector.derived());
        }
        vector = normalizeDynamicDelta(vector, baselineVector, modifiers, candidate.getKey());
        candidates.put(candidate.getKey(), vector);
        merge(possibleBitmaps, vector.bitmaps());
        possibleImmunities |= vector.immunities();
        possibleVulnerabilities |= vector.vulnerabilities();
        possibleRolloverEffect |= vector.rolloverEffect();
        possibleDynamics |= vector.dynamics();
        possibleDerived = mergeDerived(possibleDerived, vector.derived());
      }
      var bounds =
          groupBounds(
              candidates, group.copiesByItem(), group.capacity(), baselineVector.values().length);
      groupVectors.add(
          new GroupVectors(
              group.slots(),
              Map.copyOf(candidates),
              indexedCandidates(candidates),
              group.capacity(),
              bounds.lower(),
              bounds.upper(),
              possibleBitmaps,
              possibleImmunities,
              possibleVulnerabilities,
              possibleRolloverEffect,
              possibleDynamics,
              possibleDerived,
              group.capacityMode()));
    }

    return new MaximizerNumericBound(
        terms,
        modifiers,
        baselineVector,
        roleVectors,
        List.copyOf(groupVectors),
        twoHandedWeapons,
        interactionUpper,
        jointBrimstoneCloathingRootUpper,
        jointBrimstoneCloathingUpper);
  }

  BoundResult evaluate(MaximizerSpeculation speculation) {
    this.prepare(speculation, null, false);
    return this.scorePrepared();
  }

  private void prepare(
      MaximizerSpeculation speculation, Slot excludedSlot, boolean excludeOffhand) {
    double[] lower = this.lowerScratch;
    double[] upper = this.upperScratch;
    System.arraycopy(this.baseline.values(), 0, lower, 0, lower.length);
    System.arraycopy(this.baseline.values(), 0, upper, 0, upper.length);
    int[] lowerBitmaps = this.lowerBitmapScratch;
    int[] upperBitmaps = this.upperBitmapScratch;
    System.arraycopy(this.baseline.bitmaps(), 0, lowerBitmaps, 0, lowerBitmaps.length);
    System.arraycopy(this.baseline.bitmaps(), 0, upperBitmaps, 0, upperBitmaps.length);
    this.lowerImmunities = this.baseline.immunities();
    this.upperImmunities = this.baseline.immunities();
    this.lowerVulnerabilities = this.baseline.vulnerabilities();
    this.upperVulnerabilities = this.baseline.vulnerabilities();
    this.rolloverEffect = this.baseline.rolloverEffect();
    this.selectedDynamics = 0;
    this.possibleDynamics = 0;
    System.arraycopy(
        this.baseline.derived().controls(), 0, this.selectedControls, 0, CONTROL_COUNT);
    Arrays.fill(this.possibleControls, 0);
    System.arraycopy(this.baseline.derived().limits(), 0, this.selectedLimits, 0, 3);
    Arrays.fill(this.possibleLimits, 0);
    this.selectedMpControls = this.baseline.derived().mpControls();
    this.possibleMpControls = 0;
    AdventureResult weapon = speculation.equipment.get(Slot.WEAPON);
    boolean blocksOffhand =
        weapon != null && weapon.getItemId() > 0 && this.twoHandedWeapons.get(weapon.getItemId());
    for (var role : this.preparedRoles) {
      this.prepareRole(
          speculation, excludedSlot, excludeOffhand || blocksOffhand, role.slot(), role.vectors());
    }
    for (int groupIndex = 0; groupIndex < this.groups.size(); groupIndex++) {
      var group = this.groups.get(groupIndex);
      int remaining =
          group.capacityMode() == MaximizerBranchBound.CapacityMode.FIXED ? group.capacity() : 0;
      for (var slot : group.slots()) {
        AdventureResult item = speculation.equipment.get(slot);
        if (item == null) {
          if (group.capacityMode() == MaximizerBranchBound.CapacityMode.UNRESOLVED_SLOTS) {
            remaining++;
          }
          continue;
        }
        CandidateVector vector = group.candidate(item.getItemId());
        if (vector != null) {
          add(lower, vector.values());
          add(upper, vector.values());
          merge(lowerBitmaps, vector.bitmaps());
          merge(upperBitmaps, vector.bitmaps());
          this.lowerImmunities |= vector.immunities();
          this.upperImmunities |= vector.immunities();
          this.lowerVulnerabilities |= vector.vulnerabilities();
          this.upperVulnerabilities |= vector.vulnerabilities();
          this.rolloverEffect |= vector.rolloverEffect();
          this.selectedDynamics |= vector.dynamics();
          this.addSelectedDerived(vector.derived());
        }
      }
      // A copy-limited group contributes the best and worst attainable sum at each remaining
      // capacity, independently for every modifier dimension.
      for (int index = 0; index < lower.length; index++) {
        lower[index] += group.lowerByCapacity()[index][remaining];
        upper[index] += group.upperByCapacity()[index][remaining];
      }
      if (remaining > 0) {
        merge(upperBitmaps, group.possibleBitmaps());
        this.lowerVulnerabilities |= group.possibleVulnerabilities();
        this.upperImmunities |= group.possibleImmunities();
        this.rolloverEffect |= group.possibleRolloverEffect();
        this.possibleDynamics |= group.possibleDynamics();
        this.addPossible(group.possibleDerived());
      }
    }
  }

  private void prepareRole(
      MaximizerSpeculation speculation,
      Slot excludedSlot,
      boolean excludeOffhand,
      Slot slot,
      RoleVectors vectors) {
    if (slot == excludedSlot || (excludeOffhand && slot == Slot.OFFHAND)) return;
    AdventureResult item = speculation.equipment.get(slot);
    if (item == null) {
      add(this.lowerScratch, vectors.minimum().values());
      add(this.upperScratch, vectors.maximum().values());
      this.lowerVulnerabilities |= vectors.minimum().vulnerabilities();
      merge(this.upperBitmapScratch, vectors.maximum().bitmaps());
      this.upperImmunities |= vectors.maximum().immunities();
      this.rolloverEffect |= vectors.maximum().rolloverEffect();
      this.possibleDynamics |= vectors.maximum().dynamics();
      this.addPossible(vectors.maximum().derived());
      return;
    }
    CandidateVector vector = vectors.candidate(item.getItemId());
    if (vector != null) {
      this.addSelected(vector);
    }
  }

  private void addSelected(CandidateVector vector) {
    add(this.lowerScratch, vector.values());
    add(this.upperScratch, vector.values());
    merge(this.lowerBitmapScratch, vector.bitmaps());
    merge(this.upperBitmapScratch, vector.bitmaps());
    this.lowerImmunities |= vector.immunities();
    this.upperImmunities |= vector.immunities();
    this.lowerVulnerabilities |= vector.vulnerabilities();
    this.upperVulnerabilities |= vector.vulnerabilities();
    this.rolloverEffect |= vector.rolloverEffect();
    this.selectedDynamics |= vector.dynamics();
    this.addSelectedDerived(vector.derived());
  }

  private void addSelectedDerived(DerivedState derived) {
    merge(this.selectedControls, derived.controls());
    mergeLimits(this.selectedLimits, derived.limits());
    this.selectedMpControls |= derived.mpControls();
  }

  private void addPossible(DerivedState derived) {
    merge(this.possibleControls, derived.controls());
    mergeLimits(this.possibleLimits, derived.limits());
    this.possibleMpControls |= derived.mpControls();
  }

  private BoundResult scorePrepared() {
    double[] lower = this.lowerScratch;
    double[] upper = this.upperScratch;
    int[] lowerBitmaps = this.lowerBitmapScratch;
    int[] upperBitmaps = this.upperBitmapScratch;
    this.minimumFailed = false;

    adjustForDynamicItemDoubling(lower, upper, this.selectedDynamics, this.possibleDynamics);

    // Item identity bonuses and rollover effects do not appear in aggregate modifier totals.
    double itemScore = upper[this.modifiers.length];
    double rolloverScore = this.rolloverEffect ? 0.01 : 0.0;
    DerivedBounds derivedBounds = null;
    double score =
        this.interactionUpper - this.jointCategoryReduction() + rolloverScore + itemScore;
    for (var preparedTerm : this.preparedTerms) {
      var term = preparedTerm.term();
      double value;
      if (term.modifier() instanceof BitmapModifier bitmapModifier) {
        int[] bitmaps = term.weight() >= 0.0 ? upperBitmaps : lowerBitmaps;
        value = Integer.bitCount(bitmaps[bitmapModifier.ordinal()]);
      } else if (term.modifier() == DoubleModifier.FAMILIAR_WEIGHT) {
        value = familiarWeight(lower, upper, term.weight() >= 0.0);
      } else if (term.modifier() == DoubleModifier.EXPERIENCE) {
        value = experience(lower, upper, term.weight() >= 0.0);
      } else {
        var modifier = (DoubleModifier) term.modifier();
        if (modifier == DoubleModifier.MUS
            || modifier == DoubleModifier.MYS
            || modifier == DoubleModifier.MOX
            || modifier == DoubleModifier.HP
            || modifier == DoubleModifier.MP) {
          boolean reuse = this.derivedTermCount > 1;
          if (reuse && derivedBounds == null) {
            derivedBounds = this.derivedBounds(lower, upper);
          }
          value =
              derivedValue(
                  modifier, lower, upper, term.weight() >= 0.0, reuse ? derivedBounds : null);
          if (term.weight() >= 0.0 && value < term.min()) {
            this.minimumFailed = true;
          }
          double contribution = term.weight() * Math.min(value, term.max());
          score += contribution;
          continue;
        }
        double[] values = term.weight() >= 0.0 ? upper : lower;
        int immunities = term.weight() >= 0.0 ? this.upperImmunities : this.lowerImmunities;
        int vulnerabilities =
            term.weight() >= 0.0 ? this.upperVulnerabilities : this.lowerVulnerabilities;
        value =
            switch (modifier) {
              case ITEMDROP ->
                  value(values, DoubleModifier.ITEMDROP)
                      + 100.0
                      + Math.min(0.0, value(values, DoubleModifier.ITEMDROP_PENALTY))
                      + value(values, DoubleModifier.SPORADIC_ITEMDROP);
              case MEATDROP ->
                  value(values, DoubleModifier.MEATDROP)
                      + 100.0
                      + Math.min(0.0, value(values, DoubleModifier.MEATDROP_PENALTY))
                      + value(values, DoubleModifier.SPORADIC_MEATDROP)
                      + value(values, DoubleModifier.MEAT_BONUS) / 10000.0;
              case MANA_COST ->
                  value(values, DoubleModifier.MANA_COST)
                      + value(values, DoubleModifier.STACKABLE_MANA_COST);
              case INITIATIVE ->
                  value(values, DoubleModifier.INITIATIVE)
                      + Math.min(0.0, value(values, DoubleModifier.INITIATIVE_PENALTY));
              case WEAPON_DAMAGE ->
                  value(values, DoubleModifier.WEAPON_DAMAGE)
                      + value(values, DoubleModifier.WEAPON_DAMAGE_PCT);
              case RANGED_DAMAGE ->
                  value(values, DoubleModifier.RANGED_DAMAGE)
                      + value(values, DoubleModifier.RANGED_DAMAGE_PCT);
              case SPELL_DAMAGE ->
                  value(values, DoubleModifier.SPELL_DAMAGE)
                      + value(values, DoubleModifier.SPELL_DAMAGE_PCT);
              case COLD_RESISTANCE ->
                  resistance(
                      value(values, DoubleModifier.COLD_RESISTANCE),
                      immunities,
                      vulnerabilities,
                      0,
                      term.weight() >= 0.0);
              case HOT_RESISTANCE ->
                  resistance(
                      value(values, DoubleModifier.HOT_RESISTANCE),
                      immunities,
                      vulnerabilities,
                      1,
                      term.weight() >= 0.0);
              case SLEAZE_RESISTANCE ->
                  resistance(
                      value(values, DoubleModifier.SLEAZE_RESISTANCE),
                      immunities,
                      vulnerabilities,
                      2,
                      term.weight() >= 0.0);
              case SPOOKY_RESISTANCE ->
                  resistance(
                      value(values, DoubleModifier.SPOOKY_RESISTANCE),
                      immunities,
                      vulnerabilities,
                      3,
                      term.weight() >= 0.0);
              case STENCH_RESISTANCE ->
                  resistance(
                      value(values, DoubleModifier.STENCH_RESISTANCE),
                      immunities,
                      vulnerabilities,
                      4,
                      term.weight() >= 0.0);
              case DAMAGE_AURA ->
                  value(values, DoubleModifier.DAMAGE_AURA)
                      + value(values, DoubleModifier.SPORADIC_DAMAGE_AURA);
              case THORNS ->
                  value(values, DoubleModifier.THORNS)
                      + value(values, DoubleModifier.SPORADIC_THORNS);
              default -> values[preparedTerm.directIndex()];
            };
      }
      if (term.weight() >= 0.0 && value < term.min()) {
        this.minimumFailed = true;
      }
      double contribution = term.weight() * Math.min(value, term.max());
      score += contribution;
    }
    return new BoundResult(score, this.minimumFailed);
  }

  private double jointCategoryReduction() {
    if (this.jointBrimstoneCloathingUpper.length == 0) return 0.0;
    // As choices eliminate possible set pieces, these upper masks shrink to the optimistic counts
    // still available. Subtract the residual lost since the root; nextDown keeps that conservative.
    int brimstone = Integer.bitCount(this.upperBitmapScratch[BitmapModifier.BRIMSTONE.ordinal()]);
    int cloathing = Integer.bitCount(this.upperBitmapScratch[BitmapModifier.CLOATHING.ordinal()]);
    double upper =
        this.jointBrimstoneCloathingUpper[
            Math.min(brimstone, this.jointBrimstoneCloathingUpper.length - 1)][
            Math.min(cloathing, this.jointBrimstoneCloathingUpper[0].length - 1)];
    return Math.max(0.0, Math.nextDown(this.jointBrimstoneCloathingRootUpper - upper));
  }

  private static CandidateVector vector(
      DoubleModifier[] dependencies, Modifiers modifiers, double itemScore) {
    double[] values = new double[dependencies.length + 1];
    for (int index = 0; index < dependencies.length; index++) {
      values[index] = modifiers.getDouble(dependencies[index]);
    }
    values[dependencies.length] = itemScore;
    int[] bitmaps = new int[BitmapModifier.values().length];
    for (var modifier : BitmapModifier.values()) {
      bitmaps[modifier.ordinal()] = modifiers.getRawBitmap(modifier);
    }
    int immunities = 0;
    int vulnerabilities = 0;
    if (modifiers.getBoolean(BooleanModifier.COLD_IMMUNITY)) immunities |= 1;
    if (modifiers.getBoolean(BooleanModifier.HOT_IMMUNITY)) immunities |= 1 << 1;
    if (modifiers.getBoolean(BooleanModifier.SLEAZE_IMMUNITY)) immunities |= 1 << 2;
    if (modifiers.getBoolean(BooleanModifier.SPOOKY_IMMUNITY)) immunities |= 1 << 3;
    if (modifiers.getBoolean(BooleanModifier.STENCH_IMMUNITY)) immunities |= 1 << 4;
    if (modifiers.getBoolean(BooleanModifier.COLD_VULNERABILITY)) vulnerabilities |= 1;
    if (modifiers.getBoolean(BooleanModifier.HOT_VULNERABILITY)) vulnerabilities |= 1 << 1;
    if (modifiers.getBoolean(BooleanModifier.SLEAZE_VULNERABILITY)) vulnerabilities |= 1 << 2;
    if (modifiers.getBoolean(BooleanModifier.SPOOKY_VULNERABILITY)) vulnerabilities |= 1 << 3;
    if (modifiers.getBoolean(BooleanModifier.STENCH_VULNERABILITY)) vulnerabilities |= 1 << 4;
    return new CandidateVector(
        values,
        bitmaps,
        immunities,
        vulnerabilities,
        modifiers.hasString(StringModifier.ROLLOVER_EFFECT),
        0,
        derivedState(modifiers));
  }

  private static CandidateVector[] indexedCandidates(Map<Integer, CandidateVector> candidates) {
    int maximumItemId =
        candidates.keySet().stream()
            .mapToInt(Integer::intValue)
            .filter(itemId -> itemId >= 0)
            .max()
            .orElse(-1);
    if ((long) maximumItemId + 1 > (long) candidates.size() * MAX_INDEX_SPARSITY) {
      return new CandidateVector[0];
    }
    CandidateVector[] indexed = new CandidateVector[maximumItemId + 1];
    for (var candidate : candidates.entrySet()) {
      if (candidate.getKey() >= 0) indexed[candidate.getKey()] = candidate.getValue();
    }
    return indexed;
  }

  private static CandidateVector delta(CandidateVector baseline, CandidateVector candidate) {
    double[] values = new double[baseline.values().length];
    for (int index = 0; index < values.length; index++) {
      values[index] = candidate.values()[index] - baseline.values()[index];
    }
    int[] bitmaps = new int[BitmapModifier.values().length];
    for (var modifier : BitmapModifier.values()) {
      bitmaps[modifier.ordinal()] =
          candidate.bitmaps()[modifier.ordinal()] & ~baseline.bitmaps()[modifier.ordinal()];
    }
    return new CandidateVector(
        values,
        bitmaps,
        candidate.immunities() & ~baseline.immunities(),
        candidate.vulnerabilities() & ~baseline.vulnerabilities(),
        candidate.rolloverEffect() && !baseline.rolloverEffect(),
        0,
        candidate.derived());
  }

  private static CandidateVector normalizeDynamicDelta(
      CandidateVector vector, CandidateVector baseline, DoubleModifier[] modifiers, int itemId) {
    int dynamics = dynamics(itemId);
    dynamics &= ~baseline.dynamics();
    if (dynamics == 0) {
      return vector;
    }
    for (int index = 0; index < modifiers.length; index++) {
      if (isDynamicModifier(dynamics, modifiers[index])) {
        vector.values()[index] = (vector.values()[index] - baseline.values()[index]) / 2.0;
      }
    }
    return withDynamics(vector, dynamics);
  }

  private static CandidateVector withDynamics(CandidateVector vector, int dynamics) {
    return new CandidateVector(
        vector.values(),
        vector.bitmaps(),
        vector.immunities(),
        vector.vulnerabilities(),
        vector.rolloverEffect(),
        dynamics,
        vector.derived());
  }

  private static CandidateVector withDerived(CandidateVector vector, DerivedState derived) {
    return new CandidateVector(
        vector.values(),
        vector.bitmaps(),
        vector.immunities(),
        vector.vulnerabilities(),
        vector.rolloverEffect(),
        vector.dynamics(),
        derived);
  }

  private static DerivedState emptyDerivedState() {
    return new DerivedState(new int[CONTROL_COUNT], new int[3], 0);
  }

  private static DerivedState derivedState(Modifiers modifiers) {
    if (modifiers == null) return emptyDerivedState();
    int[] controls = new int[CONTROL_COUNT];
    controls[EQUALIZE] = control(modifiers.getString(StringModifier.EQUALIZE), true);
    controls[EQUALIZE_MUS] = control(modifiers.getString(StringModifier.EQUALIZE_MUSCLE), false);
    controls[EQUALIZE_MYS] = control(modifiers.getString(StringModifier.EQUALIZE_MYST), false);
    controls[EQUALIZE_MOX] = control(modifiers.getString(StringModifier.EQUALIZE_MOXIE), false);
    controls[FLOOR_MUS] = control(modifiers.getString(StringModifier.FLOOR_BUFFED_MUSCLE), false);
    controls[FLOOR_MYS] = control(modifiers.getString(StringModifier.FLOOR_BUFFED_MYST), false);
    controls[FLOOR_MOX] = control(modifiers.getString(StringModifier.FLOOR_BUFFED_MOXIE), false);
    int[] limits = {
      positiveInt(modifiers.getDouble(DoubleModifier.MUS_LIMIT)),
      positiveInt(modifiers.getDouble(DoubleModifier.MYS_LIMIT)),
      positiveInt(modifiers.getDouble(DoubleModifier.MOX_LIMIT))
    };
    int mpControls = 0;
    if (modifiers.getBoolean(BooleanModifier.MOXIE_CONTROLS_MP)) mpControls |= CONTROLS_MP;
    if (modifiers.getBoolean(BooleanModifier.MOXIE_MAY_CONTROL_MP)) mpControls |= MAY_CONTROL_MP;
    return new DerivedState(controls, limits, mpControls);
  }

  private static int control(String value, boolean allowHigh) {
    if (value.startsWith("Mus")) return MUS;
    if (value.startsWith("Mys")) return MYS;
    if (value.startsWith("Mox")) return MOX;
    if (allowHigh && value.startsWith("High")) return HIGH;
    return NONE;
  }

  private static int positiveInt(double value) {
    return value > 0.0 ? (int) value : 0;
  }

  private static int dynamics(Iterable<AdventureResult> equipment) {
    int dynamics = 0;
    for (var item : equipment) {
      if (item != null) dynamics |= dynamics(item.getItemId());
    }
    return dynamics;
  }

  private static int dynamics(int itemId) {
    if (itemId == ItemPool.BROKEN_CHAMPAGNE
        && (Preferences.getInteger("garbageChampagneCharge") > 0
            || !Preferences.getBoolean("_garbageItemChanged"))) return CHAMPAGNE;
    if (itemId == ItemPool.MAKESHIFT_GARBAGE_SHIRT
        && (Preferences.getInteger("garbageShirtCharge") > 0
            || !Preferences.getBoolean("_garbageItemChanged"))) return GARBAGE_SHIRT;
    return 0;
  }

  private static boolean isDynamicModifier(int dynamics, DoubleModifier modifier) {
    if ((dynamics & CHAMPAGNE) != 0 && modifier == DoubleModifier.ITEMDROP) return true;
    if ((dynamics & GARBAGE_SHIRT) == 0) return false;
    return switch (modifier) {
      case EXPERIENCE,
          MUS_EXPERIENCE,
          MYS_EXPERIENCE,
          MOX_EXPERIENCE,
          MUS_EXPERIENCE_PCT,
          MYS_EXPERIENCE_PCT,
          MOX_EXPERIENCE_PCT ->
          true;
      default -> false;
    };
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

  private double familiarWeight(double[] lower, double[] upper, boolean maximum) {
    double lowerWeight =
        value(lower, DoubleModifier.FAMILIAR_WEIGHT)
            + value(lower, DoubleModifier.HIDDEN_FAMILIAR_WEIGHT);
    double upperWeight =
        value(upper, DoubleModifier.FAMILIAR_WEIGHT)
            + value(upper, DoubleModifier.HIDDEN_FAMILIAR_WEIGHT);
    double result = maximum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    if (value(upper, DoubleModifier.FAMILIAR_WEIGHT_PCT) >= 0.0) {
      result = maximum ? Math.max(result, upperWeight) : Math.min(result, lowerWeight);
    }
    if (value(lower, DoubleModifier.FAMILIAR_WEIGHT_PCT) < 0.0) {
      double first = lowerWeight * 0.5;
      double second = upperWeight * 0.5;
      result =
          maximum
              ? Math.max(result, Math.max(first, second))
              : Math.min(result, Math.min(first, second));
    }
    return result;
  }

  private double experience(double[] lower, double[] upper, boolean maximum) {
    double result = maximum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    // Monster level and experience bonuses interact multiplicatively, so every interval corner
    // can potentially be an extremum.
    for (int mask = 0; mask < 16; mask++) {
      double monsterLevel = value((mask & 1) == 0 ? lower : upper, DoubleModifier.MONSTER_LEVEL);
      double monsterLevelPercent =
          value((mask & 2) == 0 ? lower : upper, DoubleModifier.MONSTER_LEVEL_PERCENT);
      double experience = value((mask & 4) == 0 ? lower : upper, DoubleModifier.primeStatExp());
      double experiencePercent =
          value((mask & 8) == 0 ? lower : upper, DoubleModifier.primeStatExpPercent()) / 100.0;
      double baseExperience =
          net.sourceforge.kolmafia.KoLCharacter.estimatedBaseExp(
              monsterLevel * (1 + monsterLevelPercent / 100));
      double candidate = ((baseExperience + experience) * (1 + experiencePercent)) / 2.0;
      result = maximum ? Math.max(result, candidate) : Math.min(result, candidate);
    }
    return result;
  }

  private double derivedValue(
      DoubleModifier modifier,
      double[] lower,
      double[] upper,
      boolean maximum,
      DerivedBounds prepared) {
    DerivedBounds bounds = prepared == null ? this.derivedBounds(lower, upper) : prepared;
    double[][] baseStats = bounds.baseStats();
    double[][] buffedStats = bounds.buffedStats();
    if (modifier == DoubleModifier.MUS
        || modifier == DoubleModifier.MYS
        || modifier == DoubleModifier.MOX) {
      return buffedStats[
          modifier == DoubleModifier.MUS ? 0 : modifier == DoubleModifier.MYS ? 1 : 2][
          maximum ? 1 : 0];
    }
    return modifier == DoubleModifier.HP
        ? hpBound(baseStats[0], buffedStats[0], lower, upper, maximum)
        : mpBound(baseStats[1], buffedStats[1], buffedStats[2], lower, upper, maximum);
  }

  private DerivedBounds derivedBounds(double[] lower, double[] upper) {
    double[][] baseStats = baseStatBounds();
    // Mirror authoritative stat evaluation order: limit/equalize base stats, apply modifiers, then
    // apply buffed-stat floors.
    applyEqualize(baseStats, EQUALIZE, true);
    baseStats[0] = controlled(baseStats[0], baseStats, EQUALIZE_MUS, false);
    baseStats[1] = controlled(baseStats[1], baseStats, EQUALIZE_MYS, false);
    baseStats[2] = controlled(baseStats[2], baseStats, EQUALIZE_MOX, false);
    double[][] buffedStats = {
      statBounds(baseStats[0], DoubleModifier.MUS, DoubleModifier.MUS_PCT, lower, upper),
      statBounds(baseStats[1], DoubleModifier.MYS, DoubleModifier.MYS_PCT, lower, upper),
      statBounds(baseStats[2], DoubleModifier.MOX, DoubleModifier.MOX_PCT, lower, upper)
    };
    buffedStats[0] = floored(buffedStats[0], buffedStats, FLOOR_MUS);
    buffedStats[1] = floored(buffedStats[1], buffedStats, FLOOR_MYS);
    buffedStats[2] = floored(buffedStats[2], buffedStats, FLOOR_MOX);
    return new DerivedBounds(baseStats, buffedStats);
  }

  private double[][] baseStatBounds() {
    int[] bases = {
      KoLCharacter.getBaseMuscle(), KoLCharacter.getBaseMysticality(), KoLCharacter.getBaseMoxie()
    };
    double[][] bounds = new double[3][2];
    for (int index = 0; index < 3; index++) {
      int lowerLimit = lowerLimit(index);
      int upperLimit = this.selectedLimits[index];
      bounds[index][0] = lowerLimit == 0 ? bases[index] : Math.min(bases[index], lowerLimit);
      bounds[index][1] = upperLimit == 0 ? bases[index] : Math.min(bases[index], upperLimit);
    }
    return bounds;
  }

  private int lowerLimit(int index) {
    int selected = this.selectedLimits[index];
    int possible = this.possibleLimits[index];
    if (selected == 0) return possible;
    if (possible == 0) return selected;
    return Math.min(selected, possible);
  }

  private void applyEqualize(double[][] stats, int control, boolean allowHigh) {
    double[][] original = {stats[0].clone(), stats[1].clone(), stats[2].clone()};
    for (int index = 0; index < 3; index++) {
      stats[index] = controlled(original[index], original, control, allowHigh);
    }
  }

  private double[] controlled(double[] original, double[][] stats, int control, boolean allowHigh) {
    int options = this.selectedControls[control] | this.possibleControls[control];
    double minimum = Double.POSITIVE_INFINITY;
    double maximum = Double.NEGATIVE_INFINITY;
    if ((options & NONE) != 0) {
      minimum = Math.min(minimum, original[0]);
      maximum = Math.max(maximum, original[1]);
    }
    if ((options & MUS) != 0) {
      minimum = Math.min(minimum, stats[0][0]);
      maximum = Math.max(maximum, stats[0][1]);
    }
    if ((options & MYS) != 0) {
      minimum = Math.min(minimum, stats[1][0]);
      maximum = Math.max(maximum, stats[1][1]);
    }
    if ((options & MOX) != 0) {
      minimum = Math.min(minimum, stats[2][0]);
      maximum = Math.max(maximum, stats[2][1]);
    }
    if (allowHigh && (options & HIGH) != 0) {
      minimum = Math.min(minimum, Math.max(stats[0][0], Math.max(stats[1][0], stats[2][0])));
      maximum = Math.max(maximum, Math.max(stats[0][1], Math.max(stats[1][1], stats[2][1])));
    }
    return minimum == Double.POSITIVE_INFINITY ? original : new double[] {minimum, maximum};
  }

  private double[] statBounds(
      double[] base,
      DoubleModifier flatModifier,
      DoubleModifier percentModifier,
      double[] lower,
      double[] upper) {
    double minimum = Double.POSITIVE_INFINITY;
    double maximum = Double.NEGATIVE_INFINITY;
    // Flat and percentage modifiers can cross zero, and rounding makes interior shortcuts unsafe.
    for (double baseValue : base) {
      for (double flat : new double[] {value(lower, flatModifier), value(upper, flatModifier)}) {
        for (double percent :
            new double[] {value(lower, percentModifier), value(upper, percentModifier)}) {
          double candidate = baseValue + (int) flat + (int) Math.ceil(percent * baseValue / 100.0);
          minimum = Math.min(minimum, candidate);
          maximum = Math.max(maximum, candidate);
        }
      }
    }
    return new double[] {minimum, maximum};
  }

  private double[] floored(double[] original, double[][] stats, int control) {
    int options = this.selectedControls[control] | this.possibleControls[control];
    double minimum = (options & NONE) != 0 ? original[0] : Double.POSITIVE_INFINITY;
    double maximum = (options & NONE) != 0 ? original[1] : Double.NEGATIVE_INFINITY;
    for (int index = 0; index < 3; index++) {
      int option = index == 0 ? MUS : index == 1 ? MYS : MOX;
      if ((options & option) == 0) continue;
      minimum = Math.min(minimum, Math.max(original[0], stats[index][0]));
      maximum = Math.max(maximum, Math.max(original[1], stats[index][1]));
    }
    return minimum == Double.POSITIVE_INFINITY ? original : new double[] {minimum, maximum};
  }

  private double hpBound(
      double[] baseMuscle, double[] buffedMuscle, double[] lower, double[] upper, boolean maximum) {
    double unbuffedStat = baseMuscle[maximum ? 1 : 0];
    double flat = value(maximum ? upper : lower, DoubleModifier.HP);
    if (KoLCharacter.isVampyre()) {
      double hp = KoLCharacter.getBaseMuscle() + flat;
      return Math.max(hp, unbuffedStat);
    }
    if (KoLCharacter.inRobocore()) return 30.0 + flat;
    if (KoLCharacter.isGreyGoo()) {
      return KoLCharacter.getBaseMaxHP()
          - KoLCharacter.currentNumericModifier(DoubleModifier.HP)
          + flat;
    }
    if (KoLCharacter.inZootomist()) {
      double hp = buffedMuscle[maximum ? 1 : 0] + 3.0 + flat;
      return Math.max(hp, unbuffedStat);
    }

    double result = maximum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    double classMultiplier = KoLCharacter.isMuscleClass() ? 1.5 : 1.0;
    for (double muscle : buffedMuscle) {
      for (double percent :
          new double[] {value(lower, DoubleModifier.HP_PCT), value(upper, DoubleModifier.HP_PCT)}) {
        for (double hpFlat :
            new double[] {value(lower, DoubleModifier.HP), value(upper, DoubleModifier.HP)}) {
          double hp =
              Math.ceil((muscle + 3.0) * (classMultiplier + percent / 100.0)) + (int) hpFlat;
          double candidate = Math.max(hp, unbuffedStat);
          result = maximum ? Math.max(result, candidate) : Math.min(result, candidate);
        }
      }
    }
    return result;
  }

  private double mpBound(
      double[] baseMysticality,
      double[] buffedMysticality,
      double[] buffedMoxie,
      double[] lower,
      double[] upper,
      boolean maximum) {
    double flat = value(maximum ? upper : lower, DoubleModifier.MP);
    if (KoLCharacter.isGreyGoo()) {
      return KoLCharacter.getBaseMaxMP()
          - KoLCharacter.currentNumericModifier(DoubleModifier.MP)
          + flat;
    }

    double result = maximum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    double classMultiplier = KoLCharacter.isMysticalityClass() ? 1.5 : 1.0;
    int mpControls = this.selectedMpControls | this.possibleMpControls;
    double[] controllingStat = buffedMysticality;
    if ((mpControls & CONTROLS_MP) != 0) {
      controllingStat = union(controllingStat, buffedMoxie);
    }
    if ((mpControls & MAY_CONTROL_MP) != 0) {
      controllingStat =
          union(
              controllingStat,
              new double[] {
                Math.max(buffedMysticality[0], buffedMoxie[0]),
                Math.max(buffedMysticality[1], buffedMoxie[1])
              });
    }
    for (double stat : controllingStat) {
      for (double percent :
          new double[] {value(lower, DoubleModifier.MP_PCT), value(upper, DoubleModifier.MP_PCT)}) {
        for (double mpFlat :
            new double[] {value(lower, DoubleModifier.MP), value(upper, DoubleModifier.MP)}) {
          double mp = Math.ceil(stat * (classMultiplier + percent / 100.0)) + (int) mpFlat;
          double candidate = Math.max(mp, baseMysticality[maximum ? 1 : 0]);
          result = maximum ? Math.max(result, candidate) : Math.min(result, candidate);
        }
      }
    }
    return result;
  }

  private static double[] union(double[] first, double[] second) {
    return new double[] {Math.min(first[0], second[0]), Math.max(first[1], second[1])};
  }

  private static double resistance(
      double value, int immunities, int vulnerabilities, int bit, boolean maximum) {
    double adjusted = (vulnerabilities & (1 << bit)) != 0 ? value - 100.0 : value;
    if ((immunities & (1 << bit)) == 0) return adjusted;
    // Immunity scores as 100, but raw resistance can legitimately provide a higher upper bound.
    return maximum ? Math.max(adjusted, 100.0) : 100.0;
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

  private void adjustForDynamicItemDoubling(
      double[] lower, double[] upper, int selectedDynamics, int possibleDynamics) {
    if ((selectedDynamics | possibleDynamics) == 0) return;
    for (int index = 0; index < this.modifiers.length; index++) {
      int selected = selectedDynamics;
      int possible = possibleDynamics;
      DoubleModifier modifier = this.modifiers[index];
      if (!isDynamicModifier(selected, modifier)) selected = 0;
      if (!isDynamicModifier(possible, modifier)) possible = 0;
      if (selected != 0) {
        lower[index] *= 2.0;
        upper[index] *= 2.0;
      } else if (possible != 0) {
        // An unresolved dynamic item may leave the value unchanged or double it.
        lower[index] = Math.min(lower[index], lower[index] * 2.0);
        upper[index] = Math.max(upper[index], upper[index] * 2.0);
      }
    }
  }

  private static void merge(int[] target, int[] addition) {
    for (int index = 0; index < target.length; index++) target[index] |= addition[index];
  }

  private static void mergeLimits(int[] target, int[] addition) {
    for (int index = 0; index < target.length; index++) {
      if (addition[index] > 0 && (target[index] == 0 || addition[index] < target[index])) {
        target[index] = addition[index];
      }
    }
  }

  private static DerivedState mergeDerived(DerivedState target, DerivedState addition) {
    merge(target.controls(), addition.controls());
    mergeLimits(target.limits(), addition.limits());
    return new DerivedState(
        target.controls(), target.limits(), target.mpControls() | addition.mpControls());
  }

  private static DoubleModifier[] dependencies(List<Evaluator.NumericScoreTerm> terms) {
    var dependencies = EnumSet.noneOf(DoubleModifier.class);
    for (var term : terms) {
      if (!(term.modifier() instanceof DoubleModifier modifier)) continue;
      dependencies.add(modifier);
      switch (modifier) {
        case MUS -> dependencies.add(DoubleModifier.MUS_PCT);
        case MYS -> dependencies.add(DoubleModifier.MYS_PCT);
        case MOX -> dependencies.add(DoubleModifier.MOX_PCT);
        case HP ->
            dependencies.addAll(
                List.of(DoubleModifier.MUS, DoubleModifier.MUS_PCT, DoubleModifier.HP_PCT));
        case MP ->
            dependencies.addAll(
                List.of(
                    DoubleModifier.MYS,
                    DoubleModifier.MYS_PCT,
                    DoubleModifier.MOX,
                    DoubleModifier.MOX_PCT,
                    DoubleModifier.MP_PCT));
        case FAMILIAR_WEIGHT ->
            dependencies.addAll(
                List.of(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT, DoubleModifier.FAMILIAR_WEIGHT_PCT));
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
        case EXPERIENCE ->
            dependencies.addAll(
                List.of(
                    DoubleModifier.MONSTER_LEVEL,
                    DoubleModifier.MONSTER_LEVEL_PERCENT,
                    DoubleModifier.primeStatExp(),
                    DoubleModifier.primeStatExpPercent()));
        case DAMAGE_AURA -> dependencies.add(DoubleModifier.SPORADIC_DAMAGE_AURA);
        case THORNS -> dependencies.add(DoubleModifier.SPORADIC_THORNS);
        default -> {}
      }
    }
    return dependencies.toArray(DoubleModifier[]::new);
  }

  private static int modifierIndex(DoubleModifier[] modifiers, DoubleModifier target) {
    for (int index = 0; index < modifiers.length; index++) {
      if (modifiers[index] == target) return index;
    }
    return -1;
  }
}
