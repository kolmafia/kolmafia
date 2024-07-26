package net.sourceforge.kolmafia.persistence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierExpression;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.Lookup;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.modifiers.ModifierList.ModifierValue;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.Indexed;
import net.sourceforge.kolmafia.utilities.IntOrString;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.utilities.TwoLevelEnumHashMap;

public class ModifierDatabase {
  // maps for modifiers
  private static final TwoLevelEnumHashMap<ModifierType, IntOrString, String>
      modifierStringsByName = new TwoLevelEnumHashMap<>(ModifierType.class);
  private static final TwoLevelEnumHashMap<ModifierType, IntOrString, Modifiers> modifiersByName =
      new TwoLevelEnumHashMap<>(ModifierType.class);
  private static final Map<String, Modifier> modifierTypesByName = new HashMap<>();
  private static final Map<String, String> familiarEffectByName = new HashMap<>();

  /** Map of synergetic item name to bitmap mask of all items in set */
  private static final Map<String, Integer> synergies = new HashMap<>();

  /** List of slash-separated members of a mutex */
  private static final List<String> mutexes = new ArrayList<>();

  private static final HashSet<String> numericModifiers = new HashSet<>();

  private static final Map<BitmapModifier, Integer> bitmapMasks =
      new EnumMap<>(BitmapModifier.class);

  // constant fields

  public static final String EXPR = "(?:([-+]?[\\d.]+)|\\[([^]]+)\\])";

  private static final Pattern FAMILIAR_EFFECT_PATTERN =
      Pattern.compile("Familiar Effect: \"(.*?)\"");
  private static final Pattern FAMILIAR_EFFECT_TRANSLATE_PATTERN =
      Pattern.compile("([\\d.]+)\\s*x\\s*(Volley|Somb|Lep|Fairy)");
  private static final String FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT = "$2: $1 ";
  private static final Pattern FAMILIAR_EFFECT_TRANSLATE_PATTERN2 =
      Pattern.compile("cap ([\\d.]+)");
  private static final String FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT2 = "Familiar Weight Cap: $1 ";

  private static final String COLD = DoubleModifier.COLD_RESISTANCE.getTag() + ": ";
  private static final String HOT = DoubleModifier.HOT_RESISTANCE.getTag() + ": ";
  private static final String SLEAZE = DoubleModifier.SLEAZE_RESISTANCE.getTag() + ": ";
  private static final String SPOOKY = DoubleModifier.SPOOKY_RESISTANCE.getTag() + ": ";
  private static final String STENCH = DoubleModifier.STENCH_RESISTANCE.getTag() + ": ";
  private static final String SLIME = DoubleModifier.SLIME_RESISTANCE.getTag() + ": ";
  private static final String SUPERCOLD = DoubleModifier.SUPERCOLD_RESISTANCE.getTag() + ": ";

  private static final String MOXIE = DoubleModifier.MOX.getTag() + ": ";
  private static final String MUSCLE = DoubleModifier.MUS.getTag() + ": ";
  private static final String MYSTICALITY = DoubleModifier.MYS.getTag() + ": ";

  private static final String MOXIE_PCT = DoubleModifier.MOX_PCT.getTag() + ": ";
  private static final String MUSCLE_PCT = DoubleModifier.MUS_PCT.getTag() + ": ";
  private static final String MYSTICALITY_PCT = DoubleModifier.MYS_PCT.getTag() + ": ";

  private static final String HP_TAG = DoubleModifier.HP.getTag() + ": ";
  private static final String MP_TAG = DoubleModifier.MP.getTag() + ": ";

  private static final String HP_REGEN_MIN_TAG = DoubleModifier.HP_REGEN_MIN.getTag() + ": ";
  private static final String HP_REGEN_MAX_TAG = DoubleModifier.HP_REGEN_MAX.getTag() + ": ";
  private static final String MP_REGEN_MIN_TAG = DoubleModifier.MP_REGEN_MIN.getTag() + ": ";
  private static final String MP_REGEN_MAX_TAG = DoubleModifier.MP_REGEN_MAX.getTag() + ": ";

  private static final Pattern SKILL_PATTERN = Pattern.compile("Grants Skill:.*?<b>(.*?)</b>");
  private static final Pattern DR_PATTERN =
      Pattern.compile("Damage Reduction: (<b>)?([+-]?\\d+)(</b>)?");
  private static final Pattern SINGLE_PATTERN =
      Pattern.compile("You may not equip more than one of these at a time");
  private static final Pattern SOFTCORE_PATTERN =
      Pattern.compile("This item cannot be equipped while in Hardcore");
  private static final Pattern ITEM_DROPPER_PATTERN = Pattern.compile("Occasional Hilarity");
  private static final Pattern LASTS_ONE_DAY_PATTERN =
      Pattern.compile("This item will disappear at the end of the day");
  private static final Pattern FREE_PULL_PATTERN = Pattern.compile("Free pull from Hagnk's");
  private static final Pattern EFFECT_PATTERN =
      Pattern.compile("Effect: <b><a([^>]*)>([^<]*)</a></b>");
  private static final Pattern EFFECT_DURATION_PATTERN =
      Pattern.compile("</a></b> \\(([\\d]*) Adventures?\\)");
  private static final Pattern SONG_DURATION_PATTERN =
      Pattern.compile("Song Duration: <b>([\\d]*) Adventures</b>");
  private static final Pattern ALL_ATTR_PATTERN = Pattern.compile("^All Attributes ([+-]\\d+)$");
  private static final Pattern ALL_ATTR_PCT_PATTERN =
      Pattern.compile("^All Attributes ([+-]\\d+)%$");
  private static final Pattern CLASS_PATTERN =
      Pattern.compile("Bonus&nbsp;for&nbsp;(.*)&nbsp;only");
  private static final Pattern COMBAT_PATTERN =
      Pattern.compile("Monsters (?:are|will be) (.*) attracted to you");
  private static final Pattern HP_MP_PATTERN = Pattern.compile("^Maximum HP/MP ([+-]\\d+)$");
  private static final Pattern REGEN_PATTERN =
      Pattern.compile("Regenerate (\\d*)-?(\\d*)? ([HM]P)( and .*)? per [aA]dventure$");
  private static final Pattern RESISTANCE_PATTERN = Pattern.compile("Resistance \\(([+-]\\d+)\\)");

  private static final Map<String, String> COMBAT_RATE_DESCRIPTIONS =
      Map.ofEntries(
          Map.entry("incredibly very much more", "+25"),
          Map.entry("<i>way</i> more", "+20"),
          Map.entry("significantly more", "+15"),
          Map.entry("much more", "+10"),
          Map.entry("more", "+5"),
          Map.entry("slightly less", "-3"),
          Map.entry("less", "-5"),
          Map.entry("more than a little less", "-7"),
          Map.entry("quite a bit less", "-9"),
          Map.entry("much less", "-10"),
          Map.entry("very much less", "-11"),
          Map.entry("significantly less", "-15"),
          Map.entry("very very very much less", "-20"),
          Map.entry("<i>way</i> less", "-20"),
          Map.entry("incredibly very much less", "-25"));

  public static final Set<ModifierType> DOUBLED_BY_SQUINT_CHAMPAGNE =
      Set.of(
          ModifierType.BALLROOM,
          ModifierType.BJORN,
          ModifierType.EFFECT,
          ModifierType.ITEM,
          ModifierType.LOCAL_VOTE,
          ModifierType.OUTFIT,
          ModifierType.PATH,
          ModifierType.SIGN,
          ModifierType.SKILL,
          ModifierType.SYNERGY,
          ModifierType.THRONE,
          ModifierType.UNBREAKABLE_UMBRELLA,
          ModifierType.PASSIVES);

  public static void ensureModifierDatabaseInitialised() {
    if (modifierTypesByName.isEmpty()) {
      initialiseModifierDatabase();
    }
  }

  public static void initialiseModifierDatabase() {
    for (var modifier : DoubleModifier.DOUBLE_MODIFIERS) {
      modifierTypesByName.put(modifier.getName(), modifier);
      String tag = modifier.getTag();
      modifierTypesByName.put(tag, modifier);
      numericModifiers.add(tag);
    }
    for (var modifier : BitmapModifier.BITMAP_MODIFIERS) {
      bitmapMasks.put(modifier, 1);
      modifierTypesByName.put(modifier.getName(), modifier);
      modifierTypesByName.put(modifier.getTag(), modifier);
    }
    for (var modifier : BooleanModifier.BOOLEAN_MODIFIERS) {
      modifierTypesByName.put(modifier.getName(), modifier);
      modifierTypesByName.put(modifier.getTag(), modifier);
    }
    for (var modifier : StringModifier.STRING_MODIFIERS) {
      modifierTypesByName.put(modifier.getName(), modifier);
      modifierTypesByName.put(modifier.getTag(), modifier);
    }
    resetModifiers();
  }

  // region: exposure of private fields

  public static boolean isNumericModifier(final String key) {
    // TODO: why is this just doubles, not also derived + bitmap?
    return numericModifiers.contains(key);
  }

  public static final Collection<Entry<IntOrString, String>> getAllModifiersOfType(
      final ModifierType type) {
    return modifierStringsByName.getAll(type).entrySet();
  }

  // WARNING: Slow. Don't use in code that runs repeatedly.
  public static final Collection<Lookup> getAllModifiers() {
    ArrayList<Lookup> result = new ArrayList<>();
    for (var entry : modifierStringsByName.entrySet()) {
      ModifierType type = entry.getKey();
      for (IntOrString key : entry.getValue().keySet()) {
        result.add(new Lookup(type, key));
      }
    }
    return result;
  }

  public static Modifier getModifierByName(String name) {
    return modifierTypesByName.get(name);
  }

  public static final String getFamiliarEffect(final String itemName) {
    return familiarEffectByName.get(itemName);
  }

  // Returned set yields bitmaps keyed by names
  public static Set<Entry<String, Integer>> getSynergies() {
    return Collections.unmodifiableSet(synergies.entrySet());
  }

  // region: utility functions

  public static DoubleModifier elementalResistance(final Element element) {
    return switch (element) {
      case COLD -> DoubleModifier.COLD_RESISTANCE;
      case HOT -> DoubleModifier.HOT_RESISTANCE;
      case SLEAZE -> DoubleModifier.SLEAZE_RESISTANCE;
      case SPOOKY -> DoubleModifier.SPOOKY_RESISTANCE;
      case STENCH -> DoubleModifier.STENCH_RESISTANCE;
      case SLIME -> DoubleModifier.SLIME_RESISTANCE;
      case SUPERCOLD -> DoubleModifier.SUPERCOLD_RESISTANCE;
      default -> null;
    };
  }

  public static final String trimModifiers(final String modifiers, final String remove) {
    ModifierList list = splitModifiers(modifiers);
    list.removeModifier(remove);
    return list.toString();
  }

  /** Get a double, derived or boolean modifier by name, ignoring case */
  public static Modifier numericByCaselessName(String name) {
    DoubleModifier modifier = DoubleModifier.byCaselessName(name);
    if (modifier != null) {
      return modifier;
    }
    DerivedModifier derived = DerivedModifier.byCaselessName(name);
    if (derived != null) {
      return derived;
    }
    return BitmapModifier.byCaselessName(name);
  }

  private static List<Modifier> allModifiers = null;

  public static List<Modifier> allModifiers() {
    if (allModifiers != null) return allModifiers;
    List<Modifier> mods = new ArrayList<>();
    mods.addAll(Arrays.asList(DoubleModifier.values()));
    mods.addAll(Arrays.asList(BitmapModifier.values()));
    mods.addAll(Arrays.asList(DerivedModifier.values()));
    mods.addAll(Arrays.asList(StringModifier.values()));
    mods.addAll(Arrays.asList(BooleanModifier.values()));
    mods.sort(Comparator.comparing(Modifier::getName));
    allModifiers = mods;
    return mods;
  }

  /** Get any modifier by name, ignoring case */
  public static Modifier byCaselessName(String name) {
    var num = ModifierDatabase.numericByCaselessName(name);
    if (num != null) {
      return num;
    }
    var str = StringModifier.byCaselessName(name);
    if (str != null) {
      return str;
    }
    return BooleanModifier.byCaselessName(name);
  }

  // region: get registered values

  public static final Modifiers getItemModifiers(final int id) {
    if (id <= 0) {
      return null;
    }
    return getModifiers(ModifierType.ITEM, id);
  }

  /**
   * Get item modifiers if the item is to be equipped on Disembodied Hand or Left-Hand Man
   *
   * @param id Item id
   * @return Returns modifiers for item excluding some that just do not apply
   */
  public static final Modifiers getItemModifiersInFamiliarSlot(final int id) {
    Modifiers mods = new Modifiers(getItemModifiers(id));

    mods.setDouble(DoubleModifier.SLIME_HATES_IT, 0.0f);
    mods.setBitmap(BitmapModifier.BRIMSTONE, 0);
    mods.setBitmap(BitmapModifier.CLOATHING, 0);
    mods.setBitmap(BitmapModifier.SYNERGETIC, 0);
    mods.setBoolean(BooleanModifier.MOXIE_MAY_CONTROL_MP, false);
    mods.setBoolean(BooleanModifier.MOXIE_CONTROLS_MP, false);

    return mods;
  }

  public static final Modifiers getEffectModifiers(final int id) {
    if (id <= 0) {
      return null;
    }
    if (KoLCharacter.inGLover()) {
      String effectName = EffectDatabase.getEffectName(id);
      if (!KoLCharacter.hasGs(effectName)) {
        return null;
      }
    }
    return getModifiers(ModifierType.EFFECT, id);
  }

  private static final String getModifierString(final Lookup lookup) {
    return modifierStringsByName.get(lookup.type, lookup.getKey());
  }

  public static final Modifiers getModifiers(final ModifierType type, final int id) {
    return getModifiers(new Lookup(type, id));
  }

  public static final Modifiers getModifiers(final ModifierType type, final String name) {
    return getModifiers(new Lookup(type, name));
  }

  public static final Modifiers getModifiers(final Lookup lookup) {
    ModifierType originalType = null;
    ModifierType type = lookup.type;
    IntOrString key = lookup.getKey();
    if (type == ModifierType.BJORN) {
      originalType = type;
      type = ModifierType.THRONE;
    }

    Modifiers modifiers = modifiersByName.get(type, key);

    if (modifiers == null) {
      String modifierString = getModifierString(new Lookup(type, key));

      if (modifierString == null) {
        return null;
      }

      modifiers = parseModifiers(lookup, modifierString);

      if (originalType != null) {
        modifiers.setLookup(new Lookup(originalType, key));
      }

      modifiers.variable = modifiers.override(lookup);
      modifiers.applyPathModifiers();

      modifiersByName.put(type, key, modifiers);
    }

    if (modifiers.variable) {
      modifiers.override(lookup);
      if (originalType != null) {
        modifiers.setLookup(new Lookup(originalType, key));
      }
    }

    return modifiers;
  }

  public static final double getNumericModifier(
      final ModifierType type, final int id, final Modifier mod) {
    return getNumericModifier(new Lookup(type, id), mod);
  }

  public static final double getNumericModifier(
      final ModifierType type, final String name, final Modifier mod) {
    return getNumericModifier(new Lookup(type, name), mod);
  }

  public static final double getNumericModifier(final Lookup lookup, final Modifier mod) {
    Modifiers mods = getModifiers(lookup);
    if (mods == null) {
      return 0.0;
    }
    return mods.getNumeric(mod);
  }

  public static final double getNumericModifier(final FamiliarData fam, final Modifier mod) {
    return getNumericModifier(fam, mod, fam.getModifiedWeight(false), fam.getItem());
  }

  public static final double getNumericModifier(
      final FamiliarData fam,
      final Modifier mod,
      final int passedWeight,
      final AdventureResult item) {
    int familiarId = fam != null ? fam.getId() : -1;
    if (familiarId == -1) {
      return 0.0;
    }

    // TODO: I'm not keen on this setting a static variable??
    Modifiers.setFamiliar(fam);

    int weight = passedWeight;

    Modifiers tempMods = new Modifiers();

    // Mad Hatrack ... hats do not give their normal modifiers
    // Fancypants Scarecrow ... pants do not give their normal modifiers
    int itemId = item.getItemId();
    ConsumptionType type = ItemDatabase.getConsumptionType(itemId);
    if ((familiarId != FamiliarPool.HATRACK || type != ConsumptionType.HAT)
        && (familiarId != FamiliarPool.SCARECROW || type != ConsumptionType.PANTS)) {
      // Add in all the modifiers bestowed by this item
      tempMods.add(getItemModifiers(itemId));

      // Apply weight modifiers right now
      weight += (int) tempMods.getDouble(DoubleModifier.FAMILIAR_WEIGHT);
      weight += (int) tempMods.getDouble(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT);
      weight += (fam.getFeasted() ? 10 : 0);
      weight += fam.getSoupWeight();
      double percent = tempMods.getDouble(DoubleModifier.FAMILIAR_WEIGHT_PCT) / 100.0;
      if (percent != 0.0) {
        weight = (int) Math.floor(weight + weight * percent);
      }
    }

    tempMods.lookupFamiliarModifiers(fam, weight, item);

    return tempMods.getNumeric(mod);
  }

  public static final boolean getBooleanModifier(
      final ModifierType type, final int id, final BooleanModifier mod) {
    return getBooleanModifier(new Lookup(type, id), mod);
  }

  public static final boolean getBooleanModifier(
      final ModifierType type, final String name, final BooleanModifier mod) {
    return getBooleanModifier(new Lookup(type, name), mod);
  }

  public static final boolean getBooleanModifier(final Lookup lookup, final BooleanModifier mod) {
    Modifiers mods = getModifiers(lookup);
    if (mods == null) {
      return false;
    }
    return mods.getBoolean(mod);
  }

  public static final String getStringModifier(
      final ModifierType type, final int id, final StringModifier mod) {
    return getStringModifier(new Lookup(type, id), mod);
  }

  public static final String getStringModifier(
      final ModifierType type, final String name, final StringModifier mod) {
    return getStringModifier(new Lookup(type, name), mod);
  }

  public static final String getStringModifier(final Lookup lookup, final StringModifier mod) {
    Modifiers mods = getModifiers(lookup);
    if (mods == null) {
      return "";
    }
    return mods.getString(mod);
  }

  // sub-region: parse modifiers.txt expressions to Modifiers / ModifierList

  public static final ModifierList getModifierList(final Lookup lookup) {
    Modifiers mods = ModifierDatabase.getModifiers(lookup);
    if (mods == null) {
      return new ModifierList();
    }

    return splitModifiers(mods.getString(StringModifier.MODIFIERS));
  }

  public static final ModifierList splitModifiers(String modifiers) {
    ModifierList list = new ModifierList();

    // Iterate over string, pulling off modifiers
    while (modifiers != null) {
      // Moxie: +5, Muscle: +5, Mysticality: +5, Familiar Effect: "1xPotato, 3xGhuol, cap 18"
      int comma = modifiers.indexOf(",");
      if (comma != -1) {
        int bracket1 = modifiers.indexOf("[");
        if (bracket1 != -1 && bracket1 < comma) {
          int bracket2 = modifiers.indexOf("]", bracket1 + 1);
          if (bracket2 != -1) {
            comma = modifiers.indexOf(",", bracket2 + 1);
          } else {
            // bogus: no close bracket
            comma = -1;
          }
        } else {
          int quote1 = modifiers.indexOf("\"");
          if (quote1 != -1 && quote1 < comma) {
            int quote2 = modifiers.indexOf("\"", quote1 + 1);
            if (quote2 != -1) {
              comma = modifiers.indexOf(",", quote2 + 1);
            } else {
              // bogus: no close quote
              comma = -1;
            }
          }
        }
      }

      String string;
      if (comma == -1) {
        string = modifiers;
        modifiers = null;
      } else {
        string = modifiers.substring(0, comma).trim();
        modifiers = modifiers.substring(comma + 1).trim();
      }

      String key, value;

      // Every pattern for a modifier with a value separates
      // the key with a colon and a single space. Therefore,
      // split on precisely that string and trim neither the
      // key nor the value.
      int colon = string.indexOf(": ");
      if (colon == -1) {
        key = string;
        value = null;
      } else {
        key = string.substring(0, colon);
        value = string.substring(colon + 2);
      }

      list.addModifier(key, value);
    }

    return list;
  }

  public static final Modifiers evaluatedModifiers(final Lookup lookup, final String modifiers) {
    return new Modifiers(lookup, evaluateModifiers(lookup, modifiers));
  }

  public static final ModifierList evaluateModifiers(final Lookup lookup, final String modifiers) {
    ModifierList list = splitModifiers(modifiers);
    // Nothing to do if no expressions
    if (!modifiers.contains("[")) {
      return list;
    }

    // Otherwise, break apart the string and rebuild it with all
    // expressions evaluated.
    for (ModifierValue modifier : list) {
      // Evaluate the modifier expression
      modifier.eval(lookup);
    }

    return list;
  }

  // sub-region: parse in-game text to Modifiers

  public static final Modifiers parseModifiers(
      final ModifierType type, final int key, final String string) {
    return parseModifiers(new Lookup(type, key), string);
  }

  public static final Modifiers parseModifiers(
      final ModifierType type, final String key, final String string) {
    return parseModifiers(new Lookup(type, key), string);
  }

  public static final Modifiers parseModifiers(final Lookup lookup, final String string) {
    return parseModifiers(lookup, splitModifiers(string));
  }

  public static final Modifiers parseModifiers(final Lookup lookup, final ModifierList list) {
    Modifiers newMods = new Modifiers();

    newMods.setLookup(lookup);

    modLoop:
    for (var modValue : list) {
      var string = modValue.toString();

      for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
        Pattern pattern = mod.getTagPattern();
        if (pattern == null) {
          continue;
        }

        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
          continue;
        }

        if (matcher.group(1) != null) {
          newMods.setDouble(mod, Double.parseDouble(matcher.group(1)));
        } else {
          newMods.addExpression(
              new Indexed<>(mod, ModifierExpression.getInstance(matcher.group(2), lookup)));
        }
        continue modLoop;
      }

      for (var mod : BitmapModifier.BITMAP_MODIFIERS) {
        Pattern pattern = mod.getTagPattern();
        if (pattern == null) {
          continue;
        }

        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
          continue;
        }
        int bitcount = 1;
        if (matcher.groupCount() > 0) {
          bitcount = StringUtilities.parseInt(matcher.group(1));
          if (mod == BitmapModifier.CLOWNINESS) {
            bitcount = bitcount / 25;
          }
        }
        // bitmapMasks stores the next mask we're going to use for modifier mod
        int mask = bitmapMasks.get(mod);
        bitmapMasks.put(mod, mask << bitcount);
        for (int i = 0; i < bitcount - 1; i++) {
          mask |= mask << 1;
        }
        if (bitmapMasks.get(mod) == 0) {
          String message =
              "ERROR: too many sources for bitmap modifier "
                  + mod.getName()
                  + ", consider using longs.";
          KoLmafia.updateDisplay(message);
        }

        newMods.addBitmap(mod, mask);
        continue modLoop;
      }

      for (var mod : BooleanModifier.BOOLEAN_MODIFIERS) {
        Pattern pattern = mod.getTagPattern();
        if (pattern == null) {
          continue;
        }

        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
          continue;
        }

        newMods.setBoolean(mod, true);
        continue modLoop;
      }

      for (var mod : StringModifier.STRING_MODIFIERS) {
        Pattern pattern = mod.getTagPattern();
        if (pattern == null) {
          continue;
        }

        Matcher matcher = pattern.matcher(string);
        if (!matcher.matches()) {
          continue;
        }

        String value = matcher.group(1);

        if (mod == StringModifier.CLASS) {
          value = StringModifier.depluralizeClassName(value);
        }

        newMods.setString(mod, value);
        continue modLoop;
      }
    }
    newMods.setString(StringModifier.MODIFIERS, list.toString());

    return newMods;
  }

  // TODO: what's the difference between these are the above?
  // Parsing item enchantments into KoLmafia modifiers

  public static final String parseSkill(final String text) {
    Matcher matcher = SKILL_PATTERN.matcher(text);
    if (matcher.find()) {
      return StringModifier.SKILL.getTag() + ": \"" + matcher.group(1) + "\"";
    }

    return null;
  }

  public static final String parseDamageReduction(final String text) {
    if (!text.contains("Damage Reduction:")) {
      return null;
    }

    Matcher matcher = DR_PATTERN.matcher(text);
    int dr = 0;

    while (matcher.find()) {
      dr += StringUtilities.parseInt(matcher.group(2));
    }

    return DoubleModifier.DAMAGE_REDUCTION.getTag() + ": " + dr;
  }

  public static final String parseSingleEquip(final String text) {
    Matcher matcher = SINGLE_PATTERN.matcher(text);
    if (matcher.find()) {
      return BooleanModifier.SINGLE.getTag();
    }

    return null;
  }

  public static final String parseSoftcoreOnly(final String text) {
    Matcher matcher = SOFTCORE_PATTERN.matcher(text);
    if (matcher.find()) {
      return BooleanModifier.SOFTCORE.getTag();
    }

    return null;
  }

  public static final String parseDropsItems(final String text) {
    Matcher matcher = ITEM_DROPPER_PATTERN.matcher(text);
    if (matcher.find()) {
      return BooleanModifier.DROPS_ITEMS.getTag();
    }

    return null;
  }

  public static final String parseLastsOneDay(final String text) {
    Matcher matcher = LASTS_ONE_DAY_PATTERN.matcher(text);
    if (matcher.find()) {
      return BooleanModifier.LASTS_ONE_DAY.getTag();
    }

    return null;
  }

  public static final String parseFreePull(final String text) {
    Matcher matcher = FREE_PULL_PATTERN.matcher(text);
    if (matcher.find()) {
      return BooleanModifier.FREE_PULL.getTag();
    }

    return null;
  }

  public static final String parseEffect(final String text) {
    Matcher matcher = EFFECT_PATTERN.matcher(text);
    if (matcher.find()) {
      // matcher.group( 1 ) contains the link to the description
      // matcher.group( 2 ) contains the name.
      // Look up the effect by descid. If it is unknown, we'll just use the name.
      // Otherwise, we may need to disambiguate the name by effectId.
      String name = matcher.group(2).trim();
      int[] effectIds = EffectDatabase.getEffectIds(name, false);
      if (effectIds.length > 1) {
        String descid = DebugDatabase.parseEffectDescid(matcher.group(1));
        int effectId = EffectDatabase.getEffectIdFromDescription(descid);
        if (effectId != -1) {
          name = "[" + effectId + "]" + name;
        }
      }
      return StringModifier.EFFECT.getTag() + ": \"" + name + "\"";
    }

    return null;
  }

  public static final String parseEffectDuration(final String text) {
    Matcher matcher = EFFECT_DURATION_PATTERN.matcher(text);
    if (matcher.find()) {
      return DoubleModifier.EFFECT_DURATION.getTag() + ": " + matcher.group(1);
    }

    return null;
  }

  public static final String parseSongDuration(final String text) {
    Matcher matcher = SONG_DURATION_PATTERN.matcher(text);
    if (matcher.find()) {
      return DoubleModifier.SONG_DURATION.getTag() + ": " + matcher.group(1);
    }

    return null;
  }

  public static final String parseModifier(final String enchantment) {
    String result;

    // Search the double modifiers first

    result = DoubleModifier.parseModifier(enchantment);
    if (result != null) {
      return result;
    }

    // Then the boolean modifiers

    result = BooleanModifier.parseModifier(enchantment);
    if (result != null) {
      return result;
    }

    // Then the string modifiers

    result = StringModifier.parseModifier(enchantment);
    if (result != null) {
      return result;
    }

    // Then the bitmap modifiers

    result = BitmapModifier.parseModifier(enchantment);
    if (result != null) {
      return result;
    }

    // Special handling needed

    Matcher matcher;

    matcher = ALL_ATTR_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String mod = matcher.group(1);
      return MUSCLE + mod + ", " + MYSTICALITY + mod + ", " + MOXIE + mod;
    }

    matcher = ALL_ATTR_PCT_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String mod = matcher.group(1);
      return MUSCLE_PCT + mod + ", " + MYSTICALITY_PCT + mod + ", " + MOXIE_PCT + mod;
    }

    matcher = CLASS_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String plural = matcher.group(1);
      AscensionClass ascensionClass = AscensionClass.findByPlural(plural.replace("&nbsp;", " "));

      if (ascensionClass == null) return null;

      return StringModifier.CLASS.getTag() + ": \"" + ascensionClass.getName() + "\"";
    }

    matcher = COMBAT_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String tag =
          !enchantment.contains("Underwater only")
              ? DoubleModifier.COMBAT_RATE.getTag()
              : "Combat Rate (Underwater)";
      String level = matcher.group(1);
      String rate = COMBAT_RATE_DESCRIPTIONS.getOrDefault(level, "+0");
      return tag + ": " + rate;
    }

    matcher = HP_MP_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String mod = matcher.group(1);
      return HP_TAG + mod + ", " + MP_TAG + mod;
    }

    if (enchantment.contains("Regenerate")) {
      return parseRegeneration(enchantment);
    }

    if (enchantment.contains("Resistance")) {
      return parseResistance(enchantment);
    }

    if (enchantment.contains("Your familiar will always act in combat")) {
      return DoubleModifier.FAMILIAR_ACTION_BONUS.getTag() + ": +100";
    }

    return null;
  }

  public static final String parseStringModifier(final String enchantment) {
    return StringModifier.parseModifier(enchantment);
  }

  private static String parseRegeneration(final String enchantment) {
    Matcher matcher = REGEN_PATTERN.matcher(enchantment);
    if (!matcher.find()) {
      return null;
    }

    String min = matcher.group(1);
    String max = matcher.group(2) == null ? min : matcher.group(2);
    boolean hp = matcher.group(3).equals("HP");
    boolean both = matcher.group(4) != null;

    if (max.isEmpty()) {
      max = min;
    }

    if (both) {
      return HP_REGEN_MIN_TAG
          + min
          + ", "
          + HP_REGEN_MAX_TAG
          + max
          + ", "
          + MP_REGEN_MIN_TAG
          + min
          + ", "
          + MP_REGEN_MAX_TAG
          + max;
    }

    if (hp) {
      return HP_REGEN_MIN_TAG + min + ", " + HP_REGEN_MAX_TAG + max;
    }

    return MP_REGEN_MIN_TAG + min + ", " + MP_REGEN_MAX_TAG + max;
  }

  private static String parseResistanceLevel(final String enchantment) {
    Matcher matcher = RESISTANCE_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      return matcher.group(1);
    } else if (enchantment.contains("Slight")) {
      return "+1";
    } else if (enchantment.contains("So-So")) {
      return "+2";
    } else if (enchantment.contains("Serious")) {
      return "+3";
    } else if (enchantment.contains("Stupendous")) {
      return "+4";
    } else if (enchantment.contains("Superhuman")) {
      return "+5";
    } else if (enchantment.contains("Stunning")) {
      return "+7";
    } else if (enchantment.contains("Sublime")) {
      return "+9";
    }
    return "";
  }

  private static String parseResistance(final String enchantment) {
    String level = parseResistanceLevel(enchantment);
    boolean all = enchantment.contains("All Elements");

    ArrayList<String> mods = new ArrayList<>();

    if (enchantment.contains("Spooky") || all) mods.add(SPOOKY);
    if (enchantment.contains("Stench") || all) mods.add(STENCH);
    if (enchantment.contains("Hot") || all) mods.add(HOT);
    if (enchantment.contains("Cold") || all) mods.add(COLD);
    if (enchantment.contains("Sleaze") || all) mods.add(SLEAZE);
    if (enchantment.contains("Slime")) mods.add(SLIME);
    if (enchantment.contains("Supercold")) mods.add(SUPERCOLD);

    return mods.stream()
        .map(m -> m + level)
        .collect(
            Collectors.collectingAndThen(Collectors.joining(", "), m -> m.isEmpty() ? null : m));
  }

  // region: override / register / re-register modifiers

  public static final void overrideModifier(
      final ModifierType type, final int key, final String value) {
    overrideModifierInternal(new Lookup(type, key), value);
  }

  public static final void overrideModifier(
      final ModifierType type, final String key, final String value) {
    overrideModifierInternal(new Lookup(type, key), value);
  }

  public static final void overrideModifier(
      final ModifierType type, final int key, final Modifiers value) {
    overrideModifierInternal(new Lookup(type, key), value);
  }

  public static final void overrideModifier(
      final ModifierType type, final String key, final Modifiers value) {
    overrideModifierInternal(new Lookup(type, key), value);
  }

  private static final void overrideModifierInternal(final Lookup lookup, final String value) {
    overrideModifierInternal(lookup, parseModifiers(lookup, value));
  }

  private static final void overrideModifierInternal(final Lookup lookup, final Modifiers value) {
    if (!modifierStringsByName.containsKey(lookup.type, lookup.getKey())
        && !(lookup.type == ModifierType.GENERATED)) {
      RequestLogger.updateSessionLog("WARNING: updated modifier not in modifiers.txt: " + lookup);
      modifierStringsByName.put(lookup.type, lookup.getKey(), value.toString());
    }
    modifiersByName.put(lookup.type, lookup.getKey(), value);
  }

  public static final void overrideRemoveModifier(final ModifierType type, final int key) {
    overrideRemoveModifierInternal(new Lookup(type, key));
  }

  public static final void overrideRemoveModifier(final ModifierType type, final String key) {
    overrideRemoveModifierInternal(new Lookup(type, key));
  }

  private static final void overrideRemoveModifierInternal(final Lookup lookup) {
    modifiersByName.remove(lookup.type, lookup.getKey());
  }

  public static final void updateItem(final int itemId, final String known) {
    overrideModifier(ModifierType.ITEM, itemId, known);
  }

  public static final void registerItem(
      final String name, final String text, final ConsumptionType type) {
    // Examine the item description and decide what it is.
    ArrayList<String> unknown = new ArrayList<>();
    String known = DebugDatabase.parseItemEnchantments(text, unknown, type);
    DebugDatabase.parseRestores(name, text);
    registerObject(ModifierType.ITEM, name, unknown, known);
  }

  public static final void registerEffect(final String name, final String text) {
    // Examine the effect description and decide what it is.
    ArrayList<String> unknown = new ArrayList<>();
    String known = DebugDatabase.parseEffectEnchantments(text, unknown);
    registerObject(ModifierType.EFFECT, name, unknown, known);
  }

  public static final void registerSkill(final String name, final String text) {
    // Examine the effect description and decide what it is.
    ArrayList<String> unknown = new ArrayList<>();
    String known = DebugDatabase.parseSkillEnchantments(text, unknown);
    registerObject(ModifierType.SKILL, name, unknown, known);
  }

  public static final void registerOutfit(final String name, final String text) {
    // Examine the outfit description and decide what it is.
    ArrayList<String> unknown = new ArrayList<>();
    String known = DebugDatabase.parseOutfitEnchantments(text, unknown);
    registerObject(ModifierType.OUTFIT, name, unknown, known);
  }

  private static void registerObject(
      final ModifierType type,
      final String name,
      final ArrayList<String> unknown,
      final String known) {
    for (String value : unknown) {
      String printMe = modifierCommentString(name, value);
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }

    if (known.isEmpty()) {
      if (unknown.size() == 0) {
        String printMe = modifierCommentString(name);
        RequestLogger.printLine(printMe);
        RequestLogger.updateSessionLog(printMe);
      }
    } else {
      String printMe = modifierString(type, name, known);
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);

      Lookup lookup = new Lookup(type, name);
      modifierStringsByName.putIfAbsent(lookup.type, lookup.getKey(), known);
    }
  }

  // region: write to modifiers.txt

  public static void writeModifiers(final File output) {
    // Open the output file
    PrintStream writer = LogStream.openStream(output, true);
    RequestLogger.printLine("Writing data override: " + output);
    writeModifiers(writer);
  }

  public static void writeModifiers(final PrintStream writer) {
    // One map per equipment category
    Set<String> hats = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> weapons = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> offhands = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> shirts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> pants = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> accessories = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> containers = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> famitems = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> sixguns = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> bedazzlements = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> cards = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> folders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> freepulls = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> potions = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    Set<String> wikiname = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

    // Iterate over all items and assign item id to category
    for (Entry<Integer, String> entry : ItemDatabase.dataNameEntrySet()) {
      Integer key = entry.getKey();
      String name = entry.getValue();
      ConsumptionType type = ItemDatabase.getConsumptionType(key);

      switch (type) {
        case HAT -> hats.add(name);
        case PANTS -> pants.add(name);
        case SHIRT -> shirts.add(name);
        case WEAPON -> weapons.add(name);
        case OFFHAND -> offhands.add(name);
        case ACCESSORY -> accessories.add(name);
        case CONTAINER -> containers.add(name);
        case FAMILIAR_EQUIPMENT -> famitems.add(name);
        case SIXGUN -> sixguns.add(name);
        case STICKER -> bedazzlements.add(name);
        case CARD -> cards.add(name);
        case FOLDER -> folders.add(name);
        default -> {
          Modifiers mods = getModifiers(ModifierType.ITEM, name);
          if (mods == null) {
            break;
          }
          if (!mods.getString(StringModifier.EFFECT).isEmpty()) {
            potions.add(name);
          } else if (mods.getBoolean(BooleanModifier.FREE_PULL)) {
            freepulls.add(name);
          } else if (!mods.getString(StringModifier.WIKI_NAME).isEmpty()) {
            wikiname.add(name);
          }
        }
      }
    }

    // Make a map of familiars
    Set<String> familiars = new TreeSet<>();
    familiars.add("Familiar:(none)");

    for (Entry<Integer, String> entry : FamiliarDatabase.entrySet()) {
      String name = entry.getValue();
      if (getModifiers(ModifierType.FAMILIAR, name) != null) {
        familiars.add(name);
      }
    }

    // Make a map of campground items
    Set<String> campground = new TreeSet<>();

    for (var itemId : CampgroundRequest.campgroundItems) {
      String name = ItemDatabase.getItemDataName(itemId);
      // Sanity check: if the user has an old override file
      // which we didn't delete for some reason, we may have
      // an unknown item on the list of campground items.
      if (name == null) {
        KoLmafia.updateDisplay(
            "Campground item #"
                + itemId
                + " not found in data file. Do 'update clear' to remove stale override!");
      }
      // Skip toilet paper, since we want that in the free
      // pull section
      else if (itemId != ItemPool.TOILET_PAPER) {
        campground.add(name);
      }
    }

    // Make a map of status effects
    Set<String> effects = new TreeSet<>();

    for (Entry<Integer, String> entry : EffectDatabase.entrySet()) {
      String name = entry.getValue();
      // Skip effect which is also an item
      effects.add(name);
    }

    // Make a map of passive skills
    Set<String> passives = new TreeSet<>();

    for (Entry<Integer, String> entry : SkillDatabase.entrySet()) {
      Integer key = entry.getKey();
      String name = entry.getValue();
      if (SkillDatabase.isPassive(key)) {
        passives.add(name);
      }
    }

    // Make a map of outfits
    Set<String> outfits = new TreeSet<>();

    for (var outfit : EquipmentDatabase.normalOutfits.values()) {
      if (outfit != null) {
        outfits.add(outfit.getName());
      }
    }

    // Make a map of zodiac signs
    Set<String> zodiacs = new TreeSet<>();

    for (ZodiacSign sign : ZodiacSign.standardZodiacSigns) {
      zodiacs.add(sign.getName());
    }

    // Make a map of event days. This could eventually come from an enum.
    Set<String> events =
        Set.of(
            "Muscle Day",
            "Mysticality Day",
            "Moxie Day",
            "Festival of Jarlsberg",
            "Lab&oacute;r Day Eve");

    // Make a map of zones
    Set<String> zones = new TreeSet<>();

    for (String name : AdventureDatabase.ZONE_DESCRIPTIONS.keySet()) {
      if (getModifiers(ModifierType.ZONE, name) != null) {
        zones.add(name);
      }
    }

    // Make a map of locations
    Set<String> locations = new TreeSet<>();

    for (KoLAdventure key : AdventureDatabase.getAsLockableListModel()) {
      String name = key.getAdventureName();
      if (getModifiers(ModifierType.LOC, name) != null) {
        locations.add(name);
      }
    }

    // Make a map of synergies
    Set<String> synergies = new TreeSet<>();

    for (Entry<String, Integer> entry : ModifierDatabase.synergies.entrySet()) {
      String name = entry.getKey();
      int mask = entry.getValue();
      synergies.add(name);
    }

    // Make a map of mutexes

    Set<String> mutexes = new TreeSet<>(ModifierDatabase.mutexes);

    // Make a map of maximization categories
    int maximizationCount = Maximizer.maximizationCategories.length;

    Set<String> maximization =
        new TreeSet<>(
            Arrays.asList(Maximizer.maximizationCategories).subList(0, maximizationCount));

    writer.println(KoLConstants.MODIFIERS_VERSION);

    // For each equipment category, write the map entries
    writeModifierCategory(writer, hats, ModifierType.ITEM, "Hats");
    writer.println();
    writeModifierCategory(writer, pants, ModifierType.ITEM, "Pants");
    writer.println();
    writeModifierCategory(writer, shirts, ModifierType.ITEM, "Shirts");
    writer.println();
    writeModifierCategory(writer, weapons, ModifierType.ITEM, "Weapons");
    writer.println();
    writeModifierCategory(writer, offhands, ModifierType.ITEM, "Off-hand");
    writer.println();
    writeModifierCategory(writer, accessories, ModifierType.ITEM, "Accessories");
    writer.println();
    writeModifierCategory(writer, containers, ModifierType.ITEM, "Containers");
    writer.println();
    writeModifierCategory(writer, famitems, ModifierType.ITEM, "Familiar Items");
    writer.println();
    writeModifierCategory(writer, sixguns, ModifierType.ITEM, "Sixguns");
    writer.println();
    writeModifierCategory(writer, familiars, ModifierType.FAMILIAR, "Familiars");
    writer.println();
    writeModifierCategory(writer, bedazzlements, ModifierType.ITEM, "Bedazzlements");
    writer.println();
    writeModifierCategory(writer, cards, ModifierType.ITEM, "Alice's Army");
    writer.println();
    writeModifierCategory(writer, folders, ModifierType.ITEM, "Folder");
    writer.println();
    writeModifierCategory(writer, campground, ModifierType.CAMPGROUND, "Campground equipment");
    writer.println();
    writeModifierCategory(writer, effects, ModifierType.EFFECT, "Status Effects");
    writer.println();
    writeModifierCategory(writer, passives, ModifierType.SKILL, "Passive Skills");
    writer.println();
    writeModifierCategory(writer, outfits, ModifierType.OUTFIT, "Outfits");
    writer.println();
    writeModifierCategory(writer, zodiacs, ModifierType.SIGN, "Zodiac Sign");
    writer.println();
    writeModifierCategory(writer, events, ModifierType.EVENT, "Event");
    writer.println();
    writeModifierCategory(writer, zones, ModifierType.ZONE, "Zone-specific");
    writer.println();
    writeModifierCategory(writer, locations, ModifierType.LOC, "Location-specific");
    writer.println();
    writeModifierCategory(writer, synergies, ModifierType.SYNERGY, "Synergies");
    writer.println();
    writeModifierCategory(writer, mutexes, ModifierType.MUTEX_I, "Mutually exclusive items");
    writer.println();
    writeModifierCategory(writer, mutexes, ModifierType.MUTEX_E, "Mutually exclusive effects");
    writer.println();
    writeModifierCategory(writer, maximization, ModifierType.MAX_CAT, "Maximization categories");
    writer.println();
    writeModifierCategory(writer, potions, ModifierType.ITEM, "Everything Else");
    writeModifierCategory(writer, freepulls, ModifierType.ITEM);
    writeModifierCategory(writer, wikiname, ModifierType.ITEM);

    writer.close();
  }

  private static void writeModifierCategory(
      final PrintStream writer, final Set<String> set, final ModifierType type, final String tag) {
    writer.println("# " + tag + " section of modifiers.txt");
    writeModifierCategory(writer, set, type);
  }

  private static void writeModifierCategory(
      final PrintStream writer, final Set<String> set, final ModifierType type) {
    writer.println();

    for (String name : set) {
      String modifierString = getModifierString(new Lookup(type, name));
      writeModifierItem(writer, type, name, modifierString);
    }
  }

  public static void writeModifierItem(
      final PrintStream writer, final ModifierType type, final String name, String modifierString) {
    if (modifierString == null) {
      writeModifierComment(writer, name);
      return;
    }

    writeModifierString(writer, type, name, modifierString);
  }

  public static void writeModifierString(
      final PrintStream writer,
      final ModifierType type,
      final String name,
      final String modifiers) {
    writer.println(modifierString(type, name, modifiers));
  }

  public static String modifierString(
      final ModifierType type, final String name, final String modifiers) {
    return type.pascalCaseName() + "\t" + name + "\t" + modifiers;
  }

  public static String modifierCommentString(final String name, final String value) {
    return "# " + name + ": " + value;
  }

  public static void writeModifierComment(
      final PrintStream writer, final String name, final String value) {
    writer.println(modifierCommentString(name, value));
  }

  public static String modifierCommentString(final String name) {
    return "# " + name;
  }

  public static void writeModifierComment(final PrintStream writer, final String name) {
    writer.println(modifierCommentString(name));
  }

  // region: verify modifiers.txt

  public static final void checkModifiers() {
    for (Entry<ModifierType, Map<IntOrString, String>> typeEntry :
        modifierStringsByName.entrySet()) {
      ModifierType type = typeEntry.getKey();
      for (Entry<IntOrString, String> entry : typeEntry.getValue().entrySet()) {
        IntOrString key = entry.getKey();
        String modifierString = entry.getValue();

        if (modifierString == null) {
          RequestLogger.printLine("Key \"" + type + ":" + key + "\" has no modifiers");
          continue;
        }

        Modifiers modifiers = modifiersByName.get(type, key);
        if (modifiers != null) {
          modifierString = modifiers.getString(StringModifier.MODIFIERS);
        }

        ModifierList list = splitModifiers(modifierString);

        for (ModifierValue modifier : list) {
          String mod = modifier.toString();

          if (DoubleModifier.byTagPattern(mod) != null) {
            continue;
          }
          if (BitmapModifier.byTagPattern(mod) != null) {
            continue;
          }
          if (BooleanModifier.byTagPattern(mod) != null) {
            continue;
          }
          if (StringModifier.byTagPattern(mod) != null) {
            continue;
          }
          if (type == ModifierType.FAM_EQ) {
            continue; // these may contain freeform text
          }
          RequestLogger.printLine(
              "Key \"" + type + ":" + key + "\" has unknown modifier: \"" + mod + "\"");
        }
      }
    }
  }

  // region: initial load of modifiers.txt

  public static void loadAllModifiers() {
    try (BufferedReader reader =
        FileUtilities.getVersionedReader("modifiers.txt", KoLConstants.MODIFIERS_VERSION)) {
      String[] data;

      loop:
      while ((data = FileUtilities.readData(reader)) != null) {
        if (data.length != 3) {
          continue;
        }

        String typeString = data[0];
        String name = data[1];
        String modifiers = data[2];

        ModifierType type = ModifierType.fromString(typeString);
        if (type == null) {
          throw new RuntimeException("Bad modifier type " + typeString);
        }

        Lookup lookup = new Lookup(type, name);

        if (modifierStringsByName.put(type, lookup.getKey(), modifiers) != null) {
          KoLmafia.updateDisplay("Duplicate modifiers for: " + type + ":" + name);
        }

        Matcher matcher = FAMILIAR_EFFECT_PATTERN.matcher(modifiers);
        if (matcher.find()) {
          String effect = matcher.group(1);
          familiarEffectByName.put(name, effect);
          matcher = FAMILIAR_EFFECT_TRANSLATE_PATTERN.matcher(effect);
          if (matcher.find()) {
            effect = matcher.replaceAll(FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT);
          }
          matcher = FAMILIAR_EFFECT_TRANSLATE_PATTERN2.matcher(effect);
          if (matcher.find()) {
            effect = matcher.replaceAll(FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT2);
          }
          modifierStringsByName.put(ModifierType.FAM_EQ, new IntOrString(name), effect);
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private static void computeSynergies() {
    synergies.clear();
    for (IntOrString key : modifierStringsByName.getAll(ModifierType.SYNERGY).keySet()) {
      if (!key.isString()) continue;
      String name = key.getStringValue();

      String[] pieces = name.split("/");
      if (pieces.length < 2) {
        KoLmafia.updateDisplay(name + " contain less than 2 elements.");
        continue;
      }
      int mask = 0;
      for (String piece : pieces) {
        Modifiers mods = getModifiers(ModifierType.ITEM, piece);
        if (mods == null) {
          KoLmafia.updateDisplay(name + " contains element " + piece + " with no modifiers.");
          return;
        }
        int emask = mods.getRawBitmap(BitmapModifier.SYNERGETIC);
        if (emask == 0) {
          KoLmafia.updateDisplay(name + " contains element " + piece + " that isn't Synergetic.");
          return;
        }
        mask |= emask;
      }
      synergies.put(name, mask);
    }
  }

  private static void computeMutex(ModifierType type, String name) {
    String[] pieces = name.split("/");
    if (pieces.length < 2) {
      KoLmafia.updateDisplay(name + " contain less than 2 elements.");
      return;
    }
    int bit = 1 << mutexes.size();
    for (String piece : pieces) {
      Modifiers mods =
          switch (type) {
            case MUTEX_I -> getModifiers(ModifierType.ITEM, piece);
            case MUTEX_E -> getModifiers(ModifierType.EFFECT, piece);
            default -> null;
          };
      if (mods == null) {
        KoLmafia.updateDisplay(name + " contains element " + piece + " with no modifiers.");
        return;
      }
      mods.addBitmap(BitmapModifier.MUTEX, bit);
    }
    mutexes.add(name);
  }

  private static void computeMutexes() {
    mutexes.clear();
    for (ModifierType type : List.of(ModifierType.MUTEX_E, ModifierType.MUTEX_I)) {
      for (IntOrString key : modifierStringsByName.getAll(type).keySet()) {
        if (!key.isString()) continue;
        String name = key.getStringValue();
        computeMutex(type, name);
      }
    }
  }

  public static void resetModifiers() {
    // Don't reset any variables that are set up by loadAllModifiers, as subsequent calls to
    // resetModifiers then won't set them back up due to the if() guarding loadAllModifiers.
    modifiersByName.clear();
    Modifiers.resetAvailablePassiveSkills();
    for (var mod : BitmapModifier.BITMAP_MODIFIERS) {
      bitmapMasks.put(mod, 1);
    }

    if (modifierStringsByName.size() == 0) {
      loadAllModifiers();
    }

    computeSynergies();
    computeMutexes();
  }
}
