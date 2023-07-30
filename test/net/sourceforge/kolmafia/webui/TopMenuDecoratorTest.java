package net.sourceforge.kolmafia.webui;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestEditorKit;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.GenericRequest.TopMenuStyle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Enum;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;

class TopMenuDecoratorTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("TopMenuDecoratorTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("TopMenuDecoratorTest");
  }

  public static Cleanups withTopMenuStyle(final TopMenuStyle style) {
    var oldStyle = GenericRequest.topMenuStyle;
    GenericRequest.topMenuStyle = style;
    return new Cleanups(
        () -> {
          GenericRequest.topMenuStyle = oldStyle;
        });
  }

  @Nested
  class AwesomeMenu {
    private String playerToTextFile(String player) {
      // Veracity's responseText got decorated correctly
      // Captain Scotch's responseText did not.
      //
      // I see no significant difference.
      // TopMenuDecorator.decorate sees no difference.
      //
      // This is mainly to confirm that it's not a difference in KoL's
      // responseText that made Captain Scotch's script menu disappear
      return switch (player) {
        case "Veracity" -> "test_awesomemenu_1";
        case "Captain Scotch" -> "test_awesomemenu_2";
        default -> "bogus";
      };
    }

    @CartesianTest
    void awesomeMenuIsAwesome(
        @Values(strings = {"Veracity", "Captain Scotch"}) String player, @Enum TopMenuStyle style) {
      var cleanups =
          new Cleanups(
              withTopMenuStyle(style),
              withProperty("relayAddsQuickScripts", true),
              withProperty("scriptlist", "restore hp | restore mp"));
      try (cleanups) {
        String location = "awesomemenu.php";
        String input = html("request/" + playerToTextFile(player) + ".html");
        String output = RequestEditorKit.getFeatureRichHTML(location, input);

        // We detected that we have an awesomemenu and corrected it
        assertEquals(TopMenuStyle.FANCY, GenericRequest.topMenuStyle);

        // We inserted a "quick scripts" dropdown
        assertTrue(output.contains("scriptbar"));

        // We do not test for a relay script menu because that looks the
        // "relay" directory for scripts starting with "relay_".

        // Given that the response text is from awesomemenu.php, we are
        // only doing the RIGHT thing if TopMenuStype is FANCY, but
        // there is no way to check that here.
      }
    }

    @Test
    void loadingIconsDoesNotChangeStyle() {
      var cleanups =
          new Cleanups(
              withTopMenuStyle(TopMenuStyle.FANCY),
              withProperty("relayAddsQuickScripts", true),
              withProperty("scriptlist", "restore hp | restore mp"));
      try (cleanups) {
        String location = "awesomemenu.php?icons=1";
        String input = html("request/test_fancy_topmenu_icons.json");
        String output = RequestEditorKit.getFeatureRichHTML(location, input);

        // We did not change the topmenu style
        assertEquals(TopMenuStyle.FANCY, GenericRequest.topMenuStyle);

        // We inserted nothing
        assertEquals(output, input);
      }
    }
  }

  @Nested
  class MenuStyle {
    @CartesianTest
    void canDeriveMenuStyle(
        @Values(strings = {"normal", "compact", "fancy"}) String styleName,
        @Enum TopMenuStyle style) {
      String location = "";
      var actualStyle = TopMenuStyle.UNKNOWN;
      switch (styleName) {
        case "normal" -> {
          location = "topmenu.php";
          actualStyle = TopMenuStyle.NORMAL;
        }
        case "compact" -> {
          location = "topmenu.php";
          actualStyle = TopMenuStyle.COMPACT;
        }
        case "fancy" -> {
          location = "awesomemenu.php";
          actualStyle = TopMenuStyle.FANCY;
        }
        default -> {
          fail("Unknown menu style: " + styleName);
        }
      }
      // We have saved HTML for all three styles
      String path = "request/test_" + styleName + "_topmenu.html";
      var cleanups =
          new Cleanups(
              withTopMenuStyle(style),
              withProperty("relayAddsQuickScripts", true),
              withProperty("scriptlist", "restore hp | restore mp"));
      try (cleanups) {
        RequestLoggerOutput.startStream();
        String decorated = RequestEditorKit.getFeatureRichHTML(location, html(path));
        var log = RequestLoggerOutput.stopStream();

        // Verify that we have detected and corrected the topmenu style
        assertEquals(actualStyle, GenericRequest.topMenuStyle);

        // Verify that if we changed it, we logged the correction
        String expected =
            (actualStyle != style)
                ? "We think topmenu style is " + style + " but it is actually " + actualStyle
                : "";
        assertThat(log, startsWith(expected));

        // We insert a "quick scripts" dropdown for all three menu styles
        assertTrue(decorated.contains("scriptbar"));

        // We do not test for a relay script menu because that looks the
        // "relay" directory for scripts starting with "relay_".
      }
    }
  }
}
