package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withMP;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withRonin;
import static internal.helpers.Player.withSkill;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class EffectSourceDispatcherTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("EffectSourceDispatcherTest");
    Preferences.reset("EffectSourceDispatcherTest");
  }

  private static EffectSourceDispatcher.Context context(boolean includeAll, boolean haveVipKey) {
    return context(EffectPool.LEASH_OF_LINGUINI, includeAll, haveVipKey);
  }

  private static EffectSourceDispatcher.Context context(
      int effectId, boolean includeAll, boolean haveVipKey) {
    return context(
        effectId,
        includeAll,
        haveVipKey,
        EquipScope.SPECULATE_INVENTORY,
        20_000,
        PriceLevel.DONT_CHECK);
  }

  private static EffectSourceDispatcher.Context context(
      int effectId,
      boolean includeAll,
      boolean haveVipKey,
      EquipScope equipScope,
      int maxPrice,
      PriceLevel priceLevel) {
    var effect = EffectPool.get(effectId);
    return new EffectSourceDispatcher.Context(
        effect.getName(),
        effect,
        effectId,
        includeAll,
        equipScope,
        maxPrice,
        priceLevel,
        KoLCharacter.getLimitMode(),
        haveVipKey);
  }

  private static Cleanups withStorageMeat(long meat) {
    long previous = KoLCharacter.getStorageMeat();
    KoLCharacter.setStorageMeat(meat);
    return new Cleanups(() -> KoLCharacter.setStorageMeat(previous));
  }

  @Nested
  class Dispatch {
    @ParameterizedTest
    @ValueSource(
        strings = {
          "use",
          "gong",
          "cast",
          "synthesize",
          "cargo",
          "friars",
          "hatter",
          "mom",
          "summon",
          "concert",
          "telescope",
          "skeleton",
          "monorail",
          "loathingidol",
          "aprilband",
          "mayam",
          "styx",
          "skate",
          "gap",
          "alliedradio",
          "monkeypaw",
          "genie"
        })
    void preservesBareCommandsThatOnlyShareAHandledFamily(String command) {
      var plan = EffectSourceDispatcher.dispatch(command, command, context(false, false));

      assertThat(plan.skip, is(false));
      assertThat(plan.command, is(command));
      assertThat(plan.text, is(command));
      assertThat(plan.item, nullValue());
      assertThat(plan.price, is(0L));
      assertThat(plan.mpCost, is(0L));
      assertThat(plan.usesRemaining, is(0L));
      assertThat(plan.adventureCost, is(0));
      assertThat(plan.fullnessCost, is(0));
      assertThat(plan.inebrietyCost, is(0));
      assertThat(plan.spleenCost, is(0));
      assertThat(plan.soulsauceCost, is(0));
      assertThat(plan.thunderCost, is(0));
      assertThat(plan.rainCost, is(0));
      assertThat(plan.lightningCost, is(0));
      assertThat(plan.fuelCost, is(0));
      assertThat(plan.hpCost, is(0));
      assertThat(plan.duration, is(0));
      assertThat(plan.itemsRemaining, is(0));
      assertThat(plan.itemsCreatable, is(0));
    }

    @Test
    void omitsOrExplainsAMissingPaw() {
      try (var cleanups =
          new Cleanups(
              withRestricted(false),
              withItem(ItemPool.CURSED_MONKEY_PAW, 0),
              withProperty("_monkeyPawWishesUsed", 0))) {
        var omitted =
            EffectSourceDispatcher.dispatch(
                "monkeypaw effect Disquiet Riot",
                "monkeypaw effect Disquiet Riot",
                context(false, false));
        var explained =
            EffectSourceDispatcher.dispatch(
                "monkeypaw effect Disquiet Riot",
                "monkeypaw effect Disquiet Riot",
                context(true, false));

        assertThat(omitted.skip, is(true));
        assertThat(explained.skip, is(false));
        assertThat(explained.command, is(""));
        assertThat(explained.text, containsString("acquire a cursed monkey's paw"));
        assertThat(explained.duration, is(30));
        assertThat(explained.usesRemaining, is(5L));
      }
    }

    @Test
    void disablesAnExhaustedPawWithoutClaimingItIsMissing() {
      try (var cleanups =
          new Cleanups(
              withRestricted(false),
              withItem(ItemPool.CURSED_MONKEY_PAW),
              withProperty("_monkeyPawWishesUsed", 5))) {
        var plan =
            EffectSourceDispatcher.dispatch(
                "monkeypaw effect Disquiet Riot",
                "monkeypaw effect Disquiet Riot",
                context(true, false));

        assertThat(plan.skip, is(false));
        assertThat(plan.command, is(""));
        assertThat(plan.text, is("monkeypaw effect Disquiet Riot"));
        assertThat(plan.usesRemaining, is(0L));
      }
    }

    @Test
    void distinguishesHiddenAndExplainedClanAccess() {
      try (var cleanups =
          new Cleanups(
              withPath(Path.NONE),
              withRestricted(false),
              withInteractivity(true),
              withProperty("_poolGames", 0))) {
        var omitted =
            EffectSourceDispatcher.dispatch("pool stylish", "pool stylish", context(false, false));
        var explained =
            EffectSourceDispatcher.dispatch("pool stylish", "pool stylish", context(true, false));

        assertThat(omitted.skip, is(true));
        assertThat(explained.skip, is(false));
        assertThat(explained.command, is(""));
        assertThat(explained.text, is("( get access to the VIP lounge )"));
        assertThat(explained.duration, is(10));
        assertThat(explained.usesRemaining, is(3L));
      }
    }

    @Test
    void capturesCostsAndDurationForKnownSkills() {
      try (var cleanups =
          new Cleanups(
              withRestricted(false),
              withSkill(SkillPool.LEASH_OF_LINGUINI),
              withMP(100, 100, 100))) {
        var plan =
            EffectSourceDispatcher.dispatch(
                "cast 1 Leash of Linguini",
                "cast 1 Leash of Linguini",
                context(EffectPool.LEASH_OF_LINGUINI, false, false));

        assertThat(plan.skip, is(false));
        assertThat(plan.mpCost, is(12L));
        assertThat(plan.duration, is(10));
        assertThat(plan.usesRemaining, greaterThan(0L));
      }
    }

    @Test
    void toggleRequiresOneOfTheInterestEffects() {
      var unavailable = EffectSourceDispatcher.dispatch("toggle", "toggle", context(false, false));
      assertThat(unavailable.skip, is(true));

      try (var cleanups = withEffect(EffectPool.INTENSELY_INTERESTED)) {
        var available = EffectSourceDispatcher.dispatch("toggle", "toggle", context(false, false));

        assertThat(available.skip, is(false));
        assertThat(available.command, is("toggle"));
      }
    }

    @Test
    void explainsHowToUnlockUnavailableSpacegateVaccines() {
      try (var cleanups =
          new Cleanups(
              withRestricted(false),
              withProperty("spacegateAlways", false),
              withProperty("_spacegateToday", false),
              withProperty("spacegateVaccine1", false))) {
        var omitted =
            EffectSourceDispatcher.dispatch(
                "spacegate vaccine 1", "spacegate vaccine 1", context(false, false));
        var explained =
            EffectSourceDispatcher.dispatch(
                "spacegate vaccine 1", "spacegate vaccine 1", context(true, false));

        assertThat(omitted.skip, is(true));
        assertThat(explained.skip, is(false));
        assertThat(explained.command, is(""));
        assertThat(explained.text, containsString("unlock Spacegate and vaccine 1"));
        assertThat(explained.duration, is(30));
      }
    }

    @Test
    void explainsHowToInstallAMissingSourceTerminal() {
      try (var cleanups =
          new Cleanups(
              withProperty("sourceTerminalChips", ""),
              withProperty("sourceTerminalEnhanceKnown", ""))) {
        var omitted =
            EffectSourceDispatcher.dispatch(
                "terminal enhance items.enh", "terminal enhance items.enh", context(false, false));
        var explained =
            EffectSourceDispatcher.dispatch(
                "terminal enhance items.enh", "terminal enhance items.enh", context(true, false));

        assertThat(omitted.skip, is(true));
        assertThat(explained.skip, is(false));
        assertThat(explained.command, is(""));
        assertThat(explained.text, containsString("install Source Terminal"));
        assertThat(explained.duration, is(25));
      }
    }

    @Test
    void spentGenieBottleFallsBackToPocketWishes() {
      try (var cleanups =
          new Cleanups(
              withRestricted(false),
              withItem(ItemPool.GENIE_BOTTLE),
              withItem(ItemPool.POCKET_WISH),
              withProperty("_genieWishesUsed", 3))) {
        var plan =
            EffectSourceDispatcher.dispatch(
                "genie effect Disquiet Riot", "genie effect Disquiet Riot", context(false, false));

        assertThat(plan.skip, is(false));
        assertThat(plan.item.getItemId(), is(ItemPool.POCKET_WISH));
        assertThat(plan.usesRemaining, is(1L));
      }
    }
  }

  @Nested
  class Finalization {
    @Test
    void rendersSharedCostsAndVerboseDetails() {
      try (var cleanups =
          new Cleanups(
              withAdventuresLeft(10),
              withProperty("maximizerNoAdventures", false),
              withProperty("verboseMaximizer", true))) {
        var plan =
            EffectSourceDispatcher.dispatch(
                "custom source", "custom source", context(false, false));
        plan.adventureCost = 2;
        plan.mpCost = 3;
        plan.duration = 1;
        plan.usesRemaining = 1;

        var result = EffectSourcePlanFinalizer.finish(plan, context(false, false), 5);

        assertThat(result.skip, is(false));
        assertThat(
            result.text, is("custom source (2 adv, 3 mp, +5) [1 adv duration, 1 use remaining]"));
      }
    }

    @Test
    void skipsSourcesWhenAdventuresAreForbidden() {
      try (var cleanups = withProperty("maximizerNoAdventures", true)) {
        var plan =
            EffectSourceDispatcher.dispatch(
                "custom source", "custom source", context(false, false));
        plan.adventureCost = 1;

        var result = EffectSourcePlanFinalizer.finish(plan, context(false, false), 5);

        assertThat(result.skip, is(true));
      }
    }

    @Test
    void disablesSourcesWhenAdventuresAreUnavailable() {
      try (var cleanups =
          new Cleanups(withAdventuresLeft(0), withProperty("maximizerNoAdventures", false))) {
        var plan =
            EffectSourceDispatcher.dispatch(
                "custom source", "custom source", context(false, false));
        plan.adventureCost = 1;

        var result = EffectSourcePlanFinalizer.finish(plan, context(false, false), 5);

        assertThat(result.skip, is(false));
        assertThat(result.command, is(""));
        assertThat(result.text, is("custom source (1 adv, +5)"));
      }
    }

    @Test
    void reportsOwnedItemsThroughTheCommonAcquisitionPipeline() {
      try (var cleanups =
          new Cleanups(
              withRestricted(false),
              withItem(ItemPool.POCKET_WISH),
              withProperty("verboseMaximizer", true))) {
        var plan =
            EffectSourceDispatcher.dispatch(
                "custom source", "custom source", context(false, false));
        plan.item = ItemPool.get(ItemPool.POCKET_WISH);

        var result = EffectSourcePlanFinalizer.finish(plan, context(false, false), 5);

        assertThat(result.skip, is(false));
        assertThat(result.itemsRemaining, is(1));
        assertThat(result.text, containsString("[1 in inventory]"));
      }
    }

    @Test
    void skipsUnavailableItemsInInventoryOnlyScope() {
      try (var cleanups = new Cleanups(withRestricted(false), withItem(ItemPool.POCKET_WISH, 0))) {
        var plan =
            EffectSourceDispatcher.dispatch(
                "custom source", "custom source", context(false, false));
        plan.item = ItemPool.get(ItemPool.POCKET_WISH);

        var result = EffectSourcePlanFinalizer.finish(plan, context(false, false), 5);

        assertThat(result.skip, is(true));
      }
    }

    @Test
    void storageBuyingUsesACompoundCommandAndStorageMeat() {
      var context =
          context(
              EffectPool.LEASH_OF_LINGUINI,
              false,
              false,
              EquipScope.SPECULATE_ANY,
              200,
              PriceLevel.DONT_CHECK);
      try (var cleanups =
          new Cleanups(
              withPath(Path.NONE),
              withHardcore(false),
              withRonin(true),
              withInteractivity(false),
              withRestricted(false),
              withProperty("autoSatisfyWithMall", true),
              withItem(ItemPool.POCKET_WISH, 0),
              withItemInStorage(ItemPool.POCKET_WISH, 0),
              withStorageMeat(200))) {
        var plan = EffectSourceDispatcher.dispatch("custom source", "custom source", context);
        plan.item = ItemPool.get(ItemPool.POCKET_WISH);
        plan.price = 100;

        var result = EffectSourcePlanFinalizer.finish(plan, context, 5);

        assertThat(
            result.command,
            is(
                "buy using storage 1 \u00B6"
                    + ItemPool.POCKET_WISH
                    + ";pull \u00B6"
                    + ItemPool.POCKET_WISH
                    + ";custom source"));
      }

      try (var cleanups =
          new Cleanups(
              withPath(Path.NONE),
              withHardcore(false),
              withRonin(true),
              withInteractivity(false),
              withRestricted(false),
              withProperty("autoSatisfyWithMall", true),
              withItem(ItemPool.POCKET_WISH, 0),
              withItemInStorage(ItemPool.POCKET_WISH, 0),
              withStorageMeat(50))) {
        var plan = EffectSourceDispatcher.dispatch("custom source", "custom source", context);
        plan.item = ItemPool.get(ItemPool.POCKET_WISH);
        plan.price = 100;

        var result = EffectSourcePlanFinalizer.finish(plan, context, 5);

        assertThat(result.command, is(""));
      }
    }
  }
}
