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

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import org.junit.jupiter.api.Test;

class SkeletonOfCrimboPastRequestTest {
  @Test
  void isAccessibleWhenWeOwnFamiliar() {
    try (var _ = withFamiliarInTerrarium(FamiliarPool.SKELETON_OF_CRIMBO_PAST)) {
      assertThat(SkeletonOfCrimboPastRequest.accessible(), is(nullValue()));
    }
  }

  @Test
  void inaccessibleWhenNoFamiliar() {
    try (var _ = withoutFamiliarInTerrarium(FamiliarPool.SKELETON_OF_CRIMBO_PAST)) {
      assertThat(SkeletonOfCrimboPastRequest.accessible(), is(notNullValue()));
    }
  }

  @Test
  void initiatesShopAccess() {
    var builder = new FakeHttpClientBuilder();
    try (var _ = withHttpClientBuilder(builder)) {
      SkeletonOfCrimboPastRequest.equip();
      var requests = builder.client.getRequests();
      assertThat(requests.size(), is(1));
      assertGetRequest(requests.getFirst(), "/main.php", "talktosocp=1");
    }
  }

  @Test
  void exitsTheShopChoice() {
    var builder = new FakeHttpClientBuilder();
    try (var _ = withHttpClientBuilder(builder)) {
      SkeletonOfCrimboPastRequest.unequip();
      var requests = builder.client.getRequests();
      assertThat(requests.size(), is(1));
      assertPostRequest(requests.getFirst(), "/choice.php", "whichchoice=1567&option=5");
    }
  }

  @Test
  void canBuyGruelWithBuyFunction() {
    var builder = new FakeHttpClientBuilder();
    try (var _ =
        new Cleanups(
            withHttpClientBuilder(builder),
            withFamiliarInTerrarium(FamiliarPool.SKELETON_OF_CRIMBO_PAST))) {
      CoinMasterRequest.buy(
          SkeletonOfCrimboPastRequest.SKELETON_OF_CRIMBO_PAST,
          ItemPool.get(ItemPool.MEDICAL_GRUEL));
      var requests = builder.client.getRequests();
      assertThat(requests.size(), is(3));
      assertGetRequest(requests.getFirst(), "/main.php", "talktosocp=1");
      assertPostRequest(requests.get(1), "/choice.php", "whichchoice=1567&option=3");
      assertPostRequest(requests.get(2), "/choice.php", "whichchoice=1567&option=5");
    }
  }
}
