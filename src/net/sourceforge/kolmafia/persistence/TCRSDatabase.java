package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.IOException;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.Lookup;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.modifiers.MultiStringModifier;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase.ConsumableQuality;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.PHPRandom;
import net.sourceforge.kolmafia.utilities.StringUtilities;

public class TCRSDatabase {
  private TCRSDatabase() {}

  // Item attributes that vary by class/sign in a Two Random Crazy Summer run
  public static class TCRS {
    public final String name;
    public final int size;
    public final ConsumableQuality quality;
    public final String modifiers;

    TCRS(String name, int size, ConsumableQuality quality, String modifiers) {
      this.name = name;
      this.size = size;
      this.quality = quality;
      this.modifiers = modifiers;
    }
  }

  private record TCRSDeriveRunnable(int itemId) implements Runnable {
    @Override
    public void run() {
      String text = DebugDatabase.itemDescriptionText(itemId, false);
      if (text == null) {
        return;
      }

      TCRS tcrs = deriveItem(itemId, text);

      TCRSMap.put(itemId, tcrs);
    }
  }

  private static String currentClassSign; // Character class/Zodiac Sign

  // Sorted by itemId
  private static final Map<Integer, TCRS> TCRSMap = new TreeMap<>();
  private static final Map<Integer, TCRS> TCRSBoozeMap =
      new TreeMap<>(new CafeDatabase.InverseIntegerOrder());
  private static final Map<Integer, TCRS> TCRSFoodMap =
      new TreeMap<>(new CafeDatabase.InverseIntegerOrder());

  private static final List<Integer> TCRSEffectPool = new ArrayList<Integer>();

  static {
    TCRSDatabase.reset();
  }

  public static void reset() {
    currentClassSign = "";
    TCRSMap.clear();
    TCRSBoozeMap.clear();
    TCRSFoodMap.clear();
    TCRSEffectPool.clear();
    getEffectPool();
  }

  public static boolean hasData(int itemId) {
    return TCRSMap.containsKey(itemId);
  }

  public static String getTCRSName(int itemId) {
    TCRS tcrs = TCRSMap.get(itemId);
    return (tcrs == null) ? ItemDatabase.getDataName(itemId) : tcrs.name;
  }

  public static TCRS getData(int itemId) {
    return TCRSMap.get(itemId);
  }

  public static String filename() {
    return filename(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), "");
  }

  public static boolean validate(AscensionClass ascensionClass, ZodiacSign csign) {
    return (ascensionClass != null && ascensionClass.isStandard() && csign.isStandard());
  }

  public static String filename(AscensionClass ascensionClass, ZodiacSign sign, String suffix) {
    if (!validate(ascensionClass, sign)) {
      return "";
    }

    return "TCRS/TCRS_"
        + StringUtilities.globalStringReplace(ascensionClass.getName(), " ", "_")
        + "_"
        + sign.getName()
        + suffix
        + ".txt";
  }

  public static boolean load(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return false;
    }
    boolean retval = true;
    retval &= load(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
    retval &= loadCafe(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
    return retval;
  }

  public static boolean load(
      AscensionClass ascensionClass, ZodiacSign csign, final boolean verbose) {
    if (load(filename(ascensionClass, csign, ""), TCRSMap, verbose)) {
      currentClassSign = ascensionClass.getName() + "/" + csign;
      return true;
    }
    return false;
  }

  public static boolean loadCafe(
      AscensionClass ascensionClass, ZodiacSign csign, final boolean verbose) {
    boolean retval = true;
    retval &= load(filename(ascensionClass, csign, "_cafe_booze"), TCRSBoozeMap, verbose);
    retval &= load(filename(ascensionClass, csign, "_cafe_food"), TCRSFoodMap, verbose);
    return retval;
  }

  private static boolean load(String fileName, Map<Integer, TCRS> map, final boolean verbose) {
    map.clear();

    try (BufferedReader reader = FileUtilities.getReader(fileName)) {
      // No reader, no file
      if (reader == null) {
        if (verbose) {
          RequestLogger.printLine("Could not read file " + fileName);
        }
        return false;
      }

      String[] data;

      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length < 5) {
          continue;
        }
        int itemId = StringUtilities.parseInt(data[0]);
        String name = data[1];
        int size = StringUtilities.parseInt(data[2]);
        var quality = ConsumableQuality.find(data[3]);
        String modifiers = data[4];

        TCRS item = new TCRS(name, size, quality, modifiers);
        map.put(itemId, item);
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    if (verbose) {
      RequestLogger.printLine("Read file " + fileName);
    }

    return true;
  }

  public static boolean derive(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return false;
    }

    derive(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
    deriveCafe(verbose);
    return true;
  }

  private static boolean derive(
      final AscensionClass ascensionClass, final ZodiacSign sign, final boolean verbose) {
    // If we don't currently have data for this class/sign, start fresh
    String classSign = ascensionClass.getName() + "/" + sign;
    if (!currentClassSign.equals(classSign)) {
      reset();
    }

    Set<Integer> keys = ItemDatabase.descriptionIdKeySet();

    if (verbose) {
      KoLmafia.updateDisplay("Deriving TCRS item adjustments for all real items...");
    }

    List<Runnable> actions = new ArrayList<>();

    for (Integer id : keys) {
      actions.add(new TCRSDeriveRunnable(id));
    }

    RequestThread.runInParallel(actions, verbose);

    currentClassSign = classSign;

    if (verbose) {
      KoLmafia.updateDisplay("Done!");
    }

    return true;
  }

  public static boolean derive(final int itemId) {
    // Don't do this if we already know the item
    if (TCRSMap.containsKey(itemId)) {
      return false;
    }

    TCRS tcrs = deriveItem(itemId);
    if (tcrs == null) {
      return false;
    }

    TCRSMap.put(itemId, tcrs);

    return true;
  }

  public static int update(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return 0;
    }

    Set<Integer> keys = ItemDatabase.descriptionIdKeySet();

    if (verbose) {
      KoLmafia.updateDisplay("Updating TCRS item adjustments for real items...");
    }

    int count = 0;
    for (Integer id : keys) {
      // For a while, we stored the hewn moon-rune spoon
      // without modifiers.  If the data file we loaded has
      // that, force derive here to get the real modifiers.
      if (id == ItemPool.HEWN_MOON_RUNE_SPOON) {
        TCRS tcrs = TCRSMap.get(id);
        if (tcrs != null && "hewn moon-rune spoon".equals(tcrs.name)) {
          TCRSMap.remove(id);
        }
      }

      if (derive(id)) {
        count++;
      }
    }

    if (verbose) {
      KoLmafia.updateDisplay(count + " new items seen");
    }

    return count;
  }

  public static int updateCafeBooze(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return 0;
    }

    if (verbose) {
      KoLmafia.updateDisplay("Updating TCRS item adjustments for cafe booze items...");
    }

    int count = 0;
    for (Integer id : CafeDatabase.cafeBoozeKeySet()) {
      if (deriveCafe(id, CafeDatabase.boozeDescId(id), TCRSBoozeMap)) {
        count++;
      }
    }

    if (verbose) {
      KoLmafia.updateDisplay(count + " new cafe boozes seen");
    }

    return count;
  }

  public static int updateCafeFood(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return 0;
    }

    if (verbose) {
      KoLmafia.updateDisplay("Updating TCRS item adjustments for cafe food items...");
    }

    int count = 0;
    for (Integer id : CafeDatabase.cafeFoodKeySet()) {
      if (deriveCafe(id, CafeDatabase.foodDescId(id), TCRSFoodMap)) {
        count++;
      }
    }

    if (verbose) {
      KoLmafia.updateDisplay(count + " new cafe foods seen");
    }

    return count;
  }

  public static TCRS deriveItem(final int itemId) {
    // The "ring" is the path reward for completing a TCRS run.
    // Its enchantments are character-specific.
    if (itemId == ItemPool.RING) {
      return new TCRS("ring", 0, ConsumableQuality.NONE, "Single Equip");
    }

    // Read the Item Description
    String text = DebugDatabase.itemDescriptionText(itemId, false);
    if (text == null) {
      return null;
    }

    return deriveItem(itemId, text);
  }

  public static TCRS deriveAndSaveItem(final int itemId) {
    TCRS tcrs = deriveItem(itemId);
    if (tcrs != null) {
      TCRSMap.put(itemId, tcrs);
    }
    return tcrs;
  }

  public static TCRS deriveRing() {
    String text = DebugDatabase.itemDescriptionText(ItemPool.RING, false);
    return deriveItem(ItemPool.RING, text);
  }

  public static TCRS deriveSpoon() {
    String text = DebugDatabase.itemDescriptionText(ItemPool.HEWN_MOON_RUNE_SPOON, false);
    return deriveItem(ItemPool.HEWN_MOON_RUNE_SPOON, text);
  }

  public static void deriveApplyItem(final int id) {
    String text = DebugDatabase.itemDescriptionText(id, false);

    // should only be null in tests, but setting up the builder is hard
    if (text != null) {
      applyModifiers(id, deriveItem(id, text));
    }
  }

  private static final Set<Modifier> CARRIED_OVER =
      Set.of(
          MultiStringModifier.CONDITIONAL_SKILL_EQUIPPED,
          MultiStringModifier.CONDITIONAL_SKILL_INVENTORY,
          StringModifier.WIKI_NAME,
          StringModifier.LAST_AVAILABLE_DATE,
          StringModifier.RECIPE,
          StringModifier.CLASS,
          StringModifier.SKILL,
          StringModifier.EQUIPS_ON,
          BitmapModifier.BRIMSTONE,
          BitmapModifier.CLOATHING,
          BitmapModifier.SYNERGETIC,
          BitmapModifier.RAVEOSITY,
          BitmapModifier.MCHUGELARGE,
          BitmapModifier.STINKYCHEESE);

  private static List<String> carriedOverModifiers(final int itemId) {
    var modifiers = ModifierDatabase.getItemModifiers(itemId);
    if (modifiers == null) {
      return List.of();
    }

    return CARRIED_OVER.stream()
        .map(
            mod -> {
              var name = mod.getName();
              if (mod instanceof MultiStringModifier m) {
                var value = modifiers.getStrings(m);
                if (!value.isEmpty())
                  return value.stream()
                      .map(s -> name + ": \"" + s + "\"")
                      .collect(Collectors.joining(", "));
              }
              if (mod instanceof StringModifier s) {
                var value = modifiers.getString(s);
                if (!value.isBlank()) return name + ": \"" + value + "\"";
              }
              return "";
            })
        .filter(Predicate.not(String::isBlank))
        .toList();
  }

  private static TCRS deriveItem(final int itemId, final String text) {
    // Parse the things that are changed in TCRS
    String name = DebugDatabase.parseName(text);
    int size = DebugDatabase.parseConsumableSize(text);
    var quality = DebugDatabase.parseQuality(text);
    ArrayList<String> unknown = new ArrayList<>();
    StringBuilder modifiers =
        new StringBuilder(
            DebugDatabase.parseItemEnchantments(text, unknown, ConsumptionType.UNKNOWN));

    var carriedOver = carriedOverModifiers(itemId);
    for (var mod : carriedOver) {
      if (modifiers.toString().contains(mod)) {
        continue;
      }
      if (!modifiers.toString().isBlank()) {
        modifiers.append(", ");
      }
      modifiers.append(mod);
    }

    // Create and return the TCRS object
    return new TCRS(name, size, quality, modifiers.toString());
  }

  private static final List<String> COLOR_MODS =
      List.of(
          "red",
          "lime green",
          "blue",
          "gray",
          "maroon",
          "yellow",
          "olive",
          "cyan",
          "teal",
          "green",
          "fuchsia",
          "purple");

  private static final List<String> COSMETIC_MODS =
      List.of(
          "narrow",
          "huge",
          "skewed",
          "blinking",
          "upside-down",
          "mirror",
          "wobbly",
          "twirling",
          "pulsating",
          "jittery",
          "squat",
          "spinning",
          "tumbling",
          "shaking",
          "ghostly",
          "blurry",
          "bouncing");

  private static final List<String> POTION_MODS =
      List.of(
          "galvanized",
          "liquefied",
          "magnetized",
          "nitrogenated",
          "oxidized",
          "polarized",
          "polymerized",
          "quantum",
          "tarnished",
          "vacuum-sealed",
          "energized",
          "frozen",
          "diffused",
          "electrified",
          "concentrated",
          "colloidal",
          "activated",
          "aerosolized",
          "anodized",
          "alkaline",
          "ionized",
          "deionized",
          "denatured",
          "pickled",
          "cold-filtered",
          "boiled",
          "modified",
          "altered",
          "corrupted",
          "unsweetened",
          "improved",
          "adjusted",
          "enhanced",
          "moist",
          "dry",
          "chilled",
          "warmed",
          "ionized",
          "Vulcanized",
          "wet",
          "dry",
          "pressed",
          "flattened",
          "irradiated");

  private static final List<String> POTION_PREFIXES =
      List.of("double", "triple", "quadruple", "extra", "non", "super");

  private static final Set<String> ADJECTIVES =
      new HashSet<>(
          List.of(
              "Brimstone",
              "Spooky",
              "aerogel",
              "ancient",
              "antique",
              "bakelite",
              "big",
              "black",
              "blue",
              "brass",
              "candied",
              "cardboard",
              "cheap",
              "cold",
              "creepy",
              "cursed",
              "cute",
              "delicious",
              "dirty",
              "disintegrating",
              "dusty",
              "electric",
              "enchanted",
              "fancy",
              "fishy",
              "flaming",
              "floaty",
              "foam",
              "frigid",
              "frozen",
              "fuchsia",
              "gabardine",
              "giant",
              "glowing",
              "gold",
              "golden",
              "green",
              "haunted",
              "heavy",
              "intricate",
              "large",
              "lavender",
              "leather",
              "little",
              "long",
              "lucky",
              "magical",
              "maroon",
              "metal",
              "miniature",
              "oily",
              "old",
              "orange",
              "oversized",
              "paisley",
              "paraffin",
              "polka-dot",
              "porcelain",
              "portable",
              "powdered",
              "primitive",
              "purple",
              "red",
              "rusty",
              "shiny",
              "silver",
              "sour",
              "solid",
              "spicy",
              "spooky",
              "stained",
              "sticky",
              "stinky",
              "strange",
              "striped",
              "stuffed",
              "tiny",
              "white",
              "wooden",
              "wrought-iron",
              "yellow"));

  public static void getEffectPool() {
    EffectDatabase.entrySet().stream()
        .map(Map.Entry::getKey)
        // Effects must be marked as good
        .filter(id -> EffectDatabase.getQuality(id) == EffectDatabase.GOOD)
        // Effects must be hookah/wish-able
        .filter(id -> !EffectDatabase.hasAttribute(id, "nohookah"))
        // Some effects seem to be unavailable without any obvious reason, and so are tagged thusly
        .filter(id -> !EffectDatabase.hasAttribute(id, "notcrs"))
        // TCRS effects are limited to whatever was available at the time of the path (Tiki
        // Temerity)
        .filter(id -> id <= 2468)
        .forEachOrdered(TCRSEffectPool::add);
  }

  private static String removeAdjectives(final String name) {
    var words = Arrays.asList(name.split(" "));
    return String.join(" ", words.stream().filter(w -> !ADJECTIVES.contains(w)).toList());
  }

  private static String rollCosmetics(final PHPMTRandom mtRng, final PHPRandom rng, final int max) {
    // Determine cosmetic modifiers
    var cosmeticMods = new ArrayList<String>();

    //   Roll 1d6 on whether to add a color
    if (mtRng.nextInt(1, max) == 1) {
      cosmeticMods.add(mtRng.pickOne(COLOR_MODS));
    }

    //   Work out how many cosmetic modifiers to add
    var numCosmeticMods = 0;
    if (mtRng.nextInt(1, max) == 1) numCosmeticMods++;
    if (mtRng.nextInt(1, max) == 1) numCosmeticMods++;
    if (mtRng.nextInt(1, max) == 1) numCosmeticMods++;

    //   Pick and add cosmetic modifiers
    for (var i = 0; i < numCosmeticMods; i++) {
      cosmeticMods.add(mtRng.pickOne(COSMETIC_MODS));
    }

    if (cosmeticMods.size() > 0) {
      rng.shuffle(cosmeticMods);
    }

    Collections.reverse(cosmeticMods);

    return String.join(" ", cosmeticMods);
  }

  static class Enchantment {
    String effect;
    int duration;

    Enchantment(String effect, int duration) {
      this.effect = effect;
      this.duration = duration;
    }

    @Override
    public String toString() {
      if (this.effect.isBlank()) return "";
      return "Effect: \"" + this.effect + "\", Effect Duration: " + this.duration;
    }
  }

  private static ModifierList getRetainedModifiers(final int itemId) {
    var list = ModifierDatabase.getModifierList(new Lookup(ModifierType.ITEM, itemId));
    switch (ItemDatabase.getConsumptionType(itemId)) {
      case EAT, DRINK, SPLEEN, POTION, AVATAR_POTION -> {
        while (list.containsModifier("Effect")) list.removeModifier("Effect");
        while (list.containsModifier("Effect Duration")) list.removeModifier("Effect Duration");
      }
    }

    return list;
  }

  private static Enchantment rollConsumableEnchantment(final int itemId, final PHPMTRandom mtRng) {
    var roll = mtRng.nextInt(0, TCRSEffectPool.size());

    var effectName =
        (roll != TCRSEffectPool.size())
            ? EffectPool.get(TCRSEffectPool.get(roll)).getDisambiguatedName()
            : ModifierDatabase.getStringModifier(
                ModifierType.ITEM, itemId, MultiStringModifier.EFFECT);
    var duration = 5 * mtRng.nextInt(1, 10);

    return new Enchantment(effectName, duration);
  }

  public static TCRS guessPotion(
      final AscensionClass ascensionClass, final ZodiacSign sign, final AdventureResult item) {
    var id = item.getItemId();
    var seed = (50 * id) + (12345 * sign.getId()) + (100000 * ascensionClass.getId());
    var mtRng = new PHPMTRandom(seed);
    var rng = new PHPRandom(seed);

    var cosmeticsString = rollCosmetics(mtRng, rng, 6);

    var mods = getRetainedModifiers(id);

    if (TCRS_GENERIC.contains(id)) {
      mods = ModifierDatabase.getModifierList(new Lookup(ModifierType.ITEM, id));
      var name =
          Stream.of(cosmeticsString, removeAdjectives(ItemDatabase.getItemName(id)))
              .filter(Predicate.not(String::isBlank))
              .collect(Collectors.joining(" "));

      return new TCRS(name, 0, ConsumableQuality.NONE, mods.toString());
    }

    // Determine potion modifiers
    var potionMods = new ArrayList<String>();

    //   Work out how many potion modifiers to add
    var numPotionMods = 1;
    if (mtRng.nextInt(1, 3) == 1) numPotionMods++;
    if (mtRng.nextInt(1, 3) == 1) numPotionMods++;

    //   Pick and add potion modifiers
    for (var i = 0; i < numPotionMods; i++) {
      potionMods.add(mtRng.pickOne(POTION_MODS));
    }

    // Pick effect (note that purposely pick a number that can overflow the pool by 1)
    var roll = mtRng.nextInt(0, TCRSEffectPool.size());

    var effectName =
        (roll == TCRSEffectPool.size())
            ?
            //   If we picked an overflow size, the item retains its original effect
            ModifierDatabase.getStringModifier(
                ModifierType.ITEM, item.getDisambiguatedName(), MultiStringModifier.EFFECT)
            :
            //   Otherwise use the roll we got
            EffectPool.get(TCRSEffectPool.get(roll)).getDisambiguatedName();

    // @TODO what is going on here
    if (item.getItemId() == 3159 && roll == TCRSEffectPool.size()) {
      effectName = "";
    }

    // Pick duration of effect
    var duration = mtRng.nextInt(11, 69);

    // Pick potion mod prefixes
    var prefixedPotionMods = new ArrayList<String>();

    for (var mod : potionMods) {
      var prefixRoll = mtRng.nextInt(1, 40);
      if (prefixRoll <= POTION_PREFIXES.size()) {
        mod = POTION_PREFIXES.get(prefixRoll - 1) + "-" + mod;
      }

      // They get rendered in reverse
      prefixedPotionMods.add(0, mod);
    }

    var potionString = String.join(" ", prefixedPotionMods);

    if (!effectName.isBlank()) {
      mods.addModifier("Effect", effectName);
      mods.addModifier("Effect Duration", String.valueOf(duration));
    }

    var name =
        Stream.of(
                potionString,
                cosmeticsString,
                removeAdjectives(ItemDatabase.getItemName(item.getItemId())))
            .filter(Predicate.not(String::isBlank))
            .collect(Collectors.joining(" "));

    return new TCRS(name, 0, ConsumableQuality.NONE, mods.toString());
  }

  private static ConsumableQuality determineFoodQuality(
      final int qualityRoll, final boolean beverage) {
    return switch (qualityRoll) {
      case 1 -> ConsumableQuality.CRAPPY;
      case 2 -> beverage ? ConsumableQuality.DECENT : ConsumableQuality.CRAPPY;
      case 3 -> ConsumableQuality.DECENT;
      case 4 -> beverage ? ConsumableQuality.GOOD : ConsumableQuality.DECENT;
      case 5 -> ConsumableQuality.GOOD;
      case 6 -> beverage ? ConsumableQuality.AWESOME : ConsumableQuality.GOOD;
      case 7 -> beverage ? ConsumableQuality.EPIC : ConsumableQuality.AWESOME;
      default -> null;
    };
  }

  private static ConsumableQuality determineBoozeQuality(final int qualityRoll) {
    return switch (qualityRoll) {
      case 1, 2 -> ConsumableQuality.DECENT;
      case 3, 4 -> ConsumableQuality.GOOD;
      case 5 -> ConsumableQuality.AWESOME;
      case 6, 7 -> ConsumableQuality.EPIC;
      default -> null;
    };
  }

  private static ConsumableQuality determineSpleenQuality(final int qualityRoll) {
    return switch (qualityRoll) {
      case 1 -> ConsumableQuality.CRAPPY;
      case 2, 3 -> ConsumableQuality.DECENT;
      case 4, 5 -> ConsumableQuality.GOOD;
      case 6 -> ConsumableQuality.AWESOME;
      case 7 -> ConsumableQuality.EPIC;
      default -> null;
    };
  }

  private static final List<List<String>> FOOD_SIZE_DESCRIPTORS =
      List.of(
          List.of("tiny", "bite-sized", "diet", "low-calorie"),
          List.of("small", "snack-sized", "half-sized", "miniature"),
          List.of(),
          List.of(),
          List.of("big", "thick", "super-sized", "jumbo"),
          List.of("massive", "gigantic", "huge", "immense"));

  private static final List<List<String>> BOOZE_SIZE_DESCRIPTORS =
      List.of(
          List.of("practically non-alcoholic"),
          List.of("weak", "watered-down"),
          List.of(),
          List.of(),
          List.of("strong", "spirit-forward", "fortified", "boozy", "distilled", "extra-dry"),
          List.of("irresponsibly strong", "high-proof", "triple-distilled"));

  private static final List<String> FOOD_BOOZE_ENCHANTMENT_DESCRIPTOR =
      List.of("special", "fancy", "enchanted");

  private static final Map<ConsumableQuality, List<String>> FOOD_QUALITY_DESCRIPTORS =
      Map.ofEntries(
          Map.entry(ConsumableQuality.CRAPPY, List.of("rotten", "spoiled", "moldy")),
          Map.entry(ConsumableQuality.DECENT, List.of("bland", "stale", "flavorless")),
          Map.entry(ConsumableQuality.GOOD, List.of("decent", "adequate", "normal")),
          Map.entry(ConsumableQuality.AWESOME, List.of("delicious", "tasty", "toothsome", "yummy")),
          Map.entry(ConsumableQuality.EPIC, List.of("")));

  private static final Map<ConsumableQuality, List<String>> BOOZE_QUALITY_DESCRIPTORS =
      Map.ofEntries(
          Map.entry(ConsumableQuality.CRAPPY, List.of("")),
          Map.entry(ConsumableQuality.DECENT, List.of("bad", "lousy", "mediocre")),
          Map.entry(ConsumableQuality.GOOD, List.of("acceptable", "drinkable", "tolerable")),
          Map.entry(ConsumableQuality.AWESOME, List.of("delicious", "smooth", "aged")),
          Map.entry(
              ConsumableQuality.EPIC, List.of("perfectly mixed", "artisanal", "hand-crafted")));

  private static TCRS guessFoodBooze(
      final AscensionClass ascensionClass,
      final ZodiacSign sign,
      final AdventureResult item,
      final boolean isFood) {
    var id = item.getItemId();
    var seed = (50 * id) + (12345 * sign.getId()) + (100000 * ascensionClass.getId());
    var mtRng = new PHPMTRandom(seed);
    var rng = new PHPRandom(seed);

    var beverage = ConsumablesDatabase.isBeverage(id);

    var cosmeticsString = rollCosmetics(mtRng, rng, beverage ? 8 : 10);

    switch (id) {
      case ItemPool.GUNPOWDER_BURRITO, ItemPool.BEERY_BLOOD -> {
        var name =
            Stream.of(cosmeticsString, removeAdjectives(ItemDatabase.getItemName(id)))
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.joining(" "));

        var mods = getRetainedModifiers(id);

        var size =
            switch (ItemDatabase.getConsumptionType(id)) {
              case EAT -> ConsumablesDatabase.getFullness(id);
              case DRINK -> ConsumablesDatabase.getInebriety(id);
              default -> 0;
            };

        var quality = ConsumablesDatabase.getQuality(id);

        return new TCRS(name, size, quality, mods.toString());
      }
    }

    var qualityRoll = mtRng.nextInt(1, 7);
    var quality =
        isFood ? determineFoodQuality(qualityRoll, beverage) : determineBoozeQuality(qualityRoll);

    // Does it roll the size if a beverage?
    var size =
        beverage
            ? 1
            : switch (mtRng.nextInt(1, 10)) {
              case 1 -> 1;
              case 2, 3 -> 2;
              case 4, 5, 6 -> 3;
              case 7, 8 -> 4;
              case 9 -> 5;
              case 10 -> 5 + mtRng.nextInt(1, 5);
              default -> 0;
            };

    var adjectives = new ArrayList<String>();

    if (!beverage) {
      var sizeDescriptors =
          (isFood ? FOOD_SIZE_DESCRIPTORS : BOOZE_SIZE_DESCRIPTORS).get(Math.min(size - 1, 5));
      if (sizeDescriptors.size() > 0) {
        var sizeDescriptor = mtRng.pickOne(sizeDescriptors);
        adjectives.add(sizeDescriptor);
      }

      var qualityDescriptors =
          (isFood ? FOOD_QUALITY_DESCRIPTORS : BOOZE_QUALITY_DESCRIPTORS).get(quality);
      var qualityDescriptor =
          qualityDescriptors.size() > 1
              ? mtRng.pickOne(qualityDescriptors)
              : qualityDescriptors.get(0);
      adjectives.add(qualityDescriptor);
    }

    if (quality.getValue() * size >= 5) {
      mtRng.nextDouble();
    }

    var mods = getRetainedModifiers(id);

    var enchanted = mtRng.nextInt(1, 10) == 1;
    if (enchanted) {
      adjectives.add(mtRng.pickOne(FOOD_BOOZE_ENCHANTMENT_DESCRIPTOR));
    }

    var enchantment = rollConsumableEnchantment(id, mtRng);

    if (HARDCODED_EFFECT.contains(id)) {
      enchanted = true;
      enchantment.effect =
          ModifierDatabase.getStringModifier(ModifierType.ITEM, id, MultiStringModifier.EFFECT);

      if (!HARDCODED_EFFECT_DYNAMIC_DURATION.contains(id)) {
        enchantment.duration =
            (int)
                ModifierDatabase.getNumericModifier(
                    ModifierType.ITEM, id, DoubleModifier.EFFECT_DURATION);
      }
    }

    if (enchanted && !enchantment.effect.isBlank()) {
      mods.addModifier("Effect", enchantment.effect);
      mods.addModifier("Effect Duration", String.valueOf(enchantment.duration));
    }

    if (id == ItemPool.QUANTUM_TACO
        || id == ItemPool.SCHRODINGERS_THERMOS
        || id == ItemPool.SMORE) {
      size = 0;
    }

    rng.shuffle(adjectives);

    Collections.reverse(adjectives);

    adjectives.add(cosmeticsString);
    adjectives.add(removeAdjectives(ItemDatabase.getItemName(item.getItemId())));

    var name =
        adjectives.stream().filter(Predicate.not(String::isBlank)).collect(Collectors.joining(" "));

    return new TCRS(name, size, quality, mods.toString());
  }

  private static final List<String> SPLEEN_MODIFIERS =
      List.of(
          "boiled",
          "dried",
          "dehydrated",
          "diluted",
          "powdered",
          "mixed",
          "distilled",
          "altered",
          "modified",
          "twisted",
          "vaporized",
          "denatured",
          "compressed",
          "pickled");

  /** Items whose item types are ignored for TCRS */
  private static final Set<Integer> TCRS_GENERIC =
      Set.of(
          // Potions
          ItemPool.JAZZ_SOAP,
          ItemPool.CAN_OF_BINARRRCA,
          // Food
          8462,
          8899);

  /** Dynamically named items aren't renamed by TCRS */
  public static final Set<Integer> DYNAMICALLY_NAMED =
      Set.of(
          ItemPool.EXPERIMENTAL_CRIMBO_FOOD,
          ItemPool.EXPERIMENTAL_CRIMBO_BOOZE,
          ItemPool.EXPERIMENTAL_CRIMBO_SPLEEN,
          ItemPool.LOVE_POTION_XYZ,
          ItemPool.DIABOLIC_PIZZA,
          ItemPool.VAMPIRE_VINTNER_WINE);

  /** Items that keep their Effect despite rolling for a new one */
  private static final Set<Integer> HARDCODED_EFFECT =
      Set.of(
          ItemPool.WREATH_CRIMBO_COOKIE,
          ItemPool.BELL_CRIMBO_COOKIE,
          ItemPool.TREE_CRIMBO_COOKIE,
          ItemPool.JAZZ_SOAP,
          ItemPool.BAT_CRIMBOWEEN_COOKIE,
          ItemPool.SKULL_CRIMBOWEEN_COOKIE,
          ItemPool.TOMBSTONE_CRIMBOWEEN_COOKIE,
          ItemPool.TURTLE_SOUP,
          ItemPool.BEEFY_FISH_MEAT,
          ItemPool.GLISTENING_FISH_MEAT,
          ItemPool.SLICK_FISH_MEAT,
          ItemPool.BLOB_CRIMBCOOKIE,
          ItemPool.QUEEN_COOKIE,
          ItemPool.SUN_DRIED_TOFU,
          ItemPool.SOYBURGER_JUICE,
          ItemPool.CIRCULAR_CRIMBCOOKIE,
          ItemPool.TRIANGULAR_CRIMBCOOKIE,
          ItemPool.SQUARE_CRIMBCOOKIE,
          ItemPool.CHAOS_POPCORN,
          ItemPool.TEMPS_TEMPRANILLO,
          ItemPool.THYME_JELLY_DONUT);

  /** Items that keep their Effect but take on a new Effect Duration */
  private static final Set<Integer> HARDCODED_EFFECT_DYNAMIC_DURATION =
      Set.of(ItemPool.QUEEN_COOKIE, ItemPool.TURTLE_SOUP);

  private static TCRS guessSpleen(
      final AscensionClass ascensionClass, final ZodiacSign sign, final AdventureResult item) {
    var id = item.getItemId();
    var seed = (50 * id) + (12345 * sign.getId()) + (100000 * ascensionClass.getId());
    var mtRng = new PHPMTRandom(seed);
    var rng = new PHPRandom(seed);

    var cosmeticsString = rollCosmetics(mtRng, rng, 4);

    var quality = determineSpleenQuality(mtRng.nextInt(1, 7));

    var adjective = mtRng.pickOne(SPLEEN_MODIFIERS);

    // Some unknown machinations here, only CDM can explain
    {
      if (quality == ConsumableQuality.CRAPPY) {
        if (mtRng.nextInt(1, 6) == 6) {
          mtRng.nextDouble();
        }
      } else {
        mtRng.nextDouble();
        mtRng.nextDouble();
      }

      mtRng.nextDouble();
    }

    var mods = getRetainedModifiers(id);

    if ((mtRng.nextInt(1, 3) == 1)) {
      var enchantment = rollConsumableEnchantment(id, mtRng);
      if (!enchantment.effect.isBlank()) {
        mods.addModifier("Effect", enchantment.effect);
        mods.addModifier("Effect Duration", String.valueOf(enchantment.duration));
      }
    }

    var name =
        Stream.of(adjective, cosmeticsString, removeAdjectives(ItemDatabase.getItemName(id)))
            .filter(Predicate.not(String::isBlank))
            .collect(Collectors.joining(" "));

    return new TCRS(name, 1, quality, mods.toString());
  }

  static protected List<Entry<String, String>> EQUIPMENT_MODIFIERS = List.<Entry<String, String>>of(
    Map.entry("Annie Oakley's", "Critical Hit Percent: +20"),
    Map.entry("arcane researcher's", "Experience Percent (Mysticality): +10"),
    Map.entry("aromatic", "Stench Resistance: +1"),
    Map.entry("asbestos-lined", "Hot Resistance: +5"),
    Map.entry("auspicious", "Item Drop: +10"),
    Map.entry("avaricious", "Meat Drop: +30"),
    Map.entry("baleful", "Spell Damage: +25"),
    Map.entry("banded", "Damage Absorption: +100"),
    Map.entry("beefcake's", "Muscle: +25"),
    Map.entry("beefy", "Muscle: +10"),
    Map.entry("boxer's", "Muscle Percent: +10"),
    Map.entry("brainy", "Mysticality: +5"),
    Map.entry("brawny", "Muscle Percent: +20"),
    Map.entry("careful", "Monster Level: -10"),
    Map.entry("censurious", "Sleaze Resistance: +3"),
    Map.entry("chaotic", "Spell Critical Percent: +10"),
    Map.entry("chilly", "Cold Damage: +5"),
    Map.entry("clever", "Maximum MP: +20"),
    Map.entry("cool", "Moxie: +5"),
    Map.entry("coward's", "Monster Level: -20"),
    Map.entry("crafty", "Maximum MP: +10"),
    Map.entry("curative", "HP Regen Min: 3, HP Regen Max: 5"),
    Map.entry("Da Vinci's", "Experience (Mysticality): +5"),
    Map.entry("dance instructor's", "Experience Percent (Moxie): +10"),
    Map.entry("dangerous", "Weapon Damage: +10"),
    Map.entry("deadly", "Weapon Damage: +5"),
    Map.entry("dog trainer's", "Experience (familiar): +1"),
    Map.entry("double-paned", "Cold Resistance: +5"),
    Map.entry("educational", "Experience: +1"),
    Map.entry("electrified", "MP Regen Min: 3, MP Regen Max: 5"),
    Map.entry("energetic", "Maximum MP Percent: +20"),
    Map.entry("executive", "Meat Drop: +40"),
    Map.entry("experienced", "Experience: +2"),
    Map.entry("extremely unsafe", "Weapon Damage Percent: +100"),
    Map.entry("family-friendly", "Sleaze Resistance: +1"),
    Map.entry("fireproof", "Hot Resistance: +3"),
    Map.entry("flame-retardant", "Hot Resistance: +1"),
    Map.entry("flame-wreathed", "Hot Spell Damage: +10"),
    Map.entry("Fonzie's", "Experience (Moxie): +1"),
    Map.entry("forbidden", "Spell Damage Percent: +50"),
    Map.entry("fortified", "Damage Absorption: +60"),
    Map.entry("foul-smelling", "Stench Damage: +10"),
    Map.entry("friendly", "Familiar Weight: +3"),
    Map.entry("frightening", "Spooky Damage: +5"),
    Map.entry("frosty", "Cold Spell Damage: +10"),
    Map.entry("gravedigger's", "Spooky Damage: +10"),
    Map.entry("greasy", "Sleaze Spell Damage: +10"),
    Map.entry("greedy", "Meat Drop: +20"),
    Map.entry("grievous", "Weapon Damage Percent: +50"),
    Map.entry("groovy", "Moxie Percent: +10"),
    Map.entry("hale", "Maximum HP: +20"),
    Map.entry("hardened", "Damage Reduction: 3"),
    Map.entry("hardy", "Maximum HP: +50"),
    Map.entry("healthy", "Maximum HP Percent: +20"),
    Map.entry("Herculean", "Experience (Muscle): +1"),
    Map.entry("horrifying", "Spooky Damage: +25"),
    Map.entry("inspector's", "Item Drop: +15"),
    Map.entry("Jack Frost's", "Cold Spell Damage: +50"),
    Map.entry("Jeselnik's", "Sleaze Damage: +25"),
    Map.entry("Jim Carey's", "Monster Level: +25"),
    Map.entry("knitted", "Cold Resistance: +3"),
    Map.entry("lard-coated", "Sleaze Spell Damage: +25"),
    Map.entry("lightning-fast", "Initiative: +40"),
    Map.entry("lion tamer's", "Experience (familiar): +2"),
    Map.entry("Lo Pan's", "Spell Critical Percent: +30"),
    Map.entry("MacGyver's", "Maximum MP: +100"),
    Map.entry("mansplainer's", "Monster Level: +15"),
    Map.entry("manspreader's", "Monster Level: +10"),
    Map.entry("medical-grade", "HP Regen Min: 7, HP Regen Max: 10"),
    Map.entry("miser's", "Meat Drop: +10"),
    Map.entry("nasty", "Stench Damage: +5"),
    Map.entry("Newton's", "Experience (Mysticality): +3"),
    Map.entry("nippy", "Cold Damage: +10"),
    Map.entry("occult", "Spell Damage Percent: +25"),
    Map.entry("of bravery", "Spooky Resistance: +5"),
    Map.entry("of Calamity Jane", "Critical Hit Percent: +15"),
    Map.entry("of chilblains", "Cold Damage: +25"),
    Map.entry("of courage", "Spooky Resistance: +3"),
    Map.entry("of dire peril", "Weapon Damage: +20"),
    Map.entry("of doom", "Spooky Spell Damage: +50"),
    Map.entry("of extreme caution", "Monster Level: -25"),
    Map.entry("of Flo-Jo", "Initiative: +100"),
    Map.entry("of Gandalf", "Spell Critical Percent: +20"),
    Map.entry("of horror", "Spooky Spell Damage: +25"),
    Map.entry("of incineration", "Hot Spell Damage: +50"),
    Map.entry("of James Dean", "Experience (Moxie): +3"),
    Map.entry("of Leguizamo", "Monster Level: +20"),
    Map.entry("of mayonnaise", "Sleaze Spell Damage: +50"),
    Map.entry("of Tarzan", "Experience (Muscle): +3"),
    Map.entry("of temperance", "Sleaze Resistance: +5"),
    Map.entry("of terror", "Spooky Spell Damage: +10"),
    Map.entry("of the blizzard", "Cold Spell Damage: +25"),
    Map.entry("of the bloodbag", "Maximum HP: +100"),
    Map.entry("of the boozehound", "Booze Drop: +100"),
    Map.entry("of the brazier", "Hot Spell Damage: +25"),
    Map.entry("of the brute", "Muscle Percent: +30"),
    Map.entry("of the businessman", "Meat Drop: +50"),
    Map.entry("of the cheetah", "Initiative: +60"),
    Map.entry("of the cougar", "Moxie: +20"),
    Map.entry("of the dark arts", "Spell Damage Percent: +100"),
    Map.entry("of the detective", "Item Drop: +20"),
    Map.entry("of the early riser", "Adventures: +7"),
    Map.entry("of the empath", "Familiar Weight: +5"),
    Map.entry("of the glutton", "Food Drop: +100"),
    Map.entry("of the overflowing toilet", "Stench Spell Damage: +50"),
    Map.entry("of the ox", "Muscle: +20"),
    Map.entry("of the pedagogue", "Experience: +3"),
    Map.entry("of the scaredy-cat", "Monster Level: -15"),
    Map.entry("of the sewer", "Stench Damage: +25"),
    Map.entry("of the storm", "Maximum MP Percent: +40"),
    Map.entry("of the sweet-tooth", "Candy Drop: +100"),
    Map.entry("of the wise owl", "Mysticality: +20"),
    Map.entry("of vim and vigor", "Maximum HP Percent: +50"),
    Map.entry("of wisdom", "Mysticality: +15"),
    Map.entry("Oprah's", "Maximum MP Percent: +50"),
    Map.entry("padded", "Damage Absorption: +20"),
    Map.entry("perfumed", "Stench Resistance: +5"),
    Map.entry("personal trainer's", "Experience Percent (Muscle): +10"),
    Map.entry("prompt", "Adventures: +3"),
    Map.entry("quilted", "Damage Absorption: +40"),
    Map.entry("razor-sharp", "Weapon Damage Percent: +25"),
    Map.entry("reassuring", "Spooky Resistance: +1"),
    Map.entry("resourceful", "Maximum MP: +50"),
    Map.entry("ribald", "Sleaze Damage: +10"),
    Map.entry("rock-hard", "Damage Reduction: 5"),
    Map.entry("rosewater-soaked", "Stench Resistance: +3"),
    Map.entry("Rosewater's", "Mysticality Percent: +30"),
    Map.entry("rosy-cheeked", "Maximum HP Percent: +30"),
    Map.entry("ruddy", "Maximum HP Percent: +40"),
    Map.entry("sage", "Mysticality Percent: +10"),
    Map.entry("Samson's", "Experience (Muscle): +5"),
    Map.entry("savvy", "Mysticality Percent: +20"),
    Map.entry("scandalous", "Sleaze Damage: +5"),
    Map.entry("scorching", "Hot Damage: +25"),
    Map.entry("sharpshooter's", "Critical Hit Percent: +10"),
    Map.entry("shellacked", "Damage Reduction: 7"),
    Map.entry("Sherlock's", "Item Drop: +25"),
    Map.entry("Sinatra's", "Experience (Moxie): +5"),
    Map.entry("sinister", "Spell Damage: +10"),
    Map.entry("sizzling", "Hot Damage: +10"),
    Map.entry("slick", "Moxie: +10"),
    Map.entry("smartaleck's", "Moxie Percent: +30"),
    Map.entry("smelly", "Stench Spell Damage: +10"),
    Map.entry("smooth", "Moxie: +15"),
    Map.entry("Socratic", "Experience (Mysticality): +1"),
    Map.entry("stanky", "Stench Spell Damage: +25"),
    Map.entry("steel-toed", "Damage Reduction: 9"),
    Map.entry("stiffened", "Damage Reduction: 1"),
    Map.entry("strapping", "Muscle: +5"),
    Map.entry("studded", "Damage Absorption: +80"),
    Map.entry("stylish", "Moxie: +25"),
    Map.entry("supercharged", "Maximum MP Percent: +30"),
    Map.entry("supercool", "Moxie Percent: +20"),
    Map.entry("Temple Grandin's", "Familiar Weight: +7"),
    Map.entry("Tesla", "MP Regen Min: 7, MP Regen Max: 10"),
    Map.entry("therapeutic", "HP Regen Min: 5, HP Regen Max: 7"),
    Map.entry("thinker's", "Mysticality: +10"),
    Map.entry("toasty", "Hot Damage: +5"),
    Map.entry("up-at-dawn", "Adventures: +5"),
    Map.entry("Usain Bolt's", "Initiative: +80"),
    Map.entry("Van der Graaf", "MP Regen Min: 5, MP Regen Max: 7"),
    Map.entry("veiny", "Maximum HP: +10"),
    Map.entry("vibrating", "Maximum MP Percent: +10"),
    Map.entry("weightlifter's", "Muscle: +15"),
    Map.entry("wholesome", "Maximum HP Percent: +10"),
    Map.entry("wicked", "Spell Damage: +5"),
    Map.entry("wizardly", "Mysticality: +25"),
    Map.entry("wool", "Cold Resistance: +1"),
    Map.entry("zippy", "Initiative: +20")
  );

  private static TCRS guessEquipment(
      final AscensionClass ascensionClass, final ZodiacSign sign, final AdventureResult item) {
    var id = item.getItemId();
    var seed = (50 * id) + (12345 * sign.getId()) + (100000 * ascensionClass.getId());
    var mtRng = new PHPMTRandom(seed);
    var rng = new PHPRandom(seed);

    var cosmeticsString = rollCosmetics(mtRng, rng, 8);

    var name =
        new ArrayList<>(Stream.of(cosmeticsString, removeAdjectives(ItemDatabase.getItemName(id)))
            .filter(Predicate.not(String::isBlank)).toList());

    var originalMods = ModifierDatabase.getModifierList(new Lookup(ModifierType.ITEM, id));
    var mods = getRetainedModifiers(id);

    var shuffledMods = new ArrayList<>(EQUIPMENT_MODIFIERS);
    rng.shuffle(shuffledMods);
    var equipmentMods = rng.arrayPick(shuffledMods, originalMods.size());
    for (var entry : equipmentMods) {
      var descriptor = entry.getKey();
      if (descriptor.startsWith("of ")) {
        name.addLast(descriptor);
      } else {
        name.addFirst(descriptor);
      }
      DebugDatabase.appendModifier(mods, entry.getValue());
    }

    return new TCRS(String.join(" ", name), 0, ConsumableQuality.NONE, mods.toString());
  }

  private static TCRS guessGeneric(
      final AscensionClass ascensionClass, final ZodiacSign sign, final AdventureResult item) {
    var id = item.getItemId();
    var seed = (50 * id) + (12345 * sign.getId()) + (100000 * ascensionClass.getId());
    var mtRng = new PHPMTRandom(seed);
    var rng = new PHPRandom(seed);

    var cosmeticsString = rollCosmetics(mtRng, rng, 8);

    var name =
        Stream.of(cosmeticsString, removeAdjectives(ItemDatabase.getItemName(id)))
            .filter(Predicate.not(String::isBlank))
            .collect(Collectors.joining(" "));

    var mods = getRetainedModifiers(id);

    var size =
        switch (ItemDatabase.getConsumptionType(id)) {
          case EAT -> ConsumablesDatabase.getFullness(id);
          case DRINK -> ConsumablesDatabase.getInebriety(id);
          case SPLEEN -> ConsumablesDatabase.getSpleenHit(id);
          default -> 0;
        };

    var quality = ConsumablesDatabase.getQuality(id);

    return new TCRS(name, size, quality, mods.toString());
  }

  public static TCRS guessItem(
      final AscensionClass ascensionClass, final ZodiacSign sign, final int itemId) {
    var item = ItemPool.get(itemId);
    var type = ItemDatabase.getConsumptionType(itemId);

    if (DYNAMICALLY_NAMED.contains(itemId)) {
      var name = ItemDatabase.getItemName(itemId);

      var size =
          switch (type) {
            case EAT -> ConsumablesDatabase.getFullness(name);
            case DRINK -> ConsumablesDatabase.getInebriety(name);
            case SPLEEN -> ConsumablesDatabase.getSpleenHit(name);
            default -> 0;
          };

      return new TCRS(name, size, ConsumablesDatabase.getQuality(name), "");
    }

    switch (itemId) {
      case
      // Glitch item isn't really a food
      ItemPool.GLITCH_ITEM -> type = ConsumptionType.NONE;
    }

    return switch (type) {
      case POTION, AVATAR_POTION -> guessPotion(ascensionClass, sign, item);
      case EAT, DRINK -> guessFoodBooze(ascensionClass, sign, item, type == ConsumptionType.EAT);
      case SPLEEN -> guessSpleen(ascensionClass, sign, item);
      case HAT,
          SHIRT,
          CONTAINER,
          WEAPON,
          OFFHAND,
          PANTS,
          ACCESSORY,
          FAMILIAR_EQUIPMENT -> guessEquipment(ascensionClass, sign, item);
      default -> guessGeneric(ascensionClass, sign, item);
    };
  }

  private static boolean deriveCafe(final boolean verbose) {
    if (verbose) {
      KoLmafia.updateDisplay("Deriving TCRS item adjustments for all cafe booze items...");
    }

    for (Integer id : CafeDatabase.cafeBoozeKeySet()) {
      deriveCafe(id, CafeDatabase.boozeDescId(id), TCRSBoozeMap);
    }

    if (verbose) {
      KoLmafia.updateDisplay("Done!");
    }

    if (verbose) {
      KoLmafia.updateDisplay("Deriving TCRS item adjustments for all cafe food items...");
    }

    for (Integer id : CafeDatabase.cafeFoodKeySet()) {
      deriveCafe(id, CafeDatabase.foodDescId(id), TCRSFoodMap);
    }

    if (verbose) {
      KoLmafia.updateDisplay("Done!");
    }

    return true;
  }

  private static boolean deriveCafe(final int itemId, String descId, Map<Integer, TCRS> map) {
    // Don't do this if we already know the item
    if (map.containsKey(itemId)) {
      return false;
    }

    String text = DebugDatabase.cafeItemDescriptionText(descId);

    TCRS tcrs = deriveItem(itemId, text);

    map.put(itemId, tcrs);

    return true;
  }

  public static boolean applyModifiers() {
    // Remove food/booze/spleen/potion sources for effects
    StringBuilder buffer = new StringBuilder();
    for (Integer id : EffectDatabase.keys()) {
      String actions = EffectDatabase.getActions(id);
      if (actions == null || actions.startsWith("#")) {
        continue;
      }
      if (actions.contains("eat ")
          || actions.contains("drink ")
          || actions.contains("chew ")
          || actions.contains("use ")) {
        String[] split = actions.split(" *\\| *");
        buffer.setLength(0);
        for (String action : split) {
          if (action.isEmpty()
              || action.startsWith("eat ")
              || action.startsWith("drink ")
              || action.startsWith("chew ")
              || action.startsWith("use ")) {
            continue;
          }
          if (!buffer.isEmpty()) {
            buffer.append("|");
          }
          buffer.append(action);
        }
        EffectDatabase.setActions(id, buffer.isEmpty() ? null : buffer.toString());
      }
    }

    // Adjust non-cafe item data to have TCRS modifiers
    for (Entry<Integer, TCRS> entry : TCRSMap.entrySet()) {
      Integer id = entry.getKey();
      TCRS tcrs = entry.getValue();
      applyModifiers(id, tcrs);
    }

    // Do the same for cafe consumables
    for (Entry<Integer, TCRS> entry : TCRSBoozeMap.entrySet()) {
      Integer id = entry.getKey();
      TCRS tcrs = entry.getValue();
      String name = CafeDatabase.getCafeBoozeName(id);
      applyConsumableModifiers(ConsumptionType.DRINK, name, tcrs);
    }

    for (Entry<Integer, TCRS> entry : TCRSFoodMap.entrySet()) {
      Integer id = entry.getKey();
      TCRS tcrs = entry.getValue();
      String name = CafeDatabase.getCafeFoodName(id);
      applyConsumableModifiers(ConsumptionType.EAT, name, tcrs);
    }

    // Fix all the consumables whose adv yield varies by level
    ConsumablesDatabase.setLevelVariableConsumables();

    ConcoctionDatabase.refreshConcoctions();
    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
    return true;
  }

  public static boolean applyModifiers(int itemId) {
    return applyModifiers(itemId, TCRSMap.get(itemId));
  }

  private static int qualityMultiplier(ConsumableQuality quality) {
    return switch (quality) {
      case EPIC -> 5;
      case AWESOME -> 4;
      case GOOD -> 3;
      case DECENT -> 2;
      case CRAPPY -> 1;
      default -> 0;
    };
  }

  public static boolean applyModifiers(final Integer itemId, final TCRS tcrs) {
    // Adjust item data to have TCRS modifiers
    if (tcrs == null) {
      return false;
    }

    if (ItemDatabase.isFamiliarEquipment(itemId)) {
      return false;
    }

    if (CampgroundRequest.campgroundItems.contains(itemId)) {
      return false;
    }

    if (ChateauRequest.chateauItems.contains(itemId)) {
      return false;
    }

    String itemName = ItemDatabase.getItemDataName(itemId);
    if (itemName == null) {
      return false;
    }

    // Set modifiers
    ModifierDatabase.updateItem(itemId, tcrs.modifiers);

    // *** Do this after modifiers are set so can log effect modifiers
    ConsumptionType usage = ItemDatabase.getConsumptionType(itemId);
    if (usage == ConsumptionType.EAT
        || usage == ConsumptionType.DRINK
        || usage == ConsumptionType.SPLEEN) {
      applyConsumableModifiers(usage, itemName, tcrs);
    }

    // Add as effect source, if appropriate
    String effectName =
        ModifierDatabase.getStringModifier(ModifierType.ITEM, itemName, MultiStringModifier.EFFECT);
    if (effectName != null && !effectName.isEmpty()) {
      addEffectSource(itemName, usage, effectName);
    }

    // Whether or not there is an effect name, reset the concoction
    setEffectName(itemId, itemName);

    return true;
  }

  public static void setEffectName(final Integer itemId, String name) {
    Concoction c = ConcoctionPool.get(itemId, name);
    if (c != null) {
      c.setEffectName();
    }
  }

  private static void addEffectSource(
      final String itemName, final ConsumptionType usage, final String effectName) {
    int effectId = EffectDatabase.getEffectId(effectName);
    if (effectId == -1) {
      return;
    }
    String verb =
        switch (usage) {
          case EAT -> "eat ";
          case DRINK -> "drink ";
          case SPLEEN -> "chew ";
          default -> "use ";
        };
    String actions = EffectDatabase.getActions(effectId);
    boolean added = false;
    StringBuilder buffer = new StringBuilder();
    if (actions != null) {
      String either = verb + "either ";
      String[] split = actions.split(" *\\| *");
      for (String action : split) {
        if (action.isEmpty()) {
          continue;
        }
        if (!buffer.isEmpty()) {
          buffer.append("|");
        }
        if (added) {
          buffer.append(action);
          continue;
        }
        if (action.startsWith(either)) {
          buffer.append(action);
          buffer.append(", 1 ");
        } else if (action.startsWith(verb)) {
          buffer.append(StringUtilities.singleStringReplace(action, verb, either));
          buffer.append(", 1 ");
        } else {
          buffer.append(action);
          continue;
        }
        buffer.append(itemName);
        added = true;
      }
    }

    if (!added) {
      if (!buffer.isEmpty()) {
        buffer.append("|");
      }
      buffer.append(verb);
      buffer.append("1 ");
      buffer.append(itemName);
    }
    EffectDatabase.setActions(effectId, buffer.toString());
  }

  private static void applyConsumableModifiers(
      final ConsumptionType usage, final String itemName, final TCRS tcrs) {
    var consumable = ConsumablesDatabase.getConsumableByName(itemName);
    Integer lint = ConsumablesDatabase.getLevelReq(consumable);
    int level = lint == null ? 0 : lint;
    // Guess
    int adv = (usage == ConsumptionType.SPLEEN) ? 0 : (tcrs.size * qualityMultiplier(tcrs.quality));
    int mus = 0;
    int mys = 0;
    int mox = 0;

    var comment = new StringJoiner(", ").add("Unspaded");

    // Consumable attributes (like SAUCY, BEER, etc) are preserved
    ConsumablesDatabase.getAttributes(consumable).stream().map(Enum::name).forEach(comment::add);

    String effectName =
        ModifierDatabase.getStringModifier(ModifierType.ITEM, itemName, MultiStringModifier.EFFECT);
    if (effectName != null && !effectName.isEmpty()) {
      int duration =
          (int)
              ModifierDatabase.getNumericModifier(
                  ModifierType.ITEM, itemName, DoubleModifier.EFFECT_DURATION);
      String effectModifiers =
          ModifierDatabase.getStringModifier(
              ModifierType.EFFECT, effectName, StringModifier.MODIFIERS);
      comment.add(duration + " " + effectName + " (" + effectModifiers + ")");
    }

    ConsumablesDatabase.updateConsumable(
        itemName,
        tcrs.size,
        level,
        tcrs.quality,
        String.valueOf(adv),
        String.valueOf(mus),
        String.valueOf(mys),
        String.valueOf(mox),
        comment.toString());
  }

  public static void resetModifiers() {
    // Reset all the data structures that we altered in-place to
    // supper a particular TCRS class/sign to standard KoL values.

    // Nothing to reset if we didn't load TCRS data
    if (currentClassSign.isEmpty()) {
      return;
    }

    TCRSDatabase.reset();

    ModifierDatabase.resetModifiers();
    EffectDatabase.reset();
    ConsumablesDatabase.reset();

    // Check items that vary per person
    InventoryManager.checkMods();

    deriveApplyItem(ItemPool.RING);

    ConcoctionDatabase.resetEffects();
    ConcoctionDatabase.refreshConcoctions();
    ConsumablesDatabase.setVariableConsumables();
    ConsumablesDatabase.calculateAllAverageAdventures();

    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  // *** support for fetching TCRS files from KoLmafia's SVN repository

  // Remote files we have fetched this session
  private static final Set<String> remoteFetched =
      new HashSet<>(); // remote files fetched this session

  // *** support for loading up TCRS data appropriate to your current class/sign

  public static boolean loadTCRSData() {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return false;
    }

    return loadTCRSData(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), true);
  }

  private static boolean loadTCRSData(
      final AscensionClass ascensionClass, final ZodiacSign sign, final boolean verbose) {
    var nonCafeLoaded = load(ascensionClass, sign, verbose);
    var cafeLoaded = loadCafe(ascensionClass, sign, verbose);

    if (nonCafeLoaded || cafeLoaded) {
      applyModifiers();
      deriveApplyItem(ItemPool.RING);
    }

    return true;
  }
}
