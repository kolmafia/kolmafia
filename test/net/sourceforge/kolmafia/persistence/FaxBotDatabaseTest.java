package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withDataFile;
import static internal.helpers.Player.withNextResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.MonsterData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FaxBotDatabaseTest {

  public static Cleanups globalCleanup;

  @BeforeAll
  public static void beforeAll() {
    globalCleanup =
        new Cleanups(
            withDataFile("cheesefax.xml"),
            withDataFile("easyfax.xml"),
            withDataFile("onlyfax.xml"));
    // Configure
    FaxBotDatabase.reconfigure();
  }

  @AfterAll
  public static void afterAll() {
    globalCleanup.close();
  }

  private static MonsterData validMonsterData =
      new MonsterData("angry cavebugbear", 1183, new String[] {"bb_caveman.gif"}, "");
  private static MonsterData invalidMonsterData =
      new MonsterData("not a real monster", 1183, new String[] {"bb_caveman.gif"}, "");

  @Test
  public void doThingsForCoverage() {
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
    // as currently written faxbot.request is expected to fail in the test environment
    // because the faxbot is not online and the test has not yet provided resposes that
    // would indicate otherwise.
    boolean result = faxBot.request(validMonsterData);
    assertFalse(result);
  }

  @Test
  public void coverageForInvalidFaxbot() {
    FaxBotDatabase.FaxBot faxBot = new FaxBotDatabase.FaxBot(null, "notReal");
    boolean result = faxBot.request(validMonsterData);
    assertFalse(result);
  }

  @Test
  public void coverageForInvalidMonster() {
    FaxBotDatabase.FaxBot faxBot = FaxBotDatabase.getFaxbot("OnlyFax");
    boolean result = faxBot.request(invalidMonsterData);
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
