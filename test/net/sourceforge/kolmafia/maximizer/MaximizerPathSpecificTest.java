package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.getBoosts;
import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withQuestProgress;
import static internal.helpers.Player.withSign;
import static internal.helpers.Player.withStats;
import static internal.matchers.Maximizer.recommendsSlot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath.Path;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for path-specific and quest-specific behaviors in Maximizer.java.
 * These tests verify that specific game states affect maximizer recommendations correctly.
 */
public class MaximizerPathSpecificTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("MaximizerPathSpecificTest");
    Preferences.reset("MaximizerPathSpecificTest");
  }

  @Nested
  class BadMoonPath {
    @Test
    public void badMoonStyxAvailableWhenNotUsed() {
      var cleanups =
          new Cleanups(
              withPath(Path.BAD_MOON),
              withSign(ZodiacSign.BAD_MOON),
              withProperty("styxPixieVisited", false),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // In Bad Moon, Styx should be available
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("styx"))));
      }
    }

    @Test
    public void badMoonStyxNotAvailableWhenUsed() {
      var cleanups =
          new Cleanups(
              withPath(Path.BAD_MOON),
              withSign(ZodiacSign.BAD_MOON),
              withProperty("styxPixieVisited", true),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Styx should not be suggested when already used
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("styx")))));
      }
    }

    @Test
    public void badMoonRestrictsVIPPoolTable() {
      var cleanups =
          new Cleanups(
              withPath(Path.BAD_MOON),
              withSign(ZodiacSign.BAD_MOON),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withProperty("_poolGames", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Pool should not be suggested in Bad Moon
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("pool")))));
      }
    }

    @Test
    public void badMoonRestrictsShower() {
      var cleanups =
          new Cleanups(
              withPath(Path.BAD_MOON),
              withSign(ZodiacSign.BAD_MOON),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withProperty("_aprilShower", false),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Shower should not be suggested in Bad Moon
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("shower")))));
      }
    }
  }

  @Nested
  class NuclearAutumnPath {
    @Test
    public void nuclearAutumnSpaAvailable() {
      var cleanups =
          new Cleanups(
              withPath(Path.NUCLEAR_AUTUMN),
              withProperty("falloutShelterLevel", 3),
              withProperty("_falloutShelterSpaUsed", false),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Fallout shelter spa should be available via campground vault3
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("campground vault3"))));
      }
    }

    @Test
    public void nuclearAutumnSpaNotAvailableWhenUsed() {
      var cleanups =
          new Cleanups(
              withPath(Path.NUCLEAR_AUTUMN),
              withProperty("falloutShelterLevel", 3),
              withProperty("_falloutShelterSpaUsed", true),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Fallout shelter spa should not be suggested when used
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("campground vault3")))));
      }
    }

    @Test
    public void nuclearAutumnSpaRequiresLevel3() {
      var cleanups =
          new Cleanups(
              withPath(Path.NUCLEAR_AUTUMN),
              withProperty("falloutShelterLevel", 2),
              withProperty("_falloutShelterSpaUsed", false),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Fallout shelter spa needs level 3
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("campground vault3")))));
      }
    }
  }

  @Nested
  class BeecorePath {
    @Test
    public void beecoreRestrictsEquipmentWithBees() {
      var cleanups =
          new Cleanups(
              withPath(Path.BEES_HATE_YOU),
              withEquippableItem("bugbear beanie"),
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus, 0 beeosity"));
        // With 0 beeosity, items with B should not be recommended
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT, "bugbear beanie"))));
        assertThat(getBoosts(), hasItem(recommendsSlot(Slot.HAT, "helmet turtle")));
      }
    }
  }

  @Nested
  class HatTrickPath {
    @Test
    public void hatTrickDoesNotRecommendHat() {
      var cleanups =
          new Cleanups(
              withPath(Path.HAT_TRICK),
              withEquippableItem("helmet turtle"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Hat slot is blocked in Hat Trick
        assertThat(getBoosts(), not(hasItem(recommendsSlot(Slot.HAT))));
      }
    }
  }

  @Nested
  class QuestDependentEffects {
    @Test
    public void friarsNotAvailableBeforeQuest() {
      var cleanups =
          new Cleanups(
              withProperty("lastFriarCeremonyAscension", 0),
              withProperty("knownAscensions", 1),
              withProperty("friarsBlessingReceived", false),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("exp"));
        // Friars blessing not available before quest
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("friars")))));
      }
    }

    @Test
    public void friarsNotAvailableWhenUsed() {
      var cleanups =
          new Cleanups(
              withProperty("lastFriarCeremonyAscension", 1),
              withProperty("knownAscensions", 1),
              withProperty("friarsBlessingReceived", true),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("exp"));
        // Friars blessing not available when already received
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("friars")))));
      }
    }

    @Test
    public void momFoodNotAvailableBeforeSeaQuest() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.SEA_MONKEES, "unstarted"),
              withProperty("_momFoodReceived", false),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Mom food not available before quest
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("mom")))));
      }
    }

    @Test
    public void momFoodNotAvailableWhenUsed() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.SEA_MONKEES, "finished"),
              withProperty("_momFoodReceived", true),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Mom food not available when already received
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("mom")))));
      }
    }
  }

  @Nested
  class ConcertEffects {
    @Test
    public void concertNotAvailableWithoutSidequest() {
      var cleanups =
          new Cleanups(
              withProperty("sidequestArenaCompleted", "none"),
              withProperty("concertVisited", false),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Concert not available without completing sidequest
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("concert")))));
      }
    }

    @Test
    public void concertNotAvailableWhenVisited() {
      var cleanups =
          new Cleanups(
              withProperty("sidequestArenaCompleted", "fratboy"),
              withProperty("concertVisited", true),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Concert not available when already visited
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("concert")))));
      }
    }
  }

  @Nested
  class HatterEffects {
    @Test
    public void hatterNotAvailableWhenVisited() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.DRINK_ME_POTION),
              withProperty("_madTeaParty", true),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Hatter not available when already visited
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("hatter")))));
      }
    }

    @Test
    public void hatterNotAvailableWithoutPotionOrEffect() {
      var cleanups =
          new Cleanups(
              withProperty("_madTeaParty", false),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Hatter not available without potion or effect
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("hatter")))));
      }
    }
  }

  @Nested
  class SpacegateVaccines {
    @Test
    public void spacegateVaccineNotAvailableWhenUsed() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true),
              withProperty("spacegateVaccine1", true),
              withProperty("_spacegateVaccine", true),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("meat"));
        // Spacegate vaccine should not be suggested when used
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("spacegate")))));
      }
    }

    @Test
    public void spacegateVaccineNotAvailableWithoutVaccines() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true),
              withProperty("spacegateVaccine1", false),
              withProperty("spacegateVaccine2", false),
              withProperty("spacegateVaccine3", false),
              withProperty("_spacegateVaccine", false),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("meat"));
        // Spacegate vaccine should not be suggested without any vaccines
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("spacegate")))));
      }
    }
  }

  @Nested
  class TelescopeEffects {
    @Test
    public void telescopeNotAvailableWithoutUpgrades() {
      var cleanups =
          new Cleanups(
              withProperty("telescopeUpgrades", 0),
              withProperty("telescopeLookedHigh", false),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Telescope should not be available without upgrades
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("telescope")))));
      }
    }

    @Test
    public void telescopeNotAvailableWhenUsed() {
      var cleanups =
          new Cleanups(
              withProperty("telescopeUpgrades", 5),
              withProperty("telescopeLookedHigh", true),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Telescope should not be available when used
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("telescope")))));
      }
    }
  }

  @Nested
  class BallpitEffects {
    @Test
    public void ballpitNotAvailableWhenUsed() {
      var cleanups = new Cleanups(withProperty("_ballpit", true), withStats(100, 100, 100));
      try (cleanups) {
        // "stat" keyword returns false when no equipment gives stats, but ballpit shouldn't be in boosts
        maximize("mus");
        // Ballpit should not be available when used
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("ballpit")))));
      }
    }
  }

  @Nested
  class MonorailEffects {
    @Test
    public void monorailNotAvailableWhenUsed() {
      var cleanups = new Cleanups(withProperty("_lyleFavored", true), withStats(100, 100, 100));
      try (cleanups) {
        maximize("init");
        // Monorail should not be available when used
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("monorail")))));
      }
    }
  }

  @Nested
  class JukeboxEffects {
    @Test
    public void jukeboxNotAvailableWhenUsed() {
      var cleanups = new Cleanups(withProperty("_jukebox", true), withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("meat"));
        // Jukebox should not be available when used
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("jukebox")))));
      }
    }
  }

  @Nested
  class BarrelShrineEffects {
    @Test
    public void barrelPrayerNotAvailableWhenUsed() {
      var cleanups =
          new Cleanups(
              withProperty("barrelShrineUnlocked", true),
              withProperty("_barrelPrayer", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
        // Barrel prayer should not be available when used
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("barrelprayer")))));
      }
    }

    @Test
    public void barrelPrayerNotAvailableWhenNotUnlocked() {
      var cleanups =
          new Cleanups(
              withProperty("barrelShrineUnlocked", false),
              withProperty("_barrelPrayer", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
        // Barrel prayer should not be available when not unlocked
        assertThat(getBoosts(), not(hasItem(hasProperty("cmd", startsWith("barrelprayer")))));
      }
    }
  }

  @Nested
  class FamiliarSwitching {
    @Test
    public void familiarSwitchWorksWhenRequested() {
      var cleanups =
          new Cleanups(
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("meat, switch leprechaun"));
        // Should suggest switching to leprechaun for meat
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("familiar"))));
      }
    }
  }

  @Nested
  class QuantumTerrarium {
    @Test
    public void quantumPathMaximizesSuccessfully() {
      var cleanups =
          new Cleanups(
              withPath(Path.QUANTUM),
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withStats(100, 100, 100));
      try (cleanups) {
        // Quantum terrarium shouldn't crash maximize
        assertTrue(maximize("item"));
      }
    }
  }

  @Nested
  class NoobcorePath {
    @Test
    public void noobcoreMaximizesSuccessfully() {
      var cleanups =
          new Cleanups(
              withPath(Path.GELATINOUS_NOOB),
              withStats(100, 100, 100));
      try (cleanups) {
        // Noobcore path shouldn't crash maximize
        assertTrue(maximize("mus"));
      }
    }
  }

  @Nested
  class LegacyOfLoathingPath {
    @Test
    public void legacyOfLoathingReplicaCargoShorts() {
      var cleanups =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withItem(ItemPool.REPLICA_CARGO_CULTIST_SHORTS),
              withProperty("_cargoPocketEmptied", false),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("meat"));
        // Replica cargo shorts should work in LoL
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("cargo"))));
      }
    }

    @Test
    public void legacyOfLoathingReplicaDeck() {
      var cleanups =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withItem(ItemPool.REPLICA_DECK_OF_EVERY_CARD),
              withProperty("_deckCardsDrawn", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("mus"));
        // Replica deck should work in LoL
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("play"))));
      }
    }

    @Test
    public void legacyOfLoathingReplicaGenieBottle() {
      var cleanups =
          new Cleanups(
              withPath(Path.LEGACY_OF_LOATHING),
              withItem(ItemPool.REPLICA_GENIE_BOTTLE),
              withProperty("_genieWishesUsed", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("meat"));
        // Replica genie bottle should work in LoL
        assertThat(getBoosts(), hasItem(hasProperty("cmd", startsWith("genie"))));
      }
    }
  }

  @Nested
  class GLoverPath {
    @Test
    public void gloverMaximizesSuccessfully() {
      var cleanups =
          new Cleanups(
              withPath(Path.GLOVER),
              withStats(100, 100, 100));
      try (cleanups) {
        // G-Lover path shouldn't crash maximize
        assertTrue(maximize("exp"));
      }
    }
  }
}
