package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withNextResponse;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.sourceforge.kolmafia.RestrictedItemType;
import org.junit.jupiter.api.Test;

public class TrendyRequestTest {

  @Test
  public void shouldCheckTrendyItems() {
    var cleanups = withNextResponse(200, html("request/test_request_trendy_2022.html"));

    try (cleanups) {
      TrendyRequest.initialize();

      assertTrue(TrendyRequest.isTrendy(RestrictedItemType.ITEMS, "seal-clubbing club"));
      assertFalse(TrendyRequest.isTrendy(RestrictedItemType.ITEMS, "card sleeve"));
      assertTrue(TrendyRequest.isTrendy(RestrictedItemType.SKILLS, "Ancient Crymbo Lore"));
      assertTrue(TrendyRequest.isTrendy(RestrictedItemType.SKILLS, "Dead Nostrils"));
      assertFalse(
          TrendyRequest.isTrendy(RestrictedItemType.SKILLS, "Show Your Boring Familiar Pictures"));
    }
  }
}
