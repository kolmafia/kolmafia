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

    ChatFormatter.addHighlighting("john of arc", color);
    ChatFormatter.addHighlighting("prince john", color);
    ChatFormatter.addHighlighting("john", color);
    ChatFormatter.addHighlighting("prince john and the three stooges", color);
    ChatFormatter.addHighlighting("john", color);
    ChatFormatter.addHighlighting("king john", color);

    String expected =
        "john of arc\n#000000\nprince john\n#000000\nprince john and the three stooges\n#000000\njohn\n#000000\nking john\n#000000";

    assertEquals(expected, Preferences.getString("highlightList"));
  }

  @Test
  public void removeHighlightTest() {
    Color color = Color.BLACK;

    ChatFormatter.addHighlighting("john of arc", color);
    ChatFormatter.addHighlighting("prince john", color);
    ChatFormatter.addHighlighting("prince john and the three stooges", color);
    ChatFormatter.addHighlighting("john", color);
    ChatFormatter.addHighlighting("king john", color);

    ChatFormatter.removeHightlighting("john");
    String expected = "john of arc\n#000000\nprince john\n#000000\nprince john and the three stooges\n#000000\nking john\n#000000";

    assertEquals(expected, Preferences.getString("highlightList"));
  }
}
