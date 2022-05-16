package net.sourceforge.kolmafia.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AdventureSpentDatabaseTest {

  @BeforeAll
  private static void beforeAll() {
    // Simulate logging out and back in again.
    KoLCharacter.reset("");
    KoLCharacter.reset("adventure spent database user");
  }

  @BeforeEach
  private void beforeEach() {
    CharPaneRequest.reset();
    AdventureSpentDatabase.resetTurns(false);
  }

  static String loadHTMLResponse(String path) throws IOException {
    // Load the responseText from saved HTML file
    return Files.readString(Paths.get(path)).trim();
  }

  @Test
  public void canCountFightChoiceFightInHauntedBedroom() throws IOException {
    // Every encounter in The Haunted Bedroom is a fight followed by a choice adventure.
    // The fight takes a turn.
    // One of the choice/options leads to a fight - which costs another turn.
    // Every other choice/option does not take a turn.
    //
    // This tests the fight -> choice -> fight case

    KoLAdventure location = AdventureDatabase.getAdventure("The Haunted Bedroom");
    KoLAdventure.setLastAdventure(location);

    // In fight, about to administer killing blow. charpane.php
    String urlString = "charpane.php";
    String responseText = loadHTMLResponse("request/test_adventures_spent_fight_1_0.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332325, KoLCharacter.getTurnsPlayed());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332325, AdventureSpentDatabase.getLastTurnUpdated());

    // In-fight, final round. fight.php
    urlString = "fight.php?action=attack";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_1_1.html");
    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, responseText);
    assertEquals(0, FightRequest.currentRound);
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332325, AdventureSpentDatabase.getLastTurnUpdated());

    // Won fight. charpane.php
    urlString = "charpane.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_1_2.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332326, KoLCharacter.getTurnsPlayed());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332325, AdventureSpentDatabase.getLastTurnUpdated());

    // Investigate fallen foe. choice.php
    urlString = "choice.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_1_3.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertEquals(ChoiceManager.lastChoice, 879);
    assertEquals(ChoiceManager.lastDecision, 0);

    // In choice adventure. charpane.php
    urlString = "charpane.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_1_4.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332326, KoLCharacter.getTurnsPlayed());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332325, AdventureSpentDatabase.getLastTurnUpdated());

    // Select choice.
    // urlString = "choice.php?pwd&whichchoice=879&option=3";
    // This redirects:
    ChoiceManager.handlingChoice = false;
    urlString = "fight.php?ireallymeanit=1652726190";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_1_5.html");
    FightRequest.currentRound = 0;
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, responseText);
    assertEquals(1, FightRequest.currentRound);
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332325, AdventureSpentDatabase.getLastTurnUpdated());

    // NO charpane.php requested.

    // In fight. Round 1.
    urlString = "fight.php?action=attack";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_1_6.html");
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, responseText);
    assertEquals(0, FightRequest.currentRound);
    assertEquals(2, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332326, KoLCharacter.getTurnsPlayed());
    assertEquals(1332325, AdventureSpentDatabase.getLastTurnUpdated());
    assertFalse(KoLCharacter.inFightOrChoice());

    // Won fight. charpane.php
    urlString = "charpane.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_1_7.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332327, KoLCharacter.getTurnsPlayed());
    assertEquals(2, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332327, AdventureSpentDatabase.getLastTurnUpdated());
  }

  @Test
  public void canCountFightChoiceInHauntedBedroom() throws IOException {
    // Every encounter in The Haunted Bedroom is a fight followed by a choice adventure.
    // The fight takes a turn.
    // One of the choice/options leads to a fight - which costs another turn.
    // Every other choice/option does not take a turn.
    //
    // This tests the fight -> choice -> no fight case

    KoLAdventure location = AdventureDatabase.getAdventure("The Haunted Bedroom");
    KoLAdventure.setLastAdventure(location);

    // In fight, about to administer killing blow. charpane.php
    String urlString = "charpane.php";
    String responseText = loadHTMLResponse("request/test_adventures_spent_fight_2_0.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332327, KoLCharacter.getTurnsPlayed());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));

    // In-fight, final round. fight.php
    urlString = "fight.php?action=attack";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_2_1.html");
    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, responseText);
    assertEquals(0, FightRequest.currentRound);
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332327, AdventureSpentDatabase.getLastTurnUpdated());

    // Won fight. charpane.php
    urlString = "charpane.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_2_2.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332328, KoLCharacter.getTurnsPlayed());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));

    // Investigate fallen foe. choice.php
    urlString = "choice.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_2_3.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 879);
    assertEquals(ChoiceManager.lastDecision, 0);

    // In choice adventure. charpane.php
    urlString = "charpane.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_2_4.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332328, KoLCharacter.getTurnsPlayed());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));

    // Select choice.
    urlString = "choice.php?pwd&whichchoice=879&option=1";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_2_5.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertFalse(ChoiceManager.handlingChoice);
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(1332327, AdventureSpentDatabase.getLastTurnUpdated());

    // In choice adventure. charpane.php
    urlString = "charpane.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_2_6.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332328, KoLCharacter.getTurnsPlayed());
    // *** The following reveals a bug: this choice did NOT actually take an extra turn,
    // *** but the AdventureSpentDatabase counted one.
    assertEquals(2, AdventureSpentDatabase.getTurns(location, true));
    // assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332328, AdventureSpentDatabase.getLastTurnUpdated());
  }
}
