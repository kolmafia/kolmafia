package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withAscensions;
import static internal.helpers.Player.withChoice;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class VioletFogManagerTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("VioletFogManagerTest");
    Preferences.reset("VioletFogManagerTest");
  }

  @BeforeEach
  void beforeEach() {
    VioletFogManager.reset();
  }

  @Test
  void resetsOldLayouts() {
    var cleanups =
        new Cleanups(
            withProperty(
                "violetFogLayout",
                "49,0,0,-1,0,53,0,-1,0,0,0,-1,0,0,55,-1,0,0,0,-1,0,66,0,-1,0,0,0,-1,0,0,70,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,51,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0"),
            withProperty("lastVioletFogMap", 2),
            withAscensions(3));

    try (cleanups) {
      VioletFogManager.reset();
      assertThat("violetFogLayout", isSetTo(""));
      assertThat("lastVioletFogMap", isSetTo(3));
    }
  }

  @Test
  void canRenderPlainGraph() {
    var cleanups =
        new Cleanups(
            withProperty("violetFogLayout", ""),
            withProperty("lastVioletFogMap", KoLCharacter.getAscensions()));

    try (cleanups) {
      VioletFogManager.reset();
      var graph = VioletFogManager.generateGraph();
      assertThat(graph, not(containsString(" [tooltip")));
    }
  }

  @Test
  void canRenderStatefulGraph() {
    var cleanups =
        new Cleanups(
            withProperty(
                "violetFogLayout",
                "49,0,0,-1,0,53,0,-1,0,0,0,-1,0,0,55,-1,0,0,0,-1,0,66,0,-1,0,0,0,-1,0,0,70,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,51,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0"),
            withProperty("lastVioletFogMap", KoLCharacter.getAscensions()));

    try (cleanups) {
      VioletFogManager.reset();
      var graph = VioletFogManager.generateGraph();
      assertThat(graph, containsString("66 -> 51 [tooltip=\"this way\" color=blue]"));
      assertThat(graph, containsString("53 -> 66 [tooltip=\"that way\" color=red]"));
      assertThat(graph, containsString("51 -> 55 [tooltip=\"the other way\" color=purple]"));
    }
  }

  @Test
  void canRenderStatefulGraphWithMarkers() {
    var cleanups =
        new Cleanups(
            withProperty(
                "violetFogLayout",
                "49,0,0,-1,0,53,0,-1,0,0,0,-1,0,0,55,-1,0,0,0,-1,0,66,0,-1,0,0,0,-1,0,0,70,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,51,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0,-1,0,0,0"),
            withProperty("violetFogGoal", 3),
            withProperty("lastVioletFogMap", KoLCharacter.getAscensions()));

    ChoiceManager.lastChoice = 49;

    try (cleanups) {
      VioletFogManager.reset();
      var graph = VioletFogManager.generateGraph();
      assertThat(graph, containsString("49 [style=\"filled\" fillcolor=yellow color=red"));
      assertThat(graph, containsString("64 [style=\"filled\" fillcolor=yellow color=green"));
    }

    ChoiceManager.lastChoice = 0;
  }

  @Nested
  class ChoiceDecoration {
    @Test
    void decoratesAVioletFogChoice() {
      var cleanups =
          new Cleanups(
              withProperty(
                  "violetFogLayout",
                  "0,0,0,0,0,0,0,0,57,0,53,0,0,0,0,0,0,0,0,0,0,66,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,67,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,50,0,0,0,0,0,0,0,0,0,0,0,0,0,0"),
              withProperty("lastVioletFogMap", KoLCharacter.getAscensions()),
              withProperty("violetFogGoal", 7));

      try (cleanups) {
        VioletFogManager.reset();
        var choice =
            withChoice(66, 2, html("request/test_choice_violet_fog_66_that_way_to_51.html"));
        try (choice) {
          assertThat(
              "violetFogLayout",
              isSetTo(
                  "0,0,0,0,0,0,0,0,57,0,53,0,0,0,0,0,0,0,0,0,0,66,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,67,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,51,0,0,0,50,0,0,0,0,0,0,0,0,0,0,0,0,0,0"));
        }
      }
    }
  }
}
