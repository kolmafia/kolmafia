package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.BastilleBattalionManager.Style;
import net.sourceforge.kolmafia.session.BastilleBattalionManager.Upgrade;
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
    Map<Upgrade, Style> styles = BastilleBattalionManager.getCurrentStyles();

    // Ensure we parsed all four styles
    assertEquals(4, styles.size());
    assertFalse(styles.get(Upgrade.BARBICAN) == null);
    assertFalse(styles.get(Upgrade.DRAWBRIDGE) == null);
    assertFalse(styles.get(Upgrade.MURDER_HOLES) == null);
    assertFalse(styles.get(Upgrade.MOAT) == null);

    // Ensure that we have set all the properties.
    assertFalse(Preferences.getString("_bastilleStats").equals(""));

    // Ensure that the stats all agree with what the styles indicate
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

  @Test
  public void canConfigureAllUpgrades() throws IOException {
    // Test all of the configuration options from the main page.
    // Cycle through every upgrade in every upgrade location.

    String urlString = "choice.php?forceoption=0";
    String responseText = loadHTMLResponse("request/test_bastille_configure_0.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(Style.BARBERSHOP, BastilleBattalionManager.getCurrentStyle(Upgrade.BARBICAN));
    assertEquals(Style.DRAFTSMAN, BastilleBattalionManager.getCurrentStyle(Upgrade.DRAWBRIDGE));
    assertEquals(Style.GESTURE, BastilleBattalionManager.getCurrentStyle(Upgrade.MURDER_HOLES));
    assertEquals(Style.TRUTH_SERUM, BastilleBattalionManager.getCurrentStyle(Upgrade.MOAT));
    assertEquals("MA=0,MD=3,CA=2,CD=4,PA=3,PD=8", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=1";
    String expected = "Decorating the Barbican";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_1.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 1;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.BARBECUE, BastilleBattalionManager.getCurrentStyle(Upgrade.BARBICAN));
    assertEquals("MA=3,MD=5,CA=2,CD=4,PA=1,PD=5", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=1";
    expected = "Decorating the Barbican";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_2.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 1;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.BABAR, BastilleBattalionManager.getCurrentStyle(Upgrade.BARBICAN));
    assertEquals("MA=0,MD=3,CA=4,CD=7,PA=1,PD=5", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=1";
    expected = "Decorating the Barbican";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_3.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 1;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.BARBERSHOP, BastilleBattalionManager.getCurrentStyle(Upgrade.BARBICAN));
    assertEquals("MA=0,MD=3,CA=2,CD=4,PA=3,PD=8", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=2";
    expected = "Changing the Drawbridge";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_4.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 2;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.ART_NOUVEAU, BastilleBattalionManager.getCurrentStyle(Upgrade.DRAWBRIDGE));
    assertEquals("MA=0,MD=0,CA=2,CD=1,PA=5,PD=7", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=2";
    expected = "Changing the Drawbridge";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_5.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 2;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.BRUTALIST, BastilleBattalionManager.getCurrentStyle(Upgrade.DRAWBRIDGE));
    assertEquals("MA=2,MD=0,CA=3,CD=1,PA=5,PD=5", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=2";
    expected = "Changing the Drawbridge";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_6.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 2;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.DRAFTSMAN, BastilleBattalionManager.getCurrentStyle(Upgrade.DRAWBRIDGE));
    assertEquals("MA=0,MD=3,CA=2,CD=4,PA=3,PD=8", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=3";
    expected = "Sizing the Murder Holes";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_7.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 3;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.CANNON, BastilleBattalionManager.getCurrentStyle(Upgrade.MURDER_HOLES));
    assertEquals("MA=2,MD=4,CA=2,CD=3,PA=2,PD=8", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=3";
    expected = "Sizing the Murder Holes";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_8.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 3;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.CATAPULT, BastilleBattalionManager.getCurrentStyle(Upgrade.MURDER_HOLES));
    assertEquals("MA=0,MD=4,CA=3,CD=4,PA=2,PD=7", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=3";
    expected = "Sizing the Murder Holes";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_9.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 3;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.GESTURE, BastilleBattalionManager.getCurrentStyle(Upgrade.MURDER_HOLES));
    assertEquals("MA=0,MD=3,CA=2,CD=4,PA=3,PD=8", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=4";
    expected = "Filling the Moat";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_10.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 4;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.SHARKS, BastilleBattalionManager.getCurrentStyle(Upgrade.MOAT));
    assertEquals("MA=0,MD=4,CA=0,CD=4,PA=5,PD=7", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=4";
    expected = "Filling the Moat";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_11.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 4;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.LAVA, BastilleBattalionManager.getCurrentStyle(Upgrade.MOAT));
    assertEquals("MA=2,MD=3,CA=0,CD=5,PA=3,PD=7", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());

    urlString = "choice.php?whichchoice=1313&option=4";
    expected = "Filling the Moat";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_configure_12.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 4;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(Style.TRUTH_SERUM, BastilleBattalionManager.getCurrentStyle(Upgrade.MOAT));
    assertEquals("MA=0,MD=3,CA=2,CD=4,PA=3,PD=8", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkPredictions());
  }
}
