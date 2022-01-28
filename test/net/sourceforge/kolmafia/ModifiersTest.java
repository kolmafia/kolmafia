package net.sourceforge.kolmafia;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map.Entry;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.persistence.EffectDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class ModifiersTest {
  @BeforeEach
  private void beforeEach() {
    KoLCharacter.reset("ModifiersTest");
    Preferences.reset("ModifiersTest");
  }

  @Test
  public void patriotShieldClassModifiers() {
    // Wide-reaching unit test for getModifiers
    KoLCharacter.setAscensionClass(AscensionClass.AVATAR_OF_JARLSBERG);
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
  public void bendinHellWorks() {
    Modifiers mods = new Modifiers();
    var effects =
        List.of(
            EffectPool.get(EffectDatabase.getEffectId("Benetton's Medley of Diversity")),
            EffectPool.get(EffectDatabase.getEffectId("Fustulent")),
            EffectPool.get(EffectDatabase.getEffectId("Bendin' Hell")));

    for (var effect : effects) {
      mods.add(Modifiers.getEffectModifiers(effect.getEffectId()));
    }

    mods.applyMultipliers(effects, EquipmentManager.allEquipment(), false);

    assertThat(mods.get(Modifiers.HOT_DAMAGE), is(30.0));
    assertThat(mods.get(Modifiers.COLD_DAMAGE), is(30.0));
    assertThat(mods.get(Modifiers.SLEAZE_DAMAGE), is(30.0));
    assertThat(mods.get(Modifiers.SPOOKY_DAMAGE), is(30.0));
    assertThat(mods.get(Modifiers.STENCH_DAMAGE), is(60.0));
    assertThat(mods.get(Modifiers.HOT_SPELL_DAMAGE), is(0.0));
    assertThat(mods.get(Modifiers.COLD_SPELL_DAMAGE), is(0.0));
    assertThat(mods.get(Modifiers.SLEAZE_SPELL_DAMAGE), is(0.0));
    assertThat(mods.get(Modifiers.SPOOKY_SPELL_DAMAGE), is(0.0));
    assertThat(mods.get(Modifiers.STENCH_SPELL_DAMAGE), is(30.0));
  }

  @Test
  public void bowLeggedSwaggerWorks() {
    Modifiers mods = new Modifiers();
    var effects =
        List.of(
            EffectPool.get(EffectDatabase.getEffectId("Bone Springs")),
            EffectPool.get(EffectDatabase.getEffectId("Bow-Legged Swagger")));

    for (var effect : effects) {
      mods.add(Modifiers.getEffectModifiers(effect.getEffectId()));
    }

    mods.applyMultipliers(effects, EquipmentManager.allEquipment(), false);

    assertThat(mods.get(Modifiers.INITIATIVE), is(40.0));
  }

  @Test
  public void dirtyPearWorks() {
    Modifiers mods = new Modifiers();
    var effects =
        List.of(
            EffectPool.get(EffectDatabase.getEffectId("Human-Slime Hybrid")),
            EffectPool.get(EffectDatabase.getEffectId("Dirty Pear")));

    for (var effect : effects) {
      mods.add(Modifiers.getEffectModifiers(effect.getEffectId()));
    }

    mods.applyMultipliers(effects, EquipmentManager.allEquipment(), false);

    assertThat(mods.get(Modifiers.SLEAZE_DAMAGE), is(40.0));
    assertThat(mods.get(Modifiers.SLEAZE_SPELL_DAMAGE), is(40.0));
  }

  private Modifiers prepareGarbageShirt(boolean speculate) {
    // Start building inputs for multiplier
    Modifiers mods = new Modifiers();
    var equipment = EquipmentManager.allEquipment();

    // Equip the garbage shirt
    equipment[EquipmentManager.SHIRT] = AdventureResult.tallyItem("makeshift garbage shirt");

    // Give some experience modifiers to buff
    var effects = List.of(EffectPool.get(EffectDatabase.getEffectId("Lifted Spirits")));
    for (var effect : effects) {
      mods.add(Modifiers.getEffectModifiers(effect.getEffectId()));
    }

    // Apply the multipliers!
    mods.applyMultipliers(effects, equipment, speculate);

    return mods;
  }

  @Test
  public void garbageShirtWorks() {
    Preferences.setInteger("garbageShirtCharge", 10);

    var mods = prepareGarbageShirt(false);

    assertThat(mods.get(Modifiers.EXPERIENCE), is(10.0));
    assertThat(mods.get(Modifiers.MUS_EXPERIENCE), is(0.0));
    assertThat(mods.get(Modifiers.MYS_EXPERIENCE), is(0.0));
    assertThat(mods.get(Modifiers.MOX_EXPERIENCE), is(0.0));
  }

  @Test
  public void considerUnusedGarbageShirtWhenSpeculating() {
    Preferences.setBoolean("_garbageItemChanged", false);

    var mods = prepareGarbageShirt(true);

    assertThat(mods.get(Modifiers.EXPERIENCE), is(10.0));
    assertThat(mods.get(Modifiers.MUS_EXPERIENCE), is(0.0));
    assertThat(mods.get(Modifiers.MYS_EXPERIENCE), is(0.0));
    assertThat(mods.get(Modifiers.MOX_EXPERIENCE), is(0.0));
  }

  private Modifiers prepareChampagneBottle(int slot, boolean speculate) {
    // Start building inputs for multiplier
    Modifiers mods = new Modifiers();
    var equipment = EquipmentManager.allEquipment();

    // Equip the bottle
    equipment[slot] = AdventureResult.tallyItem("broken champagne bottle");

    // Give some item modifiers to buff
    var effects = List.of(EffectPool.get(EffectDatabase.getEffectId("Lucky Struck")));
    for (var effect : effects) {
      mods.add(Modifiers.getEffectModifiers(effect.getEffectId()));
    }

    // Apply the multipliers!
    mods.applyMultipliers(effects, equipment, speculate);

    return mods;
  }

  @ParameterizedTest
  @ValueSource(
      ints = {EquipmentManager.OFFHAND, EquipmentManager.WEAPON, EquipmentManager.FAMILIAR})
  public void brokenChampagneBottleWorks(int slot) {
    Preferences.setInteger("garbageChampagneCharge", 10);
    var mods = prepareChampagneBottle(slot, false);

    assertThat(mods.get(Modifiers.ITEMDROP), is(40.0));
  }

  @Test
  public void considerUnusedBrokenChampagneBottleWhenSpeculating() {
    Preferences.setBoolean("_garbageItemChanged", false);
    var mods = prepareChampagneBottle(EquipmentManager.WEAPON, true);

    assertThat(mods.get(Modifiers.ITEMDROP), is(40.0));
  }
}
