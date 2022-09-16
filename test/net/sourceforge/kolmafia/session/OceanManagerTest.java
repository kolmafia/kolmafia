package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHandlingChoice;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withMeat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class OceanManagerTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("OceanManager");
    Preferences.reset("OceanManager");
    KoLConstants.inventory.clear();
  }

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
      expected = "Setting sail for (48,47) = Power Sphere";
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
      expected = "Setting sail for (48,47) = Power Sphere";
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
