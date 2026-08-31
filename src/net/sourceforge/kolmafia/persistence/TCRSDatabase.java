package net.sourceforge.kolmafia.persistence;

import static net.sourceforge.kolmafia.persistence.ModifierDatabase.CARRIED_OVER;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.Lookup;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.modifiers.ModifierList.ModifierValue;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.Concoction;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.ConsumablesDatabase.ConsumableQuality;
import net.sourceforge.kolmafia.persistence.EffectData.Quality;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.ChateauRequest;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.PHPMTRandom;
import net.sourceforge.kolmafia.utilities.PHPRandom;
import net.sourceforge.kolmafia.utilities.PHPRandomSelection;
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

  private record TCRSIntrospectRunnable(int itemId) implements Runnable {
    @Override
    public void run() {
      String text = DebugDatabase.itemDescriptionText(itemId, false);
      if (text == null) {
        return;
      }

      TCRS tcrs = introspectItem(itemId, text);

      TCRSMap.put(itemId, tcrs);
    }
  }

  private static String currentClassSign; // Character class/Zodiac Sign

  // Sorted by itemId
  private static final Map<Integer, TCRS> TCRSMap = new ConcurrentSkipListMap<>();
  private static final Map<Integer, TCRS> TCRSBoozeMap =
      new ConcurrentSkipListMap<>(new CafeDatabase.InverseIntegerOrder());
  private static final Map<Integer, TCRS> TCRSFoodMap =
      new ConcurrentSkipListMap<>(new CafeDatabase.InverseIntegerOrder());

  private static final List<Integer> TCRSEffectPool = new ArrayList<>();

  static {
    TCRSDatabase.reset();
  }

  public static void reset() {
    currentClassSign = "";
    TCRSMap.clear();
    TCRSBoozeMap.clear();
    TCRSFoodMap.clear();
    buildEffectPool();
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

  public static boolean save(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return false;
    }
    boolean retval = true;
    retval &= save(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
    retval &= saveCafe(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
    return retval;
  }

  public static boolean save(
      AscensionClass ascensionClass, ZodiacSign csign, final boolean verbose) {
    return save(filename(ascensionClass, csign, ""), TCRSMap, verbose);
  }

  public static boolean saveCafe(
      AscensionClass ascensionClass, ZodiacSign csign, final boolean verbose) {
    boolean retval = true;
    retval &= save(filename(ascensionClass, csign, "_cafe_booze"), TCRSBoozeMap, verbose);
    retval &= save(filename(ascensionClass, csign, "_cafe_food"), TCRSFoodMap, verbose);
    return retval;
  }

  public static boolean saveCafeBooze(
      AscensionClass ascensionClass, ZodiacSign csign, final boolean verbose) {
    return save(filename(ascensionClass, csign, "_cafe_booze"), TCRSBoozeMap, verbose);
  }

  public static boolean saveCafeFood(
      AscensionClass ascensionClass, ZodiacSign csign, final boolean verbose) {
    return save(filename(ascensionClass, csign, "_cafe_food"), TCRSFoodMap, verbose);
  }

  private static boolean save(
      final String fileName, final Map<Integer, TCRS> map, final boolean verbose) {
    if (fileName == null) {
      return false;
    }

    PrintStream writer = LogStream.openStream(new File(KoLConstants.DATA_LOCATION, fileName), true);

    // No writer, no file
    if (writer == null) {
      if (verbose) {
        RequestLogger.printLine("Could not write file " + fileName);
      }
      return false;
    }

    try (writer) {
      for (Entry<Integer, TCRS> entry : map.entrySet()) {
        TCRS tcrs = entry.getValue();
        Integer itemId = entry.getKey();
        String name = tcrs.name;
        Integer size = tcrs.size;
        var quality = tcrs.quality;
        String modifiers = tcrs.modifiers;
        String line = itemId + "\t" + name + "\t" + size + "\t" + quality + "\t" + modifiers;
        writer.println(line);
      }
    }

    if (verbose) {
      RequestLogger.printLine("Wrote file " + fileName);
    }

    return true;
  }

  public static boolean introspect(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return false;
    }

    introspect(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
    introspectCafe(verbose);
    return true;
  }

  private static boolean introspect(
      final AscensionClass ascensionClass, final ZodiacSign sign, final boolean verbose) {
    // If we don't currently have data for this class/sign, start fresh
    String classSign = ascensionClass.getName() + "/" + sign;
    if (!currentClassSign.equals(classSign)) {
      reset();
    }

    Set<Integer> keys = ItemDatabase.descriptionIdKeySet();

    if (verbose) {
      KoLmafia.updateDisplay("Introspecting TCRS item adjustments for all real items...");
    }

    List<Runnable> actions = new ArrayList<>();

    for (Integer id : keys) {
      actions.add(new TCRSIntrospectRunnable(id));
    }

    RequestThread.runInParallel(actions, verbose);

    currentClassSign = classSign;

    if (verbose) {
      KoLmafia.updateDisplay("Done!");
    }

    return true;
  }

  public static boolean introspect(final int itemId) {
    // Don't do this if we already know the item
    if (TCRSMap.containsKey(itemId)) {
      return false;
    }

    TCRS tcrs = introspectItem(itemId);
    if (tcrs == null) {
      return false;
    }

    TCRSMap.put(itemId, tcrs);

    return true;
  }

  public static boolean derive(final int itemId) {
    if (TCRSMap.containsKey(itemId)) {
      return false;
    }

    TCRS tcrs = deriveItem(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), itemId);
    if (tcrs == null) {
      return false;
    }

    TCRSMap.put(itemId, tcrs);

    return true;
  }

  public static TCRS deriveAndSaveItem(final int itemId) {
    TCRS tcrs = deriveItem(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), itemId);
    if (tcrs != null) {
      TCRSMap.put(itemId, tcrs);
    }
    return tcrs;
  }

  public static boolean derive(final boolean verbose) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return false;
    }

    return derive(KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), verbose);
  }

  /** Derives the TCRS data for every real item and cafe consumable. */
  public static boolean derive(
      final AscensionClass ascensionClass, final ZodiacSign sign, final boolean verbose) {
    String classSign = ascensionClass.getName() + "/" + sign;
    if (!currentClassSign.equals(classSign)) {
      reset();
    }

    if (verbose) {
      KoLmafia.updateDisplay("Deriving TCRS item adjustments for all real items...");
    }

    for (Integer id : ItemDatabase.descriptionIdKeySet()) {
      TCRS tcrs = deriveItem(ascensionClass, sign, id);
      if (tcrs != null) {
        TCRSMap.put(id, tcrs);
      }
    }

    for (Integer id : CafeDatabase.cafeBoozeKeySet()) {
      TCRSBoozeMap.put(id, deriveCafe(ascensionClass, sign, id, false));
    }

    for (Integer id : CafeDatabase.cafeFoodKeySet()) {
      TCRSFoodMap.put(id, deriveCafe(ascensionClass, sign, id, true));
    }

    currentClassSign = classSign;

    if (verbose) {
      KoLmafia.updateDisplay("Done!");
    }

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
      // that, force introspect here to get the real modifiers.
      if (id == ItemPool.HEWN_MOON_RUNE_SPOON) {
        TCRS tcrs = TCRSMap.get(id);
        if (tcrs != null && "hewn moon-rune spoon".equals(tcrs.name)) {
          TCRSMap.remove(id);
        }
      }

      if (introspect(id)) {
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
      if (introspectCafe(id, CafeDatabase.boozeDescId(id), TCRSBoozeMap)) {
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
      if (introspectCafe(id, CafeDatabase.foodDescId(id), TCRSFoodMap)) {
        count++;
      }
    }

    if (verbose) {
      KoLmafia.updateDisplay(count + " new cafe foods seen");
    }

    return count;
  }

  public static TCRS introspectItem(final int itemId) {
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

    return introspectItem(itemId, text);
  }

  public static TCRS introspectAndSaveItem(final int itemId) {
    TCRS tcrs = introspectItem(itemId);
    if (tcrs != null) {
      TCRSMap.put(itemId, tcrs);
    }
    return tcrs;
  }

  public static TCRS introspectRing() {
    String text = DebugDatabase.itemDescriptionText(ItemPool.RING, false);
    return introspectItem(ItemPool.RING, text);
  }

  public static TCRS introspectSpoon() {
    String text = DebugDatabase.itemDescriptionText(ItemPool.HEWN_MOON_RUNE_SPOON, false);
    return introspectItem(ItemPool.HEWN_MOON_RUNE_SPOON, text);
  }

  public static void introspectApplyItem(final int id) {
    String text = DebugDatabase.itemDescriptionText(id, false);

    // should only be null in tests, but setting up the builder is hard
    if (text != null) {
      applyModifiers(id, introspectItem(id, text));
    }
  }

  /**
   * The modifier a modifier-string entry denotes. Entries are written as tags, which match a
   * modifier by pattern rather than by name: "Look like a Pirate" is the Pirate modifier, and "Item
   * Drop (sporadic)" is Sporadic Item Drop. Comparing text against getName() misses both.
   */
  private static Modifier resolveModifier(final ModifierValue value) {
    var tag = value.toString();
    Modifier mod = DoubleModifier.byTagPattern(tag);
    if (mod == null) mod = BooleanModifier.byTagPattern(tag);
    if (mod == null) mod = BitmapModifier.byTagPattern(tag);
    if (mod == null) mod = StringModifier.byTagPattern(tag);
    return mod;
  }

  /**
   * The item's CARRIED_OVER modifiers, taken verbatim from its base modifier string. These cannot
   * be read back from the item description, so introspection has to restore them from the base
   * item. Read raw rather than from a {@link net.sourceforge.kolmafia.Modifiers}: an expression
   * value like "[env(underwater)]" is an innate, context-dependent property, and evaluating it here
   * would bake in the deriving context (usually zero, which reads as absent).
   *
   * <p>Returned in CARRIED_OVER order, which is sorted by name, so the output is reproducible.
   */
  private static List<ModifierValue> carriedOverModifiers(final int itemId) {
    var byModifier = new HashMap<Modifier, List<ModifierValue>>();
    for (var m : rawModifiers(itemId)) {
      var mod = resolveModifier(m);
      if (mod != null && CARRIED_OVER.contains(mod)) {
        byModifier.computeIfAbsent(mod, k -> new ArrayList<>()).add(m);
      }
    }
    var carried = new ArrayList<ModifierValue>();
    for (var mod : CARRIED_OVER) {
      carried.addAll(byModifier.getOrDefault(mod, List.of()));
    }
    return carried;
  }

  private static TCRS introspectItem(final int itemId, final String text) {
    // Parse the things that are changed in TCRS
    String name = DebugDatabase.parseName(text);
    int size = DebugDatabase.parseConsumableSize(text);
    var quality = DebugDatabase.parseQuality(text);
    ArrayList<String> unknown = new ArrayList<>();
    var modifiers =
        ModifierDatabase.splitModifiers(
            DebugDatabase.parseItemEnchantments(text, unknown, ConsumptionType.UNKNOWN));

    // Compare whole modifiers, not substrings: "Thorns: 1" is a substring of "Sporadic Thorns: 1".
    var present = new HashSet<String>();
    for (var m : modifiers) {
      present.add(m.toString());
    }
    for (var mod : carriedOverModifiers(itemId)) {
      if (present.add(mod.toString())) {
        modifiers.addModifier(mod);
      }
    }

    // Create and return the TCRS object
    return new TCRS(name, size, quality, modifiers.toString());
  }

  private static Map<String, List<String>> STRINGS;
  private static Set<String> ADJECTIVES;

  private static void buildEffectPool() {
    TCRSEffectPool.clear();
    EffectDatabase.keys().stream()
        // Effects must be marked as good
        .filter(id -> EffectDatabase.getQuality(id) == Quality.GOOD)
        // Effects must be hookah/wish-able, except Fishy: it became nohookah after TCRS
        // launched but is still in the path's effect pool.
        .filter(id -> !EffectDatabase.hasAttribute(id, "nohookah") || id == EffectPool.FISHY)
        // Some effects are unavailable for no obvious reason, so they are tagged notcrs
        .filter(id -> !EffectDatabase.hasAttribute(id, "notcrs"))
        // Limited to effects available at path launch (Tiki Temerity and prior)
        .filter(id -> id <= EffectPool.TIKI_TEMERITY)
        .forEachOrdered(TCRSEffectPool::add);
  }

  private static String removeAdjectives(final String name) {
    return Arrays.stream(name.split(" "))
        .filter(Predicate.not(ADJECTIVES::contains))
        .collect(Collectors.joining(" "));
  }

  /**
   * The cosmetic adjectives for an item, before shuffling: an optional color followed by 0-3
   * cosmetic adjectives, all rolled from the item's base seed via mtrand.
   */
  private static ArrayList<String> buildCosmeticList(final PHPMTRandom mtRng, final int max) {
    var cosmeticMods = new ArrayList<String>();

    if (mtRng.nextInt(1, max) == 1) {
      cosmeticMods.add(mtRng.pickOne(STRINGS.get("Color")));
    }

    var numCosmeticMods = 0;
    if (mtRng.nextInt(1, max) == 1) numCosmeticMods++;
    if (mtRng.nextInt(1, max) == 1) numCosmeticMods++;
    if (mtRng.nextInt(1, max) == 1) numCosmeticMods++;

    for (var i = 0; i < numCosmeticMods; i++) {
      cosmeticMods.add(mtRng.pickOne(STRINGS.get("Cosmetic")));
    }

    return cosmeticMods;
  }

  /**
   * Shuffles the cosmetic adjectives with the given rand stream and joins them into a name prefix.
   * The stream matters: for equipment the shuffle continues the same seed+10 stream the
   * enchantments were selected from (see {@link #deriveEquipment}). Everything else uses the base
   * seed stream.
   */
  private static String shuffleCosmetics(
      final ArrayList<String> cosmeticMods, final PHPRandom rng) {
    if (cosmeticMods.size() > 0) {
      rng.shuffle(cosmeticMods);
    }

    Collections.reverse(cosmeticMods);

    return String.join(" ", cosmeticMods);
  }

  private static String rollCosmetics(final PHPMTRandom mtRng, final PHPRandom rng, final int max) {
    return shuffleCosmetics(buildCosmeticList(mtRng, max), rng);
  }

  static class Enchantment {
    String effect;
    int duration;

    Enchantment(String effect, int duration) {
      this.effect = effect;
      this.duration = duration;
    }
  }

  private static int seedFor(
      final int itemId, final AscensionClass ascensionClass, final ZodiacSign sign) {
    return (50 * itemId) + (12345 * sign.getId()) + (100000 * ascensionClass.getId());
  }

  private static int sizeFor(final int id) {
    return switch (ItemDatabase.getConsumptionType(id)) {
      case EAT -> ConsumablesDatabase.getFullness(id);
      case DRINK -> ConsumablesDatabase.getInebriety(id);
      case SPLEEN -> ConsumablesDatabase.getSpleenHit(id);
      default -> 0;
    };
  }

  private static String joinName(final String... parts) {
    return Arrays.stream(parts)
        .filter(Predicate.not(String::isBlank))
        .collect(Collectors.joining(" "));
  }

  // The raw (unevaluated) base modifiers of an item, so context-dependent expressions are preserved
  // rather than evaluated in the deriving context.
  private static ModifierList rawModifiers(final int itemId) {
    var raw = ModifierDatabase.getModifierString(new Lookup(ModifierType.ITEM, itemId));
    return ModifierDatabase.splitModifiers(raw == null ? "" : raw);
  }

  // A modifier value that is a bracketed expression is an innate, context-dependent property (e.g.
  // "[100*zone(Hobopolis)]") rather than a fixed re-rolled enchantment value.
  private static boolean isExpression(final String value) {
    return value != null && value.contains("[");
  }

  /**
   * The modifiers of an item TCRS does not re-roll: its full base modifiers, minus our internal
   * Enchantment Count bookkeeping. Used for NOT_RE_ROLLED items and the categories TCRS leaves
   * functionally unchanged (miscellaneous items, combat items, familiar equipment). Only their name
   * gains cosmetics.
   */
  private static ModifierList unalteredModifiers(final int itemId) {
    var mods = new ModifierList();
    for (var m : rawModifiers(itemId)) {
      if (!m.getName().equals("Enchantment Count")) {
        mods.addModifier(m);
      }
    }
    return mods;
  }

  /**
   * The modifiers a re-rolled item (equipment or a consumable) keeps: its base modifiers minus the
   * ones TCRS re-rolls (the enchantment stats and the item's intrinsic effect). The new
   * enchantments are added on top by the caller. Enchantment modifiers with an expression value are
   * innate, context-dependent properties (not a fixed re-roll), so they are kept verbatim.
   */
  private static ModifierList getRetainedModifiers(final int itemId) {
    var retained = new ModifierList();
    for (var m : rawModifiers(itemId)) {
      var name = m.getName();
      // TCRS never keeps an item's intrinsic effect: consumables get a fresh one added by the
      // caller, everything else (e.g. the outrageous sombrero's "La Bamba") simply loses it.
      if (name.equals("Effect") || name.equals("Effect Duration")) {
        continue;
      }
      // Our own bookkeeping modifier, never part of an item's real modifiers.
      if (name.equals("Enchantment Count")) {
        continue;
      }
      var mod = ModifierDatabase.getModifierByName(name);
      if (mod != null && mod.isEnchantment() && !isExpression(m.getValue())) {
        continue; // a re-rolled enchantment, replaced below by the caller
      }
      retained.addModifier(m);
    }
    return retained;
  }

  // The effect roll can overflow the pool by 1, which resolves to the last effect.
  private static String effectNameAt(final int roll) {
    var index = Math.min(roll, TCRSEffectPool.size() - 1);
    return EffectPool.get(TCRSEffectPool.get(index)).getDisambiguatedName();
  }

  private static Enchantment rollConsumableEnchantment(final int itemId, final PHPMTRandom mtRng) {
    var effectName = effectNameAt(mtRng.nextInt(0, TCRSEffectPool.size()));
    var duration = 5 * mtRng.nextInt(1, 10);

    return new Enchantment(effectName, duration);
  }

  public static TCRS derivePotion(
      final AscensionClass ascensionClass, final ZodiacSign sign, final AdventureResult item) {
    var id = item.getItemId();
    var seed = seedFor(id, ascensionClass, sign);
    var mtRng = new PHPMTRandom(seed);
    var rng = new PHPRandom(seed);

    var cosmeticsString = rollCosmetics(mtRng, rng, 6);

    if (TCRS_GENERIC.contains(id)) {
      var name = joinName(cosmeticsString, removeAdjectives(ItemDatabase.getItemName(id)));
      return new TCRS(name, 0, ConsumableQuality.NONE, unalteredModifiers(id).toString());
    }

    var mods = getRetainedModifiers(id);

    var potionMods = new ArrayList<String>();

    var numPotionMods = 1;
    if (mtRng.nextInt(1, 3) == 1) numPotionMods++;
    if (mtRng.nextInt(1, 3) == 1) numPotionMods++;

    for (var i = 0; i < numPotionMods; i++) {
      potionMods.add(mtRng.pickOne(STRINGS.get("Potion Mod")));
    }

    var effectName = effectNameAt(mtRng.nextInt(0, TCRSEffectPool.size()));

    var duration = mtRng.nextInt(11, 69);

    var potionPrefixes = STRINGS.get("Potion Prefix");
    var prefixedPotionMods = new ArrayList<String>();

    for (var mod : potionMods) {
      var prefixRoll = mtRng.nextInt(1, 40);
      if (prefixRoll <= potionPrefixes.size()) {
        mod = potionPrefixes.get(prefixRoll - 1) + "-" + mod;
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
        joinName(
            potionString,
            cosmeticsString,
            removeAdjectives(ItemDatabase.getItemName(item.getItemId())));

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

  // Size descriptors keyed by size bucket (1..6). Buckets with no entries are simply absent.
  private static Map<Integer, List<String>> FOOD_SIZE_DESCRIPTORS;
  private static Map<Integer, List<String>> BOOZE_SIZE_DESCRIPTORS;
  private static Map<ConsumableQuality, List<String>> FOOD_QUALITY_DESCRIPTORS;
  private static Map<ConsumableQuality, List<String>> BOOZE_QUALITY_DESCRIPTORS;

  private static final Set<Integer> ZERO_ADVENTURE_CONSUMABLES =
      Set.of(ItemPool.UNIDENTIFIED_DRINK);

  // Consumables TCRS renames but leaves otherwise unchanged: their modifiers are kept, not
  // re-rolled.
  private static final Set<Integer> UNALTERED_CONSUMABLES =
      Set.of(ItemPool.GUNPOWDER_BURRITO, ItemPool.BEERY_BLOOD);

  // Consumables whose size is forced to zero.
  private static final Set<Integer> ZERO_SIZE_CONSUMABLES =
      Set.of(ItemPool.QUANTUM_TACO, ItemPool.SCHRODINGERS_THERMOS, ItemPool.SMORE);

  private static int rollConsumableSize(final PHPMTRandom mtRng) {
    return switch (mtRng.nextInt(1, 10)) {
      case 1 -> 1;
      case 2, 3 -> 2;
      case 4, 5, 6 -> 3;
      case 7, 8 -> 4;
      case 9 -> 5;
      case 10 -> 5 + mtRng.nextInt(1, 5);
      default -> 0;
    };
  }

  private static void addSizeAndQualityAdjectives(
      final List<String> adjectives,
      final PHPMTRandom mtRng,
      final boolean isFood,
      final int size,
      final ConsumableQuality quality) {
    var sizeDescriptors =
        (isFood ? FOOD_SIZE_DESCRIPTORS : BOOZE_SIZE_DESCRIPTORS)
            .getOrDefault(Math.min(size, 6), List.of());
    if (sizeDescriptors.size() > 0) {
      adjectives.add(mtRng.pickOne(sizeDescriptors));
    }

    var qualityDescriptors =
        (isFood ? FOOD_QUALITY_DESCRIPTORS : BOOZE_QUALITY_DESCRIPTORS).get(quality);
    adjectives.add(
        qualityDescriptors.size() > 1
            ? mtRng.pickOne(qualityDescriptors)
            : qualityDescriptors.get(0));
  }

  /** An EPIC consumable is promoted further by how many adventures it packs into its size. */
  private static ConsumableQuality promoteEpic(
      final ConsumableQuality quality, final int size, final double baseAdventures) {
    if (quality != ConsumableQuality.EPIC || size <= 0) {
      return quality;
    }
    return ConsumablesDatabase.superEpicQuality(baseAdventures / size);
  }

  private static TCRS deriveFoodBooze(
      final AscensionClass ascensionClass,
      final ZodiacSign sign,
      final AdventureResult item,
      final boolean isFood) {
    var id = item.getItemId();
    var seed = seedFor(id, ascensionClass, sign);
    var mtRng = new PHPMTRandom(seed);
    var rng = new PHPRandom(seed);

    var beverage = ConsumablesDatabase.isBeverage(id);

    var cosmeticsString = rollCosmetics(mtRng, rng, beverage ? 8 : 10);

    if (UNALTERED_CONSUMABLES.contains(id)) {
      var name = joinName(cosmeticsString, removeAdjectives(ItemDatabase.getItemName(id)));
      var mods = getRetainedModifiers(id);
      var size = sizeFor(id);
      var quality = ConsumablesDatabase.getQuality(id);
      return new TCRS(name, size, quality, mods.toString());
    }

    var qualityRoll = mtRng.nextInt(1, 7);
    var quality =
        isFood ? determineFoodQuality(qualityRoll, beverage) : determineBoozeQuality(qualityRoll);

    var size = beverage ? 1 : rollConsumableSize(mtRng);

    var adjectives = new ArrayList<String>();

    if (!beverage) {
      addSizeAndQualityAdjectives(adjectives, mtRng, isFood, size, quality);
    }

    if (qualityToTurnsPerFullness(quality) * size >= 8) {
      mtRng.nextDouble();
    }

    var mods = getRetainedModifiers(id);

    var rolledEnchantment = mtRng.nextInt(1, 10) == 1;
    if (rolledEnchantment) {
      adjectives.add(mtRng.pickOne(STRINGS.get("Food Enchantment")));
    }

    var enchantment = rollConsumableEnchantment(id, mtRng);

    var hardcoded = HARDCODED_EFFECT.contains(id);
    var dynamicDuration = HARDCODED_EFFECT_DYNAMIC_DURATION.contains(id);
    if (hardcoded) {
      var effectOverride = HARDCODED_EFFECT_OVERRIDE.get(id);
      enchantment.effect =
          effectOverride != null
              ? EffectPool.get(effectOverride).getDisambiguatedName()
              : ModifierDatabase.getStringModifier(ModifierType.ITEM, id, StringModifier.EFFECT);

      if (!dynamicDuration) {
        enchantment.duration =
            (int)
                ModifierDatabase.getNumericModifier(
                    ModifierType.ITEM, id, DoubleModifier.EFFECT_DURATION);
      }
    }

    var enchanted = hardcoded || rolledEnchantment;
    if (enchanted && !enchantment.effect.isBlank()) {
      mods.addModifier("Effect", enchantment.effect);
      if (rolledEnchantment || !dynamicDuration) {
        mods.addModifier("Effect Duration", String.valueOf(enchantment.duration));
      }
    }

    if (ZERO_SIZE_CONSUMABLES.contains(id)) {
      size = 0;
    }

    rng.shuffle(adjectives);

    Collections.reverse(adjectives);

    adjectives.add(cosmeticsString);
    adjectives.add(removeAdjectives(ItemDatabase.getItemName(item.getItemId())));

    var name =
        adjectives.stream().filter(Predicate.not(String::isBlank)).collect(Collectors.joining(" "));

    var baseAdventures =
        ZERO_ADVENTURE_CONSUMABLES.contains(id)
            ? 0.0
            : ConsumablesDatabase.getBaseAverageAdventures(id);

    return new TCRS(name, size, promoteEpic(quality, size, baseAdventures), mods.toString());
  }

  /** Items whose item types are ignored for TCRS */
  private static final Set<Integer> TCRS_GENERIC =
      Set.of(
          // Potions
          ItemPool.JAZZ_SOAP,
          ItemPool.CAN_OF_BINARRRCA,
          // Food
          ItemPool.SMOOCH_SODA,
          ItemPool.TAINTED_MILK);

  /** Items that TCRS does not rename or re-roll cosmetics/enchantments for */
  public static final Set<Integer> NOT_RE_ROLLED =
      Set.of(
          // Dynamically named consumables
          ItemPool.EXPERIMENTAL_CRIMBO_FOOD,
          ItemPool.EXPERIMENTAL_CRIMBO_BOOZE,
          ItemPool.EXPERIMENTAL_CRIMBO_SPLEEN,
          ItemPool.LOVE_POTION_XYZ,
          ItemPool.DIABOLIC_PIZZA,
          ItemPool.VAMPIRE_VINTNER_WINE,
          // Equipment that TCRS never re-rolls, some of which are dynamically named
          ItemPool.RING,
          ItemPool.PANTOGRAM_PANTS,
          ItemPool.GARLAND_OF_GREATNESS,
          ItemPool.BACKUP_CAMERA,
          ItemPool.CURSED_MONKEY_PAW,
          ItemPool.AUGUST_SCEPTER,
          ItemPool.REPLICA_AUGUST_SCEPTER,
          ItemPool.FRANKEN_STEIN,
          ItemPool.FUTURISTIC_SHIRT,
          ItemPool.FUTURISTIC_HAT,
          ItemPool.FUTURISTIC_COLLAR,
          ItemPool.MIMIC_EGG,
          ItemPool.ROMAN_CANDELABRA,
          ItemPool.MONODENT_OF_THE_SEA,
          ItemPool.PRISMATIC_BERET,
          ItemPool.UNBREAKABLE_UMBRELLA,
          ItemPool.KNOCK_OFF_RETRO_SUPERHERO_CAPE,
          ItemPool.THE_ETERNITY_CODPIECE,
          ItemPool.HEARTSTONE,
          ItemPool.BASEBALL_DIAMOND,
          ItemPool.CUP_OF_13S);

  /** Items that keep their Effect despite rolling for a new one */
  private static final Set<Integer> HARDCODED_EFFECT =
      Set.of(
          ItemPool.WREATH_CRIMBO_COOKIE,
          ItemPool.BELL_CRIMBO_COOKIE,
          ItemPool.TREE_CRIMBO_COOKIE,
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

  private static final Map<Integer, Integer> HARDCODED_EFFECT_OVERRIDE =
      Map.ofEntries(
          Map.entry(ItemPool.SKULL_CRIMBOWEEN_COOKIE, EffectPool.BELLS_IN_THE_BATFRY),
          Map.entry(ItemPool.TURTLE_SOUP, EffectPool.A_LITTLE_BIT_EVIL),
          Map.entry(ItemPool.QUEEN_COOKIE, EffectPool.TOWERING_STRENGTH),
          Map.entry(ItemPool.SUN_DRIED_TOFU, EffectPool.OVERSATURATED_PALATE));

  private static TCRS deriveSpleen(
      final AscensionClass ascensionClass, final ZodiacSign sign, final AdventureResult item) {
    var id = item.getItemId();
    var seed = seedFor(id, ascensionClass, sign);
    var mtRng = new PHPMTRandom(seed);
    var rng = new PHPRandom(seed);

    var cosmeticsString = rollCosmetics(mtRng, rng, 4);

    var quality = determineSpleenQuality(mtRng.nextInt(1, 7));

    var adjective = mtRng.pickOne(STRINGS.get("Spleen Mod"));

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

    var name = joinName(adjective, cosmeticsString, removeAdjectives(ItemDatabase.getItemName(id)));

    return new TCRS(name, 1, quality, mods.toString());
  }

  protected static List<Entry<String, String>> EQUIPMENT_MODIFIERS;

  private static TCRS deriveEquipment(
      final AscensionClass ascensionClass, final ZodiacSign sign, final AdventureResult item) {
    var id = item.getItemId();
    var seed = seedFor(id, ascensionClass, sign);
    var mtRng = new PHPMTRandom(seed);

    // Cosmetics roll from the base seed, but the shuffle that orders them uses the seed+10 rand
    // stream the enchantments are selected from (see below). Build the list now and shuffle it once
    // that stream has advanced.
    var cosmeticList = buildCosmeticList(mtRng, 8);

    var root = removeAdjectives(ItemDatabase.getItemName(id));
    var mods = getRetainedModifiers(id);

    // Enchantments are selected from a rand stream seeded with the per-item seed plus 10. Each
    // enchantment produces a modifier and an adjective for the name: "of ..." adjectives are
    // suffixes. The rest are prefixes, with the earliest-selected closest to the root.
    var count = enchantCount(id);
    var enchantRng = new PHPRandom(seed + 10);
    var prefixes = new ArrayList<String>();
    var suffixes = new ArrayList<String>();
    for (var entry : getMods(id, ascensionClass, sign, count, enchantRng)) {
      var descriptor = entry.getKey();
      if (descriptor.startsWith("of ")) {
        suffixes.add(descriptor);
      } else {
        prefixes.add(0, descriptor);
      }
      DebugDatabase.appendModifier(mods, entry.getValue());
    }

    // KoL shuffles cosmetics on the same stream it selected enchantments from, so continue
    // enchantRng: it is already advanced after a multi-enchantment selection, and still fresh at
    // seed+10 after a single mtrand-picked one. With no enchantments there is no seed+10 stream, so
    // the shuffle falls back to the base seed stream.
    var shuffleRng = (count == 0) ? new PHPRandom(seed) : enchantRng;
    var cosmeticsString = shuffleCosmetics(cosmeticList, shuffleRng);

    // Enchant adjectives that are common adjectives (e.g. "lucky") are stripped from the name, as
    // KoL does after applying enchantments. Cosmetics are not stripped.
    var name =
        Stream.of(
                Stream.of(cosmeticsString),
                prefixes.stream().filter(Predicate.not(ADJECTIVES::contains)),
                Stream.of(root),
                suffixes.stream().filter(Predicate.not(ADJECTIVES::contains)))
            .flatMap(s -> s)
            .filter(Predicate.not(String::isBlank))
            .collect(Collectors.joining(" "));

    return new TCRS(name, 0, ConsumableQuality.NONE, mods.toString());
  }

  /**
   * The enchantments rolled for an equipment item, seeded with the per-item seed plus 10. A single
   * enchantment is picked with an mtrand roll. Multiple enchantments are picked together without
   * replacement. The multi-pick draws from {@code enchantRng} (seeded with that same seed+10) so
   * the caller can continue the stream for the cosmetic shuffle.
   */
  private static List<Entry<String, String>> getMods(
      final int itemId,
      final AscensionClass ascensionClass,
      final ZodiacSign sign,
      final int count,
      final PHPRandom enchantRng) {
    var seed = seedFor(itemId, ascensionClass, sign) + 10;
    var mods = new ArrayList<Entry<String, String>>(count);
    var indices =
        new PHPRandomSelection(enchantRng, new PHPMTRandom(seed))
            .pick(EQUIPMENT_MODIFIERS.size(), count);
    for (var index : indices) {
      mods.add(EQUIPMENT_MODIFIERS.get(index));
    }
    return mods;
  }

  // Families that Mafia expands from a single KoL enchantment. Members sharing a value are one
  // combined enchantment (all resistance, prismatic damage, all attributes, Maximum HP + MP, ...).
  // Members with differing values are separate enchantments, so a family contributes one
  // enchantment per distinct value present.
  private static final Set<Set<DoubleModifier>> COLLAPSIBLE =
      Set.of(
          EnumSet.of(
              DoubleModifier.HOT_RESISTANCE,
              DoubleModifier.COLD_RESISTANCE,
              DoubleModifier.SPOOKY_RESISTANCE,
              DoubleModifier.STENCH_RESISTANCE,
              DoubleModifier.SLEAZE_RESISTANCE),
          EnumSet.of(
              DoubleModifier.HOT_DAMAGE,
              DoubleModifier.COLD_DAMAGE,
              DoubleModifier.SPOOKY_DAMAGE,
              DoubleModifier.STENCH_DAMAGE,
              DoubleModifier.SLEAZE_DAMAGE));
  private static final Set<DoubleModifier> REGEN =
      EnumSet.of(
          DoubleModifier.HP_REGEN_MIN,
          DoubleModifier.HP_REGEN_MAX,
          DoubleModifier.MP_REGEN_MIN,
          DoubleModifier.MP_REGEN_MAX);

  /**
   * How many regen enchantments an item has. HP and MP regen are one combined enchantment when
   * their amounts match ("Regenerate X HP and MP"), but two separate ones when the amounts differ.
   */
  private static int regenCount(final Map<Modifier, Set<String>> present) {
    var hp =
        present.containsKey(DoubleModifier.HP_REGEN_MIN)
            || present.containsKey(DoubleModifier.HP_REGEN_MAX);
    var mp =
        present.containsKey(DoubleModifier.MP_REGEN_MIN)
            || present.containsKey(DoubleModifier.MP_REGEN_MAX);
    if (!hp || !mp) {
      return (hp || mp) ? 1 : 0;
    }
    var sameAmounts =
        Objects.equals(
                present.get(DoubleModifier.HP_REGEN_MIN), present.get(DoubleModifier.MP_REGEN_MIN))
            && Objects.equals(
                present.get(DoubleModifier.HP_REGEN_MAX), present.get(DoubleModifier.MP_REGEN_MAX));
    return sameAmounts ? 1 : 2;
  }

  // Expression functions that query live character or environment state (a preference, the current
  // zone/location environment, an active effect, ascension class or path). The enchantment
  // pre-computation can't resolve these, so a base modifier whose value depends on one isn't a
  // re-rolled enchantment and doesn't count. Pure arithmetic (min/max/floor/ceil/sqrt) and the
  // supported queries (skill, event) are fine and still count.
  // In the future it could be nice to parse the ModifierExpression and ask the AST whether the
  // value should be applied, rather than substring matching.
  private static final List<String> UNSUPPORTED_FUNCTIONS =
      List.of("pref(", "env(", "zone(", "effect(", "class(", "path(");

  static {
    loadStringData();
  }

  // Reads the ordered string tables from tcrs.txt. Order matters for every list here. The RNG
  // indexes into them by position, so the file must never be sorted or de-duplicated.
  private static void loadStringData() {
    STRINGS = new HashMap<>();
    FOOD_SIZE_DESCRIPTORS = new HashMap<>();
    BOOZE_SIZE_DESCRIPTORS = new HashMap<>();
    FOOD_QUALITY_DESCRIPTORS = new EnumMap<>(ConsumableQuality.class);
    BOOZE_QUALITY_DESCRIPTORS = new EnumMap<>(ConsumableQuality.class);
    EQUIPMENT_MODIFIERS = new ArrayList<>();
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("tcrs.txt", KoLConstants.TCRS_VERSION)) {
      String[] data;
      while ((data = FileUtilities.readData(reader)) != null) {
        switch (data[0]) {
          case "Food Size" ->
              FOOD_SIZE_DESCRIPTORS
                  .computeIfAbsent(Integer.parseInt(data[1]), k -> new ArrayList<>())
                  .add(data[2]);
          case "Booze Size" ->
              BOOZE_SIZE_DESCRIPTORS
                  .computeIfAbsent(Integer.parseInt(data[1]), k -> new ArrayList<>())
                  .add(data[2]);
          case "Food Quality" ->
              FOOD_QUALITY_DESCRIPTORS
                  .computeIfAbsent(ConsumableQuality.valueOf(data[1]), k -> new ArrayList<>())
                  .add(data[2]);
          case "Booze Quality" ->
              BOOZE_QUALITY_DESCRIPTORS
                  .computeIfAbsent(ConsumableQuality.valueOf(data[1]), k -> new ArrayList<>())
                  .add(data[2]);
          case "Equipment Enchant" -> EQUIPMENT_MODIFIERS.add(Map.entry(data[1], data[2]));
          // Every other tag is a simple ordered word list keyed by the tag.
          default -> STRINGS.computeIfAbsent(data[0], k -> new ArrayList<>()).add(data[1]);
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }

    ADJECTIVES = new HashSet<>(STRINGS.getOrDefault("Adjective", List.of()));
  }

  private static boolean isEnchantableValue(final String value) {
    if (value == null || !value.startsWith("[")) {
      return true;
    }
    return UNSUPPORTED_FUNCTIONS.stream().noneMatch(value::contains);
  }

  /**
   * How many enchantments the base item has, which is how many TCRS re-rolls. This is not the raw
   * modifier count: non-enchantment modifiers (class restrictions, familiar effects, ...) don't
   * count, an expanded family like "all resistance" counts once even though Mafia stores it as five
   * elemental resistances, a regen min/max pair counts once, and familiar equipment's innate
   * Familiar Weight doesn't count. Which modifiers are truly re-rolled isn't fully known, so this
   * is a best estimate.
   */
  static int enchantCount(final int itemId) {
    var modifiers = ModifierDatabase.getModifierList(new Lookup(ModifierType.ITEM, itemId));

    // Authoritative when present, for items whose re-rolled enchantment count can't be derived
    // from the base modifiers.
    if (modifiers.containsModifier("Enchantment Count")) {
      return (int) Double.parseDouble(modifiers.getModifierValue("Enchantment Count"));
    }

    // Enchantable base modifiers with their values, so collapsible families can be split by value.
    var present = new HashMap<Modifier, Set<String>>();
    for (var mv : modifiers) {
      var modifier = ModifierDatabase.getModifierByName(mv.getName());
      if (modifier != null && modifier.isEnchantment() && isEnchantableValue(mv.getValue())) {
        present.computeIfAbsent(modifier, key -> new HashSet<>()).add(mv.getValue());
      }
    }

    var count = 0;
    var consumed = new HashSet<Modifier>();

    // A collapsible family is one combined enchantment only when the whole family is present with a
    // single shared value (all resistance, prismatic damage, Maximum HP + MP at one value, ...).
    // Otherwise its members are separate enchantments, counted individually below.
    for (var family : COLLAPSIBLE) {
      var values = new HashSet<String>();
      var complete = true;
      for (var modifier : family) {
        if (!present.containsKey(modifier)) {
          complete = false;
          break;
        }
        values.addAll(present.get(modifier));
      }
      if (complete && values.size() == 1) {
        count += 1;
        consumed.addAll(family);
      }
    }

    var regen = regenCount(present);
    if (regen > 0) {
      count += regen;
      consumed.addAll(REGEN);
    }

    var isFamiliarEquipment =
        ItemDatabase.getConsumptionType(itemId) == ConsumptionType.FAMILIAR_EQUIPMENT;
    for (var entry : present.entrySet()) {
      var modifier = entry.getKey();
      if (consumed.contains(modifier)) continue;
      // Innate, not an enchantment.
      if (isFamiliarEquipment && modifier == DoubleModifier.FAMILIAR_WEIGHT) continue;
      // A base modifier that KoL displays as several lines (e.g. two distinct rollover effects in
      // Uncle Crimbo's hat) is one enchantment per distinct value.
      count += entry.getValue().size();
    }

    return count;
  }

  private static TCRS deriveGeneric(
      final AscensionClass ascensionClass, final ZodiacSign sign, final AdventureResult item) {
    var id = item.getItemId();
    var seed = seedFor(id, ascensionClass, sign);
    var mtRng = new PHPMTRandom(seed);
    var rng = new PHPRandom(seed);

    var cosmeticsString = rollCosmetics(mtRng, rng, 8);

    var name = joinName(cosmeticsString, removeAdjectives(ItemDatabase.getItemName(id)));

    // These items (miscellaneous, combat items, familiar equipment, ...) are not altered in
    // function by TCRS, so their modifiers are left untouched. Only the name gains cosmetics.
    var mods = unalteredModifiers(id);

    var size = sizeFor(id);

    var quality = ConsumablesDatabase.getQuality(id);

    return new TCRS(name, size, quality, mods.toString());
  }

  /**
   * Cafe consumables are rolled like any other food or booze, off the same seed as the real item
   * with that id, but they never gain an enchantment.
   */
  public static TCRS deriveCafe(
      final AscensionClass ascensionClass,
      final ZodiacSign sign,
      final int id,
      final boolean isFood) {
    var baseName = isFood ? CafeDatabase.getCafeFoodName(id) : CafeDatabase.getCafeBoozeName(id);
    if (baseName == null) {
      return null;
    }

    var seed = seedFor(id, ascensionClass, sign);
    var mtRng = new PHPMTRandom(seed);
    var rng = new PHPRandom(seed);

    var cosmeticsString = rollCosmetics(mtRng, rng, 10);

    var qualityRoll = mtRng.nextInt(1, 7);
    var quality =
        isFood ? determineFoodQuality(qualityRoll, false) : determineBoozeQuality(qualityRoll);

    var size = rollConsumableSize(mtRng);

    var adjectives = new ArrayList<String>();
    addSizeAndQualityAdjectives(adjectives, mtRng, isFood, size, quality);

    if (qualityToTurnsPerFullness(quality) * size >= 8) {
      mtRng.nextDouble();
    }

    if (mtRng.nextInt(1, 10) == 1) {
      adjectives.add(mtRng.pickOne(STRINGS.get("Food Enchantment")));
    }

    rng.shuffle(adjectives);
    Collections.reverse(adjectives);
    adjectives.add(cosmeticsString);
    adjectives.add(baseName);

    var name =
        adjectives.stream().filter(Predicate.not(String::isBlank)).collect(Collectors.joining(" "));

    var baseAdventures =
        ConsumablesDatabase.getBaseAverageAdventures(
            ConsumablesDatabase.getConsumableByName(baseName));

    return new TCRS(name, size, promoteEpic(quality, size, baseAdventures), "");
  }

  public static TCRS deriveItem(
      final AscensionClass ascensionClass, final ZodiacSign sign, final int itemId) {
    // Items not in items.txt (unknown, or only registered live) have no base data, so introspect.
    if (ItemDatabase.getItemName(itemId) == null || ItemDatabase.isRegisteredLive(itemId)) {
      return introspectItem(itemId);
    }

    var item = ItemPool.get(itemId);
    var type = ItemDatabase.getConsumptionType(itemId);

    String displayName =
        ModifierDatabase.getStringModifier(ModifierType.ITEM, itemId, StringModifier.DISPLAY_NAME);
    if (NOT_RE_ROLLED.contains(itemId) || !displayName.isEmpty()) {
      var name = !displayName.isEmpty() ? displayName : ItemDatabase.getItemName(itemId);

      var size =
          switch (type) {
            case EAT -> ConsumablesDatabase.getFullness(name);
            case DRINK -> ConsumablesDatabase.getInebriety(name);
            case SPLEEN -> ConsumablesDatabase.getSpleenHit(name);
            default -> 0;
          };

      return new TCRS(
          name, size, ConsumablesDatabase.getQuality(name), unalteredModifiers(itemId).toString());
    }

    // Not really a food
    if (itemId == ItemPool.GLITCH_ITEM) {
      type = ConsumptionType.NONE;
    }

    return switch (type) {
      case POTION, AVATAR_POTION -> derivePotion(ascensionClass, sign, item);
      case EAT, DRINK -> deriveFoodBooze(ascensionClass, sign, item, type == ConsumptionType.EAT);
      case SPLEEN -> deriveSpleen(ascensionClass, sign, item);
      case HAT, SHIRT, CONTAINER, WEAPON, OFFHAND, PANTS, ACCESSORY ->
          deriveEquipment(ascensionClass, sign, item);
      default -> deriveGeneric(ascensionClass, sign, item);
    };
  }

  private static boolean introspectCafe(final boolean verbose) {
    if (verbose) {
      KoLmafia.updateDisplay("Introspecting TCRS item adjustments for all cafe booze items...");
    }

    for (Integer id : CafeDatabase.cafeBoozeKeySet()) {
      introspectCafe(id, CafeDatabase.boozeDescId(id), TCRSBoozeMap);
    }

    if (verbose) {
      KoLmafia.updateDisplay("Done!");
    }

    if (verbose) {
      KoLmafia.updateDisplay("Introspecting TCRS item adjustments for all cafe food items...");
    }

    for (Integer id : CafeDatabase.cafeFoodKeySet()) {
      introspectCafe(id, CafeDatabase.foodDescId(id), TCRSFoodMap);
    }

    if (verbose) {
      KoLmafia.updateDisplay("Done!");
    }

    return true;
  }

  private static boolean introspectCafe(final int itemId, String descId, Map<Integer, TCRS> map) {
    // Don't do this if we already know the item
    if (map.containsKey(itemId)) {
      return false;
    }

    String text = DebugDatabase.cafeItemDescriptionText(descId);

    TCRS tcrs = introspectItem(itemId, text);

    map.put(itemId, tcrs);

    return true;
  }

  public static boolean applyModifiers() {
    // Remove food/booze/spleen/potion sources for effects
    StringBuilder buffer = new StringBuilder();
    for (var effect : EffectDatabase.values()) {
      String actions = effect.getActions();
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
        effect.setActions(buffer.isEmpty() ? null : buffer.toString());
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

  static int qualityToTurnsPerFullness(ConsumableQuality quality) {
    return switch (quality) {
      case EPIC, SUPER_EPIC, SUPER_ULTRA_EPIC, SUPER_ULTRA_MEGA_EPIC, SUPER_ULTRA_MEGA_TURBO_EPIC ->
          5;
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
        ModifierDatabase.getStringModifier(ModifierType.ITEM, itemName, StringModifier.EFFECT);
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
    var effect = EffectDatabase.getEffectData(effectId);
    String verb =
        switch (usage) {
          case EAT -> "eat ";
          case DRINK -> "drink ";
          case SPLEEN -> "chew ";
          default -> "use ";
        };
    String actions = effect.getActions();
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
    effect.setActions(buffer.toString());
  }

  private static void applyConsumableModifiers(
      final ConsumptionType usage, final String itemName, final TCRS tcrs) {
    var consumable = ConsumablesDatabase.getConsumableByName(itemName);
    Integer lint = ConsumablesDatabase.getLevelReq(consumable);
    int level = lint == null ? 0 : lint;
    int adv =
        (usage == ConsumptionType.SPLEEN)
            ? 0
            : (tcrs.size * qualityToTurnsPerFullness(tcrs.quality));
    int mus = 0;
    int mys = 0;
    int mox = 0;

    var comment = new StringJoiner(", ");

    // Consumable attributes (like SAUCY, BEER, etc) are preserved
    ConsumablesDatabase.getAttributes(consumable).stream().map(Enum::name).forEach(comment::add);

    String effectName =
        ModifierDatabase.getStringModifier(ModifierType.ITEM, itemName, StringModifier.EFFECT);
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
        comment.length() == 0 ? null : comment.toString());
  }

  public static void resetModifiers() {
    // Reset all the data structures that we altered in-place to support a particular TCRS
    // class/sign back to standard KoL values.

    // Nothing to reset if we didn't load TCRS data
    if (currentClassSign.isEmpty()) {
      return;
    }

    TCRSDatabase.reset();

    ModifierDatabase.resetKnownModifiers();
    EffectDatabase.reset();
    ConsumablesDatabase.reset();

    // Check items that vary per person
    InventoryManager.checkMods();

    introspectApplyItem(ItemPool.RING);

    ConcoctionDatabase.resetEffects();
    ConcoctionDatabase.refreshConcoctions();
    ConsumablesDatabase.setVariableConsumables();
    ConsumablesDatabase.calculateAllAverageAdventures();

    KoLCharacter.recalculateAdjustments();
    KoLCharacter.updateStatus();
  }

  // *** support for loading up TCRS data appropriate to your current class/sign
  public static boolean loadTCRSData() {
    return loadTCRSData(true);
  }

  public static boolean loadTCRSData(boolean overrideModifiers) {
    if (!KoLCharacter.isCrazyRandomTwo()) {
      return false;
    }

    return loadTCRSData(
        KoLCharacter.getAscensionClass(), KoLCharacter.getSign(), true, overrideModifiers);
  }

  private static boolean loadTCRSData(
      final AscensionClass ascensionClass,
      final ZodiacSign sign,
      final boolean verbose,
      final boolean overrideModifiers) {
    if (!validate(ascensionClass, sign)) {
      return false;
    }

    // The per-class/sign files are test fixtures and are not shipped, so there is nothing to load.
    derive(ascensionClass, sign, verbose);

    if (overrideModifiers) {
      applyModifiers();
      introspectApplyItem(ItemPool.RING);
    }

    return true;
  }
}
