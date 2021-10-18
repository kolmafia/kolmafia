package net.sourceforge.kolmafia.textui.javascript;

import static org.junit.jupiter.api.Assertions.*;

import net.bytebuddy.implementation.bind.annotation.IgnoreForBinding;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.session.ContactManager;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class AshInteropTest {

  @Test
  void getPlayerIdReturnsInt() {
    ContactManager.registerPlayerId("heeheehee", "354981");
    var js = new JavascriptRuntime("getPlayerId(\"heeheehee\")");
    assertNotNull(js, "JavascriptRuntime returned as null.");
    Value ret = js.execute(null, null, true);
    assertNotNull(ret, "Javascript execute returns null instead of a result to be tested.");
    String retS = ret.toString();
    assertEquals("354981", retS);
  }

  /*
  @BeforeEach
  void clearStatus() {
    KoLmafia.lastMessage = "";
  }

  @Test
  void getPlayerIdReturnsInt() {
    // Note that we won't do any actual lookups, since ChatManager.isLiterate is false.
    ContactManager.registerPlayerId("heeheehee", "354981");

    var js = new JavascriptRuntime("getPlayerId(\"heeheehee\")");
    assertEquals("", KoLmafia.lastMessage);
    Value ret = js.execute(null, null, true);
    assertNotNull(ret, "Javascript execute returns null instead of a result to be tested.");
    String retS = ret.toString();
    assertEquals("354981", retS);
  }

  @Test
  void getPlayerNameReturnsString() {
    ContactManager.registerPlayerId("heeheehee", "354981");

    var js = new JavascriptRuntime("getPlayerName(354981)");
    assertEquals("", KoLmafia.lastMessage);
    assertNotNull(js, "Javascript runtime is null, preventing test.");
    Value ret = js.execute(null, null, true);
    assertNotNull(ret, "Javascript execute returns null instead of a result to be tested.");
    String retS = ret.toString();
    assertEquals("heeheehee", retS);
  }

  @Test
  void getPlayerIdInvertsGetPlayerName() {
    ContactManager.registerPlayerId("heeheehee", "354981");

    var js = new JavascriptRuntime("getPlayerName(parseInt(getPlayerId(\"heeheehee\")))");
    assertEquals("", KoLmafia.lastMessage);

    Value ret = js.execute(null, null, true);
    assertNotNull(ret, "Javascript execute returns null instead of a result to be tested.");
    String retS = ret.toString();
    // currently fails with retS 354981.0
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

    var js = new JavascriptRuntime("getPlayerId(getPlayerName(354981).toString())");
    assertEquals("", KoLmafia.lastMessage);
    Value ret = js.execute(null, null, true);
    String retS = ret.toString();
    assertEquals("354981", retS);
  }
   */
}
