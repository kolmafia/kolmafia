package net.sourceforge.kolmafia;

import static internal.helpers.Player.isClass;
import static internal.helpers.Player.isDay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Map.Entry;
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
}
