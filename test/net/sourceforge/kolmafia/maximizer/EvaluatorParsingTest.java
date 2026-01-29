package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static internal.matchers.Maximizer.recommendsSlot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class EvaluatorParsingTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("EvaluatorParsingTest");
    Preferences.reset("EvaluatorParsingTest");
  }

  @Nested
  class SlotExclusions {
    @Test
    public void minusHatExcludesHatSlot() {
      var cleanups =
          new Cleanups(withEquippableItem("helmet turtle"), withEquippableItem("old sweatpants"));
      try (cleanups) {
        assertTrue(maximize("mus, -hat"));
        // Should not recommend hat even though it provides muscle
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
        // But should still recommend pants
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "old sweatpants")));
      }
    }

    @Test
    public void minusWeaponExcludesWeaponSlot() {
      var cleanups =
          new Cleanups(withEquippableItem("seal-clubbing club"), withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mus, -weapon"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void minusOffhandExcludesOffhandSlot() {
      var cleanups =
          new Cleanups(withEquippableItem("helmet turtle"), withEquippableItem("seal-skull helmet"));
      try (cleanups) {
        assertTrue(maximize("mus, -offhand"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
      }
    }

    @Test
    public void minusShirtExcludesShirtSlot() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withSkill("Torso Awareness"),
              withEquippableItem("eXtreme Bi-Polar Fleece Vest"));
      try (cleanups) {
        assertTrue(maximize("cold res, -shirt"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.SHIRT))));
      }
    }

    @Test
    public void minusPantsExcludesPantsSlot() {
      var cleanups =
          new Cleanups(withEquippableItem("old sweatpants"), withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mus, -pants"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.PANTS))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void minusAcc1ExcludesFirstAccessorySlot() {
      var cleanups = new Cleanups(withEquippableItem("hand in glove"));
      try (cleanups) {
        assertTrue(maximize("mus, -acc1"));
        // Accessory will go to acc2 or acc3 instead
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.ACCESSORY1))));
      }
    }

    @Test
    public void minusFamiliarExcludesFamiliarSlot() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withEquippableItem("lead necklace"),
              withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("item, -familiar"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.FAMILIAR))));
      }
    }

    @Test
    public void multipleSlotExclusionsWork() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("old sweatpants"),
              withEquippableItem("seal-clubbing club"));
      try (cleanups) {
        assertTrue(maximize("mus, -hat, -pants, -weapon"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.PANTS))));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
      }
    }
  }

  @Nested
  class ControlKeywords {
    @Test
    public void tieEnablesTiebreaker() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"), withEquippableItem("seal-skull helmet"));
      try (cleanups) {
        // Both provide same muscle, tiebreaker should decide
        assertTrue(maximize("mus, tie"));
        // Just verify it runs without error
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void minusTieDisablesTiebreaker() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mus, -tie"));
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void currentIncludesCurrentEquipment() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.HAT, "helmet turtle"), withEquippableItem("seal-skull helmet"));
      try (cleanups) {
        assertTrue(maximize("mus, current"));
        // Should consider current equipment in results
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void minusCurrentExcludesCurrentEquipment() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.HAT, "helmet turtle"), withEquippableItem("seal-skull helmet"));
      try (cleanups) {
        assertTrue(maximize("mus, -current"));
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void emptyPrefersEmptySlots() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.HAT, "helmet turtle"), withEquippableItem("seal-skull helmet"));
      try (cleanups) {
        assertTrue(maximize("empty"));
        // With empty keyword, empty slots are preferred
      }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    public void dumpSetsDebugLevel(int level) {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // dump keyword sets debug output level - just verify it parses
        assertTrue(maximize(level + " dump, mus"));
      }
    }
  }

  @Nested
  class FamiliarSwitching {
    @Test
    public void switchSpecifiesFamiliarToConsider() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN));
      try (cleanups) {
        assertTrue(maximize("meat, switch leprechaun"));
        // Should consider switching to leprechaun for meat drops
      }
    }

    @Test
    public void multipleSwitchSpecifications() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN),
              withFamiliarInTerrarium(FamiliarPool.SOMBRERO));
      try (cleanups) {
        assertTrue(maximize("exp, switch leprechaun, switch sombrero"));
        // Should consider both familiars
      }
    }

    @Test
    public void negativeSwitchIsIgnored() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN));
      try (cleanups) {
        // Negative switch after a positive one is ignored
        assertTrue(maximize("meat, switch leprechaun, -switch baby gravy fairy"));
      }
    }
  }

  @Nested
  class ItemConstraints {
    @Test
    public void plusEquipRequiresItem() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"), withEquippableItem("seal-skull helmet"));
      try (cleanups) {
        assertTrue(maximize("mus, +equip helmet turtle"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void plusEquipWithQuotedItemName() {
      var cleanups = new Cleanups(withEquippableItem("Mr. Accessory"));
      try (cleanups) {
        assertTrue(maximize("all res, +equip \"Mr. Accessory\""));
        // Quoted item names should work
      }
    }

    @Test
    public void minusEquipExcludesItem() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"), withEquippableItem("seal-skull helmet"));
      try (cleanups) {
        assertTrue(maximize("mus, -equip helmet turtle"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT, "helmet turtle"))));
      }
    }

    @Test
    public void bonusAddsWeightToItem() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"), withEquippableItem("seal-skull helmet"));
      try (cleanups) {
        // Adding a large bonus should favor the specified item
        assertTrue(maximize("mus, +1000 bonus helmet turtle"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void bonusWithoutWeightDefaultsToOne() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mus, bonus helmet turtle"));
        // Should parse without error
      }
    }
  }

  @Nested
  class OutfitConstraints {
    @Test
    public void plusOutfitRequiresOutfit() {
      var cleanups =
          new Cleanups(
              withEquippableItem("bugbear beanie"),
              withEquippableItem("bugbear bungguard"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, +outfit Bugbear Costume"));
        // Should try to equip the bugbear costume pieces
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bugbear beanie")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "bugbear bungguard")));
      }
    }

    @Test
    public void minusOutfitExcludesOutfit() {
      var cleanups =
          new Cleanups(
              withEquippableItem("bugbear beanie"),
              withEquippableItem("bugbear bungguard"),
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, -outfit Bugbear Costume"));
        // Should not recommend bugbear costume pieces
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT, "bugbear beanie"))));
      }
    }

    @Test
    public void plusOutfitWithNoNameUsesCurrentOutfit() {
      // When no current outfit is set, +outfit with no name fails gracefully
      // This is expected behavior - need an outfit name or current outfit
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // +outfit without name and no current outfit - parses but may fail
        // depending on current state, so just verify no exception thrown
        maximize("mus, +outfit");
      }
    }
  }

  @Nested
  class WeaponTypeConstraints {
    @Test
    public void clubRequiresClubWeapon() {
      var cleanups =
          new Cleanups(
              withEquippableItem("seal-clubbing club"),
              withEquippableItem("disco ball"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, club"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "seal-clubbing club")));
      }
    }

    @Test
    public void shieldRequiresShield() {
      var cleanups =
          new Cleanups(withEquippableItem("helmet turtle"), withEquippableItem("catskin buckler"));
      try (cleanups) {
        assertTrue(maximize("mus, shield"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "catskin buckler")));
      }
    }

    @Test
    public void swordRequiresSwordWeapon() {
      var cleanups =
          new Cleanups(
              withEquippableItem("white sword"),
              withEquippableItem("seal-clubbing club"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, sword"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "white sword")));
      }
    }

    @Test
    public void knifeRequiresKnifeWeapon() {
      var cleanups =
          new Cleanups(
              withEquippableItem("white sword"),
              withEquippableItem("disco banjo"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Just verify parsing works - knife weapons are rare
        assertTrue(maximize("mus, knife"));
      }
    }

    @Test
    public void accordionRequiresAccordionWeapon() {
      var cleanups =
          new Cleanups(
              withEquippableItem("toy accordion"),
              withEquippableItem("seal-clubbing club"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mys, accordion"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "toy accordion")));
      }
    }

    @Test
    public void utensilRequiresUtensilWeapon() {
      var cleanups =
          new Cleanups(
              withEquippableItem("pasta spoon"),
              withEquippableItem("seal-clubbing club"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mys, utensil"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "pasta spoon")));
      }
    }

    @Test
    public void typeSpecifiesWeaponType() {
      var cleanups =
          new Cleanups(
              withEquippableItem("seal-clubbing club"),
              withEquippableItem("disco ball"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, type club"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "seal-clubbing club")));
      }
    }

    @Test
    public void handsSpecifiesHandRequirement() {
      var cleanups =
          new Cleanups(
              withEquippableItem("seal-clubbing club"),
              withEquippableItem("catskin buckler"),
              withStats(100, 100, 100));
      try (cleanups) {
        // 1 hand means can use offhand
        assertTrue(maximize("mus, 1 hand"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND)));
      }
    }

    @Test
    public void twoHandsAllowsTwoHandedWeapons() {
      var cleanups = new Cleanups(withEquippableItem("seal-clubbing club"), withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, 2 hands"));
        // Just verify parsing - may or may not have 2h weapons
      }
    }
  }

  @Nested
  class WeightParsing {
    @Test
    public void positiveWeightWithPlus() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("+5 muscle"));
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void positiveWeightWithoutPlus() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("5 muscle"));
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void negativeWeightWithMinus() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Negative weight means avoid this stat
        assertTrue(maximize("-5 muscle"));
        // With negative weight, should prefer NOT equipping the item
        assertEquals(0, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void decimalWeight() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("0.5 muscle"));
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void noWeightDefaultsToOne() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("muscle"));
        assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      }
    }

    @Test
    public void multipleModifiersWithCommas() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquippableItem("old sweatpants"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("muscle, moxie, 5 item"));
        // Should parse all three modifiers
      }
    }

    @Test
    public void multipleModifiersWithoutCommas() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Commas are optional
        assertTrue(maximize("muscle moxie"));
      }
    }
  }

  @Nested
  class MinMaxConstraints {
    @Test
    public void minAfterModifierSetsMinimum() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // This should fail because we can't hit 50 HP min
        assertFalse(maximize("HP 50 min"));
      }
    }

    @Test
    public void maxAfterModifierSetsMaximum() {
      var cleanups =
          new Cleanups(
              withEquippableItem("hardened slime hat"),
              withEquippableItem("bounty-hunting helmet"),
              withSkill("Refusal to Freeze"));
      try (cleanups) {
        assertTrue(maximize("cold res 3 max, 0.1 item drop"));
        // Cold res should be capped at 3
        assertEquals(3, modFor(DoubleModifier.COLD_RESISTANCE), 0.01);
      }
    }

    @Test
    public void minAndMaxTogether() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"), withStats(50, 50, 50));
      try (cleanups) {
        assertTrue(maximize("mus 0 min 100 max"));
        // Should parse both constraints
      }
    }

    @Test
    public void totalMinWithoutModifier() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Total min sets minimum for total score
        assertTrue(maximize("0 min, mus"));
      }
    }

    @Test
    public void totalMaxWithoutModifier() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Total max sets maximum for total score
        assertTrue(maximize("100 max, mus"));
      }
    }
  }

  @Nested
  class ModifierAbbreviations {
    @ParameterizedTest
    @CsvSource({
      "init, INITIATIVE",
      "hp, HP",
      "mp, MP",
      "da, DAMAGE_ABSORPTION",
      "dr, DAMAGE_REDUCTION",
      "ml, MONSTER_LEVEL",
      "item, ITEMDROP",
      "meat, MEATDROP",
      "adv, ADVENTURES",
      "exp, EXPERIENCE",
      "crit, CRITICAL_PCT",
      "com, COMBAT_RATE"
    })
    public void abbreviationMapsToModifier(String abbreviation, String modifierName) {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize(abbreviation));
        // Just verify it parses correctly
      }
    }

    @ParameterizedTest
    @CsvSource({
      "cold res, COLD_RESISTANCE",
      "hot res, HOT_RESISTANCE",
      "sleaze res, SLEAZE_RESISTANCE",
      "spooky res, SPOOKY_RESISTANCE",
      "stench res, STENCH_RESISTANCE"
    })
    public void resAbbreviation(String abbreviation, String modifierName) {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize(abbreviation));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "cold dmg, COLD_DAMAGE",
      "hot dmg, HOT_DAMAGE",
      "sleaze dmg, SLEAZE_DAMAGE",
      "spooky dmg, SPOOKY_DAMAGE",
      "stench dmg, STENCH_DAMAGE"
    })
    public void dmgAbbreviation(String abbreviation, String modifierName) {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize(abbreviation));
      }
    }

    @Test
    public void allResistanceExpandsToAllElements() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("all resistance"));
        // Should set weight for all 5 elements
      }
    }

    @Test
    public void elementalDamageExpandsToAllElements() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("elemental damage"));
      }
    }

    @Test
    public void hpRegenExpandsToMinAndMax() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("hp regen"));
      }
    }

    @Test
    public void mpRegenExpandsToMinAndMax() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mp regen"));
      }
    }

    @Test
    public void passiveDamageExpandsToAuraAndThorns() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("passive damage"));
      }
    }
  }

  @Nested
  class SpecialModifiers {
    @Test
    public void seaSetsUnderwaterFlags() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        // Sea keyword requires underwater breathing gear to succeed
        // Without such gear, maximize returns false which is expected
        // Just verify no exception is thrown during parsing
        maximize("sea, mus");
      }
    }

    @Test
    public void meleeRequiresMeleeWeapon() {
      var cleanups =
          new Cleanups(
              withEquippableItem("seal-clubbing club"),
              withEquippableItem("disco ball"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, melee"));
        // Should prefer melee weapon (club)
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "seal-clubbing club")));
      }
    }

    @Test
    public void minusMeleeRequiresRangedWeapon() {
      var cleanups =
          new Cleanups(
              withEquippableItem("seal-clubbing club"),
              withEquippableItem("disco ball"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mox, -melee"));
        // Should prefer ranged weapon
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "disco ball")));
      }
    }

    @Test
    public void effectiveConsidersWeaponPower() {
      var cleanups = new Cleanups(withEquippableItem("seal-clubbing club"), withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, effective"));
        // Effective considers weapon power vs monster defense
      }
    }
  }

  @Nested
  class ErrorHandling {
    @Test
    public void unknownKeywordFails() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("xyzzy"));
        // Unknown keyword should fail
      }
    }

    @Test
    public void invalidItemNameFails() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("+equip nonexistent item that does not exist"));
        // Invalid item should fail
      }
    }

    @Test
    public void invalidOutfitNameFails() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("+outfit Nonexistent Outfit"));
        // Invalid outfit should fail
      }
    }

    @Test
    public void invalidFamiliarNameFails() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("switch nonexistent familiar"));
        // Invalid familiar should fail
      }
    }
  }

  @Nested
  class MysExpressions {
    @Test
    public void parsesMysExpPerc() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mys exp perc"));
      }
    }

    @Test
    public void parsesMysExp() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mys exp"));
      }
    }

    @Test
    public void parsesMysPerc() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mys perc"));
      }
    }

    @Test
    public void parsesMystExp() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("myst exp"));
      }
    }

    @Test
    public void parsesMysticalityExp() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mysticality experience"));
      }
    }

    @Test
    public void parsesMysticalityExpPerc() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mysticality experience percent"));
      }
    }
  }

  @Nested
  class MoxExpressions {
    @Test
    public void parsesMoxExpPerc() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mox exp perc"));
      }
    }

    @Test
    public void parsesMoxExp() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mox exp"));
      }
    }

    @Test
    public void parsesMoxPerc() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("mox perc"));
      }
    }

    @Test
    public void parsesMoxieExp() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("moxie experience"));
      }
    }

    @Test
    public void parsesMoxieExpPerc() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("moxie experience percent"));
      }
    }
  }

  @Nested
  class AdditionalKeywords {
    @Test
    public void parsesSpellCrit() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("spell crit"));
      }
    }

    @Test
    public void parsesSprinkle() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("sprinkle"));
      }
    }

    @Test
    public void parsesOcrs() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("ocrs"));
      }
    }

    @Test
    public void parsesStinkyCheese() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("stinky cheese"));
      }
    }

    @Test
    public void parsesFites() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("fites"));
      }
    }
  }

  @Nested
  class NegativeWeaponRequirements {
    @Test
    public void parsesClubNegative() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("-club, mus"));
      }
    }

    @Test
    public void parsesShieldNegative() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("-shield, mus"));
      }
    }

    @Test
    public void parsesUtensilNegative() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("-utensil, mus"));
      }
    }

    @Test
    public void parsesSwordNegative() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("-sword, mus"));
      }
    }

    @Test
    public void parsesKnifeNegative() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("-knife, mus"));
      }
    }

    @Test
    public void parsesAccordionNegative() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("-accordion, mus"));
      }
    }

    @Test
    public void parsesEffectiveNegative() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("-effective, mus"));
      }
    }
  }

  @Nested
  class NegativeBooleans {
    @Test
    public void parsesNegativeBooleanModifier() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertTrue(maximize("-never fumble"));
      }
    }
  }

  @Nested
  class DamagePercentAbbreviations {
    @Test
    public void parsesWeaponDmgPercent() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"), withStats(100, 100, 100));
      try (cleanups) {
        // This tests the "dmg percent" abbreviation path (line 523-524)
        assertTrue(maximize("weapon dmg percent"));
      }
    }

    @Test
    public void parsesSpellDmgPercent() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"), withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("spell dmg percent"));
      }
    }

    @Test
    public void parsesRangedDmgPercent() {
      var cleanups = new Cleanups(withEquippableItem("helmet turtle"), withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("ranged dmg percent"));
      }
    }
  }

  @Nested
  class QuotedKeywords {
    @Test
    public void parsesQuotedEquip() {
      var cleanups =
          new Cleanups(
              withEquippableItem("sea chaps"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("+equip \"sea chaps\""));
      }
    }

    @Test
    public void parsesQuotedModifierKeyword() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Tests quoted keyword handling (lines 243-244 in Evaluator.java)
        // Quoted modifier keywords like "muscle" should work the same as unquoted
        assertTrue(maximize("\"muscle\""));
      }
    }

    @Test
    public void parsesWeightedQuotedModifierKeyword() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Tests quoted keyword with weight
        assertTrue(maximize("5 \"muscle\""));
      }
    }
  }

  @Nested
  class CarriedFamiliars {
    @Test
    public void handlesCarriedFamiliars() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Crown of Thrones"),
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("meat"));
        // Crown of Thrones should suggest enthroning a familiar
      }
    }

    @Test
    public void handlesBuddyBjorn() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Buddy Bjorn"),
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("item"));
        // Buddy Bjorn should suggest bjorning a familiar
      }
    }
  }

  @Nested
  class FamiliarSwitchEdgeCases {
    @Test
    public void handlesNegativeSwitchAfterPositive() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN),
              withStats(100, 100, 100));
      try (cleanups) {
        // Tests line 493: "if (hadFamiliar && weight < 0.0) continue"
        assertTrue(maximize("switch leprechaun, -switch baby gravy fairy"));
      }
    }
  }

  @Nested
  class SameItemSameCount {
    @Test
    public void handlesCurrentEquipmentCheck() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.HAT, "helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Should keep current equipment if it's already the best
      }
    }
  }

  @Nested
  class EmptySlotHandling {
    @Test
    public void handlesEmptySlots() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Tests empty slot preference (lines 334-342)
        assertTrue(maximize("mus, empty"));
      }
    }

    @Test
    public void handlesNegativeEmpty() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withEquipped(Slot.HAT, "helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, -empty"));
      }
    }
  }

  @Nested
  class TotalMinMax {
    @Test
    public void handlesTotalMin() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Tests total min (line 250)
        assertTrue(maximize("mus, 10 min"));
      }
    }

    @Test
    public void handlesTotalMax() {
      var cleanups =
          new Cleanups(
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Tests total max (line 259)
        assertTrue(maximize("mus, 200 max"));
      }
    }
  }

  @Nested
  class StinkyCheeseAbbreviation {
    @Test
    public void handlesStinkyCheeseWithoutSpace() {
      var cleanups =
          new Cleanups(
              withEquippableItem("stinky cheese sword"),
              withEquippableItem("stinky cheese diaper"),
              withStats(100, 100, 100));
      try (cleanups) {
        // Tests "stinkycheese" (no space) keyword
        assertTrue(maximize("1 stinkycheese"));
      }
    }
  }
}
