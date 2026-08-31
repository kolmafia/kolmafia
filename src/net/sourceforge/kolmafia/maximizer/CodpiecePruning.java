package net.sourceforge.kolmafia.maximizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;

final class CodpiecePruning {
  record FamiliarScoreContributions(int gemIndex, Map<DoubleModifier, Double> ceilings) {
    boolean affects(DoubleModifier modifier) {
      return this.ceilings.containsKey(modifier);
    }

    Double ceiling(DoubleModifier modifier, int gemIndex) {
      return this.gemIndex == gemIndex ? this.ceilings.get(modifier) : null;
    }

    boolean isEmpty() {
      return this.ceilings.isEmpty();
    }
  }

  private static final EnumSet<DoubleModifier> ADDITIVE_MODIFIERS =
      EnumSet.of(
          DoubleModifier.ACCESSORYDROP,
          DoubleModifier.ADVENTURES,
          DoubleModifier.BOOZEDROP,
          DoubleModifier.BUGBEAR_DAMAGE,
          DoubleModifier.CANDYDROP,
          DoubleModifier.DAMAGE_ABSORPTION,
          DoubleModifier.DAMAGE_REDUCTION,
          DoubleModifier.FAIRY_EFFECTIVENESS,
          DoubleModifier.FAIRY_WEIGHT,
          DoubleModifier.FAMILIAR_DAMAGE,
          DoubleModifier.FAMILIAR_EXP,
          DoubleModifier.FISHING_SKILL,
          DoubleModifier.FOODDROP,
          DoubleModifier.FUMBLE,
          DoubleModifier.GEARDROP,
          DoubleModifier.GHOST_DAMAGE,
          DoubleModifier.HATDROP,
          DoubleModifier.HP_REGEN_MAX,
          DoubleModifier.HP_REGEN_MIN,
          DoubleModifier.LEPRECHAUN_EFFECTIVENESS,
          DoubleModifier.LEPRECHAUN_WEIGHT,
          DoubleModifier.MEAT_BONUS,
          DoubleModifier.MONSTER_LEVEL,
          DoubleModifier.MONSTER_LEVEL_PERCENT,
          DoubleModifier.MP_REGEN_MAX,
          DoubleModifier.MP_REGEN_MIN,
          DoubleModifier.MOX_EXPERIENCE,
          DoubleModifier.MOX_EXPERIENCE_PCT,
          DoubleModifier.MUS_EXPERIENCE,
          DoubleModifier.MUS_EXPERIENCE_PCT,
          DoubleModifier.MYS_EXPERIENCE,
          DoubleModifier.MYS_EXPERIENCE_PCT,
          DoubleModifier.OFFHANDDROP,
          DoubleModifier.PANTSDROP,
          DoubleModifier.PASTA_THRALL_EXPERIENCE,
          DoubleModifier.PICKPOCKET_CHANCE,
          DoubleModifier.POOL_SKILL,
          DoubleModifier.PVP_FIGHTS,
          DoubleModifier.SEAL_DAMAGE,
          DoubleModifier.SHIRTDROP,
          DoubleModifier.SOMBRERO_BONUS,
          DoubleModifier.SOMBRERO_EFFECTIVENESS,
          DoubleModifier.SOMBRERO_WEIGHT,
          DoubleModifier.SPORADIC_DAMAGE_AURA,
          DoubleModifier.SPORADIC_ITEMDROP,
          DoubleModifier.SPORADIC_MEATDROP,
          DoubleModifier.SPORADIC_THORNS,
          DoubleModifier.SPELL_DAMAGE_PCT,
          DoubleModifier.VAMPIRE_DAMAGE,
          DoubleModifier.VOLLEYBALL_EFFECTIVENESS,
          DoubleModifier.VOLLEYBALL_WEIGHT,
          DoubleModifier.WEAPONDROP,
          DoubleModifier.WEREWOLF_DAMAGE,
          DoubleModifier.ZOMBIE_DAMAGE);

  private static final EnumSet<DoubleModifier> TIEBREAKER_MODIFIERS =
      EnumSet.of(
          DoubleModifier.COLD_DAMAGE,
          DoubleModifier.COLD_RESISTANCE,
          DoubleModifier.COLD_SPELL_DAMAGE,
          DoubleModifier.COMBAT_RATE,
          DoubleModifier.DAMAGE_AURA,
          DoubleModifier.EXPERIENCE,
          DoubleModifier.FAMILIAR_WEIGHT,
          DoubleModifier.HOT_DAMAGE,
          DoubleModifier.HOT_RESISTANCE,
          DoubleModifier.HOT_SPELL_DAMAGE,
          DoubleModifier.INITIATIVE,
          DoubleModifier.ITEMDROP,
          DoubleModifier.MANA_COST,
          DoubleModifier.MEATDROP,
          DoubleModifier.RANGED_DAMAGE,
          DoubleModifier.SLEAZE_DAMAGE,
          DoubleModifier.SLEAZE_RESISTANCE,
          DoubleModifier.SLEAZE_SPELL_DAMAGE,
          DoubleModifier.SPELL_DAMAGE,
          DoubleModifier.SPOOKY_DAMAGE,
          DoubleModifier.SPOOKY_RESISTANCE,
          DoubleModifier.SPOOKY_SPELL_DAMAGE,
          DoubleModifier.STENCH_DAMAGE,
          DoubleModifier.STENCH_RESISTANCE,
          DoubleModifier.STENCH_SPELL_DAMAGE,
          DoubleModifier.THORNS,
          DoubleModifier.WEAPON_DAMAGE);
  private static final EnumSet<DoubleModifier> FAMILIAR_CALCULATION_MODIFIERS =
      EnumSet.of(
          DoubleModifier.FAMILIAR_WEIGHT,
          DoubleModifier.HIDDEN_FAMILIAR_WEIGHT,
          DoubleModifier.FAMILIAR_WEIGHT_PCT,
          DoubleModifier.FAMILIAR_WEIGHT_CAP,
          DoubleModifier.VOLLEYBALL_WEIGHT,
          DoubleModifier.VOLLEYBALL_EFFECTIVENESS,
          DoubleModifier.FAMILIAR_TUNING_MUSCLE,
          DoubleModifier.FAMILIAR_TUNING_MYSTICALITY,
          DoubleModifier.FAMILIAR_TUNING_MOXIE,
          DoubleModifier.SOMBRERO_WEIGHT,
          DoubleModifier.SOMBRERO_BONUS,
          DoubleModifier.SOMBRERO_EFFECTIVENESS,
          DoubleModifier.LEPRECHAUN_WEIGHT,
          DoubleModifier.LEPRECHAUN_EFFECTIVENESS,
          DoubleModifier.FAIRY_WEIGHT,
          DoubleModifier.FAIRY_EFFECTIVENESS,
          DoubleModifier.FOOD_FAIRY_WEIGHT,
          DoubleModifier.FOOD_FAIRY_EFFECTIVENESS,
          DoubleModifier.BOOZE_FAIRY_WEIGHT,
          DoubleModifier.BOOZE_FAIRY_EFFECTIVENESS,
          DoubleModifier.CANDY_FAIRY_WEIGHT,
          DoubleModifier.CANDY_FAIRY_EFFECTIVENESS);
  private static final EnumSet<DoubleModifier> TIEBREAKER_INPUTS = createTiebreakerInputs();
  private static final EnumSet<DoubleModifier> LATE_CALCULATION_INPUTS =
      createLateCalculationInputs();
  private static final EnumSet<StringModifier> TIEBREAKER_STRINGS =
      EnumSet.of(
          StringModifier.CONDITIONAL_SKILL_EQUIPPED,
          StringModifier.EVALUATED_MODIFIERS,
          StringModifier.MODIFIERS);

  private CodpiecePruning() {}

  private static EnumSet<DoubleModifier> createTiebreakerInputs() {
    var modifiers = EnumSet.copyOf(ADDITIVE_MODIFIERS);
    modifiers.addAll(TIEBREAKER_MODIFIERS);
    modifiers.addAll(
        EnumSet.of(
            DoubleModifier.HP,
            DoubleModifier.HP_PCT,
            DoubleModifier.ITEMDROP_PENALTY,
            DoubleModifier.MEATDROP_PENALTY,
            DoubleModifier.MOX,
            DoubleModifier.MOX_EXPERIENCE,
            DoubleModifier.MOX_PCT,
            DoubleModifier.MP,
            DoubleModifier.MP_PCT,
            DoubleModifier.MUS,
            DoubleModifier.MUS_EXPERIENCE,
            DoubleModifier.MUS_PCT,
            DoubleModifier.MYS,
            DoubleModifier.MYS_EXPERIENCE,
            DoubleModifier.MYS_PCT,
            DoubleModifier.PRISMATIC_DAMAGE,
            DoubleModifier.RAW_COMBAT_RATE,
            DoubleModifier.RANGED_DAMAGE_PCT,
            DoubleModifier.SPORADIC_DAMAGE_AURA,
            DoubleModifier.SPORADIC_THORNS,
            DoubleModifier.SPELL_DAMAGE_PCT,
            DoubleModifier.STACKABLE_MANA_COST,
            DoubleModifier.WEAPON_DAMAGE_PCT));
    return modifiers;
  }

  private static EnumSet<DoubleModifier> createLateCalculationInputs() {
    var modifiers = EnumSet.copyOf(TIEBREAKER_INPUTS);
    modifiers.remove(DoubleModifier.EXPERIENCE);
    modifiers.remove(DoubleModifier.SPELL_DAMAGE);
    return modifiers;
  }

  static boolean supportsScoreTerm(
      DoubleModifier modifier, double weight, Modifiers baseline, Modifiers[] gemModifiers) {
    return supportsScoreTerm(modifier, weight, baseline, gemModifiers, null);
  }

  static boolean supportsScoreTerm(
      DoubleModifier modifier,
      double weight,
      Modifiers baseline,
      Modifiers[] gemModifiers,
      FamiliarScoreContributions familiarScoreContributions) {
    return ADDITIVE_MODIFIERS.contains(modifier)
        || (supportsDirectScoreBound(modifier, weight, gemModifiers, familiarScoreContributions)
            && Arrays.stream(gemModifiers)
                .allMatch(CodpiecePruning::hasOnlySupportedTiebreakerModifiers))
        || canBoundDerivedStat(modifier, weight, baseline, gemModifiers)
        || canBoundHitPoints(modifier, weight, baseline, gemModifiers)
        || canBoundManaPoints(modifier, weight, baseline, gemModifiers);
  }

  private static boolean supportsDirectScoreBound(
      DoubleModifier modifier,
      double weight,
      Modifiers[] gemModifiers,
      FamiliarScoreContributions familiarScoreContributions) {
    if (!TIEBREAKER_MODIFIERS.contains(modifier)
        || (modifier == DoubleModifier.EXPERIENCE
            && (weight <= 0.0
                || Arrays.stream(gemModifiers)
                        .filter(CodpiecePruning.ScoreUpperBound::affectsExperience)
                        .count()
                    > 8))) {
      return false;
    }
    if (modifier != DoubleModifier.ITEMDROP
        && modifier != DoubleModifier.MEATDROP
        && modifier != DoubleModifier.EXPERIENCE) {
      return true;
    }
    if (familiarScoreContributions == null) {
      return Arrays.stream(gemModifiers).noneMatch(CodpiecePruning::affectsFamiliarCalculation);
    }
    boolean affected = familiarScoreContributions.affects(modifier);
    if (modifier == DoubleModifier.EXPERIENCE) {
      return !affected;
    }
    return !affected || weight > 0.0;
  }

  static boolean supportsTiebreakerTerm(
      DoubleModifier modifier, double weight, Modifiers baseline, Modifiers[] gemModifiers) {
    return supportsTiebreakerTerm(modifier, weight, baseline, gemModifiers, null);
  }

  static boolean supportsTiebreakerTerm(
      DoubleModifier modifier,
      double weight,
      Modifiers baseline,
      Modifiers[] gemModifiers,
      FamiliarScoreContributions familiarScoreContributions) {
    if ((modifier == DoubleModifier.ITEMDROP
            || modifier == DoubleModifier.MEATDROP
            || modifier == DoubleModifier.EXPERIENCE)
        && Arrays.stream(gemModifiers).anyMatch(CodpiecePruning::affectsFamiliarCalculation)
        && (familiarScoreContributions == null
            || familiarScoreContributions.affects(modifier)
                && (modifier == DoubleModifier.EXPERIENCE || weight <= 0.0))) {
      return false;
    }
    return ADDITIVE_MODIFIERS.contains(modifier)
        || TIEBREAKER_MODIFIERS.contains(modifier)
        || canBoundDerivedStat(modifier, weight, baseline, gemModifiers)
        || canBoundHitPoints(modifier, weight, baseline, gemModifiers)
        || canBoundManaPoints(modifier, weight, baseline, gemModifiers);
  }

  private static boolean canBoundDerivedStat(
      DoubleModifier modifier, double weight, Modifiers baseline, Modifiers[] gemModifiers) {
    StringModifier floor =
        switch (modifier) {
          case MUS -> StringModifier.FLOOR_BUFFED_MUSCLE;
          case MYS -> StringModifier.FLOOR_BUFFED_MYST;
          case MOX -> StringModifier.FLOOR_BUFFED_MOXIE;
          default -> null;
        };
    if (floor == null || weight <= 0.0 || baseline.hasString(floor)) {
      return false;
    }
    DoubleModifier limit =
        switch (modifier) {
          case MUS -> DoubleModifier.MUS_LIMIT;
          case MYS -> DoubleModifier.MYS_LIMIT;
          default -> DoubleModifier.MOX_LIMIT;
        };
    StringModifier equalize =
        switch (modifier) {
          case MUS -> StringModifier.EQUALIZE_MUSCLE;
          case MYS -> StringModifier.EQUALIZE_MYST;
          default -> StringModifier.EQUALIZE_MOXIE;
        };
    for (Modifiers modifiers : gemModifiers) {
      if (modifiers != null
          && (modifiers.getDouble(limit) != 0.0
              || modifiers.hasString(StringModifier.EQUALIZE)
              || modifiers.hasString(equalize)
              || modifiers.hasString(floor))) {
        return false;
      }
    }
    return true;
  }

  private static boolean canBoundManaPoints(
      DoubleModifier modifier, double weight, Modifiers baseline, Modifiers[] gemModifiers) {
    if (modifier != DoubleModifier.MP
        || weight <= 0.0
        || KoLCharacter.isGreyGoo()
        || baseline.hasString(StringModifier.FLOOR_BUFFED_MYST)
        || baseline.hasString(StringModifier.FLOOR_BUFFED_MOXIE)) {
      return false;
    }
    for (Modifiers modifiers : gemModifiers) {
      if (modifiers != null
          && (modifiers.getDouble(DoubleModifier.MYS_LIMIT) != 0.0
              || modifiers.getDouble(DoubleModifier.MOX_LIMIT) != 0.0
              || modifiers.hasString(StringModifier.EQUALIZE)
              || modifiers.hasString(StringModifier.EQUALIZE_MYST)
              || modifiers.hasString(StringModifier.EQUALIZE_MOXIE)
              || modifiers.hasString(StringModifier.FLOOR_BUFFED_MYST)
              || modifiers.hasString(StringModifier.FLOOR_BUFFED_MOXIE)
              || modifiers.getBoolean(BooleanModifier.MOXIE_CONTROLS_MP)
              || modifiers.getBoolean(BooleanModifier.MOXIE_MAY_CONTROL_MP))) {
        return false;
      }
    }
    return true;
  }

  private static boolean canBoundHitPoints(
      DoubleModifier modifier, double weight, Modifiers baseline, Modifiers[] gemModifiers) {
    if (modifier != DoubleModifier.HP
        || weight <= 0.0
        || KoLCharacter.isVampyre()
        || KoLCharacter.inZootomist()
        || KoLCharacter.inRobocore()
        || KoLCharacter.isGreyGoo()
        || baseline.hasString(StringModifier.FLOOR_BUFFED_MUSCLE)) {
      return false;
    }
    for (Modifiers modifiers : gemModifiers) {
      if (modifiers != null
          && (modifiers.getDouble(DoubleModifier.MUS_LIMIT) != 0.0
              || modifiers.hasString(StringModifier.EQUALIZE)
              || modifiers.hasString(StringModifier.EQUALIZE_MUSCLE)
              || modifiers.hasString(StringModifier.FLOOR_BUFFED_MUSCLE))) {
        return false;
      }
    }
    return true;
  }

  static boolean hasOnlySupportedTiebreakerModifiers(Modifiers modifiers) {
    return hasOnlySupportedModifiers(modifiers, TIEBREAKER_INPUTS, TIEBREAKER_STRINGS);
  }

  static boolean hasOnlySupportedLateCalculationModifiers(Modifiers modifiers) {
    return hasOnlySupportedModifiers(modifiers, LATE_CALCULATION_INPUTS, TIEBREAKER_STRINGS);
  }

  static boolean affectsFamiliarCalculation(Modifiers modifiers) {
    if (modifiers == null) {
      return false;
    }

    for (DoubleModifier modifier : FAMILIAR_CALCULATION_MODIFIERS) {
      if (modifiers.getDouble(modifier) != 0.0 || !modifiers.getDoubles(modifier).isEmpty()) {
        return true;
      }
    }
    return modifiers.getBoolean(BooleanModifier.VOLLEYBALL_OR_SOMBRERO);
  }

  static long[] familiarCalculationValues(Modifiers modifiers) {
    long[] values = new long[FAMILIAR_CALCULATION_MODIFIERS.size()];
    int index = 0;
    for (DoubleModifier modifier : FAMILIAR_CALCULATION_MODIFIERS) {
      values[index++] = Double.doubleToLongBits(modifiers.getDouble(modifier));
    }
    return values;
  }

  private static boolean hasOnlySupportedModifiers(
      Modifiers modifiers,
      EnumSet<DoubleModifier> supportedDoubles,
      EnumSet<StringModifier> supportedStrings) {
    if (modifiers == null) {
      return true;
    }
    for (DoubleModifier modifier : DoubleModifier.values()) {
      if (modifier.getSubsumed().length == 0
          && !supportedDoubles.contains(modifier)
          && (modifiers.getDouble(modifier) != 0.0 || !modifiers.getDoubles(modifier).isEmpty())) {
        return false;
      }
    }
    for (BitmapModifier modifier : BitmapModifier.values()) {
      if (modifiers.getRawBitmap(modifier) != 0) {
        return false;
      }
    }
    for (BooleanModifier modifier : BooleanModifier.values()) {
      if (modifiers.getBoolean(modifier)) {
        return false;
      }
    }
    for (StringModifier modifier : StringModifier.values()) {
      boolean present =
          modifier.isMultiple()
              ? !modifiers.getStrings(modifier).isEmpty()
              : !modifiers.getString(modifier).isEmpty();
      if (present && !supportedStrings.contains(modifier)) {
        return false;
      }
    }
    return true;
  }

  static final class ScoreUpperBound {
    private final List<Evaluator.ScoreTerm> scoreModifiers;
    private final double[] baseline;
    private final double[][] contributions;
    private final double[][][] suffixContributions;
    private final double[][][] maximumSuffixContributions;
    private final double[] selectedContributions;
    private final boolean[] jointModifiers;
    private final double[] jointContributions;
    private final double[][] jointSuffixContributions;
    private final double[] itemContributions;
    private final double[][] itemSuffixContributions;
    private double selectedJointContribution;
    private double selectedItemContribution;
    private double jointBaseline;
    private final double totalMin;
    private final double fixedScore;
    private final boolean exact;
    private final boolean canMeetHardRequirements;

    ScoreUpperBound(
        List<Evaluator.ScoreTerm> scoreModifiers,
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
      this.contributions = new double[scoreModifiers.size()][gemModifiers.length];
      this.suffixContributions =
          new double[scoreModifiers.size()][gemModifiers.length + 1][slotCount];
      this.maximumSuffixContributions =
          totalMin != Double.NEGATIVE_INFINITY
                  || scoreModifiers.stream()
                      .anyMatch(modifier -> modifier.min() != Double.NEGATIVE_INFINITY)
              ? new double[scoreModifiers.size()][gemModifiers.length + 1][slotCount]
              : null;
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
                              || modifier.modifier() == DoubleModifier.EXPERIENCE);
      this.jointModifiers = combineContributions ? new boolean[scoreModifiers.size()] : null;
      this.jointContributions = combineContributions ? new double[gemModifiers.length] : null;
      this.jointSuffixContributions =
          combineContributions ? new double[gemModifiers.length + 1][slotCount] : null;
      boolean hasItemContributions =
          Arrays.stream(itemContributions).anyMatch(value -> value != 0.0);
      this.itemContributions = hasItemContributions ? itemContributions : null;
      this.itemSuffixContributions =
          hasItemContributions ? new double[gemModifiers.length + 1][slotCount] : null;

      Map<DerivedModifier, Integer> predicted = null;
      double baseStatUpperBound =
          Math.max(
              KoLCharacter.getBaseMuscle(),
              Math.max(KoLCharacter.getBaseMysticality(), KoLCharacter.getBaseMoxie()));
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
      double maximumMuscleContribution = Arrays.stream(maximumMuscleContributions).sum();
      double maximumHitPointPercent =
          baseline.getDouble(DoubleModifier.HP_PCT)
              + Arrays.stream(maximumHitPointPercentContributions).sum();
      double maximumBuffedMuscle =
          baseline.predict().get(DerivedModifier.BUFFED_MUS) + maximumMuscleContribution;
      double maximumHitPointBase = Math.max(0.0, maximumBuffedMuscle + 3.0);
      double maximumHitPointMultiplier =
          Math.max(
              0.0, (KoLCharacter.isMuscleClass() ? 1.5 : 1.0) + maximumHitPointPercent / 100.0);
      double maximumMysticality =
          baseline.predict().get(DerivedModifier.BUFFED_MYS)
              + Arrays.stream(maximumMysticalityContributions).sum();
      double maximumMoxie =
          baseline.predict().get(DerivedModifier.BUFFED_MOX)
              + Arrays.stream(maximumMoxieContributions).sum();
      double maximumManaPointBase = Math.max(0.0, Math.max(maximumMysticality, maximumMoxie));
      double maximumManaPointPercent =
          baseline.getDouble(DoubleModifier.MP_PCT)
              + Arrays.stream(maximumManaPointPercentContributions).sum();
      double maximumManaPointMultiplier =
          Math.max(
              0.0,
              (KoLCharacter.isMysticalityClass() ? 1.5 : 1.0) + maximumManaPointPercent / 100.0);
      double[] maximumExperienceMarginals =
          maximumExperienceMarginals(baseline, gemModifiers, remaining, slotCount);
      for (int modifierIndex = 0; modifierIndex < scoreModifiers.size(); modifierIndex++) {
        Evaluator.ScoreTerm scoreModifier = scoreModifiers.get(modifierIndex);
        DoubleModifier modifier = scoreModifier.modifier();
        boolean descending = scoreModifier.weight() > 0.0;
        if ((modifier == DoubleModifier.MUS
                || modifier == DoubleModifier.MYS
                || modifier == DoubleModifier.MOX
                || modifier == DoubleModifier.HP
                || modifier == DoubleModifier.MP)
            && predicted == null) {
          predicted = baseline.predict();
        }
        this.baseline[modifierIndex] = Evaluator.scoreValue(modifier, baseline, predicted);

        for (int gemIndex = 0; gemIndex < gemModifiers.length; gemIndex++) {
          Modifiers modifiers = gemModifiers[gemIndex];
          if (modifiers != null) {
            Double familiarCeiling =
                familiarScoreContributions == null
                    ? null
                    : familiarScoreContributions.ceiling(modifier, gemIndex);
            this.contributions[modifierIndex][gemIndex] =
                familiarCeiling != null
                    ? familiarCeiling
                    : switch (modifier) {
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
                      case EXPERIENCE -> maximumExperienceMarginals[gemIndex];
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

        buildSuffix(
            this.suffixContributions[modifierIndex],
            this.contributions[modifierIndex],
            remaining,
            slotCount,
            descending);
        if (this.maximumSuffixContributions != null) {
          buildSuffix(
              this.maximumSuffixContributions[modifierIndex],
              this.contributions[modifierIndex],
              remaining,
              slotCount,
              true);
        }
      }
      if (this.itemContributions != null) {
        buildSuffix(
            this.itemSuffixContributions, this.itemContributions, remaining, slotCount, true);
      }
      if (combineContributions) {
        for (int modifierIndex = 0; modifierIndex < scoreModifiers.size(); modifierIndex++) {
          DoubleModifier modifier = scoreModifiers.get(modifierIndex).modifier();
          boolean joint =
              modifier != DoubleModifier.MUS
                  && modifier != DoubleModifier.MYS
                  && modifier != DoubleModifier.MOX
                  && modifier != DoubleModifier.HP
                  && modifier != DoubleModifier.MP
                  && modifier != DoubleModifier.EXPERIENCE;
          this.jointModifiers[modifierIndex] = joint;
          if (!joint) {
            continue;
          }
          double weight = scoreModifiers.get(modifierIndex).weight();
          this.jointBaseline += weight * this.baseline[modifierIndex];
          for (int gemIndex = 0; gemIndex < gemModifiers.length; gemIndex++) {
            this.jointContributions[gemIndex] +=
                weight * this.contributions[modifierIndex][gemIndex];
          }
        }
        buildSuffix(
            this.jointSuffixContributions, this.jointContributions, remaining, slotCount, true);
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
                          modifiers,
                          DoubleModifier.MYS,
                          DoubleModifier.MYS_PCT,
                          baseStatUpperBound),
                      derivedStatContribution(
                          modifiers,
                          DoubleModifier.MOX,
                          DoubleModifier.MOX_PCT,
                          baseStatUpperBound))
                  * maximumMultiplier)
          + Math.ceil(
              Math.max(0.0, modifiers.getDouble(DoubleModifier.MP_PCT)) * maximumBase / 100.0);
    }

    private static boolean affectsExperience(Modifiers modifiers) {
      if (modifiers == null) {
        return false;
      }
      return modifiers.getDouble(DoubleModifier.MONSTER_LEVEL) != 0.0
          || modifiers.getDouble(DoubleModifier.MONSTER_LEVEL_PERCENT) != 0.0
          || modifiers.getDouble(DoubleModifier.primeStatExp()) != 0.0
          || modifiers.getDouble(DoubleModifier.primeStatExpPercent()) != 0.0;
    }

    private static double[] maximumExperienceMarginals(
        Modifiers baseline, Modifiers[] gemModifiers, int[] remaining, int slotCount) {
      var relevant = new ArrayList<Integer>();
      for (int i = 0; i < gemModifiers.length; i++) {
        if (affectsExperience(gemModifiers[i])) {
          relevant.add(i);
        }
      }
      double[] marginals = new double[gemModifiers.length];
      collectExperienceMarginals(
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
        Modifiers current,
        Modifiers[] gemModifiers,
        int[] remaining,
        List<Integer> relevant,
        int[] used,
        double[] marginals,
        int start,
        int selected,
        int slotCount) {
      double currentExperience = Evaluator.scoreValue(DoubleModifier.EXPERIENCE, current, null);
      for (int gemIndex : relevant) {
        if (used[gemIndex] >= remaining[gemIndex]) {
          continue;
        }
        var next = new Modifiers(current);
        addExperienceInputs(next, gemModifiers[gemIndex]);
        marginals[gemIndex] =
            Math.max(
                marginals[gemIndex],
                Evaluator.scoreValue(DoubleModifier.EXPERIENCE, next, null) - currentExperience);
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
        addExperienceInputs(next, gemModifiers[gemIndex]);
        used[gemIndex]++;
        collectExperienceMarginals(
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

    private static void addExperienceInputs(Modifiers target, Modifiers source) {
      for (DoubleModifier modifier :
          List.of(
              DoubleModifier.MONSTER_LEVEL,
              DoubleModifier.MONSTER_LEVEL_PERCENT,
              DoubleModifier.primeStatExp(),
              DoubleModifier.primeStatExpPercent())) {
        target.setDouble(modifier, target.getDouble(modifier) + source.getDouble(modifier));
      }
    }

    void select(int gemIndex) {
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
              + estimateAdditional(this.itemSuffixContributions, start, remainingSlots);
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
              + estimateAdditional(this.itemSuffixContributions, start, remainingSlots)
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

    private static double estimateAdditional(
        double[][] suffixContributions, int start, int remainingSlots) {
      if (suffixContributions == null
          || remainingSlots == 0
          || start >= suffixContributions.length - 1) {
        return 0.0;
      }
      double score = 0.0;
      for (int i = 0; i < remainingSlots; i++) {
        score += suffixContributions[start][i];
      }
      return score;
    }

    boolean canMeetMinimum(int start, int[] remaining, int remainingSlots, double scoreUpperBound) {
      if (!this.canMeetHardRequirements) {
        return false;
      }
      if (this.maximumSuffixContributions == null) {
        return true;
      }
      if (scoreUpperBound < this.totalMin) {
        return false;
      }
      for (int modifierIndex = 0; modifierIndex < this.scoreModifiers.size(); modifierIndex++) {
        double minimum = this.scoreModifiers.get(modifierIndex).min();
        if (minimum == Double.NEGATIVE_INFINITY) {
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
  }

  static final class BooleanUpperBound {
    private final boolean[] contributions;
    private final int[] suffixCopies;

    BooleanUpperBound(
        List<CheckedItem> gems, int[] remaining, int slotCount, BooleanModifier modifier) {
      this.contributions = new boolean[gems.size()];
      this.suffixCopies = new int[gems.size() + 1];
      for (int index = gems.size() - 1; index >= 0; index--) {
        Modifiers modifiers = ModifierDatabase.getItemModifiers(gems.get(index).getItemId());
        this.contributions[index] = modifiers != null && modifiers.getBoolean(modifier);
        this.suffixCopies[index] =
            Math.min(
                slotCount,
                this.suffixCopies[index + 1] + (this.contributions[index] ? remaining[index] : 0));
      }
    }

    int estimateAdditional(int start, int[] remaining, int remainingSlots) {
      if (remainingSlots == 0 || start >= remaining.length) {
        return 0;
      }
      int available =
          this.suffixCopies[start + 1] + (this.contributions[start] ? remaining[start] : 0);
      return Math.min(remainingSlots, available);
    }
  }

  record BranchBounds(
      ScoreUpperBound score,
      ScoreUpperBound tiebreaker,
      BooleanUpperBound itemDroppers,
      BooleanUpperBound meatDroppers) {
    void select(int gemIndex) {
      if (this.score != null) {
        this.score.select(gemIndex);
      }
      if (this.tiebreaker != null) {
        this.tiebreaker.select(gemIndex);
      }
    }

    void deselect(int gemIndex) {
      if (this.score != null) {
        this.score.deselect(gemIndex);
      }
      if (this.tiebreaker != null) {
        this.tiebreaker.deselect(gemIndex);
      }
    }
  }
}
