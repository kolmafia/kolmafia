package net.sourceforge.kolmafia.session;

import static internal.helpers.Networking.getPostRequestBody;
import static internal.helpers.Networking.html;
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
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase.Monster;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FaxRequestManagerTest {
  private static Cleanups globalCleanup;

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("FaxRequestManagerTest");
    Preferences.reset("FaxRequestManagerTest");
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

  private static boolean isFaxCommand(final HttpRequest request) {
    return request.uri().getPath().equals("/submitnewchat.php")
        && String.valueOf(request.uri().getQuery()).contains("/msg");
  }

  // A clan fax machine that delivers a handsome mariachi, and a faxbot that answers with reply
  private static FakeHttpResponse<String> faxMachine(
      final HttpRequest request, final String lounge, final String reply) {
    if (isFaxCommand(request)) {
      return new FakeHttpResponse<>(
          "<a target=mainpane href=\"showplayer.php?who=1\"><font color=blue><b>Easyfax"
              + " (private):</b></font></a> <font color=blue>"
              + reply
              + "</font><br>");
    }
    String path = request.uri().getPath();
    if (path.equals("/clan_viplounge.php")) {
      String body = getPostRequestBody(request);
      if (body.contains("preaction=receivefax")) {
        return new FakeHttpResponse<>(html("request/test_clan_fax_receive.html"));
      }
      return new FakeHttpResponse<>(lounge);
    }
    if (path.equals("/desc_item.php")) {
      return new FakeHttpResponse<>(html("request/test_desc_item_photocopied_mariachi.html"));
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
  class CannotReceive {
    @Test
    void reportsWhenNoFaxbotIsConfigured() {
      assertThat(FaxRequestManager.isBotOnline(null), is(false));
      assertThat(FaxRequestManager.getStatusMessage(), is("No faxbots configured."));
    }

    @Test
    void cannotRequestFaxWithoutChat() {
      var builder = new FakeHttpClientBuilder();
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withItem(ItemPool.VIP_LOUNGE_KEY));
      cleanups.add(() -> ChatManager.setChatLiteracy(true));

      try (cleanups) {
        ChatManager.setChatLiteracy(false);

        boolean result =
            FaxRequestManager.requestFax("Easyfax", easyfaxMonster("handsome mariachi"), false);

        assertThat(result, is(false));
        assertThat(KoLmafia.getLastMessage(), is("You are not allowed to use chat."));
        assertThat(builder.client.getRequests(), empty());
      }
    }

    @Test
    void cannotRequestFaxWithoutFaxMachine() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> faxMachine(r, "This clan has no VIP lounge.", ""));
      var cleanups =
          new Cleanups(withHttpClientBuilder(builder), withItem(ItemPool.VIP_LOUNGE_KEY));

      try (cleanups) {
        boolean result =
            FaxRequestManager.requestFax("Easyfax", easyfaxMonster("handsome mariachi"), false);

        assertThat(result, is(false));
        assertThat(KoLmafia.getLastMessage(), is("Your clan does not have a fax machine."));
        assertThat(faxMachineRequests(builder, "receivefax"), empty());
      }
    }
  }

  @Nested
  class BotReplies {
    @Test
    void receivesFaxWhenBotDeliversIt() {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(
          r -> faxMachine(r, "You approach the fax machine.", "Your fax is ready."));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withItem(ItemPool.PHOTOCOPIED_MONSTER, 0),
              withProperty("photocopyMonster", ""),
              withProperty("lastSuccessfulFaxbot", ""));

      try (cleanups) {
        boolean result =
            FaxRequestManager.requestFax("Easyfax", easyfaxMonster("handsome mariachi"), false);

        assertThat(result, is(true));
        assertThat(faxMachineRequests(builder, "receivefax"), hasSize(1));
        assertThat("lastSuccessfulFaxbot", isSetTo("Easyfax"));
      }
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = {
          "I do not recognize that monster|Configuration error: unknown command sent to Easyfax",
          "Sorry, it appears you requested an invalid monster.|Configuration error: unknown command sent to Easyfax",
          "I am not whitelisted|Easyfax is not on your clan's whitelist",
          "I couldn't get into your clan|Easyfax is not on your clan's whitelist",
          "You are only allowed 20 fax requests per day.|You are only allowed 20 fax requests per day."
        })
    void reportsWhyBotDidNotSendFax(final String reply, final String message) {
      var builder = new FakeHttpClientBuilder();
      builder.client.setResponseFunc(r -> faxMachine(r, "You approach the fax machine.", reply));
      var cleanups =
          new Cleanups(
              withHttpClientBuilder(builder),
              withItem(ItemPool.VIP_LOUNGE_KEY),
              withItem(ItemPool.PHOTOCOPIED_MONSTER, 0),
              withProperty("lastSuccessfulFaxbot", ""));

      try (cleanups) {
        boolean result =
            FaxRequestManager.requestFax("Easyfax", easyfaxMonster("handsome mariachi"), false);

        assertThat(result, is(false));
        assertThat(KoLmafia.getLastMessage(), is(message));
        assertThat(faxMachineRequests(builder, "receivefax"), empty());
        assertThat("lastSuccessfulFaxbot", isSetTo(""));
      }
    }
  }
}
