package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MaximizerTest {
  @BeforeEach
  public void init() {
    KoLCharacter.reset(true);
  }

  // basic

  @Test
  public void changesGear() {
    canUse("helmet turtle");
    assertTrue(maximize("mus"));
    assertEquals(1, modFor("Buffed Muscle"), 0.01);
  }

  @Test
  public void equipsItemsOnlyIfHasStats() {
    canUse("helmet turtle");
    addItem("wreath of laurels");
    assertTrue(maximize("mus"));
    assertEquals(1, modFor("Buffed Muscle"), 0.01);
    recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
  }

  @Test
  public void nothingBetterThanSomething() {
    canUse("helmet turtle");
    assertTrue(maximize("-mus"));
    assertEquals(0, modFor("Buffed Muscle"), 0.01);
  }

  // max

  @Test
  public void maxKeywordStopsCountingBeyondTarget() {
    canUse("hardened slime hat");
    canUse("bounty-hunting helmet");
    addSkill("Refusal to Freeze");
    assertTrue(maximize("cold res 3 max, 0.1 item drop"));

    assertEquals(3, modFor("Cold Resistance"), 0.01);
    assertEquals(20, modFor("Item Drop"), 0.01);

    recommendedSlotIs(EquipmentManager.HAT, "bounty-hunting helmet");
  }

  @Test
  public void startingMaxKeywordTerminatesEarlyIfConditionMet() {
    canUse("hardened slime hat");
    canUse("bounty-hunting helmet");
    addSkill("Refusal to Freeze");
    maximize("3 max, cold res");

    assertTrue(
        Maximizer.boosts.stream()
            .anyMatch(
                b -> b.toString().contains("(maximum achieved, no further combinations checked)")));
  }

  // min

  @Test
  public void minKeywordFailsMaximizationIfNotHit() {
    canUse("helmet turtle");
    assertFalse(maximize("mus 2 min"));
    // still provides equipment
    recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
  }

  @Test
  public void minKeywordPassesMaximizationIfHit() {
    canUse("wreath of laurels");
    assertTrue(maximize("mus 2 min"));
  }

  @Test
  public void startingMinKeywordFailsMaximizationIfNotHit() {
    canUse("helmet turtle");
    assertFalse(maximize("2 min, mus"));
    // still provides equipment
    recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
  }

  @Test
  public void startingMinKeywordPassesMaximizationIfHit() {
    canUse("wreath of laurels");
    assertTrue(maximize("2 min, mus"));
  }

  // clownosity

  @Test
  public void clownosityTriesClownEquipment() {
    canUse("clown wig");
    assertFalse(maximize("clownosity"));
    // still provides equipment
    recommendedSlotIs(EquipmentManager.HAT, "clown wig");
  }

  @Test
  public void clownositySucceedsWithEnoughEquipment() {
    canUse("clown wig");
    canUse("polka-dot bow tie");
    assertTrue(maximize("clownosity"));
    recommendedSlotIs(EquipmentManager.HAT, "clown wig");
    recommendedSlotIs(EquipmentManager.ACCESSORY1, "polka-dot bow tie");
  }

  // raveosity

  @Test
  public void raveosityTriesRaveEquipment() {
    canUse("rave visor");
    canUse("baggy rave pants");
    canUse("rave whistle");
    assertFalse(maximize("raveosity"));
    // still provides equipment
    recommendedSlotIs(EquipmentManager.HAT, "rave visor");
    recommendedSlotIs(EquipmentManager.PANTS, "baggy rave pants");
    recommendedSlotIs(EquipmentManager.WEAPON, "rave whistle");
  }

  @Test
  public void raveositySucceedsWithEnoughEquipment() {
    canUse("blue glowstick");
    canUse("glowstick on a string");
    canUse("teddybear backpack");
    canUse("rave visor");
    canUse("baggy rave pants");
    assertTrue(maximize("raveosity"));
    recommendedSlotIs(EquipmentManager.HAT, "rave visor");
    recommendedSlotIs(EquipmentManager.PANTS, "baggy rave pants");
    recommendedSlotIs(EquipmentManager.CONTAINER, "teddybear backpack");
    recommendedSlotIs(EquipmentManager.OFFHAND, "glowstick on a string");
    recommends("blue glowstick");
  }

  // club

  @Test
  public void clubModifierDoesntAffectOffhand() {
    addSkill("Double-Fisted Skull Smashing");
    canUse("flaming crutch", 2);
    canUse("white sword", 2);
    canUse("dense meat sword");
    assertTrue(EquipmentManager.canEquip("white sword"), "Can equip white sword");
    assertTrue(EquipmentManager.canEquip("flaming crutch"), "Can equip flaming crutch");
    assertTrue(maximize("mus, club"));
    // Should equip 1 flaming crutch, 1 white sword.
    assertEquals(2, modFor("Muscle"), 0.01, "Muscle as expected.");
    assertEquals(3, modFor("Hot Damage"), 0.01, "Hot damage as expected.");
  }

  // effect limits

  @Test
  public void maximizeGiveBestScoreWithEffectsAtNoncombatLimit() {
    canUse("Space Trip safety headphones");
    canUse("Krampus horn");
    // get ourselves to -25 combat
    addEffect("Shelter of Shed");
    addEffect("Smooth Movements");
    assertTrue(
        EquipmentManager.canEquip("Space Trip safety headphones"),
        "Cannot equip Space Trip safety headphones");
    assertTrue(EquipmentManager.canEquip("Krampus horn"), "Cannot equip Krampus Horn");
    assertTrue(
        maximize(
            "cold res,-combat -hat -weapon -offhand -back -shirt -pants -familiar -acc1 -acc2 -acc3"));
    assertEquals(25, modFor("Cold Resistance") - modFor("Combat Rate"), 0.01, "Base score is 25");
    assertTrue(maximize("cold res,-combat -acc2 -acc3"));
    assertEquals(
        27,
        modFor("Cold Resistance") - modFor("Combat Rate"),
        0.01,
        "Maximizing one slot should reach 27");

    recommendedSlotIs(EquipmentManager.ACCESSORY1, "Krampus horn");
  }

  @Test
  public void aboveWaterZonesDoNotCheckUnderwaterNegativeCombat() {
    Modifiers.setLocation(AdventureDatabase.getAdventure("Noob Cave"));
    canUse("Mer-kin sneakmask");
    assertTrue(maximize("-combat -tie"));
    assertEquals(0, modFor("Combat Rate"), 0.01);

    recommendedSlotIsEmpty(EquipmentManager.HAT);
  }

  @Test
  public void underwaterZonesCheckUnderwaterNegativeCombat() {
    Modifiers.setLocation(AdventureDatabase.getAdventure("The Ice Hole"));
    canUse("Mer-kin sneakmask");
    assertEquals(AdventureDatabase.getEnvironment(Modifiers.currentLocation), "underwater");
    assertTrue(maximize("-combat -tie"));

    recommendedSlotIs(EquipmentManager.HAT, "Mer-kin sneakmask");
  }

  // outfits

  @Test
  public void considersOutfitsIfHelpful() {
    canUse("eldritch hat");
    canUse("eldritch pants");
    assertTrue(maximize("item -tie"));

    assertEquals(50, modFor("Item Drop"), 0.01);
    recommendedSlotIs(EquipmentManager.HAT, "eldritch hat");
    recommendedSlotIs(EquipmentManager.PANTS, "eldritch pants");
  }

  @Test
  public void avoidsOutfitsIfOtherItemsBetter() {
    canUse("eldritch hat");
    canUse("eldritch pants");
    canUse("Team Avarice cap");
    assertTrue(maximize("item -tie"));

    assertEquals(100, modFor("Item Drop"), 0.01);
    recommendedSlotIs(EquipmentManager.HAT, "Team Avarice cap");
  }

  @Test
  public void forcingOutfitRequiresThatOutfit() {
    canUse("bounty-hunting helmet");
    canUse("bounty-hunting rifle");
    canUse("bounty-hunting pants");
    canUse("eldritch hat");
    canUse("eldritch pants");
    assertTrue(maximize("item -tie"));

    assertEquals(70, modFor("Item Drop"), 0.01);
    recommendedSlotIs(EquipmentManager.HAT, "bounty-hunting helmet");
    recommendedSlotIs(EquipmentManager.WEAPON, "bounty-hunting rifle");
    recommendedSlotIs(EquipmentManager.PANTS, "bounty-hunting pants");

    assertTrue(maximize("item, +outfit Eldritch Equipage -tie"));
    assertEquals(65, modFor("Item Drop"), 0.01);
    recommendedSlotIs(EquipmentManager.HAT, "eldritch hat");
    recommendedSlotIs(EquipmentManager.WEAPON, "bounty-hunting rifle");
    recommendedSlotIs(EquipmentManager.PANTS, "eldritch pants");

    assertTrue(maximize("item, -outfit Bounty-Hunting Rig -tie"));
    assertEquals(65, modFor("Item Drop"), 0.01);
    recommendedSlotIs(EquipmentManager.HAT, "eldritch hat");
    recommendedSlotIs(EquipmentManager.WEAPON, "bounty-hunting rifle");
    recommendedSlotIs(EquipmentManager.PANTS, "eldritch pants");
  }

  @Test
  public void considersOutfitPartsIfHelpful() {
    canUse("Brimstone Beret");
    canUse("Brimstone Boxers");
    assertTrue(maximize("ml -tie"));

    assertEquals(4, modFor("Monster Level"), 0.01);
    recommendedSlotIs(EquipmentManager.HAT, "Brimstone Beret");
    recommendedSlotIs(EquipmentManager.PANTS, "Brimstone Boxers");
  }

  // regression

  // https://kolmafia.us/threads/maximizer-reduces-score-with-combat-chance-at-soft-limit-failing-test-included.25672/
  @Test
  public void maximizeShouldNotRemoveEquipmentThatCanNoLongerBeEquipped() {
    // slippers have a Moxie requirement of 125
    equip(EquipmentManager.ACCESSORY1, "Fuzzy Slippers of Hatred");
    // get our Moxie below 125 (e.g. basic hot dogs, stat limiting effects)
    setStats(0, 0, 0);
    assertFalse(
        EquipmentManager.canEquip("Fuzzy Slippers of Hatred"),
        "Can still equip Fuzzy Slippers of Hatred");
    assertTrue(
        maximize("-combat -hat -weapon -offhand -back -shirt -pants -familiar -acc1 -acc2 -acc3"));
    assertEquals(5, -modFor("Combat Rate"), 0.01, "Base score is 5");
    assertTrue(maximize("-combat"));
    assertEquals(5, -modFor("Combat Rate"), 0.01, "Maximizing should not reduce score");
  }

  // https://kolmafia.us/threads/maximizer-reduces-score-with-combat-chance-at-soft-limit-failing-test-included.25672/
  @Test
  public void freshCharacterShouldNotRecommendEverythingWithCurrentScore() {
    KoLCharacter.setSign("Platypus");
    assertTrue(maximize("familiar weight"));
    assertEquals(5, modFor("Familiar Weight"), 0.01, "Base score is 5");
    // monorail buff should always be available, but should not improve familiar weight.
    // so are friars, but I don't know why and that might be a bug
    assertEquals(1, Maximizer.boosts.size());
    Boost ar = Maximizer.boosts.get(0);
    assertEquals("", ar.getCmd());
  }

  // Sample test for https://kolmafia.us/showthread.php?23648&p=151903#post151903.
  @Test
  public void noTieCanLeaveSlotsEmpty() {
    canUse("helmet turtle");
    assertTrue(maximize("mys -tie"));
    assertEquals(0, modFor("Buffed Muscle"), 0.01);
  }

  // Tests for https://kolmafia.us/threads/26413
  @Test
  public void keepBjornInhabitantEvenWhenUseless() {
    canUse("Buddy Bjorn");
    KoLCharacter.setBjorned(new FamiliarData(FamiliarPool.HAPPY_MEDIUM));

    assertTrue(maximize("+25 bonus buddy bjorn -tie"));

    // Actually equipped the buddy bjorn with its current inhabitant.
    assertEquals(25, modFor("Meat Drop"), 0.01);
  }

  // Tests for https://kolmafia.us/threads/26413
  @Test
  public void actuallyEquipsBonusBjorn() {
    canUse("Buddy Bjorn");
    KoLCharacter.setBjorned(new FamiliarData(FamiliarPool.HAPPY_MEDIUM));
    // +10 to all attributes. Should not be equipped.
    KoLCharacter.addFamiliar(new FamiliarData(FamiliarPool.DICE));

    // +7 Muscle
    equip(EquipmentManager.CONTAINER, "barskin cloak");

    assertTrue(maximize("mus -buddy-bjorn +25 bonus buddy bjorn -tie"));
    // Unequipped the barskin cloak
    assertEquals(0, modFor("Buffed Muscle"), 0.01);
    // Actually equipped the buddy bjorn
    assertEquals(25, modFor("Meat Drop"), 0.01);
  }

  @Test
  public void keepsBonusBjornUnchanged() {
    canUse("Buddy Bjorn");
    canUse("Crown of Thrones");
    KoLCharacter.setBjorned(new FamiliarData(FamiliarPool.HAPPY_MEDIUM));
    // +10 to all attributes. Worse than current hat.
    KoLCharacter.addFamiliar(new FamiliarData(FamiliarPool.DICE));

    // +7 Muscle
    equip(EquipmentManager.CONTAINER, "barskin cloak");
    // +25 Muscle
    equip(EquipmentManager.HAT, "wreath of laurels");

    assertTrue(maximize("mus -buddy-bjorn +25 bonus buddy bjorn -tie"));
    // Unequipped the barskin cloak, still have wreath of laurels equipped.
    assertEquals(25, modFor("Buffed Muscle"), 0.01);
    // Actually equipped the buddy bjorn
    assertEquals(25, modFor("Meat Drop"), 0.01);
  }

  // https://kolmafia.us/threads/27073/
  @Test
  public void noTiePrefersCurrentGear() {
    // Drops items, but otherwise irrelevant.
    canUse("Camp Scout backpack");
    // +1 mys, +2 mox; 7 mox required; Maximizer needs to recommend changes in order to create a
    // speculation.
    canUse("basic meat fez");
    // +7 mus; 75 mys required
    equip(EquipmentManager.CONTAINER, "barskin cloak");
    assertTrue(maximize("mys -tie"));
    recommendedSlotIs(EquipmentManager.HAT, "basic meat fez");
    // No back change recommended.
    assertFalse(getSlot(EquipmentManager.CONTAINER).isPresent());
    assertEquals(7, modFor("Buffed Muscle"), 0.01);
  }

  // helper methods

  private boolean maximize(String maximizerString) {
    return Maximizer.maximize(maximizerString, 0, 0, true);
  }

  private double modFor(String modifier) {
    return Modifiers.getNumericModifier("Generated", "_spec", modifier);
  }

  private Optional<AdventureResult> getSlot(int slot) {
    var boost =
        Maximizer.boosts.stream()
            .filter(Boost::isEquipment)
            .filter(b -> b.getSlot() == slot)
            .findAny();
    return boost.map(Boost::getItem);
  }

  private void recommendedSlotIs(int slot, String item) {
    Optional<AdventureResult> equipment = getSlot(slot);
    assertTrue(equipment.isPresent(), "Expected " + item + " to be recommended, but it was not");
    assertEquals(equipment.get(), AdventureResult.parseResult(item));
  }

  private void recommendedSlotIsEmpty(int slot) {
    Optional<AdventureResult> equipment = getSlot(slot);
    assertTrue(equipment.isEmpty(), "Expected empty slot " + slot + ", but it was not");
  }

  private void recommends(String item) {
    Optional<Boost> found =
        Maximizer.boosts.stream()
            .filter(Boost::isEquipment)
            .filter(b -> item.equals(b.getItem().getName()))
            .findAny();
    assertTrue(found.isPresent(), "Expected " + item + " to be recommended, but it was not");
  }
}
