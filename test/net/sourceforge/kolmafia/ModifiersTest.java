package net.sourceforge.kolmafia;

import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withHP;
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
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.DayOfWeek;
import java.time.Month;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.request.LatteRequest;
import net.sourceforge.kolmafia.request.LatteRequest.Latte;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
      Modifiers mods = Modifiers.getModifiers(ModifierType.ITEM, ItemPool.PATRIOT_SHIELD);

      // Always has
      assertEquals(3, mods.get(DoubleModifier.EXPERIENCE));

      // Has because of class
      assertEquals(5.0, mods.get(DoubleModifier.MP_REGEN_MIN));
      assertEquals(6.0, mods.get(DoubleModifier.MP_REGEN_MAX));
      assertEquals(20.0, mods.get(DoubleModifier.SPELL_DAMAGE));

      // Does not have because of class
      assertEquals(0, mods.get(DoubleModifier.HP_REGEN_MAX));
      assertEquals(0, mods.get(DoubleModifier.HP_REGEN_MIN));
      assertEquals(0, mods.get(DoubleModifier.WEAPON_DAMAGE));
      assertEquals(0, mods.get(DoubleModifier.DAMAGE_REDUCTION));
      assertEquals(0, mods.get(DoubleModifier.FAMILIAR_WEIGHT));
      assertEquals(0, mods.get(DoubleModifier.RANGED_DAMAGE));
      assertFalse(mods.getBoolean(Modifiers.FOUR_SONGS));
      assertEquals(0, mods.get(DoubleModifier.COMBAT_MANA_COST));
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
      Modifiers mods = Modifiers.getModifiers(ModifierType.ITEM, "Tuesday's Ruby");

      assertThat(mods.get(DoubleModifier.MEATDROP), equalTo(dotw == DayOfWeek.SUNDAY ? 5.0 : 0.0));
      assertThat(mods.get(DoubleModifier.MUS_PCT), equalTo(dotw == DayOfWeek.MONDAY ? 5.0 : 0.0));
      assertThat(
          mods.get(DoubleModifier.MP_REGEN_MIN), equalTo(dotw == DayOfWeek.TUESDAY ? 3.0 : 0.0));
      assertThat(
          mods.get(DoubleModifier.MP_REGEN_MAX), equalTo(dotw == DayOfWeek.TUESDAY ? 7.0 : 0.0));
      assertThat(
          mods.get(DoubleModifier.MYS_PCT), equalTo(dotw == DayOfWeek.WEDNESDAY ? 5.0 : 0.0));
      assertThat(
          mods.get(DoubleModifier.ITEMDROP), equalTo(dotw == DayOfWeek.THURSDAY ? 5.0 : 0.0));
      assertThat(mods.get(DoubleModifier.MOX_PCT), equalTo(dotw == DayOfWeek.FRIDAY ? 5.0 : 0.0));
      assertThat(
          mods.get(DoubleModifier.HP_REGEN_MIN), equalTo(dotw == DayOfWeek.SATURDAY ? 3.0 : 0.0));
      assertThat(
          mods.get(DoubleModifier.HP_REGEN_MAX), equalTo(dotw == DayOfWeek.SATURDAY ? 7.0 : 0.0));
    }
  }

  @Test
  public void testSynergies() {
    // The "synergy" bitmap modifier is assigned dynamically, based on appearance order in
    // Modifiers.txt
    // The first Synergetic item seen gets 0b00001, the 2nd: 0b00010, 3rd: 0b00100, etc.

    for (Entry<String, Integer> entry : Modifiers.getSynergies()) {
      String name = entry.getKey();
      int mask = entry.getValue();

      int manualMask = 0;
      for (String piece : name.split("/")) {
        Modifiers mods = Modifiers.getModifiers(ModifierType.ITEM, piece);
        manualMask |= mods.getRawBitmap(Modifiers.SYNERGETIC);
      }

      assertEquals(manualMask, mask, name);
    }
  }

  @Test
  public void intrinsicSpicinessModifiers() {
    KoLCharacter.setAscensionClass(AscensionClass.SAUCEROR);
    for (int i = 1; i <= 11; i++) {
      int myst = (i == 1) ? 0 : (i - 1) * (i - 1) + 4;
      KoLCharacter.setStatPoints(0, 0, myst, (long) myst * myst, 0, 0);
      Modifiers mods = Modifiers.getModifiers(ModifierType.SKILL, "Intrinsic Spiciness");
      assertEquals(Math.min(i, 10), mods.get(DoubleModifier.SAUCE_SPELL_DAMAGE));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "+50% Spell Damage, Spell Damage Percent: +50",
    "Successful hit weakens opponent, Weakens Monster",
    "Only Accordion Thieves may use this item, Class: \"Accordion Thief\"",
    "All Attributes +5, 'Muscle: +5, Mysticality: +5, Moxie: +5'",
    "All Attributes +30%, 'Muscle Percent: +30, Mysticality Percent: +30, Moxie Percent: +30'",
    "Bonus&nbsp;for&nbsp;Saucerors&nbsp;only, Class: \"Sauceror\"",
    "Monsters are much more attracted to you., Combat Rate: +10",
    "Monsters will be significantly less attracted to you. (Underwater only), Combat Rate (Underwater): -15",
    "Maximum HP/MP +200, 'Maximum HP: +200, Maximum MP: +200'",
    "Regenerate 100 MP per adventure, 'MP Regen Min: 100, MP Regen Max: 100'",
    "Regenerate 15-20 HP and MP per adventure, 'HP Regen Min: 15, HP Regen Max: 20, MP Regen Min: 15, MP Regen Max: 20'",
    "Serious Cold Resistance (+3), Cold Resistance: +3",
    "Sublime Resistance to All Elements (+9), 'Spooky Resistance: +9, Stench Resistance: +9, Hot Resistance: +9, Cold Resistance: +9, Sleaze Resistance: +9'",
    "So-So Slime Resistance (+2), Slime Resistance: +2",
    "Slight Supercold Resistance, Supercold Resistance: +1",
    "Your familiar will always act in combat, Familiar Action Bonus: +100"
  })
  public void canParseModifier(String enchantment, String modifier) {
    assertEquals(modifier, Modifiers.parseModifier(enchantment));
  }

  @Test
  public void correctlyCalculatesCappedCombatRate() {
    Modifiers mod = new Modifiers();
    mod.addDouble(DoubleModifier.COMBAT_RATE, 25, ModifierType.NONE, "");
    mod.addDouble(DoubleModifier.COMBAT_RATE, 7, ModifierType.NONE, "");
    assertEquals(26, mod.get(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, 9, ModifierType.NONE, "");
    assertEquals(28, mod.get(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, 9, ModifierType.NONE, "");
    assertEquals(30, mod.get(DoubleModifier.COMBAT_RATE));

    mod = new Modifiers();
    mod.addDouble(DoubleModifier.COMBAT_RATE, -25, ModifierType.NONE, "");
    mod.addDouble(DoubleModifier.COMBAT_RATE, -7, ModifierType.NONE, "");
    assertEquals(-26, mod.get(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, -9, ModifierType.NONE, "");
    assertEquals(-28, mod.get(DoubleModifier.COMBAT_RATE));
    mod.addDouble(DoubleModifier.COMBAT_RATE, -9, ModifierType.NONE, "");
    assertEquals(-30, mod.get(DoubleModifier.COMBAT_RATE));
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
      assertThat(familiarMods.get(mod), closeTo(50, 0.001));
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

        assertThat(familiarMods.get(DoubleModifier.ITEMDROP), closeTo(50.166, 0.001));
      }
    }

    @Test
    void fixodeneConsideredInFamiliarModifiersNotExceedingTwenty() {
      var cleanups = withEffect("Fidoxene");

      try (cleanups) {
        Modifiers familiarMods = new Modifiers();
        var familiar = FamiliarData.registerFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 400);

        familiarMods.applyFamiliarModifiers(familiar, null);

        assertThat(familiarMods.get(DoubleModifier.ITEMDROP), closeTo(50.166, 0.001));
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
        assertThat(mods.get(DoubleModifier.ITEMDROP), equalTo(300.0));
      }
    }

    @Test
    public void champagneDoublesEffect() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.WEAPON, ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 11),
              withEffect(EffectPool.SYNTHESIS_COLLECTION));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.get(DoubleModifier.ITEMDROP), equalTo(300.0));
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
        assertThat(mods.get(DoubleModifier.ITEMDROP), equalTo(25.0));
      }
    }

    @Test
    public void squintDoublesUmbrella() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.OFFHAND, ItemPool.UNBREAKABLE_UMBRELLA),
              withProperty("umbrellaState", "bucket style"),
              withEffect(EffectPool.STEELY_EYED_SQUINT));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        var item = mods.get(DoubleModifier.ITEMDROP);
        assertThat(item, equalTo(50.0));
      }
    }

    @Test
    public void squintAndChampagneStack() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.WEAPON, ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 11),
              withEffect(EffectPool.STEELY_EYED_SQUINT),
              withEffect(EffectPool.SYNTHESIS_COLLECTION));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.get(DoubleModifier.ITEMDROP), equalTo(600.0));
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
        assertThat(mods.get(DoubleModifier.ITEMDROP), equalTo(400.0));
      }
    }

    @Test
    public void champagneDoesntDoubleOtoscope() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.WEAPON, ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 11),
              withOverrideModifiers(ModifierType.GENERATED, "fightMods", "Item Drop: +200"));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.get(DoubleModifier.ITEMDROP), equalTo(200.0));
      }
    }

    @Test
    public void squintAndChampagneDoublesOtoscopeOnce() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.WEAPON, ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 11),
              withEffect(EffectPool.STEELY_EYED_SQUINT),
              withOverrideModifiers(ModifierType.GENERATED, "fightMods", "Item Drop: +200"));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.get(DoubleModifier.ITEMDROP), equalTo(400.0));
      }
    }
  }

  @Nested
  class ElementalDoublers {
    @BeforeAll
    public static void setup() {
      Preferences.reset("ElementalDoublers");
    }

    @Test
    public void bendinHellDoublesEffect() {
      var cleanups =
          new Cleanups(
              withEffect(EffectPool.BENDIN_HELL), withEffect(EffectPool.PAINTED_ON_BIKINI));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.get(DoubleModifier.SLEAZE_DAMAGE), equalTo(100.0));
        assertThat(mods.get(DoubleModifier.SLEAZE_SPELL_DAMAGE), equalTo(100.0));
      }
    }

    @Test
    public void dirtyPearDoublesEffect() {
      var cleanups =
          new Cleanups(withEffect(EffectPool.DIRTY_PEAR), withEffect(EffectPool.PAINTED_ON_BIKINI));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        assertThat(mods.get(DoubleModifier.SLEAZE_DAMAGE), equalTo(100.0));
        assertThat(mods.get(DoubleModifier.SLEAZE_SPELL_DAMAGE), equalTo(100.0));
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
        assertThat(mods.get(DoubleModifier.SLEAZE_DAMAGE), equalTo(200.0));
        assertThat(mods.get(DoubleModifier.SLEAZE_SPELL_DAMAGE), equalTo(200.0));
      }
    }
  }

  @Nested
  class ExperienceDoublers {
    @BeforeAll
    public static void setup() {
      Preferences.reset("ExperienceDoublers");
    }

    @Test
    public void makeshiftGarbageShirtDoublesEffect() {
      var cleanups =
          new Cleanups(
              withEquipped(EquipmentManager.SHIRT, ItemPool.MAKESHIFT_GARBAGE_SHIRT),
              withProperty("garbageShirtCharge", 37),
              withEffect(EffectPool.FEELING_LOST));

      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        // 3 from garbage shirt, 30 from Feeling Lost, *2 = 66
        assertThat(mods.get(DoubleModifier.EXPERIENCE), equalTo(66.0));
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
        int[] stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(155, stats[Modifiers.BUFFED_HP]);

        // viking helmet: (+1 muscle)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.VIKING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(101, stats[Modifiers.BUFFED_MUS]);
        assertEquals(156, stats[Modifiers.BUFFED_HP]);

        // reinforced beaded headband (+40 HP)
        EquipmentManager.setEquipment(
            EquipmentManager.HAT, ItemPool.get(ItemPool.REINFORCED_BEADED_HEADBAND));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(195, stats[Modifiers.BUFFED_HP]);

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(
            EquipmentManager.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(258, stats[Modifiers.BUFFED_HP]);
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
        int[] stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(103, stats[Modifiers.BUFFED_HP]);

        // viking helmet: (+1 muscle)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.VIKING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(101, stats[Modifiers.BUFFED_MUS]);
        assertEquals(104, stats[Modifiers.BUFFED_HP]);

        // reinforced beaded headband (+40 HP)
        EquipmentManager.setEquipment(
            EquipmentManager.HAT, ItemPool.get(ItemPool.REINFORCED_BEADED_HEADBAND));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(143, stats[Modifiers.BUFFED_HP]);

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(
            EquipmentManager.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(206, stats[Modifiers.BUFFED_HP]);
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
        int[] stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(100, stats[Modifiers.BUFFED_HP]);

        // viking helmet: (+1 muscle)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.VIKING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(101, stats[Modifiers.BUFFED_MUS]);
        assertEquals(100, stats[Modifiers.BUFFED_HP]);

        // reinforced beaded headband (+40 HP)
        EquipmentManager.setEquipment(
            EquipmentManager.HAT, ItemPool.get(ItemPool.REINFORCED_BEADED_HEADBAND));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(140, stats[Modifiers.BUFFED_HP]);

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(
            EquipmentManager.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(100, stats[Modifiers.BUFFED_HP]);
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
        int[] stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(30, stats[Modifiers.BUFFED_HP]);

        // viking helmet: (+1 muscle)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.VIKING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(101, stats[Modifiers.BUFFED_MUS]);
        assertEquals(30, stats[Modifiers.BUFFED_HP]);

        // reinforced beaded headband (+40 HP)
        EquipmentManager.setEquipment(
            EquipmentManager.HAT, ItemPool.get(ItemPool.REINFORCED_BEADED_HEADBAND));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(70, stats[Modifiers.BUFFED_HP]);

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(
            EquipmentManager.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(30, stats[Modifiers.BUFFED_HP]);
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
        int[] stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(176, stats[Modifiers.BUFFED_HP]);

        // viking helmet: (+1 muscle)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.VIKING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(101, stats[Modifiers.BUFFED_MUS]);
        assertEquals(176, stats[Modifiers.BUFFED_HP]);

        // reinforced beaded headband (+40 HP)
        EquipmentManager.setEquipment(
            EquipmentManager.HAT, ItemPool.get(ItemPool.REINFORCED_BEADED_HEADBAND));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(176, stats[Modifiers.BUFFED_HP]);

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(
            EquipmentManager.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(176, stats[Modifiers.BUFFED_HP]);
      }
    }

    @Test
    public void correctlySpeculatesGreyYouMaximumHP() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.GREY_GOO),
              withStats(100, 100, 100),
              withHP(216, 216, 216),
              withEquipped(EquipmentManager.HAT, ItemPool.REINFORCED_BEADED_HEADBAND));
      try (cleanups) {
        // Base HP = (starting value + absorptions + currently worn equipment)
        // Buffed HP = Base HP - currently worn equipment + mod(HP)

        // We are starting with a reinforced beaded headband, which provides +40 HP
        // Therefore, our actual "baseHP" is 176

        Modifiers current = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        assertEquals(40, current.get(DoubleModifier.HP));

        int[] currentStats = current.predict();
        assertEquals(100, currentStats[Modifiers.BUFFED_MUS]);
        assertEquals(216, currentStats[Modifiers.BUFFED_HP]);

        // Make some modifiers to speculate with
        Modifiers speculate = new Modifiers(current);
        assertEquals(40, speculate.get(DoubleModifier.HP));
        // Suppose we want to replace the reinforced beaded headband (+40 HP)
        // with a nurse's hat (+300 HP)
        speculate.setDouble(DoubleModifier.HP, 300.0);

        int[] speculateStats = speculate.predict();
        assertEquals(476, speculateStats[Modifiers.BUFFED_HP]);
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
        int[] stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MYS]);
        assertEquals(150, stats[Modifiers.BUFFED_MP]);

        // fuzzy earmuffs: (+11 myst)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.FUZZY_EARMUFFS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(111, stats[Modifiers.BUFFED_MYS]);
        assertEquals(167, stats[Modifiers.BUFFED_MP]);

        // beer helmet (+40 MP)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.BEER_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MYS]);
        assertEquals(190, stats[Modifiers.BUFFED_MP]);

        EquipmentManager.setEquipment(EquipmentManager.HAT, EquipmentRequest.UNEQUIP);

        // Cargo Cultist Shorts (+6 myst, +66% MP)
        EquipmentManager.setEquipment(
            EquipmentManager.PANTS, ItemPool.get(ItemPool.CARGO_CULTIST_SHORTS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(106, stats[Modifiers.BUFFED_MYS]);
        assertEquals(229, stats[Modifiers.BUFFED_MP]);
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
        int[] stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MYS]);
        assertEquals(100, stats[Modifiers.BUFFED_MP]);

        // fuzzy earmuffs: (+11 myst)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.FUZZY_EARMUFFS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(111, stats[Modifiers.BUFFED_MYS]);
        assertEquals(111, stats[Modifiers.BUFFED_MP]);

        // beer helmet (+40 MP)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.BEER_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MYS]);
        assertEquals(140, stats[Modifiers.BUFFED_MP]);

        EquipmentManager.setEquipment(EquipmentManager.HAT, EquipmentRequest.UNEQUIP);

        // Cargo Cultist Shorts (+6 myst, +66% MP)
        EquipmentManager.setEquipment(
            EquipmentManager.PANTS, ItemPool.get(ItemPool.CARGO_CULTIST_SHORTS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(106, stats[Modifiers.BUFFED_MYS]);
        assertEquals(176, stats[Modifiers.BUFFED_MP]);
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
        int[] stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MYS]);
        assertEquals(126, stats[Modifiers.BUFFED_MP]);

        // fuzzy earmuffs: (+11 myst)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.FUZZY_EARMUFFS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(111, stats[Modifiers.BUFFED_MYS]);
        assertEquals(126, stats[Modifiers.BUFFED_MP]);

        // beer helmet (+40 MP)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.BEER_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MYS]);
        assertEquals(126, stats[Modifiers.BUFFED_MP]);

        EquipmentManager.setEquipment(EquipmentManager.HAT, EquipmentRequest.UNEQUIP);

        // Cargo Cultist Shorts (+6 myst, +66% MP)
        EquipmentManager.setEquipment(
            EquipmentManager.PANTS, ItemPool.get(ItemPool.CARGO_CULTIST_SHORTS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(106, stats[Modifiers.BUFFED_MYS]);
        assertEquals(126, stats[Modifiers.BUFFED_MP]);
      }
    }

    @Test
    public void correctlySpeculatesGreyYouMaximumMP() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.GREY_GOO),
              withStats(100, 100, 100),
              withMP(126, 126, 126),
              withEquipped(EquipmentManager.HAT, ItemPool.BEER_HELMET));
      try (cleanups) {
        // Base MP = (starting value + absorptions + currently worn equipment)
        // Buffed MP = Base MP - currently worn equipment + mod(MP)

        // We are starting with a beer helmet, which provides +40 MP
        // Therefore, our actual "baseMP" is 86

        Modifiers current = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        assertEquals(40, current.get(DoubleModifier.MP));

        int[] currentStats = current.predict();
        assertEquals(100, currentStats[Modifiers.BUFFED_MYS]);
        assertEquals(126, currentStats[Modifiers.BUFFED_MP]);

        // Make some modifiers to speculate with
        Modifiers speculate = new Modifiers(current);
        assertEquals(40, speculate.get(DoubleModifier.MP));
        // Suppose we want to replace the beer helmet (+40 HP)
        // with Covers-Your-Head (+100 MP)
        speculate.setDouble(DoubleModifier.MP, 100.0);

        int[] speculateStats = speculate.predict();
        assertEquals(186, speculateStats[Modifiers.BUFFED_MP]);
      }
    }

    @Test
    public void correctlyCalculatesMoxieControlledMaximumMP() {
      // moxie magnet: Moxie Controls MP
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.DISCO_BANDIT),
              withStats(100, 100, 150),
              withEquipped(EquipmentManager.ACCESSORY1, "moxie magnet"));
      try (cleanups) {
        // Buffed MOX = Base MOX + mod(MOX) + ceiling(Base MOX * mod(MOX_PCT)/100.0)
        // Base MP = Buffed MUS
        // C = 1.5 if MYS class, otherwise 1.0
        // Buffed MP = ceiling(Base MP * (C + mod(MP_PCT)/100.0) + mod(MP))

        Modifiers mods = KoLCharacter.getCurrentModifiers();
        KoLCharacter.recalculateAdjustments(false);
        int[] stats = mods.predict();
        assertEquals(150, stats[Modifiers.BUFFED_MOX]);
        assertEquals(150, stats[Modifiers.BUFFED_MP]);

        // Disco 'Fro Pick (+11 mox)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.DISCO_FRO_PICK));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(161, stats[Modifiers.BUFFED_MOX]);
        assertEquals(161, stats[Modifiers.BUFFED_MP]);

        // beer helmet (+40 MP)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.BEER_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(150, stats[Modifiers.BUFFED_MOX]);
        assertEquals(190, stats[Modifiers.BUFFED_MP]);

        // training helmet: (+25% mox)
        EquipmentManager.setEquipment(EquipmentManager.HAT, ItemPool.get(ItemPool.TRAINING_HELMET));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(188, stats[Modifiers.BUFFED_MOX]);
        assertEquals(188, stats[Modifiers.BUFFED_MP]);

        EquipmentManager.setEquipment(EquipmentManager.HAT, EquipmentRequest.UNEQUIP);

        // Cargo Cultist Shorts (+6 mox, +66% MP)
        EquipmentManager.setEquipment(
            EquipmentManager.PANTS, ItemPool.get(ItemPool.CARGO_CULTIST_SHORTS));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(156, stats[Modifiers.BUFFED_MOX]);
        assertEquals(259, stats[Modifiers.BUFFED_MP]);
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
              withEquipped(EquipmentManager.OFFHAND, "latte lovers member's mug"));
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
            Modifiers.getModifiers(ModifierType.ITEM, "[" + ItemPool.LATTE_MUG + "]");
        assertEquals(5, latteModifiers.get(DoubleModifier.FAMILIAR_WEIGHT));
        assertEquals(40, latteModifiers.get(DoubleModifier.MEATDROP));
        assertEquals(1, latteModifiers.get(DoubleModifier.MOX_EXPERIENCE));
        assertEquals(5, latteModifiers.get(DoubleModifier.MOX_PCT));
        assertEquals(5, latteModifiers.get(DoubleModifier.PICKPOCKET_CHANCE));

        // Verify that KoLCharacter applied the mods
        Modifiers currentModifiers = KoLCharacter.getCurrentModifiers();
        assertEquals(5, currentModifiers.get(DoubleModifier.FAMILIAR_WEIGHT));
        assertEquals(40, currentModifiers.get(DoubleModifier.MEATDROP));
        assertEquals(1, currentModifiers.get(DoubleModifier.MOX_EXPERIENCE));
        assertEquals(5, currentModifiers.get(DoubleModifier.MOX_PCT));
        assertEquals(5, currentModifiers.get(DoubleModifier.PICKPOCKET_CHANCE));

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
      Modifiers.Lookup lookup = new Modifiers.Lookup(ModifierType.LOCAL_VOTE, "");

      Modifiers mods = Modifiers.parseModifiers(lookup, setting);
      assertEquals(30, mods.get(DoubleModifier.MEATDROP));
      assertEquals(2, mods.get(DoubleModifier.FAMILIAR_EXP));
      assertEquals(4, mods.get(DoubleModifier.MUS_EXPERIENCE));

      Modifiers evaluated = Modifiers.evaluatedModifiers(lookup, setting);
      assertEquals(30, evaluated.get(DoubleModifier.MEATDROP));
      assertEquals(2, evaluated.get(DoubleModifier.FAMILIAR_EXP));
      assertEquals(4, evaluated.get(DoubleModifier.MUS_EXPERIENCE));

      var cleanups = new Cleanups(withProperty("_voteModifier", setting));
      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers current = KoLCharacter.getCurrentModifiers();
        assertEquals(30, current.get(DoubleModifier.MEATDROP));
        assertEquals(2, current.get(DoubleModifier.FAMILIAR_EXP));
        assertEquals(4, current.get(DoubleModifier.MUS_EXPERIENCE));
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

      assertThat(current.get(DoubleModifier.FAMILIAR_WEIGHT), equalTo(weightModifier));
    }

    KoLCharacter.recalculateAdjustments(false);
    Modifiers current = KoLCharacter.getCurrentModifiers();

    assertThat(current.get(DoubleModifier.FAMILIAR_WEIGHT), equalTo(0.0));
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
    assertThat(Modifiers.getModifiers(ModifierType.SKILL, skillName).variable, equalTo(variable));
  }

  @Test
  @Disabled("modifiers.txt would need to be modified")
  public void writeModifiersSubsetOfModifiersTxt() throws IOException {
    ByteArrayOutputStream ostream = new ByteArrayOutputStream();
    PrintStream writer = new PrintStream(ostream);

    Modifiers.writeModifiers(writer);
    writer.close();
    List<String> writeModifiersLines = ostream.toString().lines().collect(Collectors.toList());

    BufferedReader reader =
        DataUtilities.getReader(KoLConstants.DATA_DIRECTORY, "modifiers.txt", true);

    String line;
    Iterator<String> writeModifiersIterator = writeModifiersLines.iterator();
    String writeModifiersLine = writeModifiersIterator.next();
    while ((line = reader.readLine()) != null) {
      if (writeModifiersLine.startsWith("# ")
          ? line.startsWith(writeModifiersLine)
          : line.equals(writeModifiersLine)) {
        writeModifiersLine =
            writeModifiersIterator.hasNext() ? writeModifiersIterator.next() : null;
      }
    }

    StringBuilder message = new StringBuilder();
    if (writeModifiersLine != null) {
      int index = writeModifiersLines.indexOf(writeModifiersLine);
      for (int i = Math.min(3, index); i >= 0; i--) {
        message.append("previous line: [" + writeModifiersLines.get(index - i) + "]\n");
      }
    }
    message.append("unmatched line: [" + writeModifiersLine + "]");
    assertThat(message.toString(), writeModifiersIterator.hasNext(), is(false));
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
            familiarMods.get(DoubleModifier.ITEMDROP),
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
            familiarMods.get(mod), closeTo(fairyFunction(weight) * otherFairyEffectiveness, 0.001));
        assertThat(
            familiarMods.get(DoubleModifier.ITEMDROP),
            closeTo(fairyFunction(weight) * itemFairyEffectiveness, 0.001));
      }
    }
  }
}
