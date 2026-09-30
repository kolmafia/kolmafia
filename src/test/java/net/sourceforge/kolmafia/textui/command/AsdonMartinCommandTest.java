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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
import internal.helpers.Networking;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.request.CampgroundRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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
              "Usage: asdonmartin drive style [times]|clear, fuel [#] item name  - Get drive buff or convert items to fuel"));
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
              "Usage: asdonmartin drive style [times]|clear, fuel [#] item name  - Get drive buff or convert items to fuel"));
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
  void driveSameEffectMultipleTimesExtends() {
    var builder = new FakeHttpClientBuilder();

    builder.client.addResponse(200, html("request/test_campground_drive_more_observantly.html"));

    var cleanups =
        new Cleanups(
            withWorkshedItem(ItemPool.ASDON_MARTIN),
            withEffect("Driving Observantly"),
            withFuel(522),
            withHttpClientBuilder(builder));

    try (cleanups) {
      execute("drive observantly 2");

      var requests = builder.client.getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          requests.get(0),
          "/campground.php",
          "preaction=drive&whichdrive=7&more=Drive+More+Observantly&drivetimes=2");
      assertThat(CampgroundRequest.getFuel(), is(448));
    }
  }

  @Test
  void driveNewEffectMultipleTimesAdds() {
    var cleanups = new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), withFuel(111));

    try (cleanups) {
      execute("drive observantly 3");

      var requests = getRequests();

      assertThat(requests, hasSize(1));
      assertPostRequest(
          requests.get(0), "/campground.php", "preaction=drive&whichdrive=7&drivetimes=3");
    }
  }

  @Test
  void driveNewEffectMultipleTimesRemovesAndAdds() {
    var builder = new FakeHttpClientBuilder();

    builder.client.addResponse(200, html("request/test_campground_asdon_not_driving.html"));

    var cleanups =
        new Cleanups(
            withWorkshedItem(ItemPool.ASDON_MARTIN),
            withEffect("Driving Obnoxiously"),
            withFuel(200),
            withHttpClientBuilder(builder));

    try (cleanups) {
      execute("drive observantly 2");

      var requests = builder.client.getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(
          requests.get(0), "/campground.php", "preaction=undrive&stop=Stop+Driving+Obnoxiously");
      assertThat(
          requests.stream()
              .filter(r -> r.method().equals("POST"))
              .map(Networking::getPostRequestBody)
              .toList(),
          hasItem("preaction=drive&whichdrive=7&drivetimes=2"));
      assertThat(CampgroundRequest.getFuel(), is(82));
    }
  }

  @Test
  void driveMultipleTimesNotEnoughFuelErrors() {
    var cleanups =
        new Cleanups(
            withWorkshedItem(ItemPool.ASDON_MARTIN),
            withEffect("Driving Observantly"),
            withFuel(73));

    try (cleanups) {
      String output = execute("drive observantly 2");

      assertThat(output, containsString("You haven't got enough fuel"));
      assertThat(getRequests(), empty());
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

  @Test
  void unknownSubcommandProvidesUsage() {
    var cleanups = withWorkshedItem(ItemPool.ASDON_MARTIN);

    try (cleanups) {
      String output = execute("honk");

      assertThat(output, containsString("Usage: asdonmartin drive style [times]|clear"));
      assertThat(getRequests(), empty());
    }
  }

  @Nested
  class DriveOptions {
    @ParameterizedTest
    @CsvSource({
      "obnoxiously, 0",
      "stealthily, 1",
      "wastefully, 2",
      "safely, 3",
      "recklessly, 4",
      "quickly, 5",
      "intimidatingly, 6",
      "observantly, 7",
      "waterproofly, 8",
    })
    void driveSendsStyleId(final String style, final int id) {
      var cleanups = new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), withFuel());

      try (cleanups) {
        execute("drive " + style);

        var requests = getRequests();

        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/campground.php", "preaction=drive&whichdrive=" + id);
      }
    }

    @Test
    void driveStyleIsCaseInsensitive() {
      var cleanups = new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), withFuel());

      try (cleanups) {
        execute("drive OBSERVANTLY");

        var requests = getRequests();

        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/campground.php", "preaction=drive&whichdrive=7");
      }
    }

    @Test
    void driveOnceOmitsDriveTimes() {
      var cleanups = new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), withFuel());

      try (cleanups) {
        execute("drive observantly 1");

        var requests = getRequests();

        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/campground.php", "preaction=drive&whichdrive=7");
      }
    }

    @Test
    void driveMultipleTimesWithExactFuelSucceeds() {
      var cleanups = new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), withFuel(74));

      try (cleanups) {
        execute("drive observantly 2");

        var requests = getRequests();

        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/campground.php", "preaction=drive&whichdrive=7&drivetimes=2");
      }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "lots"})
    void driveInvalidTimesErrors(final String times) {
      var cleanups = new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), withFuel(1000));

      try (cleanups) {
        String output = execute("drive observantly " + times);

        assertErrorState();
        assertThat(output, containsString("Invalid number of times to drive"));
        assertThat(getRequests(), empty());
      }
    }

    @Test
    void driveClearIgnoresFuel() {
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.ASDON_MARTIN),
              withEffect("Driving Waterproofly"),
              withFuel(0));

      try (cleanups) {
        execute("drive clear");

        var requests = getRequests();

        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/campground.php", "preaction=undrive&stop=Stop+Driving+Waterproofly");
      }
    }
  }

  @Nested
  class FuelOptions {
    @Test
    void fuelWithNoItemProvidesUsage() {
      var cleanups = withWorkshedItem(ItemPool.ASDON_MARTIN);

      try (cleanups) {
        String output = execute("fuel");

        assertThat(output, containsString("Usage: asdonmartin drive style [times]|clear"));
        assertThat(getRequests(), empty());
      }
    }

    @Test
    void fuelWithoutCountConvertsOne() {
      var cleanups =
          new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), withItem("loaf of soda bread", 5));

      try (cleanups) {
        execute("fuel soda bread");

        var requests = getRequests();

        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/campground.php", "action=fuelconvertor&qty=1&iid=8195");
      }
    }

    @Test
    void fuelWithCountConvertsThatMany() {
      var cleanups =
          new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), withItem("loaf of soda bread", 5));

      try (cleanups) {
        execute("fuel 3 soda bread");

        var requests = getRequests();

        assertThat(requests, hasSize(1));
        assertPostRequest(
            requests.get(0), "/campground.php", "action=fuelconvertor&qty=3&iid=8195");
      }
    }

    @Test
    void invalidFuelItemErrors() {
      var cleanups =
          new Cleanups(
              withWorkshedItem(ItemPool.ASDON_MARTIN), withItem("chewing gum on a string", 1));

      try (cleanups) {
        String output = execute("fuel chewing gum on a string");

        assertErrorState();
        assertThat(output, containsString("chewing gum on a string cannot be used as fuel"));
        assertThat(getRequests(), empty());
      }
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
