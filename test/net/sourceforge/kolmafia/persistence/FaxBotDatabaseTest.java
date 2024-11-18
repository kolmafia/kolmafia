package net.sourceforge.kolmafia.persistence;

import static internal.helpers.Player.withNextResponse;

import internal.helpers.Cleanups;
import org.junit.jupiter.api.Test;

class FaxBotDatabaseTest {

  @Test
  public void configureFax() {
    var cleanups = new Cleanups(withNextResponse(200, "xyzzy"));
    try (cleanups) {
      FaxBotDatabase.configure();
    }
  }
}
