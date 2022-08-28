package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.commandStartsWith;
import static internal.helpers.Maximizer.getSlot;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Maximizer.recommendedSlotIs;
import static internal.helpers.Maximizer.recommendedSlotIsUnchanged;
import static internal.helpers.Maximizer.recommends;
import static internal.helpers.Maximizer.someBoostIs;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withLocation;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.Optional;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class MaximizerTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("MaximizerTest");
    Preferences.reset("MaximizerTest");
  }
  // basic

  @Test
  public void changesGear() {
    final var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
    try (cleanups) {
      assertTrue(maximize("mus"));
      assertEquals(1, modFor("Buffed Muscle"), 0.01);
    }
  }

  @Test
  public void equipsItemsOnlyIfHasStats() {
    final var cleanups =
        new Cleanups(withEquippableItem("helmet turtle"), withItem("wreath of laurels"));
    try (cleanups) {
      assertTrue(maximize("mus"));
      assertEquals(1, modFor("Buffed Muscle"), 0.01);
      recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
    }
  }

  @Test
  public void nothingBetterThanSomething() {
    final var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
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
              withEquippableItem("hardened slime hat"),
              withEquippableItem("bounty-hunting helmet"),
              withSkill("Refusal to Freeze"));
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
              withEquippableItem("hardened slime hat"),
              withEquippableItem("bounty-hunting helmet"),
              withSkill("Refusal to Freeze"));
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
      final var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("mus 2 min"));
        // still provides equipment
        recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
      }
    }

    @Test
    public void minKeywordPassesMaximizationIfHit() {
      final var cleanups = new Cleanups(withEquippableItem("wreath of laurels"));
      try (cleanups) {
        assertTrue(maximize("mus 2 min"));
      }
    }

    @Test
    public void startingMinKeywordFailsMaximizationIfNotHit() {
      final var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
      try (cleanups) {
        assertFalse(maximize("2 min, mus"));
        // still provides equipment
        recommendedSlotIs(EquipmentManager.HAT, "helmet turtle");
      }
    }

    @Test
    public void startingMinKeywordPassesMaximizationIfHit() {
      final var cleanups = new Cleanups(withEquippableItem("wreath of laurels"));
      try (cleanups) {
        assertTrue(maximize("2 min, mus"));
      }
    }
  }

  @Nested
  class Clownosity {
    @Test
    public void clownosityTriesClownEquipment() {
      final var cleanups = new Cleanups(withEquippableItem("clown wig"));
      try (cleanups) {
        assertFalse(maximize("clownosity -tie"));
        // still provides equipment
        recommendedSlotIs(EquipmentManager.HAT, "clown wig");
        assertEquals(50, modFor("Clowniness"), 0.01);
      }
    }

    @Test
    public void clownositySucceedsWithEnoughEquipment() {
      final var cleanups =
          new Cleanups(withEquippableItem("clown wig"), withEquippableItem("polka-dot bow tie"));
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
          new Cleanups(
              withEquippableItem("rave visor"),
              withEquippableItem("baggy rave pants"),
              withEquippableItem("rave whistle"));
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
              withEquippableItem("blue glowstick"),
              withEquippableItem("glowstick on a string"),
              withEquippableItem("teddybear backpack"),
              withEquippableItem("rave visor"),
              withEquippableItem("baggy rave pants"));
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
              withEquippableItem("head mirror"),
              withEquippableItem("bloodied surgical dungarees"),
              withEquippableItem("surgical apron"),
              withEquippableItem("surgical mask"),
              withEquippableItem("half-size scalpel"),
              withSkill("Torso Awareness"));
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
          new Cleanups(
              withPath(Path.BEES_HATE_YOU), withEquippableItem("bubblewrap bottlecap turtleban"));
      try (cleanups) {
        maximize("mys");
        recommendedSlotIsUnchanged(EquipmentManager.HAT);
      }
    }

    @Test
    public void itemsCanHaveAtMostBeeosityBees() {
      final var cleanups =
          new Cleanups(
              withPath(Path.BEES_HATE_YOU), withEquippableItem("bubblewrap bottlecap turtleban"));
      try (cleanups) {
        maximize("mys, 5beeosity");
        recommendedSlotIs(EquipmentManager.HAT, "bubblewrap bottlecap turtleban");
      }
    }

    @Test
    public void beeosityDoesntApplyOutsideBeePath() {
      final var cleanups = new Cleanups(withEquippableItem("bubblewrap bottlecap turtleban"));
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
                withEquippableItem("Crown of Thrones"),
                withFamiliarInTerrarium(FamiliarPool.LOBSTER), // 15% spell damage
                withFamiliarInTerrarium(FamiliarPool.GALLOPING_GRILL)); // 10% spell damage

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
                withPath(Path.BEES_HATE_YOU),
                withEquippableItem("Crown of Thrones"),
                withFamiliarInTerrarium(FamiliarPool.LOBSTER), // 15% spell damage
                withFamiliarInTerrarium(FamiliarPool.GALLOPING_GRILL)); // 10% spell damage

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
        final var cleanups = new Cleanups(withItem("baggie of powdered sugar"));

        try (cleanups) {
          maximize("meat drop");

          assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 baggie of powdered sugar")));
        }
      }

      @Test
      public void cannotUsePotionsWithBeesInBeecore() {
        final var cleanups =
            new Cleanups(withPath(Path.BEES_HATE_YOU), withItem("baggie of powdered sugar"));

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
      final var cleanups = new Cleanups(withPath(Path.AVATAR_OF_BORIS));

      try (cleanups) {
        assertFalse(maximize("plumber"));
        assertFalse(maximize("cold plumber"));
      }
    }

    @Test
    public void plumberCommandForcesSomePlumberItem() {
      final var cleanups =
          new Cleanups(
              withPath(Path.PATH_OF_THE_PLUMBER),
              withEquippableItem("work boots"),
              withEquippableItem("shiny ring", 3));

      try (cleanups) {
        assertTrue(maximize("plumber, mox"));
        recommends("work boots");
      }
    }

    @Test
    public void coldPlumberCommandForcesFlowerAndFrostyButton() {
      final var cleanups =
          new Cleanups(
              withPath(Path.PATH_OF_THE_PLUMBER),
              withEquippableItem("work boots"),
              withEquippableItem("bonfire flower"),
              withEquippableItem("frosty button"),
              withEquippableItem("shiny ring", 3));

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
              withPath(Path.GELATINOUS_NOOB),
              withItem("Knob mushroom"),
              withItem("beer lens"),
              withItem("crossbow string"));

      try (cleanups) {
        assertTrue(maximize("meat"));
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶303"))); // Knob mushroom
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶443"))); // beer lens
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶109"))); // crossbow string
      }
    }

    @Test
    public void canAbsorbEquipmentForEnchants() {
      final var cleanups = new Cleanups(withPath(Path.GELATINOUS_NOOB), withItem("disco mask"));

      try (cleanups) {
        assertTrue(maximize("moxie -tie"));
        recommendedSlotIsUnchanged(EquipmentManager.HAT);
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶9"))); // disco mask
      }
    }

    @Test
    public void canAbsorbHelmetTurtleForEnchants() {
      final var cleanups = new Cleanups(withPath(Path.GELATINOUS_NOOB), withItem("helmet turtle"));

      try (cleanups) {
        assertTrue(maximize("muscle -tie"));
        recommendedSlotIsUnchanged(EquipmentManager.HAT);
        assertTrue(someBoostIs(x -> commandStartsWith(x, "absorb ¶3"))); // helmet turtle
      }
    }

    @Test
    public void canBenefitFromOutfits() {
      final var cleanups =
          new Cleanups(
              withPath(Path.GELATINOUS_NOOB),
              withEquippableItem("bugbear beanie"),
              withEquippableItem("bugbear bungguard"));

      try (cleanups) {
        assertTrue(maximize("spell dmg -tie"));
        recommendedSlotIs(EquipmentManager.HAT, "bugbear beanie");
        recommendedSlotIs(EquipmentManager.PANTS, "bugbear bungguard");
      }
    }

    @Test
    public void canBenefitFromOutfitsWithWeapons() {
      final var cleanups =
          new Cleanups(
              withPath(Path.GELATINOUS_NOOB),
              withEquippableItem("The Jokester's wig"),
              withEquippableItem("The Jokester's gun"),
              withEquippableItem("The Jokester's pants"));

      try (cleanups) {
        assertTrue(maximize("meat -tie"));
        recommendedSlotIs(EquipmentManager.HAT, "The Jokester's wig");
        recommendedSlotIs(EquipmentManager.WEAPON, "The Jokester's gun");
        recommendedSlotIs(EquipmentManager.PANTS, "The Jokester's pants");
      }
    }
  }

  @Nested
  class Letter {
    @Test
    public void equipLongestItems() {
      final var cleanups =
          new Cleanups(withEquippableItem("spiked femur"), withEquippableItem("sweet ninja sword"));

      try (cleanups) {
        maximize("letter");

        recommendedSlotIs(EquipmentManager.WEAPON, "sweet ninja sword");
      }
    }

    @Test
    public void equipMostLetterItems() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("asparagus knife"),
              withEquippableItem("sweet ninja sword"),
              withEquippableItem("Fourth of May Cosplay Saber"),
              withEquippableItem("old sweatpants"));

      try (cleanups) {
        maximize("letter n");

        recommendedSlotIs(EquipmentManager.WEAPON, "sweet ninja sword");
        recommendedSlotIs(EquipmentManager.PANTS, "old sweatpants");
      }
    }

    @Test
    public void equipMostNumberItems() {
      final var cleanups =
          new Cleanups(withEquippableItem("X-37 gun"), withEquippableItem("sweet ninja sword"));

      try (cleanups) {
        maximize("number");

        recommendedSlotIs(EquipmentManager.WEAPON, "X-37 gun");
      }
    }
  }

  @Nested
  class WeaponModifiers {
    @Test
    public void clubModifierDoesntAffectOffhand() {
      final var cleanups =
          new Cleanups(
              withSkill("Double-Fisted Skull Smashing"),
              withEquippableItem("flaming crutch", 2),
              withEquippableItem("white sword", 2),
              withEquippableItem("dense meat sword"));

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
      final var cleanups =
          new Cleanups(withEquippableItem("sweet ninja sword"), withEquippableItem("spiked femur"));

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
            withEquippableItem("Space Trip safety headphones"),
            withEquippableItem("Krampus horn"),
            // get ourselves to -25 combat
            withEffect("Shelter of Shed"),
            withEffect("Smooth Movements"));

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
      final var cleanups =
          new Cleanups(withLocation("Noob Cave"), withEquippableItem("Mer-kin sneakmask"));

      try (cleanups) {
        assertTrue(maximize("-combat -tie"));
        assertEquals(0, modFor("Combat Rate"), 0.01);

        recommendedSlotIsUnchanged(EquipmentManager.HAT);
      }
    }

    @Test
    public void underwaterZonesCheckUnderwaterNegativeCombat() {
      final var cleanups =
          new Cleanups(withLocation("The Ice Hole"), withEquippableItem("Mer-kin sneakmask"));

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
      final var cleanups =
          new Cleanups(withEquippableItem("eldritch hat"), withEquippableItem("eldritch pants"));

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
              withEquippableItem("eldritch hat"),
              withEquippableItem("eldritch pants"),
              withEquippableItem("Team Avarice cap"));

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
              withEquippableItem("bounty-hunting helmet"),
              withEquippableItem("bounty-hunting rifle"),
              withEquippableItem("bounty-hunting pants"),
              withEquippableItem("eldritch hat"),
              withEquippableItem("eldritch pants"));

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
      final var cleanups =
          new Cleanups(
              withEquippableItem("Brimstone Beret"), withEquippableItem("Brimstone Boxers"));

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
        final var cleanups =
            new Cleanups(
                withEquippableItem("Half a Purse"), withEquippableItem("Hairpiece On Fire"));

        try (cleanups) {
          assertTrue(maximize("meat -tie"));
          recommendedSlotIs(EquipmentManager.OFFHAND, "Half a Purse");
          recommendedSlotIs(EquipmentManager.HAT, "Hairpiece On Fire");
        }
      }

      @Test
      public void usesFlaskfullOfHollowWithSmithsness() {
        final var cleanups =
            new Cleanups(
                withItem("Flaskfull of Hollow"),
                withStats(100, 100, 100),
                withEquipped(EquipmentManager.PANTS, "Vicar's Tutu"));

        try (cleanups) {
          assertTrue(maximize("muscle -tie"));
          assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 Flaskfull of Hollow")));
        }
      }

      @Test
      @Disabled("fails to recommend to use the flask")
      public void usesFlaskfullOfHollow() {
        final var cleanups =
            new Cleanups(withItem("Flaskfull of Hollow"), withStats(100, 100, 100));

        try (cleanups) {
          assertTrue(maximize("muscle -tie"));
          assertTrue(someBoostIs(x -> commandStartsWith(x, "use 1 Flaskfull of Hollow")));
        }
      }
    }

    @Test
    public void considersCloathingIfHelpful() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("Goggles of Loathing"), withEquippableItem("Jeans of Loathing"));

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
                withLocation("The Slime Tube"),
                withEquippableItem("pernicious cudgel"),
                withEquippableItem("grisly shield"),
                withEquippableItem("shield of the Skeleton Lord"),
                withItem("bitter pill"));

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
                withLocation("Noob Cave"),
                withEquippableItem("pernicious cudgel"),
                withEquippableItem("grisly shield"),
                withEquippableItem("shield of the Skeleton Lord"));

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
                withEquippableItem("Hodgman's garbage sticker"),
                withEquippableItem("Hodgman's bow tie"),
                withEquippableItem("Hodgman's lobsterskin pants"),
                withEquippableItem("Hodgman's porkpie hat"),
                withEquippableItem("silver cow creamer"));

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
            new Cleanups(
                withEquippableItem("Hodgman's bow tie"), withEquippableItem("silver cow creamer"));

        try (cleanups) {
          assertTrue(maximize("meat -tie"));

          assertEquals(30, modFor("Meat Drop"), 0.01);
          recommendedSlotIs(EquipmentManager.OFFHAND, "silver cow creamer");
          recommendedSlotIsUnchanged(EquipmentManager.ACCESSORY1);
          recommendedSlotIsUnchanged(EquipmentManager.ACCESSORY2);
          recommendedSlotIsUnchanged(EquipmentManager.ACCESSORY3);
        }
      }
    }
  }

  @Nested
  class Modeables {
    @Test
    public void canFoldUmbrella() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"), withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("Monster Level Percent"));
        recommendedSlotIs(EquipmentManager.OFFHAND, "umbrella broken");
      }
    }

    @Test
    public void expShouldSuggestUmbrella() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(EquipmentManager.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp"));
        recommendedSlotIs(EquipmentManager.OFFHAND, "umbrella broken");
      }
    }

    @Test
    public void expShouldNotSuggestUmbrellaIfBetterInSlot() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(EquipmentManager.PANTS, "old patched suit-pants"),
              withEquippableItem("vinyl shield"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp"));
        recommendedSlotIs(EquipmentManager.OFFHAND, "vinyl shield");
      }
    }

    @Test
    public void chooseForwardFacingUmbrellaToSatisfyShield() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquippableItem("tip jar"),
              withEquipped(EquipmentManager.PANTS, "old sweatpants"));

      try (cleanups) {
        assertTrue(maximize("meat, shield"));
        someBoostIs(b -> commandStartsWith(b, "umbrella forward-facing"));
        recommendedSlotIs(EquipmentManager.OFFHAND, "unbreakable umbrella");
      }
    }

    @Test
    public void edPieceChoosesFishWithSea() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("The Crown of Ed the Undying"),
              withEquippableItem("star shirt"),
              withEquipped(EquipmentManager.PANTS, "old sweatpants"),
              withProperty("edPiece", "puma"));

      try (cleanups) {
        assertTrue(maximize("muscle, sea"));
        someBoostIs(b -> commandStartsWith(b, "edpiece fish"));
        recommendedSlotIs(EquipmentManager.HAT, "The Crown of Ed the Undying");
      }
    }

    @Test
    public void edPieceChoosesBasedOnModesWithoutSea() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("The Crown of Ed the Undying"),
              withEquippableItem("star shirt"),
              withEquipped(EquipmentManager.PANTS, "old sweatpants"),
              withProperty("edPiece", "puma"));

      try (cleanups) {
        assertTrue(maximize("muscle"));
        someBoostIs(b -> commandStartsWith(b, "edpiece bear"));
        recommendedSlotIs(EquipmentManager.HAT, "The Crown of Ed the Undying");
      }
    }

    @Test
    public void multipleModeables() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("backup camera"),
              withEquippableItem("unbreakable umbrella"),
              withEquipped(EquipmentManager.PANTS, "old sweatpants"));
      try (cleanups) {
        assertTrue(maximize("ml, -combat, equip backup camera, equip unbreakable umbrella"));
        someBoostIs(b -> commandStartsWith(b, "umbrella cocoon"));
        someBoostIs(b -> commandStartsWith(b, "backupcamera ml"));
      }
    }

    @Test
    public void doesNotSelectUmbrellaIfNegativeToOurGoal() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquippableItem("old sweatpants"),
              withEquippableItem("star boomerang"));

      try (cleanups) {
        assertTrue(maximize("-hp"));
        recommendedSlotIs(EquipmentManager.WEAPON, "star boomerang");
        assertThat(getSlot(EquipmentManager.OFFHAND), equalTo(Optional.empty()));
      }
    }

    @Test
    public void equipUmbrellaOnLeftHandMan() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withEquipped(EquipmentManager.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp, -offhand"));
        recommendedSlotIs(EquipmentManager.FAMILIAR, "unbreakable umbrella");
        assertThat(getSlot(EquipmentManager.OFFHAND), equalTo(Optional.empty()));
      }
    }

    @Test
    public void suggestEquippingUmbrellaOnLeftHandMan() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withEquipped(EquipmentManager.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp, -offhand, switch left-hand man"));
        assertThat(someBoostIs(b -> commandStartsWith(b, "familiar Left-Hand Man")), equalTo(true));
        assertThat(
            someBoostIs(b -> commandStartsWith(b, "umbrella broken; equip familiar ¶10899")),
            equalTo(true));
      }
    }

    @Test
    public void suggestEquippingSomethingBetterThanUmbrellaOnLeftHandMan() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withEquipped(EquipmentManager.PANTS, "old patched suit-pants"),
              withEquippableItem("shield of the Skeleton Lord"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp, -offhand, switch left-hand man"));
        assertThat(someBoostIs(b -> commandStartsWith(b, "familiar Left-Hand Man")), equalTo(true));
        assertThat(someBoostIs(b -> commandStartsWith(b, "equip familiar ¶9890")), equalTo(true));
      }
    }

    @Test
    public void shouldSuggestTunedRetrocape() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unwrapped knock-off retro superhero cape"),
              withEquippableItem("palm-frond cloak"),
              withProperty("retroCapeSuperhero", "vampire"),
              withProperty("retroCapeWashingInstructions", "thrill"));

      try (cleanups) {
        assertTrue(maximize("hot res"));
        recommendedSlotIs(EquipmentManager.CONTAINER, "unwrapped knock-off retro superhero cape");
        assertTrue(someBoostIs(x -> commandStartsWith(x, "retrocape vampire hold")));
      }
    }

    @Test
    public void shouldSuggestTunedSnowsuit() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("Snow Suit"),
              withEquippableItem("wax lips"),
              withFamiliar(FamiliarPool.BLOOD_FACED_VOLLEYBALL));

      try (cleanups) {
        assertTrue(maximize("exp, hp regen"));
        recommendedSlotIs(EquipmentManager.FAMILIAR, "Snow Suit");
        assertTrue(someBoostIs(x -> commandStartsWith(x, "snowsuit goatee")));
      }
    }

    @Test
    public void shouldSuggestCameraIfSecondBest() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("backup camera"),
              withEquippableItem("incredibly dense meat gem"),
              withProperty("backupCameraMode", "ml"));

      try (cleanups) {
        assertTrue(maximize("meat"));
        recommends("backup camera");
        recommends("incredibly dense meat gem");
        assertTrue(someBoostIs(x -> commandStartsWith(x, "backupcamera meat")));
      }
    }

    @Test
    public void shouldSuggestCameraIfSlotsExcluded() {
      final var cleanups =
          new Cleanups(withEquippableItem("backup camera"), withProperty("backupCameraMode", "ml"));

      try (cleanups) {
        assertTrue(maximize("meat -acc1 -acc2"));
        recommendedSlotIs(EquipmentManager.ACCESSORY3, "backup camera");
        assertTrue(someBoostIs(x -> commandStartsWith(x, "backupcamera meat")));
      }
    }
  }

  @Nested
  class Horsery {
    @Test
    public void suggestsHorseryIfAvailable() {
      var cleanups = withProperty("horseryAvailable", true);

      try (cleanups) {
        assertTrue(maximize("-combat"));
        assertTrue(someBoostIs(x -> commandStartsWith(x, "horsery dark")));
      }
    }

    @Test
    public void doesNotSuggestHorseryIfUnaffordable() {
      var cleanups =
          new Cleanups(
              withProperty("horseryAvailable", true),
              withProperty("_horsery", "normal horse"),
              withMeat(0));

      try (cleanups) {
        assertTrue(maximize("-combat"));
        assertFalse(someBoostIs(x -> commandStartsWith(x, "horsery dark")));
      }
    }

    @Test
    public void doesNotSuggestHorseryIfNotAllowedInStandard() {
      var cleanups =
          new Cleanups(
              withProperty("horseryAvailable", true),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Horsery contract"));

      try (cleanups) {
        assertTrue(maximize("-combat"));
        assertFalse(someBoostIs(x -> commandStartsWith(x, "horsery dark")));
      }
    }
  }
}
