package net.sourceforge.kolmafia;

import static net.sourceforge.kolmafia.utilities.Statics.DateTimeManager;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.VYKEACompanionData.VYKEACompanionType;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.BitmapModifierCollection;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.BooleanModifierCollection;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifierCollection;
import net.sourceforge.kolmafia.modifiers.Lookup;
import net.sourceforge.kolmafia.modifiers.Modifier;
import net.sourceforge.kolmafia.modifiers.ModifierList;
import net.sourceforge.kolmafia.modifiers.ModifierList.ModifierValue;
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.modifiers.StringModifierCollection;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.persistence.ItemDatabase;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.persistence.SkillDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CharPaneRequest.Companion;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.FloristRequest;
import net.sourceforge.kolmafia.request.FloristRequest.Florist;
import net.sourceforge.kolmafia.request.StandardRequest;
import net.sourceforge.kolmafia.session.AutumnatonManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import net.sourceforge.kolmafia.utilities.Indexed;
import net.sourceforge.kolmafia.utilities.IntOrString;

public class Modifiers {
  // static fields used to compute current modifiers

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

  // caching of passive skills for the current character
  private static boolean availableSkillsChanged = false;
  private static final Map<Boolean, List<Modifiers>> availablePassiveSkillModifiersByVariable =
      new TreeMap<>();
  private static Modifiers cachedPassiveModifiers = null;

  // fields used in Modifiers objects

  private Lookup originalLookup;
  // Assume modifiers are variable until proven otherwise.
  public boolean variable = true;
  private final DoubleModifierCollection doubles = new DoubleModifierCollection();
  private final BooleanModifierCollection booleans = new BooleanModifierCollection();
  private final BitmapModifierCollection bitmaps = new BitmapModifierCollection();
  private final StringModifierCollection strings = new StringModifierCollection();
  private ArrayList<Indexed<DoubleModifier, ModifierExpression>> expressions = null;
  // These are used for Steely-Eyed Squint and so on
  private final DoubleModifierCollection accumulators = new DoubleModifierCollection();

  // constants

  private static final AdventureResult somePigs = EffectPool.get(EffectPool.SOME_PIGS);

  private static final AdventureResult FIDOXENE = EffectPool.get(EffectPool.FIDOXENE);

  public Modifiers() {
    // Everything should be initialized above.
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

  static {
    // this is here, instead of in `ModifierDatabase`, as tests (e.g. ConcertCommandTest) failed
    // without the db intialised
    ModifierDatabase.ensureModifierDatabaseInitialised();
  }

  public Map<DerivedModifier, Integer> predict() {
    Map<DerivedModifier, Integer> rv = new EnumMap<>(DerivedModifier.class);

    int mus = KoLCharacter.getBaseMuscle();
    int mys = KoLCharacter.getBaseMysticality();
    int mox = KoLCharacter.getBaseMoxie();

    String equalize = this.getString(StringModifier.EQUALIZE);
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

    String mus_equalize = this.getString(StringModifier.EQUALIZE_MUSCLE);
    if (mus_equalize.startsWith("Mys")) {
      mus = mys;
    } else if (mus_equalize.startsWith("Mox")) {
      mus = mox;
    }
    String mys_equalize = this.getString(StringModifier.EQUALIZE_MYST);
    if (mys_equalize.startsWith("Mus")) {
      mys = mus;
    } else if (mys_equalize.startsWith("Mox")) {
      mys = mox;
    }
    String mox_equalize = this.getString(StringModifier.EQUALIZE_MOXIE);
    if (mox_equalize.startsWith("Mus")) {
      mox = mus;
    } else if (mox_equalize.startsWith("Mys")) {
      mox = mys;
    }

    int mus_limit = (int) this.getDouble(DoubleModifier.MUS_LIMIT);
    if (mus_limit > 0 && mus > mus_limit) {
      mus = mus_limit;
    }
    int mys_limit = (int) this.getDouble(DoubleModifier.MYS_LIMIT);
    if (mys_limit > 0 && mys > mys_limit) {
      mys = mys_limit;
    }
    int mox_limit = (int) this.getDouble(DoubleModifier.MOX_LIMIT);
    if (mox_limit > 0 && mox > mox_limit) {
      mox = mox_limit;
    }

    rv.put(
        DerivedModifier.BUFFED_MUS,
        mus
            + (int) this.getDouble(DoubleModifier.MUS)
            + (int) Math.ceil(this.getDouble(DoubleModifier.MUS_PCT) * mus / 100.0));
    rv.put(
        DerivedModifier.BUFFED_MYS,
        mys
            + (int) this.getDouble(DoubleModifier.MYS)
            + (int) Math.ceil(this.getDouble(DoubleModifier.MYS_PCT) * mys / 100.0));
    rv.put(
        DerivedModifier.BUFFED_MOX,
        mox
            + (int) this.getDouble(DoubleModifier.MOX)
            + (int) Math.ceil(this.getDouble(DoubleModifier.MOX_PCT) * mox / 100.0));

    String mus_buffed_floor = this.getString(StringModifier.FLOOR_BUFFED_MUSCLE);
    if (mus_buffed_floor.startsWith("Mys")) {
      var mod = rv.get(DerivedModifier.BUFFED_MYS);
      if (mod > rv.get(DerivedModifier.BUFFED_MUS)) {
        rv.put(DerivedModifier.BUFFED_MUS, mod);
      }
    } else if (mus_buffed_floor.startsWith("Mox")) {
      var mod = rv.get(DerivedModifier.BUFFED_MOX);
      if (mod > rv.get(DerivedModifier.BUFFED_MUS)) {
        rv.put(DerivedModifier.BUFFED_MUS, mod);
      }
    }
    String mys_buffed_floor = this.getString(StringModifier.FLOOR_BUFFED_MYST);
    if (mys_buffed_floor.startsWith("Mus")) {
      var mod = rv.get(DerivedModifier.BUFFED_MUS);
      if (mod > rv.get(DerivedModifier.BUFFED_MYS)) {
        rv.put(DerivedModifier.BUFFED_MYS, mod);
      }
    } else if (mys_buffed_floor.startsWith("Mox")) {
      var mod = rv.get(DerivedModifier.BUFFED_MOX);
      if (mod > rv.get(DerivedModifier.BUFFED_MYS)) {
        rv.put(DerivedModifier.BUFFED_MYS, mod);
      }
    }
    String mox_buffed_floor = this.getString(StringModifier.FLOOR_BUFFED_MOXIE);
    if (mox_buffed_floor.startsWith("Mus")) {
      var mod = rv.get(DerivedModifier.BUFFED_MUS);
      if (mod > rv.get(DerivedModifier.BUFFED_MOX)) {
        rv.put(DerivedModifier.BUFFED_MOX, mod);
      }
    } else if (mox_buffed_floor.startsWith("Mys")) {
      var mod = rv.get(DerivedModifier.BUFFED_MYS);
      if (mod > rv.get(DerivedModifier.BUFFED_MOX)) {
        rv.put(DerivedModifier.BUFFED_MOX, mod);
      }
    }

    int hpbase;
    int hp;
    int buffedHP;
    if (KoLCharacter.isVampyre()) {
      hpbase = KoLCharacter.getBaseMuscle();
      hp = hpbase + (int) this.getDouble(DoubleModifier.HP);
      buffedHP = Math.max(hp, mus);
    } else if (KoLCharacter.inRobocore()) {
      hpbase = 30;
      hp = hpbase + (int) this.getDouble(DoubleModifier.HP);
      buffedHP = hp;
    } else if (KoLCharacter.isGreyGoo()) {
      hpbase =
          (int) KoLCharacter.getBaseMaxHP()
              - (int) KoLCharacter.currentNumericModifier(DoubleModifier.HP);
      hp = hpbase + (int) this.getDouble(DoubleModifier.HP);
      buffedHP = hp;
    } else {
      hpbase = rv.get(DerivedModifier.BUFFED_MUS) + 3;
      double C = KoLCharacter.isMuscleClass() ? 1.5 : 1.0;
      double hpPercent = this.getDouble(DoubleModifier.HP_PCT);
      hp =
          (int) Math.ceil(hpbase * (C + hpPercent / 100.0))
              + (int) this.getDouble(DoubleModifier.HP);
      buffedHP = Math.max(hp, mus);
    }
    rv.put(DerivedModifier.BUFFED_HP, buffedHP);

    int mpbase;
    int mp;
    int buffedMP;
    if (KoLCharacter.isGreyGoo()) {
      mpbase =
          (int) KoLCharacter.getBaseMaxMP()
              - (int) KoLCharacter.currentNumericModifier(DoubleModifier.MP);
      mp = mpbase + (int) this.getDouble(DoubleModifier.MP);
      buffedMP = mp;
    } else {
      mpbase = rv.get(DerivedModifier.BUFFED_MYS);
      if (this.getBoolean(BooleanModifier.MOXIE_CONTROLS_MP)
          || (this.getBoolean(BooleanModifier.MOXIE_MAY_CONTROL_MP)
              && rv.get(DerivedModifier.BUFFED_MOX) > mpbase)) {
        mpbase = rv.get(DerivedModifier.BUFFED_MOX);
      }
      double C = KoLCharacter.isMysticalityClass() ? 1.5 : 1.0;
      double mpPercent = this.getDouble(DoubleModifier.MP_PCT);
      mp =
          (int) Math.ceil(mpbase * (C + mpPercent / 100.0))
              + (int) this.getDouble(DoubleModifier.MP);
      buffedMP = Math.max(mp, mys);
    }
    rv.put(DerivedModifier.BUFFED_MP, buffedMP);

    return rv;
  }

  public Lookup getLookup() {
    return this.originalLookup;
  }

  public void setLookup(Lookup lookup) {
    this.originalLookup = lookup;
  }

  public final void reset() {
    this.doubles.reset();
    this.strings.reset();
    this.booleans.reset();
    this.bitmaps.reset();
    this.expressions = null;
  }

  public double getNumeric(final Modifier modifier) {
    if (modifier instanceof DoubleModifier db) {
      return getDouble(db);
    } else if (modifier instanceof DerivedModifier db) {
      return getDerived(db);
    } else if (modifier instanceof BitmapModifier bm) {
      return getBitmap(bm);
    } else {
      return 0.0;
    }
  }

  private double derivePrismaticDamage() {
    double damage = this.doubles.get(DoubleModifier.COLD_DAMAGE);
    damage = Math.min(damage, this.doubles.get(DoubleModifier.HOT_DAMAGE));
    damage = Math.min(damage, this.doubles.get(DoubleModifier.SLEAZE_DAMAGE));
    damage = Math.min(damage, this.doubles.get(DoubleModifier.SPOOKY_DAMAGE));
    damage = Math.min(damage, this.doubles.get(DoubleModifier.STENCH_DAMAGE));
    // TODO: check if there is a point to this -- we never read the cached value?
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

  public double getDouble(final DoubleModifier modifier) {
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

  public int getRawBitmap(final BitmapModifier modifier) {
    if (modifier == null) {
      return 0;
    }

    return this.bitmaps.get(modifier);
  }

  public int getBitmap(final BitmapModifier modifier) {
    if (modifier == null) {
      return 0;
    }

    int n = this.bitmaps.get(modifier);
    // Count the bits:
    if (n == 0) return 0;
    n = ((n & 0xAAAAAAAA) >>> 1) + (n & 0x55555555);
    n = ((n & 0xCCCCCCCC) >>> 2) + (n & 0x33333333);
    n = ((n & 0xF0F0F0F0) >>> 4) + (n & 0x0F0F0F0F);
    n = ((n & 0xFF00FF00) >>> 8) + (n & 0x00FF00FF);
    n = ((n & 0xFFFF0000) >>> 16) + (n & 0x0000FFFF);

    return modifier == BitmapModifier.CLOWNINESS ? 25 * n : n;
  }

  public double getDerived(final DerivedModifier modifier) {
    return this.predict().get(modifier);
  }

  public boolean getBoolean(final BooleanModifier modifier) {
    if (modifier == null) {
      return false;
    }

    return this.booleans.get(modifier);
  }

  /**
   * Get all boolean values matching a given mask.
   *
   * <p>Used in Evaluator to test whether an evaluation should be marked as failed.
   */
  public EnumSet<BooleanModifier> getBooleans(final EnumSet<BooleanModifier> mask) {
    var bools = this.booleans.raw();
    bools.retainAll(mask);
    return bools;
  }

  public String getString(final StringModifier modifier) {
    if (modifier == null) {
      return "";
    }

    // Can't cache this as expressions can be dependent on things
    // that can change within a session, like character level.
    if (modifier == StringModifier.EVALUATED_MODIFIERS) {
      return ModifierDatabase.evaluateModifiers(
              this.originalLookup, this.strings.get(StringModifier.MODIFIERS))
          .toString();
    }

    return this.strings.get(modifier);
  }

  public double getAccumulator(final DoubleModifier modifier) {
    if (modifier == null) {
      // For now, make it obvious that something went wrong
      return -9999.0;
    }
    return this.accumulators.get(modifier);
  }

  public boolean setDouble(final DoubleModifier mod, final double value) {
    if (mod == null) {
      return false;
    }

    return this.doubles.set(mod, value);
  }

  public boolean setBitmap(final BitmapModifier modifier, final int value) {
    if (modifier == null) {
      return false;
    }

    return this.bitmaps.set(modifier, value);
  }

  public boolean setBoolean(final BooleanModifier modifier, final boolean value) {
    if (modifier == null) {
      return false;
    }

    return this.booleans.set(modifier, value);
  }

  public boolean setString(final StringModifier modifier, String mod) {
    if (modifier == null) {
      return false;
    }

    if (mod == null) {
      mod = "";
    }

    return this.strings.set(modifier, mod);
  }

  public boolean set(final Modifiers mods) {
    if (mods == null) {
      return false;
    }

    boolean changed = false;
    this.originalLookup = mods.originalLookup;

    for (var mod : DoubleModifier.DOUBLE_MODIFIERS) {
      changed |= this.setDouble(mod, mods.doubles.get(mod));
    }

    for (var mod : BitmapModifier.BITMAP_MODIFIERS) {
      changed |= this.setBitmap(mod, mods.bitmaps.get(mod));
    }

    for (var mod : BooleanModifier.BOOLEAN_MODIFIERS) {
      changed |= this.setBoolean(mod, mods.booleans.get(mod));
    }

    for (var mod : StringModifier.STRING_MODIFIERS) {
      changed |= this.setString(mod, mods.strings.get(mod));
    }

    return changed;
  }

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
        {
          // Only the lowest limiter applies
          double current = this.doubles.get(mod);
          if ((current == 0.0 || current > value) && value > 0.0) {
            this.doubles.set(mod, value);
          }
          break;
        }
      case ITEMDROP:
        if (ModifierDatabase.DOUBLED_BY_SQUINT_CHAMPAGNE.contains(type)) {
          this.accumulators.add(mod, value);
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
        // accumulators acts as an accumulator for modifiers that are possibly multiplied by
        // multipliers like makeshift garbage shirt, Bendin' Hell, Bow-Legged Swagger, or Dirty
        // Pear.
        // TODO: Figure out which ones aren't multiplied and exclude them. BoomBox?
        this.accumulators.add(mod, value);
        this.doubles.add(mod, value);
        break;
      case FAMILIAR_ACTION_BONUS:
        this.doubles.set(mod, Math.min(100, this.getDouble(mod) + value));
        break;
      case STOMACH_CAPACITY:
        if (KoLCharacter.canExpandStomachCapacity()) {
          this.doubles.add(mod, value);
        }
        break;
      case LIVER_CAPACITY:
        if (KoLCharacter.canExpandLiverCapacity()) {
          this.doubles.add(mod, value);
        }
        break;
      case SPLEEN_CAPACITY:
        if (KoLCharacter.canExpandSpleenCapacity()) {
          this.doubles.add(mod, value);
        }
        break;
      default:
        this.doubles.add(mod, value);
        break;
    }
  }

  public void addBitmap(BitmapModifier modifier, int bit) {
    this.bitmaps.add(modifier, bit);
  }

  public void add(final Modifiers mods) {
    if (mods == null) {
      return;
    }

    // Make sure the modifiers apply to current class
    String className = mods.strings.get(StringModifier.CLASS);
    if (className != null && !className.isEmpty()) {
      AscensionClass ascensionClass = AscensionClass.findByExactName(className);
      if (ascensionClass != null && ascensionClass != KoLCharacter.getAscensionClass()) {
        return;
      }
    }

    // Unarmed modifiers apply only if the character has no weapon or offhand
    boolean unarmed = mods.getBoolean(BooleanModifier.UNARMED);
    if (unarmed && !Modifiers.unarmed) {
      return;
    }

    Lookup lookup = mods.originalLookup;

    // Add in the double modifiers
    var bothWatches =
        mods.booleans.get(BooleanModifier.NONSTACKABLE_WATCH)
            && this.booleans.get(BooleanModifier.NONSTACKABLE_WATCH);

    mods.doubles.forEach(
        (i, addition) -> {
          if (!bothWatches || i != DoubleModifier.ADVENTURES) {
            this.addDouble(i, addition, lookup);
          }
        });

    // Add in string modifiers as appropriate.

    String val;
    val = mods.strings.get(StringModifier.EQUALIZE);
    if (!val.isEmpty() && this.strings.get(StringModifier.EQUALIZE).isEmpty()) {
      this.strings.set(StringModifier.EQUALIZE, val);
    }
    val = mods.strings.get(StringModifier.INTRINSIC_EFFECT);
    if (!val.isEmpty()) {
      String prev = this.strings.get(StringModifier.INTRINSIC_EFFECT);
      if (prev.isEmpty()) {
        this.strings.set(StringModifier.INTRINSIC_EFFECT, val);
      } else {
        this.strings.set(StringModifier.INTRINSIC_EFFECT, prev + "\t" + val);
      }
    }
    val = mods.strings.get(StringModifier.STAT_TUNING);
    if (!val.isEmpty()) {
      this.strings.set(StringModifier.STAT_TUNING, val);
    }
    val = mods.strings.get(StringModifier.EQUALIZE_MUSCLE);
    if (!val.isEmpty()) {
      this.strings.set(StringModifier.EQUALIZE_MUSCLE, val);
    }
    val = mods.strings.get(StringModifier.EQUALIZE_MYST);
    if (!val.isEmpty()) {
      this.strings.set(StringModifier.EQUALIZE_MYST, val);
    }
    val = mods.strings.get(StringModifier.EQUALIZE_MOXIE);
    if (!val.isEmpty()) {
      this.strings.set(StringModifier.EQUALIZE_MOXIE, val);
    }

    // OR in the bitmap modifiers
    var mutexes = this.bitmaps.get(BitmapModifier.MUTEX) & mods.bitmaps.get(BitmapModifier.MUTEX);
    this.bitmaps.add(BitmapModifier.MUTEX_VIOLATIONS, mutexes);
    for (var mod : BitmapModifier.BITMAP_MODIFIERS) {
      this.bitmaps.add(mod, mods.bitmaps.get(mod));
    }

    // OR in the boolean modifiers
    for (var mod : BooleanModifier.BOOLEAN_MODIFIERS) {
      if (mods.booleans.get(mod)) {
        this.booleans.set(mod, true);
      }
    }
  }

  public boolean setModifier(final ModifierValue mod) {
    if (mod == null) {
      return false;
    }

    var modifier = ModifierDatabase.getModifierByName(mod.getName());
    if (modifier != null) {
      if (modifier instanceof DoubleModifier d) {
        return this.setDouble(d, Double.parseDouble(mod.getValue()));
      } else if (modifier instanceof StringModifier s) {
        return this.setString(s, mod.getValue());
      } else if (modifier instanceof BitmapModifier b) {
        return this.setBitmap(b, Integer.parseInt(mod.getValue()));
      } else if (modifier instanceof BooleanModifier b) {
        return this.setBoolean(b, mod.getValue().equals("true"));
      }
    }
    return false;
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
        this.setBoolean(
            BooleanModifier.DROPS_ITEMS, Preferences.getInteger("_pantsgivingCrumbs") < 10);
        return true;
      }
      case ItemPool.PATRIOT_SHIELD, ItemPool.REPLICA_PATRIOT_SHIELD -> {
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
        this.setBoolean(BooleanModifier.FOUR_SONGS, false);
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
            case SEAL_CLUBBER, ZOMBIE_MASTER, ED, COW_PUNCHER, BEANSLINGER, SNAKE_OILER -> {
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
            case ACCORDION_THIEF -> this.setBoolean(BooleanModifier.FOUR_SONGS, true);
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
        this.setBoolean(BooleanModifier.DROPS_ITEMS, Preferences.getInteger("_oreDropsCrown") < 6);
        return true;
      }
      case "Garbage Fire" -> {
        this.setBoolean(
            BooleanModifier.DROPS_ITEMS, Preferences.getInteger("_garbageFireDropsCrown") < 3);
        return true;
      }
      case "Grimstone Golem" -> {
        this.setBoolean(
            BooleanModifier.DROPS_ITEMS, Preferences.getInteger("_grimstoneMaskDropsCrown") < 1);
        return true;
      }
      case "Grim Brother" -> {
        this.setBoolean(
            BooleanModifier.DROPS_ITEMS, Preferences.getInteger("_grimFairyTaleDropsCrown") < 2);
        return true;
      }
      case "Machine Elf" -> {
        this.setBoolean(
            BooleanModifier.DROPS_ITEMS, Preferences.getInteger("_abstractionDropsCrown") < 25);
        return true;
      }
      case "Puck Man", "Ms. Puck Man" -> {
        this.setBoolean(
            BooleanModifier.DROPS_ITEMS, Preferences.getInteger("_yellowPixelDropsCrown") < 25);
        return true;
      }
      case "Optimistic Candle" -> {
        this.setBoolean(
            BooleanModifier.DROPS_ITEMS, Preferences.getInteger("_optimisticCandleDropsCrown") < 3);
        return true;
      }
      case "Trick-or-Treating Tot" -> {
        this.setBoolean(
            BooleanModifier.DROPS_ITEMS, Preferences.getInteger("_hoardedCandyDropsCrown") < 3);
        return true;
      }
      case "Twitching Space Critter" -> {
        this.setBoolean(
            BooleanModifier.DROPS_ITEMS, Preferences.getInteger("_spaceFurDropsCrown") < 1);
        return true;
      }
    }
    return false;
  }

  // TODO: what does this do? what is expressions? Something to do with the [X] strings?
  public boolean override(final Lookup lookup) {
    if (this.expressions != null) {
      for (Indexed<DoubleModifier, ModifierExpression> entry : this.expressions) {
        this.setDouble(entry.index, entry.value.eval());
      }
    }

    // If the object does not require hard-coding, we're done
    if (!this.getBoolean(BooleanModifier.VARIABLE)) {
      return this.expressions != null;
    }

    return switch (lookup.type) {
      case ITEM -> overrideItem(lookup.getIntKey());
      case THRONE -> overrideThrone(lookup);
      case LOC, ZONE -> true;
      default -> false;
    };
  }

  public static synchronized void availableSkillsChanged() {
    availableSkillsChanged = true;
  }

  public void addExpression(Indexed<DoubleModifier, ModifierExpression> entry) {
    int index = -1;

    if (this.expressions == null) {
      this.expressions = new ArrayList<>();
    } else {
      for (int i = 0; i < this.expressions.size(); i++) {
        Indexed<DoubleModifier, ModifierExpression> e = this.expressions.get(i);
        if (e != null && e.index == entry.index) {
          index = i;
          break;
        }
      }
    }

    if (index < 0) {
      this.expressions.add(entry);
    } else {
      this.expressions.get(index).value.combine(entry.value, '+');
    }
  }

  public void applyPassiveModifiers(final boolean debug) {
    if (Modifiers.cachedPassiveModifiers == null) {
      Modifiers.cachedPassiveModifiers =
          new Modifiers(new Lookup(ModifierType.PASSIVES, "cachedPassives"));
      PreferenceListenerRegistry.registerPreferenceListener(
          new String[] {"(skill)", "kingLiberated", "(ronin)"},
          () -> Modifiers.availableSkillsChanged());
    }
    if (KoLCharacter.getAvailableSkillIds().isEmpty()) {
      // We probably haven't loaded the player's skills yet. Avoid populating
      // availablePassiveSkillModifiersByVariable with two empty lists.
      return;
    }

    synchronized (Modifiers.class) {
      if (debug
          || Modifiers.availableSkillsChanged
          || Modifiers.availablePassiveSkillModifiersByVariable.isEmpty()) {
        // Collect all passive skills currently on the character.
        Modifiers.availablePassiveSkillModifiersByVariable.putAll(
            KoLCharacter.getAvailableSkillIds().stream()
                .filter(SkillDatabase::isPassive)
                .map(skill -> ModifierDatabase.getModifiers(ModifierType.SKILL, skill))
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
    }

    // If we're debugging we've already added the modifiers while building the passive cache.
    if (!debug) {
      this.add(Modifiers.cachedPassiveModifiers);
    }

    // Add variable modifiers.
    Modifiers.availablePassiveSkillModifiersByVariable.get(true).forEach(this::add);
  }

  public static void resetAvailablePassiveSkills() {
    availablePassiveSkillModifiersByVariable.clear();
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
      this.add(ModifierDatabase.getModifiers(ModifierType.FLORIST, plant.toString()));
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

  public final void applyMotorbikeModifiers() {
    // If Sneaky Pete, add Motorbike effects
    if (KoLCharacter.isSneakyPete()) {
      this.add(
          ModifierDatabase.getModifiers(
              ModifierType.MOTORBIKE, Preferences.getString("peteMotorbikeTires")));
      this.add(
          ModifierDatabase.getModifiers(
              ModifierType.MOTORBIKE, Preferences.getString("peteMotorbikeGasTank")));
      this.add(
          ModifierDatabase.getModifiers(
              ModifierType.MOTORBIKE, Preferences.getString("peteMotorbikeHeadlight")));
      this.add(
          ModifierDatabase.getModifiers(
              ModifierType.MOTORBIKE, Preferences.getString("peteMotorbikeCowling")));
      this.add(
          ModifierDatabase.getModifiers(
              ModifierType.MOTORBIKE, Preferences.getString("peteMotorbikeMuffler")));
      this.add(
          ModifierDatabase.getModifiers(
              ModifierType.MOTORBIKE, Preferences.getString("peteMotorbikeSeat")));
    }
  }

  public final void applyAdditionalRolloverAdventureModifiers() {
    var resolutionAdv = Preferences.getInteger("_resolutionAdv");
    if (resolutionAdv > 0) {
      this.addDouble(
          DoubleModifier.ADVENTURES,
          resolutionAdv,
          ModifierType.ITEM,
          ItemPool.RESOLUTION_ADVENTUROUS);
    }
    var circadianAdv = Preferences.getInteger("_circadianRhythmsAdventures");
    if (circadianAdv > 0) {
      this.addDouble(
          DoubleModifier.ADVENTURES,
          circadianAdv,
          ModifierType.SKILL,
          SkillPool.RECALL_FACTS_CIRCADIAN_RHYTHMS);
    }
    var hareAdv = Preferences.getInteger("_hareAdv");
    if (hareAdv > 0) {
      this.addDouble(DoubleModifier.ADVENTURES, hareAdv, ModifierType.FAMILIAR, "Wild Hare");
    }
    var gibberAdv = Preferences.getInteger("_gibbererAdv");
    if (gibberAdv > 0) {
      this.addDouble(
          DoubleModifier.ADVENTURES, gibberAdv, ModifierType.FAMILIAR, "Squamous Gibberer");
    }
    var usedBorrowedTime = Preferences.getBoolean("_borrowedTimeUsed");
    if (usedBorrowedTime) {
      this.addDouble(DoubleModifier.ADVENTURES, -20, ModifierType.ITEM, ItemPool.BORROWED_TIME);
    }
  }

  public final void applyAdditionalStomachCapacityModifiers() {
    var usedDistentionPill = Preferences.getBoolean("_distentionPillUsed");
    if (usedDistentionPill) {
      this.addDouble(
          DoubleModifier.STOMACH_CAPACITY, 1, ModifierType.ITEM, ItemPool.DISTENTION_PILL);
    }
    var usedLupineHormones = Preferences.getBoolean("_lupineHormonesUsed");
    if (usedLupineHormones) {
      this.addDouble(
          DoubleModifier.STOMACH_CAPACITY, 3, ModifierType.ITEM, ItemPool.LUPINE_APPETITE_HORMONES);
    }
    var usedSweetTooth = Preferences.getBoolean("_sweetToothUsed");
    if (usedSweetTooth) {
      this.addDouble(DoubleModifier.STOMACH_CAPACITY, 1, ModifierType.ITEM, ItemPool.SWEET_TOOTH);
    }
    var usedVoraciTea = Preferences.getBoolean("_voraciTeaUsed");
    if (usedVoraciTea) {
      this.addDouble(DoubleModifier.STOMACH_CAPACITY, 1, ModifierType.ITEM, ItemPool.VORACI_TEA);
    }
    var pantsgivingFullness = Preferences.getInteger("_pantsgivingFullness");
    if (pantsgivingFullness > 0) {
      this.addDouble(
          DoubleModifier.STOMACH_CAPACITY,
          pantsgivingFullness,
          ModifierType.ITEM,
          ItemPool.PANTSGIVING);
    }
  }

  public final void applyAdditionalSpleenCapacityModifiers() {
    if (Preferences.getInteger("lastStillBeatingSpleen") == KoLCharacter.getAscensions()) {
      this.addDouble(
          DoubleModifier.SPLEEN_CAPACITY, 1, ModifierType.ITEM, ItemPool.STILL_BEATING_SPLEEN);
    }
  }

  public final void applyAdditionalFreeRestModifiers() {
    // Unconscious Collective contributes in G-Lover (e.g.) but not in Standard
    if (StandardRequest.isAllowed(RestrictedItemType.FAMILIARS, "Unconscious Collective")
        && KoLCharacter.ownedFamiliar(FamiliarPool.UNCONSCIOUS_COLLECTIVE).isPresent()) {
      this.addDouble(
          DoubleModifier.FREE_RESTS, 3, ModifierType.TERRARIUM_FAMILIAR, "Unconscious Collective");
    }
    if (StandardRequest.isAllowed(RestrictedItemType.ITEMS, "Distant Woods Getaway Brochure")
        && Preferences.getBoolean("getawayCampsiteUnlocked")) {
      this.addDouble(DoubleModifier.FREE_RESTS, 1, ModifierType.ITEM, ItemPool.GETAWAY_BROCHURE);
    }
    if (InventoryManager.equippedOrInInventory(ItemPool.MOTHERS_NECKLACE)) {
      this.addDouble(
          DoubleModifier.FREE_RESTS, 5, ModifierType.INVENTORY_ITEM, "mother's necklace");
    }
    if (InventoryManager.equippedOrInInventory(ItemPool.CINCHO_DE_MAYO)) {
      this.addDouble(DoubleModifier.FREE_RESTS, 3, ModifierType.INVENTORY_ITEM, "Cincho de Mayo");
    }
    if (InventoryManager.equippedOrInInventory(ItemPool.REPLICA_CINCHO_DE_MAYO)) {
      this.addDouble(
          DoubleModifier.FREE_RESTS, 3, ModifierType.INVENTORY_ITEM, "replica Cincho de Mayo");
    }
    var yamRests = Preferences.getInteger("_mayamRests");
    if (yamRests > 0) {
      this.addDouble(
          DoubleModifier.FREE_RESTS, yamRests, ModifierType.ITEM, ItemPool.MAYAM_CALENDAR);
    }
  }

  public void applySynergies() {
    int synergetic = this.getRawBitmap(BitmapModifier.SYNERGETIC);
    if (synergetic == 0) return; // nothing possible
    for (Entry<String, Integer> entry : ModifierDatabase.getSynergies()) {
      String name = entry.getKey();
      int mask = entry.getValue();
      if ((synergetic & mask) == mask) {
        this.add(ModifierDatabase.getModifiers(ModifierType.SYNERGY, name));
      }
    }
  }

  public void applyFamiliarModifiers(final FamiliarData familiar, AdventureResult famItem) {
    if (KoLConstants.activeEffects.contains(Modifiers.somePigs)) {
      // Under the effect of SOME PIGS, familiar gives no modifiers
      return;
    }

    int weight = familiar.getUncappedWeight();

    if (KoLConstants.activeEffects.contains(FIDOXENE)) {
      weight = Math.max(weight, 20);
    }

    weight += (int) this.getDouble(DoubleModifier.FAMILIAR_WEIGHT);
    weight += (int) this.getDouble(DoubleModifier.HIDDEN_FAMILIAR_WEIGHT);
    weight += (familiar.getFeasted() ? 10 : 0);
    weight += familiar.getSoupWeight();
    // Comma Chameleons gain a passive 5lbs while they are imitating another familiar
    weight +=
        (familiar.getId() == FamiliarPool.CHAMELEON && familiar.getId() != familiar.getEffectiveId()
            ? 5
            : 0);

    double percent = this.getDouble(DoubleModifier.FAMILIAR_WEIGHT_PCT) / 100.0;
    if (percent != 0.0) {
      weight = (int) Math.floor(weight + weight * percent);
    }

    weight = Math.max(1, weight);
    this.lookupFamiliarModifiers(familiar, weight, famItem);
  }

  public void lookupFamiliarModifiers(
      final FamiliarData familiar, int weight, final AdventureResult famItem) {
    int familiarId = familiar.getEffectiveId();
    weight = Math.max(1, weight);
    Modifiers.currentWeight = weight;

    String race = familiar.getEffectiveRace();

    this.add(ModifierDatabase.getModifiers(ModifierType.FAMILIAR, race));
    if (famItem != null) {
      // "fameq" modifiers are generated when "Familiar Effect" is parsed
      // from modifiers.txt
      this.add(ModifierDatabase.getModifiers(ModifierType.FAM_EQ, famItem.getName()));
    }

    int cap = (int) this.getDouble(DoubleModifier.FAMILIAR_WEIGHT_CAP);
    int cappedWeight = (cap == 0) ? weight : Math.min(weight, cap);

    double volleyFactor = 0.0;
    double sombreroFactor = 0.0;

    double effective = cappedWeight * this.getDouble(DoubleModifier.VOLLEYBALL_WEIGHT);
    if (effective == 0.0 && FamiliarDatabase.isVolleyType(familiarId)) {
      effective = weight;
    }
    if (effective != 0.0) {
      double factor = this.getDouble(DoubleModifier.VOLLEYBALL_EFFECTIVENESS);
      // The 0->1 factor for generic familiars conflicts with the JitB
      if (factor == 0.0 && familiarId != FamiliarPool.JACK_IN_THE_BOX) factor = 1.0;
      factor = factor * (2 + effective / 5);
      double tuning;
      if ((tuning = this.getDouble(DoubleModifier.FAMILIAR_TUNING_MUSCLE)) > 0) {
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
      } else if ((tuning = this.getDouble(DoubleModifier.FAMILIAR_TUNING_MYSTICALITY)) > 0) {
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
      } else if ((tuning = this.getDouble(DoubleModifier.FAMILIAR_TUNING_MOXIE)) > 0) {
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
        volleyFactor = factor;
      }
    }

    effective = cappedWeight * this.getDouble(DoubleModifier.SOMBRERO_WEIGHT);
    if (effective == 0.0 && FamiliarDatabase.isSombreroType(familiarId)) {
      effective = weight;
    }
    effective += this.getDouble(DoubleModifier.SOMBRERO_BONUS);
    if (effective != 0.0) {
      double factor = this.getDouble(DoubleModifier.SOMBRERO_EFFECTIVENESS);
      if (factor == 0.0) factor = 1.0;
      // currentML is always >= 4, so we don't need to check for negatives
      int maxStats = 230;
      sombreroFactor =
          Math.min(
              Math.max(factor * (Modifiers.currentML / 4) * (0.1 + 0.005 * effective), 1),
              maxStats);
    }

    if (this.getBoolean(BooleanModifier.VOLLEYBALL_OR_SOMBRERO)) {
      if (volleyFactor > sombreroFactor) {
        this.addDouble(DoubleModifier.EXPERIENCE, volleyFactor, ModifierType.VOLLEYBALL, race);
      } else {
        this.addDouble(DoubleModifier.EXPERIENCE, sombreroFactor, ModifierType.FAMILIAR, race);
      }
    } else {
      if (volleyFactor > 0) {
        this.addDouble(DoubleModifier.EXPERIENCE, volleyFactor, ModifierType.VOLLEYBALL, race);
      }
      if (sombreroFactor > 0) {
        this.addDouble(DoubleModifier.EXPERIENCE, sombreroFactor, ModifierType.FAMILIAR, race);
      }
    }

    effective = cappedWeight * this.getDouble(DoubleModifier.LEPRECHAUN_WEIGHT);
    if (effective == 0.0 && FamiliarDatabase.isMeatDropType(familiarId)) {
      effective = weight;
    }
    if (effective != 0.0) {
      double factor = this.getDouble(DoubleModifier.LEPRECHAUN_EFFECTIVENESS);
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
      this.setBoolean(BooleanModifier.UNDERWATER_FAMILIAR, true);
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
    var effective = cappedWeight * this.getDouble(fairyModifier);

    // If it has no explicit modifier but is the right familiar type, add effect regardless
    if (effective == 0.0 && FamiliarDatabase.isFairyType(familiar.getId(), fairyModifier)) {
      effective = weight;
    }

    if (effective == 0.0) return;

    double factor = this.getDouble(effectivenessModifier);
    // The 0->1 factor for generic familiars conflicts with the JitB
    if (factor == 0.0 && familiar.getId() != FamiliarPool.JACK_IN_THE_BOX) factor = 1.0;

    this.addDouble(
        modifier,
        factor * (Math.sqrt(55 * effective) + effective - 3),
        ModifierType.FAMILIAR,
        familiar.getRace());
  }

  public void applyMinstrelModifiers(final int level, AdventureResult instrument) {
    String instrumentName = instrument.getName();
    Lookup lookup = new Lookup(ModifierType.CLANCY, instrumentName);
    Modifiers imods = ModifierDatabase.getModifiers(lookup);

    double effective = imods.getDouble(DoubleModifier.VOLLEYBALL_WEIGHT);
    if (effective != 0.0) {
      double factor = 2 + effective / 5;
      this.addDouble(DoubleModifier.EXPERIENCE, factor, lookup);
    }

    effective = imods.getDouble(DoubleModifier.FAIRY_WEIGHT);
    if (effective != 0.0) {
      double factor = Math.sqrt(55 * effective) + effective - 3;
      this.addDouble(DoubleModifier.ITEMDROP, factor, lookup);
    }

    this.addDouble(
        DoubleModifier.HP_REGEN_MIN, imods.getDouble(DoubleModifier.HP_REGEN_MIN), lookup);
    this.addDouble(
        DoubleModifier.HP_REGEN_MAX, imods.getDouble(DoubleModifier.HP_REGEN_MAX), lookup);
    this.addDouble(
        DoubleModifier.MP_REGEN_MIN, imods.getDouble(DoubleModifier.MP_REGEN_MIN), lookup);
    this.addDouble(
        DoubleModifier.MP_REGEN_MAX, imods.getDouble(DoubleModifier.MP_REGEN_MAX), lookup);
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
      Modifiers ensorcelMods = ModifierDatabase.getModifiers(ModifierType.ENSORCEL, phylum);
      if (ensorcelMods != null) {
        this.addDouble(
            DoubleModifier.MEATDROP,
            ensorcelMods.getDouble(DoubleModifier.MEATDROP) * 0.25,
            ModifierType.ITEM,
            ItemPool.VAMPYRIC_CLOAKE);
        this.addDouble(
            DoubleModifier.ITEMDROP,
            ensorcelMods.getDouble(DoubleModifier.ITEMDROP) * 0.25,
            ModifierType.ITEM,
            ItemPool.VAMPYRIC_CLOAKE);
        this.addDouble(
            DoubleModifier.CANDYDROP,
            ensorcelMods.getDouble(DoubleModifier.CANDYDROP) * 0.25,
            ModifierType.ITEM,
            ItemPool.VAMPYRIC_CLOAKE);
      }
    }
  }

  public void applyPathModifiers() {
    if (KoLCharacter.inElevenThingIHateAboutU()) {
      if (originalLookup == null || originalLookup.type != ModifierType.ITEM) {
        return;
      }

      int itemId;
      String name;
      if (this.originalLookup.getKey().isInt()) {
        itemId = this.originalLookup.getIntKey();
        name = ItemDatabase.getItemName(this.originalLookup.getIntKey());
      } else {
        itemId = ItemDatabase.getItemId(this.originalLookup.getStringKey());
        name = this.originalLookup.getStringKey();
      }

      if (ItemDatabase.getConsumptionType(itemId) != KoLConstants.ConsumptionType.POTION
          || name == null) {
        return;
      }

      int delta = KoLCharacter.getEyeosity(name) * 2 - KoLCharacter.getEweosity(name);
      if (delta == 0) {
        return;
      }

      if (this.variable) {
        this.addExpression(
            new Indexed<>(
                DoubleModifier.EFFECT_DURATION,
                ModifierExpression.getInstance(
                    delta + "*path(" + AscensionPath.Path.ELEVEN_THINGS.name + ')',
                    AscensionPath.Path.ELEVEN_THINGS.name)));
      } else {
        this.addDouble(
            DoubleModifier.EFFECT_DURATION,
            delta,
            ModifierType.PATH,
            AscensionPath.Path.ELEVEN_THINGS.name);
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

  @Override
  public String toString() {
    return this.getString(StringModifier.MODIFIERS);
  }
}
