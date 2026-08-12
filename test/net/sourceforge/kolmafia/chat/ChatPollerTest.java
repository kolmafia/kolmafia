package net.sourceforge.kolmafia.chat;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

public class ChatPollerTest {

  // Real newchatmessages.php responses captured August 2026. As of then KoL gives events and
  // private messages prefixed string "mid"s ("ev1786406503_0", "pm1786471290_1197090_0") which used
  // to crash parsing (NumberFormatException). Public messages keep a plain numeric mid.

  @Test
  public void itShouldParsePrivateMessageWithPrefixedMid() {
    String response =
        "{\"msgs\":[{\"type\":\"private\",\"mid\":\"pm1786471290_1197090_0\","
            + "\"msg\":\"fart fart fart fart\",\"who\":{\"name\":\"Gausie\",\"id\":\"1197090\"},"
            + "\"time\":\"1786471290\"}],\"last\":\"1573109330\",\"delay\":5000}";
    List<ChatMessage> messages = assertDoesNotThrow(() -> ChatPoller.parseNewChat(response));
    assertEquals(1, messages.size());
    assertEquals("Gausie", messages.get(0).getSender());
    assertEquals("fart fart fart fart", messages.get(0).getContent());
  }

  @Test
  public void itShouldParsePublicMessageWithNumericMid() {
    String response =
        "{\"msgs\":[{\"msg\":\"hello games\",\"type\":\"public\",\"mid\":\"1573109278\","
            + "\"who\":{\"name\":\"capn castom\",\"id\":\"2488885\",\"color\":\"#666666\"},"
            + "\"format\":\"0\",\"channel\":\"games\",\"channelcolor\":\"#FF33CC\","
            + "\"time\":\"1786471197\"}],\"last\":\"1573109278\",\"delay\":5000}";
    List<ChatMessage> messages = assertDoesNotThrow(() -> ChatPoller.parseNewChat(response));
    assertEquals(1, messages.size());
    assertEquals("hello games", messages.get(0).getContent());
  }

  @Test
  public void parseMidReadsNumericPublicMidForDeduplication() {
    // Public mids are counters in the same space as "last" and must still be comparable.
    assertEquals(1573109278L, ChatPoller.parseMid("1573109278"));
    assertEquals(1573L, ChatPoller.parseMid(1573L));
  }

  @Test
  public void parseMidTreatsPrefixedAndMissingMidsAsZero() {
    // Prefixed event/pm ids embed a Unix timestamp, not a counter, so they must not be compared
    // against "last"; 0 means "don't deduplicate by mid".
    assertEquals(0L, ChatPoller.parseMid("ev1786406503_0"));
    assertEquals(0L, ChatPoller.parseMid("pm1786471290_1197090_0"));
    assertEquals(0L, ChatPoller.parseMid(null));
    assertEquals(0L, ChatPoller.parseMid(""));
  }
}
