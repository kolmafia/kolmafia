package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.*;
import static internal.helpers.Player.*;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MaximizerTest {
  // basic

  @Test
  public void changesGear() {
    final var cleanups = new Cleanups(canUse("helmet turtle"));
    try (cleanups) {
      assertTrue(maximize("mus"));
      assertEquals(1, modFor("Buffed Muscle"), 0.01);
    }
  }

  @Test
  public void equipsItemsOnlyIfHasStats() {
    final var cleanups = new Cleanups(canUse("helmet turtle"), addItem("wreath of laurels"));
    try (cleanups) {
      assertTrue(maximize("mus"));
      assertEquals(1, modFor("Buffed Muscle"), 0.01);
      recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
    }
  }

  @Test
  public void nothingBetterThanSomething() {
    final var cleanups = new Cleanups(canUse("helmet turtle"));
    try (cleanups) {
      assertTrue(maximize("-mus"));
      assertEquals(0, modFor("Buffed Muscle"), 0.01);
    }
  }

  @Nested
  class Max {
    @Test
    public void maxKeywordStopsCountingBeyondTarget() {
      final var cleanups =
          new Cleanups(
              canUse("hardened slime hat"),
              canUse("bounty-hunting helmet"),
              addSkill("Refusal to Freeze"));
      try (cleanups) {
        assertTrue(maximize("cold res 3 max, 0.1 item drop"));

        assertEquals(3, modFor("Cold Resistance"), 0.01);
        assertEquals(20, modFor("Item Drop"), 0.01);

        recommendedSlotIs(EquipmentManager.HAT, "bounty-hunting helmet");
      }
    }

    @Test
    public void startingMaxKeywordTerminatesEarlyIfConditionMet() {
      final var cleanups =
          new Cleanups(
              canUse("hardened slime hat"),
              canUse("bounty-hunting helmet"),
              addSkill("Refusal to Freeze"));
      try (cleanups) {
        maximize("3 max, cold res");

        assertTrue(
            Maximizer.boosts.stream()
                .anyMatch(
                    b ->
                        b.toString()
                            .contains("(maximum achieved, no further combinations checked)")));
      }
    }
  }

  @Nested
  class Min {
    @Test
    public void minKeywordFailsMaximizationIfNotHit() {
      final var cleanups = new Cleanups(canUse("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("mus 2 min"));
        // still provides equipment
        recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
      }
    }

    @Test
    public void minKeywordPassesMaximizationIfHit() {
      final var cleanups = new Cleanups(canUse("wreath of laurels"));
      try (cleanups) {
        assertTrue(maximize("mus 2 min"));
      }
    }

    @Test
    public void startingMinKeywordFailsMaximizationIfNotHit() {
      final var cleanups = new Cleanups(canUse("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("2 min, mus"));
        // still provides equipment
        recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
      }
    }

    @Test
    public void startingMinKeywordPassesMaximizationIfHit() {
      final var cleanups = new Cleanups(canUse("wreath of laurels"));
      try (cleanups) {
        assertTrue(maximize("2 min, mus"));
      }
    }
  }

  @Nested
  class Clownosity {
    @Test
    public void clownosityTriesClownEquipment() {
      final var cleanups = new Cleanups(canUse("clown wig"));
      try (cleanups) {
        assertFalse(maximize("clownosity -tie"));
        // still provides equipment
        recommendedSlotIs(EquipmentManager.HAT, "clown wig");
        assertEquals(50, modFor("Clowniness"), 0.01);
      }
    }

    @Test
    public void clownositySucceedsWithEnoughEquipment() {
      final var cleanups = new Cleanups(canUse("clown wig"), canUse("polka-dot bow tie"));
      try (cleanups) {
        assertTrue(maximize("clownosity -tie"));
        recommendedSlotIs(EquipmentManager.HAT, "clown wig");
        recommendedSlotIs(EquipmentManager.ACCESSORY1, "polka-dot bow tie");
        assertEquals(125, modFor("Clowniness"), 0.01);
      }
    }
  }

  @Nested
  class Raveosity {
    @Test
    public void raveosityTriesRaveEquipment() {
      final var cleanups =
          new Cleanups(canUse("rave visor"), canUse("baggy rave pants"), canUse("rave whistle"));
      try (cleanups) {
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
      final var cleanups =
          new Cleanups(
              canUse("blue glowstick"),
              canUse("glowstick on a string"),
              canUse("teddybear backpack"),
              canUse("rave visor"),
              canUse("baggy rave pants"));
      try (cleanups) {
        assertTrue(maximize("raveosity -tie"));
        recommendedSlotIs(EquipmentManager.HAT, "rave visor");
        recommendedSlotIs(EquipmentManager.PANTS, "baggy rave pants");
        recommendedSlotIs(EquipmentManager.CONTAINER, "teddybear backpack");
        recommendedSlotIs(EquipmentManager.OFFHAND, "glowstick on a string");
        assertEquals(7, modFor("Raveosity"), 0.01);
      }
    }
  }

  @Nested
  class Surgeonosity {
    @Test
    public void surgeonosityTriesSurgeonEquipment() {
      final var cleanups =
          new Cleanups(
              canUse("head mirror"),
              canUse("bloodied surgical dungarees"),
              canUse("surgical apron"),
              canUse("surgical mask"),
              canUse("half-size scalpel"),
              addSkill("Torso Awareness"));
      try (cleanups) {
        assertTrue(maximize("surgeonosity -tie"));
        recommendedSlotIs(EquipmentManager.PANTS, "bloodied surgical dungarees");
        recommends("head mirror");
        recommends("surgical mask");
        recommends("half-size scalpel");
        recommendedSlotIs(EquipmentManager.SHIRT, "surgical apron");
        assertEquals(5, modFor("Surgeonosity"), 0.01);
      }
    }
  }

  @Nested
  class Beecore {

    @Test
    public void itemsCanHaveAtMostTwoBeesByDefault() {
      final var cleanups =
          new Cleanups(inPath(Path.BEES_HATE_YOU), canUse("bubblewrap bottlecap turtleban"));
      try (cleanups) {
        maximize("mys");
        recommendedSlotIsEmpty(EquipmentManager.HAT);
      }
    }

    @Test
    public void itemsCanHaveAtMostBeeosityBees() {
      final var cleanups =
          new Cleanups(inPath(Path.BEES_HATE_YOU), canUse("bubblewrap bottlecap turtleban"));
      try (cleanups) {
        maximize("mys, 5beeosity");
        recommendedSlotIs(EquipmentManager.HAT, "bubblewrap bottlecap turtleban");
      }
    }

    @Test
    public void beeosityDoesntApplyOutsideBeePath() {
      final var cleanups = new Cleanups(canUse("bubblewrap bottlecap turtleban"));
      try (cleanups) {
        maximize("mys");
        recommendedSlotIs(EquipmentManager.HAT, "bubblewrap bottlecap turtleban");
      }
    }

    @Nested
    class Crown {
      @Test
      public void canCrownFamiliarsWithBeesOutsideBeecore() {
        final var cleanups =
            new Cleanups(
                canUse("Crown of Thrones"),
                hasFamiliar(FamiliarPool.LOBSTER), // 15% spell damage
                hasFamiliar(FamiliarPool.GALLOPING_GRILL)); // 10% spell damage

        try (cleanups) {
          maximize("spell dmg");

          // used the lobster in the throne.
          assertTrue(someBoostIs(x -> commandStartsWith(x, "enthrone Rock Lobster")));
        }
      }

      @Test
      public void cannotCrownFamiliarsWithBeesInBeecore() {
        final var cleanups =
            new Cleanups(
                inPath(Path.BEES_HATE_YOU),
                canUse("Crown of Thrones"),
                hasFamiliar(FamiliarPool.LOBSTER), // 15% spell damage
                hasFamiliar(FamiliarPool.GALLOPING_GRILL)); // 10% spell damage

        try (cleanups) {
          maximize("spell dmg");

          // used the grill in the throne.
          assertTrue(someBoostIs(x -> commandStartsWith(x, "enthrone Galloping Grill")));
        }
      }
    }

    @Nested
    class Potions {
      @Test
      public void canUsePotionsWithBeesOutsideBeecore() {
        final var cleanups = new Cleanups(addItem("baggie of powdered sugar"));

        try (cleanups) {
          maximize("meat drop");

          assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 baggie of powdered sugar")));
        }
      }

      @Test
      public void cannotUsePotionsWithBeesInBeecore() {
        final var cleanups =
            new Cleanups(inPath(Path.BEES_HATE_YOU), addItem("baggie of powdered sugar"));

        try (cleanups) {
          maximize("meat drop");

          assertFalse(someBoostIs(x -> commandStartsWith(x, "use 1 baggie of powdered sugar")));
        }
      }
    }
  }

  @Nested
  class Plumber {
    @Test
    public void plumberCommandsErrorOutsidePlumber() {
      final var cleanups = new Cleanups(inPath(Path.AVATAR_OF_BORIS));

      try (cleanups) {
        assertFalse(maximize("plumber"));
        assertFalse(maximize("cold plumber"));
      }
    }

    @Test
    public void plumberCommandForcesSomePlumberItem() {
      final var cleanups =
          new Cleanups(
              inPath(Path.PATH_OF_THE_PLUMBER), canUse("work boots"), canUse("shiny ring", 3));

      try (cleanups) {
        assertTrue(maximize("plumber, mox"));
        recommends("work boots");
      }
    }

    @Test
    public void coldPlumberCommandForcesFlowerAndFrostyButton() {
      final var cleanups =
          new Cleanups(
              inPath(Path.PATH_OF_THE_PLUMBER),
              canUse("work boots"),
              canUse("bonfire flower"),
              canUse("frosty button"),
              canUse("shiny ring", 3));

      try (cleanups) {
        assertTrue(maximize("cold plumber, mox"));
        recommends("bonfire flower");
        recommends("frosty button");
      }
    }
  }

  @Nested
  class GelatinousNoob {
    @Test
    public void canAbsorbItemsForSkills() {
      final var cleanups =
          new Cleanups(
              inPath(Path.GELATINOUS_NOOB),
              addItem("Knob mushroom"),
              addItem("beer lens"),
              addItem("crossbow string"));

      try (cleanups) {
        assertTrue(maximize("meat"));
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶303"))); // Knob mushroom
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶443"))); // beer lens
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶109"))); // crossbow string
      }
    }

    @Test
    public void canAbsorbEquipmentForEnchants() {
      final var cleanups = new Cleanups(inPath(Path.GELATINOUS_NOOB), addItem("disco mask"));

      try (cleanups) {
        assertTrue(maximize("moxie -tie"));
        recommendedSlotIsEmpty(EquipmentManager.HAT);
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶9"))); // disco mask
      }
    }

    @Test
    @Disabled(
        "Doesn't try to absorb the turtle, even though this should use the same code as the mask case above")
    public void canAbsorbHelmetTurtleForEnchants() {
      final var cleanups = new Cleanups(inPath(Path.GELATINOUS_NOOB), addItem("helmet turtle"));

      try (cleanups) {
        assertTrue(maximize("muscle -tie"));
        recommendedSlotIsEmpty(EquipmentManager.HAT);
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶3"))); // helmet turtle
      }
    }

    @Test
    @Disabled("Bug: doesn't work in Mafia, but should")
    public void canBenefitFromOutfits() {
      final var cleanups =
          new Cleanups(
              inPath(Path.GELATINOUS_NOOB),
              addItem("The Jokester's wig"),
              addItem("The Jokester's gun"),
              addItem("The Jokester's pants"));

      try (cleanups) {
        assertTrue(maximize("meat -tie"));
        recommendedSlotIs(EquipmentManager.HAT, "The Jokester's wig");
        recommendedSlotIs(EquipmentManager.WEAPON, "The Jokester's gun");
        recommendedSlotIs(EquipmentManager.PANTS, "The Jokester's pants");
      }
    }
  }

  @Nested
  class WeaponModifiers {
    @Test
    public void clubModifierDoesntAffectOffhand() {
      final var cleanups =
          new Cleanups(
              addSkill("Double-Fisted Skull Smashing"),
              canUse("flaming crutch", 2),
              canUse("white sword", 2),
              canUse("dense meat sword"));
      try (cleanups) {
        assertTrue(EquipmentManager.canEquip("white sword"), "Can equip white sword");
        assertTrue(EquipmentManager.canEquip("flaming crutch"), "Can equip flaming crutch");
        assertTrue(maximize("mus, club"));
        // Should equip 1 flaming crutch, 1 white sword.
        recommendedSlotIs(EquipmentManager.WEAPON, "flaming crutch");
        recommendedSlotIs(EquipmentManager.OFFHAND, "white sword");
      }
    }

    @Test
    public void swordModifierFavorsSword() {
      final var cleanups = new Cleanups(canUse("sweet ninja sword"), canUse("spiked femur"));
      try (cleanups) {
        assertTrue(maximize("spooky dmg, sword"));
        recommendedSlotIs(EquipmentManager.WEAPON, "sweet ninja sword");
      }
    }
  }

  // effect limits

  @Test
  public void maximizeGiveBestScoreWithEffectsAtNoncombatLimit() {
    final var cleanups =
        new Cleanups(
            canUse("Space Trip safety headphones"),
            canUse("Krampus horn"),
            // get ourselves to -25 combat
            addEffect("Shelter of Shed"),
            addEffect("Smooth Movements"));
    try (cleanups) {
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

  @Nested
  class Underwater {
    @Test
    public void aboveWaterZonesDoNotCheckUnderwaterNegativeCombat() {
      final var cleanups = new Cleanups(inLocation("Noob Cave"), canUse("Mer-kin sneakmask"));
      try (cleanups) {
        assertTrue(maximize("-combat -tie"));
        assertEquals(0, modFor("Combat Rate"), 0.01);

        recommendedSlotIsEmpty(EquipmentManager.HAT);
      }
    }

    @Test
    public void underwaterZonesCheckUnderwaterNegativeCombat() {
      final var cleanups = new Cleanups(inLocation("The Ice Hole"), canUse("Mer-kin sneakmask"));
      try (cleanups) {
        assertEquals(AdventureDatabase.getEnvironment(Modifiers.currentLocation), "underwater");
        assertTrue(maximize("-combat -tie"));

        recommendedSlotIs(EquipmentManager.HAT, "Mer-kin sneakmask");
      }
    }
  }

  @Nested
  class Outfits {
    @Test
    public void considersOutfitsIfHelpful() {
      final var cleanups = new Cleanups(canUse("eldritch hat"), canUse("eldritch pants"));
      try (cleanups) {
        assertTrue(maximize("item -tie"));

        assertEquals(50, modFor("Item Drop"), 0.01);
        recommendedSlotIs(EquipmentManager.HAT, "eldritch hat");
        recommendedSlotIs(EquipmentManager.PANTS, "eldritch pants");
      }
    }

    @Test
    public void avoidsOutfitsIfOtherItemsBetter() {
      final var cleanups =
          new Cleanups(
              canUse("eldritch hat"), canUse("eldritch pants"), canUse("Team Avarice cap"));
      try (cleanups) {
        assertTrue(maximize("item -tie"));

        assertEquals(100, modFor("Item Drop"), 0.01);
        recommendedSlotIs(EquipmentManager.HAT, "Team Avarice cap");
      }
    }

    @Test
    public void forcingOutfitRequiresThatOutfit() {
      final var cleanups =
          new Cleanups(
              canUse("bounty-hunting helmet"),
              canUse("bounty-hunting rifle"),
              canUse("bounty-hunting pants"),
              canUse("eldritch hat"),
              canUse("eldritch pants"));
      try (cleanups) {
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
  }

  @Nested
  class Synergy {
    @Test
    public void considersBrimstoneIfHelpful() {
      final var cleanups = new Cleanups(canUse("Brimstone Beret"), canUse("Brimstone Boxers"));
      try (cleanups) {
        assertTrue(maximize("ml -tie"));

        assertEquals(4, modFor("Monster Level"), 0.01);
        recommendedSlotIs(EquipmentManager.HAT, "Brimstone Beret");
        recommendedSlotIs(EquipmentManager.PANTS, "Brimstone Boxers");
      }
    }

    @Nested
    class Smithsness {
      @Test
      public void considersSmithsnessIfHelpful() {
        final var cleanups = new Cleanups(canUse("Half a Purse"), canUse("Hairpiece On Fire"));
        try (cleanups) {
          assertTrue(maximize("meat -tie"));

          recommendedSlotIs(EquipmentManager.OFFHAND, "Half a Purse");
          recommendedSlotIs(EquipmentManager.HAT, "Hairpiece On Fire");
        }
      }

      @Test
      public void usesFlaskfullOfHollowWithSmithsness() {
        final var cleanups = new Cleanups(addItem("Flaskfull of Hollow"), setStats(100, 100, 100), equip(EquipmentManager.PANTS, "Vicar's Tutu"));
        try (cleanups) {
          assertTrue(maximize("muscle -tie"));

          assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 Flaskfull of Hollow")));
        }
      }

      @Test
      @Disabled("fails to recommend to use the flask")
      public void usesFlaskfullOfHollow() {
        final var cleanups = new Cleanups(addItem("Flaskfull of Hollow"));
        try (cleanups) {
          assertTrue(maximize("muscle -tie"));

          assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 Flaskfull of Hollow")));
        }
      }
    }

    @Test
    public void considersCloathingIfHelpful() {
      final var cleanups = new Cleanups(canUse("Goggles of Loathing"), canUse("Jeans of Loathing"));
      try (cleanups) {
        assertTrue(maximize("item -tie"));

        assertEquals(2, modFor("Item Drop"), 0.01);
        recommendedSlotIs(EquipmentManager.HAT, "Goggles of Loathing");
        recommendedSlotIs(EquipmentManager.PANTS, "Jeans of Loathing");
      }
    }

    @Nested
    class SlimeHatesIt {
      @Test
      public void considersInSlimeTube() {
        final var cleanups =
            new Cleanups(
                inLocation("The Slime Tube"),
                canUse("pernicious cudgel"),
                canUse("grisly shield"),
                canUse("shield of the Skeleton Lord"),
                addItem("bitter pill"));
        try (cleanups) {
          assertTrue(maximize("ml -tie"));

          recommendedSlotIs(EquipmentManager.WEAPON, "pernicious cudgel");
          recommendedSlotIs(EquipmentManager.OFFHAND, "grisly shield");
          assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 bitter pill")));
        }
      }

      @Test
      public void doesntCountIfNotInSlimeTube() {
        final var cleanups =
            new Cleanups(
                inLocation("Noob Cave"),
                canUse("pernicious cudgel"),
                canUse("grisly shield"),
                canUse("shield of the Skeleton Lord"));
        try (cleanups) {
          assertTrue(maximize("ml -tie"));

          recommendedSlotIs(EquipmentManager.OFFHAND, "shield of the Skeleton Lord");
        }
      }
    }

    @Nested
    class HoboPower {
      @Test
      public void usesHoboPowerIfPossible() {
        final var cleanups =
            new Cleanups(
                canUse("Hodgman's garbage sticker"),
                canUse("Hodgman's bow tie"),
                canUse("Hodgman's lobsterskin pants"),
                canUse("Hodgman's porkpie hat"),
                canUse("silver cow creamer"));
        try (cleanups) {
          assertTrue(maximize("meat -tie"));

          recommendedSlotIs(EquipmentManager.HAT, "Hodgman's porkpie hat");
          recommendedSlotIs(EquipmentManager.PANTS, "Hodgman's lobsterskin pants");
          recommendedSlotIs(EquipmentManager.OFFHAND, "Hodgman's garbage sticker");
          recommends("Hodgman's bow tie");
        }
      }

      @Test
      public void hoboPowerDoesntCountWithoutOffhand() {
        final var cleanups =
            new Cleanups(canUse("Hodgman's bow tie"), canUse("silver cow creamer"));
        try (cleanups) {
          assertTrue(maximize("meat -tie"));

          assertEquals(30, modFor("Meat Drop"), 0.01);
          recommendedSlotIs(EquipmentManager.OFFHAND, "silver cow creamer");
          recommendedSlotIsEmpty(EquipmentManager.ACCESSORY1);
          recommendedSlotIsEmpty(EquipmentManager.ACCESSORY2);
          recommendedSlotIsEmpty(EquipmentManager.ACCESSORY3);
        }
      }
    }
  }
}
