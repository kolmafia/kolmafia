package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withPath;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AscensionPath;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SeaMerkinRequestTest {
  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset("SeaMerkinRequestTest");
    Preferences.reset("SeaMerkinRequestTest");
  }

  @Nested
  class SeaPath {
    @Test
    void canDetectShubDefeated() {
      var cleanups =
          new Cleanups(
              withPath(AscensionPath.Path.UNDER_THE_SEA),
              withProperty("merkinQuestPath"),
              withProperty("shubJigguwattDefeated", false));
      try (cleanups) {
        var text = html("request/test_quest_sea_monkee_path_boss_defeated.html");
        SeaMerkinRequest.parseResponse("sea_merkin.php?action=temple&subaction=left", text);
        assertThat("shubJigguwattDefeated", isSetTo(true));
      }
    }

    @Test
    void canDetectYogDefeated() {
      var cleanups =
          new Cleanups(
              withPath(AscensionPath.Path.UNDER_THE_SEA),
              withProperty("merkinQuestPath"),
              withProperty("yogUrtDefeated", false));
      try (cleanups) {
        var text = html("request/test_quest_sea_monkee_path_boss_defeated.html");
        SeaMerkinRequest.parseResponse("sea_merkin.php?action=temple&subaction=right", text);
        assertThat("yogUrtDefeated", isSetTo(true));
      }
    }
  }

  @Nested
  class Colosseum {
    @ParameterizedTest
    @CsvSource({
      "done,done", // Yog-Urt defeated
      "scholar,scholar", // High Priest, but Yog-Urt not defeated
      "gladiator,scholar", // Should not usually happen, but clean it up anyway
      "none,scholar" // Should not usually happen, but clean it up anyway
    })
    void canDetectHighPriest(String before, String after) {
      var cleanups =
          new Cleanups(
              withProperty("merkinQuestPath", before), withProperty("isMerkinHighPriest", false));

      try (cleanups) {
        var text = html("request/test_merkin_colosseum_scholar.html");
        SeaMerkinRequest.parseColosseumResponse("adventure.php?snarfblat=210", text);
        assertThat("merkinQuestPath", isSetTo(after));
        assertThat("isMerkinHighPriest", isSetTo(true));
      }
    }

    @ParameterizedTest
    @CsvSource({
      "done,done", // Shub-Jigguwatt defeated (or in Sea Path and Yog-Urt defeated)
      "gladiator,gladiator", // Gladiator champion, but Shub-Jigguwatt not defeated
      "scholar,gladiator", // Should not usually happen, but clean it up anyway
      "none,gladiator" // Should not usually happen, but clean it up anyway
    })
    void canDetectGladiatorChampion(String before, String after) {
      var cleanups =
          new Cleanups(
              withProperty("merkinQuestPath", before),
              withProperty("isMerkinGladiatorChampion", false),
              withProperty("lastColosseumRoundWon", 14));

      try (cleanups) {
        var text = html("request/test_merkin_colosseum_gladiator.html");
        SeaMerkinRequest.parseColosseumResponse("adventure.php?snarfblat=210", text);
        assertThat("merkinQuestPath", isSetTo(after));
        assertThat("isMerkinGladiatorChampion", isSetTo(true));
        assertThat("lastColosseumRoundWon", isSetTo(15));
      }
    }
  }
}
