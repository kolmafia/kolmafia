package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.getPostRequestBody;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withMeatInCloset;
import static internal.helpers.Player.withNoItemsInCloset;
import static internal.helpers.Player.withResponses;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.network.FakeHttpResponse;
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
            withResponses(
                r -> {
                  var path = r.uri().getPath();
                  if (path.startsWith("/closet.php")) {
                    return new FakeHttpResponse<>(html("request/test_closet_refresh.html"));
                  }
                  if (path.startsWith("/api.php")) {
                    var body = getPostRequestBody(r);
                    // response contains charpane.php, so GenericRequest will call
                    // api.php?what=status
                    if (body.startsWith("what=status")) {
                      return new FakeHttpResponse<>(html("request/test_status.json"));
                    }
                    if (body.startsWith("what=closet")) {
                      return new FakeHttpResponse<>(html("request/test_api_closet.json"));
                    }
                  }
                  return null;
                }));

    try (cleanups) {
      ClosetRequest.refresh();

      assertThat(KoLCharacter.getClosetMeat(), is(100_000_000L));
      var cranberries = ItemPool.get(ItemPool.CRANBERRIES);
      assertThat(cranberries.getCount(KoLConstants.closet), is(550));
    }
  }
}
