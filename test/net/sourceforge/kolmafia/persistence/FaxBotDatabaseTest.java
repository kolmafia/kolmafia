package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withNextResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.MonsterData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FaxBotDatabaseTest {

  private static MonsterData valieMonsterData =
    new MonsterData("angry cavebugbear", 1183,"", "" );
  @Test
  public void doThingsForCoverage() {
    FaxBotDatabase.reconfigure();
    FaxBotDatabase.FaxBot faxBot = FaxBotDatabase.getFaxbot("OnlyFax");
    assertEquals(faxBot.getName(), "OnlyFax");
    assertEquals(faxBot.getPlayerId(), 3690803);
    List<String> categories = faxBot.getCategories();
    assertEquals(categories.size(), 13);
    assertTrue(categories.contains("Standard"));
    List<LockableListModel<FaxBotDatabase.Monster>> monCat = faxBot.getMonstersByCategory();
    assertEquals(monCat.size(), 13);
  }

  @Test
  public void makeARequestWithMonsterData() {
    FaxBotDatabase.reconfigure();
    FaxBotDatabase.FaxBot faxBot = FaxBotDatabase.getFaxbot("OnlyFax");
    String name = "angry cavebugbear";
    String attributes = "";
    int id = 1183;
    String[] images = {"bb_caveman.gif"};
    MonsterData monster = new MonsterData(name, id, images, attributes);
    // as currently written faxbot.request is expected to fail in the test environment
    // because the faxbot is not online and the test has not yet provided resposes that
    // would indicate otherwise.
    boolean result = faxBot.request(monster);
    assertFalse(result);
  }

  @Test
  public void coverageForInvalidFaxbot() {
    FaxBotDatabase.FaxBot faxBot = new FaxBotDatabase.FaxBot(null, "notReal");
    String name = "angry cavebugbear";
    String attributes = "";
    int id = 1183;
    String[] images = {"bb_caveman.gif"};
    MonsterData monster = new MonsterData(name, id, images, attributes);
    boolean result = faxBot.request(monster);
    assertFalse(result);
  }

  @Disabled
  @Test
  public void configureFax() {
    var cleanups = new Cleanups(withNextResponse(200, "xyzzy"));
    try (cleanups) {
      FaxBotDatabase.configure();
    }
  }
}
