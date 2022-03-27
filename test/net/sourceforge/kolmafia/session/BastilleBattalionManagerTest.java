package net.sourceforge.kolmafia.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.EffectPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.BastilleBattalionManager.Stat;
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
    KoLConstants.activeEffects.clear();
    ChoiceManager.lastChoice = 0;
    ChoiceManager.lastDecision = 0;
  }

  static String loadHTMLResponse(String path) throws IOException {
    // Load the responseText from saved HTML file
    return Files.readString(Paths.get(path)).trim();
  }

  @Test
  public void canLoadStats() {
    String value = "";
    BastilleBattalionManager.loadStats(value);
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.MA));
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.MD));
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.CA));
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.CD));
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.PA));
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.PD));

    Preferences.setString("_bastilleStats", "MA=0,MD=3,CA=2,CD=4,PA=3,PD=8");
    BastilleBattalionManager.loadStats();
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.MA));
    assertEquals(3, BastilleBattalionManager.getCurrentStat(Stat.MD));
    assertEquals(2, BastilleBattalionManager.getCurrentStat(Stat.CA));
    assertEquals(4, BastilleBattalionManager.getCurrentStat(Stat.CD));
    assertEquals(3, BastilleBattalionManager.getCurrentStat(Stat.PA));
    assertEquals(8, BastilleBattalionManager.getCurrentStat(Stat.PD));
  }

  @Test
  public void canDetectBoosts() throws IOException {
    BastilleBattalionManager.logBoosts();
    assertEquals("", Preferences.getString("_bastilleBoosts"));
    AdventureResult.addResultToList(
        KoLConstants.activeEffects, EffectPool.get(EffectPool.SHARK_TOOTH_GRIN));
    BastilleBattalionManager.logBoosts();
    assertEquals("M", Preferences.getString("_bastilleBoosts"));
    AdventureResult.addResultToList(
        KoLConstants.activeEffects, EffectPool.get(EffectPool.BOILING_DETERMINATION));
    BastilleBattalionManager.logBoosts();
    assertEquals("MC", Preferences.getString("_bastilleBoosts"));
    AdventureResult.addResultToList(
        KoLConstants.activeEffects, EffectPool.get(EffectPool.ENHANCED_INTERROGATION));
    BastilleBattalionManager.logBoosts();
    assertEquals("MCP", Preferences.getString("_bastilleBoosts"));
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

  @Test
  public void canProcessGame() throws IOException {
    // This is 12-turn game, ending in a loss.

    // Enter the control console
    String urlString = "choice.php?forceoption=0";
    String responseText = loadHTMLResponse("request/test_bastille_game1_0.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    BastilleBattalionManager.visitChoice(request);

    // Start a game
    urlString = "choice.php?whichchoice=1313&option=5";
    String expected = "Starting game #1";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_0_1.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 5;
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA=0,MD=3,CA=2,CD=4,PA=3,PD=8", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(0, Preferences.getInteger("_bastilleCheese"));
    assertEquals(1, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("Shield Uriah", Preferences.getString("_bastilleEnemyName"));
    assertEquals("shieldmaster", Preferences.getString("_bastilleEnemyCastle"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #1: Choose to improve offense
    urlString = "choice.php?whichchoice=1314&option=1";
    expected = "Turn #1: Improving offense.";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_1.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1317;
    BastilleBattalionManager.visitChoice(request);
    assertEquals("Let the citizens hurl cheese at you", Preferences.getString("_bastilleChoice1"));
    assertEquals("Adopt the radical combat style", Preferences.getString("_bastilleChoice2"));
    assertEquals("Commission some art", Preferences.getString("_bastilleChoice3"));

    // Select an option
    urlString = "choice.php?whichchoice=1317&option=2";
    expected = "Adopt the radical combat style";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_1_2.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1317;
    ChoiceManager.lastDecision = 2;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA=2,MD=1,CA=4,CD=2,PA=5,PD=6", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(2, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #2: Choose to improve offense
    urlString = "choice.php?whichchoice=1314&option=1";
    expected = "Turn #2: Improving offense.";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    request.constructURLString(urlString);
    responseText = loadHTMLResponse("request/test_bastille_game1_2.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1317;
    BastilleBattalionManager.visitChoice(request);
    assertEquals("Pick up the boulders", Preferences.getString("_bastilleChoice1"));
    assertEquals("Draft those artists", Preferences.getString("_bastilleChoice2"));
    assertEquals("Improve the keep", Preferences.getString("_bastilleChoice3"));

    // Select an option
    urlString = "choice.php?whichchoice=1317&option=2";
    expected = "Draft those artists";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_2_3.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1317;
    ChoiceManager.lastDecision = 3;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA=4,MD=1,CA=4,CD=2,PA=5,PD=5", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(3, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #3: Select a stance
    urlString = "choice.php?whichchoice=1315&option=1";
    expected = "Turn #3: Charge!";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_3_4.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1315;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1315, responseText));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA>MD,CA<CD,PA>PD", Preferences.getString("_bastilleLastBattleResults"));
    assertTrue(Preferences.getBoolean("_bastilleLastBattleWon"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(38, Preferences.getInteger("_bastilleCheese"));
    assertEquals(4, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("Mace Lilly", Preferences.getString("_bastilleEnemyName"));
    assertEquals("berserker", Preferences.getString("_bastilleEnemyCastle"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #4: Choose to improve defense
    urlString = "choice.php?whichchoice=1314&option=2";
    expected = "Turn #4: Focusing on defense.";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_4.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 2;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(38, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1318;
    BastilleBattalionManager.visitChoice(request);
    assertEquals("Convert the galleries", Preferences.getString("_bastilleChoice1"));
    assertEquals("Cut military spending", Preferences.getString("_bastilleChoice2"));
    assertEquals("Do the plowshares thing", Preferences.getString("_bastilleChoice3"));

    // Select an option
    urlString = "choice.php?whichchoice=1318&option=1";
    expected = "Convert the galleries";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_4_5.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1318;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1318, responseText));
    assertEquals(38, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA=4,MD=2,CA=4,CD=2,PA=5,PD=5", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(5, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #5: Choose to improve defense
    urlString = "choice.php?whichchoice=1314&option=2";
    expected = "Turn #5: Focusing on defense.";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_5.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 2;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(38, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1318;
    BastilleBattalionManager.visitChoice(request);
    assertEquals("Repurpose the statues", Preferences.getString("_bastilleChoice1"));
    assertEquals("Add more murals", Preferences.getString("_bastilleChoice2"));
    assertEquals("Build the weird statue", Preferences.getString("_bastilleChoice3"));

    // Select an option
    urlString = "choice.php?whichchoice=1318&option=1";
    expected = "Repurpose the statues";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_5_6.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1318;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1318, responseText));
    assertEquals(38, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA=4,MD=3,CA=4,CD=3,PA=5,PD=4", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(6, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #6: Select a stance
    urlString = "choice.php?whichchoice=1315&option=1";
    expected = "Turn #6: Charge!";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_6_7.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1315;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1315, responseText));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA>MD,CA>CD,PA>PD", Preferences.getString("_bastilleLastBattleResults"));
    assertTrue(Preferences.getBoolean("_bastilleLastBattleWon"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(118, Preferences.getInteger("_bastilleCheese"));
    assertEquals(7, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("Sergeant Ludwig", Preferences.getString("_bastilleEnemyName"));
    assertEquals("barracks", Preferences.getString("_bastilleEnemyCastle"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #7: Choose to improve offense
    urlString = "choice.php?whichchoice=1314&option=1";
    expected = "Turn #7: Improving offense.";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_7.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(118, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1317;
    BastilleBattalionManager.visitChoice(request);
    assertEquals("Build the memorial", Preferences.getString("_bastilleChoice1"));
    assertEquals("Improve the keep", Preferences.getString("_bastilleChoice2"));
    assertEquals("Add more windows", Preferences.getString("_bastilleChoice3"));

    // Select an option
    urlString = "choice.php?whichchoice=1317&option=1";
    expected = "Build the memorial";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_7_8.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1317;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1317, responseText));
    assertEquals(118, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA=4,MD=3,CA=4,CD=3,PA=6,PD=4", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(8, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #8: Choose to improve offense
    urlString = "choice.php?whichchoice=1314&option=1";
    expected = "Turn #8: Improving offense.";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_8.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(118, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1317;
    BastilleBattalionManager.visitChoice(request);
    assertEquals("Let the citizens hurl cheese at you", Preferences.getString("_bastilleChoice1"));
    assertEquals("Approve the retrofit", Preferences.getString("_bastilleChoice2"));
    assertEquals("Build the memorial", Preferences.getString("_bastilleChoice3"));

    // Select an option
    urlString = "choice.php?whichchoice=1317&option=2";
    expected = "Approve the retrofit";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_8_9.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1317;
    ChoiceManager.lastDecision = 2;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1317, responseText));
    assertEquals(118, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA=4,MD=3,CA=4,CD=3,PA=7,PD=4", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(9, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #9: Select a stance
    urlString = "choice.php?whichchoice=1315&option=1";
    expected = "Turn #9: Charge!";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_9_10.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1315;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1315, responseText));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA<MD,CA>CD,PA>PD", Preferences.getString("_bastilleLastBattleResults"));
    assertTrue(Preferences.getBoolean("_bastilleLastBattleWon"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(257, Preferences.getInteger("_bastilleCheese"));
    assertEquals(10, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("Bradley the Samey", Preferences.getString("_bastilleEnemyName"));
    assertEquals("masterofnone", Preferences.getString("_bastilleEnemyCastle"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #10: Choose to improve offense
    urlString = "choice.php?whichchoice=1314&option=1";
    expected = "Turn #10: Improving offense.";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_10.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(257, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1317;
    BastilleBattalionManager.visitChoice(request);
    assertEquals("Pick up the boulders", Preferences.getString("_bastilleChoice1"));
    assertEquals("Conscript the soldiers", Preferences.getString("_bastilleChoice2"));
    assertEquals("Strengthen the walls", Preferences.getString("_bastilleChoice3"));

    // Select an option
    urlString = "choice.php?whichchoice=1317&option=3";
    expected = "Strengthen the walls";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_10_11.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1317;
    ChoiceManager.lastDecision = 3;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1317, responseText));
    assertEquals(257, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA=6,MD=3,CA=6,CD=3,PA=5,PD=4", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(11, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #11: Look for cheese
    urlString = "choice.php?whichchoice=1314&option=3";
    expected = "Turn #11: Looking for cheese.";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_11.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 3;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(257, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1319;
    BastilleBattalionManager.visitChoice(request);
    assertEquals("Raid the cave", Preferences.getString("_bastilleChoice1"));
    assertEquals("Have the cheese contest", Preferences.getString("_bastilleChoice2"));
    assertEquals("Let the cheese horse in", Preferences.getString("_bastilleChoice3"));

    // Select an option
    urlString = "choice.php?whichchoice=1319&option=1";
    expected = "Raid the cave";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_11_12.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1319;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1319, responseText));
    assertEquals(390, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA=6,MD=3,CA=6,CD=3,PA=5,PD=4", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(12, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #12: Select a stance
    urlString = "choice.php?whichchoice=1315&option=1";
    expected = "Turn #12: Charge!";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = loadHTMLResponse("request/test_bastille_game1_12_loss.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1315;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1315, responseText));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA>MD,CA<CD,PA<PD", Preferences.getString("_bastilleLastBattleResults"));
    assertFalse(Preferences.getBoolean("_bastilleLastBattleWon"));

    // The response is the "visit" to a new choice, but KoL will not "visit" it
    // until the user clicks a button and submits a request to choice 1316.

    // GAME OVER
    urlString = "choice.php?pwd&whichchoice=1316&option=3";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    responseText = loadHTMLResponse("request/test_bastille_game1_done.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1316;
    ChoiceManager.lastDecision = 3;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1316, responseText));
    BastilleBattalionManager.visitChoice(request);
    BastilleBattalionManager.postChoice1(urlString, request);

    // The game is complete and we are no longer in a game
    assertEquals(1, Preferences.getInteger("_bastilleGames"));
    assertEquals(0, Preferences.getInteger("_bastilleGameTurn"));

    // We lost, but details of the last battle remain
    assertEquals("Bradley the Samey", Preferences.getString("_bastilleEnemyName"));
    assertEquals("masterofnone", Preferences.getString("_bastilleEnemyCastle"));
    assertEquals("MA=6,MD=3,CA=6,CD=3,PA=5,PD=4", Preferences.getString("_bastilleStats"));
    assertEquals("MA>MD,CA<CD,PA<PD", Preferences.getString("_bastilleLastBattleResults"));
    assertEquals(390, Preferences.getInteger("_bastilleCheese"));
  }
}
