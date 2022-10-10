package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withGender;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withMeat;
import static internal.helpers.Player.withPasswordHash;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.helpers.RequestLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.AdventureRequest;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.OceanManager.Destination;
import net.sourceforge.kolmafia.session.OceanManager.Point;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class OceanManagerTest {
  @BeforeAll
  public static void beforeAll() {
    // Simulate logging out and back in again.
    Preferences.saveSettingsToFile = false;
  }

  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("OceanManager");
    Preferences.reset("OceanManager");
    KoLConstants.inventory.clear();
  }

  @AfterAll
  public static void afterAll() {
    Preferences.saveSettingsToFile = true;
  }

  @Nested
  class NotAutomated {
    @Test
    public void canGetPowerSphere() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withMeat(977), withHandlingChoice(189));
      try (cleanups) {
        builder.client.addResponse(302, Map.of("location", List.of("ocean.php?intro=1")), "");
        builder.client.addResponse(200, html("request/test_ocean_intro.html"));
        builder.client.addResponse(200, ""); // api.php
        builder.client.addResponse(200, html("request/test_ocean_sphere.html"));

        String urlString = "choice.php?pwd&whichchoice=189&option=1";
        var choice = new GenericRequest(urlString);
        choice.run();

        // Validate sensible request logging
        String expected = "Encounter: Set an Open Course for the Virgin Booty";
        assertEquals(RequestLogger.previousUpdateString, expected);

        // Redirects to ocean.php
        // No longer in a choice
        assertFalse(ChoiceManager.handlingChoice);
        // We paid for the privilege
        assertEquals(0, KoLCharacter.getAvailableMeat());

        // Validate sensible request logging
        urlString = "ocean.php?lon=48&lat=47";
        expected = "Setting sail for (48,47) = El Vibrato power sphere";
        assertTrue(OceanManager.registerRequest(urlString));
        assertEquals(expected, RequestLogger.previousUpdateString);

        // Actually run the request
        var sail = new GenericRequest(urlString);
        sail.run();
        assertTrue(InventoryManager.hasItem(ItemPool.POWER_SPHERE));

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(4));
        assertPostRequest(requests.get(0), "/choice.php", "whichchoice=189&option=1");
        assertGetRequest(requests.get(1), "/ocean.php", "intro=1");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/ocean.php", "lon=48&lat=47");
      }
    }

    @Test
    public void canGetTrapezoid() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withMeat(977),
              withHandlingChoice(189),
              withItem(ItemPool.POWER_SPHERE));
      try (cleanups) {
        builder.client.addResponse(302, Map.of("location", List.of("ocean.php?intro=1")), "");
        builder.client.addResponse(200, html("request/test_ocean_intro.html"));
        builder.client.addResponse(200, ""); // api.php
        builder.client.addResponse(200, html("request/test_ocean_plinth.html"));

        String urlString = "choice.php?pwd&whichchoice=189&option=1";
        var choice = new GenericRequest(urlString);
        choice.run();

        // Validate sensible request logging
        String expected = "Encounter: Set an Open Course for the Virgin Booty";
        assertEquals(RequestLogger.previousUpdateString, expected);

        // Redirects to ocean.php
        // No longer in a choice
        assertFalse(ChoiceManager.handlingChoice);
        // We paid for the privilege
        assertEquals(0, KoLCharacter.getAvailableMeat());

        // Validate sensible request logging
        urlString = "ocean.php?lon=48&lat=47";
        expected = "Setting sail for (48,47) = El Vibrato power sphere";
        assertTrue(OceanManager.registerRequest(urlString));
        assertEquals(expected, RequestLogger.previousUpdateString);

        // Actually run the request
        var sail = new GenericRequest(urlString);
        sail.run();

        // We exchanged our power sphere for a trapezoid
        assertFalse(InventoryManager.hasItem(ItemPool.POWER_SPHERE));
        assertTrue(InventoryManager.hasItem(ItemPool.TRAPEZOID));

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(4));
        assertPostRequest(requests.get(0), "/choice.php", "whichchoice=189&option=1");
        assertGetRequest(requests.get(1), "/ocean.php", "intro=1");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/ocean.php", "lon=48&lat=47");
      }
    }
  }

  @Nested
  class OceanDestinations {
    @Test
    public void destinationSetsAreValid() {
      assertEquals(15, OceanManager.muscleDestinations.size());
      assertEquals(15, OceanManager.mysticalityDestinations.size());
      assertEquals(15, OceanManager.moxieDestinations.size());
      assertEquals(11, OceanManager.sandDestinations.size());
      assertEquals(43, OceanManager.altarDestinations.size());
      assertEquals(3, OceanManager.sphereDestinations.size());
      assertEquals(1, OceanManager.plinthDestinations.size());
      assertEquals(102, Destination.MAINLAND.getLocations().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"muscle", "mysticality", "moxie", "sand", "altar", "sphere", "plinth"})
    public void canChooseDestinationFromProperty(String keyword) {
      var cleanups = new Cleanups(withProperty("oceanDestination", keyword));
      try (cleanups) {
        List<Point> destinations = OceanManager.getDestinations(keyword);
        Point destination = OceanManager.getDestination();
        assertNotNull(destination);
        assertTrue(destinations.contains(destination));
      }
    }

    @Test
    public void canChooseRandomDestination() {
      var cleanups = new Cleanups(withProperty("oceanDestination", "random"));
      try (cleanups) {
        Point destination = OceanManager.getDestination();
        assertNotNull(destination);
      }
    }

    @Test
    public void canChooseSpecificDestination() {
      var cleanups = new Cleanups(withProperty("oceanDestination", "50,50"));
      try (cleanups) {
        Point destination = OceanManager.getDestination();
        assertNotNull(destination);
        assertEquals("50,50", destination.toString());
      }
    }

    @Test
    public void manualControlAborts() {
      var builder = new FakeHttpClientBuilder();
      RequestLoggerOutput.startStream();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("oceanDestination", "manual"),
              withContinuationState());
      try (cleanups) {
        OceanManager.processOceanAdventure();
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ABORT));
        var text = RequestLoggerOutput.stopStream();
        assertThat(text, containsString("Pick a valid course."));
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(0));
      }
    }

    @Test
    public void rejectsMainlandDestination() {
      var builder = new FakeHttpClientBuilder();
      RequestLoggerOutput.startStream();
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withProperty("oceanDestination", "12,12"));
      try (cleanups) {
        builder.client.addResponse(200, "");
        // OceanDestinationComboBox should disallow oceanDestination from
        // having coordinates on the mainland. However, if the user somehow did
        // it manually, we currently handle it by choosing a random location.
        Point destination = OceanManager.getDestination();
        assertNotNull(destination);
        assertEquals("12,12", destination.toString());
        assertTrue(Destination.MAINLAND.getLocations().contains(destination));

        OceanManager.processOceanAdventure();
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ERROR));
        var text = RequestLoggerOutput.stopStream();
        assertThat(text, containsString("You cannot sail to the mainland."));

        // Extract and parse the new destination
        Matcher matcher = OceanManager.POINT_PATTERN.matcher(text);
        int lon = 0;
        int lat = 0;
        if (matcher.find()) {
          lon = StringUtilities.parseInt(matcher.group(1));
          lat = StringUtilities.parseInt(matcher.group(2));
        }

        assertThat(text, containsString("Random destination chosen: " + lon + "," + lat));
        assertThat(text, containsString("Setting sail for (" + lon + "," + lat + ")"));

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/ocean.php", "lon=" + lon + "&lat=" + lat);
      }
    }
  }

  @Nested
  class Automation {

    // Automation is controlled by 2 (3) settings:
    //
    // choiceadventure189 / oceanDestination
    //   0 / manual (defaults)
    //   1 / muscle, mysticality, moxie, sand, altar, sphere, plinth, random, XXX,YYY
    //   2 / ignore
    //
    // oceanAction
    //   continue
    //   show
    //   stop
    //   savecontinue (default)
    //   saveshow
    //   savestop

    private static final String ABORT_MESSAGE =
        "<a href=main.php target=mainpane class=error>Click here to continue in the relay browser.</a><br>";

    @Test
    public void canAutomateWithManualControl() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withMeat(977),
              withProperty("choiceAdventure189", 0),
              withProperty("oceanDestination", "manual"),
              withProperty("oceanAction", "continue"),
              // Needed when automating AdventureRequest -> CHOICE_HANDLER
              withPasswordHash("choice"),
              withGender(1));
      try (cleanups) {
        // adventure.php?snarfblat=159
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        // choice.php?forceoption=0
        builder.client.addResponse(200, html("request/test_ocean_choice.html"));
        // api.php?what=status&for=KoLmafia
        builder.client.addResponse(200, ""); // api.php

        var adventure = new AdventureRequest("The Poop Deck", AdventurePool.POOP_DECK);
        adventure.run();

        // This redirects to a choice.
        // Attempt to automate it and discover the user wants manual control
        // Abort and tell them to continue in the relay browser
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ABORT));
        String expected = ABORT_MESSAGE;
        assertEquals(RequestLogger.previousUpdateString, expected);

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(3));
        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=159&pwd=choice");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canAutomateWithKeywordDestination() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withMeat(977),
              withProperty("choiceAdventure189", 1),
              withProperty("oceanDestination", "sphere"),
              withProperty("oceanAction", "continue"),
              // Needed when automating AdventureRequest -> CHOICE_HANDLER
              withPasswordHash("choice"),
              withGender(1));
      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        // choice.php?forceoption=0
        builder.client.addResponse(200, html("request/test_ocean_choice.html"));
        // api.php?what=status&for=KoLmafia
        builder.client.addResponse(200, ""); // api.php
        // choice.php?whichchoice=189&option=1&pwd
        builder.client.addResponse(302, Map.of("location", List.of("ocean.php?intro=1")), "");
        // We ignore the redirect to ocean.php?intro=1
        // builder.client.addResponse(200, html("request/test_ocean_intro.html"));
        // Instead, we submit: ocean.php?lon=59&lat=10
        builder.client.addResponse(200, html("request/test_ocean_sphere_2.html"));
        builder.client.addResponse(200, ""); // api.php

        var adventure = new AdventureRequest("The Poop Deck", AdventurePool.POOP_DECK);
        adventure.run();

        // This redirects to a choice.
        // Attempt to automate it and discover the user wants a power sphere
        // Submit URL to accept the helm, which redirects to ocean.php
        // OceanManager completes the processing
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        // We paid 977 Meat for the journey
        assertEquals(0, KoLCharacter.getAvailableMeat());
        // We ended up with a power sphere
        assertTrue(InventoryManager.hasItem(ItemPool.POWER_SPHERE));

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(6));
        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=159&pwd=choice");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/choice.php", "whichchoice=189&option=1&pwd=choice");
        // With "sphere", we choose one of three random destinations:
        // assertPostRequest(requests.get(4), "/ocean.php", "lon=48&lat=47&pwd=choice");
        // assertPostRequest(requests.get(4), "/ocean.php", "lon=59&lat=10&pwd=choice");
        // assertPostRequest(requests.get(4), "/ocean.php", "lon=86&lat=40&pwd=choice");
        assertPostRequest(requests.get(5), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canAutomateWithCoordinatesDestination() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withMeat(977),
              withProperty("choiceAdventure189", 1),
              withProperty("oceanDestination", "86,40"),
              withProperty("oceanAction", "continue"),
              // Needed when automating AdventureRequest -> CHOICE_HANDLER
              withPasswordHash("choice"),
              withGender(1));
      try (cleanups) {
        builder.client.addResponse(
            302, Map.of("location", List.of("choice.php?forceoption=0")), "");
        // choice.php?forceoption=0
        builder.client.addResponse(200, html("request/test_ocean_choice.html"));
        // api.php?what=status&for=KoLmafia
        builder.client.addResponse(200, ""); // api.php
        // choice.php?whichchoice=189&option=1&pwd
        builder.client.addResponse(302, Map.of("location", List.of("ocean.php?intro=1")), "");
        // We ignore the redirect to ocean.php?intro=1
        // builder.client.addResponse(200, html("request/test_ocean_intro.html"));
        // Instead, we submit: ocean.php?lon=86&lat=40
        builder.client.addResponse(200, html("request/test_ocean_sphere_3.html"));
        builder.client.addResponse(200, ""); // api.php

        var adventure = new AdventureRequest("The Poop Deck", AdventurePool.POOP_DECK);
        adventure.run();

        // This redirects to a choice.
        // Attempt to automate it and discover the user wants a power sphere
        // Submit URL to accept the helm, which redirects to ocean.php
        // OceanManager completes the processing
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        // We paid 977 Meat for the journey
        assertEquals(0, KoLCharacter.getAvailableMeat());
        // We ended up with a power sphere
        assertTrue(InventoryManager.hasItem(ItemPool.POWER_SPHERE));

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(6));
        assertPostRequest(requests.get(0), "/adventure.php", "snarfblat=159&pwd=choice");
        assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
        assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
        assertPostRequest(requests.get(3), "/choice.php", "whichchoice=189&option=1&pwd=choice");
        assertPostRequest(requests.get(4), "/ocean.php", "lon=86&lat=40&pwd=choice");
        assertPostRequest(requests.get(5), "/api.php", "what=status&for=KoLmafia");
      }
    }
  }
}
