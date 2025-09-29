package net.sourceforge.kolmafia;

import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHatTrickHats;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withLevel;
import static internal.helpers.Player.withLocation;
import static internal.helpers.Player.withMP;
import static internal.helpers.Player.withOverrideModifiers;
import static internal.helpers.Player.withParadoxicity;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import internal.helpers.Cleanups;
import internal.helpers.Player;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.BooleanModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.modifiers.Lookup;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.LatteRequest;
import net.sourceforge.kolmafia.request.LatteRequest.Latte;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ModifiersTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("ModifiersTest");
    Preferences.reset("ModifiersTest");
  }

  private Double fairyFunction(final double weight) {
    return Math.max(Math.sqrt(55 * weight) + weight - 3, 0);
  }

  private Double lepFunction(final double weight) {
    return 2 * fairyFunction(weight);
  }

  @Test
  public void patriotShieldClassModifiers() {
    // Wide-reaching unit test for getModifiers
    var cleanup = withClass(AscensionClass.AVATAR_OF_JARLSBERG);
    try (cleanup) {
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, ItemPool.PATRIOT_SHIELD);

      // Always has
      assertEquals(3, mods.getDouble(DoubleModifier.EXPERIENCE));

      // Has because of class
      assertEquals(5.0, mods.getDouble(DoubleModifier.MP_REGEN_MIN));
      assertEquals(6.0, mods.getDouble(DoubleModifier.MP_REGEN_MAX));
      assertEquals(20.0, mods.getDouble(DoubleModifier.SPELL_DAMAGE));

      // Does not have because of class
      assertEquals(0, mods.getDouble(DoubleModifier.HP_REGEN_MAX));
      assertEquals(0, mods.getDouble(DoubleModifier.HP_REGEN_MIN));
      assertEquals(0, mods.getDouble(DoubleModifier.WEAPON_DAMAGE));
      assertEquals(0, mods.getDouble(DoubleModifier.DAMAGE_REDUCTION));
      assertEquals(0, mods.getDouble(DoubleModifier.FAMILIAR_WEIGHT));
      assertEquals(0, mods.getDouble(DoubleModifier.RANGED_DAMAGE));
      assertFalse(mods.getBoolean(BooleanModifier.FOUR_SONGS));
      assertEquals(0, mods.getDouble(DoubleModifier.COMBAT_MANA_COST));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7})
  public void tuesdayRubyModifiers(int date) {
    // Wide-reaching unit test for getModifiers
    var cleanup = withDay(2017, Month.MAY, date);

    // January 1st 2017 was a Monday, so we can use day of the week and day of the month as the same
    // value
    var dotw = DayOfWeek.of(date);

    try (cleanup) {
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, "Tuesday's Ruby");

      assertThat(
          mods.getDouble(DoubleModifier.MEATDROP), equalTo(dotw == DayOfWeek.SUNDAY ? 5.0 : 0.0));
      assertThat(
          mods.getDouble(DoubleModifier.MUS_PCT), equalTo(dotw == DayOfWeek.MONDAY ? 5.0 : 0.0));
      assertThat(
          mods.getDouble(DoubleModifier.MP_REGEN_MIN),
          equalTo(dotw == DayOfWeek.TUESDAY ? 3.0 : 0.0));
      assertThat(
          mods.getDouble(DoubleModifier.MP_REGEN_MAX),
          equalTo(dotw == DayOfWeek.TUESDAY ? 7.0 : 0.0));
      assertThat(
          mods.getDouble(DoubleModifier.MYS_PCT), equalTo(dotw == DayOfWeek.WEDNESDAY ? 5.0 : 0.0));
      assertThat(
          mods.getDouble(DoubleModifier.ITEMDROP), equalTo(dotw == DayOfWeek.THURSDAY ? 5.0 : 0.0));
      assertThat(
          mods.getDouble(DoubleModifier.MOX_PCT), equalTo(dotw == DayOfWeek.FRIDAY ? 5.0 : 0.0));
      assertThat(
          mods.getDouble(DoubleModifier.HP_REGEN_MIN),
          equalTo(dotw == DayOfWeek.SATURDAY ? 3.0 : 0.0));
      assertThat(
          mods.getDouble(DoubleModifier.HP_REGEN_MAX),
          equalTo(dotw == DayOfWeek.SATURDAY ? 7.0 : 0.0));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11})
  public void intrinsicSpicinessModifiers(int level) {
    var cleanups = new Cleanups(withClass(AscensionClass.SAUCEROR), withLevel(level));

    try (cleanups) {
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.SKILL, "Intrinsic Spiciness");
      assertEquals(Math.min(level, 10), mods.getDouble(DoubleModifier.SAUCE_SPELL_DAMAGE));
    }
  }

  @Test
  public void correctlyCalculatesCappedCombatRate() {
    Modifiers mod = new Modifiers();
    mod.addDouble(DoubleModifier.COMBAT_RATE, 25, ModifierType.NONE, "");
    assertEquals(25, mod.getDouble(DoubleModifier.RAW_COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, 7, ModifierType.NONE, "");
    assertEquals(26, mod.getDouble(DoubleModifier.COMBAT_RATE));
    assertEquals(32, mod.getDouble(DoubleModifier.RAW_COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, 9, ModifierType.NONE, "");
    assertEquals(28, mod.getDouble(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, 9, ModifierType.NONE, "");
    assertEquals(30, mod.getDouble(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, 50, ModifierType.NONE, "");
    assertEquals(35, mod.getDouble(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, 50, ModifierType.NONE, "");
    assertEquals(35, mod.getDouble(DoubleModifier.COMBAT_RATE));
    assertEquals(150, mod.getDouble(DoubleModifier.RAW_COMBAT_RATE));

    mod = new Modifiers();
    mod.addDouble(DoubleModifier.COMBAT_RATE, -25, ModifierType.NONE, "");
    assertEquals(-25, mod.getDouble(DoubleModifier.RAW_COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, -7, ModifierType.NONE, "");
    assertEquals(-26, mod.getDouble(DoubleModifier.COMBAT_RATE));
    assertEquals(-32, mod.getDouble(DoubleModifier.RAW_COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, -9, ModifierType.NONE, "");
    assertEquals(-28, mod.getDouble(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, -9, ModifierType.NONE, "");
    assertEquals(-30, mod.getDouble(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, -50, ModifierType.NONE, "");
    assertEquals(-35, mod.getDouble(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, -50, ModifierType.NONE, "");
    assertEquals(-35, mod.getDouble(DoubleModifier.COMBAT_RATE));
    assertEquals(-150, mod.getDouble(DoubleModifier.RAW_COMBAT_RATE));
  }

  @Test
  public void passivesIgnoreGsInGLover() {
    Modifiers mods = new Modifiers();
    var cleanups = new Cleanups(withPath(Path.GLOVER), withSkill(SkillPool.STEEL_LIVER));
    try (cleanups) {
      mods.applyPassiveModifiers(/* debug= */ true);
      assertEquals(5, mods.getDouble(DoubleModifier.LIVER_CAPACITY));
    }
    // Remove liver from passive skill cache.
    mods.applyPassiveModifiers(/* debug= */ true);
  }

  public static Stream<Arguments> getsRightModifiersNakedHatrack() {
    return Stream.of(
        Arguments.of(FamiliarPool.HATRACK, DoubleModifier.HATDROP),
        Arguments.of(FamiliarPool.SCARECROW, DoubleModifier.PANTSDROP));
  }

  @ParameterizedTest
  @MethodSource
  public void getsRightModifiersNakedHatrack(int famId, DoubleModifier mod) {
    var cleanups = new Cleanups(withFamiliar(famId));

    try (cleanups) {
      Modifiers familiarMods = new Modifiers();
      var fam = KoLCharacter.getFamiliar();
      fam.setExperience(400);
      familiarMods.applyFamiliarModifiers(fam, EquipmentRequest.UNEQUIP);
      KoLCharacter.recalculateAdjustments();

      assertThat(fam.getModifiedWeight(), equalTo(1));
      assertThat(familiarMods.getDouble(mod), closeTo(50, 0.001));
    }
  }

  @Nested
  class Fixodene {
    @Test
    void fixodeneConsideredInFamiliarModifiers() {
      var cleanups = withEffect("Fidoxene");

      try (cleanups) {
        Modifiers familiarMods = new Modifiers();
        var familiar = FamiliarData.registerFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 0);

        familiarMods.applyFamiliarModifiers(familiar, null);

        assertThat(familiarMods.getDouble(DoubleModifier.ITEMDROP), closeTo(50.166, 0.001));
      }
    }

    @Test
    void fixodeneConsideredInFamiliarModifiersNotExceedingTwenty() {
      var cleanups = withEffect("Fidoxene");

      try (cleanups) {
        Modifiers familiarMods = new Modifiers();
        var familiar = FamiliarData.registerFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 400);

        familiarMods.applyFamiliarModifiers(familiar, null);

        assertThat(familiarMods.getDouble(DoubleModifier.ITEMDROP), closeTo(50.166, 0.001));
      }
    }
  }

  @Nested
  class SquintChampagne {
    @BeforeAll
    public static void setup() {
      Preferences.reset("SquintChampagne");
    }

    @Test
    public void squintDoublesEffect() {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.STEELY_EYED_SQUINT),
              withEffect(EffectPool.SYNTHESIS_COLLECTION));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.ITEMDROP), equalTo(300.0));
      }
    }

    @Test
    public void squintDoublesPassive() {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.STEELY_EYED_SQUINT), withSkill(SkillPool.OBSERVATIOGN));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.ITEMDROP), equalTo(20.0));
      }
    }

    @Test
    public void champagneDoublesEffect() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.WEAPON, ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 11),
              withEffect(EffectPool.SYNTHESIS_COLLECTION));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.ITEMDROP), equalTo(300.0));
      }
    }

    @Test
    public void champagneDoublesPassive() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.WEAPON, ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 11),
              withSkill(SkillPool.OBSERVATIOGN));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.ITEMDROP), equalTo(20.0));
      }
    }

    @Test
    public void squintDoesntDoubleMummery() {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.STEELY_EYED_SQUINT),
              withProperty("_mummeryMods", "Item Drop: +25"));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.ITEMDROP), equalTo(25.0));
      }
    }

    @Test
    public void squintDoublesUmbrella() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.OFFHAND, ItemPool.UNBREAKABLE_UMBRELLA),
              withProperty("umbrellaState", "bucket style"),
              withEffect(EffectPool.STEELY_EYED_SQUINT));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        var item = mods.getDouble(DoubleModifier.ITEMDROP);
        assertThat(item, equalTo(50.0));
      }
    }

    @Test
    public void squintAndChampagneStack() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.WEAPON, ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 11),
              withEffect(EffectPool.STEELY_EYED_SQUINT),
              withEffect(EffectPool.SYNTHESIS_COLLECTION));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.ITEMDROP), equalTo(600.0));
      }
    }

    @Test
    public void squintDoublesOtoscope() {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.STEELY_EYED_SQUINT),
              withOverrideModifiers(ModifierType.GENERATED, "fightMods", "Item Drop: +200"));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.ITEMDROP), equalTo(400.0));
      }
    }

    @Test
    public void champagneDoesntDoubleOtoscope() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.WEAPON, ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 11),
              withOverrideModifiers(ModifierType.GENERATED, "fightMods", "Item Drop: +200"));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.ITEMDROP), equalTo(200.0));
      }
    }

    @Test
    public void squintAndChampagneDoublesOtoscopeOnce() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.WEAPON, ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 11),
              withEffect(EffectPool.STEELY_EYED_SQUINT),
              withOverrideModifiers(ModifierType.GENERATED, "fightMods", "Item Drop: +200"));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.ITEMDROP), equalTo(400.0));
      }
    }
  }

  @Nested
  class ElementalMultipliers {
    @BeforeAll
    public static void setup() {
      Preferences.reset("ElementalMultipliers");
    }

    @Test
    public void bendinHellDoublesEffect() {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.BENDIN_HELL), withEffect(EffectPool.PAINTED_ON_BIKINI));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.SLEAZE_DAMAGE), equalTo(100.0));
        assertThat(mods.getDouble(DoubleModifier.SLEAZE_SPELL_DAMAGE), equalTo(100.0));
      }
    }

    @Test
    public void dirtyPearDoublesEffect() {
      var cleanups =
          new Cleanups(withEffect(EffectPool.DIRTY_PEAR), withEffect(EffectPool.PAINTED_ON_BIKINI));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.SLEAZE_DAMAGE), equalTo(100.0));
        assertThat(mods.getDouble(DoubleModifier.SLEAZE_SPELL_DAMAGE), equalTo(100.0));
      }
    }

    @Test
    public void bendinHellAndDirtyPearStack() {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.BENDIN_HELL),
              withEffect(EffectPool.DIRTY_PEAR),
              withEffect(EffectPool.PAINTED_ON_BIKINI));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.SLEAZE_DAMAGE), equalTo(200.0));
        assertThat(mods.getDouble(DoubleModifier.SLEAZE_SPELL_DAMAGE), equalTo(200.0));
      }
    }

    @Test
    void shadowRiftFifthsItemDrop() {
      var cleanups = new Cleanups(withEffect(EffectPool.BLUE_TONGUE), withLocation("Shadow Rift"));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.getDouble(DoubleModifier.ITEMDROP), equalTo(6.0));
      }
    }
  }

  @Nested
  class ExperienceMultipliers {
    @BeforeAll
    public static void setup() {
      Preferences.reset("ExperienceMultipliers");
    }

    @Test
    public void makeshiftGarbageShirtDoublesEffect() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.SHIRT, ItemPool.MAKESHIFT_GARBAGE_SHIRT),
              withProperty("garbageShirtCharge", 37),
              withEffect(EffectPool.FEELING_LOST));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        // 3 from garbage shirt, 30 from Feeling Lost, *2 = 66
        assertThat(mods.getDouble(DoubleModifier.EXPERIENCE), equalTo(66.0));
      }
    }
  }

  @Nested
  class BuffedHP {
    @AfterEach
    public void afterEach() {
      EquipmentManager.resetEquipment();
    }

    @Test
    public void correctlyCalculatesSealClubberMaximumHP() {
      var cleanups = new Cleanups(withClass(AscensionClass.SEAL_CLUBBER), withStats(100, 100, 100));
      try (cleanups) {
        // Buffed MUS = Base MUS + mod(MUS) + ceiling(Base MUS * mod(MUS_PCT)/100.0)
        // Base HP = Buffed MUS + 3
        // C = 1.5 if MUS class, otherwise 1.0
        // Buffed HP = ceiling(Base HP * (C + mod(HP_PCT)/100.0) + mod(HP))

        Modifiers mods = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        var stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(155, stats.get(DerivedModifier.BUFFED_HP));

        // viking helmet: (+1 muscle)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.VIKING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(101, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(156, stats.get(DerivedModifier.BUFFED_HP));

        // reinforced beaded headband (+40 HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.REINFORCED_BEADED_HEADBAND));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(195, stats.get(DerivedModifier.BUFFED_HP));

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(258, stats.get(DerivedModifier.BUFFED_HP));
      }
    }

    @Test
    public void correctlyCalculatesPastamancerMaximumHP() {
      var cleanups = new Cleanups(withClass(AscensionClass.PASTAMANCER), withStats(100, 100, 100));
      try (cleanups) {
        // Buffed MUS = Base MUS + mod(MUS) + ceiling(Base MUS * mod(MUS_PCT)/100.0)
        // Base HP = Buffed MUS + 3
        // C = 1.5 if MUS class, otherwise 1.0
        // Buffed HP = ceiling(Base HP * (C + mod(HP_PCT)/100.0) + mod(HP))

        Modifiers mods = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        var stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(103, stats.get(DerivedModifier.BUFFED_HP));

        // viking helmet: (+1 muscle)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.VIKING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(101, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(104, stats.get(DerivedModifier.BUFFED_HP));

        // reinforced beaded headband (+40 HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.REINFORCED_BEADED_HEADBAND));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(143, stats.get(DerivedModifier.BUFFED_HP));

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(206, stats.get(DerivedModifier.BUFFED_HP));
      }
    }

    @Test
    public void correctlyCalculatesVampyreMaximumHP() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.VAMPYRE),
              withPath(Path.DARK_GYFFTE),
              withStats(100, 100, 100),
              withProperty("darkGyfftePoints", 0));
      try (cleanups) {
        // Base HP = Base MUS
        // Buffed HP = max(Base MUS, Base HP + mod(HP))
        // Note that every Vampyre gets 20 additional base HP, plus another 20 for every DG Point

        Modifiers mods = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        var stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(120, stats.get(DerivedModifier.BUFFED_HP));

        // viking helmet: (+1 muscle)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.VIKING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(101, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(120, stats.get(DerivedModifier.BUFFED_HP));

        // reinforced beaded headband (+40 HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.REINFORCED_BEADED_HEADBAND));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(160, stats.get(DerivedModifier.BUFFED_HP));

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(120, stats.get(DerivedModifier.BUFFED_HP));
      }
    }

    @Test
    public void correctlyCalculatesYouRobotMaximumHP() {
      var cleanups = new Cleanups(withPath(Path.YOU_ROBOT), withStats(100, 100, 100));
      try (cleanups) {
        // Base HP = 30
        // Buffed HP = Base HP + mod(HP)

        Modifiers mods = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        var stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(30, stats.get(DerivedModifier.BUFFED_HP));

        // viking helmet: (+1 muscle)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.VIKING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(101, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(30, stats.get(DerivedModifier.BUFFED_HP));

        // reinforced beaded headband (+40 HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.REINFORCED_BEADED_HEADBAND));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(70, stats.get(DerivedModifier.BUFFED_HP));

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(30, stats.get(DerivedModifier.BUFFED_HP));
      }
    }

    @Test
    public void correctlyCalculatesGreyYouMaximumHP() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.GREY_GOO), withStats(100, 100, 100), withHP(176, 176, 176));
      try (cleanups) {
        // Base HP = (starting value + absorptions + currently worn equipment)
        // Buffed HP = Base HP - currently worn equipment + mod(HP)

        Modifiers mods = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        var stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(176, stats.get(DerivedModifier.BUFFED_HP));

        // viking helmet: (+1 muscle)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.VIKING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(101, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(176, stats.get(DerivedModifier.BUFFED_HP));

        // reinforced beaded headband (+40 HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.REINFORCED_BEADED_HEADBAND));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(176, stats.get(DerivedModifier.BUFFED_HP));

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(176, stats.get(DerivedModifier.BUFFED_HP));
      }
    }

    @Test
    public void correctlySpeculatesGreyYouMaximumHP() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.GREY_GOO),
              withStats(100, 100, 100),
              withHP(216, 216, 216),
              withEquipped(Slot.HAT, ItemPool.REINFORCED_BEADED_HEADBAND));
      try (cleanups) {
        // Base HP = (starting value + absorptions + currently worn equipment)
        // Buffed HP = Base HP - currently worn equipment + mod(HP)

        // We are starting with a reinforced beaded headband, which provides +40 HP
        // Therefore, our actual "baseHP" is 176

        Modifiers current = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        assertEquals(40, current.getDouble(DoubleModifier.HP));

        var currentStats = current.predict();
        assertEquals(100, currentStats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(216, currentStats.get(DerivedModifier.BUFFED_HP));

        // Make some modifiers to speculate with
        Modifiers speculate = new Modifiers(current);
        assertEquals(40, speculate.getDouble(DoubleModifier.HP));
        // Suppose we want to replace the reinforced beaded headband (+40 HP)
        // with a nurse's hat (+300 HP)
        speculate.setDouble(DoubleModifier.HP, 300.0);

        var speculateStats = speculate.predict();
        assertEquals(476, speculateStats.get(DerivedModifier.BUFFED_HP));
      }
    }
  }

  @Nested
  class BuffedMP {
    @AfterEach
    public void afterEach() {
      EquipmentManager.resetEquipment();
    }

    @Test
    public void correctlyCalculatesSaucerorMaximumMP() {
      var cleanups = new Cleanups(withClass(AscensionClass.SAUCEROR), withStats(100, 100, 100));
      try (cleanups) {
        // Buffed MYS = Base MYS + mod(MYS) + ceiling(Base MYS * mod(MYS_PCT)/100.0)
        // Base MP = Buffed MYS
        // C = 1.5 if MYS class, otherwise 1.0
        // Buffed MP = ceiling(Base MP * (C + mod(MP_PCT)/100.0) + mod(MP))

        Modifiers mods = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        var stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(150, stats.get(DerivedModifier.BUFFED_MP));

        // fuzzy earmuffs: (+11 myst)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.FUZZY_EARMUFFS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(111, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(167, stats.get(DerivedModifier.BUFFED_MP));

        // beer helmet (+40 MP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.BEER_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(190, stats.get(DerivedModifier.BUFFED_MP));

        EquipmentManager.setEquipment(Slot.HAT, EquipmentRequest.UNEQUIP);

        // Cargo Cultist Shorts (+6 myst, +66% MP)
        EquipmentManager.setEquipment(Slot.PANTS, ItemPool.get(ItemPool.CARGO_CULTIST_SHORTS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(106, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(229, stats.get(DerivedModifier.BUFFED_MP));
      }
    }

    @Test
    public void correctlyCalculatesTurtleTamerMaximumMP() {
      var cleanups = new Cleanups(withClass(AscensionClass.TURTLE_TAMER), withStats(100, 100, 100));
      try (cleanups) {
        // Buffed MYS = Base MYS + mod(MYS) + ceiling(Base MYS * mod(MYS_PCT)/100.0)
        // Base MP = Buffed MYS
        // C = 1.5 if MYS class, otherwise 1.0
        // Buffed MP = ceiling(Base MP * (C + mod(MP_PCT)/100.0) + mod(MP))

        Modifiers mods = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        var stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MP));

        // fuzzy earmuffs: (+11 myst)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.FUZZY_EARMUFFS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(111, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(111, stats.get(DerivedModifier.BUFFED_MP));

        // beer helmet (+40 MP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.BEER_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(140, stats.get(DerivedModifier.BUFFED_MP));

        EquipmentManager.setEquipment(Slot.HAT, EquipmentRequest.UNEQUIP);

        // Cargo Cultist Shorts (+6 myst, +66% MP)
        EquipmentManager.setEquipment(Slot.PANTS, ItemPool.get(ItemPool.CARGO_CULTIST_SHORTS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(106, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(176, stats.get(DerivedModifier.BUFFED_MP));
      }
    }

    @Test
    public void correctlyCalculatesGreyYouMaximumMP() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.GREY_GOO), withStats(100, 100, 100), withMP(126, 126, 126));
      try (cleanups) {
        // Base MP = (starting value + absorptions + currently worn equipment)
        // Buffed MP = Base MP - currently worn equipment + mod(HP)

        Modifiers mods = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        var stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(126, stats.get(DerivedModifier.BUFFED_MP));

        // fuzzy earmuffs: (+11 myst)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.FUZZY_EARMUFFS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(111, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(126, stats.get(DerivedModifier.BUFFED_MP));

        // beer helmet (+40 MP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.BEER_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(126, stats.get(DerivedModifier.BUFFED_MP));

        EquipmentManager.setEquipment(Slot.HAT, EquipmentRequest.UNEQUIP);

        // Cargo Cultist Shorts (+6 myst, +66% MP)
        EquipmentManager.setEquipment(Slot.PANTS, ItemPool.get(ItemPool.CARGO_CULTIST_SHORTS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(106, stats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(126, stats.get(DerivedModifier.BUFFED_MP));
      }
    }

    @Test
    public void correctlySpeculatesGreyYouMaximumMP() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.GREY_GOO),
              withStats(100, 100, 100),
              withMP(126, 126, 126),
              withEquipped(Slot.HAT, ItemPool.BEER_HELMET));
      try (cleanups) {
        // Base MP = (starting value + absorptions + currently worn equipment)
        // Buffed MP = Base MP - currently worn equipment + mod(MP)

        // We are starting with a beer helmet, which provides +40 MP
        // Therefore, our actual "baseMP" is 86

        Modifiers current = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        assertEquals(40, current.getDouble(DoubleModifier.MP));

        var currentStats = current.predict();
        assertEquals(100, currentStats.get(DerivedModifier.BUFFED_MYS));
        assertEquals(126, currentStats.get(DerivedModifier.BUFFED_MP));

        // Make some modifiers to speculate with
        Modifiers speculate = new Modifiers(current);
        assertEquals(40, speculate.getDouble(DoubleModifier.MP));
        // Suppose we want to replace the beer helmet (+40 HP)
        // with Covers-Your-Head (+100 MP)
        speculate.setDouble(DoubleModifier.MP, 100.0);

        var speculateStats = speculate.predict();
        assertEquals(186, speculateStats.get(DerivedModifier.BUFFED_MP));
      }
    }

    @Test
    public void correctlyCalculatesMoxieControlledMaximumMP() {
      // moxie magnet: Moxie Controls MP
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.DISCO_BANDIT),
              withStats(100, 100, 150),
              withEquipped(Slot.ACCESSORY1, "moxie magnet"));
      try (cleanups) {
        // Buffed MOX = Base MOX + mod(MOX) + ceiling(Base MOX * mod(MOX_PCT)/100.0)
        // Base MP = Buffed MUS
        // C = 1.5 if MYS class, otherwise 1.0
        // Buffed MP = ceiling(Base MP * (C + mod(MP_PCT)/100.0) + mod(MP))

        Modifiers mods = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        var stats = mods.predict();
        assertEquals(150, stats.get(DerivedModifier.BUFFED_MOX));
        assertEquals(150, stats.get(DerivedModifier.BUFFED_MP));

        // Disco 'Fro Pick (+11 mox)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.DISCO_FRO_PICK));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(161, stats.get(DerivedModifier.BUFFED_MOX));
        assertEquals(161, stats.get(DerivedModifier.BUFFED_MP));

        // beer helmet (+40 MP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.BEER_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(150, stats.get(DerivedModifier.BUFFED_MOX));
        assertEquals(190, stats.get(DerivedModifier.BUFFED_MP));

        // training helmet: (+25% mox)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.TRAINING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(188, stats.get(DerivedModifier.BUFFED_MOX));
        assertEquals(188, stats.get(DerivedModifier.BUFFED_MP));

        EquipmentManager.setEquipment(Slot.HAT, EquipmentRequest.UNEQUIP);

        // Cargo Cultist Shorts (+6 mox, +66% MP)
        EquipmentManager.setEquipment(Slot.PANTS, ItemPool.get(ItemPool.CARGO_CULTIST_SHORTS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(156, stats.get(DerivedModifier.BUFFED_MOX));
        assertEquals(259, stats.get(DerivedModifier.BUFFED_MP));
      }
    }
  }

  @Nested
  class Noobcore {
    @BeforeAll
    public static void beforeAll() {
      Preferences.reset("noob");
    }

    @Test
    public void noobUnderstandsLatteEnchantments() {
      var cleanups =
          new Cleanups(
              withPath(Path.GELATINOUS_NOOB),
              withFamiliar(FamiliarPool.EMO_SQUID),
              withProperty("latteModifier", ""),
              withEquipped(Slot.OFFHAND, "latte lovers member's mug"));
      try (cleanups) {
        FamiliarData familiar = KoLCharacter.getFamiliar();
        familiar.setExperience(400);
        assertEquals(20, familiar.getWeight());
        assertEquals(20, familiar.getModifiedWeight());

        Latte[] ingredients = LatteRequest.parseIngredients("rawhide", "cajun", "cinnamon");
        String[] mods = Arrays.stream(ingredients).map(Latte::getModifier).toArray(String[]::new);
        LatteRequest.setLatteEnchantments(mods);

        String expected =
            "Familiar Weight: 5, Meat Drop: 40, Experience (Moxie): 1, Moxie Percent: 5, Pickpocket Chance: 5";
        assertEquals(expected, Preferences.getString("latteModifier"));

        // Modifiers set "override" modifiers for the latte mug
        Modifiers latteModifiers =
            ModifierDatabase.getModifiers(ModifierType.ITEM, "[" + ItemPool.LATTE_MUG + "]");
        assertEquals(5, latteModifiers.getDouble(DoubleModifier.FAMILIAR_WEIGHT));
        assertEquals(40, latteModifiers.getDouble(DoubleModifier.MEATDROP));
        assertEquals(1, latteModifiers.getDouble(DoubleModifier.MOX_EXPERIENCE));
        assertEquals(5, latteModifiers.getDouble(DoubleModifier.MOX_PCT));
        assertEquals(5, latteModifiers.getDouble(DoubleModifier.PICKPOCKET_CHANCE));

        // Verify that KoLCharacter applied the mods
        Modifiers currentModifiers = KoLCharacter.getCurrentModifiers();
        assertEquals(5, currentModifiers.getDouble(DoubleModifier.FAMILIAR_WEIGHT));
        assertEquals(40, currentModifiers.getDouble(DoubleModifier.MEATDROP));
        assertEquals(1, currentModifiers.getDouble(DoubleModifier.MOX_EXPERIENCE));
        assertEquals(5, currentModifiers.getDouble(DoubleModifier.MOX_PCT));
        assertEquals(5, currentModifiers.getDouble(DoubleModifier.PICKPOCKET_CHANCE));

        // Verify the familiar is heavier
        assertEquals(25, familiar.getModifiedWeight());
      }
    }
  }

  @Nested
  class Voter {
    @BeforeAll
    public static void beforeAll() {
      Preferences.reset("voter");
    }

    @Test
    void canEvaluateExperienceModifiers() {
      String setting = "Meat Drop: +30, Experience (familiar): +2, Experience (Muscle): +4";
      Lookup lookup = new Lookup(ModifierType.LOCAL_VOTE, "");

      Modifiers mods = ModifierDatabase.parseModifiers(lookup, setting);
      assertEquals(30, mods.getDouble(DoubleModifier.MEATDROP));
      assertEquals(2, mods.getDouble(DoubleModifier.FAMILIAR_EXP));
      assertEquals(4, mods.getDouble(DoubleModifier.MUS_EXPERIENCE));

      Modifiers evaluated = ModifierDatabase.evaluatedModifiers(lookup, setting);
      assertEquals(30, evaluated.getDouble(DoubleModifier.MEATDROP));
      assertEquals(2, evaluated.getDouble(DoubleModifier.FAMILIAR_EXP));
      assertEquals(4, evaluated.getDouble(DoubleModifier.MUS_EXPERIENCE));

      var cleanups = new Cleanups(withProperty("_voteModifier", setting));
      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers current = KoLCharacter.getCurrentModifiers();
        assertEquals(30, current.getDouble(DoubleModifier.MEATDROP));
        assertEquals(2, current.getDouble(DoubleModifier.FAMILIAR_EXP));
        assertEquals(4, current.getDouble(DoubleModifier.MUS_EXPERIENCE));
      }
    }
  }

  @ParameterizedTest
  @CsvSource({
    FamiliarPool.DODECAPEDE + ", -5.0",
    FamiliarPool.ALIEN + ", 5.0",
  })
  public void correctlyAppliesAmphibianSympathyToDodecapede(
      final int familiar, final double weightModifier) {
    var cleanups = new Cleanups(withFamiliar(familiar), withSkill("Amphibian Sympathy"));
    try (cleanups) {
      KoLCharacter.recalculateAdjustments(false);
      Modifiers current = KoLCharacter.getCurrentModifiers();

      assertThat(current.getDouble(DoubleModifier.FAMILIAR_WEIGHT), equalTo(weightModifier));
    }

    KoLCharacter.recalculateAdjustments(false);
    Modifiers current = KoLCharacter.getCurrentModifiers();

    assertThat(current.getDouble(DoubleModifier.FAMILIAR_WEIGHT), equalTo(0.0));
  }

  @ParameterizedTest
  @CsvSource({
    "Amphibian Sympathy, true",
    "Disco Greed, true",
    "Expert Panhandling, true",
    "Slimy Sinews, true",
    "Mad Looting Skillz, false",
    "Overdeveloped Sense of Self Preservation, false",
  })
  public void identifiesVariableModifiers(String skillName, boolean variable) {
    ModifierDatabase.ensureModifierDatabaseInitialised();
    assertThat(
        ModifierDatabase.getModifiers(ModifierType.SKILL, skillName).variable, equalTo(variable));
  }

  @Nested
  class FairyFamiliars {
    private Modifiers getFamiliarMods(final int weight) {
      Modifiers familiarMods = new Modifiers();
      var fam = KoLCharacter.getFamiliar();
      fam.setExperience(weight * weight);
      familiarMods.applyFamiliarModifiers(fam, EquipmentRequest.UNEQUIP);
      KoLCharacter.recalculateAdjustments();
      return familiarMods;
    }

    @ParameterizedTest
    @CsvSource({
      FamiliarPool.BABY_GRAVY_FAIRY + ", 1.0",
      FamiliarPool.JUMPSUITED_HOUND_DOG + ", 1.25",
      FamiliarPool.WIZARD_ACTION_FIGURE + ", 0.3333"
    })
    public void simpleFairies(final int familiarId, final double effectiveness) {
      var cleanups = new Cleanups(withFamiliar(familiarId));
      try (cleanups) {
        var weight = 20;
        var familiarMods = getFamiliarMods(weight);
        assertThat(
            familiarMods.getDouble(DoubleModifier.ITEMDROP),
            closeTo(fairyFunction(weight * effectiveness), 0.001));
      }
    }

    public static Stream<Arguments> otherFairies() {
      return Stream.of(
          Arguments.of(FamiliarPool.VAMPIRE_VINTNER, DoubleModifier.BOOZEDROP, 1.5, 0.0),
          Arguments.of(FamiliarPool.PEPPERMINT_RHINO, DoubleModifier.CANDYDROP, 1.0, 1.0),
          Arguments.of(FamiliarPool.COOKBOOKBAT, DoubleModifier.FOODDROP, 1.5, 0.0));
    }

    @ParameterizedTest
    @MethodSource
    public void otherFairies(
        final int familiar,
        final DoubleModifier mod,
        final double otherFairyEffectiveness,
        final double itemFairyEffectiveness) {
      var cleanups = new Cleanups(withFamiliar(familiar));
      try (cleanups) {
        var weight = 20;
        var familiarMods = getFamiliarMods(weight);
        assertThat(
            familiarMods.getDouble(mod),
            closeTo(fairyFunction(weight) * otherFairyEffectiveness, 0.001));
        assertThat(
            familiarMods.getDouble(DoubleModifier.ITEMDROP),
            closeTo(fairyFunction(weight) * itemFairyEffectiveness, 0.001));
      }
    }
  }

  @Nested
  public class OffhandRemarkable {
    @BeforeAll
    public static void setup() {
      Preferences.reset("OffhandRemarkable");
    }

    @Test
    public void doublesOffhands() {
      var cleanups =
          new Cleanups(
              withEquipped(ItemPool.BRIMSTONE_BUNKER), withEffect(EffectPool.OFFHAND_REMARKABLE));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.MUS_PCT), equalTo(100.0));
      }
    }

    @Test
    public void onlyDoublesOffhandOffhands() {
      var cleanups =
          new Cleanups(
              withSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING),
              withEquipped(ItemPool.BRIMSTONE_BLUDGEON),
              withEquipped(Slot.OFFHAND, ItemPool.BRIMSTONE_BLUDGEON),
              withEffect(EffectPool.OFFHAND_REMARKABLE));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.MUS_PCT), equalTo(100.0));
      }
    }

    @Test
    public void doublesOffhandsOnFamiliar() {
      var cleanups =
          new Cleanups(
              withEquipped(ItemPool.BRIMSTONE_BUNKER),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withEquipped(Slot.FAMILIAR, ItemPool.BRIMSTONE_BUNKER),
              withEffect(EffectPool.OFFHAND_REMARKABLE));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.MUS_PCT), equalTo(200.0));
      }
    }

    @Test
    public void doublesUmbrellaMods() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.OFFHAND, ItemPool.UNBREAKABLE_UMBRELLA),
              withProperty("umbrellaState", "bucket style"),
              withEffect(EffectPool.OFFHAND_REMARKABLE));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.MEATDROP), equalTo(50.0));
        assertThat(current.getDouble(DoubleModifier.ITEMDROP), equalTo(50.0));
      }
    }

    @Test
    public void doublesSleevedCard() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.OFFHAND, ItemPool.CARD_SLEEVE),
              withEquipped(Slot.CARDSLEEVE, "Alice's Army Foil Lanceman"),
              withEffect(EffectPool.OFFHAND_REMARKABLE));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.PVP_FIGHTS), equalTo(12.0));
      }
    }

    @Test
    public void doublesMcHugeLargeLeftPole() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.CONTAINER, ItemPool.MCHUGELARGE_DUFFEL_BAG),
              withEquipped(Slot.WEAPON, ItemPool.MCHUGELARGE_RIGHT_POLE),
              withEquipped(Slot.OFFHAND, ItemPool.MCHUGELARGE_LEFT_POLE),
              withEquipped(Slot.ACCESSORY1, ItemPool.MCHUGELARGE_LEFT_SKI),
              withEquipped(Slot.ACCESSORY2, ItemPool.MCHUGELARGE_RIGHT_SKI),
              withEffect(EffectPool.OFFHAND_REMARKABLE));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.COLD_RESISTANCE), equalTo(18.0));
      }
    }

    @Test
    public void doesNotDoubleHoboPowerConversion() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.OFFHAND, ItemPool.HODGMANS_GARBAGE_STICKER),
              withEquipped(Slot.ACCESSORY1, ItemPool.HODGMANS_BOW_TIE),
              withEffect(EffectPool.OFFHAND_REMARKABLE));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.MEATDROP), equalTo(25.0));
      }
    }

    @Test
    public void doublesHamsterStatsOnly() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.OFFHAND, ItemPool.HODGMANS_HAMSTER),
              withEquipped(Slot.ACCESSORY1, ItemPool.HODGMANS_BOW_TIE),
              withEffect(EffectPool.OFFHAND_REMARKABLE));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.MUS_PCT), equalTo(60.0));
        assertThat(current.getDouble(DoubleModifier.MEATDROP), equalTo(25.0));
      }
    }
  }

  @Nested
  class Events {
    @Test
    void correctlyAppliesLaborDayAdventures() {
      var cleanups = new Cleanups(withDay(2023, Month.JULY, 6));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.ADVENTURES), equalTo(10.0));
      }
    }

    @Test
    void correctlyAppliedModsFromMultipleEventDay() {
      var cleanups = new Cleanups(withDay(2023, Month.AUGUST, 3), withInteractivity(true));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.MOX_EXPERIENCE_PCT), equalTo(25.0));
        assertThat(current.getDouble(DoubleModifier.MANA_COST), equalTo(-3.0));
      }
    }
  }

  @Nested
  class Familiars {
    @Test
    void volleyballGivesExperience() {
      var cleanups = withFamiliar(FamiliarPool.BLOOD_FACED_VOLLEYBALL, 100);

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();
        assertThat(current.getDouble(DoubleModifier.EXPERIENCE), equalTo(4.0));
      }
    }

    @Test
    void sombreroGivesExperience() {
      var cleanups = withFamiliar(FamiliarPool.SOMBRERO, 100);

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();
        assertThat(current.getDouble(DoubleModifier.EXPERIENCE), equalTo(1.0));
      }
    }

    @Test
    void gravyFairyGivesItem() {
      var cleanups = withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 100);

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();
        assertThat(current.getDouble(DoubleModifier.ITEMDROP), closeTo(fairyFunction(10), 0.001));
      }
    }

    @Test
    void leprechaunGivesMeat() {
      var cleanups = withFamiliar(FamiliarPool.LEPRECHAUN, 100);

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();
        assertThat(current.getDouble(DoubleModifier.MEATDROP), closeTo(lepFunction(10), 0.001));
      }
    }
  }

  @Nested
  class JillOfAllTrades {
    @BeforeAll
    static void beforeAll() {
      // clearer to set here than in every volleyball test
      // there's a leak somewhere
      Modifiers.setLocation(null);
    }

    private Cleanups withJill(int weight) {
      return withFamiliar(FamiliarPool.JILL_OF_ALL_TRADES, weight * weight);
    }

    private Cleanups withJillAndCandle(int weight, String candleSetting) {
      return new Cleanups(
          withProperty("ledCandleMode", candleSetting),
          withJill(weight),
          withEquipped(Slot.FAMILIAR, ItemPool.LED_CANDLE));
    }

    @Test
    void nakedJillIsAOneTimesFairy() {
      var cleanups = withJill(10);

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.ITEMDROP), closeTo(fairyFunction(10), 0.001));
      }
    }

    @Test
    void configuredCandleMakesJillABetterFairy() {
      // 5-lbs Jill because candle is +5 lb
      var cleanups = withJillAndCandle(5, "disco");

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.ITEMDROP), closeTo(fairyFunction(15), 0.001));
      }
    }

    @Test
    void nakedJillIsAOneTimesLep() {
      var cleanups = withJill(10);

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.MEATDROP), closeTo(lepFunction(10), 0.001));
      }
    }

    @Test
    void configuredCandleMakesJillABetterLep() {
      // 5-lbs Jill because candle is +5 lb
      var cleanups = withJillAndCandle(5, "ultraviolet");

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.MEATDROP), closeTo(lepFunction(15), 0.001));
      }
    }

    @Test
    void nakedJillIsAOneTimesVolley() {
      var cleanups = withJill(10);

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.EXPERIENCE), equalTo(4.0));
      }
    }

    @Test
    void configuredCandleMakesJillABetterVolley() {
      // 5-lbs Jill because candle is +5 lb
      var cleanups = withJillAndCandle(5, "reading");

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.EXPERIENCE), closeTo(6.0, 0.001));
      }
    }

    @Test
    void atHighMLJillIsASombrero() {
      var cleanups = new Cleanups(withLocation("The Briniest Deepests"), withJill(10));

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();

        // 400 ML
        assertThat(current.getDouble(DoubleModifier.EXPERIENCE), closeTo(15.0, 0.001));
      }
    }

    @Test
    void configuredCandleMakesJillABetterSombrero() {
      // 5-lbs Jill because candle is +5 lb
      var cleanups =
          new Cleanups(withLocation("The Briniest Deepests"), withJillAndCandle(5, "reading"));

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.EXPERIENCE), closeTo(22.5, 0.001));
      }
    }
  }

  @Nested
  class ElevenThings {
    private Cleanups baseCleanups() {
      return new Cleanups(
          Player.withPath(Path.ELEVEN_THINGS), Player.withFamiliar(FamiliarPool.MOSQUITO));
    }

    @Test
    void birthdaySuitAppliesNoExtraModifiers() {
      var cleanups = baseCleanups();

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();
        assertThat(current.getDouble(DoubleModifier.INITIATIVE), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.HP), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.MP), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.WEAPON_DAMAGE), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.FUMBLE), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.ITEMDROP), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.MEATDROP), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.COLD_RESISTANCE), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.HOT_RESISTANCE), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.SLEAZE_RESISTANCE), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.SPOOKY_RESISTANCE), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.STENCH_RESISTANCE), closeTo(0, 0.001));
        assertThat(current.getDouble(DoubleModifier.FAMILIAR_WEIGHT), closeTo(0, 0.001));
      }
    }

    // Based on data from You Hate Eyes Spading spreadsheet
    static Stream<Arguments> calculatesCorrectPositiveBonus() {
      return Stream.of(
          Arguments.of(
              Slot.ACCESSORY1, ItemPool.TINY_PLASTIC_WISNIEWSKI, DoubleModifier.MEATDROP, 60),
          Arguments.of(
              Slot.ACCESSORY1,
              ItemPool.TINY_PLASTIC_CONSERVATIONIST_HIPPY,
              DoubleModifier.MEATDROP,
              50),
          Arguments.of(
              Slot.ACCESSORY1,
              ItemPool.TINY_PLASTIC_KNOB_GOBLIN_MAD_SCIENTIST,
              DoubleModifier.MEATDROP,
              50),
          Arguments.of(
              Slot.ACCESSORY1, ItemPool.TINY_PLASTIC_IITI_KITTY, DoubleModifier.MEATDROP, 60),
          Arguments.of(
              Slot.ACCESSORY1,
              ItemPool.FLASH_LIQUIDIZER_ULTRA_DOUSING_ACCESSORY,
              DoubleModifier.MEATDROP,
              40),
          Arguments.of(
              Slot.ACCESSORY1, ItemPool.RING_OF_DETECT_BORING_DOORS, DoubleModifier.MEATDROP, 20),
          Arguments.of(
              Slot.ACCESSORY1, ItemPool.MAFIA_MIDDLE_FINGER_RING, DoubleModifier.MEATDROP, 40),
          Arguments.of(Slot.ACCESSORY1, ItemPool.GIANT_PINKY_RING, DoubleModifier.MEATDROP, 30),
          Arguments.of(Slot.ACCESSORY1, ItemPool.KNOB_GOBLIN_CODPIECE, DoubleModifier.MEATDROP, 30),
          Arguments.of(Slot.ACCESSORY1, ItemPool.IMP_UNITY_RING, DoubleModifier.MEATDROP, 30),
          Arguments.of(Slot.ACCESSORY1, ItemPool.EXTREME_AMULET, DoubleModifier.MEATDROP, 30),
          Arguments.of(Slot.ACCESSORY1, ItemPool.YAMTILITY_BELT, DoubleModifier.MEATDROP, 75),
          Arguments.of(
              Slot.FAMILIAR,
              ItemPool.SOLID_SHIFTING_TIME_WEIRDNESS,
              DoubleModifier.FAMILIAR_WEIGHT,
              25),
          Arguments.of(
              Slot.FAMILIAR, ItemPool.APRIL_BAND_PICCOLO, DoubleModifier.FAMILIAR_WEIGHT, 25),
          Arguments.of(Slot.FAMILIAR, ItemPool.STILLSUIT, DoubleModifier.FAMILIAR_WEIGHT, 15),
          Arguments.of(Slot.FAMILIAR, ItemPool.TINY_GOLD_MEDAL, DoubleModifier.FAMILIAR_WEIGHT, 15),
          Arguments.of(Slot.HAT, ItemPool.APRILING_BAND_HELMET, DoubleModifier.INITIATIVE, 50),
          Arguments.of(Slot.HAT, ItemPool.LONGHAIRED_HIPPY_WIG, DoubleModifier.INITIATIVE, 75),
          Arguments.of(
              Slot.HAT, ItemPool.INDIE_COMIC_HIPSTER_GLASSES, DoubleModifier.INITIATIVE, 100),
          Arguments.of(Slot.HAT, ItemPool.MARK_III_STEAM_HAT, DoubleModifier.INITIATIVE, 90),
          Arguments.of(Slot.HAT, ItemPool.MARIACHI_HAT, DoubleModifier.INITIATIVE, 50),
          Arguments.of(Slot.HAT, ItemPool.KNOB_GOBLIN_CROWN, DoubleModifier.INITIATIVE, 50),
          Arguments.of(Slot.HAT, ItemPool.VIKING_HELMET, DoubleModifier.INITIATIVE, 50),
          Arguments.of(Slot.HAT, ItemPool.KNOB_GOBLIN_HAREM_VEIL, DoubleModifier.INITIATIVE, 50),
          Arguments.of(Slot.HAT, ItemPool.RAVIOLI_HAT, DoubleModifier.INITIATIVE, 50),
          Arguments.of(
              Slot.OFFHAND, ItemPool.GAZPACHOS_GLACIAL_GRIMOIRE, DoubleModifier.ITEMDROP, 30),
          Arguments.of(Slot.OFFHAND, ItemPool.MINI_ZEPPELIN, DoubleModifier.ITEMDROP, 30),
          Arguments.of(Slot.OFFHAND, ItemPool.DISTURBING_FANFIC, DoubleModifier.ITEMDROP, 30),
          Arguments.of(Slot.OFFHAND, ItemPool.WHITE_SATIN_SHIELD, DoubleModifier.ITEMDROP, 30),
          Arguments.of(Slot.OFFHAND, ItemPool.WHATSIAN_IONIC_PLIERS, DoubleModifier.ITEMDROP, 40),
          Arguments.of(Slot.OFFHAND, ItemPool.GIANT_PENGUIN_KEYCHAIN, DoubleModifier.ITEMDROP, 30),
          Arguments.of(
              Slot.OFFHAND, ItemPool.TIME_LORD_PARTICIPATION_MUG, DoubleModifier.ITEMDROP, 40),
          Arguments.of(Slot.PANTS, ItemPool.CHAIN_MAIL_MONOKINI, DoubleModifier.COLD_RESISTANCE, 4),
          Arguments.of(
              Slot.PANTS, ItemPool.PENGUINSKIN_MINI_SKIRT, DoubleModifier.COLD_RESISTANCE, 5),
          Arguments.of(
              Slot.PANTS, ItemPool.PENGUINSKIN_MINI_KILT, DoubleModifier.COLD_RESISTANCE, 5));
    }

    @ParameterizedTest
    @MethodSource
    void calculatesCorrectPositiveBonus(
        Slot slot, int itemId, DoubleModifier modifier, double bonus) {
      var cleanups = new Cleanups(baseCleanups(), Player.withEquipped(slot, itemId));

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();
        assertThat(current.getDouble(modifier), closeTo(bonus, 0.001));
      }
    }

    static Stream<Arguments> calculatesCorrectNegativeBonus() {
      return Stream.of(
          Arguments.of(
              Slot.WEAPON, ItemPool.CORRUPT_CLUB_OF_CORRUPT_CORRUPTION, DoubleModifier.FUMBLE, 44),
          Arguments.of(
              Slot.HAT, ItemPool.BIPHASIC_MOLECULAR_OCULUS, DoubleModifier.INITIATIVE, -100),
          Arguments.of(Slot.PANTS, ItemPool.BUGBEAR_BUNGGUARD, DoubleModifier.MEATDROP, -30),
          Arguments.of(
              Slot.OFFHAND, ItemPool.ANNIVERSARY_CHUTNEY_SCULPTURE, DoubleModifier.MEATDROP, -30),
          Arguments.of(Slot.ACCESSORY1, ItemPool.TRANSFUNCTIONER, DoubleModifier.ITEMDROP, -30),
          Arguments.of(Slot.FAMILIAR, ItemPool.BLUNDARRRBUS, DoubleModifier.FAMILIAR_WEIGHT, -5));
    }

    @ParameterizedTest
    @MethodSource
    void calculatesCorrectNegativeBonus(
        Slot slot, int itemId, DoubleModifier modifier, double bonus) {
      var cleanups =
          new Cleanups(
              Player.withPath(Path.ELEVEN_THINGS),
              Player.withFamiliar(FamiliarPool.SPOOKY_PIRATE_SKELETON),
              Player.withEquipped(slot, itemId));

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();
        assertThat(current.getDouble(modifier), closeTo(bonus, 0.001));
      }
    }

    @Test
    void calculatesCorrectBonusForMultipleAccessories() {
      var cleanups =
          new Cleanups(
              baseCleanups(),
              Player.withEquipped(
                  Slot.ACCESSORY1, ItemPool.FLASH_LIQUIDIZER_ULTRA_DOUSING_ACCESSORY),
              Player.withEquipped(Slot.ACCESSORY2, ItemPool.EXTREME_AMULET),
              Player.withEquipped(Slot.ACCESSORY3, ItemPool.RING_OF_DETECT_BORING_DOORS));

      try (cleanups) {
        Modifiers current = KoLCharacter.getCurrentModifiers();
        assertThat(current.getDouble(DoubleModifier.MEATDROP), closeTo(90, 0.001));
        assertThat(current.getDouble(DoubleModifier.ITEMDROP), closeTo(-20, 0.001));
      }
    }

    static Stream<Arguments> calculatesNewPotionDuration() {
      return Stream.of(
          Arguments.of(ItemPool.KUMQUAT_SUPERSUCKER, 26),
          Arguments.of(ItemPool.POTION_OF_PULCHRITUDE, 8),
          Arguments.of(ItemPool.RECORDING_INIGO, 36),
          Arguments.of(ItemPool.POTION_OF_SPIRITUAL_GUNKIFYING, 28));
    }

    @ParameterizedTest
    @MethodSource
    void calculatesNewPotionDuration(int itemId, int duration) {
      var cleanups = baseCleanups();

      try (cleanups) {
        Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, itemId);
        assertThat(mods.getDouble(DoubleModifier.EFFECT_DURATION), closeTo(duration, 0.001));
      }
    }

    @Test
    void resetsNewPotionDuration() {
      var cleanups = baseCleanups();
      var item = ItemPool.POTION_OF_PULCHRITUDE;

      try (cleanups) {
        Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, item);
        assertThat(mods.getDouble(DoubleModifier.EFFECT_DURATION), closeTo(8, 0.001));
      }

      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, item);
      assertThat(mods.getDouble(DoubleModifier.EFFECT_DURATION), closeTo(5, 0.001));
    }
  }

  @Nested
  class TimeTwitchingTowerSoup {
    @Test
    void protogeneticSoupConsideredInFamiliarWeight() {
      Modifiers familiarMods = new Modifiers();
      var familiar = FamiliarData.registerFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 0);
      familiar.setSoupWeight(19);

      familiarMods.applyFamiliarModifiers(familiar, null);

      assertThat(familiarMods.getDouble(DoubleModifier.ITEMDROP), closeTo(50.166, 0.001));

      KoLCharacter.removeFamiliar(familiar);
    }
  }

  @Nested
  class DoubleExpressions {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void helmOnlyFreePullInAxecore(final boolean axecore) {
      var cleanups = axecore ? withPath(Path.AVATAR_OF_BORIS) : new Cleanups();
      try (cleanups) {
        Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, ItemPool.BORIS_HELM);
        assertThat(mods.getBoolean(BooleanModifier.FREE_PULL), is(axecore));
      }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void helmOnlySoftcoreOnlyNotInAxecore(final boolean axecore) {
      var cleanups = axecore ? withPath(Path.AVATAR_OF_BORIS) : new Cleanups();
      try (cleanups) {
        Modifiers mods = ModifierDatabase.getModifiers(ModifierType.ITEM, ItemPool.BORIS_HELM);
        assertThat(mods.getBoolean(BooleanModifier.SOFTCORE), is(!axecore));
      }
    }
  }

  @Test
  public void hatTrickHatsCountOnlyHighestPowerForDamageAbsorption() {
    var cleanups =
        new Cleanups(
            withPath(Path.HAT_TRICK),
            withHatTrickHats(List.of(ItemPool.SEAL_HELMET, ItemPool.LONGHAIRED_HIPPY_WIG)));
    try (cleanups) {
      Modifiers current = KoLCharacter.getCurrentModifiers();
      assertThat(current.getDouble(DoubleModifier.DAMAGE_ABSORPTION), closeTo(200, 0.001));
      assertThat(current.getDouble(DoubleModifier.STENCH_DAMAGE), closeTo(20, 0.001));
      assertThat(current.getDouble(DoubleModifier.WEAPON_DAMAGE), closeTo(1, 0.001));
    }
  }

  @Test
  public void prismaticBeretRespectsSoftCap() {
    Modifiers mods = new Modifiers();
    mods.applyPrismaticBeretModifiers(1370);
    assertThat(mods.getDouble(DoubleModifier.DAMAGE_ABSORPTION), closeTo(237, 0.001));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 100, 200})
  public void unironicKnife(final int advs) {
    var cleanup = withAdventuresLeft(advs);

    try (cleanup) {
      var mods = ModifierDatabase.getModifiers(ModifierType.ITEM, "unironic knife");

      assertThat(mods.getDouble(DoubleModifier.ITEMDROP), equalTo(advs > 0 ? 0.0 : 100.0));
      assertThat(mods.getDouble(DoubleModifier.ADVENTURES), equalTo(advs < 200 ? 0.0 : 10.0));
    }
  }

  @Test
  public void mobiusRing() {
    var cleanup = withParadoxicity(6);

    try (cleanup) {
      var mods = ModifierDatabase.getModifiers(ModifierType.ITEM, "M&ouml;bius ring");

      assertThat(mods.getDouble(DoubleModifier.WEAPON_DAMAGE), equalTo(0.0));
      assertThat(mods.getDouble(DoubleModifier.HOT_RESISTANCE), equalTo(2.0));
      assertThat(mods.getDouble(DoubleModifier.GEARDROP), equalTo(100.0));
      assertThat(mods.getDouble(DoubleModifier.DAMAGE_REDUCTION), equalTo(6.0));
      assertThat(mods.getDouble(DoubleModifier.INITIATIVE), equalTo(50.0));
      assertThat(mods.getDouble(DoubleModifier.FAMILIAR_WEIGHT), equalTo(5.0));
    }
  }

  @Nested
  class Monodent {
    @Test
    public void noWaveZoneDoesNotAddItemForNoZone() {
      var cleanups = new Cleanups(withProperty("_seadentWaveZone", ""), withLocation(""));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.ITEMDROP), equalTo(0.0));
      }
    }

    @Test
    public void waveZoneDoesNotAddItemForOtherZone() {
      var cleanups =
          new Cleanups(withProperty("_seadentWaveZone", "Noob Cave"), withLocation("Dire Warren"));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.ITEMDROP), equalTo(0.0));
      }
    }

    @Test
    public void waveZoneAddsItemForZone() {
      var cleanups =
          new Cleanups(withProperty("_seadentWaveZone", "Noob Cave"), withLocation("Noob Cave"));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments(false);
        Modifiers current = KoLCharacter.getCurrentModifiers();

        assertThat(current.getDouble(DoubleModifier.ITEMDROP), equalTo(30.0));
      }
    }
  }
}
