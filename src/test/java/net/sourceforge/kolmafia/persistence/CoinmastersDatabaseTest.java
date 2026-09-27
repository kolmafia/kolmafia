package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withQuestProgress;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.QuestDatabase.Quest;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CoinmastersDatabaseTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("CoinmastersDatabaseTest");
  }

  @BeforeAll
  static void beforeEach() {
    Preferences.reset("CoinmastersDatabaseTest");
  }

  @Nested
  class Contains {
    static final AdventureResult WRECKED_GENERATOR = ItemPool.get("Wrecked Generator", 1);

    @Test
    public void containsWithoutValidationWorks() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GENERATOR, QuestDatabase.UNSTARTED),
              withEffect("Transpondent", 0));
      int itemId = WRECKED_GENERATOR.getItemId();
      try (cleanups) {
        assertTrue(CoinmastersDatabase.contains(itemId, false));
        assertFalse(CoinmastersDatabase.contains(itemId, true));
      }
    }

    @Test
    public void containsWithValidationWorks() {
      var cleanups =
          new Cleanups(
              withQuestProgress(Quest.GENERATOR, QuestDatabase.FINISHED),
              withEffect("Transpondent", 20));
      int itemId = WRECKED_GENERATOR.getItemId();
      try (cleanups) {
        assertTrue(CoinmastersDatabase.contains(itemId, false));
        assertTrue(CoinmastersDatabase.contains(itemId, true));
      }
    }
  }
}
