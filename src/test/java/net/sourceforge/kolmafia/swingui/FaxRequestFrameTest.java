package net.sourceforge.kolmafia.swingui;

import static internal.helpers.Networking.getPostRequestBody;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withDataFile;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static internal.matchers.Preference.isSetTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.net.http.HttpRequest;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.InventoryManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FaxRequestFrameTest {
  private static Cleanups globalCleanup;

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("FaxRequestFrameTest");
    Preferences.reset("FaxRequestFrameTest");
    ChatManager.setChatLiteracy(true);

    globalCleanup =
        new Cleanups(
            withDataFile("cheesefax.xml"),
            withDataFile("easyfax.xml"),
            withDataFile("onlyfax.xml"));
    FaxBotDatabase.configure();
  }

  @AfterAll
  static void afterAll() {
    globalCleanup.close();
  }

  private static Monster easyfaxMonster(final String name) {
    return FaxBotDatabase.getFaxbot("Easyfax").getMonsterByActualName(name);
  }

  private static FakeHttpResponse<String> faxMachine(
      final HttpRequest request, final String onReceive, final String onSend) {
    return faxMachine(
        request, onReceive, onSend, html("request/test_desc_item_photocopied_mariachi.html"));
  }

  private static FakeHttpResponse<String> faxMachine(
      final HttpRequest request,
      final String onReceive,
      final String onSend,
      final String description) {
    String path = request.uri().getPath();
    if (path.equals("/clan_viplounge.php")) {
      String body = getPostRequestBody(request);
      if (body.contains("preaction=receivefax")) {
        return new FakeHttpResponse<>(onReceive);
      }
      if (body.contains("preaction=sendfax")) {
        return new FakeHttpResponse<>(onSend);
      }
      return new FakeHttpResponse<>("You approach the fax machine.");
    }
    if (path.equals("/desc_item.php")) {
      return new FakeHttpResponse<>(description);
    }
    return new FakeHttpResponse<>("");
  }

  private static List<HttpRequest> faxMachineRequests(
      final FakeHttpClientBuilder builder, final String preaction) {
    return builder.client.getRequests().stream()
        .filter(r -> r.uri().getPath().equals("/clan_viplounge.php"))
        .filter(r -> getPostRequestBody(r).contains("preaction=" + preaction))
        .toList();
  }

  @Nested
  class NoResponse {
    @Test
    void keepsRequestedMonsterFromFaxMachine() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(
          r -> faxMachine(r, html("request/test_clan_fax_receive.html"), ""));

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("faxbotTimeout", 0),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withItem(ItemPool.PHOTOCOPIED_MONSTER, 0),
              withProperty("photocopyMonster", ""),
              withProperty("lastSuccessfulFaxbot", ""));

      try (cleanups) {
        boolean result =
            FaxRequestFrame.requestFax("Easyfax", easyfaxMonster("handsome mariachi"), false);

        assertThat(result, is(true));
        assertThat(faxMachineRequests(builder, "receivefax"), hasSize(1));
        assertThat(faxMachineRequests(builder, "sendfax"), empty());
        assertThat("photocopyMonster", isSetTo("handsome mariachi"));
        assertThat("lastSuccessfulFaxbot", isSetTo("Easyfax"));
      }
    }

    @Test
    void returnsOtherMonsterToFaxMachine() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(
          r ->
              faxMachine(
                  r,
                  html("request/test_clan_fax_receive.html"),
                  "Your photocopy slowly slides into the machine"));

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("faxbotTimeout", 0),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withItem(ItemPool.PHOTOCOPIED_MONSTER, 0),
              withProperty("photocopyMonster", ""),
              withProperty("lastSuccessfulFaxbot", ""));

      try (cleanups) {
        boolean result =
            FaxRequestFrame.requestFax("Easyfax", easyfaxMonster("Knob Goblin Embezzler"), false);

        assertThat(result, is(false));
        assertThat(faxMachineRequests(builder, "sendfax"), hasSize(1));
        assertThat(InventoryManager.hasItem(ItemPool.PHOTOCOPIED_MONSTER), is(false));
        assertThat("photocopyMonster", isSetTo(""));
        assertThat("lastSuccessfulFaxbot", isSetTo(""));
      }
    }

    @Test
    void returnsFaxItCannotIdentifyToFaxMachine() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(
          r ->
              faxMachine(
                  r,
                  html("request/test_clan_fax_receive.html"),
                  "Your photocopy slowly slides into the machine",
                  ""));

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("faxbotTimeout", 0),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withItem(ItemPool.PHOTOCOPIED_MONSTER, 0),
              withProperty("photocopyMonster", ""));

      try (cleanups) {
        boolean result =
            FaxRequestFrame.requestFax("Easyfax", easyfaxMonster("handsome mariachi"), false);

        assertThat(result, is(false));
        assertThat(faxMachineRequests(builder, "sendfax"), hasSize(1));
        assertThat(InventoryManager.hasItem(ItemPool.PHOTOCOPIED_MONSTER), is(false));
        assertThat("photocopyMonster", isSetTo(""));
      }
    }

    @Test
    void reportsWrongFaxTheMachineWillNotTakeBack() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(
          r -> faxMachine(r, html("request/test_clan_fax_receive.html"), ""));

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withContinuationState(),
              withProperty("faxbotTimeout", 0),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withItem(ItemPool.PHOTOCOPIED_MONSTER, 0),
              withProperty("photocopyMonster", ""));

      try (cleanups) {
        boolean result =
            FaxRequestFrame.requestFax("Easyfax", easyfaxMonster("Knob Goblin Embezzler"), false);

        assertThat(result, is(false));
        assertThat(StaticEntity.getContinuationState(), is(MafiaState.CONTINUE));
        assertThat(
            KoLmafia.getLastMessage(), is("Could not put the photocopy back in the fax machine."));
        assertThat(InventoryManager.hasItem(ItemPool.PHOTOCOPIED_MONSTER), is(true));
      }
    }

    @Test
    void failsWhenFaxMachineIsEmpty() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> faxMachine(r, "just be a blank sheet of paper", ""));

      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withProperty("faxbotTimeout", 0),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withItem(ItemPool.PHOTOCOPIED_MONSTER, 0),
              withProperty("lastSuccessfulFaxbot", ""));

      try (cleanups) {
        boolean result =
            FaxRequestFrame.requestFax("Easyfax", easyfaxMonster("handsome mariachi"), false);

        assertThat(result, is(false));
        assertThat(faxMachineRequests(builder, "receivefax"), hasSize(1));
        assertThat(faxMachineRequests(builder, "sendfax"), empty());
        assertThat("lastSuccessfulFaxbot", isSetTo(""));
      }
    }
  }

  @Test
  void cannotRequestFaxWithoutVipKey() {
    var builder = new FakeHttpClientBuilder();

    var cleanups = new Cleanups(withHttpClientBuilder(builder));

    try (cleanups) {
      boolean result =
          FaxRequestFrame.requestFax("Easyfax", easyfaxMonster("handsome mariachi"), false);

      assertThat(result, is(false));
      assertThat(builder.client.getRequests(), empty());
    }
  }
}
