package net.sourceforge.kolmafia;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

import internal.helpers.Cleanups;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.VioletFogManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class RequestEditorKitTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("RequestEditorKitTest");
    Preferences.reset("RequestEditorKitTest");
  }

  @Test
  public void willSuppressRedundantCharPaneRefreshes() {
    // Obsolete usage: put script into HTML comment.
    //
    // <script language=Javascript>
    // <!--
    // if (parent.frames.length == 0) location.href="game.php";
    // top.charpane.location.href="charpane.php";
    // //-->
    // </script>
    //
    // Current usage: no HTML comments:
    //
    // <script>top.charpane.location.href="charpane.php";</script>
    // <script>parent.charpane.location.href="charpane.php";</script>
    //
    // Either will force the browser to issue a request for charpane.php.
    // The issue is that KoL will sometimes include BOTH, forcing two requests.

    String refresh = "(?:top|parent).charpane.location.href=\"charpane.php\";";
    Pattern CHARPANE_REFRESH_PATTERN = Pattern.compile(refresh + "\\n?", Pattern.DOTALL);

    // No charpane refresh requested
    String location = "fight.php?ireallymeanit=1652976032";
    String html = html("request/test_feature_rich_html_charpane_refreshes_0.html");
    StringBuffer buffer = new StringBuffer(html);
    Matcher matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(0, matcher.results().count());
    RequestEditorKit.getFeatureRichHTML(location, buffer);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(0, matcher.results().count());

    // Only "obsolete" usage.
    location = "fight.php?action=steal";
    html = html("request/test_feature_rich_html_charpane_refreshes_1.html");
    buffer = new StringBuffer(html);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());
    RequestEditorKit.getFeatureRichHTML(location, buffer);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());

    // Only "current" usage.
    location = "fight.php?action=attack";
    html = html("request/test_feature_rich_html_charpane_refreshes_1a.html");
    buffer = new StringBuffer(html);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());
    RequestEditorKit.getFeatureRichHTML(location, buffer);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());

    // Both "obsolete" and "current" usage.
    location = "choice.php?pwd&whichchoice=28&option=2";
    html = html("request/test_feature_rich_html_charpane_refreshes_2.html");
    buffer = new StringBuffer(html);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(2, matcher.results().count());
    RequestEditorKit.getFeatureRichHTML(location, buffer);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());
  }

  @Nested
  class VioletFog {
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void addsDecorations(final boolean addComplexFeatures) {
      var cleanups =
          new Cleanups(
              withProperty("relayShowSpoilers", true),
              withProperty(
                  "violetFogLayout",
                  "0,0,0,0,0,0,0,0,57,0,53,0,0,0,0,0,0,0,0,0,0,66,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,67,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,50,0,0,0,0,0,0,0,0,0,0,0,0,0,0"),
              withProperty("lastVioletFogMap", KoLCharacter.getAscensions()),
              withProperty("violetFogGoal", 7));

      try (cleanups) {
        VioletFogManager.reset();
        var buffer =
            new StringBuffer(html("request/test_choice_violet_fog_66_that_way_to_51.html"));
        RequestEditorKit.getFeatureRichHTML(
            "choice.php?pwd&whichchoice=66&option=2", buffer, addComplexFeatures);
        var contents = buffer.toString();
        // Go to Goal is rendered
        assertThat(
            contents, containsString("<input class=button type=submit value=\"Go To Goal\">"));
        // Graph is rendered if we are adding complex features to the page
        var graphMatcher = containsString("id=\"violetFogGraph\"");
        assertThat(contents, addComplexFeatures ? graphMatcher : not(graphMatcher));
      }

      VioletFogManager.reset();
    }
  }
}
