package net.sourceforge.kolmafia.maximizer;

import java.util.EnumSet;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;

/**
 * Conservative classification of Codpiece gem modifiers. A gem may only take part in the cached
 * late-adjustment path when every modifier it supplies is known to behave additively there, and
 * familiar-dependent gems must be identified so they are present when familiar effects are
 * calculated.
 */
final class CodpieceModifierSafety {
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

  private static final EnumSet<DoubleModifier> DIRECTLY_SCORED_MODIFIERS =
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
  private static final EnumSet<DoubleModifier> INCREMENTAL_SCORE_MODIFIERS =
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
  private static final EnumSet<DoubleModifier> LATE_CALCULATION_INPUTS =
      createLateCalculationInputs();
  private static final EnumSet<StringModifier> LATE_CALCULATION_STRINGS =
      EnumSet.of(
          StringModifier.CONDITIONAL_SKILL_EQUIPPED,
          StringModifier.EVALUATED_MODIFIERS,
          StringModifier.MODIFIERS);

  private CodpieceModifierSafety() {}

  private static EnumSet<DoubleModifier> createLateCalculationInputs() {
    var modifiers = EnumSet.copyOf(ADDITIVE_MODIFIERS);
    modifiers.addAll(DIRECTLY_SCORED_MODIFIERS);
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
    modifiers.remove(DoubleModifier.EXPERIENCE);
    modifiers.remove(DoubleModifier.SPELL_DAMAGE);
    return modifiers;
  }

  static boolean supportsIncrementalScore(DoubleModifier modifier) {
    return INCREMENTAL_SCORE_MODIFIERS.contains(modifier)
        && (modifier != DoubleModifier.ADVENTURES || KoLCharacter.canGainRolloverAdventures());
  }

  static boolean hasOnlySupportedLateCalculationModifiers(Modifiers modifiers) {
    if (modifiers == null) {
      return true;
    }
    if (modifiers.hasDoubleModifier(
        modifier ->
            modifier.getSubsumed().length == 0 && !LATE_CALCULATION_INPUTS.contains(modifier))) {
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
      if (present && !LATE_CALCULATION_STRINGS.contains(modifier)) {
        return false;
      }
    }
    return true;
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
}
