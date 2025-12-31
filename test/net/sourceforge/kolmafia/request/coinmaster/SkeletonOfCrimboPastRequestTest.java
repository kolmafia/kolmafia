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
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.objectpool.ConcoctionPool;
import net.sourceforge.kolmafia.objectpool.FamiliarPool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SkeletonOfCrimboPastRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("SkeletonOfCrimboPastRequestTest");
    Preferences.reset("SkeletonOfCrimboPastRequestTest");
  }

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

  @Test
  public void canRegisterDailySpecial() {
    // Daily Special: <item description link> (10 knucklebones)
    String responseText =
        "Daily Special: <a class=\"nounder\" href=\"descitem(352309634)\"><b>rethinking cnady</b></a> (10 knucklebones)";

    // Visit the skeleton with the response text
    SkeletonOfCrimboPastRequest.visit(responseText);

    // Verify preferences are set correctly
    assertThat(
        Preferences.getInteger("_crimboPastDailySpecialItem"), is(ItemPool.RETHINKING_CANDY_BOOK));
    assertThat(Preferences.getInteger("_crimboPastDailySpecialPrice"), is(10));
    assertThat(
        Preferences.getBoolean("_crimboPastDailySpecial"),
        is(false)); // false means "not yet bought"
  }

  @Test
  public void dailySpecialPopulatesInConcoctionPool() {
    // Item 10883 is "Crimbo snack mix" (descid 957545937)
    String responseText =
        "Daily Special: <a class=\"nounder\" href=\"descitem(352309634)\"><b>rethinking candy</b></a> (10 knucklebones)";

    SkeletonOfCrimboPastRequest.visit(responseText);

    // Verify it is in the ConcoctionPool
    var concoction = ConcoctionPool.get(ItemPool.RETHINKING_CANDY_BOOK);
    assertThat(concoction, notNullValue());
    assertThat(concoction.getPurchaseRequest(), notNullValue());
    assertThat(
        concoction.getPurchaseRequest().getShopName(),
        is(SkeletonOfCrimboPastRequest.SKELETON_OF_CRIMBO_PAST.toString()));
    assertThat("Has more than 1 ingredient", concoction.getIngredients().length, is(1));
  }
}
