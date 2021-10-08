package net.sourceforge.kolmafia.textui.javascript;

import static org.junit.jupiter.api.Assertions.*;

import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.session.ContactManager;
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
    assertEquals("354981", js.execute(null, null, true).toString());
  }

  @Test
  void getPlayerNameReturnsString() {
    ContactManager.registerPlayerId("heeheehee", "354981");

    var js = new JavascriptRuntime("getPlayerName(354981)");
    assertEquals("", KoLmafia.lastMessage);
    assertEquals("heeheehee", js.execute(null, null, true).toString());

  }

  @Test
  void getPlayerIdInvertsGetPlayerName() {
    ContactManager.registerPlayerId("heeheehee", "354981");

    var js = new JavascriptRuntime("getPlayerName(parseInt(getPlayerId(\"heeheehee\")))");
    assertEquals("", KoLmafia.lastMessage);
    assertEquals("heeheehee", js.execute(null, null, true).toString());
  }
}