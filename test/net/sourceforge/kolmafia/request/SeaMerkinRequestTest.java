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
}
