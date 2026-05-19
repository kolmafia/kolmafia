package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withNextResponse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.RestrictedItemType;
import org.junit.jupiter.api.Test;

public class ThriftyRequestTest {

  @Test
  public void shouldCheckThriftyItems() {
    var cleanups = withNextResponse(200, html("request/test_request_thrifty_2026.html"));

    try (cleanups) {
      ThriftyRequest.initialize();

      assertTrue(ThriftyRequest.isAllowed(RestrictedItemType.ITEMS, "seal-clubbing club"));
      assertFalse(ThriftyRequest.isAllowed(RestrictedItemType.ITEMS, "card sleeve"));
      assertTrue(ThriftyRequest.isAllowed(RestrictedItemType.SKILLS, "Pastamastery"));
      assertFalse(ThriftyRequest.isAllowed(RestrictedItemType.SKILLS, "Unaccompanied Miner"));
      assertTrue(ThriftyRequest.isAllowed(RestrictedItemType.FAMILIARS, "Mosquito"));
      assertFalse(ThriftyRequest.isAllowed(RestrictedItemType.FAMILIARS, "Cookbookbat"));
    }
  }
}
