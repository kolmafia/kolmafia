package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHermitReset;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class HermitRequestTest {

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("Hermit");
  }

  @Test
  public void testCloverCountsAreKnown() {
    var cleanups =
        new Cleanups(
            withHermitReset(),
            withNextResponse(200, html("request/test_track_clover_visit_hermit.html")));

    try (cleanups) {
      var request = new HermitRequest();
      request.run();

      assertEquals(3, HermitRequest.cloverCount());
    }
  }

  @Test
  public void testThatCloverPurchasesAreTracked() {
    var cleanups =
        new Cleanups(
            withHermitReset(),
            withProperty("_cloversPurchased", 0),
            withItem(ItemPool.WORTHLESS_TRINKET, 3),
            withItem(ItemPool.ELEVEN_LEAF_CLOVER, 0),
            withNextResponse(200, html("request/test_track_clover_purchase_hermit.html")));

    try (cleanups) {
      var request = new GenericRequest("hermit.php?action=trade&quantity=3&whichitem=10881");
      request.run();

      assertThat("_cloversPurchased", isSetTo(3));
      assertEquals(3, ItemPool.get(ItemPool.ELEVEN_LEAF_CLOVER).getCount(KoLConstants.inventory));
      assertEquals(0, ItemPool.get(ItemPool.WORTHLESS_TRINKET).getCount(KoLConstants.inventory));
    }
  }
}
