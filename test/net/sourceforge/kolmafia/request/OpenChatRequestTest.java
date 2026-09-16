package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

public class OpenChatRequestTest {
  @Test
  public void itShouldParseTheChannelsWeAreListeningTo() {
    OpenChatRequest request = new OpenChatRequest();
    request.responseText = html("request/test_mchat.html");

    assertEquals(
        List.of(
            "/clan",
            "/challenge",
            "/dread",
            "/foodcourt",
            "/games",
            "/haiku",
            "/hardcore",
            "/newbie",
            "/normal",
            "/pvp",
            "/radio",
            "/talkie",
            "/trade",
            "/veteran"),
        request.getChannels());
  }

  @Test
  public void itShouldParseTheActiveChannelWithAChannelPrefix() {
    OpenChatRequest request = new OpenChatRequest();
    request.responseText = html("request/test_mchat.html");

    assertEquals("/clan", request.getActiveChannel());
  }
}
