package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withDataFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AdventureQueueDatabaseTest {
  private static final String USER_NAME = "AdventureQueueDatabaseTestUser";

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset(USER_NAME);
    AdventureQueueDatabase.allowSerializationWrite = false;
  }

  @AfterAll
  static void afterAll() {
    AdventureQueueDatabase.allowSerializationWrite = true;
  }

  @Test
  void resolvesSerializedClassesAgainstRunningPackage() {
    var cleanups =
        new Cleanups(withDataFile("serialized_queue.ser", USER_NAME.toLowerCase() + "_queue.ser"));

    try (cleanups) {
      AdventureQueueDatabase.deserialize();

      assertThat(
          AdventureQueueDatabase.getZoneQueue("The Smut Orc Logging Camp"),
          contains("smut orc jacker", "smut orc nailer"));
      assertThat(
          AdventureQueueDatabase.getZoneNoncombatQueue("The Smut Orc Logging Camp"),
          contains("Fire Up Above"));
    }
  }
}
