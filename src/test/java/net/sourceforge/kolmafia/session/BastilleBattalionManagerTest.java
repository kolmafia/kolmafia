package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withChoice;
import static internal.helpers.Player.withPostChoice1;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.SessionLoggerOutput;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.BastilleBattalionManager.Stat;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class BastilleBattalionManagerTest {

  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    GenericRequest.passwordHash = "";
    KoLCharacter.reset("");
    KoLCharacter.reset("bastille battalion user");
  }

  @BeforeEach
  public void beforeEach() {
    BastilleBattalionManager.reset();
    KoLConstants.activeEffects.clear();
    ChoiceManager.lastChoice = 0;
    ChoiceManager.lastDecision = 0;
  }

  @Test
  public void canLoadStats() {
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.MA));
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.MD));
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.CA));
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.CD));
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.PA));
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.PD));

    Preferences.setString("_bastilleStats", "MA=0,MD=3,CA=2,CD=4,PA=3,PD=8");
    assertEquals(0, BastilleBattalionManager.getCurrentStat(Stat.MA));
    assertEquals(3, BastilleBattalionManager.getCurrentStat(Stat.MD));
    assertEquals(2, BastilleBattalionManager.getCurrentStat(Stat.CA));
    assertEquals(4, BastilleBattalionManager.getCurrentStat(Stat.CD));
    assertEquals(3, BastilleBattalionManager.getCurrentStat(Stat.PA));
    assertEquals(8, BastilleBattalionManager.getCurrentStat(Stat.PD));
  }

  @Test
  public void canConfigureAllUpgrades() {
    // Test all of the configuration options from the main page.
    // Cycle through every upgrade in every upgrade location.

    String urlString = "choice.php?forceoption=0";
    String responseText = html("request/test_bastille_configure_0.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(
        "BARBERSHOP,DRAFTSMAN,GESTURE,TRUTH", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=100,MD=110,CA=110,CD=120,PA=130,PD=150", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=1";
    String expected = "Decorating the Barbican";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_1.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 1;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "BARBECUE,DRAFTSMAN,GESTURE,TRUTH", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=120,MD=130,CA=110,CD=120,PA=110,PD=130", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=1";
    expected = "Decorating the Barbican";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_2.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 1;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("BABAR,DRAFTSMAN,GESTURE,TRUTH", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=100,MD=110,CA=130,CD=140,PA=110,PD=130", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=1";
    expected = "Decorating the Barbican";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_3.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 1;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "BARBERSHOP,DRAFTSMAN,GESTURE,TRUTH", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=100,MD=110,CA=110,CD=120,PA=130,PD=150", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=2";
    expected = "Changing the Drawbridge";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_4.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 2;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "BARBERSHOP,NOUVEAU,GESTURE,TRUTH", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=100,MD=90,CA=110,CD=100,PA=145,PD=145", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=2";
    expected = "Changing the Drawbridge";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_5.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 2;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "BARBERSHOP,BRUTALIST,GESTURE,TRUTH", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=110,MD=90,CA=120,CD=100,PA=140,PD=130", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=2";
    expected = "Changing the Drawbridge";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_6.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 2;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "BARBERSHOP,DRAFTSMAN,GESTURE,TRUTH", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=100,MD=110,CA=110,CD=120,PA=130,PD=150", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=3";
    expected = "Sizing the Murder Holes";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_7.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 3;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "BARBERSHOP,DRAFTSMAN,CANNON,TRUTH", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=110,MD=120,CA=110,CD=110,PA=120,PD=150", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=3";
    expected = "Sizing the Murder Holes";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_8.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 3;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "BARBERSHOP,DRAFTSMAN,CATAPULT,TRUTH", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=100,MD=120,CA=120,CD=120,PA=120,PD=140", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=3";
    expected = "Sizing the Murder Holes";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_9.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 3;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "BARBERSHOP,DRAFTSMAN,GESTURE,TRUTH", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=100,MD=110,CA=110,CD=120,PA=130,PD=150", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=4";
    expected = "Filling the Moat";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_10.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 4;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "BARBERSHOP,DRAFTSMAN,GESTURE,SHARKS", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=100,MD=120,CA=100,CD=120,PA=140,PD=140", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=4";
    expected = "Filling the Moat";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_11.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 4;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "BARBERSHOP,DRAFTSMAN,GESTURE,LAVA", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=110,MD=110,CA=100,CD=130,PA=130,PD=140", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));

    urlString = "choice.php?whichchoice=1313&option=4";
    expected = "Filling the Moat";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_configure_12.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 4;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "BARBERSHOP,DRAFTSMAN,GESTURE,TRUTH", Preferences.getString("_bastilleCurrentStyles"));
    assertEquals(
        "MA=100,MD=110,CA=110,CD=120,PA=130,PD=150", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));
  }

  @Test
  public void canProcessGame() {
    // This is 12-turn game, ending in a loss.

    // Enter the control console
    String urlString = "choice.php?forceoption=0";
    String responseText = html("request/test_bastille_game1_0.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    BastilleBattalionManager.visitChoice(request);

    // Start a game
    urlString = "choice.php?whichchoice=1313&option=5";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    responseText = html("request/test_bastille_game1_0_1.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 5;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "MA=100,MD=110,CA=110,CD=120,PA=130,PD=150", Preferences.getString("_bastilleStats"));

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
    String expected = "Turn #1: Improving offense.";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_game1_1.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(0, Preferences.getInteger("_bastilleCheese"));
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
    responseText = html("request/test_bastille_game1_1_2.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1317;
    ChoiceManager.lastDecision = 2;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(0, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "MA=115,MD=95,CA=125,CD=105,PA=145,PD=135", Preferences.getString("_bastilleStats"));

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
    responseText = html("request/test_bastille_game1_2.html");
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(0, Preferences.getInteger("_bastilleCheese"));
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
    responseText = html("request/test_bastille_game1_2_3.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1317;
    ChoiceManager.lastDecision = 2;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(0, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "MA=125,MD=95,CA=125,CD=105,PA=145,PD=130", Preferences.getString("_bastilleStats"));

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
    responseText = html("request/test_bastille_game1_3_4.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1315;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1315, responseText));
    assertEquals(38, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(38, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA>MD,CA<CD,PA>PD", Preferences.getString("_bastilleLastBattleResults"));
    assertTrue(Preferences.getBoolean("_bastilleLastBattleWon"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
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
    responseText = html("request/test_bastille_game1_4.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 2;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(38, Preferences.getInteger("_bastilleCheese"));
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
    responseText = html("request/test_bastille_game1_4_5.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1318;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1318, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(38, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "MA=125,MD=105,CA=125,CD=105,PA=140,PD=130", Preferences.getString("_bastilleStats"));

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
    responseText = html("request/test_bastille_game1_5.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 2;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(38, Preferences.getInteger("_bastilleCheese"));
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
    responseText = html("request/test_bastille_game1_5_6.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1318;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1318, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(38, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "MA=125,MD=115,CA=125,CD=115,PA=140,PD=120", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    Preferences.setString("_bastilleEnemyCastle", "");
    ChoiceManager.lastChoice = 1315;
    BastilleBattalionManager.visitChoice(request);
    assertThat("_bastilleEnemyCastle", isSetTo("berserker"));
    assertEquals(6, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Turn #6: Select a stance
    urlString = "choice.php?whichchoice=1315&option=1";
    expected = "Turn #6: Charge!";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_game1_6_7.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1315;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1315, responseText));
    assertEquals(80, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(118, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA>MD,CA>CD,PA>PD", Preferences.getString("_bastilleLastBattleResults"));
    assertTrue(Preferences.getBoolean("_bastilleLastBattleWon"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
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
    responseText = html("request/test_bastille_game1_7.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(118, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1317;
    BastilleBattalionManager.visitChoice(request);
    assertEquals("Build the memorial", Preferences.getString("_bastilleChoice1"));
    assertEquals("Improve the keep", Preferences.getString("_bastilleChoice2"));
    assertEquals("Add more windows", Preferences.getString("_bastilleChoice3"));

    // Select an option
    urlString = "choice.php?whichchoice=1317&option=3";
    expected = "Add more windows";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_game1_7_8.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1317;
    ChoiceManager.lastDecision = 3;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1317, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(118, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "MA=125,MD=115,CA=125,CD=110,PA=150,PD=120", Preferences.getString("_bastilleStats"));

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
    responseText = html("request/test_bastille_game1_8.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(118, Preferences.getInteger("_bastilleCheese"));
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
    responseText = html("request/test_bastille_game1_8_9.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1317;
    ChoiceManager.lastDecision = 2;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1317, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(118, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "MA=130,MD=115,CA=130,CD=110,PA=155,PD=120", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1315;
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
    responseText = html("request/test_bastille_game1_9_10.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1315;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1315, responseText));
    assertEquals(139, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(257, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA<MD,CA>CD,PA>PD", Preferences.getString("_bastilleLastBattleResults"));
    assertTrue(Preferences.getBoolean("_bastilleLastBattleWon"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1314;
    BastilleBattalionManager.visitChoice(request);
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
    responseText = html("request/test_bastille_game1_10.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(257, Preferences.getInteger("_bastilleCheese"));
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
    responseText = html("request/test_bastille_game1_10_11.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1317;
    ChoiceManager.lastDecision = 3;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1317, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(257, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "MA=140,MD=115,CA=140,CD=110,PA=145,PD=120", Preferences.getString("_bastilleStats"));

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
    responseText = html("request/test_bastille_game1_11.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1314;
    ChoiceManager.lastDecision = 3;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1314, responseText));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(257, Preferences.getInteger("_bastilleCheese"));
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
    responseText = html("request/test_bastille_game1_11_12.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1319;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1319, responseText));
    assertEquals(133, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(390, Preferences.getInteger("_bastilleCheese"));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "MA=140,MD=115,CA=140,CD=110,PA=145,PD=120", Preferences.getString("_bastilleStats"));
    assertThat(
        "_bastilleOptionsTaken",
        isSetTo(
            "Adopt the radical combat style,Draft those artists,Convert the galleries,"
                + "Repurpose the statues,Add more windows,Approve the retrofit,"
                + "Strengthen the walls,Raid the cave"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1315;
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
    responseText = html("request/test_bastille_game1_12_loss.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1315;
    ChoiceManager.lastDecision = 1;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1315, responseText));
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals("MA>MD,CA<CD,PA<PD", Preferences.getString("_bastilleLastBattleResults"));
    assertFalse(Preferences.getBoolean("_bastilleLastBattleWon"));

    // The response is the "visit" to a new choice, but KoL will not "visit" it
    // until the user clicks a button and submits a request to choice 1316.

    // GAME OVER
    urlString = "choice.php?whichchoice=1316&option=3";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    responseText = html("request/test_bastille_game1_done.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1316;
    ChoiceManager.lastDecision = 3;
    assertNull(AdventureRequest.parseChoiceEncounter(urlString, 1316, responseText));
    BastilleBattalionManager.postChoice1(urlString, request);

    // The game is complete and we are no longer in a game
    assertEquals(1, Preferences.getInteger("_bastilleGames"));
    assertEquals(0, Preferences.getInteger("_bastilleGameTurn"));
    assertThat("_bastilleRewardsCollected", isSetTo(false));

    // We lost. The enemy is cleared, but the results, stats and cheese remain
    assertEquals("", Preferences.getString("_bastilleEnemyName"));
    assertEquals("", Preferences.getString("_bastilleEnemyCastle"));
    assertEquals(
        "MA=140,MD=115,CA=140,CD=110,PA=145,PD=120", Preferences.getString("_bastilleStats"));
    assertEquals("MA>MD,CA<CD,PA<PD", Preferences.getString("_bastilleLastBattleResults"));
    assertEquals(0, Preferences.getInteger("_bastilleLastCheese"));
    assertEquals(390, Preferences.getInteger("_bastilleCheese"));
  }

  @Test
  public void thatStartingNewGameResetsStats() {
    // When you lose a battle, the game ends and your stats are reset to
    // only what your upgrades provide to you.

    // We are presumed to have parsed these before
    Preferences.setInteger("_bastilleGames", 3);
    Preferences.setInteger("_bastilleGameTurn", 5);
    Preferences.setInteger("_bastilleCheese", 284);
    Preferences.setString("_bastilleEnemyName", "Lew the Vast");
    Preferences.setString("_bastilleEnemyCastle", "bigcastle");
    Preferences.setString("_bastilleChoice1", "Blunt everything");
    Preferences.setString("_bastilleChoice2", "Lower the walls");
    Preferences.setString("_bastilleChoice3", "Make the soldiers masons");
    Preferences.setString("_bastilleStats", "MA=145,MD=125,CA=140,CD=120,PA=125,PD=120");
    Preferences.setString("_bastilleOptionsTaken", "Lower the walls");

    // Finish upgrading just before the battle
    String urlString = "choice.php?whichchoice=1318&option=3";
    String expected = "Make the soldiers masons";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    String responseText = html("request/test_bastille_end_game_start_game_1.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1318;
    ChoiceManager.lastDecision = 3;
    BastilleBattalionManager.postChoice1(urlString, request);
    assertEquals(
        "MA=140,MD=125,CA=140,CD=130,PA=125,PD=120", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));
    assertThat("_bastilleOptionsTaken", isSetTo("Lower the walls,Make the soldiers masons"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1315;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(6, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));

    // Enter into battle with your foe.
    urlString = "choice.php?whichchoice=1315&option=1";
    expected = "Turn #6: Charge!";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    assertEquals(expected, RequestLogger.previousUpdateString);
    responseText = html("request/test_bastille_end_game_start_game_2.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1315;
    ChoiceManager.lastDecision = 1;
    BastilleBattalionManager.postChoice1(urlString, request);

    // We lost the battle. The enemy is cleared, but stats and results are not
    // reset yet as a script might need to look at them.
    assertEquals("MA<MD,CA<CD,PA>PD", Preferences.getString("_bastilleLastBattleResults"));
    assertEquals(false, Preferences.getBoolean("_bastilleLastBattleWon"));
    assertEquals(0, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("", Preferences.getString("_bastilleEnemyName"));
    assertEquals("", Preferences.getString("_bastilleEnemyCastle"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));
    assertEquals(
        "MA=140,MD=125,CA=140,CD=130,PA=125,PD=120", Preferences.getString("_bastilleStats"));

    // The response is the "visit" to a new choice
    ChoiceManager.lastChoice = 1316;
    BastilleBattalionManager.visitChoice(request);
    assertEquals(1, Preferences.getInteger("_bastilleGames"));

    // Choose not to simply Walk Away
    urlString = "choice.php?whichchoice=1316&option=2";
    // We don't log returning to the console
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    responseText = html("request/test_bastille_end_game_start_game_3.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1316;
    ChoiceManager.lastDecision = 2;
    BastilleBattalionManager.postChoice1(urlString, request);
    // Games played comes from the game over screen
    assertEquals(1, Preferences.getInteger("_bastilleGames"));
    assertEquals(0, Preferences.getInteger("_bastilleGameTurn"));
    assertThat("_bastilleRewardsCollected", isSetTo(true));

    // Start a new game.
    urlString = "choice.php?whichchoice=1313&option=5";
    assertTrue(BastilleBattalionManager.registerRequest(urlString));
    responseText = html("request/test_bastille_end_game_start_game_4.html");
    request.constructURLString(urlString);
    request.responseText = responseText;
    ChoiceManager.lastChoice = 1313;
    ChoiceManager.lastDecision = 5;
    BastilleBattalionManager.postChoice1(urlString, request);
    // Reset stats, upcoming foe, and turn
    assertEquals(1, Preferences.getInteger("_bastilleGameTurn"));
    assertEquals("Murderous Moore", Preferences.getString("_bastilleEnemyName"));
    assertEquals("berserker", Preferences.getString("_bastilleEnemyCastle"));
    assertEquals("", Preferences.getString("_bastilleChoice1"));
    assertEquals("", Preferences.getString("_bastilleChoice2"));
    assertEquals("", Preferences.getString("_bastilleChoice3"));
    assertEquals(
        "MA=120,MD=140,CA=100,CD=120,PA=120,PD=120", Preferences.getString("_bastilleStats"));
    assertTrue(BastilleBattalionManager.checkNeedles(responseText));
    assertEquals(0, Preferences.getInteger("_bastilleCheese"));
    assertThat("_bastilleOptionsTaken", isSetTo(""));
  }

  @Test
  public void detectsNeedleMismatch() {
    var html = html("request/test_bastille_configure_0.html");
    try (var _ =
        new Cleanups(withProperty("_bastilleStats", "MA=100,MD=110,CA=110,CD=120,PA=130,PD=150"))) {
      assertTrue(BastilleBattalionManager.checkNeedles(html));
    }
    try (var _ =
        new Cleanups(withProperty("_bastilleStats", "MA=110,MD=110,CA=110,CD=120,PA=130,PD=150"))) {
      assertFalse(BastilleBattalionManager.checkNeedles(html));
    }
  }

  @Test
  public void revisitingMidGameKeepsStats() {
    var stats = "MA=140,MD=125,CA=140,CD=130,PA=125,PD=120";
    var html = html("request/test_bastille_end_game_start_game_1.html");
    try (var _ = new Cleanups(withProperty("_bastilleStats", stats), withChoice(1315, html))) {
      var request = new GenericRequest("choice.php?forceoption=0");
      request.responseText = html;
      BastilleBattalionManager.visitChoice(request);
      assertThat("_bastilleStats", isSetTo(stats));
    }
  }

  @Nested
  class LockIn {
    private static final String LOCK_IN_TEXT = "<p>Happy with your score, you Lock it in!";

    @Test
    public void lockingInZeroCheeseRecordsOne() {
      try (var _ =
          new Cleanups(
              withProperty("_bastilleCheese", 0), withPostChoice1(1316, 1, LOCK_IN_TEXT))) {
        assertThat("_bastilleLockedInScore", isSetTo(1));
      }
    }

    @Test
    public void lockInWithoutConfirmationIsIgnored() {
      try (var _ =
          new Cleanups(withProperty("_bastilleCheese", 390), withPostChoice1(1316, 1, ""))) {
        assertThat("_bastilleLockedInScore", isSetTo(0));
      }
    }

    @Test
    public void lockingInRecordsScore() {
      try (var _ =
          new Cleanups(
              withProperty("_bastilleCheese", 390), withPostChoice1(1316, 1, LOCK_IN_TEXT))) {
        assertThat("_bastilleLockedInScore", isSetTo(390));
      }
    }

    @Test
    public void lockingInAgainUpdatesScore() {
      try (var _ =
          new Cleanups(
              withProperty("_bastilleCheese", 500),
              withProperty("_bastilleLockedInScore", 390),
              withPostChoice1(1316, 1, LOCK_IN_TEXT))) {
        assertThat("_bastilleLockedInScore", isSetTo(500));
      }
    }

    @Test
    public void playingAgainDoesNotLockIn() {
      try (var _ =
          new Cleanups(withProperty("_bastilleCheese", 390), withPostChoice1(1316, 2, ""))) {
        assertThat("_bastilleLockedInScore", isSetTo(0));
      }
    }

    @Test
    public void lockedInScoreResetsDaily() {
      try (var _ = new Cleanups(withProperty("_bastilleLockedInScore", 390))) {
        Preferences.resetDailies();
        assertThat("_bastilleLockedInScore", isSetTo(0));
      }
    }
  }

  @Nested
  class StartGame {
    @Test
    public void startingWithNoGamesLeftKeepsState() {
      try (var _ =
          new Cleanups(
              withProperty("_bastilleCheese", 390),
              withProperty("_bastilleOptionsTaken", "Lower the walls"),
              withPostChoice1(1313, 5, html("request/test_bastille_configure_0.html")))) {
        assertThat("_bastilleCheese", isSetTo(390));
        assertThat("_bastilleOptionsTaken", isSetTo("Lower the walls"));
      }
    }

    @Test
    public void startingWithNoGamesLeftDoesNotLogStart() {
      SessionLoggerOutput.startStream();
      BastilleBattalionManager.registerRequest("choice.php?whichchoice=1313&option=5");
      try (var _ =
          new Cleanups(withPostChoice1(1313, 5, html("request/test_bastille_configure_0.html")))) {
        assertThat(SessionLoggerOutput.stopStream(), not(containsString("Starting game")));
      }
    }

    @Test
    public void startingGameLogsStart() {
      SessionLoggerOutput.startStream();
      BastilleBattalionManager.registerRequest("choice.php?whichchoice=1313&option=5");
      try (var _ =
          new Cleanups(withPostChoice1(1313, 5, html("request/test_bastille_game1_0_1.html")))) {
        assertThat(SessionLoggerOutput.stopStream(), containsString("Starting game #1"));
      }
    }
  }
}
