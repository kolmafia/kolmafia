package net.sourceforge.kolmafia.maximizer;

import static internal.helpers.Maximizer.maximize;
import static internal.helpers.Player.withCampgroundItem;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEquippableItem;
import static internal.helpers.Player.withEquipped;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withSign;
import static internal.helpers.Player.withSkill;
import static internal.helpers.Player.withStats;
import static internal.helpers.Player.withWorkshedItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ZodiacSign;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the various effect sources in Maximizer.java.
 * These tests exercise the code paths for different effect sources
 * without asserting specific recommendations (which depend on complex game state).
 */
public class MaximizerEffectSourcesTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("MaximizerEffectSourcesTest");
    Preferences.reset("MaximizerEffectSourcesTest");
  }

  @Nested
  class Horsery {
    @Test
    public void exercisesHorseryPath() {
      var cleanups =
          new Cleanups(
              withProperty("horseryAvailable", true),
              withProperty("_horsery", ""),
              withMeat(1000),
              withStats(100, 100, 100));
      try (cleanups) {
        // Should not throw - exercises horsery code path
        maximize("meat");
      }
    }

    @Test
    public void exercisesHorseryWithExistingHorse() {
      var cleanups =
          new Cleanups(
              withProperty("horseryAvailable", true),
              withProperty("_horsery", "normal"),
              withMeat(1000),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesHorseryWithoutMeat() {
      var cleanups =
          new Cleanups(
              withProperty("horseryAvailable", true),
              withProperty("_horsery", "normal"),
              withMeat(100),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesHorseryUnavailable() {
      var cleanups =
          new Cleanups(
              withProperty("horseryAvailable", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }
  }

  @Nested
  class BoomBox {
    @Test
    public void exercisesBoomBoxPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.BOOMBOX),
              withProperty("_boomBoxSongsLeft", 11),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesBoomBoxNoUsesLeft() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.BOOMBOX),
              withProperty("_boomBoxSongsLeft", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesBoomBoxWithoutItem() {
      var cleanups =
          new Cleanups(
              withProperty("_boomBoxSongsLeft", 11),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }
  }

  @Nested
  class MindControlDevice {
    @Test
    public void exercisesMCDPath() {
      var cleanups =
          new Cleanups(
              withSign(ZodiacSign.MONGOOSE),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("ml");
      }
    }

    @Test
    public void exercisesCanadianMCD() {
      var cleanups =
          new Cleanups(
              withSign(ZodiacSign.WALLABY),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("ml");
      }
    }
  }

  @Nested
  class VIPLounge {
    @Test
    public void exercisesPoolPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withProperty("_poolGames", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesPoolUsedUp() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withProperty("_poolGames", 3),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesShowerPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withProperty("_aprilShower", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesShowerUsed() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withProperty("_aprilShower", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesSwimPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withProperty("_olympicSwimmingPool", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("init");
      }
    }

    @Test
    public void exercisesFortuneBuffPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withProperty("_clanFortuneBuffUsed", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesPhotoBoothPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withProperty("_photoBoothEffects", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesPhotoBoothUsedUp() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withProperty("_photoBoothEffects", 3),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesWithoutVIPKey() {
      var cleanups =
          new Cleanups(
              withProperty("_poolGames", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }
  }

  @Nested
  class SourceTerminal {
    @Test
    public void exercisesTerminalPath() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.SOURCE_TERMINAL),
              withProperty("sourceTerminalChips", ""),
              withProperty("sourceTerminalEnhanceKnown", "items,meat,init,critical,damage,substats"),
              withProperty("_sourceTerminalEnhanceUses", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }

    @Test
    public void exercisesTerminalWithCRAM() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.SOURCE_TERMINAL),
              withProperty("sourceTerminalChips", "CRAM"),
              withProperty("sourceTerminalEnhanceKnown", "items,meat,init,critical,damage,substats"),
              withProperty("_sourceTerminalEnhanceUses", 1),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }

    @Test
    public void exercisesTerminalWithSCRAM() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.SOURCE_TERMINAL),
              withProperty("sourceTerminalChips", "CRAM,SCRAM"),
              withProperty("sourceTerminalEnhanceKnown", "items"),
              withProperty("_sourceTerminalEnhanceUses", 2),
              withProperty("sourceTerminalPram", 3),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }

    @Test
    public void exercisesTerminalWithINGRAM() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.SOURCE_TERMINAL),
              withProperty("sourceTerminalChips", "INGRAM"),
              withProperty("sourceTerminalEnhanceKnown", "items"),
              withProperty("_sourceTerminalEnhanceUses", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }

    @Test
    public void exercisesTerminalUsedUp() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.SOURCE_TERMINAL),
              withProperty("sourceTerminalChips", ""),
              withProperty("sourceTerminalEnhanceKnown", "items"),
              withProperty("_sourceTerminalEnhanceUses", 1),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }

    @Test
    public void exercisesNoTerminal() {
      var cleanups =
          new Cleanups(
              withProperty("_sourceTerminalEnhanceUses", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }
  }

  @Nested
  class AsdonMartin {
    @Test
    public void exercisesAsdonPath() {
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.ASDON_MARTIN),
              withProperty("_campgroundFuel", 100),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesAsdonLowFuel() {
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.ASDON_MARTIN),
              withProperty("_campgroundFuel", 10),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }
  }

  @Nested
  class MayoClinic {
    @Test
    public void exercisesMayoSoakPath() {
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.MAYO_CLINIC),
              withProperty("_mayoTankSoaked", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }

    @Test
    public void exercisesMayoSoakUsed() {
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.MAYO_CLINIC),
              withProperty("_mayoTankSoaked", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }
  }

  @Nested
  class Witchess {
    @Test
    public void exercisesWitchessPath() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.WITCHESS_SET),
              withProperty("_witchessBuff", false),
              withProperty("puzzleChampBonus", 20),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesWitchessUsed() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.WITCHESS_SET),
              withProperty("_witchessBuff", true),
              withProperty("puzzleChampBonus", 20),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesWitchessNoPuzzleBonus() {
      var cleanups =
          new Cleanups(
              withCampgroundItem(ItemPool.WITCHESS_SET),
              withProperty("_witchessBuff", false),
              withProperty("puzzleChampBonus", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }
  }

  @Nested
  class BeachComb {
    @Test
    public void exercisesBeachHeadPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.BEACH_COMB),
              withProperty("_beachHeadsUsed", ""),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesDriftwoodBeachComb() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.DRIFTWOOD_BEACH_COMB),
              withProperty("_beachHeadsUsed", ""),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesBeachHeadUsed() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.BEACH_COMB),
              withProperty("_beachHeadsUsed", "1,2,3,4,5,6,7,8,9,10,11"),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }
  }

  @Nested
  class GrimBrother {
    @Test
    public void exercisesGrimPath() {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.GRIM_BROTHER),
              withProperty("_grimBuff", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }

    @Test
    public void exercisesGrimUsed() {
      var cleanups =
          new Cleanups(
              withFamiliarInTerrarium(FamiliarPool.GRIM_BROTHER),
              withProperty("_grimBuff", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }
  }

  @Nested
  class DeckOfEveryCard {
    @Test
    public void exercisesDeckPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.DECK_OF_EVERY_CARD),
              withProperty("_deckCardsDrawn", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesDeckUsedUp() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.DECK_OF_EVERY_CARD),
              withProperty("_deckCardsDrawn", 15),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }
  }

  @Nested
  class ProtonAccelerator {
    @Test
    public void exercisesCrossStreamsPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.PROTON_ACCELERATOR),
              withProperty("_streamsCrossed", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("all res");
      }
    }

    @Test
    public void exercisesCrossStreamsUsed() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.PROTON_ACCELERATOR),
              withProperty("_streamsCrossed", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("all res");
      }
    }
  }

  @Nested
  class Monorail {
    @Test
    public void exercisesMonorailPath() {
      var cleanups =
          new Cleanups(withProperty("_lyleFavored", false), withStats(100, 100, 100));
      try (cleanups) {
        maximize("init");
      }
    }

    @Test
    public void exercisesMonorailUsed() {
      var cleanups = new Cleanups(withProperty("_lyleFavored", true), withStats(100, 100, 100));
      try (cleanups) {
        maximize("init");
      }
    }
  }

  @Nested
  class BoxingDaycare {
    @Test
    public void exercisesDaycarePath() {
      var cleanups =
          new Cleanups(
              withProperty("daycareOpen", true),
              withProperty("_daycareSpa", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesDaycareToday() {
      var cleanups =
          new Cleanups(
              withProperty("daycareOpen", false),
              withProperty("_daycareToday", true),
              withProperty("_daycareSpa", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesDaycareUsed() {
      var cleanups =
          new Cleanups(
              withProperty("daycareOpen", true),
              withProperty("_daycareSpa", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }
  }

  @Nested
  class GreatestAmericanPants {
    @Test
    public void exercisesGAPPath() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.PANTS, "Greatest American Pants"),
              withProperty("_gapBuffs", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesGAPUsedUp() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.PANTS, "Greatest American Pants"),
              withProperty("_gapBuffs", 5),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesGAPNotEquipped() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.GREAT_PANTS),
              withProperty("_gapBuffs", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }
  }

  @Nested
  class CampAway {
    @Test
    public void exercisesCampAwayPath() {
      var cleanups =
          new Cleanups(
              withProperty("_campAwayCloudBuffs", 0),
              withProperty("getawayCampsiteUnlocked", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("adv");
      }
    }

    @Test
    public void exercisesCampAwayUsed() {
      var cleanups =
          new Cleanups(
              withProperty("_campAwayCloudBuffs", 1),
              withProperty("getawayCampsiteUnlocked", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("adv");
      }
    }
  }

  @Nested
  class MonkeyPaw {
    @Test
    public void exercisesMonkeyPawPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.CURSED_MONKEY_PAW),
              withProperty("_monkeyPawWishesUsed", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesMonkeyPawUsedUp() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.CURSED_MONKEY_PAW),
              withProperty("_monkeyPawWishesUsed", 5),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }
  }

  @Nested
  class GenieBottle {
    @Test
    public void exercisesGenieBottlePath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.GENIE_BOTTLE),
              withProperty("_genieWishesUsed", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesGenieBottleUsedUp() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.GENIE_BOTTLE),
              withProperty("_genieWishesUsed", 3),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesPocketWishPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.POCKET_WISH),
              withProperty("_genieWishesUsed", 3),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }
  }

  @Nested
  class Telescope {
    @Test
    public void exercisesTelescopePath() {
      var cleanups =
          new Cleanups(
              withProperty("telescopeUpgrades", 5),
              withProperty("telescopeLookedHigh", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesTelescopeUsed() {
      var cleanups =
          new Cleanups(
              withProperty("telescopeUpgrades", 5),
              withProperty("telescopeLookedHigh", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }

    @Test
    public void exercisesTelescopeNoUpgrades() {
      var cleanups =
          new Cleanups(
              withProperty("telescopeUpgrades", 0),
              withProperty("telescopeLookedHigh", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("mus");
      }
    }
  }

  @Nested
  class Ballpit {
    @Test
    public void exercisesBallpitPath() {
      var cleanups = new Cleanups(withProperty("_ballpit", false), withStats(100, 100, 100));
      try (cleanups) {
        maximize("stat");
      }
    }

    @Test
    public void exercisesBallpitUsed() {
      var cleanups = new Cleanups(withProperty("_ballpit", true), withStats(100, 100, 100));
      try (cleanups) {
        maximize("stat");
      }
    }
  }

  @Nested
  class Jukebox {
    @Test
    public void exercisesJukeboxPath() {
      var cleanups = new Cleanups(withProperty("_jukebox", false), withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesJukeboxUsed() {
      var cleanups = new Cleanups(withProperty("_jukebox", true), withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }
  }

  @Nested
  class Friars {
    @Test
    public void exercisesFriarsPath() {
      var cleanups =
          new Cleanups(
              withProperty("lastFriarCeremonyAscension", 1),
              withProperty("knownAscensions", 1),
              withProperty("friarsBlessingReceived", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }

    @Test
    public void exercisesFriarsUsed() {
      var cleanups =
          new Cleanups(
              withProperty("lastFriarCeremonyAscension", 1),
              withProperty("knownAscensions", 1),
              withProperty("friarsBlessingReceived", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }

    @Test
    public void exercisesFriarsNotUnlocked() {
      var cleanups =
          new Cleanups(
              withProperty("lastFriarCeremonyAscension", 0),
              withProperty("knownAscensions", 1),
              withProperty("friarsBlessingReceived", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }
  }

  @Nested
  class BarrelShrine {
    @Test
    public void exercisesBarrelPrayerPath() {
      var cleanups =
          new Cleanups(
              withProperty("barrelShrineUnlocked", true),
              withProperty("_barrelPrayer", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }

    @Test
    public void exercisesBarrelPrayerUsed() {
      var cleanups =
          new Cleanups(
              withProperty("barrelShrineUnlocked", true),
              withProperty("_barrelPrayer", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }
  }

  @Nested
  class PillKeeper {
    @Test
    public void exercisesPillKeeperPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.PILL_KEEPER),
              withProperty("_freePillKeeperUsed", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }

    @Test
    public void exercisesPillKeeperAfterFreeUse() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.PILL_KEEPER),
              withProperty("_freePillKeeperUsed", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }
  }

  @Nested
  class CargoShorts {
    @Test
    public void exercisesCargoShortsPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.CARGO_CULTIST_SHORTS),
              withProperty("_cargoPocketEmptied", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesCargoShortsUsed() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.CARGO_CULTIST_SHORTS),
              withProperty("_cargoPocketEmptied", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }
  }

  @Nested
  class AlliedRadio {
    @Test
    public void exercisesAlliedRadioPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.HANDHELD_ALLIED_RADIO),
              withProperty("_alliedRadioDropsUsed", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }

    @Test
    public void exercisesAlliedRadioBackpack() {
      var cleanups =
          new Cleanups(
              withEquipped(Slot.CONTAINER, ItemPool.ALLIED_RADIO_BACKPACK),
              withProperty("_alliedRadioDropsUsed", 0),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }
  }

  @Nested
  class SweetSynthesis {
    @Test
    public void exercisesSynthesisPath() {
      var cleanups =
          new Cleanups(
              withSkill("Sweet Synthesis"),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }
  }

  @Nested
  class AprilingBand {
    @Test
    public void exercisesAprilBandPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.APRILING_BAND_HELMET),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }
  }

  @Nested
  class MayamCalendar {
    @Test
    public void exercisesMayamPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.MAYAM_CALENDAR),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("exp");
      }
    }
  }

  @Nested
  class ToggleEffects {
    @Test
    public void exercisesToggleWithIntenselyInterested() {
      var cleanups =
          new Cleanups(
              withEffect("Intensely Interested"),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }

    @Test
    public void exercisesToggleWithSuperficiallyInterested() {
      var cleanups =
          new Cleanups(
              withEffect("Superficially Interested"),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesToggleWithoutEffect() {
      var cleanups = new Cleanups(withStats(100, 100, 100));
      try (cleanups) {
        maximize("item");
      }
    }
  }

  @Nested
  class EmitSlot {
    @Test
    public void exercisesEnthroning() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Crown of Thrones"),
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesBjornifying() {
      var cleanups =
          new Cleanups(
              withEquippableItem("Buddy Bjorn"),
              withFamiliar(FamiliarPool.BABY_GRAVY_FAIRY),
              withFamiliarInTerrarium(FamiliarPool.LEPRECHAUN),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesModeableItems() {
      var cleanups =
          new Cleanups(
              withEquippableItem("backup camera"),
              withStats(100, 100, 100));
      try (cleanups) {
        assertTrue(maximize("init"));
      }
    }
  }

  @Nested
  class Spacegate {
    @Test
    public void exercisesSpacegatePath() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true),
              withProperty("spacegateVaccine1", true),
              withProperty("_spacegateVaccine", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesSpacegateToday() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", false),
              withProperty("_spacegateToday", true),
              withProperty("spacegateVaccine2", true),
              withProperty("_spacegateVaccine", false),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }

    @Test
    public void exercisesSpacegateUsed() {
      var cleanups =
          new Cleanups(
              withProperty("spacegateAlways", true),
              withProperty("spacegateVaccine1", true),
              withProperty("_spacegateVaccine", true),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }
  }

  @Nested
  class LoathingIdol {
    @Test
    public void exercisesLoathingIdolPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.LOATHING_IDOL_MICROPHONE),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("meat");
      }
    }
  }

  @Nested
  class Skeleton {
    @Test
    public void exercisesSkeletonPath() {
      var cleanups =
          new Cleanups(
              withItem(ItemPool.SKELETON),
              withStats(100, 100, 100));
      try (cleanups) {
        maximize("spooky res");
      }
    }
  }
}
