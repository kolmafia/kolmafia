package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
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
      int estimatedCount = AscensionSeedManager.estimatedAscensionSeedPossibilities();
      assertThat(estimatedCount, is(2));

      List<Integer> matches = AscensionSeedManager.possibleAscensionSeeds();
      assertThat(matches.size(), is(4));
      assertThat(matches, contains(1062694, 2209935, 5572718, 7252442));
    }
  }

  @Test
  void returnSizeLimited() {
    var cleanups = new Cleanups(withProperty("dailyDungeonRooms", "T???_????_????"));

    try (cleanups) {
      int estimatedCount = AscensionSeedManager.estimatedAscensionSeedPossibilities();
      assertThat(estimatedCount, is(3_000_000));

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
                withProperty("lastBangPotion825", "teleportitis"));

        try (cleanups) {
          Criterion c = new AscensionSeedManager.BangPotions();
          assertThat(c.matchesSeed(seed), is(expected));
        }
      }
    }

    @Nested
    class CardCatalog {
      @ParameterizedTest
      @CsvSource({
        "1234567,TN117.43|YF156.29|OZ980.28|OS533.13|FU892.87|TL186.71|AC701.85|JI145.98|MY679.95|MQ420.27|UU920.57|DV791.78|MD418.18",
        "2345678,KN118.99|LU120.13|HZ497.45|EZ848.51|SX963.79|CF674.23|QM227.14|GS958.56|UI521.52|BL607.31|RN604.43|BA717.68|SG434.71",
        "7777777,RL672.15|CN799.60|HU568.36|IS600.91|MT918.59|RL678.12|PZ451.51|DC701.12|LV820.88|HV338.90|QZ280.64|ES965.12|XH650.28",
      })
      void calculates(int seed, String books) {
        Criterion c = new AscensionSeedManager.CardCatalog();
        Map<String, String> result = c.toProperties(seed);
        for (String book : books.split("\\|")) {
          assertThat(result.get("merkinCatalogChoices"), containsString(book));
        }
      }

      @ParameterizedTest
      @CsvSource({
        "1234567,false",
        "8675309,false",
        "7777777,true",
      })
      void predicts(int seed, boolean expected) {
        var cleanups =
            withProperty(
                "merkinCatalogChoices", "CN799.60:1:unknown,HU568.36:2:unknown,RL672.15:3:unknown");

        try (cleanups) {
          Criterion c = new AscensionSeedManager.CardCatalog();
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
    class IntriguingPuzzleBox {
      private static List<String> optionsFor(int seed) {
        var stored =
            AscensionSeedManager.getPropertiesForSeed(seed).get("intriguingPuzzleBoxOptions");
        return List.of(stored.substring("0:".length()).split(","));
      }

      private static Cleanups withStores(final String options, final String outcomes) {
        return new Cleanups(
            withProperty("intriguingPuzzleBoxOptions", options),
            withProperty("intriguingPuzzleBoxOutcomes", outcomes));
      }

      @Test
      void recordedOptionsNarrowToSeed() {
        var seed = 1062694;
        try (var cleanups = withStores("0:" + String.join(",", optionsFor(seed)), "")) {
          List<Integer> matches = AscensionSeedManager.possibleAscensionSeeds();
          assertThat(matches, hasItem(seed));
        }
      }

      @Test
      void matchesOnlyTheSeedWithTheRecordedOptions() {
        try (var cleanups = withStores("0:FCW,PB,RCCW,LCW,KCW,PR", "")) {
          var criterion = new AscensionSeedManager.IntriguingPuzzleBox();
          List<Integer> seeds = AscensionSeedManager.possibleAscensionSeeds();
          assertThat(seeds.size(), is(1));
          assertThat(criterion.matchesSeed(seeds.getFirst()), is(true));
          assertThat(criterion.matchesSeed(1234567), is(false));
        }
      }

      @Test
      void calculatesRecordedOptionsForSeed() {
        var criterion = new AscensionSeedManager.IntriguingPuzzleBox();
        assertThat(
            criterion.toProperties(1234567).get("intriguingPuzzleBoxOptions"),
            is("0:PR,FCCW,LCW,BCW,PK,LCCW"));
      }

      @Test
      void outcomesNarrowToSeedFromFirstPage() {
        try (var cleanups =
            withStores(
                "0:FCW,PB,RCCW,LCW,KCW,PR|1:FCW,TCW,TCCW,PT,FCCW,PR|2:BCW,PB,PF,RCW,BCCW,KCW",
                "0,1:click|0,3:buzz|1,1:click")) {
          assertThat(AscensionSeedManager.possibleAscensionSeeds(), is(List.of(2340482)));
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

    @Nested
    class VioletFog {
      @Test
      void calculates() {
        int seed = 7777777;
        Criterion c = new AscensionSeedManager.VioletFog();
        Map<String, String> result = c.toProperties(seed);
        assertThat(
            result.get("violetFogLayout"),
            is(
                "49,51,50,-1,53,56,52,-1,54,53,57,-1,52,54,55,-1,68,61,65,-1,66,61,69,-1,67,70,61,-1,65,58,70,-1,68,59,66,-1,69,60,67,-1,52,63,51,-1,62,49,53,-1,50,64,54,-1,49,50,51,-1,-1,61,50,52,-1,53,61,51,-1,54,61,49,-1,54,51,50,-1,51,52,49,-1,50,53,49,-1,50,53,49,-1,54,51,50,-1,49,52,51"));
      }

      @ParameterizedTest
      @CsvSource({
        "1234567,false",
        "8675309,false",
        "7777777,true",
        "4309003,true",
      })
      void predicts(int seed, boolean expected) {
        var cleanups =
            withProperty(
                "violetFogLayout",
                "49,0,0,-1,53,0,0,-1,54,0,0,-1,52,0,0,-1,0,61,0,-1,0,61,0,-1,0,70,0,-1,0,58,0,-1,0,59,0,-1,0,0,67,-1,0,0,51,-1,0,0,53,-1,0,0,0,-1,0,0,0,-1,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0");
        try (cleanups) {
          Criterion c = new AscensionSeedManager.VioletFog();
          assertThat(c.matchesSeed(seed), is(expected));
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
}
