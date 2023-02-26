package net.sourceforge.kolmafia;

import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withLocation;
import static internal.helpers.Player.withMP;
import static internal.helpers.Player.withOverrideModifiers;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import internal.helpers.Cleanups;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.Arrays;
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

  @Test
  public void intrinsicSpicinessModifiers() {
    KoLCharacter.setAscensionClass(AscensionClass.SAUCEROR);
    for (int i = 1; i <= 11; i++) {
      int myst = (i == 1) ? 0 : (i - 1) * (i - 1) + 4;
      KoLCharacter.setStatPoints(0, 0, myst, (long) myst * myst, 0, 0);
      Modifiers mods = ModifierDatabase.getModifiers(ModifierType.SKILL, "Intrinsic Spiciness");
      assertEquals(Math.min(i, 10), mods.getDouble(DoubleModifier.SAUCE_SPELL_DAMAGE));
    }
  }

  @Test
  public void correctlyCalculatesCappedCombatRate() {
    Modifiers mod = new Modifiers();
    mod.addDouble(DoubleModifier.COMBAT_RATE, 25, ModifierType.NONE, "");
    mod.addDouble(DoubleModifier.COMBAT_RATE, 7, ModifierType.NONE, "");
    assertEquals(26, mod.getDouble(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, 9, ModifierType.NONE, "");
    assertEquals(28, mod.getDouble(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, 9, ModifierType.NONE, "");
    assertEquals(30, mod.getDouble(DoubleModifier.COMBAT_RATE));

    mod = new Modifiers();
    mod.addDouble(DoubleModifier.COMBAT_RATE, -25, ModifierType.NONE, "");
    mod.addDouble(DoubleModifier.COMBAT_RATE, -7, ModifierType.NONE, "");
    assertEquals(-26, mod.getDouble(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, -9, ModifierType.NONE, "");
    assertEquals(-28, mod.getDouble(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, -9, ModifierType.NONE, "");
    assertEquals(-30, mod.getDouble(DoubleModifier.COMBAT_RATE));
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
      var cleanups = new Cleanups(withClass(AscensionClass.VAMPYRE), withStats(100, 100, 100));
      try (cleanups) {
        // Base HP = Base MUS
        // Buffed HP = max(Base MUS, Base HP + mod(HP))

        Modifiers mods = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        var stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(100, stats.get(DerivedModifier.BUFFED_HP));

        // viking helmet: (+1 muscle)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.VIKING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(101, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(100, stats.get(DerivedModifier.BUFFED_HP));

        // reinforced beaded headband (+40 HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.REINFORCED_BEADED_HEADBAND));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(140, stats.get(DerivedModifier.BUFFED_HP));

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(Slot.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats.get(DerivedModifier.BUFFED_MUS));
        assertEquals(100, stats.get(DerivedModifier.BUFFED_HP));
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
    private Double fairyFunction(final double weight) {
      return Math.max(Math.sqrt(55 * weight) + weight - 3, 0);
    }

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
}
