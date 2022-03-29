package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.*;
import static internal.helpers.Player.canUse;
import static internal.helpers.Player.equip;
import static internal.helpers.Player.setStats;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MaximizerRegressionTest {
  @BeforeEach
  public void init() {
    KoLCharacter.reset(true);
  }

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

  @Test
  public void equipEmptyBjornNoSlot() {
    equip(EquipmentManager.CONTAINER, "Buddy Bjorn");
    KoLCharacter.addFamiliar(new FamiliarData(FamiliarPool.MOSQUITO));
    KoLCharacter.setBjorned(FamiliarData.NO_FAMILIAR);

    // Here, buddy-bjorn refers to the slot, while Buddy Bjorn refers to the item associated with
    // that slot.
    // We've earlier forced that slot to be empty, and here we're asking mafia not to fill it when
    // maximizing, but to still equip the bjorn.
    assertTrue(maximize("item, -buddy-bjorn, +equip Buddy Bjorn"));
  }

  @Test
  public void equipEmptyCrownNoSlot() {
    equip(EquipmentManager.HAT, "Crown of Thrones");
    KoLCharacter.addFamiliar(new FamiliarData(FamiliarPool.MOSQUITO));
    KoLCharacter.setEnthroned(FamiliarData.NO_FAMILIAR);

    assertTrue(maximize("item, -crown-of-thrones, +equip Crown of Thrones"));
  }

  @Test
  public void keepBjornEquippedWithBonus() {
    // Provide a helpful alternative to the bjorn.
    canUse("vampyric cloake");
    equip(EquipmentManager.CONTAINER, "Buddy Bjorn");

    assertTrue(maximize("adventures, -buddy-bjorn, +25 bonus Buddy Bjorn"));
    recommendedSlotIsUnchanged(EquipmentManager.CONTAINER);
  }

  @Test
  public void keepBjornEquippedAndUnchangedWithCrownAndBonus() {
    canUse("Crown of Thrones");
    canUse("time helmet");

    KoLCharacter.addFamiliar(new FamiliarData(FamiliarPool.DICE));
    equip(EquipmentManager.CONTAINER, "Buddy Bjorn");

    assertTrue(maximize("adventures, -buddy-bjorn, +25 bonus Buddy Bjorn"));

    recommendedSlotIs(EquipmentManager.HAT, "time helmet");
    recommendedSlotIsUnchanged(EquipmentManager.CONTAINER);
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
}
