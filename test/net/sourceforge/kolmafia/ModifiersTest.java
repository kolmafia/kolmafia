package net.sourceforge.kolmafia;

import static internal.helpers.Player.addEffect;
import static internal.helpers.Player.equip;
import static internal.helpers.Player.inPath;
import static internal.helpers.Player.isClass;
import static internal.helpers.Player.isDay;
import static internal.helpers.Player.setHP;
import static internal.helpers.Player.setMP;
import static internal.helpers.Player.setProperty;
import static internal.helpers.Player.setStats;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map.Entry;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ModifiersTest {

  @Test
  public void patriotShieldClassModifiers() {
    // Wide-reaching unit test for getModifiers
    var cleanup = isClass(AscensionClass.AVATAR_OF_JARLSBERG);
    try (cleanup) {
      Modifiers mods = Modifiers.getModifiers("Item", "Patriot Shield");

      // Always has
      assertEquals(3, mods.get(Modifiers.EXPERIENCE));

      // Has because of class
      assertEquals(5.0, mods.get(Modifiers.MP_REGEN_MIN));
      assertEquals(6.0, mods.get(Modifiers.MP_REGEN_MAX));
      assertEquals(20.0, mods.get(Modifiers.SPELL_DAMAGE));

      // Does not have because of class
      assertEquals(0, mods.get(Modifiers.HP_REGEN_MAX));
      assertEquals(0, mods.get(Modifiers.HP_REGEN_MIN));
      assertEquals(0, mods.get(Modifiers.WEAPON_DAMAGE));
      assertEquals(0, mods.get(Modifiers.DAMAGE_REDUCTION));
      assertEquals(0, mods.get(Modifiers.FAMILIAR_WEIGHT));
      assertEquals(0, mods.get(Modifiers.RANGED_DAMAGE));
      assertEquals(false, mods.getBoolean(Modifiers.FOUR_SONGS));
      assertEquals(0, mods.get(Modifiers.COMBAT_MANA_COST));
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7})
  public void tuesdayRubyModifiers(int dotw) {
    // Wide-reaching unit test for getModifiers
    var cleanup = isDay(new GregorianCalendar(2017, Calendar.JANUARY, dotw));
    try (cleanup) {
      Modifiers mods = Modifiers.getModifiers("Item", "Tuesday's Ruby");

      assertThat(mods.get(Modifiers.MEATDROP), equalTo(dotw == Calendar.SUNDAY ? 5.0 : 0.0));
      assertThat(mods.get(Modifiers.MUS_PCT), equalTo(dotw == Calendar.MONDAY ? 5.0 : 0.0));
      assertThat(mods.get(Modifiers.MP_REGEN_MIN), equalTo(dotw == Calendar.TUESDAY ? 3.0 : 0.0));
      assertThat(mods.get(Modifiers.MP_REGEN_MAX), equalTo(dotw == Calendar.TUESDAY ? 7.0 : 0.0));
      assertThat(mods.get(Modifiers.MYS_PCT), equalTo(dotw == Calendar.WEDNESDAY ? 5.0 : 0.0));
      assertThat(mods.get(Modifiers.ITEMDROP), equalTo(dotw == Calendar.THURSDAY ? 5.0 : 0.0));
      assertThat(mods.get(Modifiers.MOX_PCT), equalTo(dotw == Calendar.FRIDAY ? 5.0 : 0.0));
      assertThat(mods.get(Modifiers.HP_REGEN_MIN), equalTo(dotw == Calendar.SATURDAY ? 3.0 : 0.0));
      assertThat(mods.get(Modifiers.HP_REGEN_MAX), equalTo(dotw == Calendar.SATURDAY ? 7.0 : 0.0));
    }
  }

  @Test
  public void testSynergies() {
    // The "synergy" bitmap modifier is assigned dynamically, based on appearance order in
    // Modifiers.txt
    // The first Synergetic item seen gets 0b00001, the 2nd: 0b00010, 3rd: 0b00100, etc.

    for (Entry<String, Integer> entry : Modifiers.getSynergies()) {
      String name = entry.getKey();
      int mask = entry.getValue().intValue();

      int manualMask = 0;
      for (String piece : name.split("/")) {
        Modifiers mods = Modifiers.getModifiers("Item", piece);
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
      KoLCharacter.setStatPoints(0, 0, myst, myst * myst, 0, 0);
      Modifiers mods = Modifiers.getModifiers("Skill", "Intrinsic Spiciness");
      assertEquals(Math.min(i, 10), mods.get(Modifiers.SAUCE_SPELL_DAMAGE));
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
    mod.add(Modifiers.COMBAT_RATE, 25, "Start");
    mod.add(Modifiers.COMBAT_RATE, 7, "32");
    assertEquals(26, mod.get(Modifiers.COMBAT_RATE));
    mod.add(Modifiers.COMBAT_RATE, 9, "41");
    assertEquals(28, mod.get(Modifiers.COMBAT_RATE));
    mod.add(Modifiers.COMBAT_RATE, 9, "50");
    assertEquals(30, mod.get(Modifiers.COMBAT_RATE));

    mod = new Modifiers();
    mod.add(Modifiers.COMBAT_RATE, -25, "Start");
    mod.add(Modifiers.COMBAT_RATE, -7, "-32");
    assertEquals(-26, mod.get(Modifiers.COMBAT_RATE));
    mod.add(Modifiers.COMBAT_RATE, -9, "-41");
    assertEquals(-28, mod.get(Modifiers.COMBAT_RATE));
    mod.add(Modifiers.COMBAT_RATE, -9, "-50");
    assertEquals(-30, mod.get(Modifiers.COMBAT_RATE));
  }

  @Test
  void fixodeneConsideredInFamiliarModifiers() {
    var cleanups = addEffect("Fidoxene");

    try (cleanups) {
      Modifiers familiarMods = new Modifiers();
      var familiar = FamiliarData.registerFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 0);

      familiarMods.applyFamiliarModifiers(familiar, null);

      assertThat(familiarMods.get(Modifiers.ITEMDROP), closeTo(50.166, 0.001));
    }
  }

  @Test
  void fixodeneConsideredInFamiliarModifiersNotExceedingTwenty() {
    var cleanups = addEffect("Fidoxene");

    try (cleanups) {
      Modifiers familiarMods = new Modifiers();
      var familiar = FamiliarData.registerFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 400);

      familiarMods.applyFamiliarModifiers(familiar, null);

      assertThat(familiarMods.get(Modifiers.ITEMDROP), closeTo(50.166, 0.001));
    }
  }

  @Nested
  class Squint {
    @BeforeAll
    public static void setup() {
      Preferences.reset("squinter");
    }

    @Test
    public void squintAffectsUmbrella() {
      var cleanups =
          new Cleanups(
              equip(EquipmentManager.OFFHAND, "unbreakable umbrella"),
              setProperty("umbrellaState", "bucket style"),
              addEffect("Steely-Eyed Squint"));
      try (cleanups) {
        KoLCharacter.recalculateAdjustments();
        Modifiers mods = KoLCharacter.getCurrentModifiers();
        var item = mods.get(Modifiers.ITEMDROP);
        assertThat(item, equalTo(50.0));
      }
    }
  }

  @Nested
  class BuffedHP {
    @AfterEach
    private void afterEach() {
      EquipmentManager.resetEquipment();
    }

    @Test
    public void correctlyCalculatesSealClubberMaximumHP() {
      var cleanups = new Cleanups(isClass(AscensionClass.SEAL_CLUBBER), setStats(100, 100, 100));
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
      var cleanups = new Cleanups(isClass(AscensionClass.PASTAMANCER), setStats(100, 100, 100));
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
      var cleanups = new Cleanups(isClass(AscensionClass.VAMPYRE), setStats(100, 100, 100));
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
      var cleanups = new Cleanups(inPath(Path.YOU_ROBOT), setStats(100, 100, 100));
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
              isClass(AscensionClass.GREY_GOO), setStats(100, 100, 100), setHP(176, 176, 176));
      try (cleanups) {
        // Base HP = (starting value + absorptions)
        // Buffed HP = Base HP + mod(HP)

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
        assertEquals(216, stats[Modifiers.BUFFED_HP]);

        // extra-wide head candle (+100% HP)
        EquipmentManager.setEquipment(
            EquipmentManager.HAT, ItemPool.get(ItemPool.EXTRA_WIDE_HEAD_CANDLE));
        KoLCharacter.recalculateAdjustments(false);
        stats = mods.predict();
        assertEquals(100, stats[Modifiers.BUFFED_MUS]);
        assertEquals(176, stats[Modifiers.BUFFED_HP]);
      }
    }
  }

  @Nested
  class BuffedMP {
    @AfterEach
    private void afterEach() {
      EquipmentManager.resetEquipment();
    }

    @Test
    public void correctlyCalculatesSaucerorMaximumMP() {
      var cleanups = new Cleanups(isClass(AscensionClass.SAUCEROR), setStats(100, 100, 100));
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
      var cleanups = new Cleanups(isClass(AscensionClass.TURTLE_TAMER), setStats(100, 100, 100));
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
              isClass(AscensionClass.GREY_GOO), setStats(100, 100, 100), setMP(126, 126, 126));
      try (cleanups) {
        // Base MP = (starting value + absorptions)
        // Buffed MP = Base MP + mod(MP)

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
        assertEquals(166, stats[Modifiers.BUFFED_MP]);

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
    public void correctlyCalculatesMoxieControlledMaximumMP() {
      // moxie magnet: Moxie Controls MP
      var cleanups =
          new Cleanups(
              isClass(AscensionClass.DISCO_BANDIT),
              setStats(100, 100, 150),
              equip(EquipmentManager.ACCESSORY1, "moxie magnet"));
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
}
