package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Maximizer.maximizeAny;
import static internal.helpers.Maximizer.modFor;
import static internal.helpers.Player.withAdjustmentsRecalculated;
import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withCampgroundItem;
import static internal.helpers.Player.withClan;
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
import static internal.helpers.Player.withItemInStash;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withLocation;
import static internal.helpers.Player.withMCD;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withMoxie;
import static internal.helpers.Player.withMuscle;
import static internal.helpers.Player.withNotAllowedInStandard;
import static internal.helpers.Player.withOutfit;
import static internal.helpers.Player.withOverrideModifiers;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withRonin;
import static internal.helpers.Player.withSign;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static internal.matchers.Maximizer.recommends;
import static internal.matchers.Maximizer.recommendsEffect;
import static internal.matchers.Maximizer.recommendsSlot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
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
import net.sourceforge.kolmafia.modifiers.StringModifier;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.OutfitPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.AdventureDatabase;
import net.sourceforge.kolmafia.persistence.AdventureDatabase.Environment;
import net.sourceforge.kolmafia.persistence.FamiliarDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ClanManager;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class MaximizerTest {
  @Test
  void respectsCachedCombinationLimit() {
    try (var _ =
        new Cleanups(
            withEquippableItem("hardened slime hat"),
            withEquippableItem("bounty-hunting helmet"),
            withProperty("maximizerCombinationLimit", 1))) {
      maximize("item drop");
      assertThat(Maximizer.bestChecked, is(1));
      assertThat(Maximizer.combinationLimit, is(1L));
    }

    try (var _ =
        new Cleanups(
            withEquippableItem("hardened slime hat"),
            withEquippableItem("bounty-hunting helmet"),
            withProperty("maximizerCombinationLimit", 0))) {
      maximize("item drop");
      assertThat(Maximizer.combinationLimit, is(0L));
    }
  }

  @Test
  void invalidatesCachedTieComparisons() {
    Maximizer.eval = new Evaluator("0, -tie");
    var speculation = new MaximizerSpeculation();
    var comparison = new MaximizerSpeculation();

    speculation.equip(Slot.HAT, ItemPool.get(ItemPool.ANTIQUE_HELMET));
    speculation.setUnscored();
    speculation.getTiebreaker();

    var helmetTurtle = ItemPool.get(ItemPool.HELMET_TURTLE);
    speculation.equip(Slot.HAT, helmetTurtle);
    speculation.setUnscored();
    comparison.equip(Slot.HAT, helmetTurtle);
    comparison.setUnscored();

    assertThat(speculation.compareTo(comparison), is(0));
  }

  @Test
  void clonedSpeculationOwnsCalculatedModifiers() {
    var speculation = new MaximizerSpeculation();
    double itemDrop = speculation.calculate().getDouble(DoubleModifier.ITEMDROP);
    var copy = speculation.clone();

    speculation.getModifiers().setDouble(DoubleModifier.ITEMDROP, itemDrop + 1);

    assertThat(copy.getModifiers().getDouble(DoubleModifier.ITEMDROP), equalTo(itemDrop));
  }

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("MaximizerTest");
    Preferences.reset("MaximizerTest");
  }

  // basic

  @Test
  public void changesGear() {
    try (var _ = new Cleanups(withEquippableItem("helmet turtle"))) {
      assertTrue(maximize("mus"));
      assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
    }
  }

  @Test
  public void equipsItemsOnlyIfHasStats() {
    try (var _ = new Cleanups(withEquippableItem("helmet turtle"), withItem("wreath of laurels"))) {
      assertTrue(maximize("mus"));
      assertEquals(1, modFor(DerivedModifier.BUFFED_MUS), 0.01);
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
    }
  }

  @Test
  public void nothingBetterThanSomething() {
    try (var _ = new Cleanups(withEquippableItem("helmet turtle"))) {
      assertTrue(maximize("-mus"));
      assertEquals(0, modFor(DerivedModifier.BUFFED_MUS), 0.01);
    }
  }

  @Test
  public void exactMatchFindsModifier() {
    try (var _ =
        new Cleanups(
            withEquippableItem("hemlock helm"),
            withEquippableItem("government-issued slacks"),
            // Not a muscle day
            withDay(2023, Month.SEPTEMBER, 27))) {
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
    try (var _ =
        new Cleanups(
            withEquippableItem("government-issued slacks"),
            withEquippableItem("pantsgiving"),
            withEquippableItem("sugar shorts"),
            withEquippableItem("leg-mounted Trainbots"),
            // Not a muscle day
            withDay(2023, Month.SEPTEMBER, 27))) {
      assertTrue(maximize(abbreviation + ", -tie"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, expectedItem)));
    }
  }

  @Nested
  class Max {
    @Test
    public void maxKeywordStopsCountingBeyondTarget() {
      try (var _ =
          new Cleanups(
              withEquippableItem("hardened slime hat"),
              withEquippableItem("bounty-hunting helmet"),
              withSkill("Refusal to Freeze"))) {
        assertTrue(maximize("cold res 3 max, 0.1 item drop"));

        assertEquals(3, modFor(DoubleModifier.COLD_RESISTANCE), 0.01);
        assertEquals(20, modFor(DoubleModifier.ITEMDROP), 0.01);

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet")));
      }
    }

    @Test
    public void startingMaxKeywordTerminatesEarlyIfConditionMet() {
      try (var _ =
          new Cleanups(
              withEquippableItem("hardened slime hat"),
              withEquippableItem("bounty-hunting helmet"),
              withSkill("Refusal to Freeze"))) {
        maximize("3 max, cold res");

        assertThat(
            getBoosts(),
            hasItem(
                hasToString(
                    containsString("(maximum achieved, no further combinations checked)"))));
      }
    }

    @Test
    void maximumAfterNonModifierTermIsInvalid() {
      assertFalse(maximize("2 da, hat, 3 max, -tie"));

      assertThat(
          KoLmafia.lastMessage,
          is("max must follow a modifier or appear at the start of the expression"));
    }
  }

  @Nested
  class Min {
    @Test
    public void minKeywordFailsMaximizationIfNotHit() {
      try (var _ = new Cleanups(withEquippableItem("helmet turtle"))) {
        assertFalse(maximize("mus 2 min"));
        // still provides equipment
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void minKeywordPassesMaximizationIfHit() {
      try (var _ = new Cleanups(withEquippableItem("wreath of laurels"))) {
        assertTrue(maximize("mus 2 min"));
      }
    }

    @Test
    public void startingMinKeywordFailsMaximizationIfNotHit() {
      try (var _ = new Cleanups(withEquippableItem("helmet turtle"))) {
        assertFalse(maximize("2 min, mus"));
        // still provides equipment
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }

    @Test
    public void startingMinKeywordPassesMaximizationIfHit() {
      try (var _ = new Cleanups(withEquippableItem("wreath of laurels"))) {
        assertTrue(maximize("2 min, mus"));
      }
    }

    @Test
    public void zeroWeightModifierStillEnforcesMinimum() {
      Evaluator evaluator = new Evaluator("0 da, 2 min, -tie");
      Modifiers modifiers = new Modifiers();

      modifiers.setDouble(DoubleModifier.DAMAGE_ABSORPTION, 1.0);
      evaluator.getScore(modifiers);
      assertTrue(evaluator.failed);

      modifiers.setDouble(DoubleModifier.DAMAGE_ABSORPTION, 2.0);
      evaluator.getScore(modifiers);
      assertFalse(evaluator.failed);
    }

    @Test
    void minimumAfterNonModifierTermIsInvalid() {
      assertFalse(maximize("2 da, hat, 3 min, -tie"));

      assertThat(
          KoLmafia.lastMessage,
          is("min must follow a modifier or appear at the start of the expression"));
    }
  }

  @Nested
  class Effective {
    @Test
    public void useRangedWeaponWhenMoxieHigh() {
      try (var _ =
          new Cleanups(
              withStats(100, 100, 150),
              withEquippableItem("disco ball"),
              withEquippableItem("two-handed depthsword"))) {
        assertTrue(maximize("weapon dmg, effective"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "disco ball")));
      }
    }

    @Test
    public void useMeleeWeaponWhenMuscleHigh() {
      try (var _ =
          new Cleanups(
              withStats(150, 100, 100),
              withEquippableItem("automatic catapult"),
              withEquippableItem("seal-clubbing club"))) {
        assertTrue(maximize("weapon dmg, effective"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "seal-clubbing club")));
      }
    }

    @Test
    public void useJuneCleaverWhenMoxieHigh() {
      try (var _ =
          new Cleanups(
              withStats(100, 100, 150),
              withEquippableItem("disco ball"),
              withEquippableItem("June cleaver"))) {
        assertTrue(maximize("weapon dmg, effective"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "June cleaver")));
      }
    }

    @Test
    public void useCosplaySaberWhenMoxieHigh() {
      try (var _ =
          new Cleanups(
              withStats(100, 100, 150),
              withEquippableItem("disco ball"),
              withEquippableItem("Fourth of May Cosplay Saber"))) {
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
      try (var _ =
          new Cleanups(
              withStats(10, 5, 5),
              withEquippableItem("seal-skull helmet"),
              withEquippableItem("astral shirt"),
              withEquippableItem("old sweatpants"),
              withEquippableItem("sewer snake"))) {
        assertTrue(maximize(maxStr));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
      }
    }

    @Test
    public void moxieEffectiveDoesNotSelectMelee() {
      String maxStr = "effective";
      try (var _ =
          new Cleanups(
              withStats(5, 5, 10),
              withEquippableItem("seal-skull helmet"),
              withEquippableItem("astral shirt"),
              withEquippableItem("old sweatpants"),
              withEquippableItem("seal-clubbing club"))) {
        assertTrue(maximize(maxStr));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
      }
    }
  }

  @Nested
  class OsityScoring {
    @Test
    public void zeroWeightOsityStillEnforcesExplicitMinimum() {
      var evaluator = new Evaluator("0 raveosity, 3 min, -tie");
      var modifiers = new Modifiers();

      setOsityValue(modifiers, BitmapModifier.RAVEOSITY, 2);
      assertEquals(0.0, evaluator.getScore(modifiers));
      assertTrue(evaluator.failed);

      setOsityValue(modifiers, BitmapModifier.RAVEOSITY, 3);
      assertEquals(0.0, evaluator.getScore(modifiers));
      assertFalse(evaluator.failed);
    }

    @Test
    public void bareOsitiesAcceptTheirDefaultMinimum() {
      assertBareOsityMinimum("clownosity", BitmapModifier.CLOWNINESS, 100);
      assertBareOsityMinimum("clowniness", BitmapModifier.CLOWNINESS, 100);
      assertBareOsityMinimum("raveosity", BitmapModifier.RAVEOSITY, 7);
      assertBareOsityMinimum("surgeonosity", BitmapModifier.SURGEONOSITY, 1);
    }

    private void assertBareOsityMinimum(String expression, BitmapModifier modifier, int minimum) {
      var evaluator = new Evaluator(expression + ", -tie");
      var modifiers = new Modifiers();
      setOsityValue(modifiers, modifier, minimum);

      evaluator.getScore(modifiers);

      assertFalse(evaluator.failed);
    }

    private void setOsityValue(Modifiers modifiers, BitmapModifier modifier, int value) {
      int sourceCount = modifier == BitmapModifier.CLOWNINESS ? value / 25 : value;
      modifiers.setBitmap(modifier, (1 << sourceCount) - 1);
    }
  }

  @Nested
  class Clownosity {
    @Test
    public void clownosityTriesClownEquipment() {
      try (var _ = new Cleanups(withEquippableItem("clown wig"))) {
        assertTrue(maximize("clownosity 50 min -tie"));
        assertFalse(maximize("clownosity -tie"));
        assertFalse(maximize("10 clownosity -tie"));
        // still provides equipment
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "clown wig")));
        assertEquals(50, modFor(BitmapModifier.CLOWNINESS), 0.01);
      }
    }

    @Test
    public void clownositySucceedsWithEnoughEquipment() {
      try (var _ =
          new Cleanups(withEquippableItem("clown wig"), withEquippableItem("polka-dot bow tie"))) {
        assertTrue(maximize("clownosity -tie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "clown wig")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY1, "polka-dot bow tie")));
        assertEquals(125, modFor(BitmapModifier.CLOWNINESS), 0.01);
      }
    }

    @Test
    public void clownosityWeightRetainsDefaultMinimum() {
      try (var _ =
          new Cleanups(
              withEquippableItem("mesh cap"),
              withEquippableItem("clown wig"),
              withEquippableItem("polka-dot bow tie"))) {
        assertTrue(maximize("100 muscle 5 clownosity -tie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "clown wig")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY1, "polka-dot bow tie")));
        assertEquals(125, modFor(BitmapModifier.CLOWNINESS), 0.01);
      }
    }

    @Test
    public void clownosityStopsAt100() {
      try (var _ =
          new Cleanups(
              withEquippableItem("clown wig"),
              withEquippableItem("balloon sword"),
              withEquippableItem("clownskin buckler"))) {
        assertTrue(maximize("clownosity -tie"));
        assertEquals(100, modFor(BitmapModifier.CLOWNINESS), 0.01);
        assertThat(getBoosts().stream().filter(Boost::isEquipment).count(), equalTo(2L));
      }
    }

    @Test
    public void clownosityItemsDontStack() {
      try (var _ = withEquippableItem("clownskin belt", 3)) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem("rave visor"),
              withEquippableItem("baggy rave pants"),
              withEquippableItem("rave whistle"))) {
        assertTrue(maximize("raveosity 5 min -tie"));
        assertFalse(maximize("10 raveosity -tie"));
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
      try (var _ =
          new Cleanups(
              withEquippableItem("blue glowstick"),
              withEquippableItem("glowstick on a string"),
              withEquippableItem("teddybear backpack"),
              withEquippableItem("rave visor"),
              withEquippableItem("baggy rave pants"))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem("head mirror"),
              withEquippableItem("bloodied surgical dungarees"),
              withEquippableItem("surgical apron"),
              withEquippableItem("surgical mask"),
              withEquippableItem("half-size scalpel"),
              withSkill("Torso Awareness"))) {
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
      try (var _ = withEquippableItem("surgical mask", 3)) {
        assertFalse(maximize("surgeonosity 3 min, -tie"));
        assertTrue(maximize("surgeonosity, -tie"));
        assertEquals(1, modFor(BitmapModifier.SURGEONOSITY), 0.01);
        assertThat(
            getBoosts().stream()
                .filter(x -> x.isEquipment() && "surgical mask".equals(x.getItem().getName()))
                .count(),
            equalTo(1L));
      }
    }

    @Test
    public void weightedSurgeonosityEquipsEveryAvailablePiece() {
      try (var _ =
          new Cleanups(
              withEquippableItem("head mirror"),
              withEquippableItem("bloodied surgical dungarees"),
              withEquippableItem("surgical apron"),
              withEquippableItem("surgical mask"),
              withEquippableItem("half-size scalpel"),
              withSkill("Torso Awareness"))) {
        assertTrue(maximize("surgeonosity, -tie"));
        assertEquals(5, modFor(BitmapModifier.SURGEONOSITY), 0.01);
        assertThat(getBoosts(), hasItem(recommends("head mirror")));
        assertThat(getBoosts(), hasItem(recommends("surgical mask")));
        assertThat(getBoosts(), hasItem(recommends("half-size scalpel")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "bloodied surgical dungarees")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "surgical apron")));
      }
    }

    @Test
    public void surgeonosityRespectsMaximum() {
      try (var _ =
          new Cleanups(
              withEquippableItem("head mirror"),
              withEquippableItem("bloodied surgical dungarees"),
              withEquippableItem("surgical apron"),
              withEquippableItem("surgical mask"),
              withEquippableItem("half-size scalpel"),
              withSkill("Torso Awareness"))) {
        assertTrue(maximize("surgeonosity, 3 max, -tie"));
        assertEquals(3, modFor(BitmapModifier.SURGEONOSITY), 0.01);
        assertThat(getBoosts(), hasItem(recommends("head mirror")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "bloodied surgical dungarees")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "surgical apron")));
      }
    }
  }

  @Nested
  class Potions {
    @Test
    public void recommendsUsableNonPotion() {
      try (var _ = withItem(ItemPool.CHARTER_NELLYVILLE)) {
        maximize("hot dmg");

        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("use 1 Charter: Nellyville"))));
      }
    }

    @Test
    public void recommendsLoathingIdol() {
      try (var _ = withItem(ItemPool.LOATHING_IDOL_MICROPHONE_50)) {
        maximize("init");

        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("loathingidol pop"))));
      }
    }

    @Test
    public void givesCorrectEffectDuration() {
      try (var _ =
          new Cleanups(withProperty("verboseMaximizer", true), withItem(ItemPool.CUP_OF_SUGAR))) {
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
      try (var _ =
          new Cleanups(
              withProperty("verboseMaximizer", true),
              withItem(ItemPool.BLACK_CANDLE),
              withOverrideModifiers(
                  ModifierType.ITEM, ItemPool.BLACK_CANDLE, "Effect: \"Rainy Soul Miasma\""))) {
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
      try (var _ =
          new Cleanups(
              withPath(Path.BEES_HATE_YOU), withEquippableItem("bubblewrap bottlecap turtleban"))) {
        maximize("mys");
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
      }
    }

    @Test
    public void itemsCanHaveAtMostBeeosityBees() {
      try (var _ =
          new Cleanups(
              withPath(Path.BEES_HATE_YOU), withEquippableItem("bubblewrap bottlecap turtleban"))) {
        maximize("mys, 5beeosity");
        assertThat(
            getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bubblewrap bottlecap turtleban")));
      }
    }

    @Test
    public void beeosityDoesntApplyOutsideBeePath() {
      try (var _ = new Cleanups(withEquippableItem("bubblewrap bottlecap turtleban"))) {
        maximize("mys");
        assertThat(
            getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bubblewrap bottlecap turtleban")));
      }
    }

    @Test
    void duplicateTwoBeeRequirementDoesNotAllowAThirdBee() {
      try (var _ =
          new Cleanups(
              withPath(Path.BEES_HATE_YOU),
              withEquippableItem("Buddy Bjorn"),
              withEquippableItem("bounty-hunting helmet"))) {
        // Buddy Bjorn has two Bs. Requiring it twice must not raise the allowance to four.
        assertTrue(
            maximize(
                "equip Buddy Bjorn, equip Buddy Bjorn, 100 bonus bounty-hunting helmet, -tie"));

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.CONTAINER, "Buddy Bjorn")));
        // The one-B helmet's bonus ensures it would be recommended if a third B were allowed.
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet"))));
      }
    }

    @Nested
    class Crown {
      @Test
      public void canCrownFamiliarsWithBeesOutsideBeecore() {
        try (var _ =
            new Cleanups(
                withEquippableItem("Crown of Thrones"),
                withFamiliarInTerrarium(FamiliarPool.LOBSTER), // 15% spell damage
                withFamiliarInTerrarium(FamiliarPool.GALLOPING_GRILL))) {
          maximize("spell dmg");

          // used the lobster in the throne.
          assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("enthrone Rock Lobster"))));
        }
      }

      @Test
      public void cannotCrownFamiliarsWithBeesInBeecore() {
        try (var _ =
            new Cleanups(
                withPath(Path.BEES_HATE_YOU),
                withEquippableItem("Crown of Thrones"),
                withFamiliarInTerrarium(FamiliarPool.LOBSTER), // 15% spell damage
                withFamiliarInTerrarium(FamiliarPool.GALLOPING_GRILL))) {
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
        try (var _ = new Cleanups(withItem("baggie of powdered sugar"))) {
          maximize("meat drop");

          assertThat(
              getBoosts(),
              hasItem(hasProperty("cmd", startsWith("use 1 baggie of powdered sugar"))));
        }
      }

      @Test
      public void cannotUsePotionsWithBeesInBeecore() {
        try (var _ =
            new Cleanups(withPath(Path.BEES_HATE_YOU), withItem("baggie of powdered sugar"))) {
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
      try (var _ = new Cleanups(withPath(Path.AVATAR_OF_BORIS))) {
        assertFalse(maximize("plumber"));
        assertFalse(maximize("cold plumber"));
      }
    }

    @Test
    public void plumberCommandForcesSomePlumberItem() {
      try (var _ =
          new Cleanups(
              withPath(Path.PATH_OF_THE_PLUMBER),
              withEquippableItem("work boots"),
              withEquippableItem("shiny ring", 3))) {
        assertTrue(maximize("plumber, mox"));
        assertThat(getBoosts(), hasItem(recommends("work boots")));
      }
    }

    @Test
    public void coldPlumberCommandForcesFlowerAndFrostyButton() {
      try (var _ =
          new Cleanups(
              withPath(Path.PATH_OF_THE_PLUMBER),
              withEquippableItem("work boots"),
              withEquippableItem("bonfire flower"),
              withEquippableItem("frosty button"),
              withEquippableItem("shiny ring", 3))) {
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
      try (var _ =
          new Cleanups(
              withPath(Path.GELATINOUS_NOOB),
              withItem("Knob mushroom"),
              withItem("beer lens"),
              withItem("crossbow string"))) {
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
      try (var _ = new Cleanups(withPath(Path.GELATINOUS_NOOB), withItem("disco mask"))) {
        assertTrue(maximize("moxie -tie"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("absorb ¶9")))); // disco mask
      }
    }

    @Test
    public void canAbsorbHelmetTurtleForEnchants() {
      try (var _ = new Cleanups(withPath(Path.GELATINOUS_NOOB), withItem("helmet turtle"))) {
        assertTrue(maximize("muscle -tie"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("absorb ¶3")))); // helmet turtle
      }
    }

    @Test
    public void canRetrieveAndAbsorbEquipment() {
      try (var _ =
          new Cleanups(
              withPath(Path.GELATINOUS_NOOB),
              withProperty("autoSatisfyWithCloset", true),
              withItemInCloset(ItemPool.HELMET_TURTLE))) {
        assertTrue(maximize("muscle -tie"));
        assertThat(
            getBoosts(),
            hasItem(
                hasProperty(
                    "cmd",
                    startsWith(
                        "closet take 1 ¶"
                            + ItemPool.HELMET_TURTLE
                            + ";absorb ¶"
                            + ItemPool.HELMET_TURTLE))));
      }
    }

    @Test
    public void canBenefitFromOutfits() {
      try (var _ =
          new Cleanups(
              withPath(Path.GELATINOUS_NOOB),
              withEquippableItem("bugbear beanie"),
              withEquippableItem("bugbear bungguard"))) {
        assertTrue(maximize("spell dmg -tie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bugbear beanie")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "bugbear bungguard")));
      }
    }

    @Test
    public void canBenefitFromOutfitsWithWeapons() {
      try (var _ =
          new Cleanups(
              withPath(Path.GELATINOUS_NOOB),
              withEquippableItem("The Jokester's wig"),
              withEquippableItem("The Jokester's gun"),
              withEquippableItem("The Jokester's pants"))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem("spiked femur"), withEquippableItem("sweet ninja sword"))) {
        maximize("letter");

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "sweet ninja sword")));
      }
    }

    @Test
    public void equipMostLetterItems() {
      try (var _ =
          new Cleanups(
              withEquippableItem("asparagus knife"),
              withEquippableItem("sweet ninja sword"),
              withEquippableItem("Fourth of May Cosplay Saber"),
              withEquippableItem("old sweatpants"))) {
        maximize("letter n");

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "sweet ninja sword")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "old sweatpants")));
      }
    }

    @Test
    public void equipMostNumberItems() {
      try (var _ =
          new Cleanups(withEquippableItem("X-37 gun"), withEquippableItem("sweet ninja sword"))) {
        maximize("number");

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "X-37 gun")));
      }
    }
  }

  @Nested
  class WeaponModifiers {
    @Test
    public void clubModifierDoesntAffectOffhand() {
      try (var _ =
          new Cleanups(
              withSkill("Double-Fisted Skull Smashing"),
              withEquippableItem("flaming crutch", 2),
              withEquippableItem("white sword", 2),
              withEquippableItem("dense meat sword"))) {
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
      try (var _ = new Cleanups(withEquippableItem("lawn dart"))) {
        assertTrue(maximize("-tie, club"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "lawn dart")));
      }
    }

    @Test
    public void shieldModifierWorksWithoutTieBreaker() {
      try (var _ = new Cleanups(withEquippableItem("vinyl shield"))) {
        assertTrue(maximize("-tie, shield"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "vinyl shield")));
      }
    }

    @Test
    public void swordModifierFavorsSword() {
      try (var _ =
          new Cleanups(
              withEquippableItem("sweet ninja sword"), withEquippableItem("spiked femur"))) {
        assertTrue(maximize("spooky dmg, sword"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "sweet ninja sword")));
      }
    }
  }

  // effect limits

  @Test
  public void maximizeGiveBestScoreWithEffectsAtNoncombatLimit() {
    try (var _ =
        new Cleanups(
            withEquippableItem("Space Trip safety headphones"),
            withEquippableItem("Krampus Horn"),
            // get ourselves to -25 combat
            withEffect("Shelter of Shed"),
            withEffect("Smooth Movements"))) {
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
      try (var _ =
          new Cleanups(withLocation("Noob Cave"), withEquippableItem("Mer-kin sneakmask"))) {
        assertTrue(maximize("-combat -tie"));
        assertEquals(0, modFor(DoubleModifier.COMBAT_RATE), 0.01);

        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
      }
    }

    @Test
    public void underwaterZonesCheckUnderwaterNegativeCombat() {
      try (var _ =
          new Cleanups(withLocation("The Ice Hole"), withEquippableItem("Mer-kin sneakmask"))) {
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
      try (var _ =
          new Cleanups(withEquippableItem("eldritch hat"), withEquippableItem("eldritch pants"))) {
        assertTrue(maximize("item -tie"));

        assertEquals(50, modFor(DoubleModifier.ITEMDROP), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "eldritch hat")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "eldritch pants")));
      }
    }

    @Test
    public void avoidsOutfitsIfOtherItemsBetter() {
      try (var _ =
          new Cleanups(
              withEquippableItem("eldritch hat"),
              withEquippableItem("eldritch pants"),
              withEquippableItem("Team Avarice cap"))) {
        assertTrue(maximize("item -tie"));

        assertEquals(100, modFor(DoubleModifier.ITEMDROP), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Team Avarice cap")));
      }
    }

    @Test
    public void forcingOutfitRequiresThatOutfit() {
      try (var _ =
          new Cleanups(
              withEquippableItem("bounty-hunting helmet"),
              withEquippableItem("bounty-hunting rifle"),
              withEquippableItem("bounty-hunting pants"),
              withEquippableItem("eldritch hat"),
              withEquippableItem("eldritch pants"))) {
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
      try (var _ =
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
              withEquippableItem("hardened slime belt"))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem("Brimstone Beret"), withEquippableItem("Brimstone Boxers"))) {
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
        try (var _ =
            new Cleanups(
                withEquippableItem("Half a Purse"), withEquippableItem("Hairpiece On Fire"))) {
          assertTrue(maximize("meat -tie"));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "Half a Purse")));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Hairpiece On Fire")));
        }
      }

      @Test
      public void usesFlaskfullOfHollowWithSmithsness() {
        try (var _ =
            new Cleanups(
                withItem("Flaskfull of Hollow"),
                withStats(100, 100, 100),
                withEquipped(Slot.PANTS, "Vicar's Tutu"))) {
          assertTrue(maximize("muscle -tie"));
          assertThat(
              getBoosts(), hasItem(hasProperty("cmd", startsWith("use 1 Flaskfull of Hollow"))));
        }
      }

      @Test
      public void usesFlaskfullOfHollow() {
        try (var _ = new Cleanups(withItem("Flaskfull of Hollow"), withStats(100, 100, 100))) {
          assertTrue(maximize("muscle -tie"));
          assertThat(
              getBoosts(), hasItem(hasProperty("cmd", startsWith("use 1 Flaskfull of Hollow"))));
        }
      }
    }

    @Test
    public void considersCloathingIfHelpful() {
      try (var _ =
          new Cleanups(
              withEquippableItem("Goggles of Loathing"), withEquippableItem("Jeans of Loathing"))) {
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
        try (var _ =
            new Cleanups(
                withLocation("The Slime Tube"),
                withEquippableItem("pernicious cudgel"),
                withEquippableItem("grisly shield"),
                withEquippableItem("shield of the Skeleton Lord"),
                withItem("bitter pill"))) {
          assertTrue(maximize("ml -tie"));

          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "pernicious cudgel")));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "grisly shield")));
          assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("use 1 bitter pill"))));
        }
      }

      @Test
      public void doesntCountIfNotInSlimeTube() {
        try (var _ =
            new Cleanups(
                withLocation("Noob Cave"),
                withEquippableItem("pernicious cudgel"),
                withEquippableItem("grisly shield"),
                withEquippableItem("shield of the Skeleton Lord"))) {
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
        try (var _ =
            new Cleanups(
                withEquippableItem("Hodgman's garbage sticker"),
                withEquippableItem("Hodgman's bow tie"),
                withEquippableItem("Hodgman's lobsterskin pants"),
                withEquippableItem("Hodgman's porkpie hat"),
                withEquippableItem("silver cow creamer"))) {
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
        try (var _ =
            new Cleanups(
                withEquippableItem("Hodgman's bow tie"),
                withEquippableItem("silver cow creamer"))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.MCHUGELARGE_DUFFEL_BAG),
              withEquippableItem(ItemPool.MCHUGELARGE_LEFT_POLE),
              withEquippableItem(ItemPool.MCHUGELARGE_RIGHT_POLE),
              withEquippableItem(ItemPool.FLAMING_CARDBOARD_SWORD))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.SHINING_HALO),
              withEquippableItem(ItemPool.TIME_HALO),
              withEquippableItem(ItemPool.TIME_SWORD))) {
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
      try (var _ =
          new Cleanups(
              withMoxie(100),
              withEffect("Disco Smirk"),
              withSkill("Disco Smirk"),
              withSkill("Quiet Desperation"))) {
        assertTrue(maximize("moxie"));

        assertThat(getBoosts(), hasItem(recommendsEffect("Quiet Desperation")));
      }
    }

    @Test
    public void doNotSuggestWorseFacialExpression() {
      try (var _ =
          new Cleanups(
              withMoxie(100),
              withEffect("Quiet Desperation"),
              withSkill("Disco Smirk"),
              withSkill("Quiet Desperation"))) {
        assertTrue(maximize("moxie"));

        assertThat(getBoosts(), not(hasItem(recommendsEffect("Disco Smirk"))));
      }
    }

    @Test
    public void suggestBetterShanty() {
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 400),
              withEffect("Only Dogs Love a Drunken Sailor"),
              withSkill("Only Dogs Love a Drunken Sailor"),
              withSkill("Who's Going to Pay This Drunken Sailor?"))) {
        assertTrue(maximize("item"));

        assertThat(
            getBoosts(), hasItem(recommendsEffect("Who's Going to Pay This Drunken Sailor?")));
      }
    }

    @Test
    public void doNotSuggestWorseShanty() {
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 400),
              withEffect("Who's Going to Pay This Drunken Sailor?"),
              withSkill("Only Dogs Love a Drunken Sailor"),
              withSkill("Who's Going to Pay This Drunken Sailor?"))) {
        assertTrue(maximize("item"));

        assertThat(getBoosts(), not(hasItem(recommendsEffect("Only Dogs Love a Drunken Sailor"))));
      }
    }
  }

  @Nested
  class Modeables {
    @Test
    public void canFoldUmbrella() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withProperty("umbrellaState", "cocoon"))) {
        assertTrue(maximize("Monster Level Percent"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella broken"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "unbreakable umbrella")));
      }
    }

    @Test
    public void expShouldSuggestUmbrella() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"))) {
        assertTrue(maximize("exp"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella broken"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "unbreakable umbrella")));
      }
    }

    @Test
    public void expShouldNotSuggestUmbrellaIfBetterInSlot() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("vinyl shield"),
              withProperty("umbrellaState", "cocoon"))) {
        assertTrue(maximize("exp"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "vinyl shield")));
      }
    }

    @Test
    public void chooseForwardFacingUmbrellaToSatisfyShield() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquippableItem("tip jar"),
              withEquipped(Slot.PANTS, "old sweatpants"))) {
        assertTrue(maximize("meat, shield"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella forward-facing"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "unbreakable umbrella")));
      }
    }

    @Test
    public void edPieceChoosesFishWithSea() {
      try (var _ =
          new Cleanups(
              withEquippableItem("The Crown of Ed the Undying"),
              withEquippableItem("star shirt"),
              withEquipped(Slot.PANTS, "old sweatpants"),
              withProperty("edPiece", "puma"))) {
        assertTrue(maximize("muscle, sea"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("edpiece fish"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "The Crown of Ed the Undying")));
      }
    }

    @Test
    public void edPieceChoosesBasedOnModesWithoutSea() {
      try (var _ =
          new Cleanups(
              withEquippableItem("The Crown of Ed the Undying"),
              withEquippableItem("star shirt"),
              withEquipped(Slot.PANTS, "old sweatpants"),
              withProperty("edPiece", "puma"))) {
        assertTrue(maximize("muscle"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("edpiece bear"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "The Crown of Ed the Undying")));
      }
    }

    @Test
    public void multipleModeables() {
      try (var _ =
          new Cleanups(
              withEquippableItem("backup camera"),
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old sweatpants"))) {
        assertTrue(maximize("ml, -combat, equip backup camera, equip unbreakable umbrella"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella cocoon"))));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera ml"))));
      }
    }

    @Test
    public void doesNotSelectUmbrellaIfNegativeToOurGoal() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquippableItem("old sweatpants"),
              withEquippableItem("star boomerang"))) {
        assertTrue(maximize("-hp"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "star boomerang")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
      }
    }

    @Test
    public void equipUmbrellaOnLeftHandMan() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"))) {
        assertTrue(maximize("exp, -offhand"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "unbreakable umbrella")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.OFFHAND))));
      }
    }

    @Test
    public void suggestEquippingUmbrellaOnLeftHandMan() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("Microplushie: Hipsterine"),
              withProperty("umbrellaState", "cocoon"))) {
        assertTrue(maximize("exp, -offhand, switch left-hand man"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("familiar Left-Hand Man"))));
        assertThat(
            getBoosts(),
            hasItem(hasProperty("cmd", startsWith("umbrella broken; equip familiar ¶10899"))));
      }
    }

    @Test
    public void suggestEquippingSomethingBetterThanUmbrellaOnLeftHandMan() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withEquipped(Slot.PANTS, "old patched suit-pants"),
              withEquippableItem("shield of the Skeleton Lord"),
              withProperty("umbrellaState", "cocoon"))) {
        assertTrue(maximize("exp, -offhand, switch left-hand man"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("familiar Left-Hand Man"))));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("equip familiar ¶9890"))));
      }
    }

    @Test
    public void shouldSuggestTunedRetrocape() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unwrapped knock-off retro superhero cape"),
              withEquippableItem("palm-frond cloak"),
              withProperty("retroCapeSuperhero", "vampire"),
              withProperty("retroCapeWashingInstructions", "thrill"))) {
        assertTrue(maximize("hot res"));
        assertThat(
            getBoosts(),
            hasItem(recommendsSlot(Slot.CONTAINER, "unwrapped knock-off retro superhero cape")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("retrocape vampire hold"))));
      }
    }

    @Test
    public void shouldSuggestTunedSnowsuit() {
      try (var _ =
          new Cleanups(
              withEquippableItem("Snow Suit"),
              withEquippableItem("wax lips"),
              withFamiliar(FamiliarPool.BLOOD_FACED_VOLLEYBALL))) {
        assertTrue(maximize("exp, hp regen"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "Snow Suit")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("snowsuit goatee"))));
      }
    }

    @Test
    public void shouldSuggestCameraIfSecondBest() {
      try (var _ =
          new Cleanups(
              withEquippableItem("backup camera"),
              withEquippableItem("incredibly dense meat gem"),
              withProperty("backupCameraMode", "ml"))) {
        assertTrue(maximize("meat"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BACKUP_CAMERA)));
        assertThat(getBoosts(), hasItem(recommends("incredibly dense meat gem")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera meat"))));
      }
    }

    @Test
    public void shouldSuggestCameraIfSlotsExcluded() {
      try (var _ =
          new Cleanups(
              withEquippableItem("backup camera"), withProperty("backupCameraMode", "ml"))) {
        assertTrue(maximize("meat -acc1 -acc2"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY3, "backup camera")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera meat"))));
      }
    }

    @Test
    public void shouldSuggestPullableCameraIfNotRestricted() {
      try (var _ =
          new Cleanups(
              withItemInStorage("backup camera"), withProperty("backupCameraMode", "ml"))) {
        maximizeAny("meat");
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BACKUP_CAMERA)));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("pull"))));
      }
    }

    @Test
    public void shouldNotSuggestPullableCameraIfRestricted() {
      try (var _ =
          new Cleanups(
              withItemInStorage("backup camera"),
              withProperty("backupCameraMode", "ml"),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "backup camera"))) {
        maximizeAny("meat");
        assertThat(getBoosts(), not(hasItem(recommends(ItemPool.BACKUP_CAMERA))));
      }
    }

    @Test
    public void shouldSuggestReplicaParka() {
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.REPLICA_JURASSIC_PARKA), withSkill(SkillPool.TORSO))) {
        assertTrue(maximize("dr"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "replica Jurassic Parka")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void shouldSuggestUsingLedCandleWithJill() {
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.JILL_OF_ALL_TRADES, 400), withItem(ItemPool.LED_CANDLE))) {
        assertTrue(maximize("item"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("ledcandle disco"))));
      }
    }

    @Test
    public void shouldNotSuggestUsingLedCandleWithoutJill() {
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY, 400), withItem(ItemPool.LED_CANDLE))) {
        assertTrue(maximize("item"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("ledcandle disco")))));
      }
    }

    @Test
    public void equipWithModeForcesThatMode() {
      try (var _ =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO))) {
        // If not asked for, "ml" would pick spikolodon
        assertTrue(maximize("ml, equip Jurassic Parka (ghostasaurus mode)"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "Jurassic Parka")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void equipWithModeAcceptsAliases() {
      try (var _ =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO))) {
        assertTrue(maximize("ml, equip Jurassic Parka (spooky mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void equipWithModeAcceptsBareParenthetical() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old sweatpants"), // Get some ML on
              withProperty("umbrellaState", "broken"))) {
        // If not asked for, "ml" would keep the umbrella broken
        assertTrue(maximize("ml, equip unbreakable umbrella (cocoon)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella cocoon"))));
      }
    }

    @Test
    public void equipWithModeAcceptsMultiWordModes() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unwrapped knock-off retro superhero cape"),
              withProperty("retroCapeSuperhero", "vampire"),
              withProperty("retroCapeWashingInstructions", "thrill"))) {
        // If not asked for, "hot res" would pick vampire hold
        assertTrue(
            maximize("hot res, equip unwrapped knock-off retro superhero cape (robot kill mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("retrocape robot kill"))));
      }
    }

    @Test
    public void equipWithModeAppliesToReplicaParka() {
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.REPLICA_JURASSIC_PARKA), withSkill(SkillPool.TORSO))) {
        assertTrue(maximize("ml, equip replica Jurassic Parka (kachungasaur mode)"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "replica Jurassic Parka")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka kachungasaur"))));
      }
    }

    @Test
    public void equipStillMatchesItemNamesEndingInMode() {
      try (var _ = new Cleanups(withEquippableItem("Jarlsberg's pan (Cosmic portal mode)"))) {
        assertTrue(maximize("spell dmg, equip Jarlsberg's pan (Cosmic portal mode)"));
        assertThat(
            getBoosts(),
            hasItem(recommendsSlot(Slot.OFFHAND, "Jarlsberg's pan (Cosmic portal mode)")));
      }
    }

    @Test
    public void equipStillMatchesItemNamesEndingInMode2() {
      try (var _ = new Cleanups(withEquippableItem("Boris's Helm (askew)"))) {
        assertTrue(maximize("ml, equip Boris's Helm (askew)"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Boris's Helm (askew)")));
      }
    }

    @Test
    public void equipWithUnknownModeErrors() {
      try (var _ =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO))) {
        assertFalse(maximize("ml, equip Jurassic Parka (magical mode)"));
      }
    }

    @Test
    public void shouldErrorWhenModesConflict() {
      try (var _ =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO))) {
        assertFalse(
            maximize(
                "equip Jurassic Parka (ghostasaurus mode), equip Jurassic Parka (kachungasaur mode)"));
      }
    }

    @Test
    public void shouldErrorWhenIndirectConflict() {
      try (var _ = new Cleanups(withEquippableItem(ItemPool.CROWN_OF_ED))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.CROWN_OF_ED),
              withEquippableItem(ItemPool.OLD_SCUBA_TANK))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.CROWN_OF_ED),
              withEquippableItem(ItemPool.OLD_SCUBA_TANK))) {
        // 'sea' cannot override an explict mode. We equip a scuba tank to sastify 'sea'
        assertTrue(maximize("equip Crown of Ed the Undying (hyena mode), sea"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("edpiece hyena"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "The Crown of Ed the Undying")));
      }
    }

    @Test
    public void doesNotChangeModeOfItemInExcludedSlot() {
      try (var _ =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, "backup camera"),
              withProperty("backupCameraMode", "init"))) {
        assertTrue(maximize("meat, -acc1"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("backupcamera")))));
      }
    }

    @Test
    public void doesNotChangeModeOfUmbrellaInExcludedSlot() {
      try (var _ =
          new Cleanups(
              withEquipped(Slot.OFFHAND, "unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withProperty("umbrellaState", "cocoon"))) {
        assertTrue(maximize("ml, -offhand, switch left-hand man"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("umbrella")))));
      }
    }

    @Test
    public void doesChangeModeOfUmbrellaInNonExcludedSlot() {
      try (var _ =
          new Cleanups(
              withEquipped(Slot.OFFHAND, "unbreakable umbrella"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withProperty("umbrellaState", "cocoon"))) {
        assertTrue(maximize("ml"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella"))));
      }
    }

    @Test
    public void doesChangeModeOfUmbrellaInNonExcludedSlot2() {
      try (var _ =
          new Cleanups(
              withEquipped(Slot.OFFHAND, "unbreakable umbrella"),
              withEquipped(Slot.ACCESSORY1, "backup camera"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
              withProperty("umbrellaState", "cocoon"),
              withProperty("backupCameraMode", "meat"))) {
        assertTrue(maximize("ml, -offhand"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("umbrella")))));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera"))));
      }
    }

    @Test
    public void doesNotEquipExcludedMode() {
      try (var _ =
          new Cleanups(
              withEquippableItem("backup camera"), withProperty("backupCameraMode", "init"))) {
        assertTrue(maximize("meat, -tie, -equip backup camera (meat)"));
        assertThat(getBoosts(), not(hasItem(recommends(ItemPool.BACKUP_CAMERA))));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("backupcamera")))));
      }
    }

    @Test
    public void bonusModeWinsOverNaturalValue() {
      try (var _ =
          new Cleanups(
              withEquippableItem("Backup camera"), withProperty("backupCameraMode", "ml"))) {
        assertTrue(maximize("init, 150 bonus Backup camera (meat)"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BACKUP_CAMERA)));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera meat"))));
      }
    }

    @Test
    public void bonusWithModeAcceptsAliases() {
      try (var _ =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO))) {
        assertTrue(maximize("ml, 100 bonus Jurassic Parka (spooky mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void bonusWithModeAcceptsModeWithoutTheWordMode() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unbreakable umbrella"),
              withEquipped(Slot.PANTS, "old sweatpants"),
              withProperty("umbrellaState", "broken"))) {
        assertTrue(maximize("ml, 100 bonus unbreakable umbrella (cocoon)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("umbrella cocoon"))));
      }
    }

    @Test
    public void bonusWithModeAcceptsMultiWordModes() {
      try (var _ =
          new Cleanups(
              withEquippableItem("unwrapped knock-off retro superhero cape"),
              withProperty("retroCapeSuperhero", "vampire"),
              withProperty("retroCapeWashingInstructions", "thrill"))) {
        assertTrue(
            maximize(
                "hot res, 100 bonus unwrapped knock-off retro superhero cape (robot kill mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("retrocape robot kill"))));
      }
    }

    @Test
    public void bonusStillMatchesItemNamesEndingInMode() {
      try (var _ = new Cleanups(withEquippableItem("Jarlsberg's pan (Cosmic portal mode)"))) {
        assertTrue(maximize("spell dmg, 100 bonus Jarlsberg's pan (Cosmic portal mode)"));
        assertThat(
            getBoosts(),
            hasItem(recommendsSlot(Slot.OFFHAND, "Jarlsberg's pan (Cosmic portal mode)")));
      }
    }

    @Test
    public void bonusStillMatchesItemNamesEndingInMode2() {
      try (var _ = new Cleanups(withEquippableItem("Boris's Helm (askew)"))) {
        assertTrue(maximize("ml, 100 bonus Boris's Helm (askew)"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Boris's Helm (askew)")));
      }
    }

    @Test
    public void bonusWithUnknownModeErrors() {
      try (var _ =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO))) {
        assertFalse(maximize("ml, 100 bonus Jurassic Parka (magical mode)"));
      }
    }

    @Test
    public void dontSwitchModeForZeroScoreWithoutTiebreaker() {
      try (var _ =
          new Cleanups(
              withEquipped(Slot.SHIRT, ItemPool.JURASSIC_PARKA),
              withSkill(SkillPool.TORSO),
              withProperty("parkaMode", "kachungasaur"))) {
        assertTrue(maximize("+adv, -tie"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("parka")))));
      }
    }

    @Test
    public void higherBonusModeWins() {
      try (var _ =
          new Cleanups(
              withEquippableItem("backup camera"), withProperty("backupCameraMode", "ml"))) {
        // 120 vs 110
        assertTrue(maximize("70 bonus backup camera (meat), 10 bonus backup camera (init)"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BACKUP_CAMERA)));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera meat"))));
      }
    }

    @Test
    public void equipModeWinsOverBonusMode() {
      try (var _ =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO))) {
        assertTrue(
            maximize(
                "equip Jurassic Parka (ghostasaurus mode), 100 bonus Jurassic Parka (kachungasaur mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void bonusModeNeverOverridesEquipModeRegardlessOfOrder() {
      try (var _ =
          new Cleanups(withEquippableItem(ItemPool.JURASSIC_PARKA), withSkill(SkillPool.TORSO))) {
        assertTrue(
            maximize(
                "100 bonus Jurassic Parka (kachungasaur mode), equip Jurassic Parka (ghostasaurus mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("parka ghostasaurus"))));
      }
    }

    @Test
    public void bonusModeNeverOverridesImplicitForce() {
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.CROWN_OF_ED),
              withEquippableItem(ItemPool.OLD_SCUBA_TANK))) {
        // 'sea' still forces fish mode
        assertTrue(maximize("sea, 100 bonus Crown of Ed the Undying (hyena mode)"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("edpiece fish"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "The Crown of Ed the Undying")));
      }
    }

    @Test
    public void bonusScoreAndModeableScoreAddsTogether() {
      try (var _ =
          new Cleanups(
              withEquippableItem("backup camera"),
              withEquippableItem("incredibly dense meat gem"),
              withProperty("backupCameraMode", "init"))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem("backup camera"),
              withEquippableItem("incredibly dense meat gem"))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem("backup camera"), withProperty("backupCameraMode", "ml"))) {
        assertTrue(maximize("ml, 100 bonus backup camera (meat)"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.BACKUP_CAMERA)));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("backupcamera meat"))));
      }
    }

    @Test
    public void doesSwitchAwayFromBadBonusForMode() {
      try (var _ =
          new Cleanups(
              withEquippableItem("backup camera"), withProperty("backupCameraMode", "init"))) {
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
      try (var _ =
          new Cleanups(withItem(ItemPool.GARBAGE_TOTE), withItem(ItemPool.TINSEL_TIGHTS))) {
        assertTrue(maximize("monster level"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "tinsel tights")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("equip pants ¶9693"))));
      }
    }

    @Test
    public void shouldSuggestEquippingGarbageToteItem2() {
      try (var _ =
          new Cleanups(
              withItem(ItemPool.REPLICA_GARBAGE_TOTE),
              withItem(ItemPool.REPLICA_HAIKU_KATANA),
              withItem(ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 5),
              withSkill("Double-Fisted Skull Smashing"))) {
        assertTrue(maximize("weapon damage percent"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "broken champagne bottle")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("equip off-hand ¶9692"))));
      }
    }

    @Test
    public void shouldFoldUnusedChampagneBottle() {
      try (var _ =
          new Cleanups(
              withItem(ItemPool.REPLICA_GARBAGE_TOTE),
              withItem(ItemPool.REPLICA_HAIKU_KATANA),
              withItem(ItemPool.BROKEN_CHAMPAGNE),
              withProperty("garbageChampagneCharge", 0),
              withProperty("_garbageItemChanged", false),
              withSkill("Double-Fisted Skull Smashing"))) {
        assertTrue(maximize("weapon damage percent"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "broken champagne bottle")));
        assertThat(
            getBoosts(),
            hasItem(hasProperty("cmd", startsWith("fold ¶9692;equip off-hand ¶9692"))));
      }
    }

    @Test
    public void shouldSuggestFoldingGarbageToteItem() {
      try (var _ =
          new Cleanups(withItem(ItemPool.GARBAGE_TOTE), withItem(ItemPool.TINSEL_TIGHTS))) {
        assertTrue(maximize("weapon damage percent"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "broken champagne bottle")));
        assertThat(
            getBoosts(), hasItem(hasProperty("cmd", startsWith("fold ¶9692;equip weapon ¶9692"))));
      }
    }

    @Test
    public void shouldNotSuggestUsingGarbageToteItem() {
      try (var _ = new Cleanups(withItem(ItemPool.TINSEL_TIGHTS))) {
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
      try (var _ = withProperty("horseryAvailable", true)) {
        assertTrue(maximize("-combat"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("horsery dark"))));
      }
    }

    @Test
    public void doesNotSuggestHorseryIfUnaffordable() {
      try (var _ =
          new Cleanups(
              withProperty("horseryAvailable", true),
              withProperty("_horsery", "normal horse"),
              withMeat(0))) {
        assertTrue(maximize("-combat"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("horsery dark")))));
      }
    }

    @Test
    public void doesNotSuggestHorseryIfNotAllowedInStandard() {
      try (var _ =
          new Cleanups(
              withProperty("horseryAvailable", true),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Horsery contract"))) {
        assertTrue(maximize("-combat"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("horsery dark")))));
      }
    }
  }

  @Nested
  public class Familiars {
    @Test
    public void leftHandManEquipsItem() {
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.LEFT_HAND),
              withEquippableItem(ItemPool.WICKER_SHIELD, 2),
              withItem(ItemPool.STUFFED_CHEST) // equipment with no enchant to test modifiers crash
              )) {
        assertTrue(maximize("moxie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "wicker shield")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "wicker shield")));
      }
    }

    @Test
    public void leftHandManConsidersRequestedItems() {
      try (var _ =
          new Cleanups(
              withEquippableItem("big stick"), // 2-handed weapon
              withEquippableItem("bread basket"),
              withEquippableItem("cyborg doll"),
              withFamiliar(FamiliarPool.LEFT_HAND))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem("big stick"), // 2-handed weapon
              withEquippableItem("bread basket"),
              withEquippableItem("cyborg doll"),
              withFamiliarInTerrarium(FamiliarPool.LEFT_HAND))) {
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
      try (var _ =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withItem(ItemPool.SOLID_SHIFTING_TIME_WEIRDNESS) // 4 adv with any familiar
              )) {
        assertTrue(maximize("adv -tie +switch mosquito"));
        assertThat(
            getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "solid shifting time weirdness")));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("familiar Mosquito"))));
      }
    }

    @Test
    public void switchMultipleFamiliarsConsidersMultipleItems() {
      try (var _ =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.TRICK_TOT),
              withFamiliarInTerrarium(FamiliarPool.HAND),
              withFamiliarInTerrarium(FamiliarPool.MOSQUITO),
              withItem(ItemPool.TRICK_TOT_UNICORN), // 5 adv with tot
              withItem(ItemPool.TRICK_TOT_CANDY), // 0 adv
              withItem(ItemPool.TIME_SWORD), // 3 adv with hand
              withItem(ItemPool.SOLID_SHIFTING_TIME_WEIRDNESS) // 4 adv with any familiar
              )) {
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
      try (var _ =
          new Cleanups(
              withFamiliar(FamiliarPool.MOSQUITO),
              withFamiliarInTerrarium(FamiliarPool.BADGER),
              withFamiliarInTerrarium(FamiliarPool.PURSE_RAT, 400),
              withItem(ItemPool.LIARS_PANTS))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem("Counterclockwise Watch"), // 10, watch
              withEquippableItem("grandfather watch"), // 6, also a watch
              withEquippableItem("plexiglass pocketwatch"), // 3, stacks
              withEquippableItem("gold wedding ring") // 1
              )) {
        assertTrue(maximize("adv"));
        assertEquals(54, modFor(DoubleModifier.ADVENTURES), 0.01);
        assertThat(getBoosts(), hasItem(recommends("Counterclockwise Watch")));
        assertThat(getBoosts(), hasItem(recommends("plexiglass pocketwatch")));
        assertThat(getBoosts(), hasItem(recommends("gold wedding ring")));
      }
    }

    @Test
    public void suggestsNoAdventureGearInSlowAndSteady() {
      try (var _ =
          new Cleanups(
              withPath(Path.SLOW_AND_STEADY),
              withEquippableItem("Counterclockwise Watch"),
              withEquippableItem("gold wedding ring"))) {
        assertTrue(maximize("adv"));
        assertEquals(100, modFor(DoubleModifier.ADVENTURES), 0.01);
        assertThat(getBoosts(), not(hasItem(recommends("Counterclockwise Watch"))));
        assertThat(getBoosts(), not(hasItem(recommends("gold wedding ring"))));
      }
    }

    @Test
    public void watchesDontStackOutsideAdventuresEither() {
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.SASQ_WATCH), // 3, watch
              withEquippableItem("Crimbolex watch") // 5, also a watch
              )) {
        assertTrue(maximize("fites"));
        assertEquals(15, modFor(DoubleModifier.PVP_FIGHTS), 0.01);
        assertThat(getBoosts(), hasItem(recommends("Crimbolex watch")));
        assertThat(getBoosts(), not(hasItem(recommends(ItemPool.SASQ_WATCH))));
      }
    }

    @Test
    public void surgeonosityItemsStackOutsideSurgeonosity() {
      try (var _ = withEquippableItem("surgical mask", 3)) {
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
      try (var _ = withEquippableItem("clownskin belt", 3)) {
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
      try (var _ = withEquippableItem("blue glowstick", 3)) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem("Brimstone Bludgeon", 3),
              withSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING))) {
        assertTrue(maximize("muscle, -tie"));
        assertEquals(100, modFor(DoubleModifier.MUS_PCT), 0.01);
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "Brimstone Bludgeon")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "Brimstone Bludgeon")));
        assertEquals(1, modFor(BitmapModifier.BRIMSTONE), 0.01);
      }
    }

    @Test
    public void cloathingItemsStackOutsideCloathing() {
      try (var _ =
          new Cleanups(
              withEquippableItem("Stick-Knife of Loathing", 3),
              withSkill(SkillPool.DOUBLE_FISTED_SKULL_SMASHING))) {
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
      try (var _ = withEquippableItem(ItemPool.ICE_SICKLE)) {
        assertTrue(maximize("ml, +equip ice baby"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "ice baby")));
      }
    }

    @Test
    public void prefersFoldableInSlotWithHigherScore() {
      try (var _ =
          new Cleanups(
              withItem(ItemPool.ORIGAMI_MAGAZINE),
              withSkill(SkillPool.TORSO),
              withFamiliar(FamiliarPool.GHUOL_WHELP))) {
        assertTrue(maximize("meat, sleaze dmg, -tie"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "origami pasties")));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.FAMILIAR))));
      }
    }

    @Test
    public void singleFoldSourceProducesRequestedForm() {
      try (var _ = new Cleanups(withItem(ItemPool.MAKESHIFT_CRANE), withStats(0, 75, 35))) {
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
        try (var _ =
            new Cleanups(
                withEquippableItem("aerogel anvil"),
                withEquippableItem("Baron von Ratsworth's monocle"),
                withEquippableItem("observational glasses"),
                withEquippableItem("ring of the Skeleton Lord"))) {
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
        try (var _ =
            new Cleanups(
                withEquippableItem("eyepatch"),
                withEquippableItem("swashbuckling pants"),
                withEquippableItem("Pantsgiving"),
                withEquippableItem("stuffed shoulder parrot"),
                withEquippableItem("pirate fledges"),
                withEquippableItem("moustache sock"),
                withEquippableItem("tube sock"),
                withEquippableItem("mirrored aviator shades"))) {
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
        try (var _ =
            new Cleanups(
                withFamiliar(FamiliarPool.MOSQUITO),
                withEquippableItem(ItemPool.DAS_BOOT),
                withEquippableItem("Mer-kin scholar mask"),
                withEquippableItem("Lens of Violence"),
                withEquippableItem("old SCUBA tank"))) {
          assertTrue(maximize("item, sea"));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "Mer-kin scholar mask")));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, "das boot")));
          assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.CONTAINER))));
        }
      }
    }

    @Nested
    class ModBonus {
      @Test
      public void canApplySameModbonusMultipleTimes() {
        try (var _ =
            new Cleanups(
                withEquippableItem(ItemPool.PANTSGIVING),
                withEquippableItem("black greaves"),
                withEquippableItem("Camp Scout backpack"),
                withEquippableItem("barskin cloak"))) {
          assertTrue(maximize("muscle, 100 modbonus Drops Items"));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "Pantsgiving")));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.CONTAINER, "Camp Scout backpack")));
        }
      }

      @Test
      public void betterModbonusWins() {
        try (var _ =
            new Cleanups(
                withEquippableItem("garbage sticker"),
                withEquippableItem(ItemPool.LEGENDARY_SEAL_CLUBBING_CLUB))) {
          assertTrue(maximize("100 modbonus Drops Meat, 50 modbonus Attacks Can't Miss"));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "garbage sticker")));
        }
      }

      @Test
      public void succeedsEvenIfNoModbonusAvailable() {
        try (var _ =
            new Cleanups(
                withEquippableItem("black greaves"), withEquippableItem("barskin cloak"))) {
          assertTrue(maximize("muscle, 100 modbonus Drops Items"));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "black greaves")));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.CONTAINER, "barskin cloak")));
        }
      }

      @Test
      public void adjustsModeableToAchieveModbonus() {
        try (var _ =
            new Cleanups(
                withEquippableItem("The Crown of Ed the Undying"),
                withEquippableItem("hangman's hood"),
                withProperty("edPiece", "puma"))) {
          assertTrue(maximize("muscle, 100 modbonus Adventure Underwater"));
          assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "The Crown of Ed the Undying")));
          assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("edpiece fish"))));
        }
      }
    }
  }

  @Nested
  public class Chefstaves {
    @Test
    public void cantEquipCheffstaffsOnLeftHandMan() {
      try (var _ =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withFamiliar(FamiliarPool.LEFT_HAND),
              withSkill(SkillPool.SPIRIT_OF_RIGATONI))) {
        assertTrue(maximize("spell dmg, -weapon"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.FAMILIAR))));
      }
    }

    @Test
    public void mustEquipSauceGloveForChefstaff() {
      try (var _ =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withEquippableItem("special sauce glove"),
              withClass(AscensionClass.SAUCEROR))) {
        assertTrue(maximize("spell dmg, -tie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "Staff of Kitchen Royalty")));
        assertThat(getBoosts(), hasItem(recommends("special sauce glove")));
      }
    }

    @Test
    public void cannotUseSauceGloveIfNotSauceror() {
      try (var _ =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withEquippableItem("special sauce glove"),
              withClass(AscensionClass.SEAL_CLUBBER))) {
        assertTrue(maximize("spell dmg, -tie"));
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.WEAPON))));
      }
    }

    @Test
    public void canUseSkillToEquipChefstaves() {
      try (var _ =
          new Cleanups(
              withEquippableItem("Staff of Kitchen Royalty"),
              withSkill(SkillPool.SPIRIT_OF_RIGATONI))) {
        assertTrue(maximize("spell dmg, -tie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "Staff of Kitchen Royalty")));
      }
    }
  }

  @Nested
  public class VampireVintnerWine {
    @Test
    public void doesNotSuggestVintnerWineIfUnavailable() {
      try (var _ = new Cleanups(withItem(ItemPool.VAMPIRE_VINTNER_WINE, 0))) {
        assertTrue(maximize("Item Drop"));
        assertThat(
            getBoosts(),
            not(hasItem(hasProperty("cmd", startsWith("drink 1 1950 Vampire Vintner wine")))));
      }
    }

    @Test
    public void doesSuggestVintnerWineIfAvailableWithCorrectEffect() {
      try (var _ =
          new Cleanups(
              withItem(ItemPool.VAMPIRE_VINTNER_WINE, 1),
              withProperty("vintnerWineEffect", "Wine-Hot"),
              withProperty("vintnerWineLevel", 12))) {
        assertTrue(maximize("Item Drop"));
        assertThat(
            getBoosts(),
            hasItem(hasProperty("cmd", startsWith("drink 1 1950 Vampire Vintner wine"))));
      }
    }

    @Test
    public void doesNotSuggestVintnerWineIfAvailableWithWrongEffect() {
      try (var _ =
          new Cleanups(
              withItem(ItemPool.VAMPIRE_VINTNER_WINE, 1),
              withProperty("vintnerWineEffect", "Wine-Hot"),
              withProperty("vintnerWineLevel", 12))) {
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
      try (var _ = new Cleanups(withSkill(SkillPool.SCARYSAUCE))) {
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
      try (var _ =
          new Cleanups(
              withSkill(SkillPool.SCARYSAUCE), withEquippableItem(ItemPool.VELOUR_VISCOMETER))) {
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
      try (var _ =
          new Cleanups(
              withClass(AscensionClass.ACCORDION_THIEF), withSkill(SkillPool.BIND_SPICE_GHOST))) {
        maximize("item");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("cast 1 Bind Spice Ghost"))));
      }
    }

    @Test
    public void doesNotSuggestSpiceHazeForPastamancer() {
      try (var _ =
          new Cleanups(
              withClass(AscensionClass.PASTAMANCER), withSkill(SkillPool.BIND_SPICE_GHOST))) {
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
      try (var _ =
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
                  "Stench Resistance: +2, Combat Rate: -9, MP Regen Min: 10, MP Regen Max: 20"))) {
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
      try (var _ =
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
              withFamiliar(FamiliarPool.MOSQUITO))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.EELSKIN_HAT),
              withEquippableItem(ItemPool.EELSKIN_PANTS),
              withEquippableItem(ItemPool.EELSKIN_SHIELD),
              withLocation("The Ice Hole"))) {
        assertTrue(maximize("passive dmg -tie"));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "eelskin hat")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "eelskin pants")));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "eelskin shield")));
      }
    }

    @Test
    public void doesNotSuggestUnderwaterPassiveDamageIfNotUnderwater() {
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.EELSKIN_HAT),
              withEquippableItem(ItemPool.EELSKIN_PANTS),
              withEquippableItem(ItemPool.EELSKIN_SHIELD),
              withLocation("Noob Cave"))) {
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
      try (var _ = new Cleanups(withWitchess)) {
        assertTrue(maximize("familiar weight"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("witchess"))));
      }
    }

    @Test
    public void doesNotSuggestWitchessWhenOutOfStandard() {
      try (var _ =
          new Cleanups(
              withPath(Path.STANDARD),
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Witchess Set"),
              withWitchess)) {
        assertTrue(maximize("familiar weight"));
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("witchess")))));
      }
    }

    @Test
    public void suggestsWitchessWhenOutOfStandardForLegacyOfLoathing() {
      try (var _ =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withRestricted(true),
              withProperty("replicaWitchessSetAvailable", true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Witchess Set"),
              withWitchess)) {
        assertTrue(maximize("familiar weight"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("witchess"))));
      }
    }
  }

  @Nested
  class GreatestAmericanPants {
    @Test
    public void suggestsGap() {
      try (var _ = withEquipped(Slot.PANTS, ItemPool.GREAT_PANTS)) {
        assertTrue(maximize("item"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("gap vision"))));
      }
    }

    @Test
    public void suggestsReplicaGap() {
      try (var _ =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withEquipped(Slot.PANTS, ItemPool.REPLICA_GREAT_PANTS))) {
        assertTrue(maximize("hot res"));
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("gap structure"))));
      }
    }
  }

  @Nested
  class CardSleeve {
    @Test
    public void suggestCardSleeveSlot() {
      try (var _ =
          new Cleanups(
              withEquippableItem("card sleeve"),
              withEquippableItem("sturdy cane"),
              withEquippableItem("Alice's Army Foil Lanceman"))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.CARD_SLEEVE),
              withEquippableItem("Alice's Army Sniper"))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.FOLDER_HOLDER), withEquippableItem("Folder (red)"))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.STICKER_SWORD),
              withEquippableItem(ItemPool.UNICORN_STICKER))) {
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
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.COWBOY_BOOTS),
              withEquippableItem(ItemPool.MOUNTAIN_SKIN),
              withEquippableItem(ItemPool.QUICKSILVER_SPURS))) {
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
      try (var _ =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withItemInStorage(ItemPool.POWERFUL_GLOVE),
              withInteractivity(false))) {
        maximizeAny("hp");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("pull")))));
      }
    }

    @Test
    public void shouldNotSuggestPullingFreePullsInLegacyOfLoathingHardcore() {
      try (var _ =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withItemInFreepulls(ItemPool.RETROSPECS),
              withHardcore(),
              withInteractivity(false))) {
        maximizeAny("mus");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("free pull")))));
      }
    }
  }

  @Nested
  class Mcd {
    @Test
    public void doesNotSuggestMcdIfSignless() {
      try (var _ = withSign(ZodiacSign.NONE)) {
        maximize("ml");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("mcd")))));
      }
    }

    @Test
    public void suggestsMcdWhenBoostingML() {
      try (var _ = withSign(ZodiacSign.MONGOOSE)) {
        maximize("ml");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("mcd 10"))));
      }
    }

    @Test
    public void suggestsTurningOffMcdWithNegativeML() {
      try (var _ = new Cleanups(withSign(ZodiacSign.MONGOOSE), withMCD(5))) {
        maximize("-ml");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("mcd 0"))));
      }
    }

    @Test
    public void suggestsMcdElevenWhenCanadiaSign() {
      try (var _ = withSign(ZodiacSign.MARMOT)) {
        maximize("ml");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("mcd 11"))));
      }
    }
  }

  @Nested
  class AprilBand {
    @Test
    public void recommendsAprilBand() {
      try (var _ = withItem(ItemPool.APRILING_BAND_HELMET)) {
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
      try (var _ =
          new Cleanups(withItem(ItemPool.MAYAM_CALENDAR), withProperty("_mayamSymbolsUsed", ""))) {
        maximize("food drop");

        assertThat(
            getBoosts(),
            hasItem(hasProperty("cmd", is("mayam resonance memories of cheesier age"))));
      }
    }

    @Test
    public void doesNotRecommendMayamWithoutItem() {
      try (var _ = withProperty("_mayamSymbolsUsed", "")) {
        maximize("food drop");

        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("mayam ")))));
      }
    }

    @Test
    public void doesNotRecommendMayamIfUsed() {
      try (var _ = withProperty("_mayamSymbolsUsed", "yam1,yam2,cheese,clock")) {
        maximize("food drop");

        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("mayam ")))));
      }
    }
  }

  @Nested
  class CampAway {
    @Test
    public void recommendsCampAwayCloud() {
      try (var _ =
          new Cleanups(
              withProperty("getawayCampsiteUnlocked", true),
              withProperty("_campAwayCloudBuffs", 0),
              withProperty("_campAwaySmileBuffs", 0))) {
        maximize("Muscle Experience Percent");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", is("campaway cloud"))));
      }
    }

    @Test
    public void doesNotRecommendCampAwayCloudIfUsed() {
      try (var _ =
          new Cleanups(
              withProperty("getawayCampsiteUnlocked", true),
              withProperty("_campAwayCloudBuffs", 1),
              withProperty("_campAwaySmileBuffs", 0))) {
        maximize("Muscle Experience Percent");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", is("campaway cloud")))));
      }
    }

    @Test
    public void doesNotRecommendCampAwayCloudIfNotOwned() {
      try (var _ =
          new Cleanups(
              withProperty("getawayCampsiteUnlocked", false),
              withProperty("_campAwayCloudBuffs", 0),
              withProperty("_campAwaySmileBuffs", 0))) {
        maximize("Muscle Experience Percent");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", is("campaway cloud")))));
      }
    }

    @Test
    public void recommendsCampAwayCloudInOldPath() {
      try (var _ =
          new Cleanups(
              withRestricted(false),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Distant Woods Getaway Brochure"),
              withProperty("getawayCampsiteUnlocked", true),
              withProperty("_campAwayCloudBuffs", 0),
              withProperty("_campAwaySmileBuffs", 0))) {
        maximize("Muscle Experience Percent");
        assertThat(getBoosts(), hasItem(hasProperty("cmd", is("campaway cloud"))));
      }
    }

    @Test
    public void doesNotRecommendCampAwayCloudIfUnderStandardRestriction() {
      try (var _ =
          new Cleanups(
              withRestricted(true),
              withNotAllowedInStandard(RestrictedItemType.ITEMS, "Distant Woods Getaway Brochure"),
              withProperty("getawayCampsiteUnlocked", true),
              withProperty("_campAwayCloudBuffs", 0),
              withProperty("_campAwaySmileBuffs", 0))) {
        maximize("Muscle Experience Percent");
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", is("campaway cloud")))));
      }
    }
  }

  @Nested
  class StinkyCheese {
    @Test
    public void weightsStinkyCheese() {
      try (var _ =
          new Cleanups(withItem(ItemPool.STINKY_CHEESE_SWORD), withItem(ItemPool.JUNE_CLEAVER))) {
        maximize("10stinky cheese, 5bonus June cleaver, -pants -offhand -acc1 -acc2 -acc3");
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "stinky cheese sword")));
        assertEquals(1, modFor(BitmapModifier.STINKYCHEESE), 0.01);
      }
    }

    @Test
    public void doesNotOvercountSameItem() {
      try (var _ =
          new Cleanups(
              withItem(ItemPool.STINKY_CHEESE_SWORD, 7),
              withEquippableItem(ItemPool.FLASH_LIQUIDIZER_ULTRA_DOUSING_ACCESSORY, 3))) {
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
    try (var _ =
        new Cleanups(
            withEquippableItem(ItemPool.PRISMATIC_BERET),
            withEquippableItem(ItemPool.GINGERBREAD_MASK),
            withEquipped(ItemPool.GREAT_WOLFS_BEASTLY_TROUSERS))) {
      maximize("hat drop");
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "prismatic beret")));
      assertEquals(31, modFor(DoubleModifier.HATDROP), 0.01);
    }
  }

  @Nested
  class Wishable {
    @Test
    public void recommendsOnlyWishableEffects() {
      try (var _ = new Cleanups(withItem(ItemPool.POCKET_WISH))) {
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
      try (var _ =
          new Cleanups(
              withItem(ItemPool.CURSED_MONKEY_PAW),
              withProperty("_monkeyPawWishesUsed", 3),
              withProperty("verboseMaximizer", true))) {
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

    @Test
    public void acquiresWishIfItIsMallBuyable() {
      try (var _ =
          new Cleanups(withProperty("autoSatisfyWithMall", true), withInteractivity(true))) {
        maximize("-combat");
        var boosts = getBoosts();
        assertThat(
            boosts,
            hasItem(
                hasProperty(
                    "cmd",
                    startsWith(
                        "acquire 1 \u00B6"
                            + ItemPool.POCKET_WISH
                            + ";genie effect Disquiet Riot"))));
      }
    }

    @Test
    public void acquiresAlliedRadioIfItIsMallBuyable() {
      try (var _ =
          new Cleanups(withProperty("autoSatisfyWithMall", true), withInteractivity(true))) {
        maximize("item");
        var boosts = getBoosts();
        assertThat(
            boosts,
            hasItem(
                hasProperty(
                    "cmd",
                    startsWith(
                        "acquire 1 \u00B6"
                            + ItemPool.HANDHELD_ALLIED_RADIO
                            + ";alliedradio effect intel"))));
      }
    }
  }

  @Nested
  class Holiday {
    @Test
    public void recommendsCrystallizedSpiceInAutumn() {
      try (var _ =
          new Cleanups(
              withItem(ItemPool.CRYSTALLIZED_PUMPKIN_SPICE), withDay(2025, Month.OCTOBER, 11))) {
        maximize("item");
        var boosts = getBoosts();
        assertThat(
            boosts, hasItem(hasProperty("cmd", equalTo("use 1 crystallized pumpkin spice"))));
      }
    }

    @Test
    public void doesNotRecommendCrystallizedSpiceOutsideAutumn() {
      try (var _ =
          new Cleanups(
              withItem(ItemPool.CRYSTALLIZED_PUMPKIN_SPICE), withDay(2025, Month.DECEMBER, 11))) {
        maximize("item");
        var boosts = getBoosts();
        assertThat(
            boosts, not(hasItem(hasProperty("cmd", equalTo("use 1 crystallized pumpkin spice")))));
      }
    }

    @Test
    public void recommendsM242OnDependenceDay() {
      try (var _ =
          new Cleanups(
              withItem(ItemPool.M282), withDay(2025, Month.OCTOBER, 30), withMuscle(100))) {
        maximize("muscle");
        var boosts = getBoosts();
        assertThat(boosts, hasItem(hasProperty("cmd", equalTo("use 1 M-242"))));
      }
    }

    @Test
    public void doesNotRecommendM242OutsideDependenceDay() {
      try (var _ =
          new Cleanups(
              withItem(ItemPool.M282), withDay(2025, Month.DECEMBER, 11), withMuscle(100))) {
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
      try (var _ =
          new Cleanups(
              withEquipped(ItemPool.TIME_SWORD),
              withEquipped(Slot.ACCESSORY1, ItemPool.EXTREME_AMULET),
              withEquipped(Slot.ACCESSORY2, ItemPool.EXTREME_AMULET),
              withEquipped(Slot.ACCESSORY3, ItemPool.EXTREME_AMULET),
              withEquippableItem(ItemPool.TIME_HALO))) {
        maximize("adv");
        var boosts = getBoosts();
        assertThat(boosts, hasItem(hasProperty("cmd", startsWith("unequip weapon"))));
        assertThat(boosts, hasItem(recommends(ItemPool.TIME_HALO)));
      }
    }

    @Test
    public void dontRecommendWorseUnarmed() {
      try (var _ =
          new Cleanups(
              withEquipped(Slot.ACCESSORY1, ItemPool.GOLD_WEDDING_RING),
              withEquipped(Slot.ACCESSORY2, ItemPool.TINY_PLASTIC_GOLDEN_GUNDAM),
              withEquipped(Slot.ACCESSORY3, ItemPool.TIME_HALO),
              withEquippableItem(ItemPool.TIME_SWORD),
              withEquippableItem(ItemPool.NOVELTY_MONORAIL_TICKET),
              withEquippableItem(ItemPool.TINY_PLASTIC_CRIMBO_REINDEER))) {
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
    try (var _ =
        new Cleanups(
            withEquippableItem(ItemPool.SILENT_NIGHTLIGHT),
            withEquippableItem(ItemPool.SPACEGATE_MILITARY_INSIGNIA),
            withEquippableItem(ItemPool.SPACEGATE_SCIENTIST_INSIGNIA),
            withEquippableItem(ItemPool.SHINY_HOOD_ORNAMENT, 3))) {
      maximize(
          "10.0 adv, 0.001 rollover effect duration, switch disembodied hand, switch left-hand man, -tie");
      assertThat(getBoosts(), hasItem(recommends(ItemPool.SPACEGATE_SCIENTIST_INSIGNIA)));
      assertThat(getBoosts(), hasItem(recommends(ItemPool.SPACEGATE_MILITARY_INSIGNIA)));
      assertThat(getBoosts(), hasItem(recommends(ItemPool.SILENT_NIGHTLIGHT)));
    }
  }

  @Test
  void shouldSuggestHolsteringIfAvailable() {
    try (var _ =
        new Cleanups(
            withClass(AscensionClass.COW_PUNCHER), withEquippableItem(ItemPool.CUSTOM_SIXGUN))) {
      maximize("muscle");
      assertThat(getBoosts(), hasItem(recommends(ItemPool.CUSTOM_SIXGUN)));
    }
  }

  @Nested
  class DamageReduction {
    @Test
    void maximizerCountsInnateShieldDamageReductionAndEnchant() {
      try (var _ =
          new Cleanups(
              withEquippableItem(ItemPool.OLD_SCHOOL_FLYING_DISC), // base 14, 10 enchant
              withEquippableItem(ItemPool.ASTRAL_SHIELD), // higher base: 15
              withEquippableItem(ItemPool.FURRY_YAM_BUCKLER) // higher enchant: 11
              )) {
        assertTrue(maximize("dr"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.OLD_SCHOOL_FLYING_DISC)));
        assertThat(modFor(DoubleModifier.DAMAGE_REDUCTION), equalTo(24.0));
      }
    }

    @Test
    void maximizerAddsShieldsWithNoBaseEnchants() {
      try (var _ = new Cleanups(withEquippableItem(ItemPool.FLAK_SHIELD))) {
        assertTrue(maximize("dr"));
        assertThat(getBoosts(), hasItem(recommends(ItemPool.FLAK_SHIELD)));
        assertThat(modFor(DoubleModifier.DAMAGE_REDUCTION), equalTo(9.0));
      }
    }
  }

  @Test
  void keepsCurrentEquipmentWhenCombinationLimitIsReached() {
    var watch = ItemPool.get("grandfather watch");
    try (var _ =
        new Cleanups(
            withItem("Boots of Twilight Whispers"),
            withEquipped(Slot.ACCESSORY1, "Elf Guard insignia (general)"),
            withEquipped(Slot.ACCESSORY2, watch),
            withEquipped(Slot.ACCESSORY3, ItemPool.THE_ETERNITY_CODPIECE),
            withEquipped(Slot.FAMILIAR, "solid shifting time weirdness"),
            withProperty("maximizerCombinationLimit", 1))) {
      double current = new Evaluator("adv").getScore(KoLCharacter.getCurrentModifiers());

      assertThat(maximize("adv"), is(true));
      assertThat(Maximizer.best.getScore(), greaterThanOrEqualTo(current));
      assertThat(
          SlotSet.ACCESSORY_SLOTS.stream().map(Maximizer.best.equipment::get).toList(),
          hasItem(watch));
    }
  }

  @Test
  void currentKeywordControlsWhetherEquippedItemsAreConsidered() {
    int alternative = ItemPool.get("bounty-hunting helmet").getItemId();
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10"),
            withOverrideModifiers(ModifierType.ITEM, alternative, "Item Drop: +20"),
            withEquipped(Slot.HAT, ItemPool.HELMET_TURTLE),
            withEquippableItem(alternative))) {
      assertTrue(maximize("item drop, -tie, current"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet")));
      int combinationsWithCurrent = Maximizer.bestChecked;

      assertTrue(maximize("item drop, -tie, -current"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet")));
      assertThat(combinationsWithCurrent, greaterThan(Maximizer.bestChecked));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "elemental damage | Cold Damage: +1",
        "any resistance | Cold Resistance: +1",
        "ele resistance | Cold Resistance: +1",
        "elemental resistance | Cold Resistance: +1",
        "organ capacity | Stomach Capacity: +1",
        "crit | Critical Hit Percent: +1",
        "spell crit | Spell Critical Percent: +1",
        "sprinkle | Sprinkle Drop: +1",
        "stomach | Stomach Capacity: +1",
        "liver | Liver Capacity: +1",
        "spleen | Spleen Capacity: +1",
        "ocrs | Random Monster Modifiers: +1",
        "weapon dmg percent | Weapon Damage Percent: +1",
        "organ | Stomach Capacity: +1",
        "mys exp perc | Mysticality Experience Percent: +1",
        "myst exp | Mysticality Experience: +1",
        "mystical perc | Mysticality Percent: +1",
        "mys exp | Mysticality Experience: +1",
        "mys perc | Mysticality Percent: +1",
        "mox exp perc | Moxie Experience Percent: +1",
        "mox exp | Moxie Experience: +1",
        "mox perc | Moxie Percent: +1",
        "critical | Critical Hit Percent: +1",
        "spell critical | Spell Critical Percent: +1",
        "\"item drop\" | Item Drop: +1"
      })
  void recognizesModifierAliases(String expression, String modifiers) {
    int alternative = ItemPool.get("bounty-hunting helmet").getItemId();
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, modifiers),
            withOverrideModifiers(ModifierType.ITEM, alternative, "Meat Drop: +100"),
            withEquippableItem(ItemPool.HELMET_TURTLE),
            withEquippableItem(alternative))) {
      assertTrue(maximize(expression + ", -tie"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      assertThat(getBoosts(), not(hasItem(recommends("bounty-hunting helmet"))));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"2 hand", "2 handed", "2 hands", "-tie", "-tiebreaker", "stinkycheese"})
  void recognizesDirectiveAliases(String directive) {
    assertTrue(maximize("item, " + directive));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "Cold Resistance: +1, Hot Resistance: +1, Sleaze Resistance: +1, Stench Resistance: +1 | Spooky Resistance: +3 | helmet turtle | bounty-hunting helmet",
        "Cold Resistance: +1, Hot Resistance: +1, Sleaze Resistance: +1 | Spooky Resistance: +4 | bounty-hunting helmet | helmet turtle"
      })
  void anyResistanceScoresTotalResistanceAcrossElements(
      String variedModifiers, String concentratedModifiers, String expected, String unexpected) {
    int alternative = ItemPool.get("bounty-hunting helmet").getItemId();
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, variedModifiers),
            withOverrideModifiers(ModifierType.ITEM, alternative, concentratedModifiers),
            withEquippableItem(ItemPool.HELMET_TURTLE),
            withEquippableItem(alternative))) {
      assertTrue(maximize("any resistance, -tie"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, expected)));
      assertThat(getBoosts(), not(hasItem(recommends(unexpected))));
    }
  }

  @Test
  void allResistanceModifierContributesToEveryElement() {
    int alternative = ItemPool.get("bounty-hunting helmet").getItemId();
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "All Resistance: +1"),
            withOverrideModifiers(ModifierType.ITEM, alternative, "Spooky Resistance: +4"),
            withEquippableItem(ItemPool.HELMET_TURTLE),
            withEquippableItem(alternative))) {
      assertTrue(maximize("any resistance, -tie"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      assertThat(getBoosts(), not(hasItem(recommends("bounty-hunting helmet"))));
    }
  }

  @ParameterizedTest
  @CsvSource({"any resistance, helmet turtle", "all resistance, bounty-hunting helmet"})
  void distinguishesAnyResistanceFromAllResistance(String expression, String expected) {
    int alternative = ItemPool.get("bounty-hunting helmet").getItemId();
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Cold Resistance: +6"),
            withOverrideModifiers(ModifierType.ITEM, alternative, "All Resistance: +1"),
            withEquippableItem(ItemPool.HELMET_TURTLE),
            withEquippableItem(alternative))) {
      assertTrue(maximize(expression + ", -tie"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, expected)));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {"utensil | pasta spoon", "knife | asparagus knife", "accordion | aerogel accordion"})
  void honorsWeaponRequirements(String expression, String itemName) {
    try (var _ = new Cleanups(withStats(100, 100, 100), withEquippableItem(itemName))) {
      assertTrue(maximize(expression + ", -tie"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, itemName)));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "type sword | lupine sword | seal-clubbing club",
        "2 hands | stone banjo | seal-clubbing club",
        "melee | seal-clubbing club | disco ball",
        "-melee | disco ball | seal-clubbing club"
      })
  void weaponQualifiersRestrictScoredRecommendations(
      String qualifier, String expected, String alternative) {
    int expectedId = ItemPool.get(expected).getItemId();
    int alternativeId = ItemPool.get(alternative).getItemId();
    try (var _ =
        new Cleanups(
            withStats(100, 100, 100),
            withOverrideModifiers(ModifierType.ITEM, expectedId, "Item Drop: +10"),
            withOverrideModifiers(ModifierType.ITEM, alternativeId, "Item Drop: +20"),
            withEquippableItem(expected),
            withEquippableItem(alternative))) {
      assertTrue(maximize("item, " + qualifier + ", -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, expected)));
    }
  }

  @ParameterizedTest
  @CsvSource({"ACCORDION_THIEF, true", "SEAL_CLUBBER, false"})
  void stolenAccordionRequirementRespectsClass(AscensionClass ascensionClass, boolean recommended) {
    try (var _ =
        new Cleanups(
            withClass(ascensionClass),
            withStats(100, 100, 100),
            withEquippableItem("stolen accordion"))) {
      assertTrue(maximize("accordion, -tie"));

      assertThat(
          getBoosts(),
          recommended
              ? hasItem(recommendsSlot(Slot.WEAPON, "stolen accordion"))
              : not(hasItem(recommends("stolen accordion"))));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "Scalp of Gorgolok | SEAL_CLUBBER | TURTLE_TAMER | muscle",
        "Elder Turtle Shell | TURTLE_TAMER | SEAL_CLUBBER | muscle",
        "Colander of Em-er'il | PASTAMANCER | SEAL_CLUBBER | mysticality",
        "Ancient Saucehelm | SAUCEROR | SEAL_CLUBBER | mysticality",
        "Disco 'Fro Pick | DISCO_BANDIT | SEAL_CLUBBER | moxie",
        "El Sombrero De Lopez | ACCORDION_THIEF | SEAL_CLUBBER | moxie"
      })
  void classRestrictedEquipmentIsRecommendedOnlyToItsClass(
      String itemName, AscensionClass requiredClass, AscensionClass otherClass, String expression) {
    try (var _ =
        new Cleanups(
            withClass(requiredClass), withStats(100, 100, 100), withEquippableItem(itemName))) {
      assertTrue(maximize(expression + ", -tie"));
      assertThat(getBoosts(), hasItem(recommends(itemName)));
    }

    try (var _ =
        new Cleanups(
            withClass(otherClass), withStats(100, 100, 100), withEquippableItem(itemName))) {
      assertTrue(maximize(expression + ", -tie"));
      assertThat(getBoosts(), not(hasItem(recommends(itemName))));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Cold", "Hot", "Sleaze", "Spooky", "Stench"})
  void elementalImmunityOutweighsOrdinaryStatsAndResistance(String element) {
    int alternative = ItemPool.get("bounty-hunting helmet").getItemId();
    try (var _ =
        new Cleanups(
            withOverrideModifiers(
                ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Muscle: +10, " + element + " Immunity"),
            withOverrideModifiers(
                ModifierType.ITEM, alternative, "Muscle: +50, " + element + " Resistance: +5"),
            withEquippableItem(ItemPool.HELMET_TURTLE),
            withEquippableItem(alternative))) {
      assertTrue(maximize("muscle, " + element.toLowerCase() + " resistance, -tie"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      assertThat(getBoosts(), not(hasItem(recommends("bounty-hunting helmet"))));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Cold", "Hot", "Sleaze", "Spooky", "Stench"})
  void elementalVulnerabilityOutweighsOrdinaryStats(String element) {
    int alternative = ItemPool.get("bounty-hunting helmet").getItemId();
    try (var _ =
        new Cleanups(
            withOverrideModifiers(
                ModifierType.ITEM,
                ItemPool.HELMET_TURTLE,
                "Muscle: +50, " + element + " Vulnerability"),
            withOverrideModifiers(
                ModifierType.ITEM, alternative, "Muscle: +10, " + element + " Resistance: +1"),
            withEquippableItem(ItemPool.HELMET_TURTLE),
            withEquippableItem(alternative))) {
      assertTrue(maximize("muscle, " + element.toLowerCase() + " resistance, -tie"));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet")));
      assertThat(getBoosts(), not(hasItem(recommends("helmet turtle"))));
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "nonsense | Unrecognized keyword: nonsense",
        "handicap | Unrecognized keyword: handicap",
        "tier | Unrecognized keyword: tier",
        "currently | Unrecognized keyword: currently",
        "letterhead | Unrecognized keyword: letterhead",
        "outfitter | Unrecognized keyword: outfitter",
        "mainline | Unrecognized keyword: mainline",
        "comedy | Unrecognized keyword: comedy",
        "advice | Unrecognized keyword: advice",
        "fiteswhatever | Unrecognized keyword: fiteswhatever",
        "organic | Unrecognized keyword: organic",
        "muscular | Unrecognized keyword: muscular",
        "mysterious | Unrecognized keyword: mysterious",
        "moxious | Unrecognized keyword: moxious",
        "itemized | Unrecognized keyword: itemized",
        "meatball | Unrecognized keyword: meatball",
        "expensive | Unrecognized keyword: expensive",
        "criticality | Unrecognized keyword: criticality",
        "sprinkler | Unrecognized keyword: sprinkler",
        "stomachache | Unrecognized keyword: stomachache",
        "liverish | Unrecognized keyword: liverish",
        "spleenful | Unrecognized keyword: spleenful",
        "cold residue | Unrecognized keyword: cold residue",
        "cold res foo | Unrecognized keyword: cold res foo",
        "weapon dmgx | Unrecognized keyword: weapon dmgx",
        "hand foo | Unrecognized keyword: hand foo",
        "hands foo | Unrecognized keyword: hands foo",
        "tie foo | Unrecognized keyword: tie foo",
        "tiebreaker foo | Unrecognized keyword: tiebreaker foo",
        "current foo | Unrecognized keyword: current foo",
        "type | Unrecognized keyword: type",
        "equip | Unrecognized keyword: equip",
        "bonus | Unrecognized keyword: bonus",
        "modbonus | Unrecognized keyword: modbonus",
        "switch | Unrecognized keyword: switch",
        "item,, | Unable to interpret: ,",
        "outfit not an outfit | Unknown or custom outfit: not an outfit",
        "outfit stinky cheese | Unknown or custom outfit: stinky cheese",
        "switch not a familiar | Unknown familiar: not a familiar"
      })
  void reportsInvalidExpressions(String expression, String error) {
    assertFalse(maximize(expression));
    assertThat(KoLmafia.lastMessage, is(error));
  }

  @Test
  void coldPlumberExplainsWhyItCannotRecommendEquipment() {
    try (var _ =
        new Cleanups(
            withClass(AscensionClass.PLUMBER),
            withPath(Path.PATH_OF_THE_PLUMBER),
            withEquippableItem("work boots"),
            withEquippableItem("frosty button"))) {
      assertFalse(maximize("cold plumber"));
      assertThat(KoLmafia.lastMessage, is("You don't have an appropriate flower to wield"));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "SEAL_CLUBBER, Silent Hunting, Nearly Silent Hunting",
    "TURTLE_TAMER, Nearly Silent Hunting, Silent Hunting"
  })
  void silentHunterRecommendationDependsOnCharacterClass(
      AscensionClass ascensionClass, String expected, String unavailable) {
    try (var _ = new Cleanups(withClass(ascensionClass), withSkill("Silent Hunter"))) {
      assertTrue(maximize("initiative, -tie"));

      assertThat(getBoosts(), hasItem(recommendsEffect(expected)));
      assertThat(getBoosts(), not(hasItem(recommendsEffect(unavailable))));
    }
  }

  @ParameterizedTest
  @CsvSource({"false, true", "true, false"})
  void noAdventuresPreferenceControlsAdventureCostEffects(
      boolean noAdventures, boolean recommended) {
    try (var _ =
        new Cleanups(
            withAdventuresLeft(3),
            withItem(ItemPool.GONG),
            withProperty("maximizerNoAdventures", noAdventures))) {
      assertTrue(maximize("item drop"));
      assertThat(
          getBoosts(),
          recommended
              ? hasItem(hasProperty("cmd", is("gong roach itemdrop")))
              : not(hasItem(hasProperty("cmd", is("gong roach itemdrop")))));
    }
  }

  @ParameterizedTest
  @CsvSource({"4, true", "5, false"})
  void dailyUsePreferenceControlsEffectSource(int buffsUsed, boolean recommended) {
    try (var _ =
        new Cleanups(
            withEquipped(Slot.PANTS, ItemPool.GREAT_PANTS), withProperty("_gapBuffs", buffsUsed))) {
      assertTrue(maximize("item drop"));
      assertThat(
          getBoosts(),
          recommended
              ? hasItem(hasProperty("cmd", is("gap vision")))
              : not(hasItem(hasProperty("cmd", is("gap vision")))));
    }
  }

  @Test
  void outfitWithoutANameKeepsTheCurrentlyWornOutfit() {
    try (var _ =
        new Cleanups(
            withOutfit(OutfitPool.WAR_FRAT_OUTFIT),
            withAdjustmentsRecalculated(),
            withEquippableItem("bounty-hunting helmet"),
            withEquippableItem("Pantsgiving"),
            withEquippableItem("lucky gold ring"))) {
      assertTrue(
          maximize(
              "+outfit, 100 bonus bounty-hunting helmet, 100 bonus Pantsgiving, 100 bonus lucky gold ring, -tie"));

      assertThat(getBoosts(), hasItem(hasToString(containsString("keep hat: beer helmet"))));
      assertThat(
          getBoosts(), hasItem(hasToString(containsString("keep pants: distressed denim pants"))));
      assertThat(
          getBoosts(), hasItem(hasToString(containsString("keep acc1: bejeweled pledge pin"))));
    }
  }

  @Test
  void outfitWithoutANameDoesNotForceAnOutfitWhenNoneIsWorn() {
    try (var _ =
        new Cleanups(
            withEquipped(Slot.HAT, "helmet turtle"),
            withEquipped(Slot.PANTS, "old sweatpants"),
            withEquipped(Slot.ACCESSORY1, "gold wedding ring"))) {
      assertThat(KoLCharacter.currentStringModifier(StringModifier.OUTFIT), is(""));
      assertFalse(maximize("+outfit, -tie"));
    }
  }

  @Test
  void negativeSwitchForSameFamiliarDoesNotCancelPositiveSwitch() {
    try (var _ = withFamiliarInTerrarium(FamiliarPool.BABY_GRAVY_FAIRY)) {
      assertTrue(maximize("switch Baby Gravy Fairy, -switch Baby Gravy Fairy, item drop"));

      assertThat(getBoosts(), hasItem(hasProperty("cmd", is("familiar Baby Gravy Fairy"))));
    }
  }

  @Test
  void negativeFamiliarSwitchIsUsedWhenPositiveSwitchIsUnavailable() {
    try (var _ =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.TRICK_TOT),
            withItem(ItemPool.SOLID_SHIFTING_TIME_WEIRDNESS))) {
      assertTrue(maximize("adv, switch Left-Hand Man, -switch Trick-or-Treating Tot"));

      assertThat(getBoosts(), hasItem(hasProperty("cmd", is("familiar Trick-or-Treating Tot"))));
    }
  }

  @Test
  void positiveFamiliarSwitchTakesPriorityOverNegativeSwitch() {
    try (var _ =
        new Cleanups(
            withFamiliarInTerrarium(FamiliarPool.LEFT_HAND),
            withFamiliarInTerrarium(FamiliarPool.TRICK_TOT),
            withItem(ItemPool.SOLID_SHIFTING_TIME_WEIRDNESS))) {
      assertTrue(maximize("adv, switch Left-Hand Man, -switch Trick-or-Treating Tot"));

      assertThat(getBoosts(), hasItem(hasProperty("cmd", is("familiar Left-Hand Man"))));
    }
  }

  @Test
  void weightedPositiveSwitchCannotUseAnUnownedFamiliar() {
    assertTrue(maximize("2 switch Baby Gravy Fairy, item drop"));

    assertThat(getBoosts(), not(hasItem(hasProperty("cmd", is("familiar Baby Gravy Fairy")))));
  }

  @Test
  void moxiePlumberPrefersFancyBoots() {
    try (var _ =
        new Cleanups(
            withClass(AscensionClass.PLUMBER),
            withPath(Path.PATH_OF_THE_PLUMBER),
            withStats(10, 10, 20),
            withEquippableItem("fancy boots"),
            withEquippableItem("work boots"))) {
      assertTrue(maximize("plumber, moxie, -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.ACCESSORY1, "fancy boots")));
    }
  }

  @Test
  void recommendsWeaponAndOffhandSynergy() {
    try (var _ =
        new Cleanups(
            withStats(100, 100, 100),
            withEquippableItem("lupine sword"),
            withEquippableItem("snarling wolf shield"))) {
      assertTrue(maximize("spooky damage, -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "lupine sword")));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "snarling wolf shield")));
    }
  }

  @Test
  void recommendsThreeAccessorySynergy() {
    try (var _ =
        new Cleanups(
            withStats(100, 100, 100),
            withEquippableItem("monstrous monocle"),
            withEquippableItem("musty moccasins"),
            withEquippableItem("molten medallion"),
            withEquippableItem("gold detective badge"))) {
      assertTrue(maximize("item drop, -tie"));

      assertThat(getBoosts(), hasItem(recommends("monstrous monocle")));
      assertThat(getBoosts(), hasItem(recommends("musty moccasins")));
      assertThat(getBoosts(), hasItem(recommends("molten medallion")));
      assertThat(getBoosts(), not(hasItem(recommends("gold detective badge"))));
    }
  }

  @ParameterizedTest
  @CsvSource({"DISCO_BANDIT, true", "SEAL_CLUBBER, false"})
  void doubleBarreledAvailabilityDependsOnClass(AscensionClass ascensionClass, boolean available) {
    try (var _ =
        new Cleanups(withClass(ascensionClass), withProperty("barrelShrineUnlocked", true))) {
      assertTrue(maximize("ranged damage percent, -tie"));

      assertThat(
          getBoosts(),
          available
              ? hasItem(hasProperty("cmd", is("barrelprayer buff")))
              : not(hasItem(hasProperty("cmd", is("barrelprayer buff")))));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "Extra-Loud Muffler, combat, Unmuffled, Muffled",
    "Extra-Quiet Muffler, -combat, Muffled, Unmuffled"
  })
  void motorbikeMufflerControlsRevEngineEffect(
      String muffler, String expression, String expected, String unavailable) {
    try (var _ =
        new Cleanups(
            withClass(AscensionClass.AVATAR_OF_SNEAKY_PETE),
            withPath(Path.AVATAR_OF_SNEAKY_PETE),
            withSkill("Rev Engine"),
            withProperty("peteMotorbikeMuffler", muffler))) {
      assertTrue(maximize(expression + ", -tie"));

      assertThat(getBoosts(), hasItem(recommendsEffect(expected)));
      assertThat(getBoosts(), not(hasItem(recommendsEffect(unavailable))));
    }
  }

  @Test
  void turtleTamerCanChangeBlessing() {
    try (var _ =
        new Cleanups(
            withClass(AscensionClass.TURTLE_TAMER), withSkill("Blessing of She-Who-Was"))) {
      assertTrue(maximize("mysticality, -tie"));

      assertThat(getBoosts(), hasItem(recommendsEffect("Blessing of She-Who-Was")));
    }
  }

  @Test
  void turtleTamerCanGainBoonMatchingCurrentBlessing() {
    try (var _ =
        new Cleanups(
            withClass(AscensionClass.TURTLE_TAMER),
            withSkill("Spirit Boon"),
            withEffect("Blessing of She-Who-Was"))) {
      assertTrue(maximize("weapon damage, -tie"));

      assertThat(getBoosts(), hasItem(recommendsEffect("Boon of She-Who-Was")));
    }
  }

  @Test
  void turtleTamerCanBecomeAvatarFromGloriousBlessing() {
    try (var _ =
        new Cleanups(
            withClass(AscensionClass.TURTLE_TAMER),
            withSkill("Turtle Power"),
            withEffect("Glorious Blessing of She-Who-Was"))) {
      assertTrue(maximize("spell damage percent, -tie"));

      assertThat(getBoosts(), hasItem(recommendsEffect("Avatar of She-Who-Was")));
    }
  }

  @Test
  void crownAndBjornUseDifferentFamiliars() {
    try (var _ =
        new Cleanups(
            withEquippableItem("Crown of Thrones"),
            withEquippableItem("Buddy Bjorn"),
            withFamiliarInTerrarium(FamiliarPool.LOBSTER),
            withFamiliarInTerrarium(FamiliarPool.GALLOPING_GRILL))) {
      assertTrue(maximize("spell damage, -tie"));

      assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("enthrone Galloping Grill"))));
      assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("bjornify Rock Lobster"))));
    }
  }

  @Nested
  class HardcorePathEquipment {
    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "Boris's Helm | AVATAR_OF_BORIS | AVATAR_OF_BORIS",
          "right bear arm | ZOMBIE_SLAYER | ZOMBIE_MASTER",
          "Jarlsberg's pan | AVATAR_OF_JARLSBERG | AVATAR_OF_JARLSBERG",
          "Sneaky Pete's leather jacket | AVATAR_OF_SNEAKY_PETE | AVATAR_OF_SNEAKY_PETE",
          "Thor's Pliers | HEAVY_RAINS | SEAL_CLUBBER",
          "The Crown of Ed the Undying | ACTUALLY_ED_THE_UNDYING | ED"
        })
    void unavailableOutsideItsPath(String itemName, Path path, AscensionClass ascensionClass) {
      int itemId = ItemPool.get(itemName).getItemId();
      try (var _ =
          new Cleanups(
              withClass(AscensionClass.SEAL_CLUBBER),
              withHardcore(),
              withSkill("Torso Awareness"),
              withStats(1000, 1000, 1000),
              withOverrideModifiers(ModifierType.ITEM, itemId, "Item Drop: +100"),
              withEquippableItem(itemId))) {
        assertTrue(maximize("item drop, -tie"));
        assertThat(getBoosts(), not(hasItem(recommends(itemName))));
      }
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "Boris's Helm | AVATAR_OF_BORIS | AVATAR_OF_BORIS",
          "right bear arm | ZOMBIE_SLAYER | ZOMBIE_MASTER",
          "Jarlsberg's pan | AVATAR_OF_JARLSBERG | AVATAR_OF_JARLSBERG",
          "Sneaky Pete's leather jacket | AVATAR_OF_SNEAKY_PETE | AVATAR_OF_SNEAKY_PETE",
          "Thor's Pliers | HEAVY_RAINS | SEAL_CLUBBER",
          "The Crown of Ed the Undying | ACTUALLY_ED_THE_UNDYING | ED"
        })
    void availableInItsPath(String itemName, Path path, AscensionClass ascensionClass) {
      int itemId = ItemPool.get(itemName).getItemId();
      try (var _ =
          new Cleanups(
              withPath(path),
              withClass(ascensionClass),
              withHardcore(),
              withSkill("Torso Awareness"),
              withStats(1000, 1000, 1000),
              withOverrideModifiers(ModifierType.ITEM, itemId, "Item Drop: +100"),
              withEquippableItem(itemId))) {
        assertTrue(maximize("item drop, -tie"));
        assertThat(getBoosts(), hasItem(recommends(itemName)));
      }
    }
  }

  @Nested
  class GarbageShirt {
    @Test
    void chargedGarbageShirtBeatsAHigherUnchargedExperienceModifier() {
      int alternative = ItemPool.get("astral shirt").getItemId();
      try (var _ =
          new Cleanups(
              withSkill("Torso Awareness"),
              withProperty("garbageShirtCharge", 1),
              withOverrideModifiers(ModifierType.ITEM, alternative, "Experience: +4"),
              withEquippableItem(alternative),
              withEquippableItem(ItemPool.MAKESHIFT_GARBAGE_SHIRT))) {
        assertTrue(maximize("experience, -tie"));

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "makeshift garbage shirt")));
      }
    }

    @Test
    void dischargedGarbageShirtLosesToAHigherExperienceModifier() {
      int alternative = ItemPool.get("astral shirt").getItemId();
      try (var _ =
          new Cleanups(
              withSkill("Torso Awareness"),
              withProperty("garbageShirtCharge", 0),
              withProperty("_garbageItemChanged", true),
              withOverrideModifiers(ModifierType.ITEM, alternative, "Experience: +4"),
              withEquippableItem(alternative),
              withEquippableItem(ItemPool.MAKESHIFT_GARBAGE_SHIRT))) {
        assertTrue(maximize("experience, -tie"));

        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.SHIRT, "astral shirt")));
      }
    }
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "Mad Hatrack | asbestos helmet turtle | bounty-hunting helmet",
        "Fancypants Scarecrow | swashbuckling pants | Pantsgiving"
      })
  void familiarCanWearItsSpecialEquipment(String familiarName, String itemName, String forcedItem) {
    int itemId = ItemPool.get(itemName).getItemId();
    int forcedItemId = ItemPool.get(forcedItem).getItemId();
    try (var _ =
        new Cleanups(
            withFamiliar(FamiliarDatabase.getFamiliarId(familiarName), 400),
            withOverrideModifiers(ModifierType.ITEM, forcedItemId, "Item Drop: +20"),
            withEquippableItem(itemId),
            withEquippableItem(forcedItemId))) {
      assertTrue(maximize("item drop, +equip " + forcedItem + ", -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.FAMILIAR, itemName)));
    }
  }

  @Test
  void recommendationIncludesClosetRetrievalCommand() {
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10"),
            withProperty("autoSatisfyWithCloset", true),
            withInteractivity(true),
            withItemInCloset(ItemPool.HELMET_TURTLE))) {
      maximizeAny("item drop, -tie");

      assertThat(
          getBoosts(),
          hasItem(
              hasProperty(
                  "cmd",
                  startsWith("closet take 1 \u00B6" + ItemPool.HELMET_TURTLE + ";equip hat"))));
    }
  }

  @Test
  void recommendationIncludesStashRetrievalCommand() {
    try (var _ = withClan(1, "Test Clan")) {
      boolean hadClan = KoLCharacter.hasClan();
      KoLCharacter.setClan(true);
      ClanManager.setStashRetrieved();

      try (var _ =
          new Cleanups(
              new Cleanups(() -> KoLCharacter.setClan(hadClan)),
              withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10"),
              withProperty("autoSatisfyWithStash", true),
              withInteractivity(true),
              withItemInStash("helmet turtle"))) {
        maximizeAny("item drop, -tie");

        assertThat(
            getBoosts(),
            hasItem(
                hasProperty(
                    "cmd",
                    startsWith("stash take 1 \u00B6" + ItemPool.HELMET_TURTLE + ";equip hat"))));
      }
    }
  }

  @Test
  void mallRecommendationIncludesAcquisitionText() {
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10"),
            withProperty("autoSatisfyWithMall", true),
            withInteractivity(true))) {
      maximizeAny("item drop, +equip helmet turtle, -tie");

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      assertThat(
          getBoosts(), hasItem(hasToString(startsWith("acquire & equip hat helmet turtle"))));
    }
  }

  @Test
  void recommendationIncludesPullCommand() {
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10"),
            withInteractivity(false),
            withRonin(true),
            withItemInStorage(ItemPool.HELMET_TURTLE))) {
      maximizeAny("item drop, +equip helmet turtle, -tie");

      assertThat(
          getBoosts(),
          hasItem(
              hasProperty(
                  "cmd", startsWith("pull \u00B6" + ItemPool.HELMET_TURTLE + ";equip hat"))));
    }
  }

  @Test
  void recommendationAcquiresAndFoldsAccessibleEquipment() {
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.TURTLE_WAX_HELMET, "Item Drop: +10"),
            withProperty("autoSatisfyWithCloset", true),
            withProperty("maximizerFoldables", true),
            withStats(100, 100, 100),
            withInteractivity(true),
            withItemInCloset(ItemPool.TURTLE_WAX_GREAVES))) {
      maximizeAny("item drop, +equip turtle wax helmet, -tie");

      assertThat(
          getBoosts(),
          hasItem(
              hasProperty(
                  "cmd",
                  startsWith(
                      "acquire 1 \u00B6"
                          + ItemPool.TURTLE_WAX_GREAVES
                          + ";fold \u00B6"
                          + ItemPool.TURTLE_WAX_HELMET
                          + ";equip hat"))));
    }
  }

  @Test
  void recommendationPullsAndFoldsStoredEquipment() {
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.TURTLE_WAX_HELMET, "Item Drop: +10"),
            withProperty("maximizerFoldables", true),
            withStats(100, 100, 100),
            withInteractivity(false),
            withRonin(true),
            withItemInStorage(ItemPool.TURTLE_WAX_GREAVES))) {
      maximizeAny("item drop, +equip turtle wax helmet, -tie");

      assertThat(
          getBoosts(),
          hasItem(
              hasProperty(
                  "cmd",
                  startsWith(
                      "pull 1 \u00B6"
                          + ItemPool.TURTLE_WAX_GREAVES
                          + ";fold \u00B6"
                          + ItemPool.TURTLE_WAX_HELMET
                          + ";equip hat"))));
    }
  }

  @Test
  void recommendationBuysToStorageAndPullsEquipment() {
    try (var _ =
        new Cleanups(
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10"),
            withProperty("autoSatisfyWithMall", true),
            withInteractivity(false),
            withRonin(true))) {
      maximizeAny("item drop, +equip helmet turtle, -tie");

      assertThat(
          getBoosts(),
          hasItem(
              hasProperty(
                  "cmd",
                  startsWith(
                      "buy using storage 1 \u00B6"
                          + ItemPool.HELMET_TURTLE
                          + ";pull \u00B6"
                          + ItemPool.HELMET_TURTLE
                          + ";equip hat"))));
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"Drops Items", "Drops Meat"})
  void defaultTiebreakerPrefersSpecialEquipment(String specialModifier) {
    try (var _ =
        new Cleanups(
            withOverrideModifiers(
                ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10, " + specialModifier),
            withOverrideModifiers(
                ModifierType.ITEM,
                ItemPool.get("bounty-hunting helmet").getItemId(),
                "Item Drop: +10"),
            withEquippableItem(ItemPool.HELMET_TURTLE),
            withEquippableItem("bounty-hunting helmet"))) {
      assertTrue(maximize("item drop"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
    }
  }

  @Test
  void defaultTiebreakerPrefersEquipmentWithARolloverEffect() {
    int oldSweatpants = ItemPool.OLD_SWEATPANTS;
    try (var _ =
        new Cleanups(
            withOverrideModifiers(
                ModifierType.ITEM,
                oldSweatpants,
                "Adventures: +2, PvP Fights: +2, Damage Absorption: +81"),
            withEquippableItem("ninjammies"),
            withEquippableItem(oldSweatpants))) {
      assertTrue(maximize("1 bonus ninjammies, 1 bonus old sweatpants"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.PANTS, "ninjammies")));
    }
  }

  @Test
  void doubleFistedSkillCanPutRangedWeaponsInBothHands() {
    try (var _ =
        new Cleanups(
            withSkill("Double-Fisted Skull Smashing"),
            withOverrideModifiers(
                ModifierType.ITEM, ItemPool.get("disco ball").getItemId(), "Item Drop: +10"),
            withEquippableItem("disco ball", 2))) {
      assertTrue(maximize("item drop, -tie"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.WEAPON, "disco ball")));
      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.OFFHAND, "disco ball")));
    }
  }

  @Test
  void hatTrickDoesNotRecommendTheNormalHatSlot() {
    try (var _ =
        new Cleanups(
            withClass(AscensionClass.SEAL_CLUBBER),
            withPath(Path.HAT_TRICK),
            withOverrideModifiers(ModifierType.ITEM, ItemPool.HELMET_TURTLE, "Item Drop: +10"),
            withEquippableItem(ItemPool.HELMET_TURTLE))) {
      assertTrue(maximize("item drop, -tie"));

      assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
    }
  }

  @Test
  void speculativeSearchLeavesEquippedItemsUnchanged() {
    var equipped = ItemPool.get("helmet turtle");
    try (var _ =
        new Cleanups(
            withEquipped(Slot.HAT, equipped), withEquippableItem("bounty-hunting helmet"))) {
      assertTrue(maximize("item"));

      assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "bounty-hunting helmet")));
      assertThat(EquipmentManager.getEquipment(Slot.HAT), is(equipped));
    }
  }

  @Test
  void emptyKeywordRecommendsKeepingOccupiedSlots() {
    try (var _ = new Cleanups(withEquipped(Slot.HAT, "helmet turtle"))) {
      assertTrue(maximize("empty"));
      assertThat(getBoosts(), contains(hasToString(containsString("keep hat: helmet turtle"))));
    }
  }
}
