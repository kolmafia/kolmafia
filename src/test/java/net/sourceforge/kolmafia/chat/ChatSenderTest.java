package net.sourceforge.kolmafia.chat;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.alibaba.fastjson2.JSON;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ChatSenderTest {
  @Test
  public void itShouldNotParseOutputItCannotAttributeToOneCommand() {
    String output =
        JSON.parseObject(html("request/test_chat_batched_graf.json")).getString("output");

    // KoL merges the replies to all three batched /who commands into one output field, so there
    // is no way to tell ChatParser which of them it is looking at.
    assertEquals(3, output.split("Players in channel", -1).length - 1);

    List<ChatMessage> messages = new LinkedList<>();
    ChatSender.processResponse(messages, output, null);

    assertEquals(0, messages.size());
  }
}
