package net.sourceforge.kolmafia.maximizer;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;

/**
 * Conservative branch bounds for Codpiece gem search. Unsupported interactions must disable a bound
 * rather than risk underestimating a reachable score.
 */
final class CodpiecePruning {
  static boolean forceExhaustiveForTests;

  record ContributionRange(double minimum, double maximum) {
    ContributionRange include(double value) {
      return new ContributionRange(Math.min(this.minimum, value), Math.max(this.maximum, value));
    }
  }

  record FamiliarScoreContributions(
      int gemIndex,
      Map<DoubleModifier, ContributionRange> ranges,
      Map<DoubleModifier, ContributionRange> baselines) {
    FamiliarScoreContributions(int gemIndex, Map<DoubleModifier, ContributionRange> ranges) {
      this(gemIndex, ranges, Map.of());
    }

    Double bound(DoubleModifier modifier, int gemIndex, boolean maximum) {
      if (this.gemIndex != gemIndex) {
        return null;
      }
      ContributionRange range = this.ranges.get(modifier);
      return range == null ? null : maximum ? range.maximum() : range.minimum();
    }

    double baseline(DoubleModifier modifier, boolean maximum) {
      ContributionRange range = this.baselines.get(modifier);
      return range == null ? 0.0 : maximum ? range.maximum() : range.minimum();
    }

    boolean isEmpty() {
      return this.ranges.isEmpty();
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
          DoubleModifier.ENCHANTMENT_COUNT,
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
          DoubleModifier.MOX_EXPERIENCE,
          DoubleModifier.MOX_EXPERIENCE_PCT,
          DoubleModifier.MP_REGEN_MAX,
          DoubleModifier.MP_REGEN_MIN,
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
          DoubleModifier.SLIME_RESISTANCE,
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

  // These are safe as objectives with today's gems, but not necessarily as future gem inputs.
  private static final EnumSet<DoubleModifier> DIRECT_SCORE_MODIFIERS =
      EnumSet.of(
          DoubleModifier.ABSORB_ADV,
          DoubleModifier.ABSORB_STAT,
          DoubleModifier.ADDITIONAL_SONG,
          DoubleModifier.AVOID_ATTACK,
          DoubleModifier.BASE_RESTING_HP,
          DoubleModifier.BASE_RESTING_MP,
          DoubleModifier.BONUS_RESTING_HP,
          DoubleModifier.BONUS_RESTING_MP,
          DoubleModifier.BOOZE_FAIRY_EFFECTIVENESS,
          DoubleModifier.BOOZE_FAIRY_WEIGHT,
          DoubleModifier.CANDY_FAIRY_EFFECTIVENESS,
          DoubleModifier.CANDY_FAIRY_WEIGHT,
          DoubleModifier.COMBAT_ITEM_DAMAGE_PCT,
          DoubleModifier.COMBAT_MANA_COST,
          DoubleModifier.CRIMBOT_POWER,
          DoubleModifier.CRITICAL_PCT,
          DoubleModifier.DB_COMBAT_DAMAGE,
          DoubleModifier.DB_COMBAT_WEAKEN,
          DoubleModifier.DISCO_STYLE,
          DoubleModifier.DRIPPY_DAMAGE,
          DoubleModifier.DRIPPY_RESISTANCE,
          DoubleModifier.EFFECT_DURATION,
          DoubleModifier.ELF_WARFARE_EFFECTIVENESS,
          DoubleModifier.ENERGY,
          DoubleModifier.FAMILIAR_ACTION_BONUS,
          DoubleModifier.FAMILIAR_TUNING_MOXIE,
          DoubleModifier.FAMILIAR_TUNING_MUSCLE,
          DoubleModifier.FAMILIAR_TUNING_MYSTICALITY,
          DoubleModifier.FAMILIAR_WEIGHT_CAP,
          DoubleModifier.FAMILIAR_WEIGHT_PCT,
          DoubleModifier.FIRST_HIT_DAMAGE_REDUCTION,
          DoubleModifier.FOOD_FAIRY_EFFECTIVENESS,
          DoubleModifier.FOOD_FAIRY_WEIGHT,
          DoubleModifier.FREE_RESTS,
          DoubleModifier.HIDDEN_FAMILIAR_WEIGHT,
          DoubleModifier.HAT_PANTS_DROP,
          DoubleModifier.HOBO_POWER,
          DoubleModifier.HP_PCT,
          DoubleModifier.INITIATIVE_PENALTY,
          DoubleModifier.ITEMDROP_PENALTY,
          DoubleModifier.KILL_MORE_SKELETONS,
          DoubleModifier.KRUEGERAND_DROP,
          DoubleModifier.LANTERN,
          DoubleModifier.LEAVES,
          DoubleModifier.LIVER_CAPACITY,
          DoubleModifier.LUCK,
          DoubleModifier.MAXIMUM_HOOCH,
          DoubleModifier.MAXIMUM_HP_MP,
          DoubleModifier.MEATDROP_PENALTY,
          DoubleModifier.MERKIN_DAMAGE,
          DoubleModifier.MINSTREL_LEVEL,
          DoubleModifier.MOX_PCT,
          DoubleModifier.MOX_LIMIT,
          DoubleModifier.MP_PCT,
          DoubleModifier.MPC_DROP,
          DoubleModifier.MUS_PCT,
          DoubleModifier.MUS_LIMIT,
          DoubleModifier.MYS_PCT,
          DoubleModifier.MYS_LIMIT,
          DoubleModifier.ORC_DAMAGE,
          DoubleModifier.OTHELLO_SKILL,
          DoubleModifier.PIECE_OF_TWELVE_DROP,
          DoubleModifier.PIRATE_WARFARE_EFFECTIVENESS,
          DoubleModifier.PLUMBER_POWER,
          DoubleModifier.POISON_CHANCE,
          DoubleModifier.POTION_DROP,
          DoubleModifier.PP,
          DoubleModifier.PRISMATIC_DAMAGE,
          DoubleModifier.RAM,
          DoubleModifier.RANDOM_MONSTER_MODIFIERS,
          DoubleModifier.RANGED_DAMAGE_PCT,
          DoubleModifier.RAW_COMBAT_RATE,
          DoubleModifier.REDUCE_ENEMY_DEFENSE,
          DoubleModifier.RESTING_HP_PCT,
          DoubleModifier.RESTING_MP_PCT,
          DoubleModifier.ROLLOVER_EFFECT_DURATION,
          DoubleModifier.RUBEE_DROP,
          DoubleModifier.SAUCE_SPELL_DAMAGE,
          DoubleModifier.SCRAP,
          DoubleModifier.SIXGUN_DAMAGE,
          DoubleModifier.SKELETON_DAMAGE,
          DoubleModifier.SLIME_HATES_IT,
          DoubleModifier.SMITHSNESS,
          DoubleModifier.SONG_DURATION,
          DoubleModifier.SPELL_CRITICAL_PCT,
          DoubleModifier.SPLEEN_CAPACITY,
          DoubleModifier.SPLEEN_DROP,
          DoubleModifier.SPRINKLES,
          DoubleModifier.STACKABLE_MANA_COST,
          DoubleModifier.STOMACH_CAPACITY,
          DoubleModifier.SUPERCOLD_RESISTANCE,
          DoubleModifier.UNDEAD_DAMAGE,
          DoubleModifier.UNDERWATER_COMBAT_RATE,
          DoubleModifier.WARBEAR_ARMOR_PENETRATION,
          DoubleModifier.WARBEAR_ITEM_DROP,
          DoubleModifier.WATER,
          DoubleModifier.WATER_LEVEL,
          DoubleModifier.WEAPON_DAMAGE_PCT);

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
      DoubleModifier modifier,
      double weight,
      Modifiers baseline,
      Modifiers[] gemModifiers,
      FamiliarScoreContributions familiarScoreContributions) {
    return supportsScoreTerm(
        modifier,
        weight,
        baseline,
        gemModifiers,
        familiarScoreContributions,
        Arrays.stream(gemModifiers).allMatch(CodpiecePruning::hasOnlySupportedTiebreakerModifiers));
  }

  static boolean supportsScoreTerm(
      DoubleModifier modifier,
      double weight,
      Modifiers baseline,
      Modifiers[] gemModifiers,
      FamiliarScoreContributions familiarScoreContributions,
      boolean hasOnlySupportedTiebreakerModifiers) {
    return (isExperienceScoreModifier(modifier)
            && supportsDirectScoreBound(modifier, weight, gemModifiers, familiarScoreContributions)
            && hasOnlySupportedTiebreakerModifiers)
        || (isSpecialFoldScoreModifier(modifier)
            && supportsDirectScoreBound(modifier, weight, gemModifiers, familiarScoreContributions))
        || (!isExperienceScoreModifier(modifier)
            && !isSpecialFoldScoreModifier(modifier)
            && ADDITIVE_MODIFIERS.contains(modifier))
        || (!isExperienceScoreModifier(modifier)
            && !isSpecialFoldScoreModifier(modifier)
            && (DIRECT_SCORE_MODIFIERS.contains(modifier)
                || supportsDirectScoreBound(
                    modifier, weight, gemModifiers, familiarScoreContributions))
            && hasOnlySupportedTiebreakerModifiers)
        || canBoundDerivedStat(modifier, weight, baseline, gemModifiers)
        || canBoundHitPoints(modifier, weight, baseline, gemModifiers)
        || canBoundManaPoints(modifier, weight, baseline, gemModifiers);
  }

  private static boolean supportsDirectScoreBound(
      DoubleModifier modifier,
      double weight,
      Modifiers[] gemModifiers,
      FamiliarScoreContributions familiarScoreContributions) {
    if ((!TIEBREAKER_MODIFIERS.contains(modifier)
            && !isExperienceScoreModifier(modifier)
            && !isSpecialFoldScoreModifier(modifier))
        || (isExperienceScoreModifier(modifier)
            && Arrays.stream(gemModifiers)
                    .filter(modifiers -> affectsExperience(modifiers, modifier))
                    .count()
                > 8)) {
      return false;
    }
    if (modifier.getSubsumed().length > 0
        && Arrays.stream(gemModifiers)
            .filter(java.util.Objects::nonNull)
            .anyMatch(
                modifiers -> modifiers.hasDoubleModifier(candidate -> candidate == modifier))) {
      return false;
    }
    if (modifier != DoubleModifier.ITEMDROP
        && modifier != DoubleModifier.MEATDROP
        && !isExperienceScoreModifier(modifier)) {
      return true;
    }

    if (familiarScoreContributions == null) {
      return Arrays.stream(gemModifiers).noneMatch(CodpiecePruning::affectsFamiliarCalculation);
    }
    return true;
  }

  static boolean isExperienceScoreModifier(DoubleModifier modifier) {
    return modifier == DoubleModifier.EXPERIENCE
        || modifier == DoubleModifier.MUS_EXPERIENCE
        || modifier == DoubleModifier.MYS_EXPERIENCE
        || modifier == DoubleModifier.MOX_EXPERIENCE;
  }

  static boolean isSpecialFoldScoreModifier(DoubleModifier modifier) {
    return modifier == DoubleModifier.FAMILIAR_WEIGHT_PCT
        || modifier == DoubleModifier.MUS_LIMIT
        || modifier == DoubleModifier.MYS_LIMIT
        || modifier == DoubleModifier.MOX_LIMIT
        || modifier == DoubleModifier.PRISMATIC_DAMAGE
        || modifier.getSubsumed().length > 0;
  }

  static boolean supportsTiebreakerTerm(
      DoubleModifier modifier,
      double weight,
      Modifiers baseline,
      Modifiers[] gemModifiers,
      FamiliarScoreContributions familiarScoreContributions) {
    if ((modifier == DoubleModifier.ITEMDROP
            || modifier == DoubleModifier.MEATDROP
            || isExperienceScoreModifier(modifier))
        && Arrays.stream(gemModifiers).anyMatch(CodpiecePruning::affectsFamiliarCalculation)
        && familiarScoreContributions == null) {
      return false;
    }
    return (isExperienceScoreModifier(modifier)
            && supportsDirectScoreBound(modifier, weight, gemModifiers, familiarScoreContributions))
        || (!isExperienceScoreModifier(modifier) && ADDITIVE_MODIFIERS.contains(modifier))
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
    if (floor == null || baseline.hasString(floor)) {
      return false;
    }
    DoubleModifier flat =
        switch (modifier) {
          case MUS -> DoubleModifier.MUS;
          case MYS -> DoubleModifier.MYS;
          default -> DoubleModifier.MOX;
        };
    DoubleModifier percent =
        switch (modifier) {
          case MUS -> DoubleModifier.MUS_PCT;
          case MYS -> DoubleModifier.MYS_PCT;
          default -> DoubleModifier.MOX_PCT;
        };
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
              || modifiers.hasString(floor)
              || (weight < 0.0 && hasNegativeContribution(modifiers, flat, percent)))) {
        return false;
      }
    }
    return true;
  }

  private static boolean canBoundManaPoints(
      DoubleModifier modifier, double weight, Modifiers baseline, Modifiers[] gemModifiers) {
    if (modifier != DoubleModifier.MP
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
              || modifiers.getBoolean(BooleanModifier.MOXIE_MAY_CONTROL_MP)
              || (weight < 0.0
                  && hasNegativeContribution(
                      modifiers,
                      DoubleModifier.MYS,
                      DoubleModifier.MYS_PCT,
                      DoubleModifier.MOX,
                      DoubleModifier.MOX_PCT,
                      DoubleModifier.MP,
                      DoubleModifier.MP_PCT)))) {
        return false;
      }
    }
    return true;
  }

  private static boolean canBoundHitPoints(
      DoubleModifier modifier, double weight, Modifiers baseline, Modifiers[] gemModifiers) {
    if (modifier != DoubleModifier.HP
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
              || modifiers.hasString(StringModifier.FLOOR_BUFFED_MUSCLE)
              || (weight < 0.0
                  && hasNegativeContribution(
                      modifiers,
                      DoubleModifier.MUS,
                      DoubleModifier.MUS_PCT,
                      DoubleModifier.HP,
                      DoubleModifier.HP_PCT)))) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasNegativeContribution(Modifiers modifiers, DoubleModifier... inputs) {
    return Arrays.stream(inputs).anyMatch(input -> modifiers.getDouble(input) < 0.0);
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

  static double familiarWeightAdjustment(Modifiers modifiers) {
    for (DoubleModifier modifier : FAMILIAR_CALCULATION_MODIFIERS) {
      if (modifier != DoubleModifier.FAMILIAR_WEIGHT && modifiers.getDouble(modifier) != 0.0) {
        return Double.NaN;
      }
    }
    return modifiers.getDouble(DoubleModifier.FAMILIAR_WEIGHT);
  }

  static double generalExperienceScoreMultiplier(Modifiers modifiers) {
    double experienceMultiplier =
        1.0 + modifiers.getDouble(DoubleModifier.primeStatExpPercent()) / 100.0;
    return CodpieceScoreBound.primeStatExperienceDistribution(modifiers)
        * experienceMultiplier
        * experienceMultiplier
        / 2.0;
  }

  static double familiarExperienceContribution(
      DoubleModifier scoreModifier,
      Modifiers modifiers,
      double directExperience,
      double generalExperience) {
    if (scoreModifier == DoubleModifier.EXPERIENCE) {
      return directExperience
              * (1.0 + modifiers.getDouble(DoubleModifier.primeStatExpPercent()) / 100.0)
              / 2.0
          + generalExperience * generalExperienceScoreMultiplier(modifiers);
    }
    double direct = scoreModifier == DoubleModifier.primeStatExp() ? directExperience : 0.0;
    double general =
        generalExperience
            * CodpieceScoreBound.statExperienceDistribution(scoreModifier, modifiers)
            * (1.0
                + modifiers.getDouble(CodpieceScoreBound.experiencePercent(scoreModifier)) / 100.0);
    return direct + (KoLCharacter.inTheSource() ? general / 3.0 : general);
  }

  static boolean affectsExperience(Modifiers modifiers, DoubleModifier scoreModifier) {
    if (modifiers == null) {
      return false;
    }
    DoubleModifier statExperience =
        scoreModifier == DoubleModifier.EXPERIENCE ? DoubleModifier.primeStatExp() : scoreModifier;
    DoubleModifier statExperiencePercent = CodpieceScoreBound.experiencePercent(statExperience);
    return modifiers.getDouble(DoubleModifier.MONSTER_LEVEL) != 0.0
        || (scoreModifier == DoubleModifier.EXPERIENCE
            && modifiers.getDouble(DoubleModifier.MONSTER_LEVEL_PERCENT) != 0.0)
        || modifiers.getDouble(DoubleModifier.EXPERIENCE) != 0.0
        || modifiers.getDouble(statExperience) != 0.0
        || modifiers.getDouble(statExperiencePercent) != 0.0;
  }

  static void addExperienceInputs(Modifiers target, Modifiers source) {
    for (DoubleModifier modifier :
        List.of(
            DoubleModifier.MONSTER_LEVEL,
            DoubleModifier.MONSTER_LEVEL_PERCENT,
            DoubleModifier.EXPERIENCE,
            DoubleModifier.MUS_EXPERIENCE,
            DoubleModifier.MUS_EXPERIENCE_PCT,
            DoubleModifier.MYS_EXPERIENCE,
            DoubleModifier.MYS_EXPERIENCE_PCT,
            DoubleModifier.MOX_EXPERIENCE,
            DoubleModifier.MOX_EXPERIENCE_PCT)) {
      target.setDouble(modifier, target.getDouble(modifier) + source.getDouble(modifier));
    }
  }

  private static boolean hasOnlySupportedModifiers(
      Modifiers modifiers,
      EnumSet<DoubleModifier> supportedDoubles,
      EnumSet<StringModifier> supportedStrings) {
    if (modifiers == null) {
      return true;
    }
    if (modifiers.hasDoubleModifier(
        modifier -> modifier.getSubsumed().length == 0 && !supportedDoubles.contains(modifier))) {
      return false;
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
      CodpieceScoreBound score,
      CodpieceScoreBound tiebreaker,
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
