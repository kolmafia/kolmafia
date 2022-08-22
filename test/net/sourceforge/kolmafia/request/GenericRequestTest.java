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
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.EquipmentManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class GenericRequestTest {
  @BeforeEach
  public void beforeEach() {
    KoLCharacter.reset("GenericRequestTest");
    Preferences.reset("GenericRequestTest");
  }

  @Test
  public void hallowienerVolcoinoNotPickedUpByLuckyGoldRing() {
    var cleanups =
        new Cleanups(
            withEquipped(EquipmentManager.ACCESSORY1, "lucky gold ring"),
            withProperty("lastEnccounter", ""));

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
}
