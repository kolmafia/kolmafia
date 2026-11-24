package net.sourceforge.kolmafia.chat;

import static internal.helpers.Player.withUserId;
import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.fastjson2.JSON;
import java.util.LinkedList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
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

  @BeforeEach
  public void beforeEach() {
    ChatPoller.reset();
  }

  private static String emote(String msg) {
    return "{\"msgs\":[{\"msg\":\""
        + msg.replace("\"", "\\\"")
        + "\",\"type\":\"public\",\"mid\":\"1573109300\","
        + "\"who\":{\"name\":\"Testy McTest\",\"id\":\"1234567\",\"color\":\"black\"},"
        + "\"format\":\"1\",\"channel\":\"clan\",\"time\":\"1786471300\"}],"
        + "\"last\":\"1573109300\",\"delay\":3000}";
  }

  @Test
  public void itShouldReadActionWhoseNameEndsInBold() {
    String response =
        emote(
            "<i><b><a target=mainpane href=\"showplayer.php?who=1234567\"><font color=black>Testy McTest</font></a></b> goes to bed</i>");
    List<ChatMessage> messages = ChatPoller.parseNewChat(response);
    assertEquals(1, messages.size());
    assertEquals("goes to bed", messages.get(0).getContent());
  }

  @Test
  public void itShouldReadActionWhoseNameEndsInLink() {
    String response =
        emote(
            "<b><i><a target=mainpane href=\"showplayer.php?who=1234567\"><font color=black>Testy McTest</b></font></a> goes to bed</i>");
    List<ChatMessage> messages = ChatPoller.parseNewChat(response);
    assertEquals(1, messages.size());
    assertEquals("goes to bed", messages.get(0).getContent());
  }

  @Test
  public void itShouldSkipOwnActionAlreadyShownWhenSent() {
    var cleanups = withUserId(1234567);
    try (cleanups) {
      // submitnewchat.php's reply to "/me goes to bed" in clan
      ChatPoller.addSentEntry(
          "<font color=green>[clan]</font> <i><b><a target=mainpane href=\"showplayer.php?who=1234567\"><font color=black>Testy McTest</font></a></b> goes to bed</i><br>",
          true);
      String response =
          emote(
              "<i><b><a target=mainpane href=\"showplayer.php?who=1234567\"><font color=black>Testy McTest</font></a></b> goes to bed</i>");
      List<ChatMessage> messages = new LinkedList<>();
      ChatPoller.parseNewChat(messages, JSON.parseObject(response), "", 0, false);
      assertTrue(messages.isEmpty());
    }
  }

  @Test
  public void itShouldSkipOwnLineAlreadyShownWhenSentWithDifferentWordBreaks() {
    var cleanups = withUserId(1234567);
    try (cleanups) {
      ChatPoller.addSentEntry(
          "<font color=green>[clan]</font> <b><a target=mainpane href=\"showplayer.php?who=1234567\"><font color=black>Testy McTest</font></b></a>: see https://example.com/a/ LongWord<br>",
          true);
      String response =
          "{\"msgs\":[{\"msg\":\"see https://example.com/a/LongWord\",\"type\":\"public\","
              + "\"mid\":\"1573109301\",\"who\":{\"name\":\"Testy McTest\",\"id\":\"1234567\","
              + "\"color\":\"black\"},\"format\":\"0\",\"channel\":\"clan\","
              + "\"time\":\"1786471301\"}],\"last\":\"1573109301\",\"delay\":3000}";
      List<ChatMessage> messages = new LinkedList<>();
      ChatPoller.parseNewChat(messages, JSON.parseObject(response), "", 0, false);
      assertTrue(messages.isEmpty());
    }
  }
}
