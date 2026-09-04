package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Player.withAdventuresLeft;
import static internal.helpers.Player.withCampgroundItem;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withFullness;
import static internal.helpers.Player.withHP;
import static internal.helpers.Player.withHardcore;
import static internal.helpers.Player.withInebriety;
import static internal.helpers.Player.withInteractivity;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withItemInCloset;
import static internal.helpers.Player.withItemInStorage;
import static internal.helpers.Player.withMP;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withRestricted;
import static internal.helpers.Player.withRonin;
import static internal.helpers.Player.withSign;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withSpleenUse;
import static internal.helpers.Player.withWorkshedItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.objectpool.SkillPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

  private static Cleanups withFuel(int fuel) {
    int previous = CampgroundRequest.getFuel();
    CampgroundRequest.setFuel(fuel);
    return new Cleanups(() -> CampgroundRequest.setFuel(previous));
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

    @ParameterizedTest
    @CsvSource({
      "pillkeeper extend, Pill Keeper",
      "cargo effect sample, Cargo Cultist Shorts",
      "daycare mysticality, Boxing Daycare",
      "play strength, Deck of Every Card",
      "witchess, Witchess Set",
      "crossstreams, protonic accelerator",
      "mayosoak, Mayo Clinic",
      "barrelprayer buff, Barrel god",
      "gap vision, Greatest American Pants",
      "asdonmartin drive observantly, Asdon Martin"
    })
    void explainsMissingInstallableSources(String command, String expectedText) {
      try (var cleanups =
          new Cleanups(
              withPath(Path.NONE),
              withRestricted(false),
              withInteractivity(true),
              withProperty("daycareOpen", false),
              withProperty("_daycareToday", false),
              withProperty("barrelShrineUnlocked", false))) {
        var plan = EffectSourceDispatcher.dispatch(command, command, context(true, false));

        assertThat(plan.skip, is(false));
        assertThat(plan.command, is(""));
        assertThat(plan.text, containsString(expectedText));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "monorail buff, _lyleFavored, 10",
      "ballpit, _ballpit, 20",
      "jukebox song, _jukebox, 10"
    })
    void disablesCompletedDailySources(String command, String preference, int duration) {
      try (var cleanups = new Cleanups(withInteractivity(true), withProperty(preference, true))) {
        var plan = EffectSourceDispatcher.dispatch(command, command, context(false, false));

        assertThat(plan.skip, is(false));
        assertThat(plan.command, is(""));
        assertThat(plan.duration, is(duration));
        assertThat(plan.usesRemaining, is(0L));
      }
    }

    @Test
    void disablesOwnedDailySourcesAfterTheirUsesAreSpent() {
      try (var cleanups =
          new Cleanups(
              withRestricted(false),
              withInteractivity(true),
              withProperty("lastFriarCeremonyAscension", 0),
              withProperty("knownAscensions", 0),
              withProperty("friarsBlessingReceived", true),
              withQuestProgress(Quest.SEA_MONKEES, QuestDatabase.FINISHED),
              withProperty("_momFoodReceived", true),
              withQuestProgress(Quest.MANOR, QuestDatabase.FINISHED),
              withProperty("demonSummoned", true),
              withProperty("sidequestArenaCompleted", "fratboy"),
              withProperty("concertVisited", true),
              withItem(ItemPool.DECK_OF_EVERY_CARD),
              withProperty("_deckCardsDrawn", 15),
              withFamiliarInTerrarium(FamiliarPool.GRIM_BROTHER),
              withProperty("_grimBuff", true),
              withCampgroundItem(ItemPool.WITCHESS_SET),
              withProperty("_witchessBuff", true),
              withProperty("puzzleChampBonus", 20),
              withItem(ItemPool.PROTON_ACCELERATOR),
              withProperty("_streamsCrossed", true),
              withWorkshedItem(ItemPool.MAYO_CLINIC),
              withProperty("_mayoTankSoaked", true),
              withProperty("barrelShrineUnlocked", true),
              withProperty("_barrelPrayer", true))) {
        assertExhausted("friars food", 20);
        assertExhausted("mom food", 50);
        assertExhausted("summon 1", 30);
        assertExhausted("concert Elvish", 20);
        assertExhausted("play strength", 20);
        assertExhausted("grim init", 30);
        assertExhausted("witchess", 25);
        assertExhausted("crossstreams", 10);
        assertExhausted("mayosoak", 20);
        assertExhausted("barrelprayer buff", 50);
      }
    }

    @Test
    void handlesAvailableBeachAndTelescopeSources() {
      try (var cleanups =
          new Cleanups(
              withRestricted(false),
              withItem(ItemPool.BEACH_COMB),
              withProperty("_beachHeadsUsed", ""),
              withProperty("telescopeUpgrades", 1),
              withProperty("telescopeLookedHigh", true))) {
        var beach =
            EffectSourceDispatcher.dispatch(
                "beach head Hot-Headed", "beach head Hot-Headed", context(2477, false, false));
        var telescope =
            EffectSourceDispatcher.dispatch(
                "telescope high", "telescope high", context(false, false));

        assertThat(beach.skip, is(false));
        assertThat(beach.command, is("beach head Hot-Headed"));
        assertThat(beach.duration, is(50));
        assertThat(beach.usesRemaining, is(1L));
        assertThat(telescope.skip, is(false));
        assertThat(telescope.command, is(""));
        assertThat(telescope.duration, is(10));
      }
    }

    @Test
    void handlesInstalledTerminalAndAsdonMartin() {
      try (var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.SOURCE_TERMINAL),
              withProperty("sourceTerminalChips", "CRAM SCRAM INGRAM"),
              withProperty("_sourceTerminalEnhanceUses", 3),
              withProperty("sourceTerminalPram", 2),
              withWorkshedItem(ItemPool.ASDON_MARTIN),
              withFuel(74))) {
        var terminal =
            EffectSourceDispatcher.dispatch(
                "terminal enhance items.enh", "terminal enhance items.enh", context(false, false));
        var asdon =
            EffectSourceDispatcher.dispatch(
                "asdonmartin drive observantly",
                "asdonmartin drive observantly",
                context(false, false));

        assertThat(terminal.skip, is(false));
        assertThat(terminal.command, is(""));
        assertThat(terminal.duration, is(60));
        assertThat(terminal.usesRemaining, is(0L));
        assertThat(asdon.skip, is(false));
        assertThat(asdon.command, is("asdonmartin drive observantly"));
        assertThat(asdon.duration, is(30));
        assertThat(asdon.usesRemaining, is(2L));
        assertThat(asdon.fuelCost, is(37));
      }
    }

    @Test
    void handlesBadMoonAndFalloutShelterDailySources() {
      try (var badMoon =
          new Cleanups(
              withPath(Path.BAD_MOON),
              withSign(ZodiacSign.BAD_MOON),
              withProperty("styxPixieVisited", true))) {
        var styx =
            EffectSourceDispatcher.dispatch("styx muscle", "styx muscle", context(false, false));

        assertThat(styx.skip, is(false));
        assertThat(styx.command, is(""));
        assertThat(styx.duration, is(10));
      }

      try (var fallout =
          new Cleanups(
              withPath(Path.NUCLEAR_AUTUMN),
              withProperty("falloutShelterLevel", 3),
              withProperty("_falloutShelterSpaUsed", true))) {
        var spa =
            EffectSourceDispatcher.dispatch(
                "campground vault3", "campground vault3", context(false, false));

        assertThat(spa.skip, is(false));
        assertThat(spa.command, is(""));
        assertThat(spa.duration, is(100));
        assertThat(spa.usesRemaining, is(0L));
      }
    }

    @Test
    void handlesHatterAndAlliedRadioState() {
      try (var cleanups =
          new Cleanups(
              withRestricted(false),
              withItem(ItemPool.DRINK_ME_POTION),
              withItem("helmet turtle"),
              withProperty("_madTeaParty", true),
              withItem(ItemPool.ALLIED_RADIO_BACKPACK),
              withProperty("_alliedRadioDropsUsed", 0),
              withProperty("_alliedRadioWildsunBoon", true))) {
        var hatter =
            EffectSourceDispatcher.dispatch("hatter 12", "hatter 12", context(false, false));
        var radio =
            EffectSourceDispatcher.dispatch(
                "alliedradio effect boon",
                "alliedradio effect boon",
                context(EffectPool.WILDSUN_BOON, false, false));

        assertThat(hatter.skip, is(false));
        assertThat(hatter.command, is(""));
        assertThat(hatter.duration, is(30));
        assertThat(radio.skip, is(false));
        assertThat(radio.command, is(""));
        assertThat(radio.item, nullValue());
        assertThat(radio.duration, is(100));
      }
    }

    @Test
    void rejectsExplicitlyExcludedConsumables() {
      try (var cleanups = withItem(ItemPool.DIETING_PILL)) {
        var dietingPill =
            EffectSourceDispatcher.dispatch(
                "use 1 dieting pill", "use 1 dieting pill", context(false, false));

        assertThat(dietingPill.skip, is(true));
      }
    }

    @Test
    void omitsOrExplainsUnidentifiedConsumables() {
      try (var cleanups = withContinuationState()) {
        var omitted =
            EffectSourceDispatcher.dispatch(
                "use 1 definitely-not-an-item",
                "use 1 definitely-not-an-item",
                context(false, false));
        var explained =
            EffectSourceDispatcher.dispatch(
                "use 1 definitely-not-an-item",
                "use 1 definitely-not-an-item",
                context(true, false));

        assertThat(omitted.skip, is(true));
        assertThat(explained.skip, is(false));
        assertThat(explained.command, is(""));
        assertThat(explained.text, is("(identify & use 1 definitely-not-an-item)"));
      }
    }

    @Test
    void modelsHotDogsWithoutInventoryItems() {
      try (var cleanups =
          new Cleanups(
              withPath(Path.NONE),
              withRestricted(false),
              withInteractivity(true),
              withProperty("_fancyHotDogEaten", false))) {
        var plan =
            EffectSourceDispatcher.dispatch(
                "eat 1 optimal dog", "eat 1 optimal dog", context(false, true));

        assertThat(plan.skip, is(false));
        assertThat(plan.item, nullValue());
        assertThat(plan.fullnessCost, greaterThan(0));
        assertThat(plan.usesRemaining, is(1L));
      }
    }

    @Test
    void modelsSynthesisAndPillKeeperResourceLimits() {
      try (var cleanups =
          new Cleanups(
              withPath(Path.NONE),
              withRestricted(false),
              withSkill(SkillPool.SWEET_SYNTHESIS),
              withSpleenUse(KoLCharacter.getSpleenLimit()),
              withItem(ItemPool.PILL_KEEPER),
              withProperty("_freePillKeeperUsed", true))) {
        var synthesis =
            EffectSourceDispatcher.dispatch(
                "synthesize effect", "synthesize effect", context(false, false));
        var pillKeeper =
            EffectSourceDispatcher.dispatch(
                "pillkeeper extend", "pillkeeper extend", context(false, false));

        assertThat(synthesis.skip, is(false));
        assertThat(synthesis.command, is(""));
        assertThat(synthesis.spleenCost, is(1));
        assertThat(pillKeeper.skip, is(false));
        assertThat(pillKeeper.command, is(""));
        assertThat(pillKeeper.spleenCost, is(3));
      }
    }

    @Test
    void disablesAvailableSpacegateDaycareAndSkateUsesAfterUse() {
      try (var cleanups =
          new Cleanups(
              withPath(Path.NONE),
              withRestricted(false),
              withProperty("spacegateAlways", true),
              withProperty("spacegateVaccine1", true),
              withProperty("_spacegateVaccine", true),
              withProperty("daycareOpen", true),
              withProperty("_daycareSpa", true),
              withProperty("skateParkStatus", "ice"),
              withProperty("_skateBuff1", true))) {
        var spacegate =
            EffectSourceDispatcher.dispatch(
                "spacegate vaccine 1", "spacegate vaccine 1", context(false, false));
        var daycare =
            EffectSourceDispatcher.dispatch(
                "daycare mysticality", "daycare mysticality", context(false, false));
        var skate =
            EffectSourceDispatcher.dispatch("skate lutz", "skate lutz", context(false, false));

        assertThat(spacegate.skip, is(false));
        assertThat(spacegate.command, is(""));
        assertThat(spacegate.usesRemaining, is(0L));
        assertThat(daycare.skip, is(false));
        assertThat(daycare.command, is(""));
        assertThat(daycare.usesRemaining, is(0L));
        assertThat(skate.skip, is(false));
        assertThat(skate.command, is(""));
        assertThat(skate.usesRemaining, is(0L));
      }
    }

    private void assertExhausted(String command, int duration) {
      var plan = EffectSourceDispatcher.dispatch(command, command, context(false, true));

      assertThat(command, plan.skip, is(false));
      assertThat(command, plan.command, is(""));
      assertThat(command, plan.duration, is(duration));
      assertThat(command, plan.usesRemaining, is(0L));
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

    @Test
    void retrievesAccessibleItemsFromTheCloset() {
      try (var cleanups =
          new Cleanups(
              withRestricted(false),
              withProperty("autoSatisfyWithCloset", true),
              withItem(ItemPool.POCKET_WISH, 0),
              withItemInCloset(ItemPool.POCKET_WISH))) {
        var plan =
            EffectSourceDispatcher.dispatch(
                "custom source", "custom source", context(false, false));
        plan.item = ItemPool.get(ItemPool.POCKET_WISH);

        var result = EffectSourcePlanFinalizer.finish(plan, context(false, false), 5);

        assertThat(
            result.command, is("closet take 1 \u00B6" + ItemPool.POCKET_WISH + ";custom source"));
        assertThat(result.text, is("uncloset & custom source (+5)"));
      }
    }

    @Test
    void pullsItemsFromStorageInRonin() {
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
              withProperty("autoSatisfyWithMall", false),
              withItem(ItemPool.POCKET_WISH, 0),
              withItemInStorage(ItemPool.POCKET_WISH))) {
        var plan = EffectSourceDispatcher.dispatch("custom source", "custom source", context);
        plan.item = ItemPool.get(ItemPool.POCKET_WISH);

        var result = EffectSourcePlanFinalizer.finish(plan, context, 5);

        assertThat(result.command, is("pull \u00B6" + ItemPool.POCKET_WISH + ";custom source"));
        assertThat(result.text, is("pull & custom source (+5)"));
      }
    }

    @Test
    void buysItemsFromNpcStores() {
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
              withRestricted(false),
              withMeat(200),
              withProperty("autoSatisfyWithNPCs", true),
              withItem(ItemPool.CHEWING_GUM, 0))) {
        var plan = EffectSourceDispatcher.dispatch("custom source", "custom source", context);
        plan.item = ItemPool.get(ItemPool.CHEWING_GUM);

        var result = EffectSourcePlanFinalizer.finish(plan, context, 5);

        assertThat(result.command, is("buy 1 \u00B6" + ItemPool.CHEWING_GUM + ";custom source"));
        assertThat(result.text, containsString("buy & custom source"));
        assertThat(result.text, containsString("50 meat"));
      }
    }

    @Test
    void disablesAndRendersUnavailableOrganAndClassResources() {
      try (var cleanups =
          new Cleanups(
              withFullness(KoLCharacter.getStomachCapacity()),
              withInebriety(KoLCharacter.getLiverCapacity()),
              withSpleenUse(KoLCharacter.getSpleenLimit()),
              withHP(0, 100, 100),
              withProperty("verboseMaximizer", false))) {
        var plan =
            EffectSourceDispatcher.dispatch(
                "custom source", "custom source", context(false, false));
        plan.fullnessCost = 1;
        plan.inebrietyCost = 2;
        plan.spleenCost = 3;
        plan.soulsauceCost = 4;
        plan.thunderCost = 5;
        plan.hpCost = 6;
        plan.fuelCost = 37;

        var result = EffectSourcePlanFinalizer.finish(plan, context(false, false), 5);

        assertThat(result.skip, is(false));
        assertThat(result.command, is(""));
        assertThat(
            result.text,
            is(
                "custom source (1 full, 2 drunk, 3 spleen, 4 soulsauce, 5 dB of thunder, "
                    + "6 hp, 37 fuel, +5)"));
      }
    }

    @ParameterizedTest
    @CsvSource({"rain, 2, 2 drops of rain", "lightning, 3, 3 bolts of lightning"})
    void rendersWeatherResourceCosts(String resource, int cost, String expected) {
      var plan =
          EffectSourceDispatcher.dispatch("custom source", "custom source", context(false, false));
      if (resource.equals("rain")) plan.rainCost = cost;
      else plan.lightningCost = cost;

      var result = EffectSourcePlanFinalizer.finish(plan, context(false, false), 5);

      assertThat(result.command, is(""));
      assertThat(result.text, containsString(expected));
    }
  }
}
