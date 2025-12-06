package net.sourceforge.kolmafia.request.coinmaster;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withFamiliarInTerrarium;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withoutFamiliarInTerrarium;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import org.junit.jupiter.api.Test;

class SkeletonOfCrimboPastRequestTest {
  @Test
  void isAccessibleWhenWeOwnFamiliar() {
    var cleanups = withFamiliarInTerrarium(FamiliarPool.SKELETON_OF_CRIMBO_PAST);
    try (cleanups) {
      assertThat(SkeletonOfCrimboPastRequest.accessible(), is(nullValue()));
    }
  }

  @Test
  void inaccessibleWhenNoFamiliar() {
    var cleanups = withoutFamiliarInTerrarium(FamiliarPool.SKELETON_OF_CRIMBO_PAST);
    try (cleanups) {
      assertThat(SkeletonOfCrimboPastRequest.accessible(), is(notNullValue()));
    }
  }

  @Test
  void initiatesShopAccess() {
    var builder = new FakeHttpClientBuilder();
    var cleanups = withHttpClientBuilder(builder);
    try (cleanups) {
      SkeletonOfCrimboPastRequest.equip();
      var requests = builder.client.getRequests();
      assertThat(requests.size(), is(1));
      assertGetRequest(requests.getFirst(), "/main.php", "talktosocp=1");
    }
  }

  @Test
  void exitsTheShopChoice() {
    var builder = new FakeHttpClientBuilder();
    var cleanups = withHttpClientBuilder(builder);
    try (cleanups) {
      SkeletonOfCrimboPastRequest.unequip();
      var requests = builder.client.getRequests();
      assertThat(requests.size(), is(1));
      assertPostRequest(requests.getFirst(), "/choice.php", "whichchoice=1567&option=5");
    }
  }
}
