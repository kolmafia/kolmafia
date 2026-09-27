package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withMeatInCloset;
import static internal.helpers.Player.withNextResponse;
import static internal.helpers.Player.withNoItemsInCloset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ClosetRequestTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("ClosetRequestTest");
  }

  @BeforeEach
  void beforeEach() {
    Preferences.reset("ClosetRequestTest");
  }

  @Test
  void shouldRefreshCloset() {
    var cleanups =
        new Cleanups(
            withNoItemsInCloset(),
            withMeatInCloset(0),
            withNextResponse(
                200,
                "{\"closet\":"
                    + html("request/test_api_closet.json")
                    + ",\"status\":"
                    + html("request/test_status2.json")
                    + "}"));

    try (cleanups) {
      ClosetRequest.refresh();

      assertThat(KoLCharacter.getClosetMeat(), is(54321L));
      var cranberries = ItemPool.get(ItemPool.CRANBERRIES);
      assertThat(cranberries.getCount(KoLConstants.closet), is(550));
    }
  }
}
