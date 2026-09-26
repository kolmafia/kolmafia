package net.sourceforge.kolmafia.request.concoction;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withFamiliar;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class GnomePartRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("GnomePartRequestTest");
    Preferences.reset("GnomePartRequestTest");
  }

  @Test
  void canMakePart() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.REAGNIMATED_GNOME), withProperty("_gnomePart", false));

    try (cleanups) {
      var canMake = GnomePartRequest.canMake(ConcoctionPool.get(ItemPool.GNOMISH_EAR));
      assertThat(canMake, is(1));
    }
  }

  @Test
  void cannotMakePartWithoutReagnimatedGnome() {
    var cleanups = new Cleanups(withProperty("_gnomePart", false));

    try (cleanups) {
      var canMake = GnomePartRequest.canMake(ConcoctionPool.get(ItemPool.GNOMISH_EAR));
      assertThat(canMake, is(0));
    }
  }

  @Test
  void cannotMakeSecondPart() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.REAGNIMATED_GNOME), withProperty("_gnomePart", true));

    try (cleanups) {
      var canMake = GnomePartRequest.canMake(ConcoctionPool.get(ItemPool.GNOMISH_EAR));
      assertThat(canMake, is(0));
    }
  }

  @Test
  void cannotMakeInvalidPart() {
    var cleanups =
        new Cleanups(
            withFamiliar(FamiliarPool.REAGNIMATED_GNOME), withProperty("_gnomePart", false));

    try (cleanups) {
      var canMake = GnomePartRequest.canMake(ConcoctionPool.get(ItemPool.SEAL_CLUB));
      assertThat(canMake, is(0));
    }
  }

  @Test
  void createsAKgnee() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withFamiliarInTerrarium(FamiliarPool.REAGNIMATED_GNOME),
            withFamiliar(FamiliarPool.CARNIE),
            withProperty("_gnomePart", false));

    try (cleanups) {
      builder.client.addResponse(new FakeHttpResponse<>(200, "You take"));
      builder.client.addResponse(
          new FakeHttpResponse<>(200, html("request/test_choice_pick_a_part.html")));
      builder.client.addResponse(
          new FakeHttpResponse<>(200, html("request/test_choice_pick_a_part_picked.html")));
      builder.client.addResponse(new FakeHttpResponse<>(200, ""));
      builder.client.addResponse(new FakeHttpResponse<>(200, "You take"));

      var request = new GnomePartRequest(ConcoctionPool.get(ItemPool.GNOMISH_EAR));
      request.run();

      var requests = client.getRequests();
      assertThat(requests, hasSize(5));
      assertPostRequest(
          requests.get(0),
          "/familiar.php",
          "action=newfam&newfam=" + FamiliarPool.REAGNIMATED_GNOME + "&ajax=1");
      assertGetRequest(requests.get(1), "/arena.php", null);
      assertPostRequest(requests.get(2), "/choice.php", "whichchoice=597&option=1");
      assertPostRequest(requests.get(3), "/api.php", "what=status&for=KoLmafia");
      assertPostRequest(
          requests.get(4),
          "/familiar.php",
          "action=newfam&newfam=" + FamiliarPool.CARNIE + "&ajax=1");
    }
  }
}
