package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.html;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withMeatInCloset;
import static internal.helpers.Player.withNoItemsInCloset;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLCharacter.Gender;
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
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;

    var cleanups =
        new Cleanups(
            withNoItemsInCloset(),
            withMeatInCloset(0),
            withHttpClientBuilder(builder),
            // skip vinyl boots lookup
            withGender(Gender.FEMALE));

    try (cleanups) {
      client.addResponse(200, html("request/test_closet_refresh.html"));
      // response contains charpane.php, so GenericRequest will call api.php?what=status
      client.addResponse(200, html("request/test_status.json"));
      client.addResponse(200, html("request/test_api_closet.json"));

      ClosetRequest.refresh();

      assertThat(KoLCharacter.getClosetMeat(), is(100_000_000L));
      var cranberries = ItemPool.get(ItemPool.CRANBERRIES);
      assertThat(cranberries.getCount(KoLConstants.closet), is(550));
    }
  }
}
