package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.*;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.objectpool.AdventurePool;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

public class GenericRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("GenericRequestTest");
    Preferences.reset("GenericRequestTest");
  }

  @ParameterizedTest
  @CsvSource({
    "inventory.php, false",
    "mall.php, true",
    "manageprices.php, true",
    "backoffice.php, true",
    "newchatmessages.php, true",
  })
  public void certainUrlsAreIgnored(final String url, final boolean expected) {
    var req = new GenericRequest(url);
    assertThat(GenericRequest.shouldIgnore(req), equalTo(expected));
  }

  @Test
  public void hallowienerVolcoinoNotPickedUpByLuckyGoldRing() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, "lucky gold ring"), withProperty("lastEncounter", ""));

    try (cleanups) {
      assertFalse(Preferences.getBoolean("_luckyGoldRingVolcoino"));

      KoLAdventure.setLastAdventure("The Bubblin' Caldera");

      GenericRequest request = new GenericRequest("adventure.php?snarfblat=451");
      request.setHasResult(true);
      request.responseText =
          html("request/test_adventure_hallowiener_volcoino_lucky_gold_ring.html");

      request.processResponse();

      assertEquals("Lava Dogs", Preferences.getString("lastEncounter"));
      assertFalse(Preferences.getBoolean("_luckyGoldRingVolcoino"));
    }
  }

  @Test
  public void hallowienerVolcoinoPickedUp() {
    var cleanups =
        new Cleanups(
            withEquipped(Slot.ACCESSORY1, "lucky gold ring"),
            withProperty("lastEncounter", ""),
            withProperty("hallowienerVolcoino", "false"));

    try (cleanups) {
      KoLAdventure.setLastAdventure("The Bubblin' Caldera");

      GenericRequest request = new GenericRequest("adventure.php?snarfblat=451");
      request.setHasResult(true);
      request.responseText =
          html("request/test_adventure_hallowiener_volcoino_lucky_gold_ring.html");

      request.processResponse();

      assertEquals("Lava Dogs", Preferences.getString("lastEncounter"));
      assertThat("hallowienerVolcoino", isSetTo(true));
    }
  }

  @Test
  public void seeingEmptySpookyPuttyMonsterSetsProperty() {
    Preferences.setString("spookyPuttyMonster", "zmobie");

    var req = new GenericRequest("desc_item.php?whichitem=324375100");
    req.responseText = html("request/test_desc_item_spooky_putty_monster_empty.html");
    req.processResponse();

    assertThat("spookyPuttyMonster", isSetTo(""));
  }

  @ParameterizedTest
  @ValueSource(strings = {"beast", "elf"})
  public void learnLocketPhylumFromLocketDescription(String phylum) {
    var req = new GenericRequest("desc_item.php?whichitem=634036450");
    req.responseText = html("request/test_desc_item_combat_lovers_locket_" + phylum + ".html");
    req.processResponse();

    assertThat("locketPhylum", isSetTo(phylum));
  }

  @ParameterizedTest
  @ValueSource(ints = {100, 0})
  public void parseDesignerSweatpants(int expectedSweat) {
    var req = new GenericRequest("desc_item.php?whichitem=800334855");
    req.responseText =
        html("request/test_desc_item_designer_sweatpants_" + expectedSweat + "_sweat.html");
    req.processResponse();

    assertThat("sweat", isSetTo(expectedSweat));
  }

  @ParameterizedTest
  @ValueSource(ints = {100, 40, 0})
  public void parsePowerfulGlove(int expectedCharge) {
    var cleanups = withProperty("_powerfulGloveBatteryPowerUsed", 50);

    try (cleanups) {
      var req = new GenericRequest("desc_item.php?whichitem=991142661");
      req.responseText =
          html("request/test_desc_item_powerful_glove_" + expectedCharge + "_charge_used.html");
      req.processResponse();

      assertThat("_powerfulGloveBatteryPowerUsed", isSetTo(expectedCharge));
    }
  }

  @Test
  public void detectsBogusChoices() {
    var cleanup =
        new Cleanups(
            withNextResponse(200, html("request/test_choice_whoops.html")),
            withProperty("_shrubDecorated"),
            withContinuationState());

    try (cleanup) {
      new GenericRequest(
              "choice.php?whichchoice=999&pwd&option=1&topper=3&lights=5&garland=1&gift=2")
          .run();

      assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.ABORT));
      assertThat("_shrubDecorated", isSetTo(false));
    }
  }

  @Nested
  class SuppressUpdate {

    public static Cleanups withUpdateSuppressed() {
      var old = GenericRequest.updateSuppressed();
      GenericRequest.suppressUpdate(true);
      return new Cleanups(
          () -> {
            GenericRequest.suppressUpdate(old);
          });
    }

    @Test
    public void willRequestUnsuppressedUpdate() {
      var builder = new FakeHttpClientBuilder();
      var cleanup =
          new Cleanups(
              withItem("seal tooth"), withHttpClientBuilder(builder), withContinuationState());

      try (cleanup) {
        var request = new GenericRequest("inv_use.php?whichitem=2&ajax=1");
        builder.client.addResponse(200, html("request/test_use_seal_tooth.html"));
        request.run();
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(2));
        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=2&ajax=1");
        assertPostRequest(requests.get(1), "/api.php", "what=status&for=KoLmafia");
      }
    }

    @Test
    public void willGloballySuppressUpdate() {
      var builder = new FakeHttpClientBuilder();
      var cleanup =
          new Cleanups(
              withItem("seal tooth"),
              withUpdateSuppressed(),
              withHttpClientBuilder(builder),
              withContinuationState());

      try (cleanup) {
        var request = new GenericRequest("inv_use.php?whichitem=2&ajax=1");
        builder.client.addResponse(200, html("request/test_use_seal_tooth.html"));
        request.run();
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=2&ajax=1");
      }
    }

    @Test
    public void willLocallySuppressUpdate() {
      var builder = new FakeHttpClientBuilder();
      var cleanup =
          new Cleanups(
              withItem("seal tooth"), withHttpClientBuilder(builder), withContinuationState());

      try (cleanup) {
        var request = new UpdateSuppressedRequest("inv_use.php?whichitem=2&ajax=1");
        builder.client.addResponse(200, html("request/test_use_seal_tooth.html"));
        request.run();
        assertThat(StaticEntity.getContinuationState(), equalTo(MafiaState.CONTINUE));
        var requests = builder.client.getRequests();
        assertThat(requests, hasSize(1));
        assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=2&ajax=1");
      }
    }
  }

  @Test
  public void testTracksTowelAcquired() {
    var cleanups =
        new Cleanups(
            withProperty("lastTowelAscension", -1),
            withAscensions(123),
            withNextResponse(
                new FakeHttpResponse<>(
                    302, Map.of("location", List.of("choice.php?forceoption=0")), ""),
                new FakeHttpResponse<>(
                    200, html("request/test_request_haunted_bathroom_off_the_rack.html")),
                // Swallow an api request triggered by the choice containing a charpane request.
                new FakeHttpResponse<>(200, ""),
                new FakeHttpResponse<>(
                    200, html("request/test_request_haunted_bathroom_towel.html"))));

    try (cleanups) {
      // Does a 302 redirect to choice.php
      var hitChoice =
          new GenericRequest("adventure.php?snarfblat=" + AdventurePool.HAUNTED_BATHROOM);
      hitChoice.run();

      // Take the towel
      var tookTowel = new GenericRequest("choice.php?pwd&whichchoice=882&option=1");
      tookTowel.run();

      assertThat("lastTowelAscension", isSetTo(123));
    }
  }

  @Nested
  class Hallowiener {
    @ParameterizedTest
    @CsvSource({
      "volcoino_lucky_gold_ring, \"The Bubblin' Caldera\", Lava Dogs, hallowienerVolcoino, true",
      "8bit_realm, 8-Bit Realm, Dog Needs Food Badly, hallowiener8BitRealm, 1",
      "sonofa_beach, Sonofa Beach, Gunbowwowder, hallowienerSonofaBeach, true",
      "secret_government_laboratory, The Secret Government Laboratory, Labrador Conspirator, hallowienerCoinspiracy, 1",
      "middle_chamber, The Middle Chamber, Ratchet-catcher, hallowienerMiddleChamber, true",
      "overgrown_lot, The Overgrown Lot, Boooooze Hound, hallowienerOvergrownLot, true",
      "madness_bakery, Madness Bakery, Baker's Dogzen, hallowienerMadnessBakery, true",
      "smut_orcs, Smut Orc Logging Camp, Carpenter Dog, hallowienerSmutOrcs, true",
      "guano_junction, Guano Junction, Are They Made of Real Dogs?, hallowienerGuanoJunction, true",
      "skeleton_store, The Skeleton Store, Fruuuuuuuit, hallowienerSkeletonStore, true",
      "degrassi_knoll_gym, The Degrassi Knoll Gym, It Isn't a Poodle, hallowienerKnollGym, true",
      "defiled_nook, The Defiled Nook, Seeing-Eyes Dog, hallowienerDefiledNook, true"
    })
    public void hallowienerPickedUp(
        String htmlName, String location, String encounterName, String property, String expected) {
      var cleanups =
          new Cleanups(
              withProperty("lastEncounter", ""),
              withProperty(property, Preferences.getDefault(property)));

      try (cleanups) {
        // Assert that the defaults are set to the correct data type
        if (expected.matches("\\d+")) {
          assertThat(property, isSetTo("0"));
        } else {
          assertThat(property, isSetTo("false"));
        }

        KoLAdventure.setLastAdventure(location);

        GenericRequest request =
            new GenericRequest("adventure.php?snarfblat=" + KoLAdventure.lastAdventureId());
        request.setHasResult(true);
        request.responseText = html("request/test_adventure_hallowiener_" + htmlName + ".html");

        request.processResponse();

        assertEquals(encounterName, Preferences.getString("lastEncounter"));
        assertThat(property, isSetTo(expected));
      }
    }
  }

  @Nested
  class DistantWoodsGetaway {
    @Test
    public void getawayRemainsUnchangedAfterVisitingLockedDistantWoods() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder), withProperty("getawayCampsiteUnlocked", true));
      try (cleanups) {
        builder.client.addResponse(200, html("request/test_place_woods_uhoh.html"));

        var request = new GenericRequest("place.php?whichplace=woods");
        request.run();

        assertThat("getawayCampsiteUnlocked", isSetTo(true));
      }
    }
  }
}
