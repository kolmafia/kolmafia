package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.sourceforge.kolmafia.KoLConstants;
import org.junit.jupiter.api.Test;

/**
 * This is a simple test for ChatRequest. It is still driven by coverage maps and not function.
 * Indeed the fact that it doesn't actually run the request created limits the utility.
 */
public class ChatRequestTest {

  ChatRequest creq;
  String fullURL;
  String expect;

  @Test
  public void itShouldBuildAModernChatRequestWithLastSeen() {
    KoLConstants.RNG.setSeed(42);

    creq = new ChatRequest(0L, true, false);
    fullURL = creq.getFullURLString();
    expect = "newchatmessages.php?aa=0.7275636800328681&j=1&lasttime=0";
    assertEquals(expect, fullURL);
    assertEquals("", creq.getGraf());
    assertTrue(creq.retryOnTimeout());
    creq = new ChatRequest(8675309L, true, false);
    fullURL = creq.getFullURLString();
    expect = "newchatmessages.php?aa=0.6832234717598454&j=1&lasttime=8675309";
    assertEquals(expect, fullURL);
    assertEquals("", creq.getGraf());
    assertTrue(creq.retryOnTimeout());
  }

  @Test
  public void itShouldBuildAnOlderChatRequestWithLastSeen() {
    creq = new ChatRequest(0L, false, false);
    fullURL = creq.getFullURLString();
    expect = "newchatmessages.php?lasttime=0&afk=0";
    assertEquals(expect, fullURL);
    assertEquals("", creq.getGraf());
    assertTrue(creq.retryOnTimeout());
    creq = new ChatRequest(8675309L, false, false);
    fullURL = creq.getFullURLString();
    expect = "newchatmessages.php?lasttime=8675309&afk=0";
    assertEquals(expect, fullURL);
    assertEquals("", creq.getGraf());
    assertTrue(creq.retryOnTimeout());
  }

  @Test
  public void itShouldBuildAnOlderChatRequestWithLastSeenAndAFK() {
    creq = new ChatRequest(0L, false, true);
    fullURL = creq.getFullURLString();
    expect = "newchatmessages.php?lasttime=0&afk=1";
    assertEquals(expect, fullURL);
    assertEquals("", creq.getGraf());
    assertTrue(creq.retryOnTimeout());
    creq = new ChatRequest(8675309L, false, true);
    fullURL = creq.getFullURLString();
    expect = "newchatmessages.php?lasttime=8675309&afk=1";
    assertEquals(expect, fullURL);
    assertEquals("", creq.getGraf());
    assertTrue(creq.retryOnTimeout());
  }

  @Test
  public void itShouldBuildAModernChatRequest() {
    creq = new ChatRequest("This is not a message.", true);
    fullURL = creq.getFullURLString();
    expect = "submitnewchat.php?j=1&pwd=&playerid=0&graf=This+is+not+a+message.";
    assertEquals(expect, fullURL);
    assertEquals("This is not a message.", creq.getGraf());
    assertTrue(creq.retryOnTimeout());
  }

  @Test
  public void itShouldBuildAnOlderChatRequest() {
    creq = new ChatRequest("This is not a message.", false);
    fullURL = creq.getFullURLString();
    expect = "submitnewchat.php?pwd=&playerid=0&graf=This+is+not+a+message.";
    assertEquals(expect, fullURL);
    assertEquals("This is not a message.", creq.getGraf());
    assertTrue(creq.retryOnTimeout());
  }

  @Test
  public void itShouldBuildABatchedChatRequest() {
    creq = new ChatRequest(List.of("/who games", "/who newbie", "/who normal"));
    fullURL = creq.getFullURLString();
    expect =
        "submitnewchat.php?playerid=0&j=1&graf[]=%2Fwho+games&graf[]=%2Fwho+newbie&graf[]=%2Fwho+normal";
    assertEquals(expect, fullURL);
    assertEquals("", creq.getGraf());
    assertTrue(creq.retryOnTimeout());
  }

  @Test
  public void itShouldKeepDuplicatesInABatchedChatRequest() {
    creq = new ChatRequest(List.of("/who games", "/who games"));
    fullURL = creq.getFullURLString();
    expect = "submitnewchat.php?playerid=0&j=1&graf[]=%2Fwho+games&graf[]=%2Fwho+games";
    assertEquals(expect, fullURL);
  }
}
