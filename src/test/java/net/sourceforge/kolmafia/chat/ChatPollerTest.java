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

  private static final String TESTUSERNAME = "ChatPollerTest User";
  private static final int TESTUSERID = 1234567;

  @BeforeEach
  public void beforeEach() {
    ChatPoller.reset();
  }

  private static String poll(final String msg, final int format) {
    return """
        {"msgs":[{"msg":%s,"type":"public","mid":"1573109300",
        "who":{"name":"%s","id":"%d","color":"black"},
        "format":"%d","channel":"clan","time":"1786471300"}],
        "last":"1573109300","delay":3000}"""
        .formatted(JSON.toJSONString(msg), TESTUSERNAME, TESTUSERID, format);
  }

  private static final String NAME_THEN_BOLD =
      """
      <i><b><a target=mainpane href="showplayer.php?who=%d"><font color=black>%s</font></a></b>"""
          .formatted(TESTUSERID, TESTUSERNAME);

  private static final String BOLD_THEN_NAME =
      """
      <b><i><a target=mainpane href="showplayer.php?who=%d"><font color=black>%s</b></font></a>"""
          .formatted(TESTUSERID, TESTUSERNAME);

  // What submitnewchat.php returns for a line sent to clan
  private static String sent(final String line) {
    return """
        <font color=green>[clan]</font> <b><a target=mainpane href="showplayer.php?who=%d">\
        <font color=black>%s</font></b></a>: %s<br>"""
        .formatted(TESTUSERID, TESTUSERNAME, line);
  }

  private static List<ChatMessage> pollOwn(final String response) {
    List<ChatMessage> messages = new LinkedList<>();
    ChatPoller.parseNewChat(messages, JSON.parseObject(response), "", 0, false);
    return messages;
  }

  @Test
  public void itShouldReadActionWhoseNameEndsInBold() {
    List<ChatMessage> messages =
        ChatPoller.parseNewChat(poll(NAME_THEN_BOLD + " goes to bed</i>", 1));
    assertEquals(1, messages.size());
    assertEquals("goes to bed", messages.get(0).getContent());
  }

  @Test
  public void itShouldReadActionWhoseNameEndsInLink() {
    List<ChatMessage> messages =
        ChatPoller.parseNewChat(poll(BOLD_THEN_NAME + " goes to bed</i>", 1));
    assertEquals(1, messages.size());
    assertEquals("goes to bed", messages.get(0).getContent());
  }

  @Test
  public void itShouldKeepEscapedTextInAnAction() {
    List<ChatMessage> messages =
        ChatPoller.parseNewChat(poll(NAME_THEN_BOLD + " &lt;3 fish &amp; chips</i>", 1));
    assertEquals("&lt;3 fish &amp; chips", messages.get(0).getContent());
  }

  @Test
  public void itShouldSkipOwnActionAlreadyShownWhenSent() {
    try (var cleanups = withUserId(TESTUSERID)) {
      // submitnewchat.php's reply to "/me goes to bed" in clan
      ChatPoller.addSentEntry(
          "<font color=green>[clan]</font> " + NAME_THEN_BOLD + " goes to bed</i><br>", true);
      assertTrue(pollOwn(poll(NAME_THEN_BOLD + " goes to bed</i>", 1)).isEmpty());
    }
  }

  @Test
  public void itShouldSkipOwnLineAlreadyShownWhenSentWithAWordBreak() {
    try (var cleanups = withUserId(TESTUSERID)) {
      ChatPoller.addSentEntry(sent("see https://example.com/ LongWord"), true);
      assertTrue(pollOwn(poll("see https://example.com/LongWord", 0)).isEmpty());
    }
  }

  @Test
  public void itShouldShowOwnLineThatDiffersFromTheSentOneInASpace() {
    try (var cleanups = withUserId(TESTUSERID)) {
      ChatPoller.addSentEntry(sent("test 1"), true);
      List<ChatMessage> messages = pollOwn(poll("test1", 0));
      assertEquals(1, messages.size());
      assertEquals("test1", messages.get(0).getContent());
    }
  }
}
