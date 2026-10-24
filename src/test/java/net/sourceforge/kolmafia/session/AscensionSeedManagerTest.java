package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import internal.helpers.Cleanups;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.AscensionSeedManager.Criterion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AscensionSeedManagerTest {

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("AscensionSeedManager");
    Preferences.reset("AscensionSeedManager");
  }

  @Test
  void combinesCriteriaToNarrowSeed() {
    var cleanups =
        new Cleanups(
            withProperty("dailyDungeonRooms", "MMTD_??MT_??DD"),
            withProperty("seahorseName", "Glitterswim"),
            withProperty("lastBangPotion819", "confusion"),
            withProperty("lastBangPotion820", "inebriety"));

    try (cleanups) {
      List<Integer> matches = AscensionSeedManager.possibleAscensionSeeds();
      assertThat(matches.size(), is(4));
      assertThat(matches, contains(1062694, 2209935, 5572718, 7252442));
    }
  }

  @Test
  void returnSizeLimited() {
    var cleanups = new Cleanups(withProperty("dailyDungeonRooms", "T???_????_????"));

    try (cleanups) {
      List<Integer> matches = AscensionSeedManager.possibleAscensionSeeds();
      assertThat(matches.size(), is(AscensionSeedManager.MAX_CALCULATED_SEEDS));
    }
  }

  @Nested
  class Criteria {
    @Nested
    class BangPotions {
      @ParameterizedTest
      @CsvSource({
        "1234567,cdithbsem",
        "2345678,ecbidmhst",
        "7777777,eisbhctmd",
      })
      void calculates(int seed, String potionInitials) {
        Criterion c = new AscensionSeedManager.BangPotions();
        Map<String, String> result = c.toProperties(seed);
        for (int i = 0; i < 9; i++) {
          assertThat(
              result.get("lastBangPotion" + (819 + i)),
              startsWith(potionInitials.substring(i, i + 1)));
        }
      }

      @ParameterizedTest
      @CsvSource({
        "1234567,false",
        "8675309,false",
        "7777777,true", // eisbhctmd
        "6928592,true", // eisbhctdm
      })
      void predicts(int seed, boolean expected) {
        var cleanups =
            new Cleanups(
                withProperty("lastBangPotion819", "ettin strength"),
                withProperty("lastBangPotion820", "inebriety"),
                withProperty("lastBangPotion821", "sleepiness"),
                withProperty("lastBangPotion822", "blessing"),
                withProperty("lastBangPotion823", "healing"),
                withProperty("lastBangPotion824", "confusion"),
                withProperty("lastBangPotion825", "teleportitis"),
                withProperty("lastBangPotion826"),
                withProperty("lastBangPotion827"));

        try (cleanups) {
          Criterion c = new AscensionSeedManager.BangPotions();
          assertThat(c.matchesSeed(seed), is(expected));
        }
      }
    }

    @Nested
    class DailyDungeon {
      @ParameterizedTest
      @CsvSource({
        "1234567,MDMM_DTDT_TMDT",
        "2345678,MTTM_DTMD_TDMD",
        "7777777,DMTM_TMDD_MTTD",
      })
      void calculates(int seed, String dungeonRooms) {
        Criterion c = new AscensionSeedManager.DailyDungeon();
        Map<String, String> result = c.toProperties(seed);
        assertThat(result.get("dailyDungeonRooms"), is(dungeonRooms));
      }

      @ParameterizedTest
      @CsvSource({
        "1234567,false",
        "8675309,false",
        "7777777,true", // DMTM_TMDD_MTTD
        "3773110,true", // DMTM_MMDD_TTTD
      })
      void predicts(int seed, boolean expected) {
        var cleanups = withProperty("dailyDungeonRooms", "DMTM_??DD_??TD");

        try (cleanups) {
          Criterion c = new AscensionSeedManager.DailyDungeon();
          assertThat(c.matchesSeed(seed), is(expected));
        }
      }
    }

    @Nested
    class Dreadscroll {
      @ParameterizedTest
      @CsvSource({
        "1234567,43124124",
        "2345678,23144244",
        "7777777,32314113",
      })
      void calculates(int seed, String dreadscroll) {
        Criterion c = new AscensionSeedManager.Dreadscroll();
        Map<String, String> result = c.toProperties(seed);
        for (int i = 1; i <= 8; i++) {
          assertThat(result.get("dreadScroll" + i), is(dreadscroll.substring(i - 1, i)));
        }
      }

      @ParameterizedTest
      @CsvSource({
        "1234567,false",
        "8675309,false",
        "7777777,true", // 32314113
        "8884557,true", // 32314122
      })
      void predicts(int seed, boolean expected) {
        var cleanups =
            new Cleanups(
                withProperty("dreadScroll1", 3),
                withProperty("dreadScroll2", 2),
                withProperty("dreadScroll3", 3),
                withProperty("dreadScroll4", 1),
                withProperty("dreadScroll5", 4),
                withProperty("dreadScroll6", 1));

        try (cleanups) {
          Criterion c = new AscensionSeedManager.Dreadscroll();
          assertThat(c.matchesSeed(seed), is(expected));
        }
      }
    }

    @Nested
    class LeprecondoNeeds {
      @ParameterizedTest
      @CsvSource({
        "1234567,msdefb",
        "2345678,bfmsed",
        "7777777,fmesbd",
      })
      void calculates(int seed, String needInitials) {
        Criterion c = new AscensionSeedManager.LeprecondoNeeds();
        Map<String, String> result = c.toProperties(seed);
        String[] parts = result.get("leprecondoNeedOrder").split(",");
        assertThat(parts.length, is(6));
        for (int i = 0; i < 6; i++) {
          assertThat(parts[i], startsWith(needInitials.substring(i, i + 1)));
        }
      }

      @ParameterizedTest
      @CsvSource({
        "1234567,true", // not a match, but condo always matches
        "8675309,true", // not a match, but condo always matches
        "7777777,true", // fmesbd
        "2305759,true", // fmesdb
      })
      void predicts(int seed, boolean expected) {
        var cleanups =
            withProperty("leprecondoNeedOrder", "food,mental stimulation,exercise,sleep");

        try (cleanups) {
          Criterion c = new AscensionSeedManager.LeprecondoNeeds();
          assertThat(c.matchesSeed(seed), is(expected));
        }
      }
    }

    @Nested
    class RaveCombos {
      @ParameterizedTest
      @CsvSource({
        "1234567,BPR|PRB|PBR|RPB|BRP|RBP",
        "2345678,RPB|BPR|BRP|PRB|RBP|PBR",
        "7777777,RPB|RBP|BPR|BRP|PRB|PBR",
      })
      void calculates(int seed, String comboInitials) {
        Criterion c = new AscensionSeedManager.RaveCombos();
        Map<String, String> result = c.toProperties(seed);
        String[] expectedCombos = comboInitials.split("\\|");
        for (int i = 0; i < 6; i++) {
          String pref = result.get("raveCombo" + (i + 1));
          String[] moves = pref.split(",");
          String initials =
              moves[0].substring(0, 1) + moves[1].substring(0, 1) + moves[2].substring(0, 1);
          assertThat(initials, is(expectedCombos[i]));
        }
      }

      @ParameterizedTest
      @CsvSource({
        "1234567,false",
        "8675309,false",
        "7777777,true", // RPB|RBP|BPR|BRP|PRB|PBR
        "6703818,true", // RPB|RBP|BPR|PRB|BRP|PBR
      })
      void predicts(int seed, boolean expected) {
        var cleanups =
            new Cleanups(
                withProperty("raveCombo1", "Run Like the Wind,Pop and Lock It,Break It On Down"),
                withProperty("raveCombo2", "Run Like the Wind,Break It On Down,Pop and Lock It"),
                withProperty("raveCombo3", "Break It On Down,Pop and Lock It,Run Like the Wind"),
                withProperty("raveCombo6", "Pop and Lock It,Break It On Down,Run Like the Wind"));

        try (cleanups) {
          Criterion c = new AscensionSeedManager.RaveCombos();
          assertThat(c.matchesSeed(seed), is(expected));
        }
      }
    }

    @Nested
    class SeahorseName {
      @ParameterizedTest
      @CsvSource({
        "1234567,Flickerswim",
        "2345678,Pokey Splash",
        "7777777,Shimmerswim",
        "4203767,Grapefruitjack",
      })
      void calculates(int seed, String name) {
        Criterion c = new AscensionSeedManager.SeahorseName();
        Map<String, String> result = c.toProperties(seed);
        assertThat(result.get("seahorseName"), is(name));
      }

      @ParameterizedTest
      @CsvSource({
        "1234567,false",
        "8675309,false",
        "7777777,true", // Shimmerswim
      })
      void predicts(int seed, boolean expected) {
        var cleanups = withProperty("seahorseName", "Shimmerswim");

        try (cleanups) {
          Criterion c = new AscensionSeedManager.SeahorseName();
          assertThat(c.matchesSeed(seed), is(expected));
        }
      }
    }

    @Nested
    class SlimePotions {
      @Test
      void calculates() {
        int seed = 7777777;
        Criterion c = new AscensionSeedManager.SlimePotions();
        Map<String, String> result = c.toProperties(seed);
        assertThat(result.get("lastSlimeVial3885"), is("strong"));
        assertThat(result.get("lastSlimeVial3886"), is("sagacious"));
        assertThat(result.get("lastSlimeVial3887"), is("speedy"));
        assertThat(result.get("lastSlimeVial3888"), is("brawn"));
        assertThat(result.get("lastSlimeVial3889"), is("briskness"));
        assertThat(result.get("lastSlimeVial3890"), is("brains"));
        assertThat(result.get("lastSlimeVial3891"), is("intensity"));
        assertThat(result.get("lastSlimeVial3892"), is("mentalism"));
        assertThat(result.get("lastSlimeVial3893"), is("muscle"));
        assertThat(result.get("lastSlimeVial3894"), is("slimeform"));
        assertThat(result.get("lastSlimeVial3895"), is("moxiousness"));
        assertThat(result.get("lastSlimeVial3896"), is("eyesight"));
      }

      @ParameterizedTest
      @CsvSource({
        "1234567,false",
        "8675309,false",
        "7777777,true", // 123_132_i21s3e
        "6703818,true", // 123_132_2i1s3e
      })
      void predicts(int seed, boolean expected) {
        var cleanups =
            new Cleanups(
                withProperty("lastSlimeVial3887", "speedy"),
                withProperty("lastSlimeVial3890", "brains"),
                withProperty("lastSlimeVial3893", "muscle"),
                withProperty("lastSlimeVial3894", "slimeform"),
                withProperty("lastSlimeVial3895", "moxiousness"));

        try (cleanups) {
          Criterion c = new AscensionSeedManager.SlimePotions();
          assertThat(c.matchesSeed(seed), is(expected));
        }
      }
    }
  }

  @Nested
  class YegDemon {
    @ParameterizedTest
    @CsvSource({
      "1234567,Yego Yegu Gigoyo",
      "2345678,Geyu Yegi Gegoga",
      "7777777,Gigi Gayo Gugaye",
    })
    void calculates(int seed, String name) {
      Criterion c = new AscensionSeedManager.YegDemon();
      Map<String, String> result = c.toProperties(seed);
      assertThat(result.get("demonName13"), is(name));
    }

    @ParameterizedTest
    @CsvSource({
      "1234567,false",
      "8675309,false",
      "7777777,true", // Gigi Gayo Gugaye
    })
    void predicts(int seed, boolean expected) {
      var cleanups = withProperty("demonName13", "Gigi Gayo Gugaye");

      try (cleanups) {
        Criterion c = new AscensionSeedManager.YegDemon();
        assertThat(c.matchesSeed(seed), is(expected));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "1234567,false",
      "8675309,false",
      "7777777,true", // Gigi Gayo Gugaye
      "9959476,true", // Yogi Gayo Guyiye
    })
    void predictsFromScraps(int seed, boolean expected) {
      var cleanups =
          new Cleanups(
              withProperty("demonName13"),
              withProperty("cargoPocketScraps", "222:gi_|7:Ga|602:yo_|172:Gu|282:ye"));

      try (cleanups) {
        Criterion c = new AscensionSeedManager.YegDemon();
        assertThat(c.matchesSeed(seed), is(expected));
      }
    }
  }
}
