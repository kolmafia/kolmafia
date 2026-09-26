package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withProperty;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class SnapperCommandTest extends AbstractCommandTestBase {
  @BeforeAll
  public static void init() {
    KoLCharacter.reset("testUser");
    Preferences.reset("testUser");
  }

  public SnapperCommandTest() {
    this.command = "snapper";
  }

  @Test
  void mustHaveSnapper() {
    String output = execute("bug");

    assertTrue(output.contains("You need to take your Red-Nosed Snapper with you"));
    assertContinueState();
  }

  @Test
  void mustSpecifyPhylum() {
    var cleanups = withFamiliar(FamiliarPool.RED_SNAPPER);
    try (cleanups) {
      String output = execute("");
      assertErrorState();
      assertTrue(output.contains("Which monster phylum do you want?"));
    }
  }

  @Test
  void mustSpecifyValidPhylum() {
    var cleanups = withFamiliar(FamiliarPool.RED_SNAPPER);
    try (cleanups) {
      String output = execute("dog");
      assertErrorState();
      assertTrue(output.contains("What kind of random monster is a dog?"));
    }
  }

  @Test
  void alreadyTrackingPhylum() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.RED_SNAPPER), withProperty("redSnapperPhylum", "beast"));
    try (cleanups) {
      String output = execute("beast");
      assertContinueState();
      assertTrue(output.contains("already hot on the tail"));
    }
  }

  @Test
  void changesPhylum() {
    var cleanups = withFamiliar(FamiliarPool.RED_SNAPPER);
    try (cleanups) {
      String output = execute("beast");
      assertContinueState();
      assertTrue(output.contains("guiding you towards beasts"));
    }
  }
}
