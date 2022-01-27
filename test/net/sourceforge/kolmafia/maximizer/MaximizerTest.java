package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.*;
import static internal.helpers.Player.*;
import static org.junit.jupiter.api.Assertions.*;

import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.Test;

public class MaximizerTest {
  // basic

  @Test
  public void changesGear() {
    final var c1 = canUse("helmet turtle");
    try (c1) {
      assertTrue(maximize("mus"));
      assertEquals(1, modFor("Buffed Muscle"), 0.01);
    }
  }

  @Test
  public void equipsItemsOnlyIfHasStats() {
    final var c1 = canUse("helmet turtle");
    final var c2 = addItem("wreath of laurels");
    try (c1;
        c2) {
      assertTrue(maximize("mus"));
      assertEquals(1, modFor("Buffed Muscle"), 0.01);
      recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
    }
  }

  @Test
  public void nothingBetterThanSomething() {
    final var c1 = canUse("helmet turtle");
    try (c1) {
      assertTrue(maximize("-mus"));
      assertEquals(0, modFor("Buffed Muscle"), 0.01);
    }
  }

  // max

  @Test
  public void maxKeywordStopsCountingBeyondTarget() {
    final var c1 = canUse("hardened slime hat");
    final var c2 = canUse("bounty-hunting helmet");
    final var c3 = addSkill("Refusal to Freeze");
    try (c1;
        c2;
        c3) {
      assertTrue(maximize("cold res 3 max, 0.1 item drop"));

      assertEquals(3, modFor("Cold Resistance"), 0.01);
      assertEquals(20, modFor("Item Drop"), 0.01);

      recommendedSlotIs(EquipmentManager.HAT, "bounty-hunting helmet");
    }
  }

  @Test
  public void startingMaxKeywordTerminatesEarlyIfConditionMet() {
    final var c1 = canUse("hardened slime hat");
    final var c2 = canUse("bounty-hunting helmet");
    final var c3 = addSkill("Refusal to Freeze");
    try (c1;
        c2;
        c3) {
      maximize("3 max, cold res");

      assertTrue(
          Maximizer.boosts.stream()
              .anyMatch(
                  b ->
                      b.toString()
                          .contains("(maximum achieved, no further combinations checked)")));
    }
  }

  // min

  @Test
  public void minKeywordFailsMaximizationIfNotHit() {
    final var c1 = canUse("helmet turtle");
    try (c1) {
      assertFalse(maximize("mus 2 min"));
      // still provides equipment
      recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
    }
  }

  @Test
  public void minKeywordPassesMaximizationIfHit() {
    final var c1 = canUse("wreath of laurels");
    try (c1) {
      assertTrue(maximize("mus 2 min"));
    }
  }

  @Test
  public void startingMinKeywordFailsMaximizationIfNotHit() {
    final var c1 = canUse("helmet turtle");
    try (c1) {
      assertFalse(maximize("2 min, mus"));
      // still provides equipment
      recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
    }
  }

  @Test
  public void startingMinKeywordPassesMaximizationIfHit() {
    final var c1 = canUse("wreath of laurels");
    try (c1) {
      assertTrue(maximize("2 min, mus"));
    }
  }

  // clownosity

  @Test
  public void clownosityTriesClownEquipment() {
    final var c1 = canUse("clown wig");
    try (c1) {
      assertFalse(maximize("clownosity -tie"));
      // still provides equipment
      recommendedSlotIs(EquipmentManager.HAT, "clown wig");
      assertEquals(50, modFor("Clowniness"), 0.01);
    }
  }

  @Test
  public void clownositySucceedsWithEnoughEquipment() {
    final var c1 = canUse("clown wig");
    final var c2 = canUse("polka-dot bow tie");
    try (c1;
        c2) {
      assertTrue(maximize("clownosity -tie"));
      recommendedSlotIs(EquipmentManager.HAT, "clown wig");
      recommendedSlotIs(EquipmentManager.ACCESSORY1, "polka-dot bow tie");
      assertEquals(125, modFor("Clowniness"), 0.01);
    }
  }

  // raveosity

  @Test
  public void raveosityTriesRaveEquipment() {
    final var c1 = canUse("rave visor");
    final var c2 = canUse("baggy rave pants");
    final var c3 = canUse("rave whistle");
    try (c1;
        c2;
        c3) {
      assertFalse(maximize("raveosity -tie"));
      // still provides equipment
      recommendedSlotIs(EquipmentManager.HAT, "rave visor");
      recommendedSlotIs(EquipmentManager.PANTS, "baggy rave pants");
      recommendedSlotIs(EquipmentManager.WEAPON, "rave whistle");
      assertEquals(5, modFor("Raveosity"), 0.01);
    }
  }

  @Test
  public void raveositySucceedsWithEnoughEquipment() {
    final var c1 = canUse("blue glowstick");
    final var c2 = canUse("glowstick on a string");
    final var c3 = canUse("teddybear backpack");
    final var c4 = canUse("rave visor");
    final var c5 = canUse("baggy rave pants");
    try (c1;
        c2;
        c3;
        c4;
        c5) {
      assertTrue(maximize("raveosity -tie"));
      recommendedSlotIs(EquipmentManager.HAT, "rave visor");
      recommendedSlotIs(EquipmentManager.PANTS, "baggy rave pants");
      recommendedSlotIs(EquipmentManager.CONTAINER, "teddybear backpack");
      recommendedSlotIs(EquipmentManager.OFFHAND, "glowstick on a string");
      assertEquals(7, modFor("Raveosity"), 0.01);
    }
  }

  // surgeonosity

  @Test
  public void surgeonosityTriesSurgeonEquipment() {
    final var c1 = canUse("head mirror");
    final var c2 = canUse("bloodied surgical dungarees");
    final var c3 = canUse("surgical apron");
    final var c4 = canUse("surgical mask");
    final var c5 = canUse("half-size scalpel");
    final var c6 = addSkill("Torso Awareness");
    try (c1;
        c2;
        c3;
        c4;
        c5;
        c6) {
      assertTrue(maximize("surgeonosity -tie"));
      recommendedSlotIs(EquipmentManager.PANTS, "bloodied surgical dungarees");
      recommends("head mirror");
      recommends("surgical mask");
      recommends("half-size scalpel");
      recommendedSlotIs(EquipmentManager.SHIRT, "surgical apron");
      assertEquals(5, modFor("Surgeonosity"), 0.01);
    }
  }

  // beecore

  @Test
  public void itemsCanHaveAtMostTwoBeesByDefault() {
    final var c1 = inPath(Path.BEES_HATE_YOU);
    final var c2 = canUse("bubblewrap bottlecap turtleban");
    try (c1;
        c2) {
      maximize("mys");
      recommendedSlotIsEmpty(EquipmentManager.HAT);
    }
  }

  @Test
  public void itemsCanHaveAtMostBeeosityBees() {
    final var c1 = inPath(Path.BEES_HATE_YOU);
    final var c2 = canUse("bubblewrap bottlecap turtleban");
    try (c1;
        c2) {
      maximize("mys, 5beeosity");
      recommendedSlotIs(EquipmentManager.HAT, "bubblewrap bottlecap turtleban");
    }
  }

  @Test
  public void beeosityDoesntApplyOutsideBeePath() {
    final var c1 = canUse("bubblewrap bottlecap turtleban");
    try (c1) {
      maximize("mys");
      recommendedSlotIs(EquipmentManager.HAT, "bubblewrap bottlecap turtleban");
    }
  }

  // proof for the next test
  @Test
  public void canCrownFamiliarsWithBeesOutsideBeecore() {
    final var c1 = canUse("Crown of Thrones");
    final var c2 = hasFamiliar(FamiliarPool.LOBSTER); // 15% spell damage
    final var c3 = hasFamiliar(FamiliarPool.GALLOPING_GRILL); // 10% spell damage

    try (c1;
        c2;
        c3) {
      maximize("spell dmg");

      // used the lobster in the throne.
      assertTrue(someBoostIs(x -> x.getCmd().startsWith("enthrone Rock Lobster")));
    }
  }

  @Test
  public void cannotCrownFamiliarsWithBeesInBeecore() {
    final var c1 = inPath(Path.BEES_HATE_YOU);
    final var c2 = canUse("Crown of Thrones");
    final var c3 = hasFamiliar(FamiliarPool.LOBSTER); // 15% spell damage
    final var c4 = hasFamiliar(FamiliarPool.GALLOPING_GRILL); // 10% spell damage

    try (c1;
        c2;
        c3;
        c4) {
      maximize("spell dmg");

      // used the grill in the throne.
      assertTrue(someBoostIs(x -> x.getCmd().startsWith("enthrone Galloping Grill")));
    }
  }

  // proof for the next test
  @Test
  public void canUsePotionsWithBeesOutsideBeecore() {
    final var c1 = addItem("baggie of powdered sugar");

    try (c1) {
      maximize("meat drop");

      assertTrue(someBoostIs(x -> x.getCmd().startsWith("use 1 baggie of powdered sugar")));
    }
  }

  @Test
  public void cannotUsePotionsWithBeesInBeecore() {
    final var c1 = inPath(Path.BEES_HATE_YOU);
    final var c2 = addItem("baggie of powdered sugar");

    try (c1;
        c2) {
      maximize("meat drop");

      assertFalse(someBoostIs(x -> x.getCmd().startsWith("use 1 baggie of powdered sugar")));
    }
  }

  // plumber

  @Test
  public void plumberCommandsErrorOutsidePlumber() {
    final var c1 = inPath(Path.AVATAR_OF_BORIS);

    try (c1) {
      assertFalse(maximize("plumber"));
      assertFalse(maximize("cold plumber"));
    }
  }

  @Test
  public void plumberCommandForcesSomePlumberItem() {
    final var c1 = inPath(Path.PATH_OF_THE_PLUMBER);
    final var c2 = canUse("work boots");
    final var c3 = canUse("shiny ring", 3);

    try (c1;
        c2;
        c3) {
      assertTrue(maximize("plumber, mox"));
      recommends("work boots");
    }
  }

  @Test
  public void coldPlumberCommandForcesFlowerAndFrostyButton() {
    final var c1 = inPath(Path.PATH_OF_THE_PLUMBER);
    final var c2 = canUse("work boots");
    final var c3 = canUse("bonfire flower");
    final var c4 = canUse("frosty button");
    final var c5 = canUse("shiny ring", 3);

    try (c1;
        c2;
        c3;
        c4;
        c5) {
      assertTrue(maximize("cold plumber, mox"));
      recommends("bonfire flower");
      recommends("frosty button");
    }
  }

  // club

  @Test
  public void clubModifierDoesntAffectOffhand() {
    final var c1 = addSkill("Double-Fisted Skull Smashing");
    final var c2 = canUse("flaming crutch", 2);
    final var c3 = canUse("white sword", 2);
    final var c4 = canUse("dense meat sword");
    try (c1;
        c2;
        c3;
        c4) {
      assertTrue(EquipmentManager.canEquip("white sword"), "Can equip white sword");
      assertTrue(EquipmentManager.canEquip("flaming crutch"), "Can equip flaming crutch");
      assertTrue(maximize("mus, club"));
      // Should equip 1 flaming crutch, 1 white sword.
      recommendedSlotIs(EquipmentManager.WEAPON, "flaming crutch");
      recommendedSlotIs(EquipmentManager.OFFHAND, "white sword");
    }
  }

  // sword

  @Test
  public void swordModifierFavorsSword() {
    final var c1 = canUse("sweet ninja sword");
    final var c2 = canUse("spiked femur");
    try (c1;
        c2) {
      assertTrue(maximize("spooky dmg, sword"));
      recommendedSlotIs(EquipmentManager.WEAPON, "sweet ninja sword");
    }
  }

  // effect limits

  @Test
  public void maximizeGiveBestScoreWithEffectsAtNoncombatLimit() {
    final var c1 = canUse("Space Trip safety headphones");
    final var c2 = canUse("Krampus horn");
    // get ourselves to -25 combat
    final var c3 = addEffect("Shelter of Shed");
    final var c4 = addEffect("Smooth Movements");
    try (c1;
        c2;
        c3;
        c4) {
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
  }

  @Test
  public void aboveWaterZonesDoNotCheckUnderwaterNegativeCombat() {
    final var c1 = inLocation("Noob Cave");
    final var c2 = canUse("Mer-kin sneakmask");
    try (c1;
        c2) {
      assertTrue(maximize("-combat -tie"));
      assertEquals(0, modFor("Combat Rate"), 0.01);

      recommendedSlotIsEmpty(EquipmentManager.HAT);
    }
  }

  @Test
  public void underwaterZonesCheckUnderwaterNegativeCombat() {
    final var c1 = inLocation("The Ice Hole");
    final var c2 = canUse("Mer-kin sneakmask");
    try (c1;
        c2) {
      assertEquals(AdventureDatabase.getEnvironment(Modifiers.currentLocation), "underwater");
      assertTrue(maximize("-combat -tie"));

      recommendedSlotIs(EquipmentManager.HAT, "Mer-kin sneakmask");
    }
  }

  // outfits

  @Test
  public void considersOutfitsIfHelpful() {
    final var c1 = canUse("eldritch hat");
    final var c2 = canUse("eldritch pants");
    try (c1;
        c2) {
      assertTrue(maximize("item -tie"));

      assertEquals(50, modFor("Item Drop"), 0.01);
      recommendedSlotIs(EquipmentManager.HAT, "eldritch hat");
      recommendedSlotIs(EquipmentManager.PANTS, "eldritch pants");
    }
  }

  @Test
  public void avoidsOutfitsIfOtherItemsBetter() {
    final var c1 = canUse("eldritch hat");
    final var c2 = canUse("eldritch pants");
    final var c3 = canUse("Team Avarice cap");
    try (c1;
        c2;
        c3) {
      assertTrue(maximize("item -tie"));

      assertEquals(100, modFor("Item Drop"), 0.01);
      recommendedSlotIs(EquipmentManager.HAT, "Team Avarice cap");
    }
  }

  @Test
  public void forcingOutfitRequiresThatOutfit() {
    final var c1 = canUse("bounty-hunting helmet");
    final var c2 = canUse("bounty-hunting rifle");
    final var c3 = canUse("bounty-hunting pants");
    final var c4 = canUse("eldritch hat");
    final var c5 = canUse("eldritch pants");
    try (c1;
        c2;
        c3;
        c4;
        c5) {
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
  }

  @Test
  public void considersOutfitPartsIfHelpful() {
    final var c1 = canUse("Brimstone Beret");
    final var c2 = canUse("Brimstone Boxers");
    try (c1;
        c2) {
      assertTrue(maximize("ml -tie"));

      assertEquals(4, modFor("Monster Level"), 0.01);
      recommendedSlotIs(EquipmentManager.HAT, "Brimstone Beret");
      recommendedSlotIs(EquipmentManager.PANTS, "Brimstone Boxers");
    }
  }
}
