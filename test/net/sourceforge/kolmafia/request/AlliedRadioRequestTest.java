package net.sourceforge.kolmafia.request;

import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.assertPostRequest;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.helpers.SessionLoggerOutput;
import internal.network.FakeHttpClientBuilder;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AlliedRadioRequestTest {
  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("AlliedRadioRequestTest");
  }

  @BeforeEach
  public void beforeEach() {
    Preferences.reset("AlliedRadioRequestTest");
  }

  @Test
  public void visitChoiceUpdatesPreference() {
    var cleanups = withProperty("_alliedRadioDropsUsed", 0);

    try (cleanups) {
      var resp = html("request/test_allied_radio_grey_text.html");
      AlliedRadioRequest.visitChoice(resp);
      assertThat("_alliedRadioDropsUsed", isSetTo(1));
    }
  }

  @Test
  public void postChoiceIncrementsPreference() {
    var cleanups = withProperty("_alliedRadioDropsUsed", 0);
    SessionLoggerOutput.startStream();

    try (cleanups) {
      var resp = html("request/test_allied_radio_success.html");
      AlliedRadioRequest.postChoice(resp, false, "radio");
      var text = SessionLoggerOutput.stopStream();
      assertThat("_alliedRadioDropsUsed", isSetTo(1));
      assertThat(text, containsString("Radio number / letter pattern received: 202 - P"));
    }
  }

  @Test
  public void postChoiceRemovesHandheld() {
    var cleanups =
        new Cleanups(
            withProperty("_alliedRadioDropsUsed", 0), withItem(ItemPool.HANDHELD_ALLIED_RADIO));
    SessionLoggerOutput.startStream();

    try (cleanups) {
      var resp = html("request/test_allied_radio_last.html");
      AlliedRadioRequest.postChoice(resp, true, "radio");
      var text = SessionLoggerOutput.stopStream();
      assertThat("_alliedRadioDropsUsed", isSetTo(0));
      assertThat(InventoryManager.getCount(ItemPool.HANDHELD_ALLIED_RADIO), is(0));
      assertThat(text, containsString("Radio number / letter pattern received: 1654 - S"));
    }
  }

  @Test
  public void postChoiceLogsGreyText() {
    var cleanups =
        new Cleanups(
            withProperty("_alliedRadioDropsUsed", 0), withProperty("demonName14Segments", "But"));
    SessionLoggerOutput.startStream();

    try (cleanups) {
      var resp = html("request/test_allied_radio_grey_text.html");
      AlliedRadioRequest.postChoice(resp, false, "anything");
      var text = SessionLoggerOutput.stopStream();
      assertThat(text, containsString("Radio grey text received: ulH"));
      assertThat("demonName14Segments", isSetTo("ulH,But"));
    }
  }

  @Test
  public void postChoiceTracksGreyTextWithFrequency() {
    var cleanups =
        new Cleanups(
            withProperty("_alliedRadioDropsUsed", 0),
            withProperty("demonName14Segments", "But,ulH"));
    try (cleanups) {
      var resp = html("request/test_allied_radio_grey_text.html");
      AlliedRadioRequest.postChoice(resp, false, "anything");
      assertThat("demonName14Segments", isSetTo("But,ulH:2"));
    }
  }

  @Test
  public void errorsIfNoRadio() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups = withHttpClientBuilder(builder);

    try (cleanups) {
      var request = new AlliedRadioRequest("radio");
      request.run();

      assertThat(client.getRequests(), empty());
      assertThat(StaticEntity.getContinuationState(), is(MafiaState.ERROR));
      assertThat(
          KoLmafia.lastMessage, equalTo("You do not have a backpack or handheld radio to use."));
    }
  }

  @Test
  public void prefersBackpackIfBothHandheldAndBackpack() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("_alliedRadioDropsUsed", 0),
            withItem(ItemPool.HANDHELD_ALLIED_RADIO),
            withItem(ItemPool.ALLIED_RADIO_BACKPACK));

    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_allied_radio_grey_text.html"));
      client.addResponse(200, ""); // api.php
      client.addResponse(200, html("request/test_allied_radio_success.html"));

      var request = new AlliedRadioRequest("radio");
      request.run();

      assertThat(StaticEntity.getContinuationState(), is(MafiaState.CONTINUE));
      var requests = client.getRequests();
      assertGetRequest(requests.get(0), "/inventory.php", "action=requestdrop&pwd=");
      assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      assertPostRequest(requests.get(3), "/choice.php", "option=1&request=radio&whichchoice=1561");
    }
  }

  @Test
  public void usesHandheldIfNoBackpack() {
    var builder = new FakeHttpClientBuilder();
    var client = builder.client;
    var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withProperty("_alliedRadioDropsUsed", 3),
            withItem(ItemPool.HANDHELD_ALLIED_RADIO),
            withItem(ItemPool.ALLIED_RADIO_BACKPACK));

    try (cleanups) {
      client.addResponse(302, Map.of("location", List.of("choice.php?forceoption=0")), "");
      client.addResponse(200, html("request/test_allied_radio_grey_text.html"));
      client.addResponse(200, ""); // api.php
      client.addResponse(200, html("request/test_allied_radio_success.html"));

      var request = new AlliedRadioRequest("radio");
      request.run();

      assertThat(StaticEntity.getContinuationState(), is(MafiaState.CONTINUE));
      var requests = client.getRequests();
      assertPostRequest(requests.get(0), "/inv_use.php", "whichitem=11946");
      assertGetRequest(requests.get(1), "/choice.php", "forceoption=0");
      assertPostRequest(requests.get(2), "/api.php", "what=status&for=KoLmafia");
      assertPostRequest(requests.get(3), "/choice.php", "option=1&request=radio&whichchoice=1563");
    }
  }

  @Test
  public void updatesPreferenceForSniperSupport() {
    var cleanups =
        new Cleanups(
            withProperty("_alliedRadioDropsUsed", 0), withProperty("noncombatForcerActive"));

    try (cleanups) {
      AlliedRadioRequest.postChoice("", false, "sniper support");
      assertThat("noncombatForcerActive", isSetTo(true));
    }
  }

  @Test
  public void updatesPreferenceForMaterielIntel() {
    var cleanups =
        new Cleanups(
            withProperty("_alliedRadioDropsUsed", 0), withProperty("_alliedRadioMaterielIntel"));

    try (cleanups) {
      AlliedRadioRequest.postChoice("", false, "materiel intel");
      assertThat("_alliedRadioMaterielIntel", isSetTo(true));
    }
  }

  @Test
  public void secondMaterielIntelDoesNotConsumeCharge() {
    var cleanups = new Cleanups(withProperty("_alliedRadioDropsUsed", 0));

    try (cleanups) {
      var resp = html("request/test_allied_radio_materiel_twice.html");
      AlliedRadioRequest.postChoice(resp, false, "materiel intel");
      assertThat("_alliedRadioDropsUsed", isSetTo(0));
    }
  }

  @Test
  public void updatesPreferenceForWildsunBoon() {
    var cleanups =
        new Cleanups(
            withProperty("_alliedRadioDropsUsed", 0), withProperty("_alliedRadioWildsunBoon"));

    try (cleanups) {
      AlliedRadioRequest.postChoice("", false, "WILDSUN BOON");
      assertThat("_alliedRadioWildsunBoon", isSetTo(true));
    }
  }
}
