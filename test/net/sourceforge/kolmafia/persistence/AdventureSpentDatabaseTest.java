package net.sourceforge.kolmafia.persistence;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.ApiRequest;
import net.sourceforge.kolmafia.request.CharPaneRequest;
import net.sourceforge.kolmafia.request.CreateItemRequest;
import net.sourceforge.kolmafia.request.FightRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.SingleUseRequest;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.AfterAll;
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

  @AfterAll
  public static void cleanup() {
    CharPaneRequest.reset();
    KoLAdventure.setLastAdventure("");
    AdventureSpentDatabase.resetTurns(false);
    KoLConstants.inventory.clear();
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
    KoLCharacter.setTurnsPlayed(1332325);
    KoLCharacter.setCurrentRun(1332325);
    AdventureSpentDatabase.setLastTurnUpdated(1332325);

    // In fight, about to administer killing blow. charpane.php
    String urlString = "charpane.php";
    String responseText = loadHTMLResponse("request/test_adventures_spent_fight_1_0.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332325, KoLCharacter.getTurnsPlayed());
    assertEquals(1332325, KoLCharacter.getCurrentRun());
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
    assertEquals(1332326, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    // *** FightRequest does not update this
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
    assertEquals(1332326, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    // *** Still not updated
    assertEquals(1332325, AdventureSpentDatabase.getLastTurnUpdated());

    // Select choice.
    // urlString = "choice.php?pwd&whichchoice=879&option=3";
    // This redirects into a fight.
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
    assertEquals(1332326, KoLCharacter.getTurnsPlayed());
    assertEquals(1332326, KoLCharacter.getCurrentRun());
    assertEquals(0, FightRequest.currentRound);
    assertEquals(2, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332325, AdventureSpentDatabase.getLastTurnUpdated());
    assertFalse(KoLCharacter.inFightOrChoice());

    // Won fight. charpane.php
    urlString = "charpane.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_fight_1_7.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332327, KoLCharacter.getTurnsPlayed());
    assertEquals(1332327, KoLCharacter.getCurrentRun());
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
    KoLCharacter.setTurnsPlayed(1332327);
    KoLCharacter.setCurrentRun(1332327);
    AdventureSpentDatabase.setLastTurnUpdated(1332327);

    // In fight, about to administer killing blow. charpane.php
    String urlString = "charpane.php";
    String responseText = loadHTMLResponse("request/test_adventures_spent_fight_2_0.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332327, KoLCharacter.getTurnsPlayed());
    assertEquals(1332327, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332327, AdventureSpentDatabase.getLastTurnUpdated());

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
    assertEquals(1332328, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    // *** FightRequest does not update this
    assertEquals(1332327, AdventureSpentDatabase.getLastTurnUpdated());

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
    assertEquals(1332328, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    // *** Still not updated
    assertEquals(1332327, AdventureSpentDatabase.getLastTurnUpdated());

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
    assertEquals(1332328, KoLCharacter.getCurrentRun());
    // *** The following reveals a bug: this choice did NOT actually take an extra turn,
    // *** but the AdventureSpentDatabase counted one.
    assertEquals(2, AdventureSpentDatabase.getTurns(location, true));
    // assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332328, AdventureSpentDatabase.getLastTurnUpdated());
  }

  @Test
  public void canCountBinderClipInHiddenOfficeBuilding() throws IOException {
    KoLAdventure location = AdventureDatabase.getAdventure("The Hidden Office Building");
    KoLAdventure.setLastAdventure(location);
    KoLCharacter.setTurnsPlayed(59404);
    KoLCharacter.setCurrentRun(622);
    AdventureSpentDatabase.setLastTurnUpdated(622);

    // Don't let response parsing trigger this; we will process the requests manually
    Preferences.setBoolean("autoCraft", false);

    KoLConstants.inventory.clear();
    AdventureResult MCCLUSKY_FILE_PAGE1 = ItemPool.get(ItemPool.MCCLUSKY_FILE_PAGE1, 1);
    AdventureResult MCCLUSKY_FILE_PAGE2 = ItemPool.get(ItemPool.MCCLUSKY_FILE_PAGE2, 1);
    AdventureResult MCCLUSKY_FILE_PAGE3 = ItemPool.get(ItemPool.MCCLUSKY_FILE_PAGE3, 1);
    AdventureResult MCCLUSKY_FILE_PAGE4 = ItemPool.get(ItemPool.MCCLUSKY_FILE_PAGE4, 1);
    AdventureResult MCCLUSKY_FILE_PAGE5 = ItemPool.get(ItemPool.MCCLUSKY_FILE_PAGE5, 1);
    AdventureResult BINDER_CLIP = ItemPool.get(ItemPool.BINDER_CLIP, 1);
    AdventureResult MCCLUSKY_FILE = ItemPool.get(ItemPool.MCCLUSKY_FILE, 1);

    AdventureResult.addResultToList(KoLConstants.inventory, MCCLUSKY_FILE_PAGE1);
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.MCCLUSKY_FILE_PAGE2, 1));
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.MCCLUSKY_FILE_PAGE3, 1));
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.MCCLUSKY_FILE_PAGE4, 1));
    AdventureResult.addResultToList(
        KoLConstants.inventory, ItemPool.get(ItemPool.MCCLUSKY_FILE_PAGE5, 1));

    // adventure.php?snarfblat=343
    // redirect -> choice.php?forceoption=0
    String urlString = "choice.php?forceoption=0.php";
    String responseText = loadHTMLResponse("request/test_adventures_spent_binder_clip_1.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 786);
    assertEquals(ChoiceManager.lastDecision, 0);

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = loadHTMLResponse("request/test_adventures_spent_binder_clip_2.html");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(59404, KoLCharacter.getTurnsPlayed());
    assertEquals(622, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(622, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "choice.php?whichchoice=786&option=2&pwd";
    responseText = loadHTMLResponse("request/test_adventures_spent_binder_clip_3.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    request.setHasResult(true);
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertFalse(ChoiceManager.handlingChoice);
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(622, AdventureSpentDatabase.getLastTurnUpdated());
    assertEquals(1, InventoryManager.getCount(BINDER_CLIP));

    // Simulate Auto crafting
    CreateItemRequest creator = CreateItemRequest.getInstance(ItemPool.MCCLUSKY_FILE);
    creator.setQuantityNeeded(1);
    assertTrue(creator instanceof SingleUseRequest);
    creator.reconstructFields();
    urlString = "inv_use.php?which=3&whichitem=6694&ajax=1";
    assertEquals(urlString, creator.getURLString());
    SingleUseRequest.registerRequest(urlString);

    responseText = loadHTMLResponse("request/test_adventures_spent_binder_clip_4.html");
    creator.responseText = responseText;
    creator.setHasResult(true);
    creator.processResponse();
    assertEquals(0, InventoryManager.getCount(MCCLUSKY_FILE_PAGE1));
    assertEquals(0, InventoryManager.getCount(MCCLUSKY_FILE_PAGE2));
    assertEquals(0, InventoryManager.getCount(MCCLUSKY_FILE_PAGE3));
    assertEquals(0, InventoryManager.getCount(MCCLUSKY_FILE_PAGE4));
    assertEquals(0, InventoryManager.getCount(MCCLUSKY_FILE_PAGE5));
    assertEquals(0, InventoryManager.getCount(BINDER_CLIP));
    assertEquals(1, InventoryManager.getCount(MCCLUSKY_FILE));

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = loadHTMLResponse("request/test_adventures_spent_binder_clip_5.html");
    ApiRequest.parseResponse(urlString, responseText);
    // The total turn and current run counts advanced
    assertEquals(59405, KoLCharacter.getTurnsPlayed());
    assertEquals(623, KoLCharacter.getCurrentRun());

    // The AdventureSpentDatabase updated as expected
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(623, AdventureSpentDatabase.getLastTurnUpdated());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
  }

  @Test
  public void canCountBeehiveInBlackForest() throws IOException {
    KoLAdventure location = AdventureDatabase.getAdventure("The Black Forest");
    KoLAdventure.setLastAdventure(location);
    KoLCharacter.setTurnsPlayed(988140);
    KoLCharacter.setCurrentRun(988140);
    AdventureSpentDatabase.setLastTurnUpdated(988140);

    KoLConstants.inventory.clear();
    AdventureResult BEEHIVE = ItemPool.get(ItemPool.BEEHIVE, 1);

    // About to adventure in The Black Forest
    String urlString = "charpane.php";
    String responseText = loadHTMLResponse("request/test_adventures_spent_beehive_0.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(988140, KoLCharacter.getTurnsPlayed());
    assertEquals(988140, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(988140, AdventureSpentDatabase.getLastTurnUpdated());

    // adventure.php?snarfblat=405
    // redirect -> choice.php?forceoption=0
    urlString = "choice.php?forceoption=0";
    responseText = loadHTMLResponse("request/test_adventures_spent_beehive_1.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 923);
    assertEquals(ChoiceManager.lastDecision, 0);

    urlString = "charpane.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_beehive_2.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(988140, KoLCharacter.getTurnsPlayed());
    assertEquals(988140, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(988140, AdventureSpentDatabase.getLastTurnUpdated());

    // Choose to visit cobbler
    urlString = "choice.php?pwd&whichchoice=923&option=1";
    responseText = loadHTMLResponse("request/test_adventures_spent_beehive_3.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    // No charpane.php call requested
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988140, AdventureSpentDatabase.getLastTurnUpdated());

    // Head towards buzzing
    urlString = "choice.php?pwd&whichchoice=924&option=3";
    responseText = loadHTMLResponse("request/test_adventures_spent_beehive_4.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988140, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "charpane.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_beehive_5.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(988141, KoLCharacter.getTurnsPlayed());
    assertEquals(988141, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    // *** Did not update turn?
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988140, AdventureSpentDatabase.getLastTurnUpdated());

    // Keep going
    urlString = "choice.php?pwd&whichchoice=1018&option=1";
    responseText = loadHTMLResponse("request/test_adventures_spent_beehive_6.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988140, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "charpane.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_beehive_7.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(988142, KoLCharacter.getTurnsPlayed());
    assertEquals(988142, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    // *** Did not update turn?
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988140, AdventureSpentDatabase.getLastTurnUpdated());

    // Almost... there...
    urlString = "choice.php?pwd&whichchoice=1019&option=1";
    responseText = loadHTMLResponse("request/test_adventures_spent_beehive_8.html");
    request = new GenericRequest(urlString);
    request.setHasResult(true);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertEquals(1, InventoryManager.getCount(BEEHIVE));
    assertFalse(ChoiceManager.handlingChoice);
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988140, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "charpane.php";
    responseText = loadHTMLResponse("request/test_adventures_spent_beehive_9.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(988143, KoLCharacter.getTurnsPlayed());
    assertEquals(988143, KoLCharacter.getCurrentRun());
    // *** What the? We just spent three turns.
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988143, AdventureSpentDatabase.getLastTurnUpdated());
  }
}
