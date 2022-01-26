package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.*;
import static internal.helpers.Player.*;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MaximizerTest {
  private List<Cleanups> cleanups;

  @BeforeEach
  public void init() {
    cleanups = new ArrayList<>();
  }

  @AfterEach
  public void tearDown() {
    cleanups.forEach(Cleanups::run);
  }

  // basic

  @Test
  public void changesGear() {
    cleanups.add(canUse("helmet turtle"));
    assertTrue(maximize("mus"));
    assertEquals(1, modFor("Buffed Muscle"), 0.01);
  }

  @Test
  public void equipsItemsOnlyIfHasStats() {
    cleanups.add(canUse("helmet turtle"));
    cleanups.add(addItem("wreath of laurels"));
    assertTrue(maximize("mus"));
    assertEquals(1, modFor("Buffed Muscle"), 0.01);
    recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
  }

  @Test
  public void nothingBetterThanSomething() {
    cleanups.add(canUse("helmet turtle"));
    assertTrue(maximize("-mus"));
    assertEquals(0, modFor("Buffed Muscle"), 0.01);
  }

  // max

  @Test
  public void maxKeywordStopsCountingBeyondTarget() {
    cleanups.add(canUse("hardened slime hat"));
    cleanups.add(canUse("bounty-hunting helmet"));
    cleanups.add(addSkill("Refusal to Freeze"));
    assertTrue(maximize("cold res 3 max, 0.1 item drop"));

    assertEquals(3, modFor("Cold Resistance"), 0.01);
    assertEquals(20, modFor("Item Drop"), 0.01);

    recommendedSlotIs(EquipmentManager.HAT, "bounty-hunting helmet");
  }

  @Test
  public void startingMaxKeywordTerminatesEarlyIfConditionMet() {
    cleanups.add(canUse("hardened slime hat"));
    cleanups.add(canUse("bounty-hunting helmet"));
    cleanups.add(addSkill("Refusal to Freeze"));
    maximize("3 max, cold res");

    assertTrue(
        Maximizer.boosts.stream()
            .anyMatch(
                b -> b.toString().contains("(maximum achieved, no further combinations checked)")));
  }

  // min

  @Test
  public void minKeywordFailsMaximizationIfNotHit() {
    cleanups.add(canUse("helmet turtle"));
    assertFalse(maximize("mus 2 min"));
    // still provides equipment
    recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
  }

  @Test
  public void minKeywordPassesMaximizationIfHit() {
    cleanups.add(canUse("wreath of laurels"));
    assertTrue(maximize("mus 2 min"));
  }

  @Test
  public void startingMinKeywordFailsMaximizationIfNotHit() {
    cleanups.add(canUse("helmet turtle"));
    assertFalse(maximize("2 min, mus"));
    // still provides equipment
    recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
  }

  @Test
  public void startingMinKeywordPassesMaximizationIfHit() {
    cleanups.add(canUse("wreath of laurels"));
    assertTrue(maximize("2 min, mus"));
  }

  // clownosity

  @Test
  public void clownosityTriesClownEquipment() {
    cleanups.add(canUse("clown wig"));
    assertFalse(maximize("clownosity -tie"));
    // still provides equipment
    recommendedSlotIs(EquipmentManager.HAT, "clown wig");
    assertEquals(50, modFor("Clowniness"), 0.01);
  }

  @Test
  public void clownositySucceedsWithEnoughEquipment() {
    cleanups.add(canUse("clown wig"));
    cleanups.add(canUse("polka-dot bow tie"));
    assertTrue(maximize("clownosity -tie"));
    recommendedSlotIs(EquipmentManager.HAT, "clown wig");
    recommendedSlotIs(EquipmentManager.ACCESSORY1, "polka-dot bow tie");
    assertEquals(125, modFor("Clowniness"), 0.01);
  }

  // raveosity

  @Test
  public void raveosityTriesRaveEquipment() {
    cleanups.add(canUse("rave visor"));
    cleanups.add(canUse("baggy rave pants"));
    cleanups.add(canUse("rave whistle"));
    assertFalse(maximize("raveosity -tie"));
    // still provides equipment
    recommendedSlotIs(EquipmentManager.HAT, "rave visor");
    recommendedSlotIs(EquipmentManager.PANTS, "baggy rave pants");
    recommendedSlotIs(EquipmentManager.WEAPON, "rave whistle");
    assertEquals(5, modFor("Raveosity"), 0.01);
  }

  @Test
  public void raveositySucceedsWithEnoughEquipment() {
    cleanups.add(canUse("blue glowstick"));
    cleanups.add(canUse("glowstick on a string"));
    cleanups.add(canUse("teddybear backpack"));
    cleanups.add(canUse("rave visor"));
    cleanups.add(canUse("baggy rave pants"));
    assertTrue(maximize("raveosity -tie"));
    recommendedSlotIs(EquipmentManager.HAT, "rave visor");
    recommendedSlotIs(EquipmentManager.PANTS, "baggy rave pants");
    recommendedSlotIs(EquipmentManager.CONTAINER, "teddybear backpack");
    recommendedSlotIs(EquipmentManager.OFFHAND, "glowstick on a string");
    assertEquals(7, modFor("Raveosity"), 0.01);
  }

  // surgeonosity

  @Test
  public void surgeonosityTriesSurgeonEquipment() {
    cleanups.add(canUse("head mirror"));
    cleanups.add(canUse("bloodied surgical dungarees"));
    cleanups.add(canUse("surgical apron"));
    cleanups.add(canUse("surgical mask"));
    cleanups.add(canUse("half-size scalpel"));
    cleanups.add(addSkill("Torso Awareness"));
    assertTrue(maximize("surgeonosity -tie"));
    recommendedSlotIs(EquipmentManager.PANTS, "bloodied surgical dungarees");
    recommends("head mirror");
    recommends("surgical mask");
    recommends("half-size scalpel");
    recommendedSlotIs(EquipmentManager.SHIRT, "surgical apron");
    assertEquals(5, modFor("Surgeonosity"), 0.01);
  }

  // beecore

  @Test
  public void itemsCanHaveAtMostTwoBeesByDefault() {
    cleanups.add(inPath(Path.BEES_HATE_YOU));
    cleanups.add(canUse("bubblewrap bottlecap turtleban"));
    maximize("mys");
    recommendedSlotIsEmpty(EquipmentManager.HAT);
  }

  @Test
  public void itemsCanHaveAtMostBeeosityBees() {
    cleanups.add(inPath(Path.BEES_HATE_YOU));
    cleanups.add(canUse("bubblewrap bottlecap turtleban"));
    maximize("mys, 5beeosity");
    recommendedSlotIs(EquipmentManager.HAT, "bubblewrap bottlecap turtleban");
  }

  @Test
  public void beeosityDoesntApplyOutsideBeePath() {
    cleanups.add(canUse("bubblewrap bottlecap turtleban"));
    maximize("mys");
    recommendedSlotIs(EquipmentManager.HAT, "bubblewrap bottlecap turtleban");
  }

  // proof for the next test
  @Test
  public void canCrownFamiliarsWithBeesOutsideBeecore() {
    cleanups.add(canUse("Crown of Thrones"));
    cleanups.add(hasFamiliar(FamiliarPool.LOBSTER)); // 15% spell damage
    cleanups.add(hasFamiliar(FamiliarPool.GALLOPING_GRILL)); // 10% spell damage

    maximize("spell dmg");

    // used the lobster in the throne.
    assertTrue(someBoostIs(x -> x.getCmd().startsWith("enthrone Rock Lobster")));
  }

  @Test
  public void cannotCrownFamiliarsWithBeesInBeecore() {
    cleanups.add(inPath(Path.BEES_HATE_YOU));
    cleanups.add(canUse("Crown of Thrones"));
    cleanups.add(hasFamiliar(FamiliarPool.LOBSTER)); // 15% spell damage
    cleanups.add(hasFamiliar(FamiliarPool.GALLOPING_GRILL)); // 10% spell damage

    maximize("spell dmg");

    // used the grill in the throne.
    assertTrue(someBoostIs(x -> x.getCmd().startsWith("enthrone Galloping Grill")));
  }

  // proof for the next test
  @Test
  public void canUsePotionsWithBeesOutsideBeecore() {
    cleanups.add(addItem("baggie of powdered sugar"));

    maximize("meat drop");

    assertTrue(someBoostIs(x -> x.getCmd().startsWith("use 1 baggie of powdered sugar")));
  }

  @Test
  public void cannotUsePotionsWithBeesInBeecore() {
    cleanups.add(inPath(Path.BEES_HATE_YOU));
    cleanups.add(addItem("baggie of powdered sugar"));

    maximize("meat drop");

    assertFalse(someBoostIs(x -> x.getCmd().startsWith("use 1 baggie of powdered sugar")));
  }

  // plumber

  @Test
  public void plumberCommandsErrorOutsidePlumber() {
    cleanups.add(inPath(Path.AVATAR_OF_BORIS));

    assertFalse(maximize("plumber"));
    assertFalse(maximize("cold plumber"));
  }

  @Test
  public void plumberCommandForcesSomePlumberItem() {
    cleanups.add(inPath(Path.PATH_OF_THE_PLUMBER));
    cleanups.add(canUse("work boots"));
    cleanups.add(canUse("shiny ring", 3));

    assertTrue(maximize("plumber, mox"));
    recommends("work boots");
  }

  @Test
  public void coldPlumberCommandForcesFlowerAndFrostyButton() {
    cleanups.add(inPath(Path.PATH_OF_THE_PLUMBER));
    cleanups.add(canUse("work boots"));
    cleanups.add(canUse("bonfire flower"));
    cleanups.add(canUse("frosty button"));
    cleanups.add(canUse("shiny ring", 3));

    assertTrue(maximize("cold plumber, mox"));
    recommends("bonfire flower");
    recommends("frosty button");
  }

  // club

  @Test
  public void clubModifierDoesntAffectOffhand() {
    cleanups.add(addSkill("Double-Fisted Skull Smashing"));
    cleanups.add(canUse("flaming crutch", 2));
    cleanups.add(canUse("white sword", 2));
    cleanups.add(canUse("dense meat sword"));
    assertTrue(EquipmentManager.canEquip("white sword"), "Can equip white sword");
    assertTrue(EquipmentManager.canEquip("flaming crutch"), "Can equip flaming crutch");
    assertTrue(maximize("mus, club"));
    // Should equip 1 flaming crutch, 1 white sword.
    recommendedSlotIs(EquipmentManager.WEAPON, "flaming crutch");
    recommendedSlotIs(EquipmentManager.OFFHAND, "white sword");
  }

  // sword

  @Test
  public void swordModifierFavorsSword() {
    cleanups.add(canUse("sweet ninja sword"));
    cleanups.add(canUse("spiked femur"));
    assertTrue(maximize("spooky dmg, sword"));
    recommendedSlotIs(EquipmentManager.WEAPON, "sweet ninja sword");
  }

  // effect limits

  @Test
  public void maximizeGiveBestScoreWithEffectsAtNoncombatLimit() {
    cleanups.add(canUse("Space Trip safety headphones"));
    cleanups.add(canUse("Krampus horn"));
    // get ourselves to -25 combat
    cleanups.add(addEffect("Shelter of Shed"));
    cleanups.add(addEffect("Smooth Movements"));
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
    cleanups.add(inLocation("Noob Cave"));
    cleanups.add(canUse("Mer-kin sneakmask"));
    assertTrue(maximize("-combat -tie"));
    assertEquals(0, modFor("Combat Rate"), 0.01);

    recommendedSlotIsEmpty(EquipmentManager.HAT);
  }

  @Test
  public void underwaterZonesCheckUnderwaterNegativeCombat() {
    cleanups.add(inLocation("The Ice Hole"));
    cleanups.add(canUse("Mer-kin sneakmask"));
    assertEquals(AdventureDatabase.getEnvironment(Modifiers.currentLocation), "underwater");
    assertTrue(maximize("-combat -tie"));

    recommendedSlotIs(EquipmentManager.HAT, "Mer-kin sneakmask");
  }

  // outfits

  @Test
  public void considersOutfitsIfHelpful() {
    cleanups.add(canUse("eldritch hat"));
    cleanups.add(canUse("eldritch pants"));
    assertTrue(maximize("item -tie"));

    assertEquals(50, modFor("Item Drop"), 0.01);
    recommendedSlotIs(EquipmentManager.HAT, "eldritch hat");
    recommendedSlotIs(EquipmentManager.PANTS, "eldritch pants");
  }

  @Test
  public void avoidsOutfitsIfOtherItemsBetter() {
    cleanups.add(canUse("eldritch hat"));
    cleanups.add(canUse("eldritch pants"));
    cleanups.add(canUse("Team Avarice cap"));
    assertTrue(maximize("item -tie"));

    assertEquals(100, modFor("Item Drop"), 0.01);
    recommendedSlotIs(EquipmentManager.HAT, "Team Avarice cap");
  }

  @Test
  public void forcingOutfitRequiresThatOutfit() {
    cleanups.add(canUse("bounty-hunting helmet"));
    cleanups.add(canUse("bounty-hunting rifle"));
    cleanups.add(canUse("bounty-hunting pants"));
    cleanups.add(canUse("eldritch hat"));
    cleanups.add(canUse("eldritch pants"));
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
    cleanups.add(canUse("Brimstone Beret"));
    cleanups.add(canUse("Brimstone Boxers"));
    assertTrue(maximize("ml -tie"));

    assertEquals(4, modFor("Monster Level"), 0.01);
    recommendedSlotIs(EquipmentManager.HAT, "Brimstone Beret");
    recommendedSlotIs(EquipmentManager.PANTS, "Brimstone Boxers");
  }
}
