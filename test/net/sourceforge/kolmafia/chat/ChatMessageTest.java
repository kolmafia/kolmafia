package net.sourceforge.kolmafia.chat;

import org.junit.Test;

import static org.junit.Assert.*;

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
        assertEquals(testMessage.getContent(),"content");
    }
}

