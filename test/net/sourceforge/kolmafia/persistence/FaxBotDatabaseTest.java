package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withDataFile;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static net.sourceforge.kolmafia.preferences.Preferences.getBoolean;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.MonsterData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FaxBotDatabaseTest {
  public static Cleanups globalCleanup;

  @BeforeEach
  public void beforeEach() {
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

  @Nested
  class CoverageDriven {

    private static final MonsterData validMonsterData =
        new MonsterData("angry cavebugbear", 1183, new String[] {"bb_caveman.gif"}, "");
    private static final MonsterData invalidMonsterData =
        new MonsterData("not a real monster", 1183, new String[] {"bb_caveman.gif"}, "");

    @Test
    public void doThingsForCoverage() {
      FaxBotDatabase.FaxBot faxBot = FaxBotDatabase.getFaxbot("OnlyFax");
      assertNotNull(faxBot);
      assertEquals("OnlyFax", faxBot.getName());
      assertEquals(3690803, faxBot.getPlayerId());
      List<String> categories = faxBot.getCategories();
      assertEquals(13, categories.size());
      assertTrue(categories.contains("Standard"));
      List<LockableListModel<FaxBotDatabase.Monster>> monCat = faxBot.getMonstersByCategory();
      assertEquals(13, monCat.size());
      FaxBotDatabase.Monster commandMe = faxBot.getMonsterByCommand("[1183]angry cavebugbear");
      assertEquals("angry cavebugbear", commandMe.getActualName());
    }

    @Test
    public void makeARequestWithMonsterData() {
      FaxBotDatabase.reconfigure();
      FaxBotDatabase.FaxBot faxBot = FaxBotDatabase.getFaxbot("OnlyFax");
      assertNotNull(faxBot);
      // as test is currently written faxbot.request is expected to fail in the test environment
      // because the faxbot is not online and the test has not yet provided responses that
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
      assertNotNull(faxBot);
      boolean result = faxBot.request(invalidMonsterData);
      assertFalse(result);
    }

    @Test
    public void exerciseSomeFaxbotOverridesForCoverage() {
      FaxBotDatabase.FaxBot nullBot = null;
      FaxBotDatabase.FaxBot botA = new FaxBotDatabase.FaxBot("A Bot", 1999);
      FaxBotDatabase.FaxBot botAlsoA = new FaxBotDatabase.FaxBot("A Bot", 1999);
      FaxBotDatabase.FaxBot botB = new FaxBotDatabase.FaxBot("B Bot", 1999);
      FaxBotDatabase.FaxBot botInvalid = new FaxBotDatabase.FaxBot(null, 1999);
      assertTrue(botA.equals(botA));
      assertTrue(botA.equals(botAlsoA));
      assertFalse(botA.equals(nullBot));
      assertFalse(botA.equals(botB));
      assertFalse(botA.equals(validMonsterData));
      assertFalse(botA.equals(botInvalid));
      assertNotEquals(0, botA.hashCode());
      assertEquals(0, botInvalid.hashCode());
      assertEquals(-1, botA.compareTo(nullBot));
      assertEquals(0, botA.compareTo(botA));
      assertEquals(0, botA.compareTo(botAlsoA));
      assertEquals(-1, botA.compareTo(botB));
      assertEquals(1, botB.compareTo(botA));
    }

    @Test
    public void exerciseSomeMonsterMethodsForCoverage() {
      FaxBotDatabase.FaxBot faxBot = FaxBotDatabase.getFaxbot("OnlyFax");
      assertNotNull(faxBot);
      FaxBotDatabase.Monster aMonster = faxBot.getMonsterByCommand("[1183]angry cavebugbear");
      assertEquals("angry cavebugbear", aMonster.getName());
      assertEquals("angry cavebugbear", aMonster.getActualName());
      assertEquals("[1183]angry cavebugbear", aMonster.getCommand());
      assertEquals("Unwishable", aMonster.getCategory());
      assertEquals("angry cavebugbear [[1183]angry cavebugbear]", aMonster.toString());
      assertEquals("angry cavebugbear [[1183]angry cavebugbear]", aMonster.toLowerCaseString());
    }

    @Test
    public void exerciseSomeMonsterOverridesForCoverage() {
      FaxBotDatabase.FaxBot faxBot = FaxBotDatabase.getFaxbot("OnlyFax");
      FaxBotDatabase.Monster aMonster = faxBot.getMonsterByCommand("[1183]angry cavebugbear");
      FaxBotDatabase.Monster bMonster =
          faxBot.getMonsterByCommand("[399]animated nightstand (mahogany combat)");
      assertTrue(aMonster.equals(aMonster));
      assertFalse(aMonster.equals(bMonster));
      assertFalse(aMonster.equals(validMonsterData));
      assertFalse(aMonster.equals(null));
      assertEquals(aMonster.hashCode(), aMonster.hashCode());
      assertNotEquals(aMonster.hashCode(), bMonster.hashCode());
      assertEquals(-1, aMonster.compareTo(null));
      assertEquals(0, aMonster.compareTo(aMonster));
      // note that the underlying compareToIgnoreCase returns an index of lexicographical
      // significance and not a value normalized to +/- 1 (or 0)
      assertEquals(-2, aMonster.compareTo(bMonster));
      assertEquals(2, bMonster.compareTo(aMonster));
    }
  }

  @Nested
  class faxChangedPreference {

    static final String response =
        """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <faxbot>
                    \t<botdata>
                    \t\t<name>OnlyFax</name>
                    \t\t<playerid>3690803</playerid>
                    \t</botdata>
                    \t<monsterlist>
                    \t\t<monsterdata>
                    \t\t\t<name>anesthesiologist bugbear</name>
                    \t\t\t<actual_name>anesthesiologist bugbear</actual_name>
                    \t\t\t<command>[1176]anesthesiologist bugbear</command>
                    \t\t\t<category>Unwishable</category>
                    \t\t</monsterdata>\t</monsterlist>
                    </faxbot>""";

    @Disabled("Resetting client needs support for fake data.")
    @Test
    public void checkPreferenceWhenNoFiles() {
      String property = "_faxDataChanged";
      // FaxRequestFrame does a static initialization which means the files are present when the
      // test starts.  Delete them since a missing file and one from a fake response will be
      // different.
      globalCleanup.close();
      FaxBotDatabase.resetInitialization();
      var cleanups = new Cleanups(withNextResponse(200, response), withProperty(property, false));
      try (cleanups) {
        assertFalse(getBoolean(property));
        FaxBotDatabase.configure();
        assertTrue(getBoolean(property));
      }
    }

    @Disabled("Resetting client needs support for fake data.")
    @Test
    public void checkPreferenceWhenFiles() {
      String property = "_faxDataChanged";
      FaxBotDatabase.resetInitialization();
      var cleanups = new Cleanups(withNextResponse(200, ""), withProperty(property, false));
      try (cleanups) {
        assertFalse(getBoolean(property));
        FaxBotDatabase.configure();
        assertTrue(getBoolean(property));
      }
    }
  }
}
