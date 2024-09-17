package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withCurrentRun;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withNextMonster;
import static internal.helpers.Player.withProperty;
import static internal.helpers.Player.withTrackedMonsters;
import static internal.helpers.Player.withTrackedPhyla;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.MonsterData;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.persistence.MonsterDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.TrackManager.Tracker;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TrackManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("TrackManagerTest");
    Preferences.reset("TrackManagerTest");
    TrackManager.clearCache();
  }

  @AfterAll
  public static void cleanup() {
    TrackManager.clearCache();
  }

  private static final MonsterData CRATE = MonsterDatabase.findMonster("crate");
  private static final MonsterData SCARY_PIRATE = MonsterDatabase.findMonster("scary pirate");
  private static final MonsterData SMUT_ORC_NAILER = MonsterDatabase.findMonster("smut orc nailer");
  private static final MonsterData SPOOKY_MUMMY = MonsterDatabase.findMonster("spooky mummy");
  private static final MonsterData ELF_GUARD_ARMORER =
      MonsterDatabase.findMonster("Elf Guard armorer");
  private static final MonsterData TAN_GNAT = MonsterDatabase.findMonster("Tan Gnat");
  private static final MonsterData MAGICAL_FRUIT_BAT =
      MonsterDatabase.findMonster("magical fruit bat");

  private Cleanups withNosyNose() {
    return withFamiliar(FamiliarPool.NOSY_NOSE);
  }

  private Cleanups withSnapper() {
    return withFamiliar(FamiliarPool.RED_SNAPPER);
  }

  private Cleanups withBeastlyOdor() {
    return withEffect(EffectPool.A_BEASTLY_ODOR);
  }

  private Cleanups withEwTheHumanity() {
    return withEffect(EffectPool.EW_THE_HUMANITY);
  }

  private boolean isTracked(String monster) {
    return TrackManager.countCopies(monster) > 0;
  }

  @Nested
  class ClearCache {
    @Test
    void clearCache() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128), withTrackedMonsters("fluffy bunny:Transcendent Olfaction:119"));

      try (cleanups) {
        assertTrue(isTracked("fluffy bunny"));
        TrackManager.clearCache();

        assertFalse(isTracked("fluffy bunny"));
      }
    }

    @Test
    void clearCachePhyla() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128), withSnapper(), withTrackedPhyla("beast:Red-Nosed Snapper:119"));

      try (cleanups) {
        assertTrue(isTracked("fluffy bunny"));
        TrackManager.clearCache();

        assertFalse(isTracked("fluffy bunny"));
      }
    }
  }

  @Nested
  class LoadTracked {

    @Test
    void loadTrackedMonsters() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withNosyNose(),
              withTrackedMonsters(
                  "gingerbread lawyer:Transcendent Olfaction:118:unhinged survivor:Nosy Nose:119:grizzled survivor:Gallapagosian Mating Call:119:cat-alien:Offer Latte to Opponent:119:alielf:Monkey Point:119:whiny survivor:Be Superficially interested:119"));

      try (cleanups) {
        assertTrue(isTracked("gingerbread lawyer"));
        assertTrue(isTracked("unhinged survivor"));
        assertTrue(isTracked("grizzled survivor"));
        assertTrue(isTracked("cat-alien"));
        assertTrue(isTracked("alielf"));
        assertTrue(isTracked("whiny survivor"));

        assertFalse(isTracked("zmobie"));
      }
    }

    @Test
    void loadTrackedPhyla() {
      var cleanups =
          new Cleanups(
              withCurrentRun(1),
              withBeastlyOdor(),
              withEwTheHumanity(),
              withSnapper(),
              withTrackedPhyla(
                  "beast:A Beastly Odor:1:dude:Ew, The Humanity:1:fish:Red-Nosed Snapper:1"));

      try (cleanups) {
        assertTrue(isTracked("vampire bat"));
        assertTrue(isTracked("unhinged survivor"));
        assertTrue(isTracked("clubfish"));

        assertFalse(isTracked("zmobie"));
      }
    }

    @Test
    void loadTrackedMonstersSkipsInvalidTracker() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withTrackedMonsters(
                  "gingerbread lawyer:made up tracker:118:unhinged survivor:Monkey Point:119"));

      try (cleanups) {
        assertFalse(isTracked("gingerbread lawyer"));
        assertTrue(isTracked("unhinged survivor"));
      }
    }
  }

  @Nested
  class Recalculate {

    @Test
    void recalculate() {
      var cleanups = new Cleanups(withCurrentRun(), withTrackedMonsters(""));

      try (cleanups) {
        // This will be removed because it's run out.
        KoLCharacter.setCurrentRun(69);
        TrackManager.trackMonster(CRATE, Tracker.LATTE);

        KoLCharacter.setCurrentRun(419);
        TrackManager.trackMonster(SMUT_ORC_NAILER, Tracker.MONKEY_POINT);

        KoLCharacter.setCurrentRun(420);
        TrackManager.trackMonster(MAGICAL_FRUIT_BAT, Tracker.SUPERFICIAL);

        TrackManager.recalculate();

        assertThat(
            "trackedMonsters",
            isSetTo(
                "smut orc nailer:Monkey Point:419:magical fruit bat:Be Superficially interested:420"));
      }
    }

    @Test
    void recalculateSortsNonMatchingPrefs() {
      var cleanups = new Cleanups(withCurrentRun(420), withTrackedMonsters("crate:snokebomb:69"));

      try (cleanups) {
        TrackManager.trackMonster(SMUT_ORC_NAILER, Tracker.CREAM_JIGGLE);
        TrackManager.recalculate();

        assertThat(
            "trackedMonsters", isSetTo("smut orc nailer:Staff of the Cream of the Cream:420"));
      }
    }
  }

  @Nested
  class Reset {

    @Test
    void resetRollover() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withTrackedMonsters(
                  "spooky vampire:Gallapagosian Mating Call:114:smut orc nailer:Offer Latte to Opponent:115:gingerbread lawyer:Staff of the Cream of the Cream:118:Elf Guard armorer:prank Crimbo card:119"));

      try (cleanups) {
        TrackManager.resetRollover();

        assertThat("trackedMonsters", isSetTo(""));
      }
    }

    @Test
    void resetAvatar() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withTrackedMonsters(
                  "smut orc nailer:Make Friends:115:gingerbread lawyer:Curse of Stench:118:unhinged survivor:Long Con:119:grizzled survivor:Motif:119:spooky vampire:Gallapagosian Mating Call:120"));

      try (cleanups) {
        TrackManager.resetAvatar();

        assertThat("trackedMonsters", isSetTo("spooky vampire:Gallapagosian Mating Call:120"));
      }
    }

    @Test
    void resetAscension() {
      var cleanups =
          new Cleanups(
              withCurrentRun(128),
              withTrackedMonsters(
                  "smut orc nailer:Transcendent Olfaction:115:gingerbread lawyer:Monkey Point:118:unhinged survivor:Staff of the Cream of the Cream:119:spooky vampire:Gallapagosian Mating Call:120"));
      try (cleanups) {
        TrackManager.resetAscension();

        assertThat("trackedMonsters", isSetTo(""));
      }
    }

    @Nested
    class EffectReset {
      @ParameterizedTest
      @CsvSource(
          value = {
            EffectPool.A_BEASTLY_ODOR + "|A Beastly Odor",
            EffectPool.EW_THE_HUMANITY + "|Ew, The Humanity",
          },
          delimiter = '|')
      void effectTracksStayWithEffect(int effectId, String trackName) {
        var cleanups =
            new Cleanups(
                withCurrentRun(4),
                withEffect(effectId),
                withTrackedPhyla("crate:" + trackName + ":3"));

        try (cleanups) {
          TrackManager.recalculate();

          assertThat("trackedPhyla", isSetTo("crate:" + trackName + ":3"));
        }
      }

      @ParameterizedTest
      @ValueSource(
          strings = {
            "A Beastly Odor",
            "Ew, The Humanity",
          })
      void effectTracksExpireWithoutEffect(String trackName) {
        var cleanups =
            new Cleanups(withCurrentRun(4), withTrackedPhyla("crate:" + trackName + ":3"));

        try (cleanups) {
          TrackManager.recalculate();

          assertThat("trackedPhyla", isSetTo(""));
        }
      }
    }
  }

  @Nested
  class TrackMonster {
    @Test
    void trackCurrentMonster() {
      var cleanups =
          new Cleanups(
              withCurrentRun(123), withProperty("trackedMonsters"), withNextMonster("W imp"));

      try (cleanups) {
        TrackManager.trackCurrentMonster(Tracker.MOTIF);
        assertTrue(isTracked("W imp"));
      }
    }

    @Test
    void trackCurrentMonsterWithNoCurrentMonster() {
      var cleanups =
          new Cleanups(
              withCurrentRun(123),
              withTrackedMonsters("spooky vampire:ice house:0"),
              withNextMonster((MonsterData) null));

      try (cleanups) {
        TrackManager.trackCurrentMonster(Tracker.PERCEIVE_SOUL);

        // Still well-formed
        assertThat("trackedMonsters", isSetTo("spooky vampire:ice house:0"));
      }
    }

    @Test
    void trackMonster() {
      var cleanups = new Cleanups(withCurrentRun(123), withProperty("trackedMonsters"));

      try (cleanups) {
        TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.LONG_CON);

        assertTrue(isTracked("spooky mummy"));
      }
    }

    @Test
    void trackMonsterWorksOnRetrack() {
      var cleanups = new Cleanups(withCurrentRun(1), withProperty("trackedMonsters"));

      try (cleanups) {
        TrackManager.trackMonster(ELF_GUARD_ARMORER, Tracker.PRANK_CARD);
        assertThat("trackedMonsters", isSetTo("Elf Guard armorer:prank Crimbo card:1"));

        TrackManager.trackMonster(ELF_GUARD_ARMORER, Tracker.PRANK_CARD);

        assertThat("trackedMonsters", isSetTo("Elf Guard armorer:prank Crimbo card:1"));
      }
    }

    @Test
    void oneExpiringTrackLeavesTheOther() {
      var cleanups = new Cleanups(withCurrentRun(), withProperty("trackedMonsters"));

      try (cleanups) {
        KoLCharacter.setCurrentRun(100);
        TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.LATTE);
        KoLCharacter.setCurrentRun(105);
        TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.PERCEIVE_SOUL);
        KoLCharacter.setCurrentRun(131);
        assertThat("trackedMonsters", isSetTo("spooky mummy:Perceive Soul:105"));
      }
    }

    @Test
    void oneOverwritingTrackLeavesTheOther() {
      var cleanups = new Cleanups(withCurrentRun(), withProperty("trackedMonsters"));

      try (cleanups) {
        KoLCharacter.setCurrentRun(100);
        TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.LATTE);
        KoLCharacter.setCurrentRun(105);
        TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.MAKE_FRIENDS);
        KoLCharacter.setCurrentRun(106);
        TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.MAKE_FRIENDS);
        assertThat(
            "trackedMonsters",
            isSetTo("spooky mummy:Offer Latte to Opponent:100:spooky mummy:Make Friends:106"));
      }
    }

    @Test
    void oneOverwritingUnrelatedTrackLeavesTheOther() {
      var cleanups = new Cleanups(withCurrentRun(), withProperty("trackedMonsters"));

      try (cleanups) {
        KoLCharacter.setCurrentRun(100);
        TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.LATTE);
        KoLCharacter.setCurrentRun(105);
        TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.MAKE_FRIENDS);
        KoLCharacter.setCurrentRun(106);
        TrackManager.trackMonster(SCARY_PIRATE, Tracker.MAKE_FRIENDS);
        assertThat(
            "trackedMonsters",
            isSetTo("spooky mummy:Offer Latte to Opponent:100:scary pirate:Make Friends:106"));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "152, true",
      "153, false",
    })
    void trackMonsterCorrectOnTurnCost(final int turns, final boolean tracked) {
      var cleanups = new Cleanups(withCurrentRun(123), withProperty("trackedMonsters"));

      try (cleanups) {
        TrackManager.trackMonster(TAN_GNAT, Tracker.LATTE);

        KoLCharacter.setCurrentRun(turns);
        assertThat(isTracked("Tan Gnat"), equalTo(tracked));
      }
    }

    @Nested
    class Legacy {
      @Test
      void olfactedMonster() {
        var cleanups =
            new Cleanups(withProperty("banishedMonsters"), withProperty("olfactedMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.OLFACTION);

          assertTrue(isTracked("spooky mummy"));
          assertThat("olfactedMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void nosyNoseMonster() {
        var cleanups =
            new Cleanups(
                withProperty("banishedMonsters"), withNosyNose(), withProperty("nosyNoseMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.NOSY_NOSE);

          assertTrue(isTracked("spooky mummy"));
          assertThat("nosyNoseMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void gallapagosMonster() {
        var cleanups =
            new Cleanups(withProperty("banishedMonsters"), withProperty("_gallapagosMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.GALLAPAGOS);

          assertTrue(isTracked("spooky mummy"));
          assertThat("_gallapagosMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void latteMonster() {
        var cleanups =
            new Cleanups(withProperty("banishedMonsters"), withProperty("_latteMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.LATTE);

          assertTrue(isTracked("spooky mummy"));
          assertThat("_latteMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void superficiallyInterestedMonster() {
        var cleanups =
            new Cleanups(
                withProperty("banishedMonsters"), withProperty("superficiallyInterestedMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.SUPERFICIAL);

          assertTrue(isTracked("spooky mummy"));
          assertThat("superficiallyInterestedMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void jiggleCreamedMonster() {
        var cleanups =
            new Cleanups(withProperty("banishedMonsters"), withProperty("_jiggleCreamedMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.CREAM_JIGGLE);

          assertTrue(isTracked("spooky mummy"));
          assertThat("_jiggleCreamedMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void makeFriendsMonster() {
        var cleanups =
            new Cleanups(withProperty("banishedMonsters"), withProperty("makeFriendsMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.MAKE_FRIENDS);

          assertTrue(isTracked("spooky mummy"));
          assertThat("makeFriendsMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void stenchCursedMonster() {
        var cleanups =
            new Cleanups(withProperty("banishedMonsters"), withProperty("stenchCursedMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.CURSE_OF_STENCH);

          assertTrue(isTracked("spooky mummy"));
          assertThat("stenchCursedMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void longConMonster() {
        var cleanups =
            new Cleanups(withProperty("banishedMonsters"), withProperty("longConMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.LONG_CON);

          assertTrue(isTracked("spooky mummy"));
          assertThat("longConMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void motifMonster() {
        var cleanups = new Cleanups(withProperty("banishedMonsters"), withProperty("motifMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.MOTIF);

          assertTrue(isTracked("spooky mummy"));
          assertThat("motifMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void monkeyPointMonster() {
        var cleanups =
            new Cleanups(withProperty("banishedMonsters"), withProperty("monkeyPointMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.MONKEY_POINT);

          assertTrue(isTracked("spooky mummy"));
          assertThat("monkeyPointMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void prankCardMonster() {
        var cleanups =
            new Cleanups(withProperty("banishedMonsters"), withProperty("_prankCardMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.PRANK_CARD);

          assertTrue(isTracked("spooky mummy"));
          assertThat("_prankCardMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void trickCoinMonster() {
        var cleanups =
            new Cleanups(withProperty("banishedMonsters"), withProperty("_trickCoinMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.TRICK_COIN);

          assertTrue(isTracked("spooky mummy"));
          assertThat("_trickCoinMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void holdHandsMonster() {
        var cleanups =
            new Cleanups(withProperty("banishedMonsters"), withProperty("holdHandsMonster"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.HOLD_HANDS);

          assertTrue(isTracked("spooky mummy"));
          assertThat("holdHandsMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void redSnapperPhylum() {
        var cleanups =
            new Cleanups(
                withProperty("banishedPhyla"), withSnapper(), withProperty("redSnapperPhylum"));

        try (cleanups) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.RED_SNAPPER);

          assertTrue(isTracked("spooky mummy"));
          assertThat("redSnapperPhylum", isSetTo("undead"));
        }
      }
    }
  }
}
