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
    assertEquals("Bozo", testMessage.getSender());
    testMessage.setRecipient("Clown School");
    assertEquals("Clown School", testMessage.getRecipient());
    testMessage.setContent("No fluff");
    assertEquals("No fluff", testMessage.getContent());
    testMessage.setContent("   No fluff    ");
    assertEquals("No fluff", testMessage.getContent());
  }

  @Test
  public void itShouldHaveExpectedValuesForParameterizedConstructor() {
    ChatMessage testMessage = new ChatMessage("sender", "recipient", "content", true);
    assertNotNull(testMessage.getDate());
    assertNotNull(testMessage.getTimestamp());
    assertTrue(testMessage.isAction());
    assertEquals("sender", testMessage.getSender());
    assertEquals("recipient", testMessage.getRecipient());
    assertEquals("content", testMessage.getContent());
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
