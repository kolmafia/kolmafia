package net.sourceforge.kolmafia;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import net.sourceforge.kolmafia.KoLConstants.ConsumptionType;
import net.sourceforge.kolmafia.VYKEACompanionData.VYKEACompanionType;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifierCollection;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.DateTimeManager;
import net.sourceforge.kolmafia.persistence.DebugDatabase;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.persistence.EquipmentDatabase;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase.Element;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest.Companion;
import net.sourceforge.kolmafia.request.ClanLoungeRequest;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.FloristRequest.Florist;
import net.sourceforge.kolmafia.request.UseSkillRequest;
import net.sourceforge.kolmafia.session.AutumnatonManager;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import net.sourceforge.kolmafia.utilities.LogStream;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.utilities.TwoLevelEnumHashMap;

public class Modifiers {
  private static final TwoLevelEnumHashMap<ModifierType, IntOrString, String>
      modifierStringsByName = new TwoLevelEnumHashMap<>(ModifierType.class);
  private static final TwoLevelEnumHashMap<ModifierType, IntOrString, Modifiers> modifiersByName =
      new TwoLevelEnumHashMap<>(ModifierType.class);
  private static final Map<String, String> familiarEffectByName = new HashMap<>();
  private static final Map<String, DoubleModifier> modifierTypesByName = new HashMap<>();
  private static final Map<String, Integer> modifierIndicesByName = new HashMap<>();
  private static boolean availableSkillsChanged = false;
  private static final Map<Boolean, List<Modifiers>> availablePassiveSkillModifiersByVariable =
      new TreeMap<>();
  private static Modifiers cachedPassiveModifiers = null;
  private static final Map<String, Integer> synergies = new HashMap<>();
  private static final List<String> mutexes = new ArrayList<>();
  private static final Map<String, Set<String>> uniques = new HashMap<>();
  public static String currentLocation = "";
  public static String currentZone = "";
  public static String currentEnvironment = "";
  public static double currentML = 4.0;
  public static String currentFamiliar = "";
  public static String mainhandClass = "";
  public static double hoboPower = 0.0;
  public static double smithsness = 0.0;
  public static double currentWeight = 0.0;
  public static boolean unarmed = false;

  private static final Pattern FAMILIAR_EFFECT_PATTERN =
      Pattern.compile("Familiar Effect: \"(.*?)\"");
  private static final Pattern FAMILIAR_EFFECT_TRANSLATE_PATTERN =
      Pattern.compile("([\\d.]+)\\s*x\\s*(Volley|Somb|Lep|Fairy)");
  private static final String FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT = "$2: $1 ";
  private static final Pattern FAMILIAR_EFFECT_TRANSLATE_PATTERN2 =
      Pattern.compile("cap ([\\d.]+)");
  private static final String FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT2 = "Familiar Weight Cap: $1 ";

  public static final String EXPR = "(?:([-+]?[\\d.]+)|\\[([^]]+)\\])";

  private static final HashSet<String> numericModifiers = new HashSet<>();

  public static final EnumSet<DoubleModifier> DOUBLE_MODIFIERS =
      EnumSet.allOf(DoubleModifier.class);

  static {
    for (var modifier : Modifiers.DOUBLE_MODIFIERS) {
      modifierTypesByName.put(modifier.getName(), modifier);
      String tag = modifier.getTag();
      modifierTypesByName.put(tag, modifier);
      numericModifiers.add(tag);
    }
  }

  public static boolean isNumericModifier(final String key) {
    return Modifiers.numericModifiers.contains(key);
  }

  public static final int BOOLEANS = 0;
  public static final int BRIMSTONE = 1;
  public static final int CLOATHING = 2;
  public static final int SYNERGETIC = 3;
  public static final int RAVEOSITY = 4;
  public static final int MUTEX = 5;
  public static final int MUTEX_VIOLATIONS = 6;

  private static final BitmapModifier[] bitmapModifiers = {
    new BitmapModifier("(booleans)", null),
    new BitmapModifier("Brimstone", Pattern.compile("Brimstone")),
    new BitmapModifier("Cloathing", Pattern.compile("Cloathing")),
    new BitmapModifier("Synergetic", Pattern.compile("Synergetic")),
    new BitmapModifier("Raveosity", Pattern.compile("Raveosity: (\\+?\\d+)")),
    new BitmapModifier("Mutually Exclusive", null),
    new BitmapModifier("Mutex Violations", null),
  };

  public static final int BITMAP_MODIFIERS = Modifiers.bitmapModifiers.length;
  private static final int[] bitmapMasks = new int[BITMAP_MODIFIERS];

  static {
    Arrays.fill(bitmapMasks, 1);

    for (int i = 0; i < BITMAP_MODIFIERS; ++i) {
      BitmapModifier modifier = Modifiers.bitmapModifiers[i];
      int index = i;
      modifierIndicesByName.put(modifier.getName(), index);
      modifierIndicesByName.put(modifier.getTag(), index);
    }
  }

  public static final int SOFTCORE = 0;
  public static final int SINGLE = 1;
  public static final int NEVER_FUMBLE = 2;
  public static final int WEAKENS = 3;
  public static final int FREE_PULL = 4;
  public static final int VARIABLE = 5;
  public static final int NONSTACKABLE_WATCH = 6;
  public static final int COLD_IMMUNITY = 7;
  public static final int HOT_IMMUNITY = 8;
  public static final int SLEAZE_IMMUNITY = 9;
  public static final int SPOOKY_IMMUNITY = 10;
  public static final int STENCH_IMMUNITY = 11;
  public static final int COLD_VULNERABILITY = 12;
  public static final int HOT_VULNERABILITY = 13;
  public static final int SLEAZE_VULNERABILITY = 14;
  public static final int SPOOKY_VULNERABILITY = 15;
  public static final int STENCH_VULNERABILITY = 16;
  public static final int MOXIE_CONTROLS_MP = 17;
  public static final int MOXIE_MAY_CONTROL_MP = 18;
  public static final int FOUR_SONGS = 19;
  public static final int ADVENTURE_UNDERWATER = 20;
  public static final int UNDERWATER_FAMILIAR = 21;
  public static final int GENERIC = 22;
  public static final int UNARMED = 23;
  public static final int NOPULL = 24;
  public static final int LASTS_ONE_DAY = 25;
  public static final int ATTACKS_CANT_MISS = 26;
  public static final int LOOK_LIKE_A_PIRATE = 27;
  public static final int BREAKABLE = 28;
  public static final int DROPS_ITEMS = 29;
  public static final int DROPS_MEAT = 30;

  private static final BooleanModifier[] booleanModifiers = {
    new BooleanModifier(
        "Softcore Only",
        Pattern.compile("This item cannot be equipped while in Hardcore"),
        Pattern.compile("Softcore Only")),
    new BooleanModifier("Single Equip", Pattern.compile("Single Equip")),
    new BooleanModifier(
        "Never Fumble", Pattern.compile("Never Fumble"), Pattern.compile("Never Fumble")),
    new BooleanModifier(
        "Weakens Monster",
        Pattern.compile("Successful hit weakens opponent"),
        Pattern.compile("Weakens Monster")),
    new BooleanModifier("Free Pull", Pattern.compile("Free Pull")),
    new BooleanModifier("Variable", Pattern.compile("Variable")),
    new BooleanModifier("Nonstackable Watch", Pattern.compile("Nonstackable Watch")),
    new BooleanModifier("Cold Immunity", Pattern.compile("Cold Immunity")),
    new BooleanModifier("Hot Immunity", Pattern.compile("Hot Immunity")),
    new BooleanModifier("Sleaze Immunity", Pattern.compile("Sleaze Immunity")),
    new BooleanModifier("Spooky Immunity", Pattern.compile("Spooky Immunity")),
    new BooleanModifier("Stench Immunity", Pattern.compile("Stench Immunity")),
    new BooleanModifier("Cold Vulnerability", Pattern.compile("Cold Vulnerability")),
    new BooleanModifier("Hot Vulnerability", Pattern.compile("Hot Vulnerability")),
    new BooleanModifier("Sleaze Vulnerability", Pattern.compile("Sleaze Vulnerability")),
    new BooleanModifier("Spooky Vulnerability", Pattern.compile("Spooky Vulnerability")),
    new BooleanModifier("Stench Vulnerability", Pattern.compile("Stench Vulnerability")),
    new BooleanModifier("Moxie Controls MP", Pattern.compile("Moxie Controls MP")),
    new BooleanModifier("Moxie May Control MP", Pattern.compile("Moxie May Control MP")),
    new BooleanModifier(
        "Four Songs",
        Pattern.compile("Allows you to keep 4 songs in your head instead of 3"),
        Pattern.compile("Four Songs")),
    new BooleanModifier(
        "Adventure Underwater",
        Pattern.compile("Lets you [bB]reathe [uU]nderwater"),
        Pattern.compile("Adventure Underwater")),
    new BooleanModifier(
        "Underwater Familiar",
        Pattern.compile("Lets your Familiar Breathe Underwater"),
        Pattern.compile("Underwater Familiar")),
    new BooleanModifier("Generic", Pattern.compile("Generic")),
    new BooleanModifier(
        "Unarmed",
        Pattern.compile("Bonus&nbsp;for&nbsp;Unarmed&nbsp;Characters&nbsp;only"),
        Pattern.compile("Unarmed")),
    new BooleanModifier("No Pull", Pattern.compile("No Pull")),
    new BooleanModifier(
        "Lasts Until Rollover",
        Pattern.compile("This item will disappear at the end of the day"),
        Pattern.compile("Lasts Until Rollover")),
    new BooleanModifier(
        "Attacks Can't Miss",
        new Pattern[] {
          Pattern.compile("Regular Attacks Can't Miss"), Pattern.compile("Cannot miss")
        },
        Pattern.compile("Attacks Can't Miss")),
    new BooleanModifier("Pirate", Pattern.compile("Look like a Pirate")),
    new BooleanModifier("Breakable", Pattern.compile("Breakable")),
    new BooleanModifier("Drops Items", Pattern.compile("Drops Items")),
    new BooleanModifier("Drops Meat", Pattern.compile("Drops Meat")),
  };

  public static final int BOOLEAN_MODIFIERS = Modifiers.booleanModifiers.length;

  static {
    if (BOOLEAN_MODIFIERS > 32) {
      KoLmafia.updateDisplay(
          "Too many boolean modifiers to fit into bitmaps[0].  Will have to store bitmaps as longs, or use two bitmaps to hold the booleans.");
    }
    for (int i = 0; i < BOOLEAN_MODIFIERS; ++i) {
      BooleanModifier modifier = Modifiers.booleanModifiers[i];
      int index = BITMAP_MODIFIERS + i;
      modifierIndicesByName.put(modifier.getName(), index);
      modifierIndicesByName.put(modifier.getTag(), index);
    }
  }

  public static final int CLASS = 0;
  public static final int INTRINSIC_EFFECT = 1;
  public static final int EQUALIZE = 2;
  public static final int WIKI_NAME = 3;
  public static final int MODIFIERS = 4;
  public static final int OUTFIT = 5;
  public static final int STAT_TUNING = 6;
  public static final int EFFECT = 7;
  public static final int EQUIPS_ON = 8;
  public static final int FAMILIAR_EFFECT = 9;
  public static final int JIGGLE = 10;
  public static final int EQUALIZE_MUSCLE = 11;
  public static final int EQUALIZE_MYST = 12;
  public static final int EQUALIZE_MOXIE = 13;
  public static final int AVATAR = 14;
  public static final int ROLLOVER_EFFECT = 15;
  public static final int SKILL = 16;
  public static final int FLOOR_BUFFED_MUSCLE = 17;
  public static final int FLOOR_BUFFED_MYST = 18;
  public static final int FLOOR_BUFFED_MOXIE = 19;
  public static final int PLUMBER_STAT = 20;
  public static final int RECIPE = 21;

  private static final StringModifier[] stringModifiers = {
    new StringModifier(
        "Class",
        new Pattern[] {
          Pattern.compile("Only (.*?) may use this item"),
          Pattern.compile("Bonus for (.*?) only"),
          Pattern.compile("Bonus&nbsp;for&nbsp;(.*?)&nbsp;only"),
        },
        Pattern.compile("Class: \"(.*?)\"")),
    new StringModifier(
        "Intrinsic Effect",
        Pattern.compile("Intrinsic Effect: <a.*?><font color=blue>(.*)</font></a>"),
        Pattern.compile("Intrinsic Effect: \"(.*?)\"")),
    new StringModifier("Equalize", Pattern.compile("Equalize: \"(.*?)\"")),
    new StringModifier("Wiki Name", Pattern.compile("Wiki Name: \"(.*?)\"")),
    new StringModifier("Modifiers", Pattern.compile("^(none)$")),
    new StringModifier("Outfit", null),
    new StringModifier("Stat Tuning", Pattern.compile("Stat Tuning: \"(.*?)\"")),
    new StringModifier("Effect", Pattern.compile("(?:^|, )Effect: \"(.*?)\"")),
    new StringModifier("Equips On", Pattern.compile("Equips On: \"(.*?)\"")),
    new StringModifier("Familiar Effect", Pattern.compile("Familiar Effect: \"(.*?)\"")),
    new StringModifier(
        "Jiggle", Pattern.compile("Jiggle: *(.*?)$"), Pattern.compile("Jiggle: \"(.*?)\"")),
    new StringModifier("Equalize Muscle", Pattern.compile("Equalize Muscle: \"(.*?)\"")),
    new StringModifier("Equalize Mysticality", Pattern.compile("Equalize Mysticality: \"(.*?)\"")),
    new StringModifier("Equalize Moxie", Pattern.compile("Equalize Moxie: \"(.*?)\"")),
    new StringModifier(
        "Avatar",
        new Pattern[] {
          Pattern.compile("Makes you look like (?:a |an |the )?(.++)(?<!doctor|gross doctor)"),
          Pattern.compile("Te hace ver como un (.++)"),
        },
        Pattern.compile("Avatar: \"(.*?)\"")),
    new StringModifier(
        "Rollover Effect",
        Pattern.compile("Adventures of <b><a.*?>(.*)</a></b> at Rollover"),
        Pattern.compile("Rollover Effect: \"(.*?)\"")),
    new StringModifier(
        "Skill",
        Pattern.compile("Grants Skill:.*?<b>(.*?)</b>"),
        Pattern.compile("Skill: \"(.*?)\"")),
    new StringModifier("Floor Buffed Muscle", Pattern.compile("Floor Buffed Muscle: \"(.*?)\"")),
    new StringModifier(
        "Floor Buffed Mysticality", Pattern.compile("Floor Buffed Mysticality: \"(.*?)\"")),
    new StringModifier("Floor Buffed Moxie", Pattern.compile("Floor Buffed Moxie: \"(.*?)\"")),
    new StringModifier("Plumber Stat", Pattern.compile("Plumber Stat: \"(.*?)\"")),
    new StringModifier("Recipe", Pattern.compile("Recipe: \"(.*?)\"")),
  };

  public static final int STRING_MODIFIERS = Modifiers.stringModifiers.length;

  static {
    for (int i = 0; i < STRING_MODIFIERS; ++i) {
      StringModifier modifier = Modifiers.stringModifiers[i];
      int index = BITMAP_MODIFIERS + BOOLEAN_MODIFIERS + i;
      modifierIndicesByName.put(modifier.getName(), index);
      modifierIndicesByName.put(modifier.getTag(), index);
    }
  }

  // Indexes for array returned by predict():
  public static final int BUFFED_MUS = 0;
  public static final int BUFFED_MYS = 1;
  public static final int BUFFED_MOX = 2;
  public static final int BUFFED_HP = 3;
  public static final int BUFFED_MP = 4;

  private static final DerivedModifier[] derivedModifiers = {
    new DerivedModifier("Buffed Muscle"),
    new DerivedModifier("Buffed Mysticality"),
    new DerivedModifier("Buffed Moxie"),
    new DerivedModifier("Buffed HP Maximum"),
    new DerivedModifier("Buffed MP Maximum"),
  };

  public static final int DERIVED_MODIFIERS = Modifiers.derivedModifiers.length;

  public int[] predict() {
    int[] rv = new int[Modifiers.DERIVED_MODIFIERS];

    int mus = KoLCharacter.getBaseMuscle();
    int mys = KoLCharacter.getBaseMysticality();
    int mox = KoLCharacter.getBaseMoxie();

    String equalize = this.getString(Modifiers.EQUALIZE);
    if (equalize.startsWith("Mus")) {
      mys = mox = mus;
    } else if (equalize.startsWith("Mys")) {
      mus = mox = mys;
    } else if (equalize.startsWith("Mox")) {
      mus = mys = mox;
    } else if (equalize.startsWith("High")) {
      int high = Math.max(Math.max(mus, mys), mox);
      mus = mys = mox = high;
    }

    String mus_equalize = this.getString(Modifiers.EQUALIZE_MUSCLE);
    if (mus_equalize.startsWith("Mys")) {
      mus = mys;
    } else if (mus_equalize.startsWith("Mox")) {
      mus = mox;
    }
    String mys_equalize = this.getString(Modifiers.EQUALIZE_MYST);
    if (mys_equalize.startsWith("Mus")) {
      mys = mus;
    } else if (mys_equalize.startsWith("Mox")) {
      mys = mox;
    }
    String mox_equalize = this.getString(Modifiers.EQUALIZE_MOXIE);
    if (mox_equalize.startsWith("Mus")) {
      mox = mus;
    } else if (mox_equalize.startsWith("Mys")) {
      mox = mys;
    }

    int mus_limit = (int) this.get(DoubleModifier.MUS_LIMIT);
    if (mus_limit > 0 && mus > mus_limit) {
      mus = mus_limit;
    }
    int mys_limit = (int) this.get(DoubleModifier.MYS_LIMIT);
    if (mys_limit > 0 && mys > mys_limit) {
      mys = mys_limit;
    }
    int mox_limit = (int) this.get(DoubleModifier.MOX_LIMIT);
    if (mox_limit > 0 && mox > mox_limit) {
      mox = mox_limit;
    }

    rv[Modifiers.BUFFED_MUS] =
        mus
            + (int) this.get(DoubleModifier.MUS)
            + (int) Math.ceil(this.get(DoubleModifier.MUS_PCT) * mus / 100.0);
    rv[Modifiers.BUFFED_MYS] =
        mys
            + (int) this.get(DoubleModifier.MYS)
            + (int) Math.ceil(this.get(DoubleModifier.MYS_PCT) * mys / 100.0);
    rv[Modifiers.BUFFED_MOX] =
        mox
            + (int) this.get(DoubleModifier.MOX)
            + (int) Math.ceil(this.get(DoubleModifier.MOX_PCT) * mox / 100.0);

    String mus_buffed_floor = this.getString(Modifiers.FLOOR_BUFFED_MUSCLE);
    if (mus_buffed_floor.startsWith("Mys")) {
      if (rv[Modifiers.BUFFED_MYS] > rv[Modifiers.BUFFED_MUS]) {
        rv[Modifiers.BUFFED_MUS] = rv[Modifiers.BUFFED_MYS];
      }
    } else if (mus_buffed_floor.startsWith("Mox")) {
      if (rv[Modifiers.BUFFED_MOX] > rv[Modifiers.BUFFED_MUS]) {
        rv[Modifiers.BUFFED_MUS] = rv[Modifiers.BUFFED_MOX];
      }
    }
    String mys_buffed_floor = this.getString(Modifiers.FLOOR_BUFFED_MYST);
    if (mys_buffed_floor.startsWith("Mus")) {
      if (rv[Modifiers.BUFFED_MUS] > rv[Modifiers.BUFFED_MYS]) {
        rv[Modifiers.BUFFED_MYS] = rv[Modifiers.BUFFED_MUS];
      }
    } else if (mys_buffed_floor.startsWith("Mox")) {
      if (rv[Modifiers.BUFFED_MOX] > rv[Modifiers.BUFFED_MYS]) {
        rv[Modifiers.BUFFED_MYS] = rv[Modifiers.BUFFED_MOX];
      }
    }
    String mox_buffed_floor = this.getString(Modifiers.FLOOR_BUFFED_MOXIE);
    if (mox_buffed_floor.startsWith("Mus")) {
      if (rv[Modifiers.BUFFED_MUS] > rv[Modifiers.BUFFED_MOX]) {
        rv[Modifiers.BUFFED_MOX] = rv[Modifiers.BUFFED_MUS];
      }
    } else if (mox_buffed_floor.startsWith("Mys")) {
      if (rv[Modifiers.BUFFED_MYS] > rv[Modifiers.BUFFED_MOX]) {
        rv[Modifiers.BUFFED_MOX] = rv[Modifiers.BUFFED_MYS];
      }
    }

    int hpbase;
    int hp;
    int buffedHP;
    if (KoLCharacter.isVampyre()) {
      hpbase = KoLCharacter.getBaseMuscle();
      hp = hpbase + (int) this.get(DoubleModifier.HP);
      buffedHP = Math.max(hp, mus);
    } else if (KoLCharacter.inRobocore()) {
      hpbase = 30;
      hp = hpbase + (int) this.get(DoubleModifier.HP);
      buffedHP = hp;
    } else if (KoLCharacter.isGreyGoo()) {
      hpbase =
          (int) KoLCharacter.getBaseMaxHP()
              - (int) KoLCharacter.currentNumericModifier(DoubleModifier.HP);
      hp = hpbase + (int) this.get(DoubleModifier.HP);
      buffedHP = hp;
    } else {
      hpbase = rv[Modifiers.BUFFED_MUS] + 3;
      double C = KoLCharacter.isMuscleClass() ? 1.5 : 1.0;
      double hpPercent = this.get(DoubleModifier.HP_PCT);
      hp = (int) Math.ceil(hpbase * (C + hpPercent / 100.0)) + (int) this.get(DoubleModifier.HP);
      buffedHP = Math.max(hp, mus);
    }
    rv[Modifiers.BUFFED_HP] = buffedHP;

    int mpbase;
    int mp;
    int buffedMP;
    if (KoLCharacter.isGreyGoo()) {
      mpbase =
          (int) KoLCharacter.getBaseMaxMP()
              - (int) KoLCharacter.currentNumericModifier(DoubleModifier.MP);
      mp = mpbase + (int) this.get(DoubleModifier.MP);
      buffedMP = mp;
    } else {
      mpbase = rv[Modifiers.BUFFED_MYS];
      if (this.getBoolean(Modifiers.MOXIE_CONTROLS_MP)
          || (this.getBoolean(Modifiers.MOXIE_MAY_CONTROL_MP)
              && rv[Modifiers.BUFFED_MOX] > mpbase)) {
        mpbase = rv[Modifiers.BUFFED_MOX];
      }
      double C = KoLCharacter.isMysticalityClass() ? 1.5 : 1.0;
      double mpPercent = this.get(DoubleModifier.MP_PCT);
      mp = (int) Math.ceil(mpbase * (C + mpPercent / 100.0)) + (int) this.get(DoubleModifier.MP);
      buffedMP = Math.max(mp, mys);
    }
    rv[Modifiers.BUFFED_MP] = buffedMP;

    return rv;
  }

  public static final Collection<Entry<IntOrString, String>> getAllModifiersOfType(
      final ModifierType type) {
    return Modifiers.modifierStringsByName.getAll(type).entrySet();
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
    overrideModifierInternal(lookup, Modifiers.parseModifiers(lookup, value));
  }

  private static final void overrideModifierInternal(final Lookup lookup, final Modifiers value) {
    Modifiers.modifiersByName.put(lookup.type, lookup.getKey(), value);
  }

  public static final void overrideRemoveModifier(final ModifierType type, final int key) {
    overrideRemoveModifierInternal(new Lookup(type, key));
  }

  public static final void overrideRemoveModifier(final ModifierType type, final String key) {
    overrideRemoveModifierInternal(new Lookup(type, key));
  }

  private static final void overrideRemoveModifierInternal(final Lookup lookup) {
    Modifiers.modifiersByName.remove(lookup.type, lookup.getKey());
  }

  public static final String getModifierName(final DoubleModifier modifier) {
    return modifier.getName();
  }

  public static final String getBitmapModifierName(final int index) {
    return Modifiers.bitmapModifiers[index].getName();
  }

  public static final String getBooleanModifierName(final int index) {
    return Modifiers.booleanModifiers[index].getName();
  }

  public static final String getStringModifierName(final int index) {
    return Modifiers.stringModifiers[index].getName();
  }

  public static final String getDerivedModifierName(final int index) {
    return Modifiers.derivedModifiers[index].getName();
  }

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

  public static List<AdventureResult> getPotentialChanges(final DoubleModifier modifier) {
    ArrayList<AdventureResult> available = new ArrayList<>();

    for (Entry<IntOrString, String> entry : Modifiers.getAllModifiersOfType(ModifierType.EFFECT)) {
      IntOrString key = entry.getKey();
      if (!key.isString()) continue;
      String effectName = key.getStringValue();
      int effectId = EffectDatabase.getEffectId(effectName);

      if (effectId == -1) {
        continue;
      }

      Modifiers currentTest = Modifiers.getEffectModifiers(effectId);
      double value = currentTest.get(modifier);

      if (value == 0.0) {
        continue;
      }

      AdventureResult currentEffect = EffectPool.get(effectId);
      boolean hasEffect = KoLConstants.activeEffects.contains(currentEffect);

      if (value > 0.0 && !hasEffect) {
        available.add(currentEffect);
      } else if (value < 0.0 && hasEffect) {
        available.add(currentEffect);
      }
    }

    return available;
  }

  private static <T extends net.sourceforge.kolmafia.modifiers.Modifier> int findName(
      final T[] table, final String name) {
    for (int i = 0; i < table.length; ++i) {
      if (name.equalsIgnoreCase(table[i].getName())) {
        return i;
      }
    }
    return -1;
  }

  public static final DoubleModifier findName(String name) {
    // TODO: does this need to be caseless? Can it not use modifierTypesByName,
    // which is case-sensitive and includes tags?
    return DoubleModifier.byCaselessName(name);
  }

  public static final int findBooleanName(String name) {
    return Modifiers.findName(Modifiers.booleanModifiers, name);
  }

  private Lookup originalLookup;
  // Assume modifiers are variable until proven otherwise.
  public boolean variable = true;
  private final DoubleModifierCollection doubles = new DoubleModifierCollection();
  private final int[] bitmaps = new int[Modifiers.BITMAP_MODIFIERS];
  private final String[] strings = new String[Modifiers.STRING_MODIFIERS];
  private ArrayList<Indexed<ModifierExpression>> expressions = null;
  // These are used for Steely-Eyed Squint and so on
  private final DoubleModifierCollection doublerAccumulators = new DoubleModifierCollection();

  public Modifiers() {
    Arrays.fill(this.strings, "");
    // Everything else should be initialized above.
  }

  public Modifiers(Modifiers copy) {
    this();
    this.set(copy);
  }

  public Modifiers(Lookup lookup) {
    this();
    this.originalLookup = lookup;
  }

  public Modifiers(Lookup lookup, ModifierList mods) {
    this(lookup);
    mods.forEach(this::setModifier);
  }

  public Lookup getLookup() {
    return this.originalLookup;
  }

  public final void reset() {
    this.doubles.reset();
    Arrays.fill(this.bitmaps, 0);
    Arrays.fill(this.strings, "");
    this.expressions = null;
  }

  private double derivePrismaticDamage() {
    double damage = this.doubles.get(DoubleModifier.COLD_DAMAGE);
    damage = Math.min(damage, this.doubles.get(DoubleModifier.HOT_DAMAGE));
    damage = Math.min(damage, this.doubles.get(DoubleModifier.SLEAZE_DAMAGE));
    damage = Math.min(damage, this.doubles.get(DoubleModifier.SPOOKY_DAMAGE));
    damage = Math.min(damage, this.doubles.get(DoubleModifier.STENCH_DAMAGE));
    this.setDouble(DoubleModifier.PRISMATIC_DAMAGE, damage);
    return damage;
  }

  private double cappedCombatRate() {
    // Combat Rate has diminishing returns beyond + or - 25%
    double rate = this.doubles.get(DoubleModifier.COMBAT_RATE);
    if (rate > 25.0) {
      double extra = rate - 25.0;
      return 25.0 + Math.floor(extra / 5.0);
    }
    if (rate < -25.0) {
      double extra = rate + 25.0;
      return -25.0 + Math.ceil(extra / 5.0);
    }
    return rate;
  }

  public double get(final DoubleModifier modifier) {
    if (modifier == DoubleModifier.PRISMATIC_DAMAGE) {
      return this.derivePrismaticDamage();
    }
    if (modifier == DoubleModifier.COMBAT_RATE) {
      return this.cappedCombatRate();
    }

    if (modifier == null) {
      return 0.0;
    }

    return this.doubles.get(modifier);
  }

  public double get(final String name) {
    if (name.equalsIgnoreCase("Prismatic Damage")) {
      return this.derivePrismaticDamage();
    }
    if (name.equalsIgnoreCase("Combat Rate")) {
      return this.cappedCombatRate();
    }

    DoubleModifier modifier = Modifiers.findName(name);
    if (modifier == null) {
      int index = Modifiers.findName(Modifiers.derivedModifiers, name);
      if (index < 0 || index >= Modifiers.DERIVED_MODIFIERS) {
        return this.getBitmap(name);
      }
      return this.predict()[index];
    }

    return this.doubles.get(modifier);
  }

  public int getRawBitmap(final int index) {
    if (index < 0 || index >= this.bitmaps.length) {
      return 0;
    }

    return this.bitmaps[index];
  }

  public int getRawBitmap(final String name) {
    int index = Modifiers.findName(Modifiers.bitmapModifiers, name);
    if (index < 0 || index >= this.bitmaps.length) {
      return 0;
    }

    return this.bitmaps[index];
  }

  public int getBitmap(final int index) {
    if (index < 0 || index >= this.bitmaps.length) {
      return 0;
    }

    int n = this.bitmaps[index];
    // Count the bits:
    if (n == 0) return 0;
    n = ((n & 0xAAAAAAAA) >>> 1) + (n & 0x55555555);
    n = ((n & 0xCCCCCCCC) >>> 2) + (n & 0x33333333);
    n = ((n & 0xF0F0F0F0) >>> 4) + (n & 0x0F0F0F0F);
    n = ((n & 0xFF00FF00) >>> 8) + (n & 0x00FF00FF);
    n = ((n & 0xFFFF0000) >>> 16) + (n & 0x0000FFFF);
    return n;
  }

  public int getBitmap(final String name) {
    return this.getBitmap(Modifiers.findName(Modifiers.bitmapModifiers, name));
  }

  public boolean getBoolean(final int index) {
    if (index < 0 || index >= Modifiers.BOOLEAN_MODIFIERS) {
      return false;
    }

    return ((this.bitmaps[0] >>> index) & 1) != 0;
  }

  public boolean getBoolean(final String name) {
    int index = Modifiers.findName(Modifiers.booleanModifiers, name);
    if (index < 0 || index >= Modifiers.BOOLEAN_MODIFIERS) {
      return false;
    }

    return ((this.bitmaps[0] >>> index) & 1) != 0;
  }

  public String getString(final int index) {
    if (index < 0 || index >= this.strings.length) {
      return "";
    }

    return this.strings[index];
  }

  public String getString(final String name) {
    // Can't cache this as expressions can be dependent on things
    // that can change within a session, like character level.
    if (name.equals("Evaluated Modifiers")) {
      return Modifiers.evaluateModifiers(this.originalLookup, this.strings[Modifiers.MODIFIERS])
          .toString();
    }

    int index = Modifiers.findName(Modifiers.stringModifiers, name);
    if (index < 0 || index >= this.strings.length) {
      return "";
    }

    return this.strings[index];
  }

  public double getDoublerAccumulator(final DoubleModifier modifier) {
    if (modifier == null) {
      return -9999.0;
    }
    return this.doublerAccumulators.get(modifier);
  }

  public double getDoublerAccumulator(final String name) {
    // doublerAccumulators uses the same indexes as doubles, so the same lookup will work
    DoubleModifier modifier = findName(name);
    if (modifier == null) {
      // For now, make it obvious that something went wrong
      return -9999.0;
    }

    return this.doublerAccumulators.get(modifier);
  }

  public boolean setDouble(final DoubleModifier mod, final double value) {
    if (mod == null) {
      return false;
    }

    return this.doubles.set(mod, value);
  }

  public boolean setBitmap(final int index, final int mod) {
    if (index < 0 || index >= this.bitmaps.length) {
      return false;
    }

    if (this.bitmaps[index] != mod) {
      this.bitmaps[index] = mod;
      return true;
    }
    return false;
  }

  public boolean setBoolean(final int index, final boolean mod) {
    if (index < 0 || index >= Modifiers.BOOLEAN_MODIFIERS) {
      return false;
    }

    int mask = 1 << index;
    int val = mod ? mask : 0;
    if ((this.bitmaps[0] & mask) != val) {
      this.bitmaps[0] ^= mask;
      return true;
    }
    return false;
  }

  public boolean setString(final int index, String mod) {
    if (index < 0 || index >= this.strings.length) {
      return false;
    }

    if (mod == null) {
      mod = "";
    }

    if (!mod.equals(this.strings[index])) {
      this.strings[index] = mod;
      return true;
    }
    return false;
  }

  public boolean set(final Modifiers mods) {
    if (mods == null) {
      return false;
    }

    boolean changed = false;
    this.originalLookup = mods.originalLookup;

    for (var mod : Modifiers.DOUBLE_MODIFIERS) {
      changed |= this.setDouble(mod, mods.doubles.get(mod));
    }

    int[] copyBitmaps = mods.bitmaps;
    for (int index = 0; index < this.bitmaps.length; ++index) {
      if (this.bitmaps[index] != copyBitmaps[index]) {
        this.bitmaps[index] = copyBitmaps[index];
        changed = true;
      }
    }

    String[] copyStrings = mods.strings;
    for (int index = 0; index < this.strings.length; ++index) {
      if (!this.strings[index].equals(copyStrings[index])) {
        this.strings[index] = copyStrings[index];
        changed = true;
      }
    }

    return changed;
  }

  private static final Set<ModifierType> doubledBySquintChampagne =
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
          ModifierType.UNBREAKABLE_UMBRELLA);

  public void addDouble(
      final DoubleModifier mod, final double value, final ModifierType type, final int key) {
    addDouble(mod, value, type, new IntOrString(key));
  }

  public void addDouble(
      final DoubleModifier mod, final double value, final ModifierType type, final String key) {
    addDouble(mod, value, type, new IntOrString(key));
  }

  public void addDouble(final DoubleModifier mod, final double value, final Lookup lookup) {
    addDouble(mod, value, lookup.type, lookup.getKey());
  }

  protected void addDouble(
      final DoubleModifier mod,
      final double value,
      final ModifierType type,
      final IntOrString key) {
    switch (mod) {
      case MANA_COST:
        // Total Mana Cost reduction cannot exceed 3
        if (this.doubles.add(mod, value) < -3) {
          this.doubles.set(mod, -3);
        }
        break;
      case FAMILIAR_WEIGHT_PCT:
        // TODO: this seems extremely fragile. Also, as the mod is negative is this right?
        // The three current sources of -wt% do not stack
        if (this.doubles.get(mod) > value) {
          this.doubles.set(mod, value);
        }
        break;
      case MUS_LIMIT:
      case MYS_LIMIT:
      case MOX_LIMIT:
        // Only the lowest limiter applies
        double current = this.doubles.get(mod);
        if ((current == 0.0 || current > value) && value > 0.0) {
          this.doubles.set(mod, value);
        }
        break;
      case ITEMDROP:
        if (Modifiers.doubledBySquintChampagne.contains(type)) {
          this.doublerAccumulators.add(mod, value);
        }
        this.doubles.add(mod, value);
        break;
      case INITIATIVE:
      case HOT_DAMAGE:
      case COLD_DAMAGE:
      case STENCH_DAMAGE:
      case SPOOKY_DAMAGE:
      case SLEAZE_DAMAGE:
      case HOT_SPELL_DAMAGE:
      case COLD_SPELL_DAMAGE:
      case STENCH_SPELL_DAMAGE:
      case SPOOKY_SPELL_DAMAGE:
      case SLEAZE_SPELL_DAMAGE:
      case EXPERIENCE:
      case MUS_EXPERIENCE:
      case MYS_EXPERIENCE:
      case MOX_EXPERIENCE:
      case MUS_EXPERIENCE_PCT:
      case MYS_EXPERIENCE_PCT:
      case MOX_EXPERIENCE_PCT:
        // doublerAccumulators acts as an accumulator for modifiers that are possibly doubled by
        // doublers like makeshift garbage shirt, Bendin' Hell, Bow-Legged Swagger, or Dirty Pear.
        // TODO: Figure out which ones aren't doubled and exclude them. BoomBox?
        this.doublerAccumulators.add(mod, value);
        this.doubles.add(mod, value);
        break;
      case FAMILIAR_ACTION_BONUS:
        this.doubles.set(mod, Math.min(100, this.get(mod) + value));
        break;
      default:
        this.doubles.add(mod, value);
        break;
    }
  }

  public void add(final Modifiers mods) {
    if (mods == null) {
      return;
    }

    // Make sure the modifiers apply to current class
    String className = mods.strings[Modifiers.CLASS];
    if (className != null && !className.isEmpty()) {
      AscensionClass ascensionClass = AscensionClass.findByExactName(className);
      if (ascensionClass != null && ascensionClass != KoLCharacter.getAscensionClass()) {
        return;
      }
    }

    // Unarmed modifiers apply only if the character has no weapon or offhand
    boolean unarmed = mods.getBoolean(Modifiers.UNARMED);
    if (unarmed && !Modifiers.unarmed) {
      return;
    }

    Lookup lookup = mods.originalLookup;

    // Add in the double modifiers

    mods.doubles.forEach(
        (i, addition) -> {
          if (i != DoubleModifier.ADVENTURES
              || (mods.bitmaps[0] & this.bitmaps[0] & (1 << Modifiers.NONSTACKABLE_WATCH)) == 0) {
            this.addDouble(i, addition, lookup);
          }
        });

    // Add in string modifiers as appropriate.

    String val;
    val = mods.strings[Modifiers.EQUALIZE];
    if (!val.isEmpty() && this.strings[Modifiers.EQUALIZE].isEmpty()) {
      this.strings[Modifiers.EQUALIZE] = val;
    }
    val = mods.strings[Modifiers.INTRINSIC_EFFECT];
    if (!val.isEmpty()) {
      String prev = this.strings[INTRINSIC_EFFECT];
      if (prev.isEmpty()) {
        this.strings[Modifiers.INTRINSIC_EFFECT] = val;
      } else {
        this.strings[Modifiers.INTRINSIC_EFFECT] = prev + "\t" + val;
      }
    }
    val = mods.strings[Modifiers.STAT_TUNING];
    if (!val.isEmpty()) {
      this.strings[Modifiers.STAT_TUNING] = val;
    }
    val = mods.strings[Modifiers.EQUALIZE_MUSCLE];
    if (!val.isEmpty()) {
      this.strings[Modifiers.EQUALIZE_MUSCLE] = val;
    }
    val = mods.strings[Modifiers.EQUALIZE_MYST];
    if (!val.isEmpty()) {
      this.strings[Modifiers.EQUALIZE_MYST] = val;
    }
    val = mods.strings[Modifiers.EQUALIZE_MOXIE];
    if (!val.isEmpty()) {
      this.strings[Modifiers.EQUALIZE_MOXIE] = val;
    }

    // OR in the bitmap modifiers (including all the boolean modifiers)
    this.bitmaps[Modifiers.MUTEX_VIOLATIONS] |=
        this.bitmaps[Modifiers.MUTEX] & mods.bitmaps[Modifiers.MUTEX];
    for (int i = 0; i < this.bitmaps.length; ++i) {
      this.bitmaps[i] |= mods.bitmaps[i];
    }
  }

  public boolean setModifier(final Modifier mod) {
    if (mod == null) {
      return false;
    }

    DoubleModifier modifier = modifierTypesByName.get(mod.getName());
    if (modifier != null) {
      return this.setDouble(modifier, Double.parseDouble(mod.getValue()));
    }
    Integer index = modifierIndicesByName.get(mod.getName());
    if (index == null) {
      return false;
    }

    if (index < BITMAP_MODIFIERS) {
      return this.setBitmap(index, Integer.parseInt(mod.getValue()));
    }

    index -= BITMAP_MODIFIERS;
    if (index < BOOLEAN_MODIFIERS) {
      return this.setBoolean(index, mod.getValue().equals("true"));
    }

    index -= BOOLEAN_MODIFIERS;
    return this.setString(index, mod.getValue());
  }

  public static final Modifiers getItemModifiers(final int id) {
    if (id <= 0) {
      return null;
    }
    return Modifiers.getModifiers(ModifierType.ITEM, id);
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
    mods.setBitmap(Modifiers.BRIMSTONE, 0);
    mods.setBitmap(Modifiers.CLOATHING, 0);
    mods.setBitmap(Modifiers.SYNERGETIC, 0);
    mods.setBoolean(Modifiers.MOXIE_MAY_CONTROL_MP, false);
    mods.setBoolean(Modifiers.MOXIE_CONTROLS_MP, false);

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
    return Modifiers.getModifiers(ModifierType.EFFECT, id);
  }

  private static final String getModifierString(final Lookup lookup) {
    return Modifiers.modifierStringsByName.get(lookup.type, lookup.getKey());
  }

  public static final Modifiers getModifiers(final ModifierType type, final int id) {
    return Modifiers.getModifiers(new Lookup(type, id));
  }

  public static final Modifiers getModifiers(final ModifierType type, final String name) {
    return Modifiers.getModifiers(new Lookup(type, name));
  }

  public static final Modifiers getModifiers(final Lookup lookup) {
    ModifierType originalType = null;
    ModifierType type = lookup.type;
    IntOrString key = lookup.getKey();
    if (type == ModifierType.BJORN) {
      originalType = type;
      type = ModifierType.THRONE;
    }

    Modifiers modifiers = Modifiers.modifiersByName.get(type, key);

    if (modifiers == null) {
      String modifierString = Modifiers.getModifierString(new Lookup(type, key));

      if (modifierString == null) {
        return null;
      }

      modifiers = Modifiers.parseModifiers(lookup, modifierString);

      if (originalType != null) {
        modifiers.originalLookup = new Lookup(originalType, key);
      }

      modifiers.variable = modifiers.override(lookup);

      Modifiers.modifiersByName.put(type, key, modifiers);
    }

    if (modifiers.variable) {
      modifiers.override(lookup);
      if (originalType != null) {
        modifiers.originalLookup = new Lookup(originalType, key);
      }
    }

    return modifiers;
  }

  public static final Modifiers parseModifiers(
      final ModifierType type, final int key, final String string) {
    return parseModifiers(new Lookup(type, key), string);
  }

  public static final Modifiers parseModifiers(
      final ModifierType type, final String key, final String string) {
    return parseModifiers(new Lookup(type, key), string);
  }

  public static final Modifiers parseModifiers(final Lookup lookup, final String string) {
    Modifiers newMods = new Modifiers();
    int[] newBitmaps = newMods.bitmaps;
    String[] newStrings = newMods.strings;

    newMods.originalLookup = lookup;

    for (var mod : Modifiers.DOUBLE_MODIFIERS) {
      Pattern pattern = mod.getTagPattern();
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(string);
      if (!matcher.find()) {
        continue;
      }

      if (matcher.group(1) != null) {
        newMods.setDouble(mod, Double.parseDouble(matcher.group(1)));
      } else {
        if (newMods.expressions == null) {
          newMods.expressions = new ArrayList<>();
        }
        newMods.expressions.add(
            new Indexed<>(mod, ModifierExpression.getInstance(matcher.group(2), lookup)));
      }
    }

    for (int i = 0; i < newBitmaps.length; ++i) {
      Pattern pattern = Modifiers.bitmapModifiers[i].getTagPattern();
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(string);
      if (!matcher.find()) {
        continue;
      }
      int bitcount = 1;
      if (matcher.groupCount() > 0) {
        bitcount = StringUtilities.parseInt(matcher.group(1));
      }
      int mask = Modifiers.bitmapMasks[i];
      switch (bitcount) {
        case 1 -> Modifiers.bitmapMasks[i] <<= 1;
        case 2 -> {
          mask |= mask << 1;
          Modifiers.bitmapMasks[i] <<= 2;
        }
        default -> {
          KoLmafia.updateDisplay(
              "ERROR: invalid count for bitmap modifier in " + lookup.toString());
          continue;
        }
      }
      if (Modifiers.bitmapMasks[i] == 0) {
        KoLmafia.updateDisplay(
            "ERROR: too many sources for bitmap modifier "
                + Modifiers.bitmapModifiers[i].getName()
                + ", consider using longs.");
      }

      newBitmaps[i] |= mask;
    }

    for (int i = 0; i < Modifiers.BOOLEAN_MODIFIERS; ++i) {
      Pattern pattern = Modifiers.booleanModifiers[i].getTagPattern();
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(string);
      if (!matcher.find()) {
        continue;
      }

      newBitmaps[0] |= 1 << i;
    }

    for (int i = 0; i < newStrings.length; ++i) {
      Pattern pattern = Modifiers.stringModifiers[i].getTagPattern();
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(string);
      if (!matcher.find()) {
        continue;
      }

      String modifierName = Modifiers.stringModifiers[i].getName();
      String value = matcher.group(1);

      if (modifierName.equals("Class")) {
        value = Modifiers.depluralizeClassName(value);
      }

      newStrings[i] = value;
    }

    newStrings[Modifiers.MODIFIERS] = string;

    return newMods;
  }

  private static final String[][] classStrings = {
    {
      AscensionClass.SEAL_CLUBBER.getName(), "Seal Clubbers", "Seal&nbsp;Clubbers",
    },
    {
      AscensionClass.TURTLE_TAMER.getName(), "Turtle Tamers", "Turtle&nbsp;Tamers",
    },
    {
      AscensionClass.PASTAMANCER.getName(), "Pastamancers",
    },
    {
      AscensionClass.SAUCEROR.getName(), "Saucerors",
    },
    {
      AscensionClass.DISCO_BANDIT.getName(), "Disco Bandits", "Disco&nbsp;Bandits",
    },
    {
      AscensionClass.ACCORDION_THIEF.getName(), "Accordion Thieves", "Accordion&nbsp;Thieves",
    },
  };

  private static String depluralizeClassName(final String string) {
    for (String[] results : Modifiers.classStrings) {
      String result = results[0];
      for (String candidate : results) {
        if (candidate.equals(string)) {
          return result;
        }
      }
    }
    return string;
  }

  public static class Modifier {
    private final String name;
    private String value;

    public Modifier(final String name, final String value) {
      this.name = name;
      this.value = value;
    }

    public String getName() {
      return this.name;
    }

    public String getValue() {
      return this.value;
    }

    public void setValue(final String value) {
      this.value = value;
    }

    public void eval(final Lookup lookup) {
      if (this.value == null) {
        return;
      }

      int lb = this.value.indexOf("[");
      if (lb == -1) {
        return;
      }

      int rb = this.value.indexOf("]");
      if (rb == -1) {
        return;
      }

      ModifierExpression expr = new ModifierExpression(this.value.substring(lb + 1, rb), lookup);
      if (expr.hasErrors()) {
        return;
      }

      int val = (int) expr.eval();
      this.value = (val > 0 ? "+" : "") + val;
    }

    public void toString(final StringBuilder buffer) {
      buffer.append(name);
      if (value != null) {
        buffer.append(": ");
        buffer.append(value);
      }
    }

    @Override
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      this.toString(buffer);
      return buffer.toString();
    }
  }

  public static class ModifierList implements Iterable<Modifier> {
    private final LinkedList<Modifier> list;

    public ModifierList() {
      this.list = new LinkedList<>();
    }

    @Override
    public Iterator<Modifier> iterator() {
      return this.list.iterator();
    }

    public Stream<Modifier> stream() {
      return StreamSupport.stream(spliterator(), false);
    }

    public void clear() {
      this.list.clear();
    }

    public int size() {
      return this.list.size();
    }

    public void addAll(final ModifierList list) {
      this.list.addAll(list.list);
    }

    public void addModifier(final Modifier modifier) {
      this.list.add(modifier);
    }

    public void addModifier(final String name, final String value) {
      this.list.add(new Modifier(name, value));
    }

    public void addToModifier(final Modifier modifier) {
      String name = modifier.getName();
      String current = this.getModifierValue(name);
      if (current == null) {
        this.list.add(modifier);
      } else {
        // We can only add to numeric values
        String value = modifier.getValue();
        if (StringUtilities.isNumeric(current) && StringUtilities.isNumeric(value)) {
          int newValue = Integer.parseInt(current) + Integer.parseInt(value);
          this.removeModifier(name);
          this.list.add(new Modifier(name, String.valueOf(newValue)));
        }
      }
    }

    public void addToModifier(final String name, final String value) {
      String current = this.getModifierValue(name);
      if (current == null) {
        this.list.add(new Modifier(name, value));
      } else {
        // We can only add to numeric values
        if (StringUtilities.isNumeric(current) && StringUtilities.isNumeric(value)) {
          int newValue = Integer.parseInt(current) + Integer.parseInt(value);
          this.removeModifier(name);
          this.list.add(new Modifier(name, String.valueOf(newValue)));
        }
      }
    }

    public boolean containsModifier(final String name) {
      for (Modifier modifier : this.list) {
        if (name.equals(modifier.name)) {
          return true;
        }
      }
      return false;
    }

    public String getModifierValue(final String name) {
      for (Modifier modifier : this.list) {
        if (name.equals(modifier.name)) {
          return modifier.value;
        }
      }
      return null;
    }

    public Modifier removeModifier(final String name) {
      Iterator<Modifier> iterator = this.iterator();
      while (iterator.hasNext()) {
        Modifier modifier = iterator.next();
        if (name.equals(modifier.name)) {
          iterator.remove();
          return modifier;
        }
      }
      return null;
    }

    @Override
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      for (Modifier modifier : this.list) {
        if (buffer.length() > 0) {
          buffer.append(", ");
        }

        modifier.toString(buffer);
      }
      return buffer.toString();
    }
  }

  public static final ModifierList getModifierList(final Lookup lookup) {
    Modifiers mods = Modifiers.getModifiers(lookup);
    if (mods == null) {
      return new ModifierList();
    }

    return Modifiers.splitModifiers(mods.getString("Modifiers"));
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
    ModifierList list = Modifiers.splitModifiers(modifiers);
    // Nothing to do if no expressions
    if (!modifiers.contains("[")) {
      return list;
    }

    // Otherwise, break apart the string and rebuild it with all
    // expressions evaluated.
    for (Modifier modifier : list) {
      // Evaluate the modifier expression
      modifier.eval(lookup);
    }

    return list;
  }

  public static final String trimModifiers(final String modifiers, final String remove) {
    ModifierList list = Modifiers.splitModifiers(modifiers);
    list.removeModifier(remove);
    return list.toString();
  }

  private boolean overrideItem(final int itemId) {
    switch (itemId) {
      case ItemPool.TUESDAYS_RUBY -> {
        // Set modifiers depending on what KoL day of the week it is
        var dotw = DateTimeManager.getArizonaDateTime().getDayOfWeek();

        this.setDouble(DoubleModifier.MEATDROP, dotw == DayOfWeek.SUNDAY ? 5.0 : 0.0);
        this.setDouble(DoubleModifier.MUS_PCT, dotw == DayOfWeek.MONDAY ? 5.0 : 0.0);
        this.setDouble(DoubleModifier.MP_REGEN_MIN, dotw == DayOfWeek.TUESDAY ? 3.0 : 0.0);
        this.setDouble(DoubleModifier.MP_REGEN_MAX, dotw == DayOfWeek.TUESDAY ? 7.0 : 0.0);
        this.setDouble(DoubleModifier.MYS_PCT, dotw == DayOfWeek.WEDNESDAY ? 5.0 : 0.0);
        this.setDouble(DoubleModifier.ITEMDROP, dotw == DayOfWeek.THURSDAY ? 5.0 : 0.0);
        this.setDouble(DoubleModifier.MOX_PCT, dotw == DayOfWeek.FRIDAY ? 5.0 : 0.0);
        this.setDouble(DoubleModifier.HP_REGEN_MIN, dotw == DayOfWeek.SATURDAY ? 3.0 : 0.0);
        this.setDouble(DoubleModifier.HP_REGEN_MAX, dotw == DayOfWeek.SATURDAY ? 7.0 : 0.0);
        return true;
      }
      case ItemPool.PANTSGIVING -> {
        this.setBoolean(Modifiers.DROPS_ITEMS, Preferences.getInteger("_pantsgivingCrumbs") < 10);
        return true;
      }
      case ItemPool.PATRIOT_SHIELD -> {
        // Muscle classes
        this.setDouble(DoubleModifier.HP_REGEN_MIN, 0.0);
        this.setDouble(DoubleModifier.HP_REGEN_MAX, 0.0);
        // Seal clubber
        this.setDouble(DoubleModifier.WEAPON_DAMAGE, 0.0);
        this.setDouble(DoubleModifier.DAMAGE_REDUCTION, 0.0);
        // Turtle Tamer
        this.setDouble(DoubleModifier.FAMILIAR_WEIGHT, 0.0);
        // Disco Bandit
        this.setDouble(DoubleModifier.RANGED_DAMAGE, 0.0);
        // Accordion Thief
        this.setBoolean(Modifiers.FOUR_SONGS, false);
        // Mysticality classes
        this.setDouble(DoubleModifier.MP_REGEN_MIN, 0.0);
        this.setDouble(DoubleModifier.MP_REGEN_MAX, 0.0);
        // Pastamancer
        this.setDouble(DoubleModifier.COMBAT_MANA_COST, 0.0);
        // Sauceror
        this.setDouble(DoubleModifier.SPELL_DAMAGE, 0.0);

        // Set modifiers depending on Character class
        AscensionClass ascensionClass = KoLCharacter.getAscensionClass();
        if (ascensionClass != null) {
          switch (ascensionClass) {
            case SEAL_CLUBBER, ZOMBIE_MASTER, ED, COWPUNCHER, BEANSLINGER, SNAKE_OILER -> {
              this.setDouble(DoubleModifier.HP_REGEN_MIN, 10.0);
              this.setDouble(DoubleModifier.HP_REGEN_MAX, 12.0);
              this.setDouble(DoubleModifier.WEAPON_DAMAGE, 15.0);
              this.setDouble(DoubleModifier.DAMAGE_REDUCTION, 1.0);
            }
            case TURTLE_TAMER -> {
              this.setDouble(DoubleModifier.HP_REGEN_MIN, 10.0);
              this.setDouble(DoubleModifier.HP_REGEN_MAX, 12.0);
              this.setDouble(DoubleModifier.FAMILIAR_WEIGHT, 5.0);
            }
            case DISCO_BANDIT, AVATAR_OF_SNEAKY_PETE -> this.setDouble(
                DoubleModifier.RANGED_DAMAGE, 20.0);
            case ACCORDION_THIEF -> this.setBoolean(Modifiers.FOUR_SONGS, true);
            case PASTAMANCER -> {
              this.setDouble(DoubleModifier.MP_REGEN_MIN, 5.0);
              this.setDouble(DoubleModifier.MP_REGEN_MAX, 6.0);
              this.setDouble(DoubleModifier.COMBAT_MANA_COST, -3.0);
            }
            case SAUCEROR, AVATAR_OF_JARLSBERG -> {
              this.setDouble(DoubleModifier.MP_REGEN_MIN, 5.0);
              this.setDouble(DoubleModifier.MP_REGEN_MAX, 6.0);
              this.setDouble(DoubleModifier.SPELL_DAMAGE, 20.0);
            }
          }
        }
        return true;
      }
    }
    return false;
  }

  private boolean overrideThrone(final Lookup lookup) {
    switch (lookup.getStringKey()) {
      case "Adventurous Spelunker" -> {
        this.setBoolean(Modifiers.DROPS_ITEMS, Preferences.getInteger("_oreDropsCrown") < 6);
        return true;
      }
      case "Garbage Fire" -> {
        this.setBoolean(
            Modifiers.DROPS_ITEMS, Preferences.getInteger("_garbageFireDropsCrown") < 3);
        return true;
      }
      case "Grimstone Golem" -> {
        this.setBoolean(
            Modifiers.DROPS_ITEMS, Preferences.getInteger("_grimstoneMaskDropsCrown") < 1);
        return true;
      }
      case "Grim Brother" -> {
        this.setBoolean(
            Modifiers.DROPS_ITEMS, Preferences.getInteger("_grimFairyTaleDropsCrown") < 2);
        return true;
      }
      case "Machine Elf" -> {
        this.setBoolean(
            Modifiers.DROPS_ITEMS, Preferences.getInteger("_abstractionDropsCrown") < 25);
        return true;
      }
      case "Puck Man", "Ms. Puck Man" -> {
        this.setBoolean(
            Modifiers.DROPS_ITEMS, Preferences.getInteger("_yellowPixelDropsCrown") < 25);
        return true;
      }
      case "Optimistic Candle" -> {
        this.setBoolean(
            Modifiers.DROPS_ITEMS, Preferences.getInteger("_optimisticCandleDropsCrown") < 3);
        return true;
      }
      case "Trick-or-Treating Tot" -> {
        this.setBoolean(
            Modifiers.DROPS_ITEMS, Preferences.getInteger("_hoardedCandyDropsCrown") < 3);
        return true;
      }
      case "Twitching Space Critter" -> {
        this.setBoolean(Modifiers.DROPS_ITEMS, Preferences.getInteger("_spaceFurDropsCrown") < 1);
        return true;
      }
    }
    return false;
  }

  private boolean override(final Lookup lookup) {
    if (this.expressions != null) {
      for (Indexed<ModifierExpression> entry : this.expressions) {
        this.setDouble(entry.index, entry.value.eval());
      }
    }

    // If the object does not require hard-coding, we're done
    if (!this.getBoolean(Modifiers.VARIABLE)) {
      return this.expressions != null;
    }

    return switch (lookup.type) {
      case ITEM -> overrideItem(lookup.getIntKey());
      case THRONE -> overrideThrone(lookup);
      case LOC, ZONE -> true;
      default -> false;
    };
  }

  public static final double getNumericModifier(
      final ModifierType type, final int id, final String mod) {
    return Modifiers.getNumericModifier(new Lookup(type, id), mod);
  }

  public static final double getNumericModifier(
      final ModifierType type, final String name, final String mod) {
    return Modifiers.getNumericModifier(new Lookup(type, name), mod);
  }

  public static final double getNumericModifier(final Lookup lookup, final String mod) {
    Modifiers mods = Modifiers.getModifiers(lookup);
    if (mods == null) {
      return 0.0;
    }
    return mods.get(mod);
  }

  public static final double getNumericModifier(final FamiliarData fam, final String mod) {
    return Modifiers.getNumericModifier(fam, mod, fam.getModifiedWeight(false), fam.getItem());
  }

  public static final double getNumericModifier(
      final FamiliarData fam,
      final String mod,
      final int passedWeight,
      final AdventureResult item) {
    int familiarId = fam != null ? fam.getId() : -1;
    if (familiarId == -1) {
      return 0.0;
    }

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
      tempMods.add(Modifiers.getItemModifiers(itemId));

      // Apply weight modifiers right now
      weight += (int) tempMods.get(DoubleModifier.FAMILIAR_WEIGHT);
      weight += (int) tempMods.get(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT);
      weight += (fam.getFeasted() ? 10 : 0);
      double percent = tempMods.get(DoubleModifier.FAMILIAR_WEIGHT_PCT) / 100.0;
      if (percent != 0.0) {
        weight = (int) Math.floor(weight + weight * percent);
      }
    }

    tempMods.lookupFamiliarModifiers(fam, weight, item);

    return tempMods.get(mod);
  }

  public static final boolean getBooleanModifier(
      final ModifierType type, final int id, final String mod) {
    return Modifiers.getBooleanModifier(new Lookup(type, id), mod);
  }

  public static final boolean getBooleanModifier(
      final ModifierType type, final String name, final String mod) {
    return Modifiers.getBooleanModifier(new Lookup(type, name), mod);
  }

  public static final boolean getBooleanModifier(final Lookup lookup, final String mod) {
    Modifiers mods = Modifiers.getModifiers(lookup);
    if (mods == null) {
      return false;
    }
    return mods.getBoolean(mod);
  }

  public static final String getStringModifier(
      final ModifierType type, final int id, final String mod) {
    return Modifiers.getStringModifier(new Lookup(type, id), mod);
  }

  public static final String getStringModifier(
      final ModifierType type, final String name, final String mod) {
    return Modifiers.getStringModifier(new Lookup(type, name), mod);
  }

  public static final String getStringModifier(final Lookup lookup, final String mod) {
    Modifiers mods = Modifiers.getModifiers(lookup);
    if (mods == null) {
      return "";
    }
    return mods.getString(mod);
  }

  public void applyPassiveModifiers(final boolean debug) {
    if (Modifiers.cachedPassiveModifiers == null) {
      Modifiers.cachedPassiveModifiers =
          new Modifiers(new Lookup(ModifierType.GENERATED, "cachedPassives"));
      PreferenceListenerRegistry.registerPreferenceListener(
          new String[] {"(skill)", "kingLiberated"}, () -> Modifiers.availableSkillsChanged = true);
    }

    if (debug
        || Modifiers.availableSkillsChanged
        || Modifiers.availablePassiveSkillModifiersByVariable.isEmpty()) {
      // Collect all passive skills currently on the character.
      Modifiers.availablePassiveSkillModifiersByVariable.putAll(
          KoLCharacter.getAvailableSkillIds().stream()
              .filter(SkillDatabase::isPassive)
              .map(UseSkillRequest::getUnmodifiedInstance)
              .filter(Objects::nonNull)
              .filter(UseSkillRequest::isEffective)
              .map(skill -> getModifiers(ModifierType.SKILL, skill.getSkillId()))
              .filter(Objects::nonNull)
              .collect(
                  Collectors.partitioningBy(
                      modifiers -> modifiers.override(modifiers.getLookup()))));

      // Recompute sum of cached constant passive skills.
      Modifiers.cachedPassiveModifiers.reset();
      Modifiers.availablePassiveSkillModifiersByVariable
          .get(false)
          .forEach(
              mods -> {
                Modifiers.cachedPassiveModifiers.add(mods);

                // If we are debugging, add them directly. Also add them to the cache though
                if (debug) {
                  this.add(mods);
                }
              });
      Modifiers.availableSkillsChanged = false;
    }

    // If we're debugging we've already added the modifiers while building the passive cache.
    if (!debug) {
      this.add(Modifiers.cachedPassiveModifiers);
    }

    // Add variable modifiers.
    Modifiers.availablePassiveSkillModifiersByVariable.get(true).forEach(this::add);
  }

  public final void applyFloristModifiers() {
    if (!FloristRequest.haveFlorist()) {
      return;
    }

    if (Modifiers.currentLocation == null) {
      return;
    }

    List<Florist> plants = FloristRequest.getPlants(Modifiers.currentLocation);
    if (plants == null) {
      return;
    }

    for (Florist plant : plants) {
      this.add(Modifiers.getModifiers(ModifierType.FLORIST, plant.toString()));
    }
  }

  public final void applyAutumnatonModifiers() {
    if (Modifiers.currentLocation == null || Modifiers.currentLocation.equals("")) return;

    var questLocation = AutumnatonManager.getQuestLocation();
    if (questLocation.equals("")) return;

    if (Modifiers.currentLocation.equals(questLocation)) {
      this.addDouble(DoubleModifier.EXPERIENCE, 1, ModifierType.AUTUMNATON, "");
    }
  }

  public void applySynergies() {
    int synergetic = this.getRawBitmap(Modifiers.SYNERGETIC);
    if (synergetic == 0) return; // nothing possible
    for (Entry<String, Integer> entry : Modifiers.synergies.entrySet()) {
      String name = entry.getKey();
      int mask = entry.getValue();
      if ((synergetic & mask) == mask) {
        this.add(Modifiers.getModifiers(ModifierType.SYNERGY, name));
      }
    }
  }

  // Returned set yields bitmaps keyed by names
  public static Set<Entry<String, Integer>> getSynergies() {
    return Collections.unmodifiableSet(Modifiers.synergies.entrySet());
  }

  private static final AdventureResult somePigs = EffectPool.get(EffectPool.SOME_PIGS);

  private static AdventureResult FIDOXENE = EffectPool.get(EffectPool.FIDOXENE);

  public void applyFamiliarModifiers(final FamiliarData familiar, AdventureResult famItem) {
    if (KoLConstants.activeEffects.contains(Modifiers.somePigs)) {
      // Under the effect of SOME PIGS, familiar gives no modifiers
      return;
    }

    int weight = familiar.getUncappedWeight();

    if (KoLConstants.activeEffects.contains(FIDOXENE)) {
      weight = Math.max(weight, 20);
    }

    weight += (int) this.get(DoubleModifier.FAMILIAR_WEIGHT);
    weight += (int) this.get(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT);
    weight += (familiar.getFeasted() ? 10 : 0);

    double percent = this.get(DoubleModifier.FAMILIAR_WEIGHT_PCT) / 100.0;
    if (percent != 0.0) {
      weight = (int) Math.floor(weight + weight * percent);
    }

    weight = Math.max(1, weight);
    this.lookupFamiliarModifiers(familiar, weight, famItem);
  }

  public void lookupFamiliarModifiers(
      final FamiliarData familiar, int weight, final AdventureResult famItem) {
    int familiarId = familiar.getId();
    weight = Math.max(1, weight);
    Modifiers.currentWeight = weight;

    String race = familiar.getRace();

    // Comma Chameleon acts as if it was something else
    if (familiarId == FamiliarPool.CHAMELEON) {
      String newRace = Preferences.getString("commaFamiliar");
      if (newRace != null && !newRace.isEmpty()) {
        race = newRace;
        familiarId = FamiliarDatabase.getFamiliarId(race);
      }
    }
    this.add(Modifiers.getModifiers(ModifierType.FAMILIAR, race));
    if (famItem != null) {
      // "fameq" modifiers are generated when "Familiar Effect" is parsed
      // from modifiers.txt
      this.add(Modifiers.getModifiers(ModifierType.FAM_EQ, famItem.getName()));
    }

    int cap = (int) this.get(DoubleModifier.FAMILIAR_WEIGHT_CAP);
    int cappedWeight = (cap == 0) ? weight : Math.min(weight, cap);

    double effective = cappedWeight * this.get(DoubleModifier.VOLLEYBALL_WEIGHT);
    if (effective == 0.0 && FamiliarDatabase.isVolleyType(familiarId)) {
      effective = weight;
    }
    if (effective != 0.0) {
      double factor = this.get(DoubleModifier.VOLLEYBALL_EFFECTIVENESS);
      // The 0->1 factor for generic familiars conflicts with the JitB
      if (factor == 0.0 && familiarId != FamiliarPool.JACK_IN_THE_BOX) factor = 1.0;
      factor = factor * (2 + effective / 5);
      double tuning;
      if ((tuning = this.get(DoubleModifier.FAMILIAR_TUNING_MUSCLE)) > 0) {
        double mainstatFactor = tuning / 100;
        double offstatFactor = (1 - mainstatFactor) / 2;
        this.addDouble(
            DoubleModifier.MUS_EXPERIENCE,
            factor * mainstatFactor,
            ModifierType.TUNED_VOLLEYBALL,
            race);
        this.addDouble(
            DoubleModifier.MYS_EXPERIENCE,
            factor * offstatFactor,
            ModifierType.TUNED_VOLLEYBALL,
            race);
        this.addDouble(
            DoubleModifier.MOX_EXPERIENCE,
            factor * offstatFactor,
            ModifierType.TUNED_VOLLEYBALL,
            race);
      } else if ((tuning = this.get(DoubleModifier.FAMILIAR_TUNING_MYSTICALITY)) > 0) {
        double mainstatFactor = tuning / 100;
        double offstatFactor = (1 - mainstatFactor) / 2;
        this.addDouble(
            DoubleModifier.MUS_EXPERIENCE,
            factor * offstatFactor,
            ModifierType.TUNED_VOLLEYBALL,
            race);
        this.addDouble(
            DoubleModifier.MYS_EXPERIENCE,
            factor * mainstatFactor,
            ModifierType.TUNED_VOLLEYBALL,
            race);
        this.addDouble(
            DoubleModifier.MOX_EXPERIENCE,
            factor * offstatFactor,
            ModifierType.TUNED_VOLLEYBALL,
            race);
      } else if ((tuning = this.get(DoubleModifier.FAMILIAR_TUNING_MOXIE)) > 0) {
        double mainstatFactor = tuning / 100;
        double offstatFactor = (1 - mainstatFactor) / 2;
        this.addDouble(
            DoubleModifier.MUS_EXPERIENCE,
            factor * offstatFactor,
            ModifierType.TUNED_VOLLEYBALL,
            race);
        this.addDouble(
            DoubleModifier.MYS_EXPERIENCE,
            factor * offstatFactor,
            ModifierType.TUNED_VOLLEYBALL,
            race);
        this.addDouble(
            DoubleModifier.MOX_EXPERIENCE,
            factor * mainstatFactor,
            ModifierType.TUNED_VOLLEYBALL,
            race);
      } else {
        this.addDouble(DoubleModifier.EXPERIENCE, factor, ModifierType.VOLLEYBALL, race);
      }
    }

    effective = cappedWeight * this.get(DoubleModifier.SOMBRERO_WEIGHT);
    if (effective == 0.0 && FamiliarDatabase.isSombreroType(familiarId)) {
      effective = weight;
    }
    effective += this.get(DoubleModifier.SOMBRERO_BONUS);
    if (effective != 0.0) {
      double factor = this.get(DoubleModifier.SOMBRERO_EFFECTIVENESS);
      if (factor == 0.0) factor = 1.0;
      // currentML is always >= 4, so we don't need to check for negatives
      int maxStats = 230;
      this.addDouble(
          DoubleModifier.EXPERIENCE,
          Math.min(
              Math.max(factor * (Modifiers.currentML / 4) * (0.1 + 0.005 * effective), 1),
              maxStats),
          ModifierType.FAMILIAR,
          race);
    }

    effective = cappedWeight * this.get(DoubleModifier.LEPRECHAUN_WEIGHT);
    if (effective == 0.0 && FamiliarDatabase.isMeatDropType(familiarId)) {
      effective = weight;
    }
    if (effective != 0.0) {
      double factor = this.get(DoubleModifier.LEPRECHAUN_EFFECTIVENESS);
      if (factor == 0.0) factor = 1.0;
      this.addDouble(
          DoubleModifier.MEATDROP,
          factor * (Math.sqrt(220 * effective) + 2 * effective - 6),
          ModifierType.FAMILIAR,
          race);
    }

    this.addFairyEffect(
        familiar,
        weight,
        cappedWeight,
        DoubleModifier.FAIRY_WEIGHT,
        DoubleModifier.FAIRY_EFFECTIVENESS,
        DoubleModifier.ITEMDROP);
    this.addFairyEffect(
        familiar,
        weight,
        cappedWeight,
        DoubleModifier.FOOD_FAIRY_WEIGHT,
        DoubleModifier.FOOD_FAIRY_EFFECTIVENESS,
        DoubleModifier.FOODDROP);
    this.addFairyEffect(
        familiar,
        weight,
        cappedWeight,
        DoubleModifier.BOOZE_FAIRY_WEIGHT,
        DoubleModifier.BOOZE_FAIRY_EFFECTIVENESS,
        DoubleModifier.BOOZEDROP);
    this.addFairyEffect(
        familiar,
        weight,
        cappedWeight,
        DoubleModifier.CANDY_FAIRY_WEIGHT,
        DoubleModifier.CANDY_FAIRY_EFFECTIVENESS,
        DoubleModifier.CANDYDROP);

    if (FamiliarDatabase.isUnderwaterType(familiarId)) {
      this.setBoolean(Modifiers.UNDERWATER_FAMILIAR, true);
    }

    switch (familiarId) {
      case FamiliarPool.HATRACK:
        if (famItem == EquipmentRequest.UNEQUIP) {
          this.addDouble(DoubleModifier.HATDROP, 50.0, ModifierType.FAMILIAR, "naked hatrack");
          this.addDouble(
              DoubleModifier.FAMILIAR_WEIGHT_CAP, 1, ModifierType.FAMILIAR, "naked hatrack");
        }
        break;
      case FamiliarPool.SCARECROW:
        if (famItem == EquipmentRequest.UNEQUIP) {
          this.addDouble(DoubleModifier.PANTSDROP, 50.0, ModifierType.FAMILIAR, "naked scarecrow");
          this.addDouble(
              DoubleModifier.FAMILIAR_WEIGHT_CAP, 1, ModifierType.FAMILIAR, "naked scarecrow");
        }
        break;
    }
  }

  private void addFairyEffect(
      final FamiliarData familiar,
      final int weight,
      final int cappedWeight,
      final DoubleModifier fairyModifier,
      final DoubleModifier effectivenessModifier,
      final DoubleModifier modifier) {
    var effective = cappedWeight * this.get(fairyModifier);

    // If it has no explicit modifier but is the right familiar type, add effect regardless
    if (effective == 0.0 && FamiliarDatabase.isFairyType(familiar.getId(), fairyModifier)) {
      effective = weight;
    }

    if (effective == 0.0) return;

    double factor = this.get(effectivenessModifier);
    // The 0->1 factor for generic familiars conflicts with the JitB
    if (factor == 0.0 && familiar.getId() != FamiliarPool.JACK_IN_THE_BOX) factor = 1.0;

    this.addDouble(
        modifier,
        factor * (Math.sqrt(55 * effective) + effective - 3),
        ModifierType.FAMILIAR,
        familiar.getRace());
  }

  public static final String getFamiliarEffect(final String itemName) {
    return Modifiers.familiarEffectByName.get(itemName);
  }

  public void applyMinstrelModifiers(final int level, AdventureResult instrument) {
    String instrumentName = instrument.getName();
    Lookup lookup = new Lookup(ModifierType.CLANCY, instrumentName);
    Modifiers imods = Modifiers.getModifiers(lookup);

    double effective = imods.get(DoubleModifier.VOLLEYBALL_WEIGHT);
    if (effective != 0.0) {
      double factor = 2 + effective / 5;
      this.addDouble(DoubleModifier.EXPERIENCE, factor, lookup);
    }

    effective = imods.get(DoubleModifier.FAIRY_WEIGHT);
    if (effective != 0.0) {
      double factor = Math.sqrt(55 * effective) + effective - 3;
      this.addDouble(DoubleModifier.ITEMDROP, factor, lookup);
    }

    this.addDouble(DoubleModifier.HP_REGEN_MIN, imods.get(DoubleModifier.HP_REGEN_MIN), lookup);
    this.addDouble(DoubleModifier.HP_REGEN_MAX, imods.get(DoubleModifier.HP_REGEN_MAX), lookup);
    this.addDouble(DoubleModifier.MP_REGEN_MIN, imods.get(DoubleModifier.MP_REGEN_MIN), lookup);
    this.addDouble(DoubleModifier.MP_REGEN_MAX, imods.get(DoubleModifier.MP_REGEN_MAX), lookup);
  }

  public void applyCompanionModifiers(Companion companion) {
    double multiplier = 1.0;
    if (KoLCharacter.hasSkill(SkillPool.WORKING_LUNCH)) {
      multiplier = 1.5;
    }

    switch (companion) {
      case EGGMAN -> this.addDouble(
          DoubleModifier.ITEMDROP, 50 * multiplier, ModifierType.COMPANION, "Eggman");
      case RADISH -> this.addDouble(
          DoubleModifier.INITIATIVE, 50 * multiplier, ModifierType.COMPANION, "Radish Horse");
      case HIPPO -> this.addDouble(
          DoubleModifier.EXPERIENCE, 3 * multiplier, ModifierType.COMPANION, "Hippotatomous");
      case CREAM -> this.addDouble(
          DoubleModifier.MONSTER_LEVEL, 20 * multiplier, ModifierType.COMPANION, "Cream Puff");
    }
  }

  public void applyServantModifiers(EdServantData servant) {
    int id = servant.getId();
    int level = servant.getLevel();
    switch (id) {
      case 1: // Cat
        if (servant.getLevel() >= 7) {
          this.addDouble(
              DoubleModifier.ITEMDROP,
              Math.sqrt(55 * level) + level - 3,
              ModifierType.SERVANT,
              "Cat");
        }
        break;

      case 3: // Maid
        this.addDouble(
            DoubleModifier.MEATDROP,
            Math.sqrt(220 * level) + 2 * level - 6,
            ModifierType.SERVANT,
            "Maid");
        break;

      case 5: // Scribe
        this.addDouble(DoubleModifier.EXPERIENCE, 2 + level / 5, ModifierType.SERVANT, "Scribe");
        break;
    }
  }

  public void applyCompanionModifiers(VYKEACompanionData companion) {
    VYKEACompanionType type = companion.getType();
    int level = companion.getLevel();
    switch (type) {
      case LAMP -> this.addDouble(DoubleModifier.ITEMDROP, level * 10, ModifierType.VYKEA, "Lamp");
      case COUCH -> this.addDouble(
          DoubleModifier.MEATDROP, level * 10, ModifierType.VYKEA, "Couch");
    }
  }

  public void applyVampyricCloakeModifiers() {
    MonsterData ensorcelee = MonsterDatabase.findMonster(Preferences.getString("ensorcelee"));

    if (ensorcelee != null) {
      String phylum = ensorcelee.getPhylum().toString();
      Modifiers ensorcelMods = Modifiers.getModifiers(ModifierType.ENSORCEL, phylum);
      if (ensorcelMods != null) {
        this.addDouble(
            DoubleModifier.MEATDROP,
            ensorcelMods.get(DoubleModifier.MEATDROP) * 0.25,
            ModifierType.ITEM,
            ItemPool.VAMPYRIC_CLOAKE);
        this.addDouble(
            DoubleModifier.ITEMDROP,
            ensorcelMods.get(DoubleModifier.ITEMDROP) * 0.25,
            ModifierType.ITEM,
            ItemPool.VAMPYRIC_CLOAKE);
        this.addDouble(
            DoubleModifier.CANDYDROP,
            ensorcelMods.get(DoubleModifier.CANDYDROP) * 0.25,
            ModifierType.ITEM,
            ItemPool.VAMPYRIC_CLOAKE);
      }
    }
  }

  // Parsing item enchantments into KoLmafia modifiers

  private static final Pattern SKILL_PATTERN = Pattern.compile("Grants Skill:.*?<b>(.*?)</b>");

  public static final String parseSkill(final String text) {
    Matcher matcher = Modifiers.SKILL_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.stringModifiers[Modifiers.SKILL].getTag() + ": \"" + matcher.group(1) + "\"";
    }

    return null;
  }

  private static final Pattern DR_PATTERN =
      Pattern.compile("Damage Reduction: (<b>)?([+-]?\\d+)(</b>)?");

  public static final String parseDamageReduction(final String text) {
    if (!text.contains("Damage Reduction:")) {
      return null;
    }

    Matcher matcher = Modifiers.DR_PATTERN.matcher(text);
    int dr = 0;

    while (matcher.find()) {
      dr += StringUtilities.parseInt(matcher.group(2));
    }

    return DoubleModifier.DAMAGE_REDUCTION.getTag() + ": " + dr;
  }

  private static final Pattern SINGLE_PATTERN =
      Pattern.compile("You may not equip more than one of these at a time");

  public static final String parseSingleEquip(final String text) {
    Matcher matcher = Modifiers.SINGLE_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.booleanModifiers[Modifiers.SINGLE].getTag();
    }

    return null;
  }

  private static final Pattern SOFTCORE_PATTERN =
      Pattern.compile("This item cannot be equipped while in Hardcore");

  public static final String parseSoftcoreOnly(final String text) {
    Matcher matcher = Modifiers.SOFTCORE_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.booleanModifiers[Modifiers.SOFTCORE].getTag();
    }

    return null;
  }

  private static final Pattern ITEM_DROPPER_PATTERN = Pattern.compile("Occasional Hilarity");

  public static final String parseDropsItems(final String text) {
    Matcher matcher = Modifiers.ITEM_DROPPER_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.booleanModifiers[Modifiers.DROPS_ITEMS].getTag();
    }

    return null;
  }

  private static final Pattern LASTS_ONE_DAY_PATTERN =
      Pattern.compile("This item will disappear at the end of the day");

  public static final String parseLastsOneDay(final String text) {
    Matcher matcher = Modifiers.LASTS_ONE_DAY_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.booleanModifiers[Modifiers.LASTS_ONE_DAY].getTag();
    }

    return null;
  }

  private static final Pattern FREE_PULL_PATTERN = Pattern.compile("Free pull from Hagnk's");

  public static final String parseFreePull(final String text) {
    Matcher matcher = Modifiers.FREE_PULL_PATTERN.matcher(text);
    if (matcher.find()) {
      return Modifiers.booleanModifiers[Modifiers.FREE_PULL].getTag();
    }

    return null;
  }

  private static final Pattern EFFECT_PATTERN =
      Pattern.compile("Effect: <b><a([^>]*)>([^<]*)</a></b>");

  public static final String parseEffect(final String text) {
    Matcher matcher = Modifiers.EFFECT_PATTERN.matcher(text);
    if (matcher.find()) {
      // matcher.group( 1 ) contains the the link to the description
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
      return Modifiers.stringModifiers[Modifiers.EFFECT].getTag() + ": \"" + name + "\"";
    }

    return null;
  }

  private static final Pattern EFFECT_DURATION_PATTERN =
      Pattern.compile("</a></b> \\(([\\d]*) Adventures?\\)");

  public static final String parseEffectDuration(final String text) {
    Matcher matcher = Modifiers.EFFECT_DURATION_PATTERN.matcher(text);
    if (matcher.find()) {
      return DoubleModifier.EFFECT_DURATION.getTag() + ": " + matcher.group(1);
    }

    return null;
  }

  private static final Pattern SONG_DURATION_PATTERN =
      Pattern.compile("Song Duration: <b>([\\d]*) Adventures</b>");

  public static final String parseSongDuration(final String text) {
    Matcher matcher = Modifiers.SONG_DURATION_PATTERN.matcher(text);
    if (matcher.find()) {
      return DoubleModifier.SONG_DURATION.getTag() + ": " + matcher.group(1);
    }

    return null;
  }

  private static final Pattern ALL_ATTR_PATTERN = Pattern.compile("^All Attributes ([+-]\\d+)$");
  private static final Pattern ALL_ATTR_PCT_PATTERN =
      Pattern.compile("^All Attributes ([+-]\\d+)%$");
  private static final Pattern CLASS_PATTERN =
      Pattern.compile("Bonus&nbsp;for&nbsp;(.*)&nbsp;only");
  private static final Pattern COMBAT_PATTERN =
      Pattern.compile("Monsters (?:are|will be) (.*) attracted to you");
  private static final Pattern HP_MP_PATTERN = Pattern.compile("^Maximum HP/MP ([+-]\\d+)$");

  private static final Map<String, String> COMBAT_RATE_DESCRIPTIONS =
      Map.ofEntries(
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
          Map.entry("<i>way</i> less", "-20"));

  public static final String parseModifier(final String enchantment) {
    String result;

    // Search the double modifiers first

    result = DoubleModifier.parseModifier(enchantment);
    if (result != null) {
      return result;
    }

    // Then the boolean modifiers

    result = Modifiers.parseModifier(Modifiers.booleanModifiers, enchantment, false);
    if (result != null) {
      return result;
    }

    // Then the string modifiers

    result = Modifiers.parseModifier(Modifiers.stringModifiers, enchantment, true);
    if (result != null) {
      return result;
    }

    // Special handling needed

    Matcher matcher;

    matcher = Modifiers.ALL_ATTR_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String mod = matcher.group(1);
      return Modifiers.MUSCLE
          + mod
          + ", "
          + Modifiers.MYSTICALITY
          + mod
          + ", "
          + Modifiers.MOXIE
          + mod;
    }

    matcher = Modifiers.ALL_ATTR_PCT_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String mod = matcher.group(1);
      return Modifiers.MUSCLE_PCT
          + mod
          + ", "
          + Modifiers.MYSTICALITY_PCT
          + mod
          + ", "
          + Modifiers.MOXIE_PCT
          + mod;
    }

    matcher = Modifiers.CLASS_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String plural = matcher.group(1);
      AscensionClass ascensionClass = AscensionClass.findByPlural(plural.replace("&nbsp;", " "));

      if (ascensionClass == null) return null;

      return Modifiers.stringModifiers[Modifiers.CLASS].getTag()
          + ": \""
          + ascensionClass.getName()
          + "\"";
    }

    matcher = Modifiers.COMBAT_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String tag =
          !enchantment.contains("Underwater only")
              ? DoubleModifier.COMBAT_RATE.getTag()
              : "Combat Rate (Underwater)";
      String level = matcher.group(1);
      String rate = COMBAT_RATE_DESCRIPTIONS.getOrDefault(level, "+0");
      return tag + ": " + rate;
    }

    matcher = Modifiers.HP_MP_PATTERN.matcher(enchantment);
    if (matcher.find()) {
      String mod = matcher.group(1);
      return Modifiers.HP_TAG + mod + ", " + Modifiers.MP_TAG + mod;
    }

    if (enchantment.contains("Regenerate")) {
      return Modifiers.parseRegeneration(enchantment);
    }

    if (enchantment.contains("Resistance")) {
      return Modifiers.parseResistance(enchantment);
    }

    if (enchantment.contains("Your familiar will always act in combat")) {
      return DoubleModifier.FAMILIAR_ACTION_BONUS.getTag() + ": +100";
    }

    return null;
  }

  public static final String parseStringModifier(final String enchantment) {
    return Modifiers.parseModifier(Modifiers.stringModifiers, enchantment, true);
  }

  private static <T extends net.sourceforge.kolmafia.modifiers.Modifier> String parseModifier(
      final T[] table, final String enchantment, final boolean quoted) {
    String quote = quoted ? "\"" : "";

    for (net.sourceforge.kolmafia.modifiers.Modifier tableRow : table) {
      Pattern[] patterns = tableRow.getDescPatterns();

      if (patterns == null) {
        continue;
      }

      for (Pattern pattern : patterns) {
        Matcher matcher = pattern.matcher(enchantment);
        if (!matcher.find()) {
          continue;
        }

        if (matcher.groupCount() == 0) {
          return tableRow.getTag();
        }

        String tag = tableRow.getTag();

        String value = matcher.group(1);

        if (tag.equals("Class")) {
          value = Modifiers.depluralizeClassName(value);
        }

        return tag + ": " + quote + value.trim() + quote;
      }
    }

    return null;
  }

  private static final Pattern REGEN_PATTERN =
      Pattern.compile("Regenerate (\\d*)-?(\\d*)? ([HM]P)( and .*)? per [aA]dventure$");

  private static String parseRegeneration(final String enchantment) {
    Matcher matcher = Modifiers.REGEN_PATTERN.matcher(enchantment);
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
      return Modifiers.HP_REGEN_MIN_TAG
          + min
          + ", "
          + Modifiers.HP_REGEN_MAX_TAG
          + max
          + ", "
          + Modifiers.MP_REGEN_MIN_TAG
          + min
          + ", "
          + Modifiers.MP_REGEN_MAX_TAG
          + max;
    }

    if (hp) {
      return Modifiers.HP_REGEN_MIN_TAG + min + ", " + Modifiers.HP_REGEN_MAX_TAG + max;
    }

    return Modifiers.MP_REGEN_MIN_TAG + min + ", " + Modifiers.MP_REGEN_MAX_TAG + max;
  }

  private static final Pattern RESISTANCE_PATTERN = Pattern.compile("Resistance \\(([+-]\\d+)\\)");

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

    if (enchantment.contains("Spooky") || all) mods.add(Modifiers.SPOOKY);
    if (enchantment.contains("Stench") || all) mods.add(Modifiers.STENCH);
    if (enchantment.contains("Hot") || all) mods.add(Modifiers.HOT);
    if (enchantment.contains("Cold") || all) mods.add(Modifiers.COLD);
    if (enchantment.contains("Sleaze") || all) mods.add(Modifiers.SLEAZE);
    if (enchantment.contains("Slime")) mods.add(Modifiers.SLIME);
    if (enchantment.contains("Supercold")) mods.add(Modifiers.SUPERCOLD);

    return mods.stream()
        .map(m -> m + level)
        .collect(
            Collectors.collectingAndThen(Collectors.joining(", "), m -> m.isEmpty() ? null : m));
  }

  private static <T extends net.sourceforge.kolmafia.modifiers.Modifier> boolean findModifier(
      final T[] table, final String tag) {
    for (T modifier : table) {
      Pattern pattern = modifier.getTagPattern();
      if (pattern == null) {
        continue;
      }

      Matcher matcher = pattern.matcher(tag);
      if (matcher.find()) {
        return true;
      }
    }
    return false;
  }

  public static final void checkModifiers() {
    for (Entry<ModifierType, Map<IntOrString, String>> typeEntry :
        Modifiers.modifierStringsByName.entrySet()) {
      ModifierType type = typeEntry.getKey();
      for (Entry<IntOrString, String> entry : typeEntry.getValue().entrySet()) {
        IntOrString key = entry.getKey();
        String modifierString = entry.getValue();

        if (modifierString == null) {
          RequestLogger.printLine("Key \"" + type + ":" + key + "\" has no modifiers");
          continue;
        }

        Modifiers modifiers = Modifiers.modifiersByName.get(type, key);
        if (modifiers != null) {
          modifierString = modifiers.getString(Modifiers.MODIFIERS);
        }

        ModifierList list = Modifiers.splitModifiers(modifierString);

        for (Modifier modifier : list) {
          String mod = modifier.toString();

          if (DoubleModifier.byTagPattern(mod) != null) {
            continue;
          }
          if (Modifiers.findModifier(Modifiers.bitmapModifiers, mod)) {
            continue;
          }
          if (Modifiers.findModifier(Modifiers.booleanModifiers, mod)) {
            continue;
          }
          if (Modifiers.findModifier(Modifiers.stringModifiers, mod)) {
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

  public static void setLocation(KoLAdventure location) {
    if (location == null) {
      Modifiers.currentLocation = "";
      Modifiers.currentZone = "";
      Modifiers.currentML = 4.0;
      return;
    }

    Modifiers.currentLocation = location.getAdventureName();
    Modifiers.currentZone = location.getZone();
    Modifiers.currentEnvironment = location.getEnvironment().toString();
    AreaCombatData data = location.getAreaSummary();
    Modifiers.currentML = Math.max(4.0, data == null ? 0.0 : data.getAverageML());
  }

  public static double getCurrentML() {
    return Modifiers.currentML;
  }

  public static void setFamiliar(FamiliarData fam) {
    Modifiers.currentFamiliar = fam == null ? "" : fam.getRace();
  }

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

        if (Modifiers.modifierStringsByName.put(type, lookup.getKey(), modifiers) != null) {
          KoLmafia.updateDisplay("Duplicate modifiers for: " + type + ":" + name);
        }

        Matcher matcher = FAMILIAR_EFFECT_PATTERN.matcher(modifiers);
        if (matcher.find()) {
          String effect = matcher.group(1);
          Modifiers.familiarEffectByName.put(name, effect);
          matcher = FAMILIAR_EFFECT_TRANSLATE_PATTERN.matcher(effect);
          if (matcher.find()) {
            effect = matcher.replaceAll(FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT);
          }
          matcher = FAMILIAR_EFFECT_TRANSLATE_PATTERN2.matcher(effect);
          if (matcher.find()) {
            effect = matcher.replaceAll(FAMILIAR_EFFECT_TRANSLATE_REPLACEMENT2);
          }
          Modifiers.modifierStringsByName.put(ModifierType.FAM_EQ, new IntOrString(name), effect);
        }
      }
    } catch (IOException e) {
      StaticEntity.printStackTrace(e);
    }
  }

  private static void computeSynergies() {
    Modifiers.synergies.clear();
    for (IntOrString key : Modifiers.modifierStringsByName.getAll(ModifierType.SYNERGY).keySet()) {
      if (!key.isString()) continue;
      String name = key.getStringValue();

      String[] pieces = name.split("/");
      if (pieces.length < 2) {
        KoLmafia.updateDisplay(name + " contain less than 2 elements.");
        continue;
      }
      int mask = 0;
      for (String piece : pieces) {
        Modifiers mods = Modifiers.getModifiers(ModifierType.ITEM, piece);
        if (mods == null) {
          KoLmafia.updateDisplay(name + " contains element " + piece + " with no modifiers.");
          return;
        }
        int emask = mods.bitmaps[Modifiers.SYNERGETIC];
        if (emask == 0) {
          KoLmafia.updateDisplay(name + " contains element " + piece + " that isn't Synergetic.");
          return;
        }
        mask |= emask;
      }
      Modifiers.synergies.put(name, mask);
    }
  }

  private static void computeMutex(ModifierType type, String name) {
    String[] pieces = name.split("/");
    if (pieces.length < 2) {
      KoLmafia.updateDisplay(name + " contain less than 2 elements.");
      return;
    }
    int bit = 1 << Modifiers.mutexes.size();
    for (String piece : pieces) {
      Modifiers mods =
          switch (type) {
            case MUTEX_I -> Modifiers.getModifiers(ModifierType.ITEM, piece);
            case MUTEX_E -> Modifiers.getModifiers(ModifierType.EFFECT, piece);
            default -> null;
          };
      if (mods == null) {
        KoLmafia.updateDisplay(name + " contains element " + piece + " with no modifiers.");
        return;
      }
      mods.bitmaps[Modifiers.MUTEX] |= bit;
    }
    Modifiers.mutexes.add(name);
  }

  private static void computeMutexes() {
    Modifiers.mutexes.clear();
    for (ModifierType type : List.of(ModifierType.MUTEX_E, ModifierType.MUTEX_I)) {
      for (IntOrString key : Modifiers.modifierStringsByName.getAll(type).keySet()) {
        if (!key.isString()) continue;
        String name = key.getStringValue();
        computeMutex(type, name);
      }
    }
  }

  private static void computeUniques() {
    Modifiers.uniques.clear();
    for (Entry<IntOrString, String> entry :
        Modifiers.modifierStringsByName.getAll(ModifierType.UNIQUE).entrySet()) {
      IntOrString key = entry.getKey();
      String modifiers = entry.getValue();
      if (!key.isString()) continue;
      String name = key.getStringValue();
      if (Modifiers.uniques.containsKey(name)) {
        KoLmafia.updateDisplay("Unique items for " + name + " already declared.");
        continue;
      }
      Modifiers.uniques.put(name, new HashSet<>(Arrays.asList(modifiers.split("/"))));
    }
  }

  public static void resetModifiers() {
    // Don't reset any variables that are set up by loadAllModifiers, as subsequent calls to
    // resetModifiers then won't set them back up due to the if() guarding loadAllModifiers.
    Modifiers.modifiersByName.clear();
    Modifiers.availablePassiveSkillModifiersByVariable.clear();
    Arrays.fill(Modifiers.bitmapMasks, 1);

    if (Modifiers.modifierStringsByName.size() == 0) {
      Modifiers.loadAllModifiers();
    }

    Modifiers.computeSynergies();
    Modifiers.computeMutexes();
    Modifiers.computeUniques();
  }

  static {
    Modifiers.resetModifiers();
  }

  public static Set<String> getUniques(String name) {
    return Modifiers.uniques.get(name);
  }

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
          Modifiers mods = Modifiers.getModifiers(ModifierType.ITEM, name);
          if (mods == null) {
            break;
          }
          if (!mods.getString(Modifiers.EFFECT).isEmpty()) {
            potions.add(name);
          } else if (mods.getBoolean(Modifiers.FREE_PULL)) {
            freepulls.add(name);
          } else if (!mods.getString(Modifiers.WIKI_NAME).isEmpty()) {
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
      if (Modifiers.getModifiers(ModifierType.FAMILIAR, name) != null) {
        familiars.add(name);
      }
    }

    // Make a map of campground items
    Set<String> campground = new TreeSet<>();

    for (int i = 0; i < CampgroundRequest.campgroundItems.length; ++i) {
      int itemId = CampgroundRequest.campgroundItems[i];
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

    // Make a map of stat days
    Set<String> statdays = new TreeSet<>();
    statdays.add("Muscle Day");
    statdays.add("Mysticality Day");
    statdays.add("Moxie Day");

    // Make a map of zones
    Set<String> zones = new TreeSet<>();

    for (String name : AdventureDatabase.ZONE_DESCRIPTIONS.keySet()) {
      if (Modifiers.getModifiers(ModifierType.ZONE, name) != null) {
        zones.add(name);
      }
    }

    // Make a map of locations
    Set<String> locations = new TreeSet<>();

    for (KoLAdventure key : AdventureDatabase.getAsLockableListModel()) {
      String name = key.getAdventureName();
      if (Modifiers.getModifiers(ModifierType.LOC, name) != null) {
        locations.add(name);
      }
    }

    // Make a map of synergies
    Set<String> synergies = new TreeSet<>();

    for (Entry<String, Integer> entry : Modifiers.synergies.entrySet()) {
      String name = entry.getKey();
      int mask = entry.getValue();
      synergies.add(name);
    }

    // Make a map of mutexes

    Set<String> mutexes = new TreeSet<>(Modifiers.mutexes);

    // Make a map of maximization categories
    int maximizationCount = Maximizer.maximizationCategories.length;

    Set<String> maximization =
        new TreeSet<>(
            Arrays.asList(Maximizer.maximizationCategories).subList(0, maximizationCount));

    writer.println(KoLConstants.MODIFIERS_VERSION);

    // For each equipment category, write the map entries
    Modifiers.writeModifierCategory(writer, hats, ModifierType.ITEM, "Hats");
    writer.println();
    Modifiers.writeModifierCategory(writer, pants, ModifierType.ITEM, "Pants");
    writer.println();
    Modifiers.writeModifierCategory(writer, shirts, ModifierType.ITEM, "Shirts");
    writer.println();
    Modifiers.writeModifierCategory(writer, weapons, ModifierType.ITEM, "Weapons");
    writer.println();
    Modifiers.writeModifierCategory(writer, offhands, ModifierType.ITEM, "Off-hand");
    writer.println();
    Modifiers.writeModifierCategory(writer, accessories, ModifierType.ITEM, "Accessories");
    writer.println();
    Modifiers.writeModifierCategory(writer, containers, ModifierType.ITEM, "Containers");
    writer.println();
    Modifiers.writeModifierCategory(writer, famitems, ModifierType.ITEM, "Familiar Items");
    writer.println();
    Modifiers.writeModifierCategory(writer, sixguns, ModifierType.ITEM, "Sixguns");
    writer.println();
    Modifiers.writeModifierCategory(writer, familiars, ModifierType.FAMILIAR, "Familiars");
    writer.println();
    Modifiers.writeModifierCategory(writer, bedazzlements, ModifierType.ITEM, "Bedazzlements");
    writer.println();
    Modifiers.writeModifierCategory(writer, cards, ModifierType.ITEM, "Alice's Army");
    writer.println();
    Modifiers.writeModifierCategory(writer, folders, ModifierType.ITEM, "Folder");
    writer.println();
    Modifiers.writeModifierCategory(
        writer, campground, ModifierType.CAMPGROUND, "Campground equipment");
    writer.println();
    Modifiers.writeModifierCategory(writer, effects, ModifierType.EFFECT, "Status Effects");
    writer.println();
    Modifiers.writeModifierCategory(writer, passives, ModifierType.SKILL, "Passive Skills");
    writer.println();
    Modifiers.writeModifierCategory(writer, outfits, ModifierType.OUTFIT, "Outfits");
    writer.println();
    Modifiers.writeModifierCategory(writer, zodiacs, ModifierType.SIGN, "Zodiac Sign");
    writer.println();
    Modifiers.writeModifierCategory(writer, statdays, ModifierType.EVENT, "Stat Day");
    writer.println();
    Modifiers.writeModifierCategory(writer, zones, ModifierType.ZONE, "Zone-specific");
    writer.println();
    Modifiers.writeModifierCategory(writer, locations, ModifierType.LOC, "Location-specific");
    writer.println();
    Modifiers.writeModifierCategory(writer, synergies, ModifierType.SYNERGY, "Synergies");
    writer.println();
    Modifiers.writeModifierCategory(
        writer, mutexes, ModifierType.MUTEX_I, "Mutually exclusive items");
    writer.println();
    Modifiers.writeModifierCategory(
        writer, mutexes, ModifierType.MUTEX_E, "Mutually exclusive effects");
    writer.println();
    Modifiers.writeModifierCategory(
        writer, maximization, ModifierType.MAX_CAT, "Maximization categories");
    writer.println();
    Modifiers.writeModifierCategory(writer, potions, ModifierType.ITEM, "Everything Else");
    Modifiers.writeModifierCategory(writer, freepulls, ModifierType.ITEM);
    Modifiers.writeModifierCategory(writer, wikiname, ModifierType.ITEM);

    writer.close();
  }

  private static void writeModifierCategory(
      final PrintStream writer, final Set<String> set, final ModifierType type, final String tag) {
    writer.println("# " + tag + " section of modifiers.txt");
    Modifiers.writeModifierCategory(writer, set, type);
  }

  private static void writeModifierCategory(
      final PrintStream writer, final Set<String> set, final ModifierType type) {
    writer.println();

    for (String name : set) {
      String modifierString = Modifiers.getModifierString(new Lookup(type, name));
      Modifiers.writeModifierItem(writer, type, name, modifierString);
    }
  }

  public static void writeModifierItem(
      final PrintStream writer, final ModifierType type, final String name, String modifierString) {
    if (modifierString == null) {
      Modifiers.writeModifierComment(writer, name);
      return;
    }

    Modifiers.writeModifierString(writer, type, name, modifierString);
  }

  public static void writeModifierString(
      final PrintStream writer,
      final ModifierType type,
      final String name,
      final String modifiers) {
    writer.println(Modifiers.modifierString(type, name, modifiers));
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
    writer.println(Modifiers.modifierCommentString(name, value));
  }

  public static String modifierCommentString(final String name) {
    return "# " + name;
  }

  public static void writeModifierComment(final PrintStream writer, final String name) {
    writer.println(Modifiers.modifierCommentString(name));
  }

  public static final void registerItem(
      final String name, final String text, final ConsumptionType type) {
    // Examine the item description and decide what it is.
    ArrayList<String> unknown = new ArrayList<>();
    String known = DebugDatabase.parseItemEnchantments(text, unknown, type);
    DebugDatabase.parseRestores(name, text);
    Modifiers.registerObject(ModifierType.ITEM, name, unknown, known);
  }

  public static final void registerEffect(final String name, final String text) {
    // Examine the effect description and decide what it is.
    ArrayList<String> unknown = new ArrayList<>();
    String known = DebugDatabase.parseEffectEnchantments(text, unknown);
    Modifiers.registerObject(ModifierType.EFFECT, name, unknown, known);
  }

  public static final void registerSkill(final String name, final String text) {
    // Examine the effect description and decide what it is.
    ArrayList<String> unknown = new ArrayList<>();
    String known = DebugDatabase.parseSkillEnchantments(text, unknown);
    Modifiers.registerObject(ModifierType.SKILL, name, unknown, known);
  }

  public static final void registerOutfit(final String name, final String text) {
    // Examine the outfit description and decide what it is.
    ArrayList<String> unknown = new ArrayList<>();
    String known = DebugDatabase.parseOutfitEnchantments(text, unknown);
    Modifiers.registerObject(ModifierType.OUTFIT, name, unknown, known);
  }

  public static final void updateItem(final int itemId, final String known) {
    Modifiers.overrideModifier(ModifierType.ITEM, itemId, known);
  }

  private static void registerObject(
      final ModifierType type,
      final String name,
      final ArrayList<String> unknown,
      final String known) {
    for (String value : unknown) {
      String printMe = Modifiers.modifierCommentString(name, value);
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);
    }

    if (known.isEmpty()) {
      if (unknown.size() == 0) {
        String printMe = Modifiers.modifierCommentString(name);
        RequestLogger.printLine(printMe);
        RequestLogger.updateSessionLog(printMe);
      }
    } else {
      String printMe = Modifiers.modifierString(type, name, known);
      RequestLogger.printLine(printMe);
      RequestLogger.updateSessionLog(printMe);

      Lookup lookup = new Lookup(type, name);
      Modifiers.modifierStringsByName.putIfAbsent(lookup.type, lookup.getKey(), known);
    }
  }

  public static class IntOrString {
    private int intValue = -1;
    private String stringValue = null;

    public IntOrString(String stringValue) {
      this.stringValue = stringValue;
    }

    public IntOrString(int intValue) {
      this.intValue = intValue;
    }

    public boolean isInt() {
      return stringValue == null;
    }

    public boolean isString() {
      return stringValue != null;
    }

    public int getIntValue() {
      return intValue;
    }

    public String getStringValue() {
      return stringValue;
    }

    @Override
    public int hashCode() {
      return isInt() ? this.intValue : this.stringValue.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof IntOrString other)) return false;
      return isInt() && other.isInt()
          ? this.intValue == other.intValue
          : this.stringValue.equals(other.stringValue);
    }

    @Override
    public String toString() {
      return isInt() ? Integer.toString(this.intValue) : this.stringValue;
    }
  }

  public static class Lookup {
    public ModifierType type;
    private IntOrString key; // int for Skill, Item, Effect; String otherwise.

    public Lookup(ModifierType type, IntOrString key) {
      this.type = type;
      this.key = key;
    }

    public Lookup(ModifierType type, int key) {
      this.type = type;
      this.key = new IntOrString(key);
    }

    public Lookup(ModifierType type, String name) {
      this.type = type;
      this.key =
          switch (type) {
            case ITEM -> new IntOrString(ItemDatabase.getExactItemId(name));
            case EFFECT -> new IntOrString(EffectDatabase.getEffectId(name, true));
            case SKILL -> new IntOrString(SkillDatabase.getSkillId(name, true));
            default -> new IntOrString(name);
          };
      if (EnumSet.of(ModifierType.ITEM, ModifierType.EFFECT, ModifierType.SKILL).contains(type)
          && this.key.getIntValue() == -1) {
        this.type = ModifierType.fromString("PSEUDO_" + type.name());
        this.key = new IntOrString(name);
      }
    }

    @Override
    public String toString() {
      return switch (this.type) {
        case ITEM, EFFECT, SKILL -> this.type.pascalCaseName() + ":[" + this.key + "]";
        default -> this.type.pascalCaseName() + ":" + this.key;
      };
    }

    public IntOrString getKey() {
      return this.key;
    }

    public int getIntKey() {
      return this.key.getIntValue();
    }

    public String getStringKey() {
      return this.key.getStringValue();
    }

    public String getName() {
      return switch (type) {
        case ITEM -> getIntKey() < -1
            ? ClanLoungeRequest.hotdogIdToName(getIntKey())
            : ItemDatabase.getItemName(getIntKey());
        case EFFECT -> EffectDatabase.getEffectName(getIntKey());
        case SKILL -> SkillDatabase.getSkillName(getIntKey());
        default -> getStringKey();
      };
    }
  }

  private static class Indexed<T> {
    public DoubleModifier index;
    public T value;

    public Indexed(DoubleModifier index, T value) {
      this.index = index;
      this.value = value;
    }
  }
}
