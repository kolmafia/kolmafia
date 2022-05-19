package net.sourceforge.kolmafia;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

public class RequestEditorKitTest {

  static String loadHTML(String path) throws IOException {
    // Load the text from saved HTML file
    return Files.readString(Paths.get(path)).trim();
  }

  @Test
  public void willSuppressRedundantCharPaneRefreshes() throws IOException {
    String refresh = "top.charpane.location.href=\"charpane.php\";";
    Pattern CHARPANE_REFRESH_PATTERN = Pattern.compile(refresh + "\\n?", Pattern.DOTALL);

    String location = "fight.php?action=steal";
    String html = loadHTML("request/test_feature_rich_html_charpane_refreshes_1.html");
    StringBuffer buffer = new StringBuffer(html);
    Matcher matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());
    RequestEditorKit.getFeatureRichHTML(location, buffer);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());

    location = "choice.php?pwd&whichchoice=28&option=2";
    html = loadHTML("request/test_feature_rich_html_charpane_refreshes_2.html");
    buffer = new StringBuffer(html);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(2, matcher.results().count());
    RequestEditorKit.getFeatureRichHTML(location, buffer);
    matcher = CHARPANE_REFRESH_PATTERN.matcher(buffer);
    assertEquals(1, matcher.results().count());
  }
}
