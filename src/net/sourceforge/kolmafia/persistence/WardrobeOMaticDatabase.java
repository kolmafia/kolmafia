package net.sourceforge.kolmafia.persistence;

import static net.sourceforge.kolmafia.modifiers.DoubleModifier.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.PHPRandom;

// Implementation ported from https://semenar.am/kol/wardrobe/js/seeding.js?version=4
public class WardrobeOMaticDatabase {
  private WardrobeOMaticDatabase() {}

  public record FuturisticClothing(
      String name, String image, Map<DoubleModifier, Integer> modifiers) {}

  private static final List<String> shirtAdjectives =
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

  private static final List<String> shirtMaterials =
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
          "linen",
          "wool");

  private static final List<String> shirtQualities =
      List.of("super", "hyper", "ultra", "mega", "reroll");

  private static final List<String> shirts =
      List.of(
          "t-shirt",
          "sweater",
          "jersey",
          "polo shirt",
          "dress shirt",
          "Neo-Hawaiian shirt",
          "sweatshirt");

  /**
   * The Futuristic Shirt generated on global day `day` at tier `tier`
   *
   * @param day Global KoL day, as in KoLCharacter.getGlobalDays()
   * @param tier Tier 1-5
   * @return Shirt record
   */
  public static FuturisticClothing shirt(int day, int tier) {
    var seed = day * 11391 + 2063;
    var mtRand = new PHPMTRandom(seed);
    var rand = new PHPRandom(seed);
    var imageNum = mtRand.nextInt(1, 9);

    mtRand.nextDouble();
    var adjective = mtRand.pickOne(shirtAdjectives);
    var matRoll = mtRand.nextInt(0, shirtMaterials.size() - 1);
    var material = shirtMaterials.get(matRoll);
    String quality = "";
    if (matRoll > 11) {
      quality = "reroll";
      while (quality.equals("reroll")) {
        quality = mtRand.pickOne(shirtQualities);
      }
    }
    var shirt = mtRand.pickOne(shirts);

    var shirtModifiers =
        new ArrayList<>(
            List.of(
                MUS,
                MYS,
                MOX,
                HOT_RESISTANCE,
                COLD_RESISTANCE,
                STENCH_RESISTANCE,
                SLEAZE_RESISTANCE,
                SPOOKY_RESISTANCE,
                HP,
                MP,
                HP_REGEN_MIN,
                MP_REGEN_MIN,
                DAMAGE_REDUCTION,
                DAMAGE_ABSORPTION));
    if (tier > 3) {
      Collections.addAll(shirtModifiers, ITEMDROP, MEATDROP, MONSTER_LEVEL);
    }
    rand.shuffle(shirtModifiers);
    var modMap = new LinkedHashMap<DoubleModifier, Integer>();
    for (var i = 0; i < tier; i++) {
      var mod = shirtModifiers.get(i);
      modMap.put(mod, modStrength(mtRand, mod, tier - 1));
      if (mod == HP_REGEN_MIN) {
        modMap.put(HP_REGEN_MAX, modStrength(mtRand, HP_REGEN_MAX, tier - 1));
      } else if (mod == MP_REGEN_MIN) {
        modMap.put(MP_REGEN_MAX, modStrength(mtRand, MP_REGEN_MAX, tier - 1));
      }
    }

    return new FuturisticClothing(
        adjective + " " + quality + material + " " + shirt, "jw_shirt" + imageNum + ".gif", modMap);
  }

  private static final List<String> hatAdjectives =
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

  private static final List<String> hatMaterials =
      List.of(
          "tungsten",
          "carbon",
          "steel",
          "aluminum",
          "titanium",
          "iron",
          "hafnium",
          "nickel",
          "zinc",
          "lead",
          "platinum",
          "copper",
          "silver",
          "tantalum",
          "niobium",
          "palladium",
          "iridium",
          "bismuth",
          "cobalt",
          "indium",
          "molybdenum",
          "vanadium",
          "yttrium",
          "antimony");

  private static final List<String> hatEmphasis =
      List.of(
          "super",
          "ultra",
          "mega",
          "double-",
          "gamma-",
          "uber-",
          "great-",
          "grand-",
          "maxi-",
          "multi-",
          "tri-",
          "duo-",
          "gargantu-",
          "crypto-",
          "hyper",
          "cyber-",
          "astro-",
          "grav-");

  private static final List<String> hats =
      List.of(
          "beanie",
          "fedora",
          "trilby",
          "beret",
          "visor",
          "turban",
          "fez",
          "balaclava",
          "tam",
          "sombrero",
          "bowler",
          "cloche",
          "tiara",
          "snood",
          "diadem",
          "crown",
          "bandana",
          "cowl",
          "capuchon");

  /**
   * The Futuristic Hat generated on global day `day` at tier `tier`
   *
   * @param day Global KoL day, as in KoLCharacter.getGlobalDays()
   * @param tier Tier 1-5
   * @return Hat record
   */
  public static FuturisticClothing hat(int day, int tier) {
    var seed = day * 11392 + 2063;
    var mtRand = new PHPMTRandom(seed);
    var rand = new PHPRandom(seed);
    var imageNum = mtRand.nextInt(1, 9);

    mtRand.nextDouble();
    mtRand.nextDouble();
    mtRand.nextDouble();
    var adjective = mtRand.pickOne(hatAdjectives);
    mtRand.nextDouble();
    var mat1 = mtRand.pickOne(hatMaterials);
    mtRand.nextDouble();
    var mat2 = mat1;
    while (mat1.equals(mat2)) {
      mat2 = mtRand.pickOne(hatMaterials);
    }
    var emphasis = mtRand.pickOne(hatEmphasis);
    var hat = mtRand.pickOne(hats);

    var hatModifiers =
        new ArrayList<>(
            List.of(
                MUS,
                MYS,
                MOX,
                HP,
                MP,
                HP_REGEN_MIN,
                MP_REGEN_MIN,
                HOT_DAMAGE,
                COLD_DAMAGE,
                STENCH_DAMAGE,
                SLEAZE_DAMAGE,
                SPOOKY_DAMAGE,
                HOT_SPELL_DAMAGE,
                COLD_SPELL_DAMAGE,
                STENCH_SPELL_DAMAGE,
                SLEAZE_SPELL_DAMAGE,
                SPOOKY_SPELL_DAMAGE));
    if (tier > 3) {
      Collections.addAll(hatModifiers, ITEMDROP, MEATDROP, MONSTER_LEVEL);
    }
    rand.shuffle(hatModifiers);
    var modMap = new LinkedHashMap<DoubleModifier, Integer>();
    for (var i = 0; i < tier; i++) {
      var mod = hatModifiers.get(i);
      modMap.put(mod, modStrength(mtRand, mod, tier - 1));
      if (mod == HP_REGEN_MIN) {
        modMap.put(HP_REGEN_MAX, modStrength(mtRand, HP_REGEN_MAX, tier - 1));
      } else if (mod == MP_REGEN_MIN) {
        modMap.put(MP_REGEN_MAX, modStrength(mtRand, MP_REGEN_MAX, tier - 1));
      }
    }

    return new FuturisticClothing(
        adjective + " " + mat1 + "-" + mat2 + " " + emphasis + hat,
        "jw_hat" + imageNum + ".gif",
        modMap);
  }

  private static final List<String> collars = List.of("pet tag", "collar", "pet sweater");

  private static final List<String> collarAdjectives =
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

  private static final List<String> collarColors =
      List.of(
          "amber",
          "aquamarine",
          "auburn",
          "azure",
          "beige",
          "black",
          "blue",
          "brown",
          "burgundy",
          "cerulean",
          "chartreuse",
          "cornflower",
          "cream",
          "crimson",
          "cyan",
          "ecru",
          "emerald",
          "fuchsia",
          "golden",
          "gray",
          "green",
          "indigo",
          "lavender",
          "lilac",
          "magenta",
          "maroon",
          "mauve",
          "mustard",
          "navy",
          "ochre",
          "olive",
          "orange",
          "periwinkle",
          "pink",
          "puce",
          "purple",
          "red",
          "rose",
          "ruby",
          "salmon",
          "scarlet",
          "sepia",
          "sienna",
          "silver",
          "tan",
          "taupe",
          "teal",
          "turquoise",
          "ultramarine",
          "vermilion",
          "violet",
          "viridian",
          "white",
          "yellow");

  /**
   * The Futuristic Collar generated on global day `day` at tier `tier`
   *
   * @param day Global KoL day, as in KoLCharacter.getGlobalDays()
   * @param tier Tier 1-5
   * @return Collar record
   */
  public static FuturisticClothing collar(int day, int tier) {
    var seed = day * 11393 + 2063;
    var mtRand = new PHPMTRandom(seed);
    var rand = new PHPRandom(seed);
    var imageNum = mtRand.nextInt(1, 9);

    mtRand.nextDouble();
    var adjective = mtRand.pickOne(collarAdjectives);
    mtRand.nextDouble();
    var and = mtRand.nextInt(0, 1) == 0 ? "-" : " and ";
    var color1 = mtRand.pickOne(collarColors);
    mtRand.nextDouble();
    var color2 = color1;
    while (color1.equals(color2)) {
      color2 = mtRand.pickOne(collarColors);
    }

    var collarModifiers = new ArrayList<>(List.of(FAMILIAR_WEIGHT, FAMILIAR_DAMAGE));
    if (tier > 3) {
      collarModifiers.add(FAMILIAR_EXP);
    }
    rand.shuffle(collarModifiers);
    var modMap = new LinkedHashMap<DoubleModifier, Integer>();
    var mod = collarModifiers.get(0);
    modMap.put(mod, modStrength(mtRand, mod, tier - 1));

    return new FuturisticClothing(
        adjective + " " + color1 + and + color2 + " " + collars.get((imageNum - 1) / 3),
        "jw_pet" + imageNum + ".gif",
        modMap);
  }

  private static int modStrength(PHPMTRandom mtRand, DoubleModifier mod, int level) {
    return switch (mod) {
      case MUS, MYS, MOX -> mtRand.nextInt(10 * level + 10, 12 * level + 12);
      case HOT_RESISTANCE,
          COLD_RESISTANCE,
          STENCH_RESISTANCE,
          SPOOKY_RESISTANCE,
          SLEAZE_RESISTANCE -> mtRand.nextInt(level + 1, level + 3);
      case HP, MP -> mtRand.nextInt(20 * level + 10, 20 * level + 30);
      case HP_REGEN_MIN -> mtRand.nextInt(2 * level + 2, 2 * level + 4);
      case MP_REGEN_MIN -> mtRand.nextInt(3 * level + 3, 3 * level + 5);
      case HP_REGEN_MAX, MP_REGEN_MAX -> mtRand.nextInt(5 * level + 5, 5 * level + 10);
      case DAMAGE_REDUCTION -> mtRand.nextInt(3 * level + 1, 3 * level + 5);
      case DAMAGE_ABSORPTION -> mtRand.nextInt(10 * level + 10, 15 * level + 15);
      case ITEMDROP -> mtRand.nextInt(5 * level + 3, 5 * level + 8);
      case MEATDROP -> mtRand.nextInt(10 * level + 5, 10 * level + 15);
      case MONSTER_LEVEL -> mtRand.nextInt(5 * level, 5 * level + 10);
      case HOT_DAMAGE,
          COLD_DAMAGE,
          STENCH_DAMAGE,
          SPOOKY_DAMAGE,
          SLEAZE_DAMAGE,
          HOT_SPELL_DAMAGE,
          COLD_SPELL_DAMAGE,
          STENCH_SPELL_DAMAGE,
          SPOOKY_SPELL_DAMAGE,
          SLEAZE_SPELL_DAMAGE -> mtRand.nextInt(4 * level + 4, 6 * level + 6);
      case FAMILIAR_WEIGHT -> 7 + 2 * level - mtRand.nextInt(0, 2);
      case FAMILIAR_DAMAGE -> mtRand.nextInt(15 * level + 15, 25 * level + 25);
      case FAMILIAR_EXP -> level + 1;
      default -> 0;
    };
  }
}
