package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withEmptyCampground;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class CampgroundRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("CampgroundRequest");
    Preferences.reset("CampgroundRequest");
  }

  @Test
  void canDetectExhaustedMedicineCabinet() {
    String html = html("request/test_campground_medicine_cabinet_out_of_consults.html");
    CampgroundRequest.parseResponse("campground.php?action=workshed", html);
    assertEquals(
        CampgroundRequest.getCurrentWorkshedItem().getItemId(), ItemPool.COLD_MEDICINE_CABINET);
  }

  @Nested
  class ElVibratoPortal {
    @Test
    public void canUseTrapezoid() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.TRAPEZOID),
              withEmptyCampground(),
              withProperty("currentPortalEnergy", 0));
      try (cleanups) {
        builder.client.addResponse(200, html("request/test_use_el_vibrato_trapezoid.html"));
        builder.client.addResponse(200, ""); // api.php

        String urlString = "inv_use.php?whichitem=3198&ajax=1";
        var request = new GenericRequest(urlString);
        request.run();

	int index = KoLConstants.campground.indexOf(ItemPool.get(ItemPool.TRAPEZOID));
	assertNotEquals(-1, index);
        AdventureResult portal = KoLConstants.campground.get(index);
        assertNotNull(portal);
	assertEquals(20, Preferences.getInteger("currentPortalEnergy"));
	assertEquals(20, portal.getCount());

        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=3198&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void canRecognizeActivePortal() {
      String html = html("request/test_visit_el_vibrato_campground.html");
      var cleanups = new Cleanups(withEmptyCampground(), withProperty("currentPortalEnergy", 0));
      try (cleanups) {
        CampgroundRequest.parseResponse("campground.php", html);
	int index = KoLConstants.campground.indexOf(ItemPool.get(ItemPool.TRAPEZOID));
	assertNotEquals(-1, index);
        AdventureResult portal = KoLConstants.campground.get(index);
        assertNotNull(portal);
	assertEquals(20, Preferences.getInteger("currentPortalEnergy"));
	assertEquals(20, portal.getCount());
      }
    }

    @Test
    public void canRecognizeActivePortalWithEnergy() {
      String html = html("request/test_visit_el_vibrato_campground.html");
      var cleanups = new Cleanups(withEmptyCampground(), withProperty("currentPortalEnergy", 10));
      try (cleanups) {
        CampgroundRequest.parseResponse("campground.php", html);
	int index = KoLConstants.campground.indexOf(ItemPool.get(ItemPool.TRAPEZOID));
	assertNotEquals(-1, index);
        AdventureResult portal = KoLConstants.campground.get(index);
        assertNotNull(portal);
	assertEquals(10, Preferences.getInteger("currentPortalEnergy"));
	assertEquals(10, portal.getCount());
      }
    }
  }
}
