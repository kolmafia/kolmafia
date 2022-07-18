package net.sourceforge.kolmafia;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class RequestEditorKitTest {

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
}
