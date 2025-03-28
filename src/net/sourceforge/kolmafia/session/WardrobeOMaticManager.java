package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.ModifierList.ModifierValue;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.PHPRandom;

public class WardrobeOMaticManager {
  private WardrobeOMaticManager() {}

  public static int calculateSeed(final int itemId, final int gameday) {
    return (itemId * gameday) + 2063;
  }

  public static int calculateTier(final int level) {
    return Math.min(level / 5, 4);
  }

  private static int rollForTier(
      PHPMTRandom mtRng, int tier, int base, int tierMultiplier, int roll, int rollTierMultiplier) {
    return base + tierMultiplier * tier + mtRng.nextInt(roll + rollTierMultiplier * tier);
  }

  private static List<ModifierValue> rollModifierStrength(
      final PHPMTRandom mtRng, final int tier, final DoubleModifier modifier) {
    return switch (modifier) {
      case MUS, MYS, MOX -> List.of(
          new ModifierValue(modifier, rollForTier(mtRng, tier, 10, 10, 2, 2)));
      case HOT_RESISTANCE,
          COLD_RESISTANCE,
          STENCH_RESISTANCE,
          SLEAZE_RESISTANCE,
          SPOOKY_RESISTANCE -> List.of(
          new ModifierValue(modifier, rollForTier(mtRng, tier, 1, 1, 2, 0)));
      case HP, MP -> List.of(new ModifierValue(modifier, rollForTier(mtRng, tier, 10, 20, 20, 0)));
      case DAMAGE_REDUCTION -> List.of(
          new ModifierValue(modifier, rollForTier(mtRng, tier, 1, 3, 4, 0)));
      case DAMAGE_ABSORPTION -> List.of(
          new ModifierValue(modifier, rollForTier(mtRng, tier, 10, 10, 5, 5)));
      case HOT_DAMAGE,
          COLD_DAMAGE,
          STENCH_DAMAGE,
          SLEAZE_DAMAGE,
          SPOOKY_DAMAGE,
          HOT_SPELL_DAMAGE,
          COLD_SPELL_DAMAGE,
          STENCH_SPELL_DAMAGE,
          SLEAZE_SPELL_DAMAGE,
          SPOOKY_SPELL_DAMAGE -> List.of(
          new ModifierValue(modifier, rollForTier(mtRng, tier, 4, 4, 2, 2)));
      case ITEMDROP -> List.of(new ModifierValue(modifier, rollForTier(mtRng, tier, 3, 5, 5, 0)));
      case MEATDROP -> List.of(new ModifierValue(modifier, rollForTier(mtRng, tier, 5, 10, 10, 0)));
      case MONSTER_LEVEL -> List.of(
          new ModifierValue(modifier, rollForTier(mtRng, tier, 0, 5, 10, 0)));
      case HP_REGEN_MIN, HP_REGEN_MAX -> List.of(
          new ModifierValue(DoubleModifier.HP_REGEN_MIN, rollForTier(mtRng, tier, 2, 2, 2, 0)),
          new ModifierValue(DoubleModifier.HP_REGEN_MAX, rollForTier(mtRng, tier, 5, 5, 5, 0)));
      case MP_REGEN_MIN, MP_REGEN_MAX -> List.of(
          new ModifierValue(DoubleModifier.MP_REGEN_MIN, rollForTier(mtRng, tier, 3, 3, 2, 0)),
          new ModifierValue(DoubleModifier.MP_REGEN_MAX, rollForTier(mtRng, tier, 5, 5, 5, 0)));
      default -> List.of();
    };
  }

  private static List<DoubleModifier> prepareModifierList(
      final List<DoubleModifier> mods, final PHPRandom rng, final int tier) {
    return prepareModifierList(mods, rng, tier, 0, mods.size() - Math.max(0, tier - 3));
  }

  private static List<DoubleModifier> prepareModifierList(
      final List<DoubleModifier> mods,
      final PHPRandom rng,
      final int tier,
      final int start,
      final int end) {
    return rng.shuffle(new ArrayList<>(mods.subList(start, end))).subList(0, tier);
  }

  private static final List<DoubleModifier> SHIRT_MODIFIERS =
      List.of(
          DoubleModifier.MUS,
          DoubleModifier.MYS,
          DoubleModifier.MOX,
          DoubleModifier.HOT_RESISTANCE,
          DoubleModifier.COLD_RESISTANCE,
          DoubleModifier.STENCH_RESISTANCE,
          DoubleModifier.SLEAZE_RESISTANCE,
          DoubleModifier.SPOOKY_RESISTANCE,
          DoubleModifier.HP,
          DoubleModifier.MP,
          DoubleModifier.HP_REGEN_MIN,
          DoubleModifier.MP_REGEN_MIN,
          DoubleModifier.DAMAGE_REDUCTION,
          DoubleModifier.DAMAGE_ABSORPTION,
          DoubleModifier.ITEMDROP,
          DoubleModifier.MEATDROP,
          DoubleModifier.MONSTER_LEVEL);

  private static final List<String> SHIRT_NOUNS =
      List.of("jersey", "Neo-Hawaiian shirt", "t-shirt", "dress shirt", "sweater", "sweatshirt");

  private static final List<String> SHIRT_ADJECTIVES =
      List.of(
          "galvanized",
          "double-creased",
          "double-breasted",
          "foil-clad",
          "aluminum-threaded",
          "electroplated",
          "carbon-coated",
          "phase-changing",
          "liquid cooled",
          "conductive",
          "radation-shielded",
          "nanotube-threaded",
          "moisture-wicking",
          "shape-memory",
          "antimicrobial",
          "liquid-cooled");

  private static final List<String> SHIRT_QUALITIES = List.of("super", "ultra", "mega", "hyper");

  private static final List<String> SHIRT_MATERIALS =
      List.of(
          "gabardine",
          "mylar",
          "polyester",
          "double-polyester",
          "triple-polyester",
          "rayon",
          "wax paper",
          "aluminum foil",
          "synthetic silk",
          "xylon",
          "gore-tex",
          "kapton",
          "flannel",
          "silk",
          "cotton",
          "wool",
          "linen");

  public static List<ModifierValue> getShirtModifiers(final int gameday, final int level) {
    var seed = calculateSeed(ItemPool.FUTURISTIC_SHIRT, gameday);
    var rng = new PHPRandom(seed);
    var mtRng = new PHPMTRandom(seed);
    var tier = calculateTier(level);

    var modifiers = prepareModifierList(SHIRT_MODIFIERS, rng, tier);

    // Select image and one other unknown roll
    mtRng.nextInt();
    mtRng.nextInt();

    var adjective = mtRng.pick(SHIRT_ADJECTIVES);

    var materialIndex = mtRng.nextInt(SHIRT_MATERIALS.size());
    var quality = (materialIndex > 11) ? mtRng.pick(SHIRT_QUALITIES) : "";
    var material = SHIRT_MATERIALS.get(materialIndex);

    // Unknown roll
    mtRng.nextInt();

    // Not quite got the name spaded yet
    var name = String.join(" ", adjective, quality + material, "shirt");

    var modifierValues = new ArrayList<ModifierValue>();
    for (var modifier : modifiers) {
      modifierValues.addAll(rollModifierStrength(mtRng, tier, modifier));
    }
    return modifierValues;
  }

  private static final List<DoubleModifier> HAT_MODIFIERS =
      List.of(
          DoubleModifier.MUS,
          DoubleModifier.MYS,
          DoubleModifier.MOX,
          DoubleModifier.HP,
          DoubleModifier.MP,
          DoubleModifier.HP_REGEN_MIN,
          DoubleModifier.MP_REGEN_MIN,
          DoubleModifier.HOT_DAMAGE,
          DoubleModifier.COLD_DAMAGE,
          DoubleModifier.STENCH_DAMAGE,
          DoubleModifier.SLEAZE_DAMAGE,
          DoubleModifier.SPOOKY_DAMAGE,
          DoubleModifier.HOT_SPELL_DAMAGE,
          DoubleModifier.COLD_SPELL_DAMAGE,
          DoubleModifier.STENCH_SPELL_DAMAGE,
          DoubleModifier.SLEAZE_SPELL_DAMAGE,
          DoubleModifier.SPOOKY_SPELL_DAMAGE,
          DoubleModifier.DAMAGE_REDUCTION,
          DoubleModifier.DAMAGE_ABSORPTION,
          DoubleModifier.ITEMDROP,
          DoubleModifier.MEATDROP,
          DoubleModifier.MONSTER_LEVEL);

  private static final List<String> HAT_ADJECTIVES =
      List.of(
          "nanoplated",
          "self-replicating",
          "autonomous",
          "fusion-powered",
          "fision-powered",
          "hyperefficient",
          "quantum",
          "nuclear",
          "magnetic",
          "laser-guided",
          "solar-powered",
          "psionic",
          "gravitronic",
          "biotronic",
          "neurolinked",
          "transforming",
          "meta-fashionable");

  public static List<ModifierValue> getHatModifiers(final int gameday, final int level) {
    var seed = calculateSeed(ItemPool.FUTURISTIC_HAT, gameday);
    var rng = new PHPRandom(seed);
    var mtRng = new PHPMTRandom(seed);
    var tier = calculateTier(level);

    var modifiers = prepareModifierList(HAT_MODIFIERS, rng, tier);

    // Select image and three other unknown rolls
    mtRng.nextInt();
    mtRng.nextInt();
    mtRng.nextInt();
    mtRng.nextInt();

    var adjective = mtRng.pick(HAT_ADJECTIVES);

    // 6 unknown rolls
    mtRng.nextInt();
    mtRng.nextInt();
    mtRng.nextInt();
    mtRng.nextInt();
    mtRng.nextInt();
    mtRng.nextInt();

    // Not quite got the name spaded yet
    var name = String.join(" ", adjective, "metal-metal", "hat");

    var modifierValues = new ArrayList<ModifierValue>();
    for (var modifier : modifiers) {
      modifierValues.addAll(rollModifierStrength(mtRng, tier, modifier));
    }
    return modifierValues;
  }

  private static final List<DoubleModifier> COLLAR_MODIFIERS =
      List.of(
          DoubleModifier.FAMILIAR_WEIGHT,
          DoubleModifier.FAMILIAR_DAMAGE,
          DoubleModifier.FAMILIAR_EXP);
  private static final List<String> COLLAR_NOUNS = List.of("pet tag", "collar", "pet sweater");
  private static final List<String> COLLAR_ADJECTIVES =
      List.of(
          "hyperchromatic",
          "pearlescent",
          "bright",
          "day-glo",
          "luminescent",
          "vibrant",
          "earthy",
          "oversaturated",
          "partially transparent",
          "opaque",
          "faded",
          "metallic",
          "shiny",
          "glow-in-the-dark",
          "neon",
          "prismatic",
          "incandescent",
          "polychromatic",
          "opalescent",
          "psychedelic",
          "kaleidoscopic");

  public static List<ModifierValue> getCollarModifiers(final int gameday, final int level) {
    var seed = calculateSeed(ItemPool.FUTURISTIC_COLLAR, gameday);
    var rng = new PHPRandom(seed);
    var mtRng = new PHPMTRandom(seed);
    var tier = calculateTier(level);

    var padding = Math.max(1, 4 - tier);
    var modifiers =
        prepareModifierList(
            COLLAR_MODIFIERS, rng, tier, padding, COLLAR_MODIFIERS.size() - padding);

    // Select image and three other unknown rolls
    var image = mtRng.nextInt(9);
    var noun = COLLAR_NOUNS.get((image - 2) / 3);

    // Something in madlib
    mtRng.nextInt();

    var adjective = mtRng.pick(COLLAR_ADJECTIVES);

    // 5 unknown rolls
    mtRng.nextInt();
    mtRng.nextInt();
    mtRng.nextInt();
    mtRng.nextInt();
    mtRng.nextInt();

    // Not quite got the name spaded yet
    var name = String.join(" ", adjective, "colour", "and", "colour", noun);

    var modifier = modifiers.getFirst();
    var value =
        switch (modifier) {
          case FAMILIAR_EXP -> tier;
          case FAMILIAR_WEIGHT -> tier * 2 + 5 - mtRng.nextInt(2);
          case FAMILIAR_DAMAGE -> tier * 15 + mtRng.nextInt(tier * 10);
          default -> null;
        };

    if (value == null) return List.of();

    return List.of(new ModifierValue(modifier, value));
  }
}
