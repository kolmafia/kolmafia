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
    Color black = Color.BLACK;
    Color white = Color.WHITE;

    // Here we're testing if the messages are added correctly
    ChatFormatter.addHighlighting("test case", black);
    // The first highlight, this is intended to be overwritten
    ChatFormatter.addHighlighting("a test", black);
    // The second highlight, this is also intended to be overwritten
    ChatFormatter.addHighlighting("test", black);
    ChatFormatter.addHighlighting("a test with a long message", black);
    // This should remove the second highlight and replace it
    ChatFormatter.addHighlighting("a test", black);
    // This should remove the first highlight and replace it with white
    ChatFormatter.addHighlighting("test", white);
    ChatFormatter.addHighlighting("the test", black);

    String expected =
        "test case\n#000000\na test with a long message\n#000000\na test\n#000000\ntest\n#ffffff\nthe test\n#000000";

    assertEquals(expected, Preferences.getString("highlightList"));
  }

  @Test
  public void removeHighlightTest() {
    Color color = Color.BLACK;

    // Here we're checking if the highlight is removed from the preferences correctly
    // The first message starts with the name of the highlight
    ChatFormatter.addHighlighting("test case", color);
    // This ends with the name of the highlight
    ChatFormatter.addHighlighting("a test", color);
    // This simply contains the highlight message
    ChatFormatter.addHighlighting("a test with a long message", color);
    // This is the highlight we intend to remove
    ChatFormatter.addHighlighting("test", color);
    // A message added on, to prove that it doesn't wipe out following highlights
    ChatFormatter.addHighlighting("another test", color);

    ChatFormatter.removeHighlighting("test");
    String expected =
        "test case\n#000000\na test\n#000000\na test with a long message\n#000000\nanother test\n#000000";

    assertEquals(expected, Preferences.getString("highlightList"));
  }
}
