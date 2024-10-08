package net.sourceforge.kolmafia.chat;

import static org.junit.jupiter.api.Assertions.*;

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
    String ep1 =
        "{\"msg\":\"content\",\"for\":{\"color\":\"black\",\"name\":\"recipient\",\"id\":\"recipient\"},\"time\":";
    String ep2 = Long.toString(testMessage.getDate().getTime() / 1000);
    String ep3 =
        ",\"type\":\"private\",\"who\":{\"color\":\"black\",\"name\":\"sender\",\"id\":\"sender\"}}";
    assertEquals(jso.toString(), ep1 + ep2 + ep3);
  }
}
