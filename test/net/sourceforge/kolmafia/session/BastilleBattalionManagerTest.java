package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.BastilleBattalionManager.Setting;
import net.sourceforge.kolmafia.session.BastilleBattalionManager.Type;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BastilleBattalionManagerTest {

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    GenericRequest.passwordHash = "";
    KoLCharacter.reset("");
    KoLCharacter.reset("bastille battalion user");
    Preferences.saveSettingsToFile = false;
  }

  @AfterAll
  private static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  @BeforeEach
  private void beforeEach() {
    BastilleBattalionManager.reset();
    ChoiceManager.lastChoice = 0;
    ChoiceManager.lastDecision = 0;
  }

  static String loadHTMLResponse(String path) throws IOException {
    // Load the responseText from saved HTML file
    return Files.readString(Paths.get(path)).trim();
  }

  static void validateConfiguration() {
    Map<Type, Setting> settings = BastilleBattalionManager.getCurrentSettings();

    // Ensure we parsed all four settings
    assertEquals(4, settings.size());
    assertFalse(settings.get(Type.BARBICAN) == null);
    assertFalse(settings.get(Type.DRAWBRIDGE) == null);
    assertFalse(settings.get(Type.MURDER_HOLES) == null);
    assertFalse(settings.get(Type.MOAT) == null);

    // Ensure that we have set all the properties.
    // *** currently, these are the "x" value of the needles, rather than
    // *** being the actual adjustment.
    assertTrue(Preferences.getInteger("_bastilleMilitaryAttack") > 0);
    assertTrue(Preferences.getInteger("_bastilleMilitaryDefense") > 0);
    assertTrue(Preferences.getInteger("_bastilleCastleAttack") > 0);
    assertTrue(Preferences.getInteger("_bastilleCastleDefense") > 0);
    assertTrue(Preferences.getInteger("_bastillePsychologicalAttack") > 0);
    assertTrue(Preferences.getInteger("_bastillePsychologicalDefense") > 0);

    // Ensure that the stats all agree with what the settings indicate
    assertTrue(BastilleBattalionManager.checkPredictions());
  }

  @Test
  public void canLoadConfigurationFromVisit() throws IOException {
    String responseText = loadHTMLResponse("request/test_bastille_battalion_visit.html");
    GenericRequest request = new GenericRequest("choice.php?forceoption=0");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;

    // "Visit" the choice.
    BastilleBattalionManager.visitChoice(request);

    // Verify that we have a valid configuration.
    validateConfiguration();
  }
}
