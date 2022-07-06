package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Player;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class GuildRequestTest {

  @BeforeAll
  private static void beforeAll() {
    KoLCharacter.reset("GuildRequestTest");
    Preferences.reset("GuildRequestTest");
    Preferences.saveSettingsToFile = false;
  }

  @BeforeEach
  protected void beforeEach() {
    Preferences.reset("GuildRequestTest");
  }

  @AfterEach
  protected void afterEach() {
    KoLConstants.inventory.clear();
  }

  @AfterAll
  private static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  @Test
  public void canTrackWizardOfEgoQuest() {
    // First talk with "ocg"
    GuildRequest request = new GuildRequest("ocg");
    String responseText = html("request/test_guild_quest_ego_intro.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), QuestDatabase.UNSTARTED);

    // Second talk with "ocg"
    request = new GuildRequest("ocg");
    responseText = html("request/test_guild_quest_ego_started.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), QuestDatabase.STARTED);

    // Talk after turning in key and getting next step
    AdventureResult key = ItemPool.get(ItemPool.FERNSWARTHYS_KEY);
    request = new GuildRequest("ocg");
    responseText = html("request/test_guild_quest_ego_step2.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), "step2");
    assertTrue(KoLConstants.inventory.contains(key));

    // Talk after finding dusty old book
    AdventureResult dusty = ItemPool.get(ItemPool.DUSTY_BOOK);
    AdventureResult manual = ItemPool.get(ItemPool.MOX_MANUAL);
    request = new GuildRequest("ocg");
    responseText = html("request/test_guild_quest_ego_finished.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), QuestDatabase.FINISHED);
    assertFalse(KoLConstants.inventory.contains(key));
    assertFalse(KoLConstants.inventory.contains(dusty));
    assertTrue(KoLConstants.inventory.contains(manual));
  }

  @Test
  public void canDetectWhiteCitadelQuestStarted() {
    // You have completed the Meatcar and visited "paco",
    // You visit "paco" again and they give the White Citadel Quest.
    //
    // We want to parse his response and start the quest
    //
    // Requesting: guild.php?place=paco
    // Field: location = [choice.php?forceoption=0]
    // Requesting: choice.php?forceoption=0
    // Requesting: choice.php?pwd&whichchoice=930&option=1

    var cleanups = Player.setProperty("questG02Whitecastle", "unstarted");

    try (cleanups) {
      // talk with "ocg"
      GenericRequest request = new GenericRequest("choice.php?forceoption=0");
      String responseText = html("request/test_guild_quest_citadel_started_0.html");
      request.responseText = responseText;
      request.setHasResult(true);
      ChoiceManager.preChoice(request);
      request.processResponse();
      assertEquals(QuestDatabase.UNSTARTED, QuestDatabase.getQuest(Quest.CITADEL));

      request = new GenericRequest("choice.php?pwd&whichchoice=930&option=1");
      responseText = html("request/test_guild_quest_citadel_started_1.html");
      request.responseText = responseText;
      request.setHasResult(true);
      ChoiceManager.preChoice(request);
      request.processResponse();
      assertEquals(QuestDatabase.STARTED, QuestDatabase.getQuest(Quest.CITADEL));
    }
  }

  @Test
  public void canDetectWhiteCitadelQuestFinished() {
    // You have visited White Citadel and been given a White Citadel Satisfaction Satchel
    // Quest.CITADEL is at "step10"
    // You return to the guild and speak to "paco"
    // They take your satchel and give you a lucky rabbit's foot.
    //
    // White Citadel Satisfaction Satchel -> removed from inventory
    // Quest.CITADEL is at QuestDatabase.FINISHED
    //
    // Requesting: guild.php?place=paco
    // Field: location = [choice.php?forceoption=0]
    // Requesting: choice.php?forceoption=0

    var cleanups = Player.addItem(ItemPool.CITADEL_SATCHEL);

    try (cleanups) {
      // talk with "ocg"
      GenericRequest request = new GenericRequest("choice.php?forceoption=0");
      String responseText = html("request/test_guild_quest_citadel_finished.html");
      request.responseText = responseText;
      request.setHasResult(true);
      ChoiceManager.preChoice(request);
      request.processResponse();
      assertEquals(1, InventoryManager.getCount(ItemPool.LUCKY_RABBIT_FOOT));
      assertEquals(0, InventoryManager.getCount(ItemPool.CITADEL_SATCHEL));
      assertEquals(QuestDatabase.FINISHED, QuestDatabase.getQuest(Quest.CITADEL));
    }
  }
}
