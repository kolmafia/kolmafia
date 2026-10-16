package net.sourceforge.kolmafia.session;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.PHPRandom;
import net.sourceforge.kolmafia.utilities.PHPRandomSelection;

public class SpacegateManager {
  private static final List<String> GREEK_LETTERS =
      List.of(
          "Alpha", "Beta", "Gamma", "Delta", "Epsilon", "Zeta", "Eta", "Theta", "Iota", "Kappa",
          "Lambda", "Mu", "Nu", "Xi", "Omicron", "Pi", "Rho", "Sigma", "Tau", "Upsilon", "Phi",
          "Chi", "Psi", "Omega");
  private static final List<String> LATIN_NAMES =
      List.of(
          "Unicornius",
          "Dominatrix",
          "Cattusdoggus",
          "Cardboardius",
          "Bubblewrapium",
          "Linoleus",
          "Dickbutkus",
          "Corpulus",
          "Perambulus",
          "Lepidopterus",
          "Doritus",
          "Fritus",
          "Cheetus",
          "Belletaco",
          "Bellegrandus",
          "Blivebarkerus",
          "Bearthurus",
          "Goldthwait",
          "Molotov",
          "Microscopius",
          "Cancerus",
          "Leprus",
          "Scabrus",
          "Fredfordus",
          "Paulreichus",
          "Paulreiserus",
          "Hamburguesus",
          "Equipajus",
          "Perfunctus",
          "Saladus",
          "Milkius",
          "Crematorius",
          "Dyxichix",
          "Gronulus",
          "Discothecus",
          "Misogynus",
          "Allopecius",
          "Mousketeerus",
          "Cromulus",
          "Gromulus",
          "Slobulus",
          "Crustulus",
          "Fustulus",
          "Crunkuli",
          "Yodeli",
          "Humptydumptus",
          "Pacmanicus",
          "Diggusduggus",
          "Polepositioni",
          "Hippidipsus",
          "Pelicanus",
          "Mexicanus",
          "Abbrevianus",
          "Pannus",
          "Diverticulus",
          "Conjunctivitus",
          "Phlebitis",
          "Billclintonus",
          "Dudebaddus",
          "Reaganus",
          "Trixrabbitus",
          "Bananaramus",
          "Bubblicius",
          "Waldorfus",
          "Bob");
  private static final List<String> ENGLISH_NAMES =
      List.of(
          "Wallace",
          "Reynolds",
          "McDonald",
          "McDurkee",
          "Shafer",
          "Johnson",
          "Jones",
          "Smith",
          "Brown",
          "Thompson",
          "Zevon",
          "McNichols",
          "DuPree",
          "Des Moines",
          "Salisbury",
          "Simon",
          "Garfunkel",
          "Brooks",
          "Dunn",
          "Haggard",
          "Nelson",
          "Meadows",
          "Goodman",
          "Smith",
          "Johnson",
          "Williams",
          "Jones",
          "Brown",
          "Davis",
          "Miller",
          "Wilson",
          "Moore",
          "Taylor",
          "Anderson",
          "Thomas",
          "Jackson",
          "White",
          "Harris",
          "Martin",
          "Thompson",
          "Garcia",
          "Martinez",
          "Robinson",
          "Clark",
          "Rodriguez",
          "Lewis",
          "Lee",
          "Walker",
          "Hall",
          "Allen",
          "Young",
          "Hernandez",
          "King",
          "Disraeli",
          "Khan",
          "Shatner",
          "Shannahan",
          "O'Mally",
          "O'Reilly",
          "O'Sullivan",
          "Sullivan",
          "Peterson",
          "Steadwick",
          "Nite",
          "O'Kelly",
          "Hathaway",
          "Symington",
          "Belvedere",
          "Cartwright",
          "Spitzner",
          "Pennington",
          "Young",
          "Woodward",
          "Nipperus",
          "Paulson",
          "Durden",
          "Mills",
          "Stipe",
          "Buck",
          "Berry",
          "Rudd",
          "Stemple",
          "Rutgers",
          "Stemple",
          "Nichols",
          "Nicholson",
          "Franklin",
          "Roosevelt",
          "Washington",
          "Benedict",
          "Madison",
          "Adams",
          "Garfield",
          "Harrison",
          "Harris",
          "Taylor",
          "Tyler",
          "Draper",
          "Cooper",
          "Black",
          "Lennon",
          "McCartney",
          "McMahon",
          "Isaacson",
          "Clinton",
          "Bush",
          "Reagan",
          "Noriega",
          "North",
          "Easthouse",
          "Westinghouse",
          "Sanford",
          "Huxtable",
          "Romero",
          "Capulet",
          "Montague",
          "Waldorf",
          "Haskell",
          "Mack",
          "Mott",
          "Goldthwait");

  private static final List<String> FIRST_LINE_TEMPLATES =
      List.of(
          "The sky above the horizon is %SKYTYPE% %COLOR%.",
          "The sky above the horizon is %SKYTYPE% %COLOR%, %FEATURE%.",
          "The planet's sky is %SKYTYPE% %COLOR%.",
          "The planet's sky is %SKYTYPE% %COLOR%, %FEATURE%.",
          "The sky is %SKYTYPE% %COLOR%.",
          "The sky is %SKYTYPE% %COLOR%, %FEATURE%.",
          "The sky is %COLOR% fading into %COLOR2% near the horizon.",
          "The sky is %COLOR% fading into %COLOR2% near the horizon, %FEATURE%.",
          "");

  private static final List<String> SKY_TYPES =
      List.of(
          "a uniform",
          "a pale",
          "a brilliant",
          "a deep",
          "a vivid",
          "a bleached",
          "a bright",
          "a dim",
          "a hazy",
          "an oppressive");

  private static final List<String> SKY_COLORS =
      List.of("orange", "red", "pink", "blue", "yellow", "green", "%RARECOLOR%");

  private static final List<String> RARE_SKY_COLORS =
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

  private static final List<String> SKY_FEATURES =
      List.of(
          "dotted by occasional wispy %COLOR% clouds",
          "with a dense covering of %COLOR% clouds",
          "speckled with puffy %COLOR% clouds",
          "and a %COLOR% ring arcs across near the horizon",
          "dominated by a system of %COLOR% rings",
          "punctuated by mottled %CLOUDCOLOR% and %CLOUDCOLOR% clouds",
          "");

  private static final List<String> SKY_FEATURE_COLORS =
      List.of("white", "grey", "pink", "pale blue", "pale orange", "black", "%RARECOLOR%-colored");

  private static final List<String> SECOND_LINE_TEMPLATES =
      List.of(
          "The system's single, distant sun hangs dimly near the horizon.",
          "A bright yellow, almost Sol-like sun hangs overhead.",
          "The system's twin %COLOR% suns loom above you.",
          "The landscape is lit by %TYPE% %COLOR% sun.",
          "The landscape is lit by %TYPE% %COLOR% sun.",
          "The landscape is lit by %TYPE% %COLOR% sun.",
          "The landscape is lit by %TYPE% %COLOR% sun.",
          "The landscape is lit by %TYPE% %COLOR% sun.",
          "The landscape is lit by %TYPE% %COLOR% sun.",
          "The landscape is lit by %TYPE% %COLOR% sun.",
          "The landscape is lit by %TYPE% %COLOR% sun.",
          "It is twilight -- the planet's sun is just out of sight.",
          "The planet's sun (if it has one) is nowhere to be seen.");

  private static final List<String> SUN_COLORS =
      List.of("orange", "white", "red", "yellow", "blue");

  private static final List<String> SUN_TYPES =
      List.of(
          "a blazing",
          "a brilliant",
          "a distant",
          "an incredibly close",
          "a sky-dominating",
          "a pale",
          "a medium-sized",
          "a dim",
          "a bright");

  private static final List<String> THIRD_LINE_TEMPLATES =
      List.of(
          "There are no moons visible.",
          "The sky is moonless.",
          "The planet doesn't appear to have any satellites.",
          "A %TYPE% moon hangs above the horizon.",
          "Two %TYPE% moons punctuate the sky.",
          "Three %TYPE% moons hang in the sky.",
          "Several %TYPE% moons pepper the planet's ecliptic plane.",
          "A single massive moon dominates the dome of the sky.",
          "A single %TYPE% moon punctuates the expanse of sky.");

  private static final List<String> MOON_TYPES =
      List.of(
          "crescent",
          "tiny",
          "medium-sized",
          "cratered",
          "gibbous",
          "waxing",
          "waning",
          "pale",
          "hazy",
          "small",
          "gaseous",
          "shimmering",
          "ovoid",
          "oblong",
          "forested",
          "craggy",
          "");

  private static final List<String> ENV_HAZARDS =
      List.of("toxic atmosphere", "high gravity", "irradiated", "magnetic storms", "high winds");

  private static final List<String> ELEM_HAZARDS =
      List.of(
          "hot solar flares",
          "frigid zones",
          "scary noises",
          "nasty gasses",
          "lewd rock formations");

  public record SpacegatePlanet(
      String coords,
      String name,
      List<String> description,
      List<String> envHazards,
      List<String> elemHazards,
      String plants,
      String animals,
      String aliens,
      AdventureResult alienItem,
      int alienItemPrice,
      String spants,
      String murderbots,
      String quest,
      int questStage,
      int rocksLevel) {}

  public static SpacegatePlanet predictPlanet(String coords) {
    int pseed = getPlanetSeed(coords);

    // The cosmetic components are each produced using their own PHPMTRandom.
    String name = predictName(pseed);
    List<String> desc = predictDesc(pseed);

    // The mechanical components are all calulcated one after the other from a single PHPRandom
    // instance.
    PHPRandom rng = new PHPRandom(pseed);
    List<String> envHazards = predictEnvHazards(coords.charAt(0), rng);
    List<String> elemHazards = predictElemHazards(coords.charAt(0), rng);
    String plants = predictWildlife(pseed, coords.charAt(1), rng);
    String animals = predictWildlife(pseed, coords.charAt(2), rng);
    Aliens aliens = predictAliens(pseed, coords.charAt(3), rng);
    String spants = predictArmy(coords.charAt(4), rng);
    String murderbots = predictArmy(coords.charAt(5), rng);
    Quest ruins = predictRuins(pseed, coords.charAt(6), rng);
    int rocks = predictRocks(plants, animals, aliens.type(), spants, murderbots);

    return new SpacegatePlanet(
        coords,
        name,
        desc,
        envHazards,
        elemHazards,
        plants,
        animals,
        aliens.type(),
        aliens.item(),
        aliens.price(),
        spants,
        murderbots,
        ruins.type(),
        ruins.stage(),
        rocks);
  }

  // The planet seed is a weighted sum of the ASCII values of the planet's coordinates and is used
  // to seed the RNG for all planetary features.
  public static int getPlanetSeed(String coords) {
    int rv = 0;
    for (int i = 0; i < 7; i++) {
      rv += (10000 + i) * coords.charAt(i);
    }
    return rv;
  }

  private static String predictName(int pseed) {
    PHPRandomSelection phpRng = new PHPRandomSelection(pseed);
    PHPMTRandom rng = phpRng.mtRng;

    int v = rng.nextInt(1, 5);
    // V=1: Greek Latin
    // V=2: Greek English
    // V=3: Greek Latin
    // V=4: Greek English
    // V=5: Latin English
    String firstWord;
    String secondWord;
    if (v < 5) {
      firstWord = rng.pickOne(GREEK_LETTERS);
    } else {
      firstWord = rng.pickOne(LATIN_NAMES);
    }
    if (v == 1 || v == 3) {
      secondWord = rng.pickOne(LATIN_NAMES);
    } else {
      secondWord = rng.pickOne(ENGLISH_NAMES);
    }

    // Roman numeral chosen from 1 to 13, re-rolling on 13 and mapping both 11 and 12 to XI.
    v = phpRng.pick(12, 1)[0] + 1;
    String thirdWord =
        switch (v) {
          case 1 -> "I";
          case 2 -> "II";
          case 3 -> "III";
          case 4 -> "IV";
          case 5 -> "V";
          case 6 -> "VI";
          case 7 -> "VII";
          case 8 -> "VIII";
          case 9 -> "IX";
          case 10 -> "X";
          case 11, 12 -> "XI";
          default -> "";
        };

    return firstWord + " " + secondWord + " " + thirdWord;
  }

  private static List<String> predictDesc(int pseed) {
    PHPMTRandom r = new PHPMTRandom(pseed);
    r.nextInt();

    String firstLine = "";
    while (firstLine.isBlank()) {
      firstLine = r.pickOne(FIRST_LINE_TEMPLATES);
    }
    if (firstLine.contains("%SKYTYPE%")) {
      String skyType = r.pickOne(SKY_TYPES);
      firstLine = firstLine.replaceFirst("%SKYTYPE%", skyType);
    }
    // I don't know why it checks for %COLOR2% here rather than %COLOR%, but this is the way it
    // works.
    if (firstLine.contains("%COLOR2%")) {
      rngBurn(r);
    }
    String firstColor = r.pickOne(SKY_COLORS);
    if (firstColor.equals("%RARECOLOR%")) {
      firstColor = r.pickOne(RARE_SKY_COLORS);
    }
    firstLine = firstLine.replaceFirst("%COLOR%", firstColor);
    if (firstLine.contains("%COLOR2%")) {
      rngBurn(r);
      String secondColor = firstColor;
      while (secondColor.equals(firstColor)) {
        secondColor = r.pickOne(SKY_COLORS);
        if (secondColor.equals("%RARECOLOR%")) {
          secondColor = r.pickOne(RARE_SKY_COLORS);
        }
      }
      firstLine = firstLine.replaceFirst("%COLOR2%", secondColor);
    }
    if (firstLine.contains("%FEATURE%")) {
      String feature = "";
      while (feature.isBlank()) feature = r.pickOne(SKY_FEATURES);
      firstLine = firstLine.replaceFirst("%FEATURE%", feature);
    }
    if (firstLine.contains("%COLOR%")) {
      String sfColor = r.pickOne(SKY_FEATURE_COLORS);
      if (sfColor.contains("%RARECOLOR%")) {
        sfColor = sfColor.replaceFirst("%RARECOLOR%", r.pickOne(RARE_SKY_COLORS));
      }
      firstLine = firstLine.replaceFirst("%COLOR%", sfColor);
    }
    if (firstLine.contains("%CLOUDCOLOR%")) {
      rngBurn(r);
      firstColor = r.pickOne(SKY_FEATURE_COLORS);
      if (firstColor.contains("%RARECOLOR%")) {
        firstColor = firstColor.replaceFirst("%RARECOLOR%", r.pickOne(RARE_SKY_COLORS));
      }
      firstLine = firstLine.replaceFirst("%CLOUDCOLOR%", firstColor);
      rngBurn(r);
      String secondColor = firstColor;
      while (secondColor.equals(firstColor)) {
        secondColor = r.pickOne(SKY_FEATURE_COLORS);
        if (secondColor.contains("%RARECOLOR%")) {
          secondColor = secondColor.replaceFirst("%RARECOLOR%", r.pickOne(RARE_SKY_COLORS));
        }
      }
      firstLine = firstLine.replaceFirst("%CLOUDCOLOR%", secondColor);
    }

    String secondLine = r.pickOne(SECOND_LINE_TEMPLATES);
    if (secondLine.contains("%TYPE%")) {
      secondLine = secondLine.replaceFirst("%TYPE%", r.pickOne(SUN_TYPES));
    }
    if (secondLine.contains("%COLOR%")) {
      secondLine = secondLine.replaceFirst("%COLOR%", r.pickOne(SUN_COLORS));
    }

    String thirdLine = r.pickOne(THIRD_LINE_TEMPLATES);
    if (thirdLine.contains("%TYPE%")) {
      String moonType = "";
      while (moonType.isBlank()) {
        moonType = r.pickOne(MOON_TYPES);
      }
      thirdLine = thirdLine.replaceFirst("%TYPE%", moonType);
    }

    return List.of(firstLine, secondLine, thirdLine);
  }

  // This probably was intended to do... something, but it doesn't appear to have any impact other
  // than advancing the RNG in some spots. Presumably some weirdness in how the wordbuckets are laid
  // out.
  private static void rngBurn(PHPMTRandom rng) {
    if (rng.nextInt(0, 6) == 6) {
      rng.nextInt();
    }
  }

  // This is the core mechanism for most of the mechanical traits. Shuffle a list of numbers and
  // take the one at an index determined by one of the letters in the coordinate (chosen based on
  // which feature is being randomized). Then map that number to an outcome.
  private static int getTraitValue(char keyLetter, PHPRandom rng) {
    List<Integer> arr =
        IntStream.rangeClosed(0, 25).boxed().collect(Collectors.toCollection(ArrayList::new));
    rng.shuffle(arr);
    return arr.get(keyLetter - 'A');
  }

  private static List<String> predictEnvHazards(char difficultyLetter, PHPRandom rng) {
    int difficulty = difficultyLetter - 'A';
    int num = (difficulty + 11) / 10;
    return predictHazards(num, ENV_HAZARDS, rng);
  }

  private static List<String> predictElemHazards(char difficultyLetter, PHPRandom rng) {
    int difficulty = difficultyLetter - 'A';
    int num = (difficulty + 5) / 10;
    return predictHazards(num, ELEM_HAZARDS, rng);
  }

  private static List<String> predictHazards(int num, List<String> allHazards, PHPRandom rng) {
    List<String> hazards = new ArrayList<>(allHazards);
    rng.shuffle(hazards);

    // Always list hazards in the same order
    return allHazards.stream()
        .filter(h -> hazards.subList(0, num).contains(h))
        .collect(Collectors.toCollection(ArrayList::new));
  }

  // Same mechanism for plants and animals, differing only in which position of the coordinate is
  // used to index the shuffled array.
  private static String predictWildlife(int planetSeed, char keyLetter, PHPRandom rng) {
    int v = getTraitValue(keyLetter, rng);
    if (v < 10) {
      return "none";
    } else if (v < 15) {
      return "friendly simple";
    } else if (v < 20) {
      return "hostile simple";
    } else if (v < 23) {
      return planetSeed % 5 == 0 ? "friendly anomalous" : "friendly complex";
    } else {
      return planetSeed % 5 == 0 ? "hostile anomalous" : "hostile complex";
    }
  }

  private record Aliens(String type, AdventureResult item, int price) {}

  // The item dropped or traded by the aliens is determined from a fresh PHPMTRandom instance. This
  // is why the dropped or traded items are correlated to certain monster images (which are also
  // selected using the first roll from a fresh PHPMTRandom instance).
  private static Aliens predictAliens(int planetSeed, char keyLetter, PHPRandom rng) {
    int v = getTraitValue(keyLetter, rng);
    if (v < 15) {
      return new Aliens("none", null, 0);
    } else if (v < 23) {
      PHPMTRandom irng = new PHPMTRandom(planetSeed);
      int itemID =
          switch (irng.nextInt(1, 5)) {
            case 1 -> ItemPool.PRIMITIVE_ALIEN_SPEAR;
            case 2 -> ItemPool.PRIMITIVE_ALIEN_BLOWGUN;
            case 3 -> ItemPool.PRIMITIVE_ALIEN_LOINCLOTH;
            case 4 -> ItemPool.PRIMITIVE_ALIEN_TOTEM;
            case 5 -> ItemPool.PRIMITIVE_ALIEN_NECKLACE;
            default -> 0;
          };
      return new Aliens("hostile", ItemPool.get(itemID), 0);
    } else {
      PHPMTRandom irng = new PHPMTRandom(planetSeed);
      int itemID =
          switch (irng.nextInt(1, 7)) {
            case 1 -> ItemPool.PRIMITIVE_ALIEN_MASK;
            case 2, 3 -> ItemPool.PRIMITIVE_ALIEN_BOOZE;
            case 4, 5 -> ItemPool.PRIMITIVE_ALIEN_SALAD;
            case 6, 7 -> ItemPool.PRIMITIVE_ALIEN_MEDICINE;
            default -> 0;
          };
      int price = 100 * irng.nextInt(100, 300);
      if (itemID == ItemPool.PRIMITIVE_ALIEN_MASK) {
        price *= 10;
      }
      return new Aliens("friendly", ItemPool.get(itemID), price);
    }
  }

  // Same mechanism for spants and murderbots, differing only in which position of the coordinate is
  // used to index the shuffled array.
  private static String predictArmy(char keyLetter, PHPRandom rng) {
    int v = getTraitValue(keyLetter, rng);
    if (v < 15) {
      return "none";
    } else if (v < 20) {
      return "drones";
    } else if (v < 23) {
      return "drones and soldiers";
    } else {
      return "artifact";
    }
  }

  private record Quest(String type, int stage) {}

  private static Quest predictRuins(int planetSeed, char keyLetter, PHPRandom rng) {
    int v = getTraitValue(keyLetter, rng);
    int step;
    if (v < 16) {
      step = 0;
    } else if (v < 20) {
      step = 1;
    } else if (v < 23) {
      step = 2;
    } else {
      step = 3;
    }

    if (step == 0) {
      return new Quest("none", 0);
    } else {
      String type =
          switch (planetSeed % 3) {
            case 0 -> "space pirates";
            case 1 -> "procrastinator";
            case 2 -> "space baby";
            default -> "";
          };
      return new Quest(type, step);
    }
  }

  // Rocks are not determined via a random process. Which rock NCs are available is determined by
  // how many other features are present on the planet among plants, animals, aliens, spants, and
  // murderbots.
  //
  // 0 features missing: Space Cave
  // 1-2 features missing: Space Cave and Cool Space Rocks
  // 3+ features missing: Space Cave, Cool Space Rocks, and Wide Open Spaces
  private static int predictRocks(
      String plants, String animals, String aliens, String spants, String murderbots) {
    int zeroes = 0;
    if (plants.equals("none")) {
      zeroes++;
    }
    if (animals.equals("none")) {
      zeroes++;
    }
    if (aliens.equals("none")) {
      zeroes++;
    }
    if (spants.equals("none")) {
      zeroes++;
    }
    if (murderbots.equals("none")) {
      zeroes++;
    }

    if (zeroes < 1) {
      return 1;
    } else if (zeroes < 3) {
      return 2;
    } else {
      return 3;
    }
  }
}
