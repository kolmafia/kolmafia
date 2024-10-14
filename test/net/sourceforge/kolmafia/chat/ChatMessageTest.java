package net.sourceforge.kolmafia.chat;

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

public class ChatMessageTest {

  @Test
  public void itShouldHaveExpectedValuesForSimpleConstructor() {
    ChatMessage testMessage = new ChatMessage();
    assertNull(testMessage.getSender());
    assertNull(testMessage.getRecipient());
    assertNull(testMessage.getContent());
    assertFalse(testMessage.isAction());
    assertNotNull(testMessage.getDate());
    assertNotNull(testMessage.getTimestamp());
    testMessage.setSender("Bozo");
    assertEquals(testMessage.getSender(), "Bozo");
    testMessage.setRecipient("Clown School");
    assertEquals(testMessage.getRecipient(), "Clown School");
    testMessage.setContent("No fluff");
    assertEquals(testMessage.getContent(), "No fluff");
    testMessage.setContent("   No fluff    ");
    assertEquals(testMessage.getContent(), "No fluff");
  }

  @Test
  public void itShouldHaveExpectedValuesForParameterizedConstructor() {
    ChatMessage testMessage = new ChatMessage("sender", "recipient", "content", true);
    assertNotNull(testMessage.getDate());
    assertNotNull(testMessage.getTimestamp());
    assertTrue(testMessage.isAction());
    assertEquals(testMessage.getSender(), "sender");
    assertEquals(testMessage.getRecipient(), "recipient");
    assertEquals(testMessage.getContent(), "content");
    JSONObject jso = testMessage.toJSON();
    assertNotNull(jso);
    JSONObject expected =
        JSON.parseObject(
            """
{
  "type": "private",
  "who": {
    "id": "sender",
    "name": "sender",
    "color": "black"
  },
  "for": {
    "id": "recipient",
    "name": "recipient",
    "color": "black"
  },
  "msg": "content"
}
""");
    // time has to be a long so compare will succeed.
    expected.put("time", testMessage.getDate().getTime() / 1000);
    assertEquals(jso, expected);
  }
}
