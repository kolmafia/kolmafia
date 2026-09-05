package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.maximizer.CodpiecePruning.ContributionRange;
import net.sourceforge.kolmafia.maximizer.CodpiecePruning.FamiliarScoreContributions;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;

/**
 * Tracks selected contributions and bounds every remaining score term. Positive objectives use
 * maximum marginals; negative objectives use minimum marginals.
 */
final class CodpieceScoreBound {
  /** Baseline-independent gem contributions and root orderings reused for every equipment set. */
  static final class ContributionBasis {
    private final double[][] contributions;
    private final int[][] ascendingOrders;
    private final int[][] descendingOrders;

    ContributionBasis(List<Evaluator.ScoreTerm> scoreModifiers, Modifiers[] gemModifiers) {
      this.contributions = new double[scoreModifiers.size()][];
      this.ascendingOrders = new int[scoreModifiers.size()][];
      this.descendingOrders = new int[scoreModifiers.size()][];
      for (int modifierIndex = 0; modifierIndex < scoreModifiers.size(); modifierIndex++) {
        DoubleModifier modifier = scoreModifiers.get(modifierIndex).modifier();
        if (modifier == DoubleModifier.MUS
            || modifier == DoubleModifier.MYS
            || modifier == DoubleModifier.MOX
            || modifier == DoubleModifier.HP
            || modifier == DoubleModifier.MP
            || CodpiecePruning.isExperienceScoreModifier(modifier)
            || CodpiecePruning.isSpecialFoldScoreModifier(modifier)) {
          continue;
        }
        double[] modifierContributions = new double[gemModifiers.length];
        for (int gemIndex = 0; gemIndex < gemModifiers.length; gemIndex++) {
          Modifiers modifiers = gemModifiers[gemIndex];
          if (modifiers != null) {
            modifierContributions[gemIndex] = contribution(modifier, modifiers);
          }
        }
        this.contributions[modifierIndex] = modifierContributions;
        this.ascendingOrders[modifierIndex] = contributionOrder(modifierContributions, false);
        this.descendingOrders[modifierIndex] = contributionOrder(modifierContributions, true);
      }
    }

    private static int[] contributionOrder(double[] contributions, boolean descending) {
      Integer[] indexes = new Integer[contributions.length];
      for (int i = 0; i < indexes.length; i++) {
        indexes[i] = i;
      }
      Arrays.sort(
          indexes,
          (left, right) ->
              descending
                  ? Double.compare(contributions[right], contributions[left])
                  : Double.compare(contributions[left], contributions[right]));
      return Arrays.stream(indexes).mapToInt(Integer::intValue).toArray();
    }

    private static double contribution(DoubleModifier modifier, Modifiers modifiers) {
      return switch (modifier) {
        case FAMILIAR_WEIGHT -> modifiers.getDouble(DoubleModifier.FAMILIAR_WEIGHT);
        case COMBAT_RATE -> modifiers.getDouble(DoubleModifier.RAW_COMBAT_RATE);
        case MANA_COST ->
            modifiers.getDouble(DoubleModifier.MANA_COST)
                + modifiers.getDouble(DoubleModifier.STACKABLE_MANA_COST);
        case ITEMDROP ->
            modifiers.getDouble(DoubleModifier.ITEMDROP)
                + Math.min(0.0, modifiers.getDouble(DoubleModifier.ITEMDROP_PENALTY))
                + modifiers.getDouble(DoubleModifier.SPORADIC_ITEMDROP);
        case MEATDROP ->
            modifiers.getDouble(DoubleModifier.MEATDROP)
                + Math.min(0.0, modifiers.getDouble(DoubleModifier.MEATDROP_PENALTY))
                + modifiers.getDouble(DoubleModifier.SPORADIC_MEATDROP)
                + modifiers.getDouble(DoubleModifier.MEAT_BONUS) / 10000.0;
        case WEAPON_DAMAGE ->
            modifiers.getDouble(DoubleModifier.WEAPON_DAMAGE)
                + modifiers.getDouble(DoubleModifier.WEAPON_DAMAGE_PCT);
        case RANGED_DAMAGE ->
            modifiers.getDouble(DoubleModifier.RANGED_DAMAGE)
                + modifiers.getDouble(DoubleModifier.RANGED_DAMAGE_PCT);
        case SPELL_DAMAGE ->
            modifiers.getDouble(DoubleModifier.SPELL_DAMAGE)
                + modifiers.getDouble(DoubleModifier.SPELL_DAMAGE_PCT);
        case DAMAGE_AURA ->
            modifiers.getDouble(DoubleModifier.DAMAGE_AURA)
                + modifiers.getDouble(DoubleModifier.SPORADIC_DAMAGE_AURA);
        case THORNS ->
            modifiers.getDouble(DoubleModifier.THORNS)
                + modifiers.getDouble(DoubleModifier.SPORADIC_THORNS);
        default -> modifiers.getDouble(modifier);
      };
    }
  }

  private final List<Evaluator.ScoreTerm> scoreModifiers;
  private final double[] baseline;
  private final double[][] contributions;
  private final int[][] ascendingRootContributionOrders;
  private final int[][] descendingRootContributionOrders;
  private final int[] initialRemaining;
  private final int slotCount;
  private final boolean needsMaximumSuffix;
  // All suffix families are built together; estimators assume they are populated together.
  private double[][][] suffixContributions;
  private double[][][] maximumSuffixContributions;
  private final double[] selectedContributions;
  private final boolean[] jointModifiers;
  private final double[] jointContributions;
  private double[][] jointSuffixContributions;
  private final double[] itemContributions;
  private double[][] itemSuffixContributions;
  private double selectedJointContribution;
  private double selectedItemContribution;
  private double jointBaseline;
  private final double totalMin;
  private final double fixedScore;
  private final boolean exact;
  private final boolean canMeetHardRequirements;

  CodpieceScoreBound(
      List<Evaluator.ScoreTerm> scoreModifiers,
      ContributionBasis contributionBasis,
      Modifiers baseline,
      Modifiers[] gemModifiers,
      int[] remaining,
      int slotCount,
      double totalMin,
      double fixedScore,
      boolean exact,
      boolean combineContributions,
      double[] itemContributions,
      FamiliarScoreContributions familiarScoreContributions,
      boolean canMeetHardRequirements) {
    this.scoreModifiers = scoreModifiers;
    this.baseline = new double[scoreModifiers.size()];
    this.contributions = new double[scoreModifiers.size()][];
    this.ascendingRootContributionOrders = contributionBasis.ascendingOrders.clone();
    this.descendingRootContributionOrders = contributionBasis.descendingOrders.clone();
    this.initialRemaining = remaining.clone();
    this.slotCount = slotCount;
    this.needsMaximumSuffix =
        totalMin != Double.NEGATIVE_INFINITY
            || scoreModifiers.stream()
                .anyMatch(modifier -> modifier.min() != Double.NEGATIVE_INFINITY);
    this.selectedContributions = new double[scoreModifiers.size()];
    this.totalMin = totalMin;
    this.fixedScore = fixedScore;
    this.canMeetHardRequirements = canMeetHardRequirements;
    this.exact =
        exact
            && (familiarScoreContributions == null || familiarScoreContributions.isEmpty())
            && scoreModifiers.stream()
                .noneMatch(
                    modifier ->
                        modifier.modifier() == DoubleModifier.MUS
                            || modifier.modifier() == DoubleModifier.MYS
                            || modifier.modifier() == DoubleModifier.MOX
                            || modifier.modifier() == DoubleModifier.HP
                            || modifier.modifier() == DoubleModifier.MP
                            || CodpiecePruning.isExperienceScoreModifier(modifier.modifier())
                            || CodpiecePruning.isSpecialFoldScoreModifier(modifier.modifier()));
    this.jointModifiers = combineContributions ? new boolean[scoreModifiers.size()] : null;
    this.jointContributions = combineContributions ? new double[gemModifiers.length] : null;
    boolean hasItemContributions = Arrays.stream(itemContributions).anyMatch(value -> value != 0.0);
    this.itemContributions = hasItemContributions ? itemContributions : null;

    // Establish conservative derived-stat inputs before evaluating individual score terms.
    Map<DerivedModifier, Integer> predicted = null;
    boolean needsDerivedStatBounds =
        scoreModifiers.stream()
            .map(Evaluator.ScoreTerm::modifier)
            .anyMatch(
                modifier ->
                    modifier == DoubleModifier.MUS
                        || modifier == DoubleModifier.MYS
                        || modifier == DoubleModifier.MOX
                        || modifier == DoubleModifier.HP
                        || modifier == DoubleModifier.MP);
    double baseStatUpperBound =
        needsDerivedStatBounds
            ? Math.max(
                KoLCharacter.getBaseMuscle(),
                Math.max(KoLCharacter.getBaseMysticality(), KoLCharacter.getBaseMoxie()))
            : 0.0;
    double maximumHitPointBase = 0.0;
    double maximumHitPointMultiplier = 0.0;
    double maximumManaPointBase = 0.0;
    double maximumManaPointMultiplier = 0.0;
    if (needsDerivedStatBounds) {
      double[] maximumMuscleContributions = new double[slotCount];
      double[] maximumHitPointPercentContributions = new double[slotCount];
      double[] maximumMysticalityContributions = new double[slotCount];
      double[] maximumMoxieContributions = new double[slotCount];
      double[] maximumManaPointPercentContributions = new double[slotCount];
      for (int gemIndex = 0; gemIndex < gemModifiers.length; gemIndex++) {
        Modifiers modifiers = gemModifiers[gemIndex];
        if (modifiers == null) {
          continue;
        }
        double muscleContribution =
            derivedStatContribution(
                modifiers, DoubleModifier.MUS, DoubleModifier.MUS_PCT, baseStatUpperBound);
        double hitPointPercent = Math.max(0.0, modifiers.getDouble(DoubleModifier.HP_PCT));
        double mysticalityContribution =
            derivedStatContribution(
                modifiers, DoubleModifier.MYS, DoubleModifier.MYS_PCT, baseStatUpperBound);
        double moxieContribution =
            derivedStatContribution(
                modifiers, DoubleModifier.MOX, DoubleModifier.MOX_PCT, baseStatUpperBound);
        double manaPointPercent = Math.max(0.0, modifiers.getDouble(DoubleModifier.MP_PCT));
        for (int copy = 0; copy < Math.min(remaining[gemIndex], slotCount); copy++) {
          insertContribution(maximumMuscleContributions, muscleContribution, true);
          insertContribution(maximumHitPointPercentContributions, hitPointPercent, true);
          insertContribution(maximumMysticalityContributions, mysticalityContribution, true);
          insertContribution(maximumMoxieContributions, moxieContribution, true);
          insertContribution(maximumManaPointPercentContributions, manaPointPercent, true);
        }
      }
      double maximumBuffedMuscle =
          baseline.predict().get(DerivedModifier.BUFFED_MUS)
              + Arrays.stream(maximumMuscleContributions).sum();
      maximumHitPointBase = Math.max(0.0, maximumBuffedMuscle + 3.0);
      maximumHitPointMultiplier =
          Math.max(
              0.0,
              (KoLCharacter.isMuscleClass() ? 1.5 : 1.0)
                  + (baseline.getDouble(DoubleModifier.HP_PCT)
                          + Arrays.stream(maximumHitPointPercentContributions).sum())
                      / 100.0);
      double maximumMysticality =
          baseline.predict().get(DerivedModifier.BUFFED_MYS)
              + Arrays.stream(maximumMysticalityContributions).sum();
      double maximumMoxie =
          baseline.predict().get(DerivedModifier.BUFFED_MOX)
              + Arrays.stream(maximumMoxieContributions).sum();
      maximumManaPointBase = Math.max(0.0, Math.max(maximumMysticality, maximumMoxie));
      maximumManaPointMultiplier =
          Math.max(
              0.0,
              (KoLCharacter.isMysticalityClass() ? 1.5 : 1.0)
                  + (baseline.getDouble(DoubleModifier.MP_PCT)
                          + Arrays.stream(maximumManaPointPercentContributions).sum())
                      / 100.0);
    }
    // Experience is nonlinear, so its marginal contribution depends on the selected gems.
    ContributionRange[][] experienceMarginals = new ContributionRange[scoreModifiers.size()][];
    for (int modifierIndex = 0; modifierIndex < scoreModifiers.size(); modifierIndex++) {
      DoubleModifier modifier = scoreModifiers.get(modifierIndex).modifier();
      if (CodpiecePruning.isExperienceScoreModifier(modifier)) {
        experienceMarginals[modifierIndex] =
            experienceMarginals(modifier, baseline, gemModifiers, remaining, slotCount);
      }
    }
    // Assemble each term's directional contribution from its supported calculation model.
    for (int modifierIndex = 0; modifierIndex < scoreModifiers.size(); modifierIndex++) {
      Evaluator.ScoreTerm scoreModifier = scoreModifiers.get(modifierIndex);
      DoubleModifier modifier = scoreModifier.modifier();
      double[] basisContributions = contributionBasis.contributions[modifierIndex];
      this.contributions[modifierIndex] =
          basisContributions == null ? new double[gemModifiers.length] : basisContributions;
      // A zero-weight term can still have a minimum, so build its upper contribution bound.
      boolean descending = scoreModifier.weight() >= 0.0;
      if ((modifier == DoubleModifier.MUS
              || modifier == DoubleModifier.MYS
              || modifier == DoubleModifier.MOX
              || modifier == DoubleModifier.HP
              || modifier == DoubleModifier.MP)
          && predicted == null) {
        predicted = baseline.predict();
      }
      this.baseline[modifierIndex] =
          (CodpiecePruning.isExperienceScoreModifier(modifier)
                  ? experienceValueAfterSuffix(modifier, baseline)
                  : Evaluator.scoreValue(modifier, baseline, predicted))
              + (familiarScoreContributions == null
                  ? 0.0
                  : familiarScoreContributions.baseline(modifier, descending));

      boolean copiedBasis = false;
      for (int gemIndex = 0; gemIndex < gemModifiers.length; gemIndex++) {
        Modifiers modifiers = gemModifiers[gemIndex];
        if (modifiers != null) {
          Double familiarBound =
              familiarScoreContributions == null
                  ? null
                  : familiarScoreContributions.bound(modifier, gemIndex, descending);
          if (familiarBound != null) {
            if (!copiedBasis && basisContributions != null) {
              this.contributions[modifierIndex] = basisContributions.clone();
              this.ascendingRootContributionOrders[modifierIndex] = null;
              this.descendingRootContributionOrders[modifierIndex] = null;
              copiedBasis = true;
            }
            this.contributions[modifierIndex][gemIndex] = familiarBound;
          } else if (basisContributions == null) {
            this.contributions[modifierIndex][gemIndex] =
                switch (modifier) {
                  case MUS ->
                      derivedStatContribution(
                          modifiers,
                          DoubleModifier.MUS,
                          DoubleModifier.MUS_PCT,
                          baseStatUpperBound);
                  case MYS ->
                      derivedStatContribution(
                          modifiers,
                          DoubleModifier.MYS,
                          DoubleModifier.MYS_PCT,
                          baseStatUpperBound);
                  case MOX ->
                      derivedStatContribution(
                          modifiers,
                          DoubleModifier.MOX,
                          DoubleModifier.MOX_PCT,
                          baseStatUpperBound);
                  case HP ->
                      hitPointContribution(
                          modifiers,
                          baseStatUpperBound,
                          maximumHitPointBase,
                          maximumHitPointMultiplier);
                  case MP ->
                      manaPointContribution(
                          modifiers,
                          baseStatUpperBound,
                          maximumManaPointBase,
                          maximumManaPointMultiplier);
                  case EXPERIENCE, MUS_EXPERIENCE, MYS_EXPERIENCE, MOX_EXPERIENCE ->
                      descending
                          ? experienceMarginals[modifierIndex][gemIndex].maximum()
                          : experienceMarginals[modifierIndex][gemIndex].minimum();
                  case FAMILIAR_WEIGHT_PCT ->
                      descending
                          ? 0.0
                          : Math.min(0.0, modifiers.getDouble(DoubleModifier.FAMILIAR_WEIGHT_PCT));
                  case MUS_LIMIT, MYS_LIMIT, MOX_LIMIT ->
                      limitContribution(modifier, baseline, modifiers, descending);
                  case PRISMATIC_DAMAGE ->
                      foldedContribution(
                          modifiers,
                          descending,
                          DoubleModifier.COLD_DAMAGE,
                          DoubleModifier.HOT_DAMAGE,
                          DoubleModifier.SLEAZE_DAMAGE,
                          DoubleModifier.SPOOKY_DAMAGE,
                          DoubleModifier.STENCH_DAMAGE);
                  case HAT_PANTS_DROP, MAXIMUM_HP_MP, ALL_ATTRIBUTES, ALL_ATTRIBUTES_PCT ->
                      baseline.hasDoubleModifier(candidate -> candidate == modifier)
                              && baseline.getRawDouble(modifier) != 0.0
                          ? 0.0
                          : combinedContributionBound(
                              baseline,
                              gemModifiers,
                              remaining,
                              slotCount,
                              descending,
                              modifier.getSubsumed());
                  default -> throw new IllegalStateException("Unexpected contribution");
                };
          }
        }
      }
    }
    // Additive terms can share a tighter joint bound than separate per-term ceilings.
    if (combineContributions) {
      for (int modifierIndex = 0; modifierIndex < scoreModifiers.size(); modifierIndex++) {
        DoubleModifier modifier = scoreModifiers.get(modifierIndex).modifier();
        boolean joint =
            modifier != DoubleModifier.MUS
                && modifier != DoubleModifier.MYS
                && modifier != DoubleModifier.MOX
                && modifier != DoubleModifier.HP
                && modifier != DoubleModifier.MP
                && !CodpiecePruning.isExperienceScoreModifier(modifier);
        this.jointModifiers[modifierIndex] = joint;
        if (!joint) {
          continue;
        }
        double weight = scoreModifiers.get(modifierIndex).weight();
        this.jointBaseline += weight * this.baseline[modifierIndex];
        for (int gemIndex = 0; gemIndex < gemModifiers.length; gemIndex++) {
          this.jointContributions[gemIndex] += weight * this.contributions[modifierIndex][gemIndex];
        }
      }
    }
  }

  private void buildSuffixes() {
    if (this.suffixContributions != null) {
      return;
    }
    int gemCount = this.initialRemaining.length;
    this.suffixContributions = new double[this.scoreModifiers.size()][gemCount + 1][this.slotCount];
    if (this.needsMaximumSuffix) {
      this.maximumSuffixContributions =
          new double[this.scoreModifiers.size()][gemCount + 1][this.slotCount];
    }
    for (int modifierIndex = 0; modifierIndex < this.scoreModifiers.size(); modifierIndex++) {
      buildSuffix(
          this.suffixContributions[modifierIndex],
          this.contributions[modifierIndex],
          this.initialRemaining,
          this.slotCount,
          this.scoreModifiers.get(modifierIndex).weight() > 0.0);
      if (this.maximumSuffixContributions != null) {
        buildSuffix(
            this.maximumSuffixContributions[modifierIndex],
            this.contributions[modifierIndex],
            this.initialRemaining,
            this.slotCount,
            true);
      }
    }
    if (this.itemContributions != null) {
      this.itemSuffixContributions = new double[gemCount + 1][this.slotCount];
      buildSuffix(
          this.itemSuffixContributions,
          this.itemContributions,
          this.initialRemaining,
          this.slotCount,
          true);
    }
    if (this.jointContributions != null) {
      this.jointSuffixContributions = new double[gemCount + 1][this.slotCount];
      buildSuffix(
          this.jointSuffixContributions,
          this.jointContributions,
          this.initialRemaining,
          this.slotCount,
          true);
    }
  }

  private static void buildSuffix(
      double[][] suffix,
      double[] contributions,
      int[] remaining,
      int slotCount,
      boolean descending) {
    for (int start = contributions.length - 1; start >= 0; start--) {
      double[] best = suffix[start];
      System.arraycopy(suffix[start + 1], 0, best, 0, slotCount);
      double contribution = beneficialContribution(contributions[start], descending);
      for (int copy = 0; copy < Math.min(remaining[start], slotCount); copy++) {
        insertContribution(best, contribution, descending);
      }
    }
  }

  private static double derivedStatContribution(
      Modifiers modifiers,
      DoubleModifier flatModifier,
      DoubleModifier percentModifier,
      double baseStatUpperBound) {
    return Math.ceil(Math.max(0.0, modifiers.getDouble(flatModifier)))
        + Math.ceil(
            Math.max(0.0, modifiers.getDouble(percentModifier)) * baseStatUpperBound / 100.0);
  }

  private static double limitContribution(
      DoubleModifier modifier, Modifiers baseline, Modifiers gem, boolean maximum) {
    double current = baseline.getDouble(modifier);
    double candidate = gem.getDouble(modifier);
    if (candidate <= 0.0) {
      return 0.0;
    }
    if (maximum) {
      return current == 0.0 ? candidate : 0.0;
    }
    return current == 0.0 ? 0.0 : Math.min(0.0, candidate - current);
  }

  private static double foldedContribution(
      Modifiers modifiers, boolean maximum, DoubleModifier... components) {
    double contribution = maximum ? Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY;
    for (DoubleModifier component : components) {
      contribution =
          maximum
              ? Math.max(contribution, modifiers.getDouble(component))
              : Math.min(contribution, modifiers.getDouble(component));
    }
    return contribution;
  }

  private static double combinedContributionBound(
      Modifiers baseline,
      Modifiers[] gemModifiers,
      int[] remaining,
      int slotCount,
      boolean maximum,
      DoubleModifier... components) {
    double minimumLower = Double.POSITIVE_INFINITY;
    double minimumUpper = Double.POSITIVE_INFINITY;
    for (DoubleModifier component : components) {
      double[] lowest = new double[slotCount];
      double[] highest = new double[slotCount];
      for (int gemIndex = 0; gemIndex < gemModifiers.length; gemIndex++) {
        Modifiers modifiers = gemModifiers[gemIndex];
        if (modifiers == null) {
          continue;
        }
        double contribution = modifiers.getDouble(component);
        for (int copy = 0; copy < Math.min(remaining[gemIndex], slotCount); copy++) {
          insertContribution(lowest, contribution, false);
          insertContribution(highest, contribution, true);
        }
      }
      double componentBaseline = baseline.getDouble(component);
      minimumLower = Math.min(minimumLower, componentBaseline + Arrays.stream(lowest).sum());
      minimumUpper = Math.min(minimumUpper, componentBaseline + Arrays.stream(highest).sum());
    }

    // Combined modifiers become zero when their components straddle zero.
    double lower = Math.min(0.0, minimumLower);
    double upper = Math.max(0.0, minimumUpper);
    return maximum ? upper - lower : lower - upper;
  }

  private static double hitPointContribution(
      Modifiers modifiers,
      double baseStatUpperBound,
      double maximumBase,
      double maximumMultiplier) {
    return Math.ceil(Math.max(0.0, modifiers.getDouble(DoubleModifier.HP)))
        + Math.ceil(
            derivedStatContribution(
                    modifiers, DoubleModifier.MUS, DoubleModifier.MUS_PCT, baseStatUpperBound)
                * maximumMultiplier)
        + Math.ceil(
            Math.max(0.0, modifiers.getDouble(DoubleModifier.HP_PCT)) * maximumBase / 100.0);
  }

  private static double manaPointContribution(
      Modifiers modifiers,
      double baseStatUpperBound,
      double maximumBase,
      double maximumMultiplier) {
    return Math.ceil(Math.max(0.0, modifiers.getDouble(DoubleModifier.MP)))
        + Math.ceil(
            Math.max(
                    derivedStatContribution(
                        modifiers, DoubleModifier.MYS, DoubleModifier.MYS_PCT, baseStatUpperBound),
                    derivedStatContribution(
                        modifiers, DoubleModifier.MOX, DoubleModifier.MOX_PCT, baseStatUpperBound))
                * maximumMultiplier)
        + Math.ceil(
            Math.max(0.0, modifiers.getDouble(DoubleModifier.MP_PCT)) * maximumBase / 100.0);
  }

  private static ContributionRange[] experienceMarginals(
      DoubleModifier scoreModifier,
      Modifiers baseline,
      Modifiers[] gemModifiers,
      int[] remaining,
      int slotCount) {
    // Experience inputs interact, so measure each gem in every reachable partial configuration.
    var relevant = new ArrayList<Integer>();
    for (int i = 0; i < gemModifiers.length; i++) {
      if (CodpiecePruning.affectsExperience(gemModifiers[i], scoreModifier)) {
        relevant.add(i);
      }
    }
    ContributionRange[] marginals = new ContributionRange[gemModifiers.length];
    Arrays.setAll(
        marginals,
        index ->
            CodpiecePruning.affectsExperience(gemModifiers[index], scoreModifier)
                ? new ContributionRange(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)
                : new ContributionRange(0.0, 0.0));
    collectExperienceMarginals(
        scoreModifier,
        baseline,
        gemModifiers,
        remaining,
        relevant,
        new int[gemModifiers.length],
        marginals,
        0,
        0,
        slotCount);
    return marginals;
  }

  private static void collectExperienceMarginals(
      DoubleModifier scoreModifier,
      Modifiers current,
      Modifiers[] gemModifiers,
      int[] remaining,
      List<Integer> relevant,
      int[] used,
      ContributionRange[] marginals,
      int start,
      int selected,
      int slotCount) {
    double currentExperience = experienceValueAfterSuffix(scoreModifier, current);
    for (int gemIndex : relevant) {
      if (used[gemIndex] >= remaining[gemIndex]) {
        continue;
      }
      var next = new Modifiers(current);
      CodpiecePruning.addExperienceInputs(next, gemModifiers[gemIndex]);
      marginals[gemIndex] =
          marginals[gemIndex].include(
              experienceValueAfterSuffix(scoreModifier, next) - currentExperience);
    }
    if (selected + 1 >= slotCount) {
      return;
    }
    for (int relevantIndex = start; relevantIndex < relevant.size(); relevantIndex++) {
      int gemIndex = relevant.get(relevantIndex);
      if (used[gemIndex] >= remaining[gemIndex]) {
        continue;
      }
      var next = new Modifiers(current);
      CodpiecePruning.addExperienceInputs(next, gemModifiers[gemIndex]);
      used[gemIndex]++;
      collectExperienceMarginals(
          scoreModifier,
          next,
          gemModifiers,
          remaining,
          relevant,
          used,
          marginals,
          relevantIndex,
          selected + 1,
          slotCount);
      used[gemIndex]--;
    }
  }

  void select(int gemIndex) {
    this.buildSuffixes();
    for (int modifierIndex = 0;
        modifierIndex < this.selectedContributions.length;
        modifierIndex++) {
      this.selectedContributions[modifierIndex] += this.contributions[modifierIndex][gemIndex];
    }
    if (this.jointContributions != null) {
      this.selectedJointContribution += this.jointContributions[gemIndex];
    }
    if (this.itemContributions != null) {
      this.selectedItemContribution += this.itemContributions[gemIndex];
    }
  }

  void deselect(int gemIndex) {
    for (int modifierIndex = 0;
        modifierIndex < this.selectedContributions.length;
        modifierIndex++) {
      this.selectedContributions[modifierIndex] -= this.contributions[modifierIndex][gemIndex];
    }
    if (this.jointContributions != null) {
      this.selectedJointContribution -= this.jointContributions[gemIndex];
    }
    if (this.itemContributions != null) {
      this.selectedItemContribution -= this.itemContributions[gemIndex];
    }
  }

  double estimate(int start, int[] remaining, int remainingSlots) {
    double score =
        this.fixedScore
            + this.selectedItemContribution
            + this.estimateAdditional(
                this.itemSuffixContributions,
                this.itemContributions,
                start,
                remaining,
                remainingSlots,
                true);
    for (int modifierIndex = 0; modifierIndex < this.scoreModifiers.size(); modifierIndex++) {
      Evaluator.ScoreTerm scoreModifier = this.scoreModifiers.get(modifierIndex);
      boolean descending = scoreModifier.weight() > 0.0;
      double value =
          this.estimateValue(
              modifierIndex,
              start,
              remaining,
              remainingSlots,
              descending,
              this.suffixContributions);
      score += scoreModifier.weight() * Math.min(value, scoreModifier.max());
    }
    if (this.jointContributions != null) {
      score = Math.min(score, this.estimateJoint(start, remaining, remainingSlots));
    }
    return score;
  }

  private double estimateJoint(int start, int[] remaining, int remainingSlots) {
    double score =
        this.fixedScore
            + this.selectedItemContribution
            + this.estimateAdditional(
                this.itemSuffixContributions,
                this.itemContributions,
                start,
                remaining,
                remainingSlots,
                true)
            + this.jointBaseline
            + this.selectedJointContribution;
    for (int modifierIndex = 0; modifierIndex < this.scoreModifiers.size(); modifierIndex++) {
      if (this.jointModifiers[modifierIndex]) {
        continue;
      }
      Evaluator.ScoreTerm scoreModifier = this.scoreModifiers.get(modifierIndex);
      double value =
          this.estimateValue(
              modifierIndex,
              start,
              remaining,
              remainingSlots,
              scoreModifier.weight() > 0.0,
              this.suffixContributions);
      score += scoreModifier.weight() * Math.min(value, scoreModifier.max());
    }
    if (remainingSlots == 0 || start >= remaining.length) {
      return score;
    }

    double current = Math.max(0.0, this.jointContributions[start]);
    int currentCopies = Math.min(remaining[start], remainingSlots);
    if (this.jointSuffixContributions == null) {
      return score
          + directAdditional(this.jointContributions, start, remaining, remainingSlots, true);
    }
    double[] later = this.jointSuffixContributions[start + 1];
    int laterIndex = 0;
    for (int slot = 0; slot < remainingSlots; slot++) {
      if (currentCopies > 0 && (laterIndex >= later.length || current > later[laterIndex])) {
        score += current;
        currentCopies--;
      } else {
        score += later[laterIndex++];
      }
    }
    return score;
  }

  private double estimateAdditional(
      double[][] suffixContributions,
      double[] contributions,
      int start,
      int[] remaining,
      int remainingSlots,
      boolean descending) {
    if (contributions == null || remainingSlots == 0 || start >= remaining.length) {
      return 0.0;
    }
    if (suffixContributions == null) {
      return directAdditional(contributions, start, remaining, remainingSlots, descending);
    }
    double score = 0.0;
    for (int i = 0; i < remainingSlots; i++) {
      score += suffixContributions[start][i];
    }
    return score;
  }

  private static double directAdditional(
      double[] contributions, int start, int[] remaining, int remainingSlots, boolean descending) {
    double[] best = new double[remainingSlots];
    for (int index = start; index < contributions.length; index++) {
      double contribution = beneficialContribution(contributions[index], descending);
      for (int copy = 0; copy < Math.min(remaining[index], remainingSlots); copy++) {
        insertContribution(best, contribution, descending);
      }
    }
    return Arrays.stream(best).sum();
  }

  boolean canMeetMinimum(int start, int[] remaining, int remainingSlots, double scoreUpperBound) {
    if (!this.canMeetHardRequirements) {
      return false;
    }
    if (!this.needsMaximumSuffix) {
      return true;
    }
    if (scoreUpperBound < this.totalMin) {
      return false;
    }
    for (int modifierIndex = 0; modifierIndex < this.scoreModifiers.size(); modifierIndex++) {
      Evaluator.ScoreTerm scoreModifier = this.scoreModifiers.get(modifierIndex);
      double minimum = scoreModifier.min();
      if (minimum == Double.NEGATIVE_INFINITY || scoreModifier.weight() < 0.0) {
        continue;
      }
      double maximum =
          this.estimateValue(
              modifierIndex,
              start,
              remaining,
              remainingSlots,
              true,
              this.maximumSuffixContributions);
      if (maximum < minimum) {
        return false;
      }
    }
    return true;
  }

  boolean isScoreSaturated(int start, int[] remaining, double scoreUpperBound) {
    return this.exact && Double.compare(scoreUpperBound, this.estimate(start, remaining, 0)) == 0;
  }

  private double estimateValue(
      int modifierIndex,
      int start,
      int[] remaining,
      int remainingSlots,
      boolean descending,
      double[][][] suffixContributions) {
    double value = this.baseline[modifierIndex] + this.selectedContributions[modifierIndex];
    if (remainingSlots == 0 || start >= remaining.length) {
      return value;
    }

    if (suffixContributions == null) {
      int[] rootOrder =
          start != 0
              ? null
              : descending
                  ? this.descendingRootContributionOrders[modifierIndex]
                  : this.ascendingRootContributionOrders[modifierIndex];
      return value
          + (rootOrder == null
              ? directAdditional(
                  this.contributions[modifierIndex], start, remaining, remainingSlots, descending)
              : orderedAdditional(
                  rootOrder,
                  this.contributions[modifierIndex],
                  remaining,
                  remainingSlots,
                  descending));
    }
    double current = beneficialContribution(this.contributions[modifierIndex][start], descending);
    int currentCopies = Math.min(remaining[start], remainingSlots);
    double[] later = suffixContributions[modifierIndex][start + 1];
    int laterIndex = 0;
    for (int slot = 0; slot < remainingSlots; slot++) {
      if (currentCopies > 0
          && (laterIndex >= later.length || isBetter(current, later[laterIndex], descending))) {
        value += current;
        currentCopies--;
      } else {
        value += later[laterIndex++];
      }
    }
    return value;
  }

  private static double beneficialContribution(double contribution, boolean descending) {
    return descending ? Math.max(0.0, contribution) : Math.min(0.0, contribution);
  }

  private static double orderedAdditional(
      int[] order,
      double[] contributions,
      int[] remaining,
      int remainingSlots,
      boolean descending) {
    double score = 0.0;
    for (int index : order) {
      double contribution = contributions[index];
      if (descending ? contribution <= 0.0 : contribution >= 0.0) {
        break;
      }
      int copies = Math.min(remaining[index], remainingSlots);
      score += contribution * copies;
      remainingSlots -= copies;
      if (remainingSlots == 0) {
        break;
      }
    }
    return score;
  }

  private static boolean isBetter(double candidate, double existing, boolean descending) {
    return descending ? candidate > existing : candidate < existing;
  }

  private static void insertContribution(
      double[] contributions, double contribution, boolean descending) {
    for (int i = 0; i < contributions.length; i++) {
      if (!isBetter(contribution, contributions[i], descending)) {
        continue;
      }
      System.arraycopy(contributions, i, contributions, i + 1, contributions.length - i - 1);
      contributions[i] = contribution;
      return;
    }
  }

  private static double experienceValueAfterSuffix(
      DoubleModifier scoreModifier, Modifiers modifiers) {
    if (scoreModifier != DoubleModifier.EXPERIENCE) {
      double statExperience = modifiers.getDouble(scoreModifier);
      double generalExperience = modifiers.getDouble(DoubleModifier.EXPERIENCE);
      if (generalExperience == 0.0) {
        return statExperience;
      }
      double baseExperience =
          KoLCharacter.estimatedBaseExp(modifiers.getDouble(DoubleModifier.MONSTER_LEVEL));
      if (KoLCharacter.inTheSource()) {
        baseExperience /= 3.0;
        generalExperience /= 3.0;
      }
      return statExperience
          + statExperienceBonus(scoreModifier, modifiers)
          + statExperienceDistribution(scoreModifier, modifiers)
              * (baseExperience + generalExperience)
              * (1.0 + modifiers.getDouble(experiencePercent(scoreModifier)) / 100.0);
    }

    double monsterLevel = modifiers.getDouble(DoubleModifier.MONSTER_LEVEL);
    double experienceMultiplier =
        1.0 + modifiers.getDouble(DoubleModifier.primeStatExpPercent()) / 100.0;
    double primeStatExperience = modifiers.getDouble(DoubleModifier.primeStatExp());
    double generalExperience = modifiers.getDouble(DoubleModifier.EXPERIENCE);
    if (generalExperience != 0.0) {
      double baseExperience = KoLCharacter.estimatedBaseExp(monsterLevel);
      if (KoLCharacter.inTheSource()) {
        baseExperience /= 3.0;
        generalExperience /= 3.0;
      }
      primeStatExperience +=
          primeStatExperienceBonus(modifiers)
              + primeStatExperienceDistribution(modifiers)
                  * (baseExperience + generalExperience)
                  * experienceMultiplier;
    }
    double scoreBaseExperience =
        KoLCharacter.estimatedBaseExp(
            monsterLevel
                * (1.0 + modifiers.getDouble(DoubleModifier.MONSTER_LEVEL_PERCENT) / 100.0));
    return (scoreBaseExperience + primeStatExperience) * experienceMultiplier / 2.0;
  }

  static double primeStatExperienceDistribution(Modifiers modifiers) {
    return statExperienceDistribution(DoubleModifier.primeStatExp(), modifiers);
  }

  static double statExperienceDistribution(DoubleModifier scoreModifier, Modifiers modifiers) {
    int primeStat = KoLCharacter.getPrimeIndex();
    String tuning = modifiers.getString(StringModifier.STAT_TUNING);
    int tunedStat =
        tuning.startsWith("Muscle")
            ? 0
            : tuning.startsWith("Mysticality") ? 1 : tuning.startsWith("Moxie") ? 2 : primeStat;
    int scoreStat = experienceStat(scoreModifier);
    if (tuning.endsWith("(all)")) {
      return tunedStat == scoreStat ? 1.0 : 0.0;
    }
    return tunedStat == scoreStat ? 0.5 : 0.25;
  }

  private static double primeStatExperienceBonus(Modifiers modifiers) {
    return statExperienceBonus(DoubleModifier.primeStatExp(), modifiers);
  }

  private static double statExperienceBonus(DoubleModifier scoreModifier, Modifiers modifiers) {
    String tuning = modifiers.getString(StringModifier.STAT_TUNING);
    if (tuning.endsWith("(all)")) {
      return statExperienceDistribution(scoreModifier, modifiers) == 1.0 ? 1.0 : 0.0;
    }
    return statExperienceDistribution(scoreModifier, modifiers) == 0.5 ? 1.0 : 0.0;
  }

  static DoubleModifier experiencePercent(DoubleModifier experience) {
    return switch (experience) {
      case MUS_EXPERIENCE -> DoubleModifier.MUS_EXPERIENCE_PCT;
      case MYS_EXPERIENCE -> DoubleModifier.MYS_EXPERIENCE_PCT;
      case MOX_EXPERIENCE -> DoubleModifier.MOX_EXPERIENCE_PCT;
      default ->
          throw new IllegalArgumentException("Not a stat Experience modifier: " + experience);
    };
  }

  private static int experienceStat(DoubleModifier experience) {
    return switch (experience) {
      case MUS_EXPERIENCE -> 0;
      case MYS_EXPERIENCE -> 1;
      case MOX_EXPERIENCE -> 2;
      default ->
          throw new IllegalArgumentException("Not a stat Experience modifier: " + experience);
    };
  }
}
