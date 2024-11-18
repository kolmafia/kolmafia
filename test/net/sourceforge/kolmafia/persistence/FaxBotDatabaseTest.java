package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withNextResponse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.shadow.com.univocity.parsers.annotations.Nested;

import java.util.List;

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
