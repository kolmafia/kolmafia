package net.sourceforge.kolmafia.textui.javascript;

import static org.junit.jupiter.api.Assertions.*;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AshInteropTest {

  @BeforeEach
  void clearMessage() {
    KoLmafia.lastMessage = "";
  }

  @Test
  void getPlayerIdReturnsInt() {
    ContactManager.registerPlayerId("heeheehee", "354981");
    var js = new JavascriptRuntime("getPlayerId(\"heeheehee\")");
    // before clearing lastMessage before the test, it wasn't empty
    String x = KoLmafia.getLastMessage();
    assertEquals(x, "", "Last message not empty getting runtime.");
    assertNotNull(js, "JavascriptRuntime returned as null.");
    Value ret = js.execute(null, null, true);
    x = KoLmafia.getLastMessage();
    assertEquals(x, "", "Last message not empty executing.");
    assertNotNull(ret, "Javascript execute returns null instead of a result to be tested.");
    String retS = ret.toString();
    assertEquals("354981", retS);
  }

  @Test
  void getPlayerNameReturnsString() {
    ContactManager.registerPlayerId("heeheehee", "354981");
    var js = new JavascriptRuntime("getPlayerName(354981)");
    assertNotNull(js, "JavascriptRuntime returned as null.");
    Value ret = js.execute(null, null, true);
    assertNotNull(ret, "Javascript execute returns null instead of a result to be tested.");
    String retS = ret.toString();
    assertEquals("heeheehee", retS);
  }

  @Test
  void getPlayerIdInvertsGetPlayerName() {
    ContactManager.registerPlayerId("heeheehee", "354981");
    var js = new JavascriptRuntime("getPlayerId(getPlayerName(354981))");
    assertNotNull(js, "JavascriptRuntime returned as null.");
    Value ret = js.execute(null, null, true);
    assertNotNull(ret, "Javascript execute returns null instead of a result to be tested.");
    String retS = ret.toString();
    assertEquals("354981", retS);
  }

  @Test
  void getPlayerNameInvertsGetPlayerId() {
    ContactManager.registerPlayerId("heeheehee", "354981");
    var js = new JavascriptRuntime("getPlayerName(parseInt(getPlayerId(\"heeheehee\")))");
    assertNotNull(js, "JavascriptRuntime returned as null.");
    Value ret = js.execute(null, null, true);
    assertNotNull(ret, "Javascript execute returns null instead of a result to be tested.");
    String retS = ret.toString();
    assertEquals("heeheehee", retS);
  }
}
