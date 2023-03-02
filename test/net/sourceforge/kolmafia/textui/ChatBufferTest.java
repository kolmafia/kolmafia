package net.sourceforge.kolmafia.textui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import net.sourceforge.kolmafia.chat.ChatFormatter;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.InternalMessage;
import net.sourceforge.kolmafia.chat.StyledChatBuffer;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.junit.jupiter.api.Test;

public class ChatBufferTest {
  @Test
  public void testChatBufferWritesInUTF8() {
    // This is processed as if we have a running chat instance, and called an ash function.
    // The function being: chat_notify("ăѣ𝔠ծềſģ", "");
    InternalMessage internalMessage = new InternalMessage("ăѣ\uD835\uDD20ծềſģ", "");

    // We now convert this into display HTML, as done in ChatManager after the InternalMessage is
    // passed around.
    String displayHTML = ChatFormatter.formatChatMessage(internalMessage);

    // We assert that the display html still contains our unicode after being thrown into a string
    // conversion function
    assertTrue(displayHTML.contains("ăѣ\uD835\uDD20ծềſģ"));

    StringBuilder builder = new StringBuilder();

    try {
      // This creates a StyledChatBuffer in ChatManager, which will generate a log file. We will
      // need to clean this up later.
      StyledChatBuffer buffer = ChatManager.getBuffer("[events]");

      // We send the html to the ChatBuffer, this will write it to file.
      buffer.append(displayHTML);

      // We have exposed the file the buffer is writing to, now we read it for this test
      try (BufferedReader reader = FileUtilities.getReader(buffer.getLogFile())) {
        String line;

        // Read the full file to a string
        while ((line = FileUtilities.readLine(reader)) != null) {
          if (builder.length() > 0) {
            builder.append("\n");
          }

          builder.append(line);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } finally {
      // We can't leave the ChatBuffer lying around
      ChatManager.reset();
    }

    // As time changes, we can't reuse the same displayHTML. So we must reuse the html we
    // generated earlier. We've already tested it still contains our unicode.
    // Given that the buffer should be writing almost exactly what we give
    // it, it should remain unchanged.
    assertTrue(builder.toString().contains(displayHTML));
  }
}
