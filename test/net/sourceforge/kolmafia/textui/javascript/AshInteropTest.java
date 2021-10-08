package net.sourceforge.kolmafia.textui.javascript;

import static org.junit.jupiter.api.Assertions.*;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class AshInteropTest {
  @BeforeAll
  static void clearStatus() {
    KoLmafia.lastMessage = "";
  }

  @Test
  void getPlayerIdReturnsInt() {
    // Note that we won't do any actual lookups, since ChatManager.isLiterate is false.
    ContactManager.registerPlayerId("heeheehee", "354981");

    var js = new JavascriptRuntime("getPlayerId(\"heeheehee\")");
    assertEquals("", KoLmafia.lastMessage);
    Value ret = js.execute(null, null, true);
    String retS = ret.toString();
    assertEquals("354981", retS);
  }

  @Test
  void getPlayerNameReturnsString() {
    ContactManager.registerPlayerId("heeheehee", "354981");

    var js = new JavascriptRuntime("getPlayerName(354981)");
    assertEquals("", KoLmafia.lastMessage);
    Value ret = js.execute(null, null, true);
    String retS = ret.toString();
    assertEquals("heeheehee", retS);
  }

  @Test
  void getPlayerIdInvertsGetPlayerName() {
    ContactManager.registerPlayerId("heeheehee", "354981");

    var js = new JavascriptRuntime("getPlayerName(parseInt(getPlayerId(\"heeheehee\")))");
    assertEquals("", KoLmafia.lastMessage);
    Value ret = js.execute(null, null, true);
    String retS = ret.toString();
    // currently fails rith retS 354981.0
    assertEquals("heeheehee", retS);
  }

  @Test
  void getPlayerIdNameCoercesArgumentToInt() {
    ContactManager.registerPlayerId("heeheehee", "354981");

    var js = new JavascriptRuntime("getPlayerName(354981.0)");
    assertEquals("", KoLmafia.lastMessage);
    assertEquals(new Value("heeheehee"), js.execute(null, null, true));
  }

  @Test
  void getPlayerNameInvertsGetPlayerId() {
    ContactManager.registerPlayerId("heeheehee", "354981");

    var js = new JavascriptRuntime("getPlayerId(parseInt(getPlayerName(354981)))");
    assertEquals("", KoLmafia.lastMessage);
    Value ret = js.execute(null, null, true);
    String retS = ret.toString();
    // currently fails with retS NaN
    assertEquals("heeheehee", retS);
  }
}
