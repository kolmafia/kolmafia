package net.sourceforge.kolmafia.persistence;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.HttpClientWrapper.setupFakeClient;
import static internal.helpers.Player.withItem;
import static org.junit.jupiter.api.Assertions.assertFalse;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ResultProcessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ConcoctionDatabaseTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("EatCommandTest");
    Preferences.reset("EatCommandTest");
  }

  @Test
  public void canResolveRecipeItems() {
    var cleanups =
        new Cleanups(
            withItem(ItemPool.MEAT_PASTE, 2),
            withItem(ItemPool.STAFF_OF_FATS),
            withItem(ItemPool.ANCIENT_AMULET),
            withItem(ItemPool.EYE_OF_ED));

    setupFakeClient();

    try (cleanups) {
      ResultProcessor.autoCreate(ItemPool.STAFF_OF_ED);
      var requests = getRequests();
      // Assert that mafia thinks we're good to craft and has made http requests
      assertFalse(requests.isEmpty());
    }
  }
}
