package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withEmptyCampground;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withWorkshedItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AsdonMartinCommandTest extends AbstractCommandTestBase {

  public AsdonMartinCommandTest() {
    this.command = "asdonmartin";
  }

  @BeforeEach
  public void initializeState() {
    HttpClientWrapper.setupFakeClient();
    StaticEntity.setContinuationState(KoLConstants.MafiaState.CONTINUE);
  }

  @Test
  void failsIfNoWorkshed() {
    String output = execute("");

    assertErrorState();
    assertThat(output, containsString("You do not have an Asdon Martin"));
  }

  @Test
  void failsifNotAsdonMartin() {
    var cleanups = withWorkshedItem(ItemPool.DNA_LAB);

    try (cleanups) {
      String output = execute("");
      assertErrorState();
      assertThat(output, containsString("You do not have an Asdon Martin"));
    }
  }

  @Test
  void providesUsageIfNoParameters() {
    var cleanups = withWorkshedItem(ItemPool.ASDON_MARTIN);

    try (cleanups) {
      String output = execute("");
      assertThat(
          output,
          containsString(
              "Usage: asdonmartin drive style|clear, fuel [#] item name  - Get drive buff or convert items to fuel"));
    }
  }

  @Test
  void providesUsageIfDriveWithNoEffect() {
    var cleanups = withWorkshedItem(ItemPool.ASDON_MARTIN);

    try (cleanups) {
      String output = execute("drive");
      assertThat(
          output,
          containsString(
              "Usage: asdonmartin drive style|clear, fuel [#] item name  - Get drive buff or convert items to fuel"));
    }
  }

  @Test
  void driveClearErrorsIfNoStyle() {
    var cleanups = withWorkshedItem(ItemPool.ASDON_MARTIN);

    try (cleanups) {
      String output = execute("drive clear");
      assertErrorState();
      assertThat(output, containsString("You do not have a driving style"));
    }
  }

  @Test
  void driveClearClearsStyle() {
    var cleanups =
        new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), withEffect("Driving Obnoxiously"));

    try (cleanups) {
      execute("drive clear");

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          requests.get(0), "/campground.php", "preaction=undrive&stop=Stop+Driving+Obnoxiously");
    }
  }

  @Test
  void driveUnrecognisedErrors() {
    var cleanups = new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN));

    try (cleanups) {
      String output = execute("drive dangerously");
      assertErrorState();
      assertThat(output, containsString("Driving style dangerously not recognised"));
    }
  }

  @Test
  void driveNoFuelErrors() {
    var cleanups = new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN));

    try (cleanups) {
      String output = execute("drive obnoxiously");
      assertThat(output, containsString("You haven't got enough fuel"));
    }
  }

  @Test
  void driveNoEffectsAdds() {
    var builder = new FakeHttpClientBuilder();

    builder.client.addResponse(200, html("request/test_campground_drive_observantly.html"));

    var cleanups =
        new Cleanups(
            withWorkshedItem(ItemPool.ASDON_MARTIN),
            withFuel(1558),
            withHttpClientBuilder(builder));

    try (cleanups) {
      execute("drive observantly");

      var requests = builder.client.getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(requests.get(0), "/campground.php", "preaction=drive&whichdrive=7");
      assertThat(CampgroundRequest.getFuel(), is(1521));
    }

    KoLConstants.activeEffects.clear();
  }

  @Test
  void driveSameEffectExtends() {
    var cleanups =
        new Cleanups(
            withWorkshedItem(ItemPool.ASDON_MARTIN), withEffect("Driving Obnoxiously"), withFuel());

    try (cleanups) {
      execute("drive obnoxiously");

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          requests.get(0),
          "/campground.php",
          "preaction=drive&whichdrive=0&more=Drive+More+Obnoxiously");
    }
  }

  @Test
  void driveNewEffectRemovesAndAdds() {
    var cleanups =
        new Cleanups(
            withWorkshedItem(ItemPool.ASDON_MARTIN), withEffect("Driving Obnoxiously"), withFuel());

    try (cleanups) {
      execute("drive observantly");

      var requests = getRequests();

      assertThat(requests, hasSize(2));
      assertPostRequest(
          requests.get(0), "/campground.php", "preaction=undrive&stop=Stop+Driving+Obnoxiously");
      assertPostRequest(requests.get(1), "/campground.php", "preaction=drive&whichdrive=7");
    }
  }

  @Test
  void fuelInvalidErrors() {
    var cleanups = new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN));

    try (cleanups) {
      String output = execute("fuel foobar");
      assertErrorState();
      assertThat(output, containsString("foobar cannot be used as fuel"));
    }
  }

  @Test
  void fuelAbsentErrors() {
    var cleanups = new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN));

    try (cleanups) {
      String output = execute("fuel 10 soda bread");
      assertErrorState();
      assertThat(output, containsString("You don't have enough loaf of soda bread"));
    }
  }

  @Test
  void fuelValidSendsRequest() {
    var builder = new FakeHttpClientBuilder();

    builder.client.addResponse(200, html("request/test_campground_fuel_asdon.html"));

    var cleanups =
        new Cleanups(
            withEmptyCampground(),
            withHttpClientBuilder(builder),
            withWorkshedItem(ItemPool.ASDON_MARTIN),
            withFuel(136),
            withItem("pie man was not meant to eat", 1));

    try (cleanups) {
      execute("fuel 1 pie man was not meant to eat");

      var requests = builder.client.getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(requests.get(0), "/campground.php", "action=fuelconvertor&qty=1&iid=7372");

      assertThat(CampgroundRequest.getFuel(), is(275));
    }
  }

  @Test
  void fuelZeroDoesNotSendRequest() {
    var cleanups =
        new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), withItem("loaf of soda bread", 10));

    try (cleanups) {
      execute("fuel 0 soda bread");

      var requests = getRequests();

      assertThat(requests, empty());
    }
  }

  private Cleanups withFuel() {
    return withFuel(37);
  }

  private Cleanups withFuel(final int fuel) {
    var old = CampgroundRequest.getFuel();
    CampgroundRequest.setFuel(fuel);
    return new Cleanups(() -> CampgroundRequest.setFuel(old));
  }
}
