package net.sourceforge.kolmafia.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Set;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WitchessRequestTest extends RequestTestBase {
  @AfterAll
  public static void afterAll() {
    KoLCharacter.setRestricted(false);
  }

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("testUser");
    KoLCharacter.reset(true);
    Set.of("puzzleChampBonus", "_witchessBuff").forEach(Preferences::resetToDefault);
    KoLCharacter.setRestricted(false);
  }

  private String captureOutput(Runnable r) {
    var outputStream = new ByteArrayOutputStream();
    RequestLogger.openCustom(new PrintStream(outputStream));
    r.run();
    RequestLogger.closeCustom();
    return outputStream.toString().trim();
  }

  @Test
  public void cannotRunIfWitchessRestricted() {
    // Restricted mode will automatically disallow the Witchess set
    KoLCharacter.setRestricted(true);
    StandardRequest.setRestricted("Items", "Witchess Set");
    var req = new WitchessRequest();
    String result = captureOutput(() -> req.run());
    assertThat(result, not(containsString("[1] Your Witchess Set")));
  }

  @Test
  public void cannotRunIfAlreadyUsedBuff() {
    Preferences.setBoolean("_witchessBuff", true);
    var req = new WitchessRequest();
    String result = captureOutput(() -> req.run());
    assertThat(result, not(containsString("[1] Your Witchess Set")));
  }

  @Test
  public void cannotRunIfNotChamp() {
    Preferences.setInteger("puzzleChampBonus", 5);
    var req = new WitchessRequest();
    String result = captureOutput(() -> req.run());
    assertThat(result, not(containsString("[1] Your Witchess Set")));
  }

  @Test
  public void runsIfAllRequirementsAreMet() {
    Preferences.setBoolean("_witchessBuff", false);
    Preferences.setInteger("puzzleChampBonus", 20);
    var req = new WitchessRequest();
    String result = captureOutput(() -> req.run());
    assertThat(result, containsString("[1] Your Witchess Set"));
  }
}
