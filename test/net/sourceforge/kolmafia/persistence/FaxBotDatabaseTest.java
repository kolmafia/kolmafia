package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withNextResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.MonsterData;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Nested;

import java.util.List;
import java.util.Map;

class FaxBotDatabaseTest {
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
      String name = "angry bugbear";
      String attributes = "";
      int id = 256;
      String[] images = {"angbugbear.gif"};
      MonsterData monster = new MonsterData(name, id, images, attributes);
      boolean result = faxBot.request(monster);
      assertTrue(result);
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
