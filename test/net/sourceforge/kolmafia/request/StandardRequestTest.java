package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withRestricted;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.FamiliarData;
import net.sourceforge.kolmafia.RestrictedItemType;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import org.junit.jupiter.api.Test;

public class StandardRequestTest {

  @Test
  public void shouldCheckStandardItems() {
    var cleanups =
        new Cleanups(
            withPasswordHash("hello"),
            withRestricted(true),
            withNextResponse(200, html("request/test_request_standard_2012.html")));

    try (cleanups) {
      StandardRequest.initialize(true);

      assertTrue(StandardRequest.isAllowed(RestrictedItemType.ITEMS, "seal-clubbing club"));
      assertFalse(StandardRequest.isAllowed(RestrictedItemType.ITEMS, "card sleeve"));

      assertFalse(
          StandardRequest.isAllowed(RestrictedItemType.BOOKSHELF_BOOKS, "Tome of Clip Art"));

      assertTrue(StandardRequest.isAllowed(RestrictedItemType.SKILLS, "Pastamastery"));
      assertFalse(StandardRequest.isAllowed(RestrictedItemType.SKILLS, "Unaccompanied Miner"));

      assertFalse(
          StandardRequest.isAllowed(new FamiliarData(FamiliarPool.DANDY_LION, "Patrick", 0)));
    }
  }
}
