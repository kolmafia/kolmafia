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
  class LoadTracked {

    @Test
    void loadTrackedMonsters() {
      try (var _ =
          new Cleanups(
              withCurrentRun(128),
              withNosyNose(),
              withTrackedMonsters(
                  "gingerbread lawyer:Transcendent Olfaction:118:unhinged survivor:Nosy Nose:119:grizzled survivor:Gallapagosian Mating Call:119:cat-alien:Offer Latte to Opponent:119:alielf:Monkey Point:119:whiny survivor:Be Superficially interested:119"))) {
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
      try (var _ =
          new Cleanups(
              withCurrentRun(1),
              withBeastlyOdor(),
              withEwTheHumanity(),
              withSnapper(),
              withTrackedPhyla(
                  "beast:A Beastly Odor:1:dude:Ew, The Humanity:1:fish:Red-Nosed Snapper:1"))) {
        assertTrue(isTracked("vampire bat"));
        assertTrue(isTracked("unhinged survivor"));
        assertTrue(isTracked("clubfish"));

        assertFalse(isTracked("zmobie"));
      }
    }

    @Test
    void loadTrackedMonstersSkipsInvalidTracker() {
      try (var _ =
          new Cleanups(
              withCurrentRun(128),
              withTrackedMonsters(
                  "gingerbread lawyer:made up tracker:118:unhinged survivor:Monkey Point:119"))) {
        assertFalse(isTracked("gingerbread lawyer"));
        assertTrue(isTracked("unhinged survivor"));
      }
    }
  }

  @Nested
  class Recalculate {

    @Test
    void recalculate() {
      try (var _ = new Cleanups(withCurrentRun(), withTrackedMonsters(""))) {
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
      try (var _ = new Cleanups(withCurrentRun(420), withTrackedMonsters("crate:snokebomb:69"))) {
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
      try (var _ =
          new Cleanups(
              withCurrentRun(128),
              withTrackedMonsters(
                  "spooky vampire:Gallapagosian Mating Call:114:smut orc nailer:Offer Latte to Opponent:115:gingerbread lawyer:Staff of the Cream of the Cream:118:Elf Guard armorer:prank Crimbo card:119"))) {
        TrackManager.resetRollover();

        assertThat("trackedMonsters", isSetTo(""));
      }
    }

    @Test
    void resetAvatar() {
      try (var _ =
          new Cleanups(
              withCurrentRun(128),
              withTrackedMonsters(
                  "smut orc nailer:Make Friends:115:gingerbread lawyer:Curse of Stench:118:unhinged survivor:Long Con:119:grizzled survivor:Motif:119:spooky vampire:Gallapagosian Mating Call:120"))) {
        TrackManager.resetAvatar();

        assertThat("trackedMonsters", isSetTo("spooky vampire:Gallapagosian Mating Call:120"));
      }
    }

    @Test
    void resetAscension() {
      try (var _ =
          new Cleanups(
              withCurrentRun(128),
              withTrackedMonsters(
                  "smut orc nailer:Transcendent Olfaction:115:gingerbread lawyer:Monkey Point:118:unhinged survivor:Staff of the Cream of the Cream:119:spooky vampire:Gallapagosian Mating Call:120"))) {
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
        try (var _ =
            new Cleanups(
                withCurrentRun(4),
                withEffect(effectId),
                withTrackedPhyla("crate:" + trackName + ":3"))) {
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
        try (var _ =
            new Cleanups(withCurrentRun(4), withTrackedPhyla("crate:" + trackName + ":3"))) {
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
      try (var _ =
          new Cleanups(
              withCurrentRun(123), withProperty("trackedMonsters"), withNextMonster("W imp"))) {
        TrackManager.trackCurrentMonster(Tracker.MOTIF);
        assertTrue(isTracked("W imp"));
      }
    }

    @Test
    void trackCurrentMonsterWithNoCurrentMonster() {
      try (var _ =
          new Cleanups(
              withCurrentRun(123),
              withTrackedMonsters("spooky vampire:ice house:0"),
              withNextMonster((MonsterData) null))) {
        TrackManager.trackCurrentMonster(Tracker.PERCEIVE_SOUL);

        // Still well-formed
        assertThat("trackedMonsters", isSetTo("spooky vampire:ice house:0"));
      }
    }

    @Test
    void trackMonster() {
      try (var _ = new Cleanups(withCurrentRun(123), withProperty("trackedMonsters"))) {
        TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.LONG_CON);

        assertTrue(isTracked("spooky mummy"));
      }
    }

    @Test
    void trackMonsterWorksOnRetrack() {
      try (var _ = new Cleanups(withCurrentRun(1), withProperty("trackedMonsters"))) {
        TrackManager.trackMonster(ELF_GUARD_ARMORER, Tracker.PRANK_CARD);
        assertThat("trackedMonsters", isSetTo("Elf Guard armorer:prank Crimbo card:1"));

        TrackManager.trackMonster(ELF_GUARD_ARMORER, Tracker.PRANK_CARD);

        assertThat("trackedMonsters", isSetTo("Elf Guard armorer:prank Crimbo card:1"));
      }
    }

    @Test
    void oneExpiringTrackLeavesTheOther() {
      try (var _ = new Cleanups(withCurrentRun(), withProperty("trackedMonsters"))) {
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
      try (var _ = new Cleanups(withCurrentRun(), withProperty("trackedMonsters"))) {
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
      try (var _ = new Cleanups(withCurrentRun(), withProperty("trackedMonsters"))) {
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
      try (var _ = new Cleanups(withCurrentRun(123), withProperty("trackedMonsters"))) {
        TrackManager.trackMonster(TAN_GNAT, Tracker.LATTE);

        KoLCharacter.setCurrentRun(turns);
        assertThat(isTracked("Tan Gnat"), equalTo(tracked));
      }
    }

    @Nested
    class Legacy {
      @Test
      void olfactedMonster() {
        try (var _ =
            new Cleanups(withProperty("banishedMonsters"), withProperty("olfactedMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.OLFACTION);

          assertTrue(isTracked("spooky mummy"));
          assertThat("olfactedMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void nosyNoseMonster() {
        try (var _ =
            new Cleanups(
                withProperty("banishedMonsters"),
                withNosyNose(),
                withProperty("nosyNoseMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.NOSY_NOSE);

          assertTrue(isTracked("spooky mummy"));
          assertThat("nosyNoseMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void gallapagosMonster() {
        try (var _ =
            new Cleanups(withProperty("banishedMonsters"), withProperty("_gallapagosMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.GALLAPAGOS);

          assertTrue(isTracked("spooky mummy"));
          assertThat("_gallapagosMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void latteMonster() {
        try (var _ =
            new Cleanups(withProperty("banishedMonsters"), withProperty("_latteMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.LATTE);

          assertTrue(isTracked("spooky mummy"));
          assertThat("_latteMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void superficiallyInterestedMonster() {
        try (var _ =
            new Cleanups(
                withProperty("banishedMonsters"), withProperty("superficiallyInterestedMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.SUPERFICIAL);

          assertTrue(isTracked("spooky mummy"));
          assertThat("superficiallyInterestedMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void jiggleCreamedMonster() {
        try (var _ =
            new Cleanups(withProperty("banishedMonsters"), withProperty("_jiggleCreamedMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.CREAM_JIGGLE);

          assertTrue(isTracked("spooky mummy"));
          assertThat("_jiggleCreamedMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void makeFriendsMonster() {
        try (var _ =
            new Cleanups(withProperty("banishedMonsters"), withProperty("makeFriendsMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.MAKE_FRIENDS);

          assertTrue(isTracked("spooky mummy"));
          assertThat("makeFriendsMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void stenchCursedMonster() {
        try (var _ =
            new Cleanups(withProperty("banishedMonsters"), withProperty("stenchCursedMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.CURSE_OF_STENCH);

          assertTrue(isTracked("spooky mummy"));
          assertThat("stenchCursedMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void longConMonster() {
        try (var _ =
            new Cleanups(withProperty("banishedMonsters"), withProperty("longConMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.LONG_CON);

          assertTrue(isTracked("spooky mummy"));
          assertThat("longConMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void motifMonster() {
        try (var _ = new Cleanups(withProperty("banishedMonsters"), withProperty("motifMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.MOTIF);

          assertTrue(isTracked("spooky mummy"));
          assertThat("motifMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void monkeyPointMonster() {
        try (var _ =
            new Cleanups(withProperty("banishedMonsters"), withProperty("monkeyPointMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.MONKEY_POINT);

          assertTrue(isTracked("spooky mummy"));
          assertThat("monkeyPointMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void prankCardMonster() {
        try (var _ =
            new Cleanups(withProperty("banishedMonsters"), withProperty("_prankCardMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.PRANK_CARD);

          assertTrue(isTracked("spooky mummy"));
          assertThat("_prankCardMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void trickCoinMonster() {
        try (var _ =
            new Cleanups(withProperty("banishedMonsters"), withProperty("_trickCoinMonster"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.TRICK_COIN);

          assertTrue(isTracked("spooky mummy"));
          assertThat("_trickCoinMonster", isSetTo("spooky mummy"));
        }
      }

      @Test
      void redSnapperPhylum() {
        try (var _ =
            new Cleanups(
                withProperty("banishedPhyla"), withSnapper(), withProperty("redSnapperPhylum"))) {
          TrackManager.trackMonster(SPOOKY_MUMMY, Tracker.RED_SNAPPER);

          assertTrue(isTracked("spooky mummy"));
          assertThat("redSnapperPhylum", isSetTo("undead"));
        }
      }
    }
  }

  @Nested
  class Zootomist {
    @Test
    void trackDuration() {
      try (var _ =
          new Cleanups(
              withTrackedMonsters("spooky vampire:Left %n Kick:0"),
              withProperty("zootGraftedFootLeftFamiliar", FamiliarPool.OBSERVER))) {
        assertThat(Tracker.LEFT_ZOOT_KICK.getCopies(), equalTo(5));
      }
    }

    @Test
    void rightKickClearsLeftKick() {
      try (var _ =
          new Cleanups(
              withTrackedMonsters("spooky vampire:Left %n Kick:0"),
              withProperty("zootGraftedFootLeftFamiliar", FamiliarPool.OBSERVER),
              withProperty("zootGraftedFootRightFamiliar", FamiliarPool.HEAT_WAVE))) {
        TrackManager.trackMonster(CRATE, Tracker.RIGHT_ZOOT_KICK);
        assertThat("trackedMonsters", isSetTo("crate:Right %n Kick:0"));
      }
    }

    @Test
    void leftKickClearsRightKick() {
      try (var _ =
          new Cleanups(
              withTrackedMonsters("spooky vampire:Right %n Kick:0"),
              withProperty("zootGraftedFootLeftFamiliar", FamiliarPool.OBSERVER),
              withProperty("zootGraftedFootRightFamiliar", FamiliarPool.HEAT_WAVE))) {
        TrackManager.trackMonster(CRATE, Tracker.LEFT_ZOOT_KICK);
        assertThat("trackedMonsters", isSetTo("crate:Left %n Kick:0"));
      }
    }
  }
}
