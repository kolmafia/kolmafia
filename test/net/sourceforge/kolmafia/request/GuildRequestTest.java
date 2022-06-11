package net.sourceforge.kolmafia.request;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
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

  static String loadHTMLResponse(String path) throws IOException {
    // Load the responseText from saved HTML file
    return Files.readString(Paths.get(path)).trim();
  }

  @Test
  public void canTrackWizardOfEgoQuest() throws IOException {
    // First talk with "ocg"
    GuildRequest request = new GuildRequest("ocg");
    String responseText = loadHTMLResponse("request/test_guild_quest_ego_intro.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), QuestDatabase.UNSTARTED);

    // Second talk with "ocg"
    request = new GuildRequest("ocg");
    responseText = loadHTMLResponse("request/test_guild_quest_ego_started.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), QuestDatabase.STARTED);

    // Talk after turning in key and getting next step
    AdventureResult key = ItemPool.get(ItemPool.FERNSWARTHYS_KEY);
    request = new GuildRequest("ocg");
    responseText = loadHTMLResponse("request/test_guild_quest_ego_step2.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), "step2");
    assertTrue(KoLConstants.inventory.contains(key));

    // Talk after finding dusty old book
    AdventureResult dusty = ItemPool.get(ItemPool.DUSTY_BOOK);
    AdventureResult manual = ItemPool.get(ItemPool.MOX_MANUAL);
    request = new GuildRequest("ocg");
    responseText = loadHTMLResponse("request/test_guild_quest_ego_finished.html");
    request.responseText = responseText;
    request.setHasResult(true);
    request.processResponse();
    assertEquals(QuestDatabase.getQuest(Quest.EGO), QuestDatabase.FINISHED);
    assertFalse(KoLConstants.inventory.contains(key));
    assertFalse(KoLConstants.inventory.contains(dusty));
    assertTrue(KoLConstants.inventory.contains(manual));
  }
}
