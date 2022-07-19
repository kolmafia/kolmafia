package net.sourceforge.kolmafia.textui;

import net.java.dev.spellcast.utilities.ChatBuffer;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.utilities.FileUtilities;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ChatBufferTest {
    @Test
    public void testChatBufferWritesInUTF8() {
        File file = new File(KoLConstants.CHATLOG_LOCATION, "chat_utf8_test.html");

        ChatBuffer chatBuffer = new ChatBuffer("Test");
        chatBuffer.setLogFile(file);
        chatBuffer.append("ăѣ\uD835\uDD20ծềſģ");

        try (BufferedReader reader = FileUtilities.getReader(file)) {
            StringBuilder builder = new StringBuilder();
            String line;

            while ((line = FileUtilities.readLine(reader)) != null) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }

                builder.append(line);
            }

            assertEquals("<html><head>\n<title>\nTest\n</title>\n<style>\nbody { font-family: sans-serif; }\n</style>\n<body>\năѣ\uD835\uDD20ծềſģ",
                builder.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
