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
  public void itShouldTimestampAPrivateMessageThatCarriesNoMid() {
    // KoL names the other party in "for", so a private message we received has its sender in both
    // "who" and "for".
    String response =
        "{\"msgs\":[{\"type\":\"private\",\"who\":{\"id\":\"3495347\","
            + "\"name\":\"TortureBot\",\"color\":\"black\"},\"for\":{\"id\":\"3495347\","
            + "\"name\":\"TortureBot\",\"color\":\"black\"},\"msg\":\"hh\","
            + "\"time\":1789528188,\"format\":0}]}";

    List<ChatMessage> messages = ChatPoller.parseNewChat(response);

    assertEquals(1, messages.size());
    ChatMessage message = messages.get(0);
    assertEquals("TortureBot", message.getSender());
    assertEquals("TortureBot", message.getRecipient());
    assertEquals(1789528188000L, message.getDate().getTime());
    assertNull(message.getMessageId());
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

  @Test
  public void itShouldStripTheSenderLinkFromAnActionMessage() {
    String response =
        "{\"msgs\":[{\"msg\":\"<i><b><a target=mainpane href=\\\"showplayer.php?who=3837556\\\">"
            + "<font color=\\\"black\\\">IrratTest</font></a></b> is testing something, ignore"
            + " this, it's normal</i>\",\"type\":\"public\",\"mid\":\"1574264783\","
            + "\"who\":{\"name\":\"IrratTest\",\"id\":\"3837556\",\"color\":\"black\"},"
            + "\"format\":\"1\",\"channel\":\"normal\",\"channelcolor\":\"green\","
            + "\"time\":\"1789532693\"}],\"last\":\"1574264783\",\"delay\":5000}";

    List<ChatMessage> messages = ChatPoller.parseNewChat(response);

    assertEquals(1, messages.size());
    ChatMessage message = messages.get(0);
    assertTrue(message.isAction());
    assertEquals("IrratTest", message.getSender());
    assertEquals("is testing something, ignore this, it's normal", message.getContent());
  }

  @Test
  public void itShouldParseAnAwayModeEvent() {
    String response =
        "{\"msgs\":[{\"type\":\"event\",\"msg\":\"You are now in away mode, chat will update"
            + " more slowly until you say something.\",\"notnew\":1,\"time\":1789532752}],"
            + "\"last\":\"1574264786\",\"delay\":20000}";

    List<ChatMessage> messages = ChatPoller.parseNewChat(response);

    assertEquals(1, messages.size());
    ChatMessage message = messages.get(0);
    assertInstanceOf(EventMessage.class, message);
    assertEquals(1789532752000L, message.getDate().getTime());
    assertNull(message.getMessageId());
  }

  @Test
  public void itShouldKeepTheServerMessageIdOfOurOwnMessage() {
    String response =
        "{\"msgs\":[{\"msg\":\"The response levels here are abnormal\",\"type\":\"public\","
            + "\"mid\":\"1574264802\",\"who\":{\"name\":\"IrratTest\",\"id\":\"3837556\","
            + "\"color\":\"black\"},\"format\":\"0\",\"channel\":\"normal\","
            + "\"channelcolor\":\"green\",\"time\":\"1789532855\"}],"
            + "\"last\":\"1574264802\",\"delay\":5000}";

    List<ChatMessage> messages = ChatPoller.parseNewChat(response);

    assertEquals(1, messages.size());
    ChatMessage message = messages.get(0);
    assertEquals("/normal", message.getRecipient());
    assertEquals(1574264802L, message.getMessageId());
    assertEquals(1789532855000L, message.getDate().getTime());
  }

  @Test
  public void itShouldParseAnEventCarryingAPrefixedMid() {
    String response =
        "{\"msgs\":[{\"type\":\"event\",\"mid\":\"ev2787280\",\"msg\":\"New message received"
            + " from <a target=mainpane href='showplayer.php?who=3342574'><font color=green>Peace"
            + " and Love</font></a>.\",\"link\":\"messages.php\",\"time\":\"1789532695\"}],"
            + "\"last\":\"1574264783\",\"delay\":5000}";

    List<ChatMessage> messages = ChatPoller.parseNewChat(response);

    assertEquals(1, messages.size());
    ChatMessage message = messages.get(0);
    assertInstanceOf(EventMessage.class, message);
    assertNull(message.getMessageId());
    assertEquals(1789532695000L, message.getDate().getTime());
  }
}
