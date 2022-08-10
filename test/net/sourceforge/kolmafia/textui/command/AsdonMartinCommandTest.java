package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Player.withEffect;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withWorkshedItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import internal.helpers.Cleanups;
import internal.helpers.HttpClientWrapper;
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
    var cleanups = new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), setFuel());

    try (cleanups) {
      execute("drive obnoxiously");

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(requests.get(0), "/campground.php", "preaction=drive&whichdrive=0");
    }
  }

  @Test
  void driveSameEffectExtends() {
    var cleanups =
        new Cleanups(
            withWorkshedItem(ItemPool.ASDON_MARTIN), withEffect("Driving Obnoxiously"), setFuel());

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
            withWorkshedItem(ItemPool.ASDON_MARTIN), withEffect("Driving Obnoxiously"), setFuel());

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
    var cleanups =
        new Cleanups(withWorkshedItem(ItemPool.ASDON_MARTIN), withItem("loaf of soda bread", 10));

    try (cleanups) {
      execute("fuel 10 soda bread");

      var requests = getRequests();

      assertThat(requests, not(empty()));
      assertPostRequest(requests.get(0), "/campground.php", "action=fuelconvertor&qty=10&iid=8195");
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

  private Cleanups setFuel() {
    CampgroundRequest.setFuel(37);
    return new Cleanups(() -> CampgroundRequest.setFuel(0));
  }
}
