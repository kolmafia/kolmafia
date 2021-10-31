package net.sourceforge.kolmafia.textui.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SnapperCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    KoLCharacter.reset("testUser");
    Preferences.resetToDefault("redSnapperPhylum");

    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public SnapperCommandTest() {
    this.command = "snapper";
  }

  static String getPhylum() {
    return Preferences.getString("redSnapperPhylum");
  }

  static void setSnapper() {
    var familiar = FamiliarData.registerFamiliar(FamiliarPool.RED_SNAPPER, 1);
    KoLCharacter.setFamiliar(familiar);
  }

  @Test
  void mustHaveSnapper() {
    KoLCharacter.setFamiliar(FamiliarData.NO_FAMILIAR);
    String output = execute("bug");

    assertTrue(output.contains("You need to take your Red-Nosed Snapper with you"));
    assertContinueState();
    assertEquals("", getPhylum());
  }

  @Test
  void mustSpecifyPhylum() {
    setSnapper();
    execute("");
    assertErrorState();
    assertEquals("", getPhylum());
  }

  @Test
  void mustSpecifyValidPhylum() {
    setSnapper();
    execute("dog");
    assertErrorState();
    assertEquals("", getPhylum());
  }

  @Test
  void alreadyTrackingPhylum() {
    Preferences.setString("redSnapperPhylum", "beast");
    setSnapper();
    String output = execute("beast");
    assertContinueState();
    assertTrue(output.contains("already hot on the tail"));
  }

  @Test
  void changesPhylum() {
    setSnapper();
    String output = execute("beast");
    assertContinueState();
    assertTrue(output.contains("guiding you towards beasts"));
  }
}
