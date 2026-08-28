package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.maximizeAny;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Player.withCampgroundItem;
import static internal.helpers.Player.withClass;
import static internal.helpers.Player.withDay;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withItemInFreepulls;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withLocation;
import static internal.helpers.Player.withMCD;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withMoxie;
import static internal.helpers.Player.withMuscle;
import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withOverrideModifiers;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withSign;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static internal.matchers.Maximizer.recommends;
import static internal.matchers.Maximizer.recommendsEffect;
import static internal.matchers.Maximizer.recommendsSlot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.time.Month;
import java.util.EnumSet;
import net.sourceforge.kolmafia.AscensionClass;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.equipment.SlotSet;
import net.sourceforge.kolmafia.modifiers.BitmapModifier;
import net.sourceforge.kolmafia.modifiers.DerivedModifier;
import net.sourceforge.kolmafia.modifiers.DoubleModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureDatabase.Environment;
import net.sourceforge.kolmafia.persistence.MallPriceDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.EquipmentRequest;
import net.sourceforge.kolmafia.session.EquipmentManager;
import net.sourceforge.kolmafia.session.MallPriceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

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
      assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
    }
  }

  @Test
  public void equipsItemsOnlyIfHasStats() {
    final var cleanups =
        new Cleanups(withEquippableItem("helmet turtle"), withItem("wreath of laurels"));
    try (cleanups) {
      assertTrue(maximize("mus"));
      assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
    }
  }

  @Test
  public void nothingBetterThanSomething() {
    final var cleanups = new Cleanups(withEquippableItem("helmet turtle"));
    try (cleanups) {
      assertTrue(maximize("-mus"));
      assertEquals(0, modFor(DerivedModifier.BUFFED_MUS), 0.01);
    }
  }

  @Test
  public void exactMatchFindsModifier() {
    var cleanups =
        new Cleanups(
            withEquippableItem("hemlock helm"),
            withEquippableItem("government-issued slacks"),
            // Not a muscle day
            withDay(2023, Month.SEPTEMBER, 27));

    try (cleanups) {
      assertTrue(maximize("Muscle Experience Percent, -tie"));
      assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "government-issued slacks")));
      assertEquals(10, modFor(DoubleModifier.MUS_EXPERIENCE_PCT), 0.01);
    }
  }

  @ParameterizedTest
  @CsvSource({
    "muscle exp perc, government-issued slacks",
    "mus experience percent, government-issued slacks",
    "mus experience percentage, government-issued slacks",
    "mus exp, Pantsgiving",
    "mus experience, Pantsgiving",
    "muscle exp, Pantsgiving",
    "muscle perc, sugar shorts",
    "mus percent, sugar shorts",
    "mus percentage, sugar shorts",
    "mus, leg-mounted Trainbots"
  })
  public void findsGenericAbbreviations(String abbreviation, String expectedItem) {
    var cleanups =
        new Cleanups(
            withEquippableItem("government-issued slacks"),
            withEquippableItem("pantsgiving"),
            withEquippableItem("sugar shorts"),
            withEquippableItem("leg-mounted Trainbots"),
            // Not a muscle day
            withDay(2023, Month.SEPTEMBER, 27));

    try (cleanups) {
      assertTrue(maximize(abbreviation + ", -tie"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, expectedItem)));
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

        assertEquals(3, modFor(DoubleModifier.COLD_RESISTANCE), 0.01);
        assertEquals(20, modFor(DoubleModifier.ITEMDROP), 0.01);

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet")));
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

        assertThat(
            getBoosts(),
            hasItem(
                hasToString(
                    containsString("(maximum achieved, no further combinations checked)"))));
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
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
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
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
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
  class Effective {
    @Test
    public void useRangedWeaponWhenMoxieHigh() {
      final var cleanups =
          new Cleanups(
              withStats(100, 100, 150),
              withEquippableItem("disco ball"),
              withEquippableItem("two-handed depthsword"));

      try (cleanups) {
        assertTrue(maximize("weapon dmg, effective"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "disco ball")));
      }
    }

    @Test
    public void useMeleeWeaponWhenMuscleHigh() {
      final var cleanups =
          new Cleanups(
              withStats(150, 100, 100),
              withEquippableItem("automatic catapult"),
              withEquippableItem("seal-clubbing club"));

      try (cleanups) {
        assertTrue(maximize("weapon dmg, effective"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "seal-clubbing club")));
      }
    }

    @Test
    public void useJuneCleaverWhenMoxieHigh() {
      final var cleanups =
          new Cleanups(
              withStats(100, 100, 150),
              withEquippableItem("disco ball"),
              withEquippableItem("June cleaver"));

      try (cleanups) {
        assertTrue(maximize("weapon dmg, effective"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "June cleaver")));
      }
    }

    @Test
    public void useCosplaySaberWhenMoxieHigh() {
      final var cleanups =
          new Cleanups(
              withStats(100, 100, 150),
              withEquippableItem("disco ball"),
              withEquippableItem("Fourth of May Cosplay Saber"));

      try (cleanups) {
        assertTrue(maximize("weapon dmg, effective"));
        assertThat(
            getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "Fourth of May Cosplay Saber")));
      }
    }

    /**
     * These tests illustrate that sometimes with the effective keyword the maximizer will choose no
     * weapon. They do not go so far as to try and verify that the selected weapon was actually
     * equipped.
     */
    @Test
    public void muscleEffectiveDoesNotSelectRanged() {
      String maxStr = "effective";
      var cleanups =
          new Cleanups(
              withStats(10, 5, 5),
              withEquippableItem("seal-skull helmet"),
              withEquippableItem("astral shirt"),
              withEquippableItem("old sweatpants"),
              withEquippableItem("sewer snake"));
      try (cleanups) {
        assertTrue(maximize(maxStr));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
      }
    }

    @Test
    public void moxieEffectiveDoesNotSelectMelee() {
      String maxStr = "effective";
      var cleanups =
          new Cleanups(
              withStats(5, 5, 10),
              withEquippableItem("seal-skull helmet"),
              withEquippableItem("astral shirt"),
              withEquippableItem("old sweatpants"),
              withEquippableItem("seal-clubbing club"));
      try (cleanups) {
        assertTrue(maximize(maxStr));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
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
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "clown wig")));
        assertEquals(50, modFor(BitmapModifier.CLOWNINESS), 0.01);
      }
    }

    @Test
    public void clownositySucceedsWithEnoughEquipment() {
      final var cleanups =
          new Cleanups(withEquippableItem("clown wig"), withEquippableItem("polka-dot bow tie"));
      try (cleanups) {
        assertTrue(maximize("clownosity -tie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "clown wig")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY1, "polka-dot bow tie")));
        assertEquals(125, modFor(BitmapModifier.CLOWNINESS), 0.01);
      }
    }

    @Test
    public void clownosityItemsDontStack() {
      var cleanups = withEquippableItem("clownskin belt", 3);

      try (cleanups) {
        maximize("clownosity, -tie");
        assertEquals(50, modFor(BitmapModifier.CLOWNINESS), 0.01);
        assertThat(
            getBoosts().stream()
                .filter(x -> x.isEquipment() && "clownskin belt".equals(x.getItem().getName()))
                .count(),
            equalTo(1L));
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
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "rave visor")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "baggy rave pants")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "rave whistle")));
        assertEquals(5, modFor(BitmapModifier.RAVEOSITY), 0.01);
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
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "rave visor")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "baggy rave pants")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.CONTAINER, "teddybear backpack")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "glowstick on a string")));
        assertEquals(7, modFor(BitmapModifier.RAVEOSITY), 0.01);
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
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "bloodied surgical dungarees")));
        assertThat(getBoosts(), hasItem(recommends("head mirror")));
        assertThat(getBoosts(), hasItem(recommends("surgical mask")));
        assertThat(getBoosts(), hasItem(recommends("half-size scalpel")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "surgical apron")));
        assertEquals(5, modFor(BitmapModifier.SURGEONOSITY), 0.01);
      }
    }

    @Test
    public void surgeonosityItemsDontStack() {
      var cleanups = withEquippableItem("surgical mask", 3);

      try (cleanups) {
        maximize("surgeonosity, -tie");
        assertEquals(1, modFor(BitmapModifier.SURGEONOSITY), 0.01);
        assertThat(
            getBoosts().stream()
                .filter(x -> x.isEquipment() && "surgical mask".equals(x.getItem().getName()))
                .count(),
            equalTo(1L));
      }
    }
  }

  @Nested
  class Potions {
    @Test
    public void recommendsUsableNonPotion() {
      var cleanups = withItem(ItemPool.CHARTER_NELLYVILLE);

      try (cleanups) {
        maximize("hot dmg");

        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("use 1 Charter: Nellyville"))));
      }
    }

    @Test
    public void recommendsLoathingIdol() {
      var cleanups = withItem(ItemPool.LOATHING_IDOL_MICROPHONE_50);

      try (cleanups) {
        maximize("init");

        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("loathingidol pop"))));
      }
    }

    @Test
    public void givesCorrectEffectDuration() {
      var cleanups =
          new Cleanups(withProperty("verboseMaximizer", true), withItem(ItemPool.CUP_OF_SUGAR));

      try (cleanups) {
        maximize("init");

        var boosts = getBoosts();
        assertThat(boosts, hasItem(hasProperty("cmd", startsWith("eat 1 cup of sugar"))));
        var boost =
            boosts.stream()
                .filter(x -> x.getCmd().startsWith("eat 1 cup of sugar"))
                .findAny()
                .orElseThrow();
        assertThat(boost.toString(), containsString("10 advs duration"));
      }
    }

    @Test
    public void doesntCrashOnInvalidData() {
      // you can end up with effects without durations e.g. in current TCRS
      var cleanups =
          new Cleanups(
              withProperty("verboseMaximizer", true),
              withItem(ItemPool.BLACK_CANDLE),
              withOverrideModifiers(
                  ModifierType.ITEM, ItemPool.BLACK_CANDLE, "Effect: \"Rainy Soul Miasma\""));

      try (cleanups) {
        maximize("muscle");

        var boosts = getBoosts();
        assertThat(boosts, hasItem(hasProperty("cmd", startsWith("use 1 thin black candle"))));
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
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
      }
    }

    @Test
    public void itemsCanHaveAtMostBeeosityBees() {
      final var cleanups =
          new Cleanups(
              withPath(Path.BEES_HATE_YOU), withEquippableItem("bubblewrap bottlecap turtleban"));
      try (cleanups) {
        maximize("mys, 5beeosity");
        assertThat(
            getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bubblewrap bottlecap turtleban")));
      }
    }

    @Test
    public void beeosityDoesntApplyOutsideBeePath() {
      final var cleanups = new Cleanups(withEquippableItem("bubblewrap bottlecap turtleban"));
      try (cleanups) {
        maximize("mys");
        assertThat(
            getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bubblewrap bottlecap turtleban")));
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
          assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("enthrone Rock Lobster"))));
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
          assertThat(
              getBoosts(), hasItem(hasProperty("cmd", startsWith("enthrone Galloping Grill"))));
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

          assertThat(
              getBoosts(),
              hasItem(hasProperty("cmd", startsWith("use 1 baggie of powdered sugar"))));
        }
      }

      @Test
      public void cannotUsePotionsWithBeesInBeecore() {
        final var cleanups =
            new Cleanups(withPath(Path.BEES_HATE_YOU), withItem("baggie of powdered sugar"));

        try (cleanups) {
          maximize("meat drop");

          assertThat(
              getBoosts(),
              not(hasItem(hasProperty("cmd", startsWith("use 1 baggie of powdered sugar")))));
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
        assertThat(getBoosts(), hasItem(recommends("work boots")));
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
        assertThat(getBoosts(), hasItem(recommends("bonfire flower")));
        assertThat(getBoosts(), hasItem(recommends("frosty button")));
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
        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("absorb ¶303")))); // Knob mushroom
        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("absorb ¶443")))); // beer lens
        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("absorb ¶109")))); // crossbow string
      }
    }

    @Test
    public void canAbsorbEquipmentForEnchants() {
      final var cleanups = new Cleanups(withPath(Path.GELATINOUS_NOOB), withItem("disco mask"));

      try (cleanups) {
        assertTrue(maximize("moxie -tie"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("absorb ¶9")))); // disco mask
      }
    }

    @Test
    public void canAbsorbHelmetTurtleForEnchants() {
      final var cleanups = new Cleanups(withPath(Path.GELATINOUS_NOOB), withItem("helmet turtle"));

      try (cleanups) {
        assertTrue(maximize("muscle -tie"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("absorb ¶3")))); // helmet turtle
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
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bugbear beanie")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "bugbear bungguard")));
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
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "The Jokester's wig")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "The Jokester's gun")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "The Jokester's pants")));
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

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "sweet ninja sword")));
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

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "sweet ninja sword")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "old sweatpants")));
      }
    }

    @Test
    public void equipMostNumberItems() {
      final var cleanups =
          new Cleanups(withEquippableItem("X-37 gun"), withEquippableItem("sweet ninja sword"));

      try (cleanups) {
        maximize("number");

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "X-37 gun")));
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
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "flaming crutch")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "white sword")));
      }
    }

    @Test
    public void clubModifierWorksWithoutTieBreaker() {
      final var cleanups = new Cleanups(withEquippableItem("lawn dart"));

      try (cleanups) {
        assertTrue(maximize("-tie, club"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "lawn dart")));
      }
    }

    @Test
    public void shieldModifierWorksWithoutTieBreaker() {
      final var cleanups = new Cleanups(withEquippableItem("vinyl shield"));

      try (cleanups) {
        assertTrue(maximize("-tie, shield"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "vinyl shield")));
      }
    }

    @Test
    public void swordModifierFavorsSword() {
      final var cleanups =
          new Cleanups(withEquippableItem("sweet ninja sword"), withEquippableItem("spiked femur"));

      try (cleanups) {
        assertTrue(maximize("spooky dmg, sword"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "sweet ninja sword")));
      }
    }
  }

  // effect limits

  @Test
  public void maximizeGiveBestScoreWithEffectsAtNoncombatLimit() {
    final var cleanups =
        new Cleanups(
            withEquippableItem("Space Trip safety headphones"),
            withEquippableItem("Krampus Horn"),
            // get ourselves to -25 combat
            withEffect("Shelter of Shed"),
            withEffect("Smooth Movements"));

    try (cleanups) {
      assertTrue(
          EquipmentManager.canEquip("Space Trip safety headphones"),
          "Cannot equip Space Trip safety headphones");
      assertTrue(EquipmentManager.canEquip("Krampus Horn"), "Cannot equip Krampus Horn");
      assertTrue(
          maximize(
              "cold res,-combat -hat -weapon -offhand -back -shirt -pants -familiar -acc1 -acc2 -acc3"));
      assertEquals(
          25,
          modFor(DoubleModifier.COLD_RESISTANCE) - modFor(DoubleModifier.COMBAT_RATE),
          0.01,
          "Base score is 25");
      assertTrue(maximize("cold res,-combat -acc2 -acc3"));
      assertEquals(
          27,
          modFor(DoubleModifier.COLD_RESISTANCE) - modFor(DoubleModifier.COMBAT_RATE),
          0.01,
          "Maximizing one slot should reach 27");

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY1, "Krampus Horn")));
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
        assertEquals(0, modFor(DoubleModifier.COMBAT_RATE), 0.01);

        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
      }
    }

    @Test
    public void underwaterZonesCheckUnderwaterNegativeCombat() {
      final var cleanups =
          new Cleanups(withLocation("The Ice Hole"), withEquippableItem("Mer-kin sneakmask"));

      try (cleanups) {
        assertEquals(
            Environment.UNDERWATER, AdventureDatabase.getEnvironment(Modifiers.currentLocation));
        assertTrue(maximize("-combat -tie"));

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Mer-kin sneakmask")));
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

        assertEquals(50, modFor(DoubleModifier.ITEMDROP), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "eldritch hat")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "eldritch pants")));
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

        assertEquals(100, modFor(DoubleModifier.ITEMDROP), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Team Avarice cap")));
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

        assertEquals(70, modFor(DoubleModifier.ITEMDROP), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "bounty-hunting rifle")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "bounty-hunting pants")));

        assertTrue(maximize("item, +outfit Eldritch Equipage -tie"));
        assertEquals(65, modFor(DoubleModifier.ITEMDROP), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "eldritch hat")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "bounty-hunting rifle")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "eldritch pants")));

        assertTrue(maximize("item, -outfit Bounty-Hunting Rig -tie"));
        assertEquals(65, modFor(DoubleModifier.ITEMDROP), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "eldritch hat")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "bounty-hunting rifle")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "eldritch pants")));
      }
    }

    @Test
    public void itShouldKeepSlimeOutfit() {
      final var cleanups =
          new Cleanups(
              withItem("Bonestabber"),
              withItem("Scepter of Loathing"),
              withItem("marble medallion"),
              withItem("white earbuds"),
              withEquippableItem("hardened slime hat"),
              withEquippableItem(ItemPool.STAFF_OF_THE_GRAND_FLAMBE),
              withEquippableItem("Stick-Knife of Loathing"),
              withEquippableItem("unwrapped knock-off retro superhero cape"),
              withEquippableItem("Hodgman's disgusting technicolor overcoat"),
              withEquippableItem("hardened slime pants"),
              withEquippableItem("Pocket Square of Loathing"),
              withEquippableItem("perfect Christmas scarf"),
              withEquippableItem("hardened slime belt"));

      try (cleanups) {
        maximizeAny("spooky resistance");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "hardened slime hat")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "hardened slime pants")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY3, "hardened slime belt")));
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
        assertEquals(4, modFor(DoubleModifier.MONSTER_LEVEL), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Brimstone Beret")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "Brimstone Boxers")));
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
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "Half a Purse")));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Hairpiece On Fire")));
        }
      }

      @Test
      public void usesFlaskfullOfHollowWithSmithsness() {
        final var cleanups =
            new Cleanups(
                withItem("Flaskfull of Hollow"),
                withStats(100, 100, 100),
                withEquipped(Slot.PANTS, "Vicar's Tutu"));

        try (cleanups) {
          assertTrue(maximize("muscle -tie"));
          assertThat(
              getBoosts(), hasItem(hasProperty("cmd", startsWith("use 1 Flaskfull of Hollow"))));
        }
      }

      @Test
      public void usesFlaskfullOfHollow() {
        final var cleanups =
            new Cleanups(withItem("Flaskfull of Hollow"), withStats(100, 100, 100));

        try (cleanups) {
          assertTrue(maximize("muscle -tie"));
          assertThat(
              getBoosts(), hasItem(hasProperty("cmd", startsWith("use 1 Flaskfull of Hollow"))));
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
        assertEquals(2, modFor(DoubleModifier.ITEMDROP), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Goggles of Loathing")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "Jeans of Loathing")));
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

          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "pernicious cudgel")));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "grisly shield")));
          assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("use 1 bitter pill"))));
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

          assertThat(
              getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "shield of the Skeleton Lord")));
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

          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Hodgman's porkpie hat")));
          assertThat(
              getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "Hodgman's lobsterskin pants")));
          assertThat(
              getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "Hodgman's garbage sticker")));
          assertThat(getBoosts(), hasItem(recommends("Hodgman's bow tie")));
        }
      }

      @Test
      public void hoboPowerDoesntCountWithoutOffhand() {
        final var cleanups =
            new Cleanups(
                withEquippableItem("Hodgman's bow tie"), withEquippableItem("silver cow creamer"));

        try (cleanups) {
          assertTrue(maximize("meat -tie"));

          assertEquals(30, modFor(DoubleModifier.MEATDROP), 0.01);
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "silver cow creamer")));
          assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.ACCESSORY1))));
          assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.ACCESSORY2))));
          assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.ACCESSORY3))));
        }
      }
    }

    @Test
    void considersMcHugeLargeIfHelpful() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.MCHUGELARGE_DUFFEL_BAG),
              withEquippableItem(ItemPool.MCHUGELARGE_LEFT_POLE),
              withEquippableItem(ItemPool.MCHUGELARGE_RIGHT_POLE),
              withEquippableItem(ItemPool.FLAMING_CARDBOARD_SWORD));

      try (cleanups) {
        assertTrue(maximize("hot dmg -tie"));
        assertEquals(15, modFor(DoubleModifier.HOT_DAMAGE), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.CONTAINER, "McHugeLarge duffel bag")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "McHugeLarge right pole")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "McHugeLarge left pole")));
      }
    }
  }

  @Nested
  class Mutex {
    @Test
    public void equipAtMostOneHalo() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.SHINING_HALO),
              withEquippableItem(ItemPool.TIME_HALO),
              withEquippableItem(ItemPool.TIME_SWORD));

      try (cleanups) {
        assertTrue(maximize("adv, exp"));

        assertEquals(45, modFor(DoubleModifier.ADVENTURES), 0.01);
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY1, "time halo")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.ACCESSORY2))));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.ACCESSORY3))));
      }
    }
  }

  @Nested
  class ReplaceableMutex {
    @Test
    public void suggestBetterFacialExpression() {
      var cleanups =
          new Cleanups(
              withMoxie(100),
              withEffect("Disco Smirk"),
              withSkill("Disco Smirk"),
              withSkill("Quiet Desperation"));

      try (cleanups) {
        assertTrue(maximize("moxie"));

        assertThat(getBoosts(), hasItem(recommendsEffect("Quiet Desperation")));
      }
    }

    @Test
    public void doNotSuggestWorseFacialExpression() {
      var cleanups =
          new Cleanups(
              withMoxie(100),
              withEffect("Quiet Desperation"),
              withSkill("Disco Smirk"),
              withSkill("Quiet Desperation"));

      try (cleanups) {
        assertTrue(maximize("moxie"));

        assertThat(getBoosts(), not(hasItem(recommendsEffect("Disco Smirk"))));
      }
    }

    @Test
    public void suggestBetterShanty() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 400),
              withEffect("Only Dogs Love a Drunken Sailor"),
              withSkill("Only Dogs Love a Drunken Sailor"),
              withSkill("Who's Going to Pay This Drunken Sailor?"));

      try (cleanups) {
        assertTrue(maximize("item"));

        assertThat(
            getBoosts(), hasItem(recommendsEffect("Who's Going to Pay This Drunken Sailor?")));
      }
    }

    @Test
    public void doNotSuggestWorseShanty() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 400),
              withEffect("Who's Going to Pay This Drunken Sailor?"),
              withSkill("Only Dogs Love a Drunken Sailor"),
              withSkill("Who's Going to Pay This Drunken Sailor?"));

      try (cleanups) {
        assertTrue(maximize("item"));

        assertThat(getBoosts(), not(hasItem(recommendsEffect("Only Dogs Love a Drunken Sailor"))));
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
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella broken"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "unbreakable umbrella")));
      }
    }

    @Test
    public void expShouldSuggestUmbrella() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella broken"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "unbreakable umbrella")));
      }
    }

    @Test
    public void expShouldNotSuggestUmbrellaIfBetterInSlot() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("vinyl shield"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "vinyl shield")));
      }
    }

    @Test
    public void chooseForwardFacingUmbrellaToSatisfyShield() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquippableItem("tip jar"),
              withEquipped(Slot.PANTS, "old sweatpants"));

      try (cleanups) {
        assertTrue(maximize("meat, shield"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella forward-facing"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "unbreakable umbrella")));
      }
    }

    @Test
    public void edPieceChoosesFishWithSea() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("The Crown of Ed the Undying"),
              withEquippableItem("star shirt"),
              withEquipped(Slot.PANTS, "old sweatpants"),
              withProperty("edPiece", "puma"));

      try (cleanups) {
        assertTrue(maximize("muscle, sea"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("edpiece fish"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "The Crown of Ed the Undying")));
      }
    }

    @Test
    public void edPieceChoosesBasedOnModesWithoutSea() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("The Crown of Ed the Undying"),
              withEquippableItem("star shirt"),
              withEquipped(Slot.PANTS, "old sweatpants"),
              withProperty("edPiece", "puma"));

      try (cleanups) {
        assertTrue(maximize("muscle"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("edpiece bear"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "The Crown of Ed the Undying")));
      }
    }

    @Test
    public void multipleModeables() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("backup camera"),
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old sweatpants"));
      try (cleanups) {
        assertTrue(maximize("ml, -combat, equip backup camera, equip unbreakable umbrella"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella cocoon"))));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera ml"))));
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
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "star boomerang")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
      }
    }

    @Test
    public void equipUmbrellaOnLeftHandMan() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp, -offhand"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "unbreakable umbrella")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
      }
    }

    @Test
    public void suggestEquippingUmbrellaOnLeftHandMan() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp, -offhand, switch left-hand man"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("familiar Left-Hand Man"))));
        assertThat(
            getBoosts(),
            hasItem(hasProperty("cmd", startsWith("umbrella broken; equip familiar ¶10899"))));
      }
    }

    @Test
    public void suggestEquippingSomethingBetterThanUmbrellaOnLeftHandMan() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("shield of the Skeleton Lord"),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("exp, -offhand, switch left-hand man"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("familiar Left-Hand Man"))));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("equip familiar ¶9890"))));
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
        assertThat(
            getBoosts(),
            hasItem(recommendsSlot(Slot.CONTAINER, "unwrapped knock-off retro superhero cape")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("retrocape vampire hold"))));
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
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "Snow Suit")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("snowsuit goatee"))));
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
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BACKUP_CAMERA)));
        assertThat(getBoosts(), hasItem(recommends("incredibly dense meat gem")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera meat"))));
      }
    }

    @Test
    public void shouldSuggestCameraIfSlotsExcluded() {
      final var cleanups =
          new Cleanups(withEquippableItem("backup camera"), withProperty("backupCameraMode", "ml"));

      try (cleanups) {
        assertTrue(maximize("meat -acc1 -acc2"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY3, "backup camera")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera meat"))));
      }
    }

    @Test
    public void shouldSuggestPullableCameraIfNotRestricted() {
      final var cleanups =
          new Cleanups(withItemInStorage("backup camera"), withProperty("backupCameraMode", "ml"));
      try (cleanups) {
        maximizeAny("meat");
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BACKUP_CAMERA)));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("pull"))));
      }
    }

    @Test
    public void shouldNotSuggestPullableCameraIfRestricted() {
      final var cleanups =
          new Cleanups(
              withItemInStorage("backup camera"),
              withProperty("backupCameraMode", "ml"),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "backup camera"));

      try (cleanups) {
        maximizeAny("meat");
        assertThat(getBoosts(), not(hasItem(recommends(ItemPool.BACKUP_CAMERA))));
      }
    }

    @Test
    public void shouldSuggestReplicaParka() {
      final var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.REPLICA_JURASSIC_PARKA), withSkill(SkillPool.TORSO));

      try (cleanups) {
        assertTrue(maximize("dr"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "replica Jurassic Parka")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void shouldSuggestUsingLedCandleWithJill() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.JILL_OF_ALL_TRADES, 400), withItem(ItemPool.LED_CANDLE));

      try (cleanups) {
        assertTrue(maximize("item"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("ledcandle disco"))));
      }
    }

    @Test
    public void shouldNotSuggestUsingLedCandleWithoutJill() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 400), withItem(ItemPool.LED_CANDLE));

      try (cleanups) {
        assertTrue(maximize("item"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("ledcandle disco")))));
      }
    }

    @Test
    public void equipWithModeForcesThatMode() {
      final var cleanups =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO));

      try (cleanups) {
        // If not asked for, "ml" would pick spikolodon
        assertTrue(maximize("ml, equip Jurassic Parka (ghostasaurus mode)"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "Jurassic Parka")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void equipWithModeAcceptsAliases() {
      final var cleanups =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO));

      try (cleanups) {
        assertTrue(maximize("ml, equip Jurassic Parka (spooky mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void equipWithModeAcceptsBareParenthetical() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old sweatpants"), // Get some ML on
              withProperty("umbrellaState", "broken"));

      try (cleanups) {
        // If not asked for, "ml" would keep the umbrella broken
        assertTrue(maximize("ml, equip unbreakable umbrella (cocoon)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella cocoon"))));
      }
    }

    @Test
    public void equipWithModeAcceptsMultiWordModes() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unwrapped knock-off retro superhero cape"),
              withProperty("retroCapeSuperhero", "vampire"),
              withProperty("retroCapeWashingInstructions", "thrill"));

      try (cleanups) {
        // If not asked for, "hot res" would pick vampire hold
        assertTrue(
            maximize("hot res, equip unwrapped knock-off retro superhero cape (robot kill mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("retrocape robot kill"))));
      }
    }

    @Test
    public void equipWithModeAppliesToReplicaParka() {
      final var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.REPLICA_JURASSIC_PARKA), withSkill(SkillPool.TORSO));

      try (cleanups) {
        assertTrue(maximize("ml, equip replica Jurassic Parka (kachungasaur mode)"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "replica Jurassic Parka")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka kachungasaur"))));
      }
    }

    @Test
    public void equipStillMatchesItemNamesEndingInMode() {
      final var cleanups = new Cleanups(withEquippableItem("Jarlsberg's pan (Cosmic portal mode)"));

      try (cleanups) {
        assertTrue(maximize("spell dmg, equip Jarlsberg's pan (Cosmic portal mode)"));
        assertThat(
            getBoosts(),
            hasItem(recommendsSlot(Slot.OFFHAND, "Jarlsberg's pan (Cosmic portal mode)")));
      }
    }

    @Test
    public void equipStillMatchesItemNamesEndingInMode2() {
      final var cleanups = new Cleanups(withEquippableItem("Boris's Helm (askew)"));

      try (cleanups) {
        assertTrue(maximize("ml, equip Boris's Helm (askew)"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Boris's Helm (askew)")));
      }
    }

    @Test
    public void equipWithUnknownModeErrors() {
      final var cleanups =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO));

      try (cleanups) {
        assertFalse(maximize("ml, equip Jurassic Parka (magical mode)"));
      }
    }

    @Test
    public void shouldErrorWhenModesConflict() {
      final var cleanups =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO));

      try (cleanups) {
        assertFalse(
            maximize(
                "equip Jurassic Parka (ghostasaurus mode), equip Jurassic Parka (kachungasaur mode)"));
      }
    }

    @Test
    public void shouldErrorWhenIndirectConflict() {
      final var cleanups = new Cleanups(withEquippableItem(ItemPool.CROWN_OF_ED));

      try (cleanups) {
        // We currently do not differnate between modes explicitly asked for or not.
        // 'sea' will give way when it's declared after, but not if it's handled first.
        assertFalse(maximize("sea, equip Crown of Ed the Undying (hyena mode)"));
        assertEquals(KoLConstants.MafiaState.ERROR, StaticEntity.getContinuationState());
        assertEquals(
            "Conflicting modes requested for The Crown of Ed the Undying: fish vs hyena",
            KoLmafia.lastMessage);
      }
    }

    @Test
    public void shouldErrorWhenIndirectConflict2() {
      final var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.CROWN_OF_ED),
              withEquippableItem(ItemPool.OLD_SCUBA_TANK));

      try (cleanups) {
        // We currently do not differnate between modes explicitly asked for or not.
        // 'sea' will give way when it's declared after, but not if it's handled first.
        // As such, this will still fail, even if we can breathe underwater
        assertFalse(maximize("sea, equip Crown of Ed the Undying (hyena mode)"));
        assertEquals(KoLConstants.MafiaState.ERROR, StaticEntity.getContinuationState());
        assertEquals(
            "Conflicting modes requested for The Crown of Ed the Undying: fish vs hyena",
            KoLmafia.lastMessage);
      }
    }

    @Test
    public void softRequestShouldGiveWayWhenIndirectConflict() {
      final var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.CROWN_OF_ED),
              withEquippableItem(ItemPool.OLD_SCUBA_TANK));

      try (cleanups) {
        // 'sea' cannot override an explict mode. We equip a scuba tank to sastify 'sea'
        assertTrue(maximize("equip Crown of Ed the Undying (hyena mode), sea"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("edpiece hyena"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "The Crown of Ed the Undying")));
      }
    }

    @Test
    public void doesNotChangeModeOfItemInExcludedSlot() {
      final var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, "backup camera"),
              withProperty("backupCameraMode", "init"));

      try (cleanups) {
        assertTrue(maximize("meat, -acc1"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("backupcamera")))));
      }
    }

    @Test
    public void doesNotChangeModeOfUmbrellaInExcludedSlot() {
      final var cleanups =
          new Cleanups(
              withEquipped(Slot.OFFHAND, "unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("ml, -offhand, switch left-hand man"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("umbrella")))));
      }
    }

    @Test
    public void doesChangeModeOfUmbrellaInNonExcludedSlot() {
      final var cleanups =
          new Cleanups(
              withEquipped(Slot.OFFHAND, "unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withProperty("umbrellaState", "cocoon"));

      try (cleanups) {
        assertTrue(maximize("ml"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella"))));
      }
    }

    @Test
    public void doesChangeModeOfUmbrellaInNonExcludedSlot2() {
      final var cleanups =
          new Cleanups(
              withEquipped(Slot.OFFHAND, "unbreakable umbrella"),
              withEquipped(Slot.ACCESSORY1, "backup camera"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withProperty("umbrellaState", "cocoon"),
              withProperty("backupCameraMode", "meat"));

      try (cleanups) {
        assertTrue(maximize("ml, -offhand"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("umbrella")))));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera"))));
      }
    }

    @Test
    public void doesNotEquipExcludedMode() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("backup camera"), withProperty("backupCameraMode", "init"));

      try (cleanups) {
        assertTrue(maximize("meat, -tie, -equip backup camera (meat)"));
        assertThat(getBoosts(), not(hasItem(recommends(ItemPool.BACKUP_CAMERA))));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("backupcamera")))));
      }
    }

    @Test
    public void bonusModeWinsOverNaturalValue() {
      final var cleanups =
          new Cleanups(withEquippableItem("Backup camera"), withProperty("backupCameraMode", "ml"));

      try (cleanups) {
        assertTrue(maximize("init, 150 bonus Backup camera (meat)"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BACKUP_CAMERA)));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera meat"))));
      }
    }

    @Test
    public void bonusWithModeAcceptsAliases() {
      final var cleanups =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO));

      try (cleanups) {
        assertTrue(maximize("ml, 100 bonus Jurassic Parka (spooky mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void bonusWithModeAcceptsModeWithoutTheWordMode() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old sweatpants"),
              withProperty("umbrellaState", "broken"));

      try (cleanups) {
        assertTrue(maximize("ml, 100 bonus unbreakable umbrella (cocoon)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella cocoon"))));
      }
    }

    @Test
    public void bonusWithModeAcceptsMultiWordModes() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("unwrapped knock-off retro superhero cape"),
              withProperty("retroCapeSuperhero", "vampire"),
              withProperty("retroCapeWashingInstructions", "thrill"));

      try (cleanups) {
        assertTrue(
            maximize(
                "hot res, 100 bonus unwrapped knock-off retro superhero cape (robot kill mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("retrocape robot kill"))));
      }
    }

    @Test
    public void bonusStillMatchesItemNamesEndingInMode() {
      final var cleanups = new Cleanups(withEquippableItem("Jarlsberg's pan (Cosmic portal mode)"));

      try (cleanups) {
        assertTrue(maximize("spell dmg, 100 bonus Jarlsberg's pan (Cosmic portal mode)"));
        assertThat(
            getBoosts(),
            hasItem(recommendsSlot(Slot.OFFHAND, "Jarlsberg's pan (Cosmic portal mode)")));
      }
    }

    @Test
    public void bonusStillMatchesItemNamesEndingInMode2() {
      final var cleanups = new Cleanups(withEquippableItem("Boris's Helm (askew)"));

      try (cleanups) {
        assertTrue(maximize("ml, 100 bonus Boris's Helm (askew)"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Boris's Helm (askew)")));
      }
    }

    @Test
    public void bonusWithUnknownModeErrors() {
      final var cleanups =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO));

      try (cleanups) {
        assertFalse(maximize("ml, 100 bonus Jurassic Parka (magical mode)"));
      }
    }

    @Test
    public void dontSwitchModeForZeroScoreWithoutTiebreaker() {
      final var cleanups =
          new Cleanups(
              withEquipped(Slot.SHIRT, ItemPool.JURASSIC_PARKA),
              withSkill(SkillPool.TORSO),
              withProperty("parkaMode", "kachungasaur"));

      try (cleanups) {
        assertTrue(maximize("+adv, -tie"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("parka")))));
      }
    }

    @Test
    public void higherBonusModeWins() {
      final var cleanups =
          new Cleanups(withEquippableItem("backup camera"), withProperty("backupCameraMode", "ml"));

      try (cleanups) {
        // 120 vs 110
        assertTrue(maximize("70 bonus backup camera (meat), 10 bonus backup camera (init)"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BACKUP_CAMERA)));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera meat"))));
      }
    }

    @Test
    public void equipModeWinsOverBonusMode() {
      final var cleanups =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO));

      try (cleanups) {
        assertTrue(
            maximize(
                "equip Jurassic Parka (ghostasaurus mode), 100 bonus Jurassic Parka (kachungasaur mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void bonusModeNeverOverridesEquipModeRegardlessOfOrder() {
      final var cleanups =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO));

      try (cleanups) {
        assertTrue(
            maximize(
                "100 bonus Jurassic Parka (kachungasaur mode), equip Jurassic Parka (ghostasaurus mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void bonusModeNeverOverridesImplicitForce() {
      final var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.CROWN_OF_ED),
              withEquippableItem(ItemPool.OLD_SCUBA_TANK));

      try (cleanups) {
        // 'sea' still forces fish mode
        assertTrue(maximize("sea, 100 bonus Crown of Ed the Undying (hyena mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("edpiece fish"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "The Crown of Ed the Undying")));
      }
    }

    @Test
    public void bonusScoreAndModeableScoreAddsTogether() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("backup camera"),
              withEquippableItem("incredibly dense meat gem"),
              withProperty("backupCameraMode", "init"));

      try (cleanups) {
        // The camera's 50 - 10 + 25 = 65 beats the gem's 60
        assertTrue(
            maximize("meat -acc1 -acc2, -10 bonus backup camera, 25 bonus backup camera (meat)"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY3, "backup camera")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera meat"))));
        assertThat(getBoosts(), not(hasItem(recommends("incredibly dense meat gem"))));
        assertEquals(165, Maximizer.best.getScore());
      }
    }

    @Test
    public void secondBonusForSameItemOverwritesFirst() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("backup camera"), withEquippableItem("incredibly dense meat gem"));

      try (cleanups) {
        // The later bonus replaces the earlier one
        assertTrue(
            maximize(
                "meat -acc1 -acc2, 15 bonus backup camera (meat), 5 bonus backup camera (meat)"));
        assertThat(
            getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY3, "incredibly dense meat gem")));
        assertThat(getBoosts(), not(hasItem(recommends(ItemPool.BACKUP_CAMERA))));
      }
    }

    @Test
    public void bonusModeWinsOverNaturalModeSelection() {
      final var cleanups =
          new Cleanups(withEquippableItem("backup camera"), withProperty("backupCameraMode", "ml"));

      try (cleanups) {
        assertTrue(maximize("ml, 100 bonus backup camera (meat)"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BACKUP_CAMERA)));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera meat"))));
      }
    }

    @Test
    public void doesSwitchAwayFromBadBonusForMode() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("backup camera"), withProperty("backupCameraMode", "init"));

      try (cleanups) {
        assertTrue(maximize("+equip backup camera, -150 bonus backup camera (init)"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BACKUP_CAMERA)));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera"))));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("backupcamera init")))));
      }
    }
  }

  @Nested
  class GarbageTote {
    @Test
    public void shouldSuggestEquippingGarbageToteItem1() {
      final var cleanups =
          new Cleanups(withItem(ItemPool.GARBAGE_TOTE), withItem(ItemPool.TINSEL_TIGHTS));

      try (cleanups) {
        assertTrue(maximize("monster level"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "tinsel tights")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("equip pants ¶9693"))));
      }
    }

    @Test
    public void shouldSuggestEquippingGarbageToteItem2() {
      final var cleanups =
          new Cleanups(
              withItem(ItemPool.REPLICA_GARBAGE_TOTE),
              withItem(ItemPool.REPLICA_HAIKU_KATANA),
              withItem(ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 5),
              withSkill("Double-Fisted Skull Smashing"));

      try (cleanups) {
        assertTrue(maximize("weapon damage percent"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "broken champagne bottle")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("equip off-hand ¶9692"))));
      }
    }

    @Test
    public void shouldFoldUnusedChampagneBottle() {
      final var cleanups =
          new Cleanups(
              withItem(ItemPool.REPLICA_GARBAGE_TOTE),
              withItem(ItemPool.REPLICA_HAIKU_KATANA),
              withItem(ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 0),
              withProperty("_garbageItemChanged", false),
              withSkill("Double-Fisted Skull Smashing"));

      try (cleanups) {
        assertTrue(maximize("weapon damage percent"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "broken champagne bottle")));
        assertThat(
            getBoosts(),
            hasItem(hasProperty("cmd", startsWith("fold ¶9692;equip off-hand ¶9692"))));
      }
    }

    @Test
    public void shouldSuggestFoldingGarbageToteItem() {
      final var cleanups =
          new Cleanups(withItem(ItemPool.GARBAGE_TOTE), withItem(ItemPool.TINSEL_TIGHTS));

      try (cleanups) {
        assertTrue(maximize("weapon damage percent"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "broken champagne bottle")));
        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("fold ¶9692;equip weapon ¶9692"))));
      }
    }

    @Test
    public void shouldNotSuggestUsingGarbageToteItem() {
      final var cleanups = new Cleanups(withItem(ItemPool.TINSEL_TIGHTS));

      try (cleanups) {
        assertTrue(maximize("weapon damage percent"));
        assertThat(
            getBoosts(),
            not(hasItem(hasProperty("cmd", startsWith("fold ¶9692;equip weapon ¶9692")))));
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
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("horsery dark"))));
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
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("horsery dark")))));
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
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("horsery dark")))));
      }
    }
  }

  @Nested
  public class Familiars {
    @Test
    public void leftHandManEquipsItem() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.LEFT_HAND),
              withEquippableItem(ItemPool.WICKER_SHIELD, 2),
              withItem(ItemPool.STUFFED_CHEST) // equipment with no enchant to test modifiers crash
              );

      try (cleanups) {
        assertTrue(maximize("moxie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "wicker shield")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "wicker shield")));
      }
    }

    @Test
    public void leftHandManConsidersRequestedItems() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("big stick"), // 2-handed weapon
              withEquippableItem("bread basket"),
              withEquippableItem("cyborg doll"),
              withFamiliar(FamiliarPool.LEFT_HAND));

      try (cleanups) {
        assertTrue(maximize("equip bread basket -familiar"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "bread basket")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.FAMILIAR))));

        assertTrue(maximize("equip big stick, equip bread basket"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "big stick")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "bread basket")));

        assertTrue(maximize("1000 bonus bread basket -offhand -tie"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "bread basket")));

        assertTrue(maximize("equip bread basket +equip cyborg doll"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND)));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR)));
        assertThat(getBoosts(), hasItem(recommends("cyborg doll")));
        assertThat(getBoosts(), hasItem(recommends("bread basket")));
      }
    }

    @Test
    public void switchLeftHandManConsidersRequestedItems() {
      final var cleanups =
          new Cleanups(
              withEquippableItem("big stick"), // 2-handed weapon
              withEquippableItem("bread basket"),
              withEquippableItem("cyborg doll"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND));

      try (cleanups) {
        assertTrue(maximize("equip bread basket -familiar +switch left-hand man"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "bread basket")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.FAMILIAR))));

        assertTrue(maximize("equip big stick, equip bread basket +switch left-hand man"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "big stick")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "bread basket")));

        assertTrue(maximize("1000 bonus bread basket -offhand -tie +switch left-hand man"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "bread basket")));

        assertTrue(maximize("equip bread basket +equip cyborg doll +switch left-hand man"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND)));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR)));
        assertThat(getBoosts(), hasItem(recommends("cyborg doll")));
        assertThat(getBoosts(), hasItem(recommends("bread basket")));
      }
    }

    @Test
    public void switchFamiliarConsidersGenericItems() {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withItem(ItemPool.SOLID_SHIFTING_TIME_WEIRDNESS) // 4 adv with any familiar
              );

      try (cleanups) {
        assertTrue(maximize("adv -tie +switch mosquito"));
        assertThat(
            getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "solid shifting time weirdness")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("familiar Mosquito"))));
      }
    }

    @Test
    public void switchMultipleFamiliarsConsidersMultipleItems() {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.TRICK_TOT),
              withFamiliarInTerrarium(FamiliarPool.HAND),
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withItem(ItemPool.TRICK_TOT_UNICORN), // 5 adv with tot
              withItem(ItemPool.TRICK_TOT_CANDY), // 0 adv
              withItem(ItemPool.TIME_SWORD), // 3 adv with hand
              withItem(ItemPool.SOLID_SHIFTING_TIME_WEIRDNESS) // 4 adv with any familiar
              );

      try (cleanups) {
        assertTrue(
            maximize(
                "adv -weapon -offhand -tie +switch tot +switch disembodied hand +switch mosquito"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "li'l unicorn costume")));
        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("familiar Trick-or-Treating Tot"))));
      }
    }

    @Test
    public void switchMultipleFamiliarsWithFoldable() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.MOSQUITO),
              withFamiliarInTerrarium(FamiliarPool.BADGER),
              withFamiliarInTerrarium(FamiliarPool.PURSE_RAT, 400),
              withItem(ItemPool.LIARS_PANTS));

      try (cleanups) {
        assertTrue(maximize("ml +switch badger +switch purse rat"));
        assertThat(
            getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "flaming familiar doppelgänger")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("familiar Purse Rat"))));
      }
    }
  }

  @Nested
  public class Uniques {
    @Test
    public void suggestsBestNonStackingWatchForAdventures() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Counterclockwise Watch"), // 10, watch
              withEquippableItem("grandfather watch"), // 6, also a watch
              withEquippableItem("plexiglass pocketwatch"), // 3, stacks
              withEquippableItem("gold wedding ring") // 1
              );

      try (cleanups) {
        assertTrue(maximize("adv"));
        assertEquals(54, modFor(DoubleModifier.ADVENTURES), 0.01);
        assertThat(getBoosts(), hasItem(recommends("Counterclockwise Watch")));
        assertThat(getBoosts(), hasItem(recommends("plexiglass pocketwatch")));
        assertThat(getBoosts(), hasItem(recommends("gold wedding ring")));
      }
    }

    @Test
    public void suggestsNoAdventureGearInSlowAndSteady() {
      var cleanups =
          new Cleanups(
              withPath(Path.SLOW_AND_STEADY),
              withEquippableItem("Counterclockwise Watch"),
              withEquippableItem("gold wedding ring"));

      try (cleanups) {
        assertTrue(maximize("adv"));
        assertEquals(100, modFor(DoubleModifier.ADVENTURES), 0.01);
        assertThat(getBoosts(), not(hasItem(recommends("Counterclockwise Watch"))));
        assertThat(getBoosts(), not(hasItem(recommends("gold wedding ring"))));
      }
    }

    @Test
    public void watchesDontStackOutsideAdventuresEither() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.SASQ_WATCH), // 3, watch
              withEquippableItem("Crimbolex watch") // 5, also a watch
              );

      try (cleanups) {
        assertTrue(maximize("fites"));
        assertEquals(15, modFor(DoubleModifier.PVP_FIGHTS), 0.01);
        assertThat(getBoosts(), hasItem(recommends("Crimbolex watch")));
        assertThat(getBoosts(), not(hasItem(recommends(ItemPool.SASQ_WATCH))));
      }
    }

    @Test
    public void surgeonosityItemsStackOutsideSurgeonosity() {
      var cleanups = withEquippableItem("surgical mask", 3);

      try (cleanups) {
        assertTrue(maximize("mp, -tie"));
        assertEquals(120, modFor(DoubleModifier.MP), 0.01);
        assertThat(
            getBoosts().stream()
                .filter(x -> x.isEquipment() && "surgical mask".equals(x.getItem().getName()))
                .count(),
            equalTo(3L));
        assertEquals(1, modFor(BitmapModifier.SURGEONOSITY), 0.01);
      }
    }

    @Test
    public void clownosityItemsStackOutsideClownosity() {
      var cleanups = withEquippableItem("clownskin belt", 3);

      try (cleanups) {
        assertTrue(maximize("mp, -tie"));
        assertEquals(45, modFor(DoubleModifier.MP), 0.01);
        assertThat(
            getBoosts().stream()
                .filter(x -> x.isEquipment() && "clownskin belt".equals(x.getItem().getName()))
                .count(),
            equalTo(3L));
        assertEquals(50, modFor(BitmapModifier.CLOWNINESS), 0.01);
      }
    }

    @Test
    public void raveosityItemsStackOutsideRaveosity() {
      var cleanups = withEquippableItem("blue glowstick", 3);

      try (cleanups) {
        assertTrue(maximize("mp, -tie"));
        assertEquals(15, modFor(DoubleModifier.MP), 0.01);
        assertThat(
            getBoosts().stream()
                .filter(x -> x.isEquipment() && "blue glowstick".equals(x.getItem().getName()))
                .count(),
            equalTo(3L));
        assertEquals(1, modFor(BitmapModifier.RAVEOSITY), 0.01);
      }
    }

    @Test
    public void brimstoneItemsStackOutsideBrimstone() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Brimstone Bludgeon", 3),
              withSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING));

      try (cleanups) {
        assertTrue(maximize("muscle, -tie"));
        assertEquals(100, modFor(DoubleModifier.MUS_PCT), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "Brimstone Bludgeon")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "Brimstone Bludgeon")));
        assertEquals(1, modFor(BitmapModifier.BRIMSTONE), 0.01);
      }
    }

    @Test
    public void cloathingItemsStackOutsideCloathing() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Stick-Knife of Loathing", 3),
              withSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING));

      try (cleanups) {
        assertTrue(maximize("spell dmg, -tie"));
        assertEquals(400, modFor(DoubleModifier.SPELL_DAMAGE_PCT), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "Stick-Knife of Loathing")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "Stick-Knife of Loathing")));
        assertEquals(1, modFor(BitmapModifier.CLOATHING), 0.01);
      }
    }
  }

  @Nested
  public class Foldables {
    @Test
    public void forcedFoldablePreventsOtherSlots() {
      var cleanups = withEquippableItem(ItemPool.ICE_SICKLE);

      try (cleanups) {
        assertTrue(maximize("ml, +equip ice baby"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "ice baby")));
      }
    }

    @Test
    public void prefersFoldableInSlotWithHigherScore() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.ORIGAMI_MAGAZINE),
              withSkill(SkillPool.TORSO),
              withFamiliar(FamiliarPool.GHUOL_WHELP));

      try (cleanups) {
        assertTrue(maximize("meat, sleaze dmg, -tie"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "origami pasties")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.FAMILIAR))));
      }
    }

    @Test
    public void singleFoldSourceProducesRequestedForm() {
      var cleanups = new Cleanups(withItem(ItemPool.MAKESHIFT_CRANE), withStats(0, 75, 35));

      try (cleanups) {
        assertTrue(maximize("+equip makeshift cape"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.CONTAINER, "makeshift cape")));
      }
    }
  }

  @Nested
  public class Booleans {
    @Nested
    public class NeverFumble {
      @Test
      public void unableToNeverFumbleMarksAsFailed() {
        assertFalse(maximize("never fumble"));
      }

      @Test
      public void requiresConditionToSucceed() {
        var cleanups =
            new Cleanups(
                withEquippableItem("aerogel anvil"),
                withEquippableItem("Baron von Ratsworth's monocle"),
                withEquippableItem("observational glasses"),
                withEquippableItem("ring of the Skeleton Lord"));

        try (cleanups) {
          assertTrue(maximize("never fumble"));
          assertThat(getBoosts(), hasItem(recommends("aerogel anvil")));
          assertThat(getBoosts(), hasItem(recommends("ring of the Skeleton Lord")));
          assertThat(getBoosts(), hasItem(recommends("Baron von Ratsworth's monocle")));
        }
      }
    }

    @Nested
    public class Pirate {
      @Test
      public void unableToPirateMarksAsFailed() {
        assertFalse(maximize("pirate"));
      }

      @Test
      public void fledgesOrOutfitBothCount() {
        var cleanups =
            new Cleanups(
                withEquippableItem("eyepatch"),
                withEquippableItem("swashbuckling pants"),
                withEquippableItem("Pantsgiving"),
                withEquippableItem("stuffed shoulder parrot"),
                withEquippableItem("pirate fledges"),
                withEquippableItem("moustache sock"),
                withEquippableItem("tube sock"),
                withEquippableItem("mirrored aviator shades"));

        try (cleanups) {
          assertTrue(maximize("pirate, meat, -tie"));
          assertThat(getBoosts(), hasItem(recommends("pirate fledges")));
          assertThat(getBoosts(), hasItem(recommends("Pantsgiving")));

          assertTrue(maximize("pirate, moxie, -tie"));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "eyepatch")));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "swashbuckling pants")));
          assertThat(getBoosts(), hasItem(recommends("stuffed shoulder parrot")));
          assertThat(getBoosts(), hasItem(recommends("moustache sock")));
          assertThat(getBoosts(), hasItem(recommends("tube sock")));
        }
      }
    }

    @Nested
    public class Sea {
      @Test
      public void unableToAdventureInSeaMarksAsFailed() {
        assertFalse(maximize("sea"));
      }

      @Test
      public void prefersSeaGearToHigherScore() {
        var cleanups =
            new Cleanups(
                withFamiliar(FamiliarPool.MOSQUITO),
                withEquippableItem(ItemPool.DAS_BOOT),
                withEquippableItem("Mer-kin scholar mask"),
                withEquippableItem("Lens of Violence"),
                withEquippableItem("old SCUBA tank"));

        try (cleanups) {
          assertTrue(maximize("item, sea"));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Mer-kin scholar mask")));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "das boot")));
          assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.CONTAINER))));
        }
      }
    }
  }

  @Nested
  public class Chefstaves {
    @Test
    public void cantEquipCheffstaffsOnLeftHandMan() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withSkill(SkillPool.SPIRIT_OF_RIGATONI));

      try (cleanups) {
        assertTrue(maximize("spell dmg, -weapon"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.FAMILIAR))));
      }
    }

    @Test
    public void mustEquipSauceGloveForChefstaff() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withEquippableItem("special sauce glove"),
              withClass(AscensionClass.SAUCEROR));

      try (cleanups) {
        assertTrue(maximize("spell dmg, -tie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "Staff of Kitchen Royalty")));
        assertThat(getBoosts(), hasItem(recommends("special sauce glove")));
      }
    }

    @Test
    public void cannotUseSauceGloveIfNotSauceror() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withEquippableItem("special sauce glove"),
              withClass(AscensionClass.SEAL_CLUBBER));

      try (cleanups) {
        assertTrue(maximize("spell dmg, -tie"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
      }
    }

    @Test
    public void canUseSkillToEquipChefstaves() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withSkill(SkillPool.SPIRIT_OF_RIGATONI));

      try (cleanups) {
        assertTrue(maximize("spell dmg, -tie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "Staff of Kitchen Royalty")));
      }
    }
  }

  @Nested
  public class VampireVintnerWine {
    @Test
    public void doesNotSuggestVintnerWineIfUnavailable() {
      var cleanups = new Cleanups(withItem(ItemPool.VAMPIRE_VINTNER_WINE, 0));

      try (cleanups) {
        assertTrue(maximize("Item Drop"));
        assertThat(
            getBoosts(),
            not(hasItem(hasProperty("cmd", startsWith("drink 1 1950 Vampire Vintner wine")))));
      }
    }

    @Test
    public void doesSuggestVintnerWineIfAvailableWithCorrectEffect() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VAMPIRE_VINTNER_WINE, 1),
              withProperty("vintnerWineEffect", "Wine-Hot"),
              withProperty("vintnerWineLevel", 12));

      try (cleanups) {
        assertTrue(maximize("Item Drop"));
        assertThat(
            getBoosts(),
            hasItem(hasProperty("cmd", startsWith("drink 1 1950 Vampire Vintner wine"))));
      }
    }

    @Test
    public void doesNotSuggestVintnerWineIfAvailableWithWrongEffect() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VAMPIRE_VINTNER_WINE, 1),
              withProperty("vintnerWineEffect", "Wine-Hot"),
              withProperty("vintnerWineLevel", 12));

      try (cleanups) {
        assertTrue(maximize("Monster Level"));
        assertThat(
            getBoosts(),
            not(hasItem(hasProperty("cmd", startsWith("drink 1 1950 Vampire Vintner wine")))));
      }
    }
  }

  @Nested
  class Skills {
    @Test
    public void suggestsSkillsIfRelevant() {
      var cleanups = new Cleanups(withSkill(SkillPool.SCARYSAUCE));

      try (cleanups) {
        assertTrue(maximize("cold res"));
        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("cast 1 Scarysauce ^ Scarysauce"))));
        assertThat(
            getBoosts(),
            not(hasItem(hasProperty("cmd", startsWith("cast 1 Scarysauce ^ Scariersauce")))));
      }
    }

    @Test
    public void suggestsSpecialEffectsFromSkillsIfHaveEquipment() {
      var cleanups =
          new Cleanups(
              withSkill(SkillPool.SCARYSAUCE), withEquippableItem(ItemPool.VELOUR_VISCOMETER));

      try (cleanups) {
        assertTrue(maximize("cold res"));
        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("cast 1 Scarysauce ^ Scarysauce"))));
        assertThat(
            getBoosts(),
            hasItem(hasProperty("cmd", startsWith("cast 1 Scarysauce ^ Scariersauce"))));
      }
    }

    @Test
    public void suggestsSpiceHazeForNonPastamancer() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.ACCORDION_THIEF), withSkill(SkillPool.BIND_SPICE_GHOST));

      try (cleanups) {
        maximize("item");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("cast 1 Bind Spice Ghost"))));
      }
    }

    @Test
    public void doesNotSuggestSpiceHazeForPastamancer() {
      var cleanups =
          new Cleanups(
              withClass(AscensionClass.PASTAMANCER), withSkill(SkillPool.BIND_SPICE_GHOST));

      try (cleanups) {
        maximize("item");
        assertThat(
            getBoosts(), not(hasItem(hasProperty("cmd", startsWith("cast 1 Bind Spice Ghost")))));
      }
    }
  }

  @Nested
  class BirdOfTheDay {
    @Test
    public void suggestsBirdIfRelevant() {
      var cleanups =
          new Cleanups(
              withProperty("_canSeekBirds", true),
              withProperty("_birdOfTheDay", "Filthy Smiling Pine Parrot"),
              withProperty(
                  "_birdOfTheDayMods",
                  "Mysticality Percent: +75, Stench Resistance: +2, Experience: +2, MP Regen Min: 10, MP Regen Max: 20"),
              withProperty("yourFavoriteBird", "Southern Clandestine Fig Chachalaca"),
              withProperty(
                  "yourFavoriteBirdMods",
                  "Stench Resistance: +2, Combat Rate: -9, MP Regen Min: 10, MP Regen Max: 20"),
              withSkill(SkillPool.SEEK_OUT_A_BIRD),
              withSkill(SkillPool.VISIT_YOUR_FAVORITE_BIRD),
              withOverrideModifiers(
                  ModifierType.EFFECT,
                  2551,
                  "Mysticality Percent: +75, Stench Resistance: +2, Experience: +2, MP Regen Min: 10, MP Regen Max: 20"),
              withOverrideModifiers(
                  ModifierType.EFFECT,
                  2552,
                  "Stench Resistance: +2, Combat Rate: -9, MP Regen Min: 10, MP Regen Max: 20"));

      try (cleanups) {
        assertTrue(maximize("mp regen"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("cast 1 Seek out a Bird"))));
        assertThat(
            getBoosts(),
            hasItem(hasProperty("cmd", startsWith("cast 1 Visit your Favorite Bird"))));
      }
    }
  }

  @Nested
  class PassiveDamage {
    @Test
    public void suggestsPassiveDamage() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.HIPPY_PROTEST_BUTTON),
              withEquippableItem(ItemPool.BOTTLE_OPENER_BELT_BUCKLE),
              withEquippableItem(ItemPool.HOT_PLATE),
              withEquippableItem(ItemPool.SHINY_RING),
              withEquippableItem(ItemPool.BEJEWELED_PLEDGE_PIN),
              withEquippableItem(ItemPool.GROLL_DOLL),
              withEquippableItem(ItemPool.ANT_RAKE),
              withEquippableItem(ItemPool.SERRATED_PROBOSCIS_EXTENSION),
              withSkill(SkillPool.JALAPENO_SAUCESPHERE),
              withItem(ItemPool.CHEAP_CIGAR_BUTT),
              withFamiliar(FamiliarPool.MOSQUITO));

      try (cleanups) {
        assertTrue(maximize("passive dmg"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "hot plate")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "ant rake")));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.HIPPY_PROTEST_BUTTON)));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BOTTLE_OPENER_BELT_BUCKLE)));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY3, "Groll doll")));
        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("cast 1 Jalapeño Saucesphere"))));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("use 1 cheap cigar butt"))));
      }
    }

    @Test
    public void suggestsUnderwaterPassiveDamageUnderwater() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.EELSKIN_HAT),
              withEquippableItem(ItemPool.EELSKIN_PANTS),
              withEquippableItem(ItemPool.EELSKIN_SHIELD),
              withLocation("The Ice Hole"));

      try (cleanups) {
        assertTrue(maximize("passive dmg -tie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "eelskin hat")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "eelskin pants")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "eelskin shield")));
      }
    }

    @Test
    public void doesNotSuggestUnderwaterPassiveDamageIfNotUnderwater() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.EELSKIN_HAT),
              withEquippableItem(ItemPool.EELSKIN_PANTS),
              withEquippableItem(ItemPool.EELSKIN_SHIELD),
              withLocation("Noob Cave"));

      try (cleanups) {
        assertTrue(maximize("passive dmg -tie"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.PANTS))));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
      }
    }
  }

  @Nested
  class Standard {
    private final Cleanups withWitchess =
        new Cleanups(
            withCampgroundItem(ItemPool.WITCHESS_SET), withProperty("puzzleChampBonus", 20));

    @Test
    public void suggestsWitchessIfOwned() {
      var cleanups = new Cleanups(withWitchess);

      try (cleanups) {
        assertTrue(maximize("familiar weight"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("witchess"))));
      }
    }

    @Test
    public void doesNotSuggestWitchessWhenOutOfStandard() {
      var cleanups =
          new Cleanups(
              withPath(Path.STANDARD),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Witchess Set"),
              withWitchess);

      try (cleanups) {
        assertTrue(maximize("familiar weight"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("witchess")))));
      }
    }

    @Test
    public void suggestsWitchessWhenOutOfStandardForLegacyOfLoathing() {
      var cleanups =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withRestricted(true),
              withProperty("replicaWitchessSetAvailable", true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Witchess Set"),
              withWitchess);

      try (cleanups) {
        assertTrue(maximize("familiar weight"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("witchess"))));
      }
    }
  }

  @Nested
  class GreatestAmericanPants {
    @Test
    public void suggestsGap() {
      var cleanups = withEquipped(Slot.PANTS, ItemPool.GREAT_PANTS);

      try (cleanups) {
        assertTrue(maximize("item"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("gap vision"))));
      }
    }

    @Test
    public void suggestsReplicaGap() {
      var cleanups =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withEquipped(Slot.PANTS, ItemPool.REPLICA_GREAT_PANTS));

      try (cleanups) {
        assertTrue(maximize("hot res"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("gap structure"))));
      }
    }
  }

  @Nested
  class CardSleeve {
    @Test
    public void suggestCardSleeveSlot() {
      var cleanups =
          new Cleanups(
              withEquippableItem("card sleeve"),
              withEquippableItem("sturdy cane"),
              withEquippableItem("Alice's Army Foil Lanceman"));

      try (cleanups) {
        assertTrue(maximize("PvP Fights"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "card sleeve")));
        assertThat(
            getBoosts(), hasItem(recommendsSlot(Slot.CARDSLEEVE, "Alice's Army Foil Lanceman")));
      }
    }

    @Test
    public void canReplaceCardSleeveNonDestructively() {
      // A card sleeve item can be switched non-destructively, we can consider it
      // This is unlike the folder holder, which destroys the replaced item
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.CARD_SLEEVE), withEquippableItem("Alice's Army Sniper"));

      try (cleanups) {
        maximizeAny("+equip card sleeve, Weapon Damage");
        assertFalse(Maximizer.best.failed);
        assertThat(getBoosts(), hasItem(recommends(ItemPool.CARD_SLEEVE)));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.CARDSLEEVE)));
      }
    }
  }

  @Nested
  class FolderHolder {
    @Test
    public void doesNotRecommendReplacingFolderDestructively() {
      // Folders are destroyed when the slot is replaced, it's not handled by default
      // This is unlike the card sleeve, which doesn't destroy the replaced item
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.FOLDER_HOLDER), withEquippableItem("Folder (red)"));

      try (cleanups) {
        maximizeAny("+equip over-the-shoulder folder holder, muscle");
        assertFalse(Maximizer.best.failed);
        assertThat(getBoosts(), hasItem(recommends(ItemPool.FOLDER_HOLDER)));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.FOLDER1))));
      }
    }
  }

  @Nested
  class StickerWeapon {
    @Test
    public void doesNotRecommendReplacingStickerDestructively() {
      // Stickers are destroyed (peeled off) when the slot is replaced, it's not handled by
      // default. This is unlike the card sleeve, which doesn't destroy the replaced item
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.STICKER_SWORD),
              withEquippableItem(ItemPool.UNICORN_STICKER));

      try (cleanups) {
        maximizeAny("+equip scratch 'n' sniff sword, muscle");
        assertFalse(Maximizer.best.failed);
        assertThat(getBoosts(), hasItem(recommends(ItemPool.STICKER_SWORD)));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.STICKER1))));
      }
    }
  }

  @Nested
  class CowboyBoots {
    @Test
    public void doesNotRecommendReplacingBootDecorationsDestructively() {
      // Boot skins and spurs are destroyed when the slot is replaced, it's not handled by
      // default. This is unlike the card sleeve, which doesn't destroy the replaced item
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.COWBOY_BOOTS),
              withEquippableItem(ItemPool.MOUNTAIN_SKIN),
              withEquippableItem(ItemPool.QUICKSILVER_SPURS));

      try (cleanups) {
        maximizeAny("+equip your cowboy boots, muscle");
        assertFalse(Maximizer.best.failed);
        assertThat(getBoosts(), hasItem(recommends(ItemPool.COWBOY_BOOTS)));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.BOOTSKIN))));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.BOOTSPUR))));
      }
    }
  }

  @Nested
  class LegacyOfLoathing {
    @Test
    public void shouldNotSuggestPullingEquipmentInLegacyOfLoathing() {
      var cleanups =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withItemInStorage(ItemPool.POWERFUL_GLOVE),
              withInteractivity(false));

      try (cleanups) {
        maximizeAny("hp");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("pull")))));
      }
    }

    @Test
    public void shouldNotSuggestPullingFreePullsInLegacyOfLoathingHardcore() {
      var cleanups =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withItemInFreepulls(ItemPool.RETROSPECS),
              withHardcore(),
              withInteractivity(false));

      try (cleanups) {
        maximizeAny("mus");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("free pull")))));
      }
    }
  }

  @Nested
  class Mcd {
    @Test
    public void doesNotSuggestMcdIfSignless() {
      var cleanups = withSign(ZodiacSign.NONE);

      try (cleanups) {
        maximize("ml");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("mcd")))));
      }
    }

    @Test
    public void suggestsMcdWhenBoostingML() {
      var cleanups = withSign(ZodiacSign.MONGOOSE);

      try (cleanups) {
        maximize("ml");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("mcd 10"))));
      }
    }

    @Test
    public void suggestsTurningOffMcdWithNegativeML() {
      var cleanups = new Cleanups(withSign(ZodiacSign.MONGOOSE), withMCD(5));

      try (cleanups) {
        maximize("-ml");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("mcd 0"))));
      }
    }

    @Test
    public void suggestsMcdElevenWhenCanadiaSign() {
      var cleanups = withSign(ZodiacSign.MARMOT);

      try (cleanups) {
        maximize("ml");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("mcd 11"))));
      }
    }
  }

  @Nested
  class AprilBand {
    @Test
    public void recommendsAprilBand() {
      var cleanups = withItem(ItemPool.APRILING_BAND_HELMET);

      try (cleanups) {
        maximize("combat");

        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("aprilband effect c"))));
      }
    }

    @Test
    public void doesNotRecommendAprilBandWithoutItem() {
      maximize("combat");

      assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("aprilband effect c")))));
    }
  }

  @Nested
  class Mayam {
    @Test
    public void recommendsMayamResonanceWithItem() {
      var cleanups =
          new Cleanups(withItem(ItemPool.MAYAM_CALENDAR), withProperty("_mayamSymbolsUsed", ""));
      try (cleanups) {
        maximize("food drop");

        assertThat(
            getBoosts(),
            hasItem(hasProperty("cmd", is("mayam resonance memories of cheesier age"))));
      }
    }

    @Test
    public void doesNotRecommendMayamWithoutItem() {
      var cleanups = withProperty("_mayamSymbolsUsed", "");
      try (cleanups) {
        maximize("food drop");

        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("mayam ")))));
      }
    }

    @Test
    public void doesNotRecommendMayamIfUsed() {
      var cleanups = withProperty("_mayamSymbolsUsed", "yam1,yam2,cheese,clock");
      try (cleanups) {
        maximize("food drop");

        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("mayam ")))));
      }
    }
  }

  @Nested
  class CampAway {
    @Test
    public void recommendsCampAwayCloud() {
      var cleanups =
          new Cleanups(
              withProperty("getawayCampsiteUnlocked", true),
              withProperty("_campAwayCloudBuffs", 0),
              withProperty("_campAwaySmileBuffs", 0));
      try (cleanups) {
        maximize("Muscle Experience Percent");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", is("campaway cloud"))));
      }
    }

    @Test
    public void doesNotRecommendCampAwayCloudIfUsed() {
      var cleanups =
          new Cleanups(
              withProperty("getawayCampsiteUnlocked", true),
              withProperty("_campAwayCloudBuffs", 1),
              withProperty("_campAwaySmileBuffs", 0));
      try (cleanups) {
        maximize("Muscle Experience Percent");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", is("campaway cloud")))));
      }
    }

    @Test
    public void doesNotRecommendCampAwayCloudIfNotOwned() {
      var cleanups =
          new Cleanups(
              withProperty("getawayCampsiteUnlocked", false),
              withProperty("_campAwayCloudBuffs", 0),
              withProperty("_campAwaySmileBuffs", 0));
      try (cleanups) {
        maximize("Muscle Experience Percent");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", is("campaway cloud")))));
      }
    }

    @Test
    public void recommendsCampAwayCloudInOldPath() {
      var cleanups =
          new Cleanups(
              withRestricted(false),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Distant Woods Getaway Brochure"),
              withProperty("getawayCampsiteUnlocked", true),
              withProperty("_campAwayCloudBuffs", 0),
              withProperty("_campAwaySmileBuffs", 0));
      try (cleanups) {
        maximize("Muscle Experience Percent");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", is("campaway cloud"))));
      }
    }

    @Test
    public void doesNotRecommendCampAwayCloudIfUnderStandardRestriction() {
      var cleanups =
          new Cleanups(
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Distant Woods Getaway Brochure"),
              withProperty("getawayCampsiteUnlocked", true),
              withProperty("_campAwayCloudBuffs", 0),
              withProperty("_campAwaySmileBuffs", 0));
      try (cleanups) {
        maximize("Muscle Experience Percent");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", is("campaway cloud")))));
      }
    }
  }

  @Nested
  class StinkyCheese {
    @Test
    public void weightsStinkyCheese() {
      var cleanups =
          new Cleanups(withItem(ItemPool.STINKY_CHEESE_SWORD), withItem(ItemPool.JUNE_CLEAVER));

      try (cleanups) {
        maximize("10stinky cheese, 5bonus June cleaver, -pants -offhand -acc1 -acc2 -acc3");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "stinky cheese sword")));
        assertEquals(1, modFor(BitmapModifier.STINKYCHEESE), 0.01);
      }
    }

    @Test
    public void doesNotOvercountSameItem() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.STINKY_CHEESE_SWORD, 7),
              withEquippableItem(ItemPool.FLASH_LIQUIDIZER_ULTRA_DOUSING_ACCESSORY, 3));

      try (cleanups) {
        maximize("30stinky cheese, item");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "stinky cheese sword")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "stinky cheese wheel")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "stinky cheese diaper")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY1, "stinky cheese eye")));
        assertThat(
            getBoosts(),
            hasItem(recommendsSlot(Slot.ACCESSORY2, "Flash Liquidizer Ultra Dousing Accessory")));
        assertThat(
            getBoosts(),
            hasItem(recommendsSlot(Slot.ACCESSORY3, "Flash Liquidizer Ultra Dousing Accessory")));
        assertEquals(4, modFor(BitmapModifier.STINKYCHEESE), 0.01);
      }
    }
  }

  @Test
  public void prismaticBeretProvidesHatDrop() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.PRISMATIC_BERET),
            withEquippableItem(ItemPool.GINGERBREAD_MASK),
            withEquipped(ItemPool.GREAT_WOLFS_BEASTLY_TROUSERS));

    try (cleanups) {
      maximize("hat drop");
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "prismatic beret")));
      assertEquals(31, modFor(DoubleModifier.HATDROP), 0.01);
    }
  }

  @Nested
  class Wishable {
    @Test
    public void recommendsOnlyWishableEffects() {
      var cleanups = new Cleanups(withItem(ItemPool.POCKET_WISH));

      try (cleanups) {
        maximize("-combat");
        var boosts = getBoosts();
        assertThat(boosts, hasItem(hasProperty("cmd", startsWith("genie effect Disquiet Riot"))));
        assertThat(
            boosts,
            not(hasItem(hasProperty("cmd", startsWith("genie effect Mild-Mannered Professor")))));
      }
    }

    @Test
    public void recommendsPawableEffectsWithPaw() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.CURSED_MONKEY_PAW),
              withProperty("_monkeyPawWishesUsed", 3),
              withProperty("verboseMaximizer", true));

      try (cleanups) {
        maximize("-combat");
        var boosts = getBoosts();
        assertThat(boosts, hasItem(hasProperty("cmd", equalTo("monkeypaw effect Disquiet Riot"))));
        assertThat(
            boosts,
            hasItem(
                hasToString(
                    equalTo(
                        "monkeypaw effect Disquiet Riot (+20) [30 advs duration, 2 uses remaining]"))));
        assertThat(
            boosts,
            not(
                hasItem(
                    hasProperty("cmd", startsWith("monkeypaw effect Mild-Mannered Professor")))));
      }
    }
  }

  @Nested
  class Holiday {
    @Test
    public void recommendsCrystallizedSpiceInAutumn() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.CRYSTALLIZED_PUMPKIN_SPICE), withDay(2025, Month.OCTOBER, 11));

      try (cleanups) {
        maximize("item");
        var boosts = getBoosts();
        assertThat(
            boosts, hasItem(hasProperty("cmd", equalTo("use 1 crystallized pumpkin spice"))));
      }
    }

    @Test
    public void doesNotRecommendCrystallizedSpiceOutsideAutumn() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.CRYSTALLIZED_PUMPKIN_SPICE), withDay(2025, Month.DECEMBER, 11));

      try (cleanups) {
        maximize("item");
        var boosts = getBoosts();
        assertThat(
            boosts, not(hasItem(hasProperty("cmd", equalTo("use 1 crystallized pumpkin spice")))));
      }
    }

    @Test
    public void recommendsM242OnDependenceDay() {
      var cleanups =
          new Cleanups(withItem(ItemPool.M282), withDay(2025, Month.OCTOBER, 30), withMuscle(100));

      try (cleanups) {
        maximize("muscle");
        var boosts = getBoosts();
        assertThat(boosts, hasItem(hasProperty("cmd", equalTo("use 1 M-242"))));
      }
    }

    @Test
    public void doesNotRecommendM242OutsideDependenceDay() {
      var cleanups =
          new Cleanups(withItem(ItemPool.M282), withDay(2025, Month.DECEMBER, 11), withMuscle(100));

      try (cleanups) {
        maximize("muscle");
        var boosts = getBoosts();
        assertThat(boosts, not(hasItem(hasProperty("cmd", equalTo("use 1 M-242")))));
      }
    }
  }

  @Nested
  class Unarmed {
    @Test
    public void recommendBetterUnarmed() {
      var cleanups =
          new Cleanups(
              withEquipped(ItemPool.TIME_SWORD),
              withEquipped(Slot.ACCESSORY1, ItemPool.EXTREME_AMULET),
              withEquipped(Slot.ACCESSORY2, ItemPool.EXTREME_AMULET),
              withEquipped(Slot.ACCESSORY3, ItemPool.EXTREME_AMULET),
              withEquippableItem(ItemPool.TIME_HALO));

      try (cleanups) {
        maximize("adv");
        var boosts = getBoosts();
        assertThat(boosts, hasItem(hasProperty("cmd", startsWith("unequip weapon"))));
        assertThat(boosts, hasItem(recommends(ItemPool.TIME_HALO)));
      }
    }

    @Test
    public void dontRecommendWorseUnarmed() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.GOLD_WEDDING_RING),
              withEquipped(Slot.ACCESSORY2, ItemPool.TINY_PLASTIC_GOLDEN_GUNDAM),
              withEquipped(Slot.ACCESSORY3, ItemPool.TIME_HALO),
              withEquippableItem(ItemPool.TIME_SWORD),
              withEquippableItem(ItemPool.NOVELTY_MONORAIL_TICKET),
              withEquippableItem(ItemPool.TINY_PLASTIC_CRIMBO_REINDEER));

      try (cleanups) {
        maximize("adv");
        var boosts = getBoosts();
        assertThat(boosts, hasItem(recommends(ItemPool.TIME_SWORD)));
        assertThat(boosts, hasItem(recommends(ItemPool.NOVELTY_MONORAIL_TICKET)));
        assertThat(boosts, hasItem(recommends(ItemPool.TINY_PLASTIC_CRIMBO_REINDEER)));
        assertThat(boosts, not(hasItem(recommends(ItemPool.TIME_HALO))));
      }
    }
  }

  @Test
  void canMaximizeRolloverEffectDuration() {
    var cleanups =
        new Cleanups(
            withEquippableItem(ItemPool.SILENT_NIGHTLIGHT),
            withEquippableItem(ItemPool.SPACEGATE_MILITARY_INSIGNIA),
            withEquippableItem(ItemPool.SPACEGATE_SCIENTIST_INSIGNIA),
            withEquippableItem(ItemPool.SHINY_HOOD_ORNAMENT, 3));

    try (cleanups) {
      maximize(
          "10.0 adv, 0.001 rollover effect duration, switch disembodied hand, switch left-hand man, -tie");
      assertThat(getBoosts(), hasItem(recommends(ItemPool.SPACEGATE_SCIENTIST_INSIGNIA)));
      assertThat(getBoosts(), hasItem(recommends(ItemPool.SPACEGATE_MILITARY_INSIGNIA)));
      assertThat(getBoosts(), hasItem(recommends(ItemPool.SILENT_NIGHTLIGHT)));
    }
  }

  @Test
  void shouldSuggestHolsteringIfAvailable() {
    var cleanups =
        new Cleanups(
            withClass(AscensionClass.COW_PUNCHER), withEquippableItem(ItemPool.CUSTOM_SIXGUN));

    try (cleanups) {
      maximize("muscle");
      assertThat(getBoosts(), hasItem(recommends(ItemPool.CUSTOM_SIXGUN)));
    }
  }

  @Nested
  class DamageReduction {
    @Test
    void maximizerCountsInnateShieldDamageReductionAndEnchant() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.OLD_SCHOOL_FLYING_DISC), // base 14, 10 enchant
              withEquippableItem(ItemPool.ASTRAL_SHIELD), // higher base: 15
              withEquippableItem(ItemPool.FURRY_YAM_BUCKLER) // higher enchant: 11
              );

      try (cleanups) {
        assertTrue(maximize("dr"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.OLD_SCHOOL_FLYING_DISC)));
        assertThat(modFor(DoubleModifier.DAMAGE_REDUCTION), equalTo(24.0));
      }
    }

    @Test
    void maximizerAddsShieldsWithNoBaseEnchants() {
      var cleanups = new Cleanups(withEquippableItem(ItemPool.FLAK_SHIELD));

      try (cleanups) {
        assertTrue(maximize("dr"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.FLAK_SHIELD)));
        assertThat(modFor(DoubleModifier.DAMAGE_REDUCTION), equalTo(9.0));
      }
    }
  }

  @Nested
  class EternityCodpiece {
    @Test
    void considersCodpieceBaseEnchantmentsAsAnAccessory() {
      var cleanups =
          new Cleanups(
              withEquippableItem("consolation ribbon", 3),
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE));

      try (cleanups) {
        assertTrue(maximize("mus, mys, mox, -tie"));
        assertThat(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .filter(item -> item.getName().equals("consolation ribbon"))
                .count(),
            equalTo(2L));
        assertTrue(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .allMatch(EquipmentRequest.UNEQUIP::equals));
      }
    }

    @Test
    void prefersHeartstoneAsAccessoryForHpRegen() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withEquippableItem(ItemPool.HEARTSTONE));

      try (cleanups) {
        assertTrue(maximize("+equip heartstone, hp regen, -tie"));
        assertTrue(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .noneMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
      }
    }

    @Test
    void removesSlottedHeartstoneWhenAccessoryScoresBetter() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withEquipped(Slot.CODPIECE1, ItemPool.HEARTSTONE));

      try (cleanups) {
        assertTrue(maximize("hp regen, -tie"));
        assertTrue(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .noneMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
      }
    }

    @Test
    void usesAdditionalSingleEquipGemCopiesInCodpieceSlots() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withEquippableItem(ItemPool.HEARTSTONE, 2));

      try (cleanups) {
        assertTrue(maximize("hp regen, familiar weight, -tie"));
        assertThat(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .filter(item -> item.getItemId() == ItemPool.HEARTSTONE)
                .count(),
            equalTo(1L));
        assertThat(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .filter(item -> item.getItemId() == ItemPool.HEARTSTONE)
                .count(),
            equalTo(1L));
      }
    }

    @ParameterizedTest
    @CsvSource({"0, 5", "2, 3"})
    void considersMultipleMallBuyableCodpieceGems(int owned, long expectedAcquisitions) {
      var onyx = ItemPool.get("unearthly onyx", 1);
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withItem(onyx.getItemId(), owned),
              withInteractivity(true),
              withProperty("autoSatisfyWithMall", true),
              withMeat(100_000));

      try (cleanups) {
        maximizeAny("spooky resistance, -tie");

        assertThat(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .filter(onyx::equals)
                .count(),
            equalTo(5L));
        assertThat(
            getBoosts().stream()
                .filter(boost -> onyx.equals(boost.getItem()))
                .filter(boost -> boost.toString().startsWith("acquire & equip codpiece"))
                .count(),
            equalTo(expectedAcquisitions));
      }
    }

    @Test
    void limitsMallCopiesForSingleEquipCodpieceGems() {
      var eye = ItemPool.get("crystalline seal eye", 1);
      var cleanups =
          new Cleanups(
              withInteractivity(true),
              withProperty("autoSatisfyWithMall", true),
              withMeat(100_000));

      try (cleanups) {
        var checked =
            new CheckedItem(eye.getItemId(), EquipScope.SPECULATE_ANY, 0, PriceLevel.DONT_CHECK);

        assertThat(checked.mallBuyable, equalTo(6));
      }
    }

    @Test
    void considersPullableCodpieceGems() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withItemInStorage(ItemPool.ALIEN_GEMSTONE),
              withInteractivity(false));

      try (cleanups) {
        maximizeAny("mus exp, -tie");

        var gemBoost =
            getBoosts().stream()
                .filter(boost -> boost.getItem() != null)
                .filter(boost -> boost.getItem().getItemId() == ItemPool.ALIEN_GEMSTONE)
                .findFirst()
                .orElseThrow();
        assertThat(gemBoost.getCmd(), startsWith("pull"));
      }
    }

    @Test
    void unclosetsAndEquipsCodpieceBeforeInsertingGem() {
      var cleanups =
          new Cleanups(
              withItemInCloset(ItemPool.THE_ETERNITY_CODPIECE),
              withProperty("autoSatisfyWithCloset", true),
              withItem(ItemPool.ALIEN_GEMSTONE));

      try (cleanups) {
        assertTrue(maximize("mus exp, -tie"));

        var boosts = getBoosts();
        var codpieceBoost =
            boosts.stream()
                .filter(boost -> boost.getItem() != null)
                .filter(boost -> boost.getItem().getItemId() == ItemPool.THE_ETERNITY_CODPIECE)
                .findFirst()
                .orElseThrow();
        var gemBoost =
            boosts.stream()
                .filter(boost -> boost.getItem() != null)
                .filter(boost -> boost.getItem().getItemId() == ItemPool.ALIEN_GEMSTONE)
                .findFirst()
                .orElseThrow();
        assertThat(codpieceBoost.getCmd(), startsWith("closet take"));
        assertTrue(boosts.indexOf(codpieceBoost) < boosts.indexOf(gemBoost));
      }
    }

    @Test
    void pullsAndEquipsCodpieceBeforeInsertingGem() {
      var cleanups =
          new Cleanups(
              withItemInStorage(ItemPool.THE_ETERNITY_CODPIECE),
              withItem(ItemPool.ALIEN_GEMSTONE),
              withInteractivity(false));

      try (cleanups) {
        maximizeAny("mus exp, -tie");

        var boosts = getBoosts();
        var codpieceBoost =
            boosts.stream()
                .filter(boost -> boost.getItem() != null)
                .filter(boost -> boost.getItem().getItemId() == ItemPool.THE_ETERNITY_CODPIECE)
                .findFirst()
                .orElseThrow();
        var gemBoost =
            boosts.stream()
                .filter(boost -> boost.getItem() != null)
                .filter(boost -> boost.getItem().getItemId() == ItemPool.ALIEN_GEMSTONE)
                .findFirst()
                .orElseThrow();
        assertThat(codpieceBoost.getCmd(), startsWith("pull"));
        assertTrue(boosts.indexOf(codpieceBoost) < boosts.indexOf(gemBoost));
      }
    }

    @Test
    void prefersBaseballDiamondAsOffhandForWeaponDamage() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withEquippableItem(ItemPool.BASEBALL_DIAMOND));

      try (cleanups) {
        assertTrue(maximize("+equip baseball diamond, weapon damage, -tie"));
        assertThat(
            Maximizer.best.equipment.get(Slot.OFFHAND).getItemId(),
            equalTo(ItemPool.BASEBALL_DIAMOND));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .noneMatch(item -> item.getItemId() == ItemPool.BASEBALL_DIAMOND));
      }
    }

    @Test
    void movesSlottedBaseballDiamondToOffhandWhenCodpieceForbidden() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withEquipped(Slot.CODPIECE1, ItemPool.BASEBALL_DIAMOND));

      try (cleanups) {
        assertTrue(maximize("+equip baseball diamond, -equip the eternity codpiece, -tie"));
        assertThat(
            Maximizer.best.equipment.get(Slot.OFFHAND).getItemId(),
            equalTo(ItemPool.BASEBALL_DIAMOND));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .noneMatch(item -> item.getItemId() == ItemPool.BASEBALL_DIAMOND));
      }
    }

    @Test
    void removesForbiddenHeartstoneFromActiveCodpiece() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withEquipped(Slot.CODPIECE1, ItemPool.HEARTSTONE));

      try (cleanups) {
        assertTrue(maximize("-equip heartstone, -tie"));
        boolean wearingCodpiece =
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE);
        boolean heartstoneSlotted =
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE);
        assertFalse(wearingCodpiece && heartstoneSlotted);
      }
    }

    @Test
    void movesSlottedHeartstoneToAccessoryWhenCodpieceForbidden() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withEquipped(Slot.CODPIECE1, ItemPool.HEARTSTONE));

      try (cleanups) {
        assertTrue(maximize("+equip heartstone, -equip the eternity codpiece, -tie"));
        assertTrue(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
        assertTrue(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .noneMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .noneMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
      }
    }

    @Test
    void honorsForcedCodpieceAndGemSlots() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withEquippableItem(ItemPool.HEARTSTONE));

      try (cleanups) {
        assertTrue(
            maximize(
                "+equip heartstone, +equip the eternity codpiece, -acc1, -acc2, -codpiece1, -codpiece2, -codpiece3, -codpiece4, -tie"));
        assertThat(
            Maximizer.best.equipment.get(Slot.ACCESSORY3).getItemId(),
            equalTo(ItemPool.THE_ETERNITY_CODPIECE));
        assertThat(
            Maximizer.best.equipment.get(Slot.CODPIECE5).getItemId(), equalTo(ItemPool.HEARTSTONE));
      }
    }

    @Test
    void considersCodpieceWhenForcedGemsFillTheAccessoryPool() {
      int bloodCubicZirconia = ItemPool.get("blood cubic zirconia").getItemId();
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withEquippableItem(ItemPool.HEARTSTONE),
              withEquippableItem(ItemPool.PERIDOT_OF_PERIL),
              withEquippableItem(bloodCubicZirconia));

      try (cleanups) {
        assertTrue(maximize("+equip heartstone, +equip peridot, +equip blood cubic, -acc1, -tie"));
        assertTrue(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE));
        assertTrue(
            SlotSet.ALL_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
        assertTrue(
            SlotSet.ALL_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.PERIDOT_OF_PERIL));
        assertTrue(
            SlotSet.ALL_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == bloodCubicZirconia));
      }
    }

    @Test
    void considersCodpieceForNonAccessoryTypedGemWithOffhandForbidden() {
      int bloodCubicZirconia = ItemPool.get("blood cubic zirconia").getItemId();
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withEquippableItem(ItemPool.BASEBALL_DIAMOND),
              withEquippableItem(ItemPool.HEARTSTONE),
              withEquippableItem(ItemPool.PERIDOT_OF_PERIL),
              withEquippableItem(bloodCubicZirconia));

      try (cleanups) {
        // Baseball is an offhand, not accessory. We're forcing it to rely on the codpiece to be
        // equipped
        assertTrue(
            maximize(
                "+equip baseball diamond, +equip heartstone, +equip peridot, +equip blood cubic, -acc1, -offhand, -tie"));
        assertTrue(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.BASEBALL_DIAMOND));
      }
    }

    @Test
    void doesNotOverflowAccessoryShortlistWhenSlotIsUnequipped() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.THE_ETERNITY_CODPIECE),
              withItem(ItemPool.PERIDOT_OF_PERIL),
              withItem("spring shoes"),
              withItem("Portable Laughing Stock"),
              withItem(ItemPool.HEARTSTONE));

      try (cleanups) {
        assertTrue(maximize("mana cost -tie"));
        assertTrue(Maximizer.bestChecked < 100);
      }
    }

    @Test
    void picksOptimalMixOfCodpieceGemsAcrossDifferentCappedStats() {
      int massiveGemstone = ItemPool.get("massive gemstone").getItemId();
      int healingCrystal = ItemPool.get("New Age healing crystal").getItemId();
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withItem(massiveGemstone, 3),
              withItem(healingCrystal, 2));

      try (cleanups) {
        // 125 caps Item Drop only with all 3 gemstones; 15 caps HP Regen Min only with both
        // crystals. Either gem alone caps one stat and leaves the other unbuffed.
        assertTrue(maximize("0.1 item drop 125 max, 0.1 hp regen min 15 max, -tie"));
        long gemstonesUsed =
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .filter(item -> item.getItemId() == massiveGemstone)
                .count();
        long healingCrystalsUsed =
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .filter(item -> item.getItemId() == healingCrystal)
                .count();
        assertEquals(3, gemstonesUsed);
        assertEquals(2, healingCrystalsUsed);
        assertTrue(Maximizer.bestChecked < 20); // exhaustive search took dozens for this pool
      }
    }

    @Test
    void placesRequiredDualUseGemWhereItScoresBestNotJustToSatisfyTheRequirement() {
      // Meat Drop is worth +60 as a plain accessory but only +30 as a codpiece gem, so a
      // required copy should land in accessory rather than being forced into the codpiece.
      int denseMeatGem = ItemPool.get("incredibly dense meat gem").getItemId();
      int massiveGemstone = ItemPool.get("massive gemstone").getItemId();
      var cleanups =
          new Cleanups(
              withItem(denseMeatGem, 2),
              withItem(massiveGemstone, 3),
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE));

      try (cleanups) {
        assertTrue(
            maximize("+equip incredibly dense meat gem, 0.1 meat drop, 0.1 item drop, -tie"));
        long meatGemsInAccessory =
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .filter(item -> item.getItemId() == denseMeatGem)
                .count();
        long meatGemsInCodpiece =
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .filter(item -> item.getItemId() == denseMeatGem)
                .count();
        long gemstonesInCodpiece =
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .filter(item -> item.getItemId() == massiveGemstone)
                .count();
        assertEquals(2, meatGemsInAccessory);
        assertEquals(0, meatGemsInCodpiece);
        assertEquals(3, gemstonesInCodpiece);
      }
    }

    @Test
    void failsWhenForcedCodpieceGemHasNoAvailableSlot() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withEquippableItem(ItemPool.HEARTSTONE));

      try (cleanups) {
        assertFalse(
            maximize(
                "+equip heartstone, +equip the eternity codpiece, -acc1, -acc2, -codpiece1, -codpiece2, -codpiece3, -codpiece4, -codpiece5, -tie"));
      }
    }

    @Test
    void failsWhenCodpieceModifierIsRequiredButCodpieceIsForbidden() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withItem(ItemPool.ALIEN_GEMSTONE));

      try (cleanups) {
        assertFalse(maximize("mus exp 1 min, -equip the eternity codpiece, -tie"));
      }
    }

    @ParameterizedTest
    @CsvSource({"1, false", "3, false", "4, false", "6, true"})
    void wearsCodpieceOnlyWhenDenseMeatGemsMakeItOptimal(int copies, boolean expectedCodpiece) {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withEquippableItem("incredibly dense meat gem", copies));

      try (cleanups) {
        assertTrue(maximize("meat, -tie"));
        assertThat(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE),
            equalTo(expectedCodpiece));
        long used =
            SlotSet.ACCESSORY_SLOTS.stream()
                    .map(Maximizer.best.equipment::get)
                    .filter(item -> item.getName().equals("incredibly dense meat gem"))
                    .count()
                + SlotSet.CODPIECE_SLOTS.stream()
                    .map(Maximizer.best.equipment::get)
                    .filter(item -> item.getName().equals("incredibly dense meat gem"))
                    .count();
        assertTrue(used <= copies);
      }
    }

    @Test
    void slotsForcedPeridotWhenThatLeavesMoreMeatGemAccessories() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withEquippableItem("incredibly dense meat gem", 3),
              withEquippableItem(ItemPool.PERIDOT_OF_PERIL));

      try (cleanups) {
        assertTrue(maximize("+equip peridot of peril, meat, -tie"));
        assertTrue(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.THE_ETERNITY_CODPIECE));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getItemId() == ItemPool.PERIDOT_OF_PERIL));
      }
    }

    @Test
    void preservesGemsInExcludedCodpieceSlots() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withEquipped(Slot.CODPIECE1, ItemPool.ALIEN_GEMSTONE),
              withItem("autumn years wisdom"));

      try (cleanups) {
        assertTrue(maximize("mus exp, mys exp, -codpiece1, -tie"));
        assertThat(
            Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(),
            equalTo(ItemPool.ALIEN_GEMSTONE));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .anyMatch(item -> item.getName().equals("autumn years wisdom")));
      }
    }

    @Test
    void replacesShortlistedGemWhoseOnlyCopyIsUsedAsAccessory() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withEquippableItem("incredibly dense meat gem"),
              withItem(ItemPool.ALIEN_GEMSTONE));

      try (cleanups) {
        assertTrue(
            maximize(
                "+equip incredibly dense meat gem, +equip the eternity codpiece, meat, mus exp, -codpiece2, -codpiece3, -codpiece4, -codpiece5, -tie"));
        assertThat(
            Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(),
            equalTo(ItemPool.ALIEN_GEMSTONE));
      }
    }

    @Test
    void doesNotReuseGemLockedInExcludedCodpieceSlot() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withEquipped(Slot.CODPIECE1, ItemPool.HEARTSTONE));

      try (cleanups) {
        assertTrue(maximize("+equip heartstone, hp regen, -codpiece1, -tie"));
        assertTrue(
            SlotSet.ACCESSORY_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .noneMatch(item -> item.getItemId() == ItemPool.HEARTSTONE));
        assertThat(
            Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(), equalTo(ItemPool.HEARTSTONE));
      }
    }

    @Test
    void failsWhenRequiredGemCannotBeReleasedFromExcludedSlot() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withEquipped(Slot.CODPIECE1, ItemPool.HEARTSTONE));

      try (cleanups) {
        assertFalse(maximize("+equip heartstone, -equip the eternity codpiece, -codpiece1, -tie"));
      }
    }

    @Test
    void considersCodpieceGemConfigurations() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withItem(ItemPool.ALIEN_GEMSTONE),
              withItem("autumn years wisdom"));

      try (cleanups) {
        assertTrue(maximize("mus exp, mys exp, -tie"));
        assertThat(getBoosts(), hasItem(recommends("alien gemstone")));
        assertThat(getBoosts(), hasItem(recommends("autumn years wisdom")));
        assertThat(modFor(DoubleModifier.MUS_EXPERIENCE), equalTo(1.0));
        assertThat(modFor(DoubleModifier.MYS_EXPERIENCE), equalTo(1.0));
      }
    }

    @Test
    void considersGemsAlreadyInCodpiece() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withEquipped(Slot.CODPIECE1, ItemPool.ALIEN_GEMSTONE),
              withItem("autumn years wisdom"));

      try (cleanups) {
        assertTrue(maximize("mus exp, mys exp, -tie"));
        assertThat(getBoosts(), not(hasItem(recommends("alien gemstone"))));
        assertThat(getBoosts(), hasItem(recommends("autumn years wisdom")));
        assertThat(modFor(DoubleModifier.MUS_EXPERIENCE), equalTo(1.0));
        assertThat(modFor(DoubleModifier.MYS_EXPERIENCE), equalTo(1.0));
      }
    }

    @Test
    void considersUnequippedCodpieceDespiteIrrelevantSlottedGems() {
      var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withEquipped(Slot.CODPIECE1, "big bumboozer marble"),
              withEquipped(Slot.CODPIECE2, "black catseye marble"),
              withEquipped(Slot.CODPIECE3, "beach ball marble"),
              withEquipped(Slot.CODPIECE4, "steely marble"),
              withEquipped(Slot.CODPIECE5, "beige clambroth marble"),
              withItem(ItemPool.ALIEN_GEMSTONE),
              withItem("autumn years wisdom"));

      try (cleanups) {
        assertTrue(maximize("mus exp, mys exp, -tie"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.THE_ETERNITY_CODPIECE)));
        assertThat(getBoosts(), hasItem(recommends("alien gemstone")));
        assertThat(getBoosts(), hasItem(recommends("autumn years wisdom")));
        assertThat(modFor(DoubleModifier.MUS_EXPERIENCE), equalTo(1.0));
        assertThat(modFor(DoubleModifier.MYS_EXPERIENCE), equalTo(1.0));
      }
    }

    @Test
    void prunesNonmatchingGemsBeforeCheckingCombinations() {
      var cleanups = new Cleanups(withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE));

      try (cleanups) {
        assertTrue(maximize("mus exp, -tie"));
        int combinationsWithoutGems = Maximizer.bestChecked;

        try (var ignored =
            new Cleanups(
                withItem("big bumboozer marble"),
                withItem("black catseye marble"),
                withItem("beach ball marble"),
                withItem("steely marble"),
                withItem("beige clambroth marble"),
                withItem("jet bennie marble"),
                withItem("bumblebee marble"))) {
          assertTrue(maximize("mus exp, -tie"));
          assertThat(Maximizer.bestChecked, equalTo(combinationsWithoutGems));
          assertThat(modFor(DoubleModifier.MUS_EXPERIENCE), equalTo(0.0));
          assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.CODPIECE1))));
        }
      }
    }

    @Test
    void choosesBestFiveWhenMoreThanFiveGemsMatch() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withItem("big bumboozer marble"),
              withItem("black catseye marble"),
              withItem("beach ball marble"),
              withItem("steely marble"),
              withItem("beige clambroth marble"));

      try (cleanups) {
        assertTrue(maximize("init, -tie"));
        int combinationsWithFiveGems = Maximizer.bestChecked;

        try (var ignored =
            new Cleanups(withItem("jet bennie marble"), withItem("bumblebee marble"))) {
          assertTrue(maximize("init, -tie"));
          assertThat(Maximizer.bestChecked, equalTo(combinationsWithFiveGems));
          assertThat(modFor(DoubleModifier.INITIATIVE), equalTo(45.0));
          assertThat(getBoosts(), hasItem(recommends("big bumboozer marble")));
          assertThat(getBoosts(), hasItem(recommends("black catseye marble")));
          assertThat(getBoosts(), hasItem(recommends("beach ball marble")));
          assertThat(getBoosts(), hasItem(recommends("steely marble")));
          assertThat(getBoosts(), hasItem(recommends("beige clambroth marble")));
          assertThat(getBoosts(), not(hasItem(recommends("jet bennie marble"))));
          assertThat(getBoosts(), not(hasItem(recommends("bumblebee marble"))));
        }
      }
    }

    @Test
    public void allCodpieceSlotsConsideredByDefault() {
      int massiveGemstone = ItemPool.get("massive gemstone", 1).getItemId();
      final var cleanups =
          new Cleanups(
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withItem(ItemPool.HAMETHYST, 5),
              withItem(massiveGemstone, 5));

      try (cleanups) {
        assertTrue(maximize("tie"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.THE_ETERNITY_CODPIECE)));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .allMatch(item -> item.getItemId() == massiveGemstone));
      }
    }

    @ParameterizedTest
    @CsvSource({"2, 22.0", "7, 55.0"})
    void respectsOwnedCopiesAndAvailableSlots(int copies, double expectedInitiative) {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withItem("big bumboozer marble", copies));

      try (cleanups) {
        assertTrue(maximize("init, -tie"));
        assertThat(modFor(DoubleModifier.INITIATIVE), equalTo(expectedInitiative));
      }
    }

    @Test
    void honorsBooleanConstraintsOnCodpieceGems() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
              withItem(ItemPool.ALIEN_GEMSTONE),
              withOverrideModifiers(
                  ModifierType.ETERNITY_CODPIECE, ItemPool.ALIEN_GEMSTONE, "Adventure Underwater"));

      try (cleanups) {
        assertTrue(maximize("adventure underwater, -tie"));
        assertThat(
            Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(),
            equalTo(ItemPool.ALIEN_GEMSTONE));

        assertTrue(maximize("-adventure underwater, -tie"));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .allMatch(EquipmentRequest.UNEQUIP::equals));
      }
    }

    @Test
    void codpieceGemsAreUsableEvenWhenNotAllowedInStandard() {
      int massiveGemstone = ItemPool.get("massive gemstone").getItemId();
      var cleanups =
          new Cleanups(
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "massive gemstone"),
              withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
              withItem("massive gemstone", 5));

      try (cleanups) {
        assertTrue(maximize("item drop, -tie"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.THE_ETERNITY_CODPIECE)));
        assertTrue(
            SlotSet.CODPIECE_SLOTS.stream()
                .map(Maximizer.best.equipment::get)
                .allMatch(item -> item.getItemId() == massiveGemstone));
      }
    }

    @Test
    void dualPurposeGemNeedsCodpieceToBypassStandardRestriction() {
      int bloodCubicZirconia = ItemPool.get("blood cubic zirconia").getItemId();
      var cleanups =
          new Cleanups(
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "blood cubic zirconia"),
              withItem("blood cubic zirconia", 1));

      try (cleanups) {
        assertTrue(maximize("spooky resistance, -tie"));
        assertThat(getBoosts(), not(hasItem(recommends("blood cubic zirconia"))));

        try (var ignored = new Cleanups(withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE))) {
          assertTrue(maximize("spooky resistance, -tie"));
          assertThat(getBoosts(), hasItem(recommends("blood cubic zirconia")));
          assertTrue(
              SlotSet.CODPIECE_SLOTS.stream()
                  .map(Maximizer.best.equipment::get)
                  .anyMatch(item -> item.getItemId() == bloodCubicZirconia));
        }
      }
    }

    @Nested
    class Tiebreaking {
      @Test
      void tieDisabledPrunesTiebreakerOnlyGems() {
        int massiveGemstone = ItemPool.get("massive gemstone", 1).getItemId();
        var cleanups =
            new Cleanups(
                withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
                withItem(massiveGemstone, 5));

        try (cleanups) {
          assertTrue(maximize("-tie"));
          assertTrue(
              SlotSet.CODPIECE_SLOTS.stream()
                  .map(Maximizer.best.equipment::get)
                  .allMatch(EquipmentRequest.UNEQUIP::equals));
        }
      }

      @Test
      void primaryScoreOutranksTiebreakerScore() {
        int massiveGemstone = ItemPool.get("massive gemstone", 1).getItemId();
        var cleanups =
            new Cleanups(
                withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
                withItem(ItemPool.ALIEN_GEMSTONE),
                withItem(massiveGemstone));

        try (cleanups) {
          assertTrue(maximize("mus exp, codpiece1"));
          assertThat(
              Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(),
              equalTo(ItemPool.ALIEN_GEMSTONE));
        }
      }

      @Test
      void tiebreakerDoesNotRescueNegativePrimaryScore() {
        int massiveGemstone = ItemPool.get("massive gemstone", 1).getItemId();
        var cleanups =
            new Cleanups(
                withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
                withItem(massiveGemstone));

        try (cleanups) {
          assertTrue(maximize("-item, tie"));
          assertThat(
              Maximizer.best.equipment.get(Slot.CODPIECE1), equalTo(EquipmentRequest.UNEQUIP));
        }
      }
    }

    @Nested
    class OptionsAndAcquisition {
      @Test
      void positiveSlotSelectorEnablesOnlyThatCodpieceSlot() {
        var cleanups =
            new Cleanups(
                withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
                withItem(ItemPool.ALIEN_GEMSTONE, 5));

        try (cleanups) {
          assertTrue(maximize("mus exp, codpiece3, -tie"));
          assertThat(
              Maximizer.best.equipment.get(Slot.CODPIECE3).getItemId(),
              equalTo(ItemPool.ALIEN_GEMSTONE));
          assertTrue(
              SlotSet.CODPIECE_SLOTS.stream()
                  .filter(slot -> slot != Slot.CODPIECE3)
                  .map(Maximizer.best.equipment::get)
                  .allMatch(EquipmentRequest.UNEQUIP::equals));
        }
      }

      @Test
      void offhandExclusionStillAllowsBaseballDiamondAsCodpieceGem() {
        var cleanups =
            new Cleanups(
                withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
                withEquippableItem(ItemPool.BASEBALL_DIAMOND));

        try (cleanups) {
          assertTrue(maximize("weapon damage, -offhand, -tie"));
          assertThat(Maximizer.best.equipment.get(Slot.OFFHAND), equalTo(EquipmentRequest.UNEQUIP));
          assertTrue(
              SlotSet.CODPIECE_SLOTS.stream()
                  .map(Maximizer.best.equipment::get)
                  .anyMatch(item -> item.getItemId() == ItemPool.BASEBALL_DIAMOND));
        }
      }

      @Test
      void equipmentFilterControlsCodpieceGemRecommendations() {
        var cleanups =
            new Cleanups(
                withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
                withItem(ItemPool.ALIEN_GEMSTONE));

        try (cleanups) {
          assertTrue(
              Maximizer.maximize(
                  "mus exp, -tie",
                  0,
                  PriceLevel.DONT_CHECK,
                  EquipScope.SPECULATE_INVENTORY,
                  EnumSet.of(KoLConstants.filterType.EQUIP)));
          assertThat(getBoosts(), hasItem(recommends(ItemPool.ALIEN_GEMSTONE)));

          Maximizer.maximize(
              "mus exp, -tie",
              0,
              PriceLevel.DONT_CHECK,
              EquipScope.SPECULATE_INVENTORY,
              EnumSet.of(KoLConstants.filterType.OTHER));
          assertThat(getBoosts(), not(hasItem(recommends(ItemPool.ALIEN_GEMSTONE))));
        }
      }

      @Test
      void currentControlsWhetherIrrelevantSlottedGemsArePreserved() {
        int massiveGemstone = ItemPool.get("massive gemstone", 1).getItemId();
        var cleanups =
            new Cleanups(
                withEquipped(Slot.ACCESSORY1, ItemPool.THE_ETERNITY_CODPIECE),
                withEquipped(Slot.CODPIECE1, massiveGemstone));

        try (cleanups) {
          assertTrue(maximize("+equip the eternity codpiece, mus exp, current, -tie"));
          assertThat(
              Maximizer.best.equipment.get(Slot.CODPIECE1).getItemId(), equalTo(massiveGemstone));

          assertTrue(maximize("+equip the eternity codpiece, mus exp, -current, -tie"));
          assertThat(
              Maximizer.best.equipment.get(Slot.CODPIECE1), equalTo(EquipmentRequest.UNEQUIP));
        }
      }

      @Test
      void pullsEnoughCopiesToFillEveryCodpieceSlot() {
        var onyx = ItemPool.get("unearthly onyx", 1);
        var cleanups =
            new Cleanups(
                withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
                withItemInStorage(onyx.getItemId(), 5),
                withInteractivity(false));

        try (cleanups) {
          maximizeAny("spooky resistance, -tie");

          assertTrue(
              SlotSet.CODPIECE_SLOTS.stream()
                  .map(Maximizer.best.equipment::get)
                  .allMatch(onyx::equals));
          assertThat(
              getBoosts().stream()
                  .filter(boost -> onyx.equals(boost.getItem()))
                  .filter(boost -> boost.getCmd().startsWith("pull"))
                  .count(),
              equalTo(5L));
        }
      }

      @Test
      void buysOnlyMissingStorageCopiesForCodpieceSlots() {
        var onyx = ItemPool.get("unearthly onyx", 1);
        var previousStorageMeat = KoLCharacter.getStorageMeat();
        KoLCharacter.setStorageMeat(100_000);
        var cleanups =
            new Cleanups(
                withItemInStorage(onyx.getItemId(), 2),
                withHardcore(false),
                withInteractivity(false),
                withProperty("autoSatisfyWithMall", true),
                new Cleanups(() -> KoLCharacter.setStorageMeat(previousStorageMeat)));

        try (cleanups;
            var mallPriceDatabase =
                Mockito.mockStatic(MallPriceDatabase.class, Mockito.CALLS_REAL_METHODS)) {
          mallPriceDatabase
              .when(() -> MallPriceDatabase.getPrice(onyx.getItemId()))
              .thenReturn(1_000L);

          var checked =
              new CheckedItem(
                  onyx.getItemId(), EquipScope.SPECULATE_ANY, 1_001, PriceLevel.BUYABLE_ONLY);

          assertThat(checked.pullable, equalTo(2));
          assertThat(checked.pullBuyable, equalTo(3));
          assertThat(checked.getAvailableCount(), equalTo(5));
        }
      }

      @ParameterizedTest
      @CsvSource({"999, 0", "1001, 5"})
      void mallPriceLimitControlsCodpieceGemAvailability(int maxPrice, long expectedGemCount) {
        var onyx = ItemPool.get("unearthly onyx", 1);
        var cleanups =
            new Cleanups(
                withEquippableItem(ItemPool.THE_ETERNITY_CODPIECE),
                withInteractivity(true),
                withProperty("autoSatisfyWithMall", true),
                withMeat(100_000));

        try (cleanups;
            var mallPriceDatabase =
                Mockito.mockStatic(MallPriceDatabase.class, Mockito.CALLS_REAL_METHODS);
            var mallPriceManager =
                Mockito.mockStatic(MallPriceManager.class, Mockito.CALLS_REAL_METHODS)) {
          mallPriceDatabase
              .when(() -> MallPriceDatabase.getPrice(onyx.getItemId()))
              .thenReturn(1_000L);
          mallPriceManager
              .when(() -> MallPriceManager.getMallPrice(onyx.getItemId()))
              .thenReturn(1_000L);
          mallPriceManager
              .when(() -> MallPriceManager.getMallPrice(onyx.getItemId(), 7.0f))
              .thenReturn(1_000L);

          assertTrue(
              Maximizer.maximize(
                  "spooky resistance, -tie",
                  maxPrice,
                  PriceLevel.BUYABLE_ONLY,
                  EquipScope.SPECULATE_ANY,
                  EnumSet.allOf(KoLConstants.filterType.class)));
          assertThat(
              SlotSet.CODPIECE_SLOTS.stream()
                  .map(Maximizer.best.equipment::get)
                  .filter(onyx::equals)
                  .count(),
              equalTo(expectedGemCount));
        }
      }
    }
  }
}
