package net.sourceforge.kolmafia.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Color;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ChatFormatterTest {
  @BeforeEach
  public void resetHighlights() {
    Preferences.setString("highlightList", "");
    StyledChatBuffer.initializeHighlights();
  }

  @Test
  public void addHightlightTest() {
    Color color = Color.BLACK;

    ChatFormatter.addHighlighting("test case", color);
    ChatFormatter.addHighlighting("a test", color);
    ChatFormatter.addHighlighting("test", color);
    ChatFormatter.addHighlighting("a test with a long message", color);
    ChatFormatter.addHighlighting("test", color);
    ChatFormatter.addHighlighting("the test", color);

    String expected =
        "test case\n#000000\na test\n#000000\na test with a long message\n#000000\ntest\n#000000\nthe test\n#000000";

    assertEquals(expected, Preferences.getString("highlightList"));
  }

  @Test
  public void removeHighlightTest() {
    Color color = Color.BLACK;

    ChatFormatter.addHighlighting("test case", color);
    ChatFormatter.addHighlighting("a test", color);
    ChatFormatter.addHighlighting("a test with a long message", color);
    ChatFormatter.addHighlighting("test", color);
    ChatFormatter.addHighlighting("another test", color);

    ChatFormatter.removeHightlighting("test");
    String expected = "test case\n#000000\na test\n#000000\na test with a long message\n#000000\nanother test\n#000000";

    assertEquals(expected, Preferences.getString("highlightList"));
  }
}
