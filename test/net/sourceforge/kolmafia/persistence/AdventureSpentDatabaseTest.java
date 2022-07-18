package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.*;

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
    Preferences.saveSettingsToFile = false;
  }

  @BeforeEach
  private void beforeEach() {
    CharPaneRequest.reset();
    AdventureSpentDatabase.resetTurns(false);
    // Some of the response texts have a CAB, some do not.
    // api.php will set this...
    Preferences.setBoolean("serverAddsCustomCombat", false);
  }

  @AfterAll
  public static void cleanup() {
    CharPaneRequest.reset();
    KoLAdventure.setLastAdventure("");
    AdventureSpentDatabase.resetTurns(false);
    KoLConstants.inventory.clear();
  }

  @Test
  public void canCountFightChoiceFightInHauntedBedroom() {
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
    String responseText = html("request/test_adventures_spent_fight_1_0.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332325, KoLCharacter.getTurnsPlayed());
    assertEquals(1332325, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332325, AdventureSpentDatabase.getLastTurnUpdated());

    // In-fight, final round. fight.php
    urlString = "fight.php?action=attack";
    responseText = html("request/test_adventures_spent_fight_1_1.html");
    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, responseText);
    assertEquals(0, FightRequest.currentRound);
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332325, AdventureSpentDatabase.getLastTurnUpdated());

    // Won fight. charpane.php
    urlString = "charpane.php";
    responseText = html("request/test_adventures_spent_fight_1_2.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332326, KoLCharacter.getTurnsPlayed());
    assertEquals(1332326, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332326, AdventureSpentDatabase.getLastTurnUpdated());

    // Investigate fallen foe. choice.php
    urlString = "choice.php";
    responseText = html("request/test_adventures_spent_fight_1_3.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertEquals(ChoiceManager.lastChoice, 879);
    assertEquals(ChoiceManager.lastDecision, 0);

    // In choice adventure. charpane.php
    urlString = "charpane.php";
    responseText = html("request/test_adventures_spent_fight_1_4.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332326, KoLCharacter.getTurnsPlayed());
    assertEquals(1332326, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332326, AdventureSpentDatabase.getLastTurnUpdated());

    // Select choice.
    // urlString = "choice.php?pwd&whichchoice=879&option=3";
    // This redirects into a fight.
    urlString = "fight.php?ireallymeanit=1652726190";
    responseText = html("request/test_adventures_spent_fight_1_5.html");
    FightRequest.preFight(true);
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, responseText);
    assertEquals(1, FightRequest.currentRound);
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332326, AdventureSpentDatabase.getLastTurnUpdated());

    // NO charpane.php requested.

    // In fight. Round 1.
    urlString = "fight.php?action=attack";
    responseText = html("request/test_adventures_spent_fight_1_6.html");
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, responseText);
    assertEquals(1332326, KoLCharacter.getTurnsPlayed());
    assertEquals(1332326, KoLCharacter.getCurrentRun());
    assertEquals(0, FightRequest.currentRound);
    assertEquals(2, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332326, AdventureSpentDatabase.getLastTurnUpdated());

    // Won fight. charpane.php
    urlString = "charpane.php";
    responseText = html("request/test_adventures_spent_fight_1_7.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332327, KoLCharacter.getTurnsPlayed());
    assertEquals(1332327, KoLCharacter.getCurrentRun());
    assertEquals(2, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332327, AdventureSpentDatabase.getLastTurnUpdated());
  }

  @Test
  public void canCountFightChoiceInHauntedBedroom() {
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
    String responseText = html("request/test_adventures_spent_fight_2_0.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332327, KoLCharacter.getTurnsPlayed());
    assertEquals(1332327, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332327, AdventureSpentDatabase.getLastTurnUpdated());

    // In-fight, final round. fight.php
    urlString = "fight.php?action=attack";
    responseText = html("request/test_adventures_spent_fight_2_1.html");
    FightRequest.currentRound = 2;
    FightRequest.registerRequest(true, urlString);
    FightRequest.updateCombatData(null, null, responseText);
    assertEquals(0, FightRequest.currentRound);
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332327, AdventureSpentDatabase.getLastTurnUpdated());

    // Won fight. charpane.php
    urlString = "charpane.php";
    responseText = html("request/test_adventures_spent_fight_2_2.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332328, KoLCharacter.getTurnsPlayed());
    assertEquals(1332328, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332328, AdventureSpentDatabase.getLastTurnUpdated());

    // Investigate fallen foe. choice.php
    urlString = "choice.php";
    responseText = html("request/test_adventures_spent_fight_2_3.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 879);
    assertEquals(ChoiceManager.lastDecision, 0);

    // In choice adventure. charpane.php
    urlString = "charpane.php";
    responseText = html("request/test_adventures_spent_fight_2_4.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332328, KoLCharacter.getTurnsPlayed());
    assertEquals(1332328, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332328, AdventureSpentDatabase.getLastTurnUpdated());

    // Select choice.
    urlString = "choice.php?pwd&whichchoice=879&option=1";
    responseText = html("request/test_adventures_spent_fight_2_5.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertFalse(ChoiceManager.handlingChoice);
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(1332328, AdventureSpentDatabase.getLastTurnUpdated());

    // In choice adventure. charpane.php
    urlString = "charpane.php";
    responseText = html("request/test_adventures_spent_fight_2_6.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(1332328, KoLCharacter.getTurnsPlayed());
    assertEquals(1332328, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1332328, AdventureSpentDatabase.getLastTurnUpdated());
  }

  @Test
  public void canCountBinderClipInHiddenOfficeBuilding() {
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
    AdventureResult.addResultToList(KoLConstants.inventory, MCCLUSKY_FILE_PAGE2);
    AdventureResult.addResultToList(KoLConstants.inventory, MCCLUSKY_FILE_PAGE3);
    AdventureResult.addResultToList(KoLConstants.inventory, MCCLUSKY_FILE_PAGE4);
    AdventureResult.addResultToList(KoLConstants.inventory, MCCLUSKY_FILE_PAGE5);

    // adventure.php?snarfblat=343
    // redirect -> choice.php?forceoption=0
    String urlString = "choice.php?forceoption=0";
    String responseText = html("request/test_adventures_spent_binder_clip_1.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 786);
    assertEquals(ChoiceManager.lastDecision, 0);

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_binder_clip_2.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(59404, KoLCharacter.getTurnsPlayed());
    assertEquals(622, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(622, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "choice.php?whichchoice=786&option=2&pwd";
    responseText = html("request/test_adventures_spent_binder_clip_3.html");
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

    responseText = html("request/test_adventures_spent_binder_clip_4.html");
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
    responseText = html("request/test_adventures_spent_binder_clip_5.json");
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
  public void canCountBeehiveInBlackForest() {
    KoLAdventure location = AdventureDatabase.getAdventure("The Black Forest");
    KoLAdventure.setLastAdventure(location);
    KoLCharacter.setTurnsPlayed(988140);
    KoLCharacter.setCurrentRun(988140);
    AdventureSpentDatabase.setLastTurnUpdated(988140);

    KoLConstants.inventory.clear();
    AdventureResult BEEHIVE = ItemPool.get(ItemPool.BEEHIVE, 1);

    // About to adventure in The Black Forest
    String urlString = "charpane.php";
    String responseText = html("request/test_adventures_spent_beehive_0.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(988140, KoLCharacter.getTurnsPlayed());
    assertEquals(988140, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(988140, AdventureSpentDatabase.getLastTurnUpdated());

    // adventure.php?snarfblat=405
    // redirect -> choice.php?forceoption=0
    urlString = "choice.php?forceoption=0";
    responseText = html("request/test_adventures_spent_beehive_1.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 923);
    assertEquals(ChoiceManager.lastDecision, 0);

    urlString = "charpane.php";
    responseText = html("request/test_adventures_spent_beehive_2.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(988140, KoLCharacter.getTurnsPlayed());
    assertEquals(988140, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(988140, AdventureSpentDatabase.getLastTurnUpdated());

    // Choose to visit cobbler
    urlString = "choice.php?pwd&whichchoice=923&option=1";
    responseText = html("request/test_adventures_spent_beehive_3.html");
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
    responseText = html("request/test_adventures_spent_beehive_4.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988140, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "charpane.php";
    responseText = html("request/test_adventures_spent_beehive_5.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(988141, KoLCharacter.getTurnsPlayed());
    assertEquals(988141, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988141, AdventureSpentDatabase.getLastTurnUpdated());

    // Keep going
    urlString = "choice.php?pwd&whichchoice=1018&option=1";
    responseText = html("request/test_adventures_spent_beehive_6.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988141, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "charpane.php";
    responseText = html("request/test_adventures_spent_beehive_7.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(988142, KoLCharacter.getTurnsPlayed());
    assertEquals(988142, KoLCharacter.getCurrentRun());
    assertEquals(2, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988142, AdventureSpentDatabase.getLastTurnUpdated());

    // Almost... there...
    urlString = "choice.php?pwd&whichchoice=1019&option=1";
    responseText = html("request/test_adventures_spent_beehive_8.html");
    request = new GenericRequest(urlString);
    request.setHasResult(true);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertEquals(1, InventoryManager.getCount(BEEHIVE));
    assertFalse(ChoiceManager.handlingChoice);
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988142, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "charpane.php";
    responseText = html("request/test_adventures_spent_beehive_9.html");
    CharPaneRequest.processResults(responseText);
    assertEquals(988143, KoLCharacter.getTurnsPlayed());
    assertEquals(988143, KoLCharacter.getCurrentRun());
    assertEquals(3, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988143, AdventureSpentDatabase.getLastTurnUpdated());
  }

  @Test
  public void canCountZeroTurnChoiceChain() {
    KoLAdventure location = AdventureDatabase.getAdventure("The Spooky Forest");
    KoLAdventure.setLastAdventure(location);
    KoLCharacter.setTurnsPlayed(988527);
    KoLCharacter.setCurrentRun(988527);
    AdventureSpentDatabase.setLastTurnUpdated(988527);

    // adventure.php?snarfblat=15
    // redirect -> choice.php?forceoption=0
    // Arboreal respite
    String urlString = "choice.php?forceoption=0";
    String responseText = html("request/test_adventures_spent_spooky_forest_1_1.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 502);
    assertEquals(ChoiceManager.lastDecision, 0);

    // Explore the Stream
    urlString = "choice.php?whichchoice=502&option=2&pwd";
    responseText = html("request/test_adventures_spent_spooky_forest_1_2.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    // NO charpane.php requested
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988527, AdventureSpentDatabase.getLastTurnUpdated());

    // Squeeze into the cave
    urlString = "choice.php?whichchoice=505&option=2&pwd";
    responseText = html("request/test_adventures_spent_spooky_forest_1_3.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertFalse(ChoiceManager.handlingChoice);
    // NO charpane.php requested
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988527, AdventureSpentDatabase.getLastTurnUpdated());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
  }

  @Test
  public void canCountOneTurnChoiceChain() {
    KoLAdventure location = AdventureDatabase.getAdventure("The Spooky Forest");
    KoLAdventure.setLastAdventure(location);
    KoLCharacter.setTurnsPlayed(988527);
    KoLCharacter.setCurrentRun(988527);
    AdventureSpentDatabase.setLastTurnUpdated(988527);

    // adventure.php?snarfblat=15
    // redirect -> choice.php?forceoption=0
    // Arboreal respite
    String urlString = "choice.php?forceoption=0";
    String responseText = html("request/test_adventures_spent_spooky_forest_2_1.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 502);
    assertEquals(ChoiceManager.lastDecision, 0);
    // charpane.php requested - after a visit?
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988527, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_spooky_forest_2_2.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(988527, KoLCharacter.getTurnsPlayed());
    assertEquals(988527, KoLCharacter.getCurrentRun());
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(988527, AdventureSpentDatabase.getLastTurnUpdated());

    // Brave the dark thicket
    urlString = "choice.php?whichchoice=502&option=3&pwd";
    responseText = html("request/test_adventures_spent_spooky_forest_2_3.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    // NO charpane.php requested
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988527, AdventureSpentDatabase.getLastTurnUpdated());

    // Follow the even darker path
    urlString = "choice.php?whichchoice=506&option=1&pwd";
    responseText = html("request/test_adventures_spent_spooky_forest_2_4.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    // NO charpane.php requested
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988527, AdventureSpentDatabase.getLastTurnUpdated());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));

    // Take the scorched path
    urlString = "choice.php?whichchoice=26&option=2&pwd";
    responseText = html("request/test_adventures_spent_spooky_forest_2_5.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    // NO charpane.php requested
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988527, AdventureSpentDatabase.getLastTurnUpdated());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));

    // Investigate the smoking crater
    urlString = "choice.php?whichchoice=28&option=2&pwd";
    responseText = html("request/test_adventures_spent_spooky_forest_2_6.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertFalse(ChoiceManager.handlingChoice);
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988527, AdventureSpentDatabase.getLastTurnUpdated());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_spooky_forest_2_7.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(988528, KoLCharacter.getTurnsPlayed());
    assertEquals(988528, KoLCharacter.getCurrentRun());
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(988528, AdventureSpentDatabase.getLastTurnUpdated());
  }

  @Test
  public void canCountHiddenTempleTurns() {
    KoLAdventure location = AdventureDatabase.getAdventure("The Hidden Temple");
    KoLAdventure.setLastAdventure(location);
    KoLCharacter.setTurnsPlayed(60387);
    KoLCharacter.setCurrentRun(559);
    AdventureSpentDatabase.setLastTurnUpdated(60387);

    KoLConstants.inventory.clear();
    AdventureResult NOSTRIL_OF_THE_SERPENT = ItemPool.get(ItemPool.NOSTRIL_OF_THE_SERPENT, 1);
    AdventureResult.addResultToList(KoLConstants.inventory, NOSTRIL_OF_THE_SERPENT);

    // adventure.php?snarfblat=280
    // redirect -> choice.php?forceoption=0 -> Fitting In
    String urlString = "choice.php?forceoption=0";
    String responseText = html("request/test_adventures_spent_hidden_temple_1.html");
    GenericRequest request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 582);
    assertEquals(ChoiceManager.lastDecision, 0);

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_2.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60387, KoLCharacter.getTurnsPlayed());
    assertEquals(559, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(559, AdventureSpentDatabase.getLastTurnUpdated());

    // Poke around the ground floor -> The Hidden Heart of the Hidden Temple
    urlString = "choice.php?whichchoice=582&option=2&pwd";
    responseText = html("request/test_adventures_spent_hidden_temple_3.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    assertEquals(ChoiceManager.lastChoice, 582);
    assertEquals(ChoiceManager.lastDecision, 2);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    // NO charpane.php requested
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(559, AdventureSpentDatabase.getLastTurnUpdated());

    // Go down the stairs -> Unconfusing Buttons
    urlString = "choice.php?whichchoice=580&option=2&pwd";
    responseText = html("request/test_adventures_spent_hidden_temple_4.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    assertEquals(ChoiceManager.lastChoice, 580);
    assertEquals(ChoiceManager.lastDecision, 2);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(559, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_5.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60387, KoLCharacter.getTurnsPlayed());
    assertEquals(559, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(559, AdventureSpentDatabase.getLastTurnUpdated());

    // The one with the cute little lightning-tailed guy on it -> The Hidden Heart of the Hidden
    // Temple
    urlString = "choice.php?whichchoice=584&option=4&pwd";
    responseText = html("request/test_adventures_spent_hidden_temple_6.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    request.setHasResult(true);
    ChoiceManager.preChoice(request);
    assertEquals(ChoiceManager.lastChoice, 584);
    assertEquals(ChoiceManager.lastDecision, 4);
    assertEquals(1, InventoryManager.getCount(NOSTRIL_OF_THE_SERPENT));
    request.processResponse();
    assertEquals(0, InventoryManager.getCount(NOSTRIL_OF_THE_SERPENT));
    assertTrue(ChoiceManager.handlingChoice);
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(559, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_7.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60387, KoLCharacter.getTurnsPlayed());
    assertEquals(559, KoLCharacter.getCurrentRun());
    assertEquals(0, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(559, AdventureSpentDatabase.getLastTurnUpdated());

    // Go through the door (3 Adventures) -> At Least It's Not Full Of Trash
    urlString = "choice.php?whichchoice=580&option=1&pwd";
    responseText = html("request/test_adventures_spent_hidden_temple_8.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    assertEquals(ChoiceManager.lastChoice, 580);
    assertEquals(ChoiceManager.lastDecision, 1);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    // NO charpane.php requested
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(559, AdventureSpentDatabase.getLastTurnUpdated());

    // Raise your hands up toward the heavens -> Now What?
    urlString = "choice.php?whichchoice=123&option=2&pwd";
    responseText = html("request/test_adventures_spent_hidden_temple_9.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    assertEquals(ChoiceManager.lastChoice, 123);
    assertEquals(ChoiceManager.lastDecision, 2);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(559, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_10.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60388, KoLCharacter.getTurnsPlayed());
    assertEquals(560, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    // Continue down the corridor...
    // choice.php
    // redirect -> tiles.php -> Beginning at the Beginning of Beginning
    urlString = "tiles.php";
    responseText = html("request/test_adventures_spent_hidden_temple_11.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertFalse(ChoiceManager.handlingChoice);
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_12.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60388, KoLCharacter.getTurnsPlayed());
    assertEquals(560, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    // Give me a B!
    urlString = "tiles.php?action=jump&whichtile=4";
    responseText = html("request/test_adventures_spent_hidden_temple_13.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_14.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60388, KoLCharacter.getTurnsPlayed());
    assertEquals(560, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    // Give me an A!
    urlString = "tiles.php?action=jump&whichtile=6";
    responseText = html("request/test_adventures_spent_hidden_temple_15.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_16.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60388, KoLCharacter.getTurnsPlayed());
    assertEquals(560, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    // Give me an N!
    urlString = "tiles.php?action=jump&whichtile=3";
    responseText = html("request/test_adventures_spent_hidden_temple_17.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_18.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60388, KoLCharacter.getTurnsPlayed());
    assertEquals(560, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    // Give me an A!
    urlString = "tiles.php?action=jump&whichtile=6";
    responseText = html("request/test_adventures_spent_hidden_temple_19.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_20.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60388, KoLCharacter.getTurnsPlayed());
    assertEquals(560, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    // Give me an N!
    urlString = "tiles.php?action=jump&whichtile=7";
    responseText = html("request/test_adventures_spent_hidden_temple_21.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_22.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60388, KoLCharacter.getTurnsPlayed());
    assertEquals(560, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    // Give me an A!
    urlString = "tiles.php?action=jump&whichtile=6";
    responseText = html("request/test_adventures_spent_hidden_temple_23.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_24.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60388, KoLCharacter.getTurnsPlayed());
    assertEquals(560, KoLCharacter.getCurrentRun());
    assertEquals(1, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(560, AdventureSpentDatabase.getLastTurnUpdated());

    // Give me an S!
    // tiles.php?action=jump&whichtile=3
    // What's that spell? BANANAS!
    // redirect -> choice.php?forceoption=0 -> No Visible Means of Support
    urlString = "choice.php?forceoption=0";
    responseText = html("request/test_adventures_spent_hidden_temple_25.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    request.setHasResult(true);
    request.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 125);
    assertEquals(ChoiceManager.lastDecision, 0);

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_26.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60389, KoLCharacter.getTurnsPlayed());
    assertEquals(561, KoLCharacter.getCurrentRun());
    assertEquals(2, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(561, AdventureSpentDatabase.getLastTurnUpdated());

    // Do nothing
    urlString = "choice.php?whichchoice=125&option=3&pwd";
    responseText = html("request/test_adventures_spent_hidden_temple_27.html");
    request = new GenericRequest(urlString);
    request.responseText = responseText;
    ChoiceManager.preChoice(request);
    assertEquals(ChoiceManager.lastChoice, 125);
    assertEquals(ChoiceManager.lastDecision, 3);
    request.processResponse();
    assertFalse(ChoiceManager.handlingChoice);
    // charpane.php requested
    assertTrue(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(561, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_hidden_temple_28.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(60390, KoLCharacter.getTurnsPlayed());
    assertEquals(562, KoLCharacter.getCurrentRun());
    assertEquals(3, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(562, AdventureSpentDatabase.getLastTurnUpdated());
  }

  @Test
  public void canTrackAutomatedAirshipChoiceFight() {
    KoLAdventure location = AdventureDatabase.getAdventure("The Penultimate Fantasy Airship");
    KoLAdventure.setLastAdventure(location);
    KoLCharacter.setTurnsPlayed(61853);
    KoLCharacter.setCurrentRun(985);
    AdventureSpentDatabase.setLastTurnUpdated(985);
    AdventureSpentDatabase.setTurns(location.getAdventureName(), 56);

    FightRequest fight = FightRequest.INSTANCE;
    GenericRequest choice = ChoiceManager.CHOICE_HANDLER;

    // adventure.php?snarfblat=81
    // redirect -> fight.php?ireallymeanit=1653112277
    String urlString = "fight.php?ireallymeanit=1653112277";
    String responseText = html("request/test_adventures_spent_airship_1.html");
    FightRequest.preFight(true);
    FightRequest.registerRequest(true, urlString);
    fight.responseText = responseText;
    fight.setHasResult(true);
    fight.processResponse();
    assertEquals(1, FightRequest.currentRound);
    assertEquals(56, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(985, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_airship_2.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(61853, KoLCharacter.getTurnsPlayed());
    assertEquals(985, KoLCharacter.getCurrentRun());
    assertEquals(56, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(985, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "fight.php?action=macro&macrotext=mark+mafiafinal%0Aattack%0Agoto+mafiafinal";
    responseText = html("request/test_adventures_spent_airship_3.html");
    FightRequest.registerRequest(true, urlString);
    fight.responseText = responseText;
    fight.setHasResult(true);
    fight.processResponse();
    assertFalse(KoLCharacter.inFight());
    assertEquals(57, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(985, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_airship_4.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(61854, KoLCharacter.getTurnsPlayed());
    assertEquals(986, KoLCharacter.getCurrentRun());
    assertEquals(57, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(986, AdventureSpentDatabase.getLastTurnUpdated());

    // adventure.php?snarfblat=81
    // redirect -> choice.php?forceoption=0
    urlString = "choice.php?forceoption=0";
    responseText = html("request/test_adventures_spent_airship_5.html");
    choice.responseText = responseText;
    choice.setHasResult(true);
    ChoiceManager.preChoice(choice);
    choice.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 182);
    assertEquals(ChoiceManager.lastDecision, 0);

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_airship_6.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(61854, KoLCharacter.getTurnsPlayed());
    assertEquals(986, KoLCharacter.getCurrentRun());
    assertEquals(57, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(986, AdventureSpentDatabase.getLastTurnUpdated());

    // urlString = "choice.php?whichchoice=182&option=1&pwd";
    // This redirects into a fight.
    choice.setHasResult(true);
    ChoiceManager.preChoice(choice);
    urlString = "fight.php?ireallymeanit=1653112281";
    responseText = html("request/test_adventures_spent_airship_7.html");
    FightRequest.preFight(true);
    FightRequest.registerRequest(true, urlString);
    fight.responseText = responseText;
    fight.setHasResult(true);
    fight.processResponse();
    assertEquals(1, FightRequest.currentRound);
    assertEquals(57, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(986, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_airship_8.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(61854, KoLCharacter.getTurnsPlayed());
    assertEquals(986, KoLCharacter.getCurrentRun());
    assertEquals(57, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(986, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "fight.php?action=macro&macrotext=mark+mafiafinal%0Aattack%0Agoto+mafiafinal";
    responseText = html("request/test_adventures_spent_airship_9.html");
    FightRequest.registerRequest(true, urlString);
    fight.responseText = responseText;
    fight.setHasResult(true);
    fight.processResponse();
    assertFalse(KoLCharacter.inFight());
    assertEquals(58, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(986, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_airship_10.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(61855, KoLCharacter.getTurnsPlayed());
    assertEquals(987, KoLCharacter.getCurrentRun());
    assertEquals(58, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(987, AdventureSpentDatabase.getLastTurnUpdated());

    // adventure.php?snarfblat=81
    // redirect -> choice.php?forceoption=0
    urlString = "choice.php?forceoption=0";
    responseText = html("request/test_adventures_spent_airship_11.html");
    choice.responseText = responseText;
    choice.setHasResult(true);
    ChoiceManager.preChoice(choice);
    choice.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 182);
    assertEquals(ChoiceManager.lastDecision, 0);

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_airship_12.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(61855, KoLCharacter.getTurnsPlayed());
    assertEquals(987, KoLCharacter.getCurrentRun());
    assertEquals(58, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(987, AdventureSpentDatabase.getLastTurnUpdated());

    // urlString = "choice.php?whichchoice=182&option=1&pwd";
    // This redirects into a fight.
    choice.setHasResult(true);
    ChoiceManager.preChoice(choice);
    urlString = "fight.php?ireallymeanit=1653112283";
    responseText = html("request/test_adventures_spent_airship_13.html");
    FightRequest.preFight(true);
    FightRequest.registerRequest(true, urlString);
    fight.responseText = responseText;
    fight.setHasResult(true);
    fight.processResponse();
    assertEquals(1, FightRequest.currentRound);
    assertEquals(58, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(987, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "fight.php?action=macro&macrotext=mark+mafiafinal%0Aattack%0Agoto+mafiafinal";
    responseText = html("request/test_adventures_spent_airship_14.html");
    FightRequest.registerRequest(true, urlString);
    fight.responseText = responseText;
    fight.setHasResult(true);
    fight.processResponse();
    assertFalse(KoLCharacter.inFight());
    assertEquals(59, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(987, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_airship_15.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(61856, KoLCharacter.getTurnsPlayed());
    assertEquals(988, KoLCharacter.getCurrentRun());
    assertEquals(59, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(988, AdventureSpentDatabase.getLastTurnUpdated());
  }

  @Test
  public void canTrackAutomatedOfficeChoiceFight() {
    KoLAdventure location = AdventureDatabase.getAdventure("The Hidden Office Building");
    KoLAdventure.setLastAdventure(location);
    KoLCharacter.setTurnsPlayed(1333901);
    KoLCharacter.setCurrentRun(1333901);
    AdventureSpentDatabase.setLastTurnUpdated(1333901);
    AdventureSpentDatabase.setTurns(location.getAdventureName(), 801146);

    FightRequest fight = FightRequest.INSTANCE;
    GenericRequest choice = ChoiceManager.CHOICE_HANDLER;

    // adventure.php?snarfblat=343
    // redirect -> fight.php?ireallymeanit=1653112277
    String urlString = "fight.php?ireallymeanit=1653163461";
    String responseText = html("request/test_adventures_spent_office_1.html");
    FightRequest.preFight(true);
    FightRequest.registerRequest(false, urlString);
    fight.responseText = responseText;
    fight.setHasResult(true);
    fight.processResponse();
    assertEquals(1, FightRequest.currentRound);
    assertEquals(801146, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1333901, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_office_2.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(1333901, KoLCharacter.getTurnsPlayed());
    assertEquals(1333901, KoLCharacter.getCurrentRun());
    assertEquals(801146, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1333901, AdventureSpentDatabase.getLastTurnUpdated());

    urlString =
        "fight.php?action=macro&macrotext=pickpocket%0Aif+hasskill+6032%0Askill+6032%0Aendif%0Amark+mafiafinal%0Aattack%0Agoto+mafiafinal";
    responseText = html("request/test_adventures_spent_office_3.html");
    FightRequest.registerRequest(true, urlString);
    fight.responseText = responseText;
    fight.setHasResult(true);
    fight.processResponse();
    assertFalse(KoLCharacter.inFight());
    assertEquals(801147, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1333901, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_office_4.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(1333902, KoLCharacter.getTurnsPlayed());
    assertEquals(1333902, KoLCharacter.getCurrentRun());
    assertEquals(801147, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1333902, AdventureSpentDatabase.getLastTurnUpdated());

    // adventure.php?snarfblat=343
    // redirect -> choice.php?forceoption=0
    urlString = "choice.php?forceoption=0";
    responseText = html("request/test_adventures_spent_office_5.html");
    choice.responseText = responseText;
    choice.setHasResult(true);
    ChoiceManager.preChoice(choice);
    choice.processResponse();
    assertTrue(ChoiceManager.handlingChoice);
    assertEquals(ChoiceManager.lastChoice, 786);
    assertEquals(ChoiceManager.lastDecision, 0);

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_office_6.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(1333902, KoLCharacter.getTurnsPlayed());
    assertEquals(1333902, KoLCharacter.getCurrentRun());
    assertEquals(801147, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(1333902, AdventureSpentDatabase.getLastTurnUpdated());

    // urlString = "choice.php?whichchoice=786&option=3&pwd"
    // This redirects into a fight.
    choice.setHasResult(true);
    ChoiceManager.preChoice(choice);
    urlString = "fight.php?ireallymeanit=1653163462";
    responseText = html("request/test_adventures_spent_office_7.html");
    FightRequest.preFight(true);
    FightRequest.registerRequest(true, urlString);
    fight.responseText = responseText;
    fight.setHasResult(true);
    fight.processResponse();
    assertEquals(1, FightRequest.currentRound);
    assertEquals(801147, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1333902, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_office_8.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(1333902, KoLCharacter.getTurnsPlayed());
    assertEquals(1333902, KoLCharacter.getCurrentRun());
    assertEquals(801147, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(1333902, AdventureSpentDatabase.getLastTurnUpdated());

    urlString =
        "fight.php?action=macro&macrotext=pickpocket%0Aif+hasskill+6032%0Askill+6032%0Aendif%0Amark+mafiafinal%0Aattack%0Agoto+mafiafinal";
    responseText = html("request/test_adventures_spent_office_9.html");
    FightRequest.registerRequest(true, urlString);
    fight.responseText = responseText;
    fight.setHasResult(true);
    fight.processResponse();
    assertFalse(KoLCharacter.inFight());
    assertEquals(801148, AdventureSpentDatabase.getTurns(location, true));
    assertEquals(1333902, AdventureSpentDatabase.getLastTurnUpdated());

    urlString = "api.php?what=status&for=KoLmafia";
    responseText = html("request/test_adventures_spent_office_10.json");
    ApiRequest.parseResponse(urlString, responseText);
    assertEquals(1333903, KoLCharacter.getTurnsPlayed());
    assertEquals(1333903, KoLCharacter.getCurrentRun());
    assertEquals(801148, AdventureSpentDatabase.getTurns(location, true));
    assertFalse(AdventureSpentDatabase.getNoncombatEncountered());
    assertEquals(1333903, AdventureSpentDatabase.getLastTurnUpdated());
  }
}
