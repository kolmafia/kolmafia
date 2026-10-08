package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.HttpClientWrapper.getRequests;
import static internal.helpers.Networking.assertGetRequest;
import static internal.helpers.Networking.getPostRequestBody;
import static internal.helpers.Networking.html;
import static internal.helpers.Player.withContinuationState;
import static internal.helpers.Player.withDataFile;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import internal.helpers.Cleanups;
import internal.helpers.CliCaller;
import internal.helpers.HttpClientWrapper;
import internal.network.FakeHttpResponse;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.stream.Stream;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.session.ChoiceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class FaxbotCommandTest extends AbstractCommandTestBase {
  public FaxbotCommandTest() {
    this.command = "faxbot";
  }

  public static Cleanups globalCleanup;

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("testUser");
    Preferences.reset("testUser");

    // Set to true so we make http requests
    ChatManager.setChatLiteracy(true);

    // This needs to happen before database configuration.
    // ignoring cleanup until testing complete
    globalCleanup =
        new Cleanups(
            withDataFile("cheesefax.xml"),
            withDataFile("easyfax.xml"),
            withDataFile("onlyfax.xml"));

    // Configure
    FaxBotDatabase.configure();
  }

  @AfterAll
  public static void afterAll() {
    globalCleanup.close();
  }

  @BeforeEach
  public void initEach() {
    HttpClientWrapper.setupFakeClient();
    ChoiceManager.handlingChoice = false;
  }

  @Test
  void doesntErrorUnknownFaxbot() {
    var cleanups = new Cleanups(withProperty("lastSuccessfulFaxbot", "$FaxBot$"));

    try (cleanups) {
      // Start the process of faxing in a Knob Goblin Embezzler
      execute("embezzler");

      var requests = getRequests();

      // Assert that the first faxbot we try is a known faxbot
      assertGetRequest(
          requests.get(0),
          "/submitnewchat.php",
          "pwd=&playerid=0&graf=/whois+" + FaxBotDatabase.getFaxbot(0).getName());
    }
  }

  static Stream<String> provideFaxbotNames() {
    return FaxBotDatabase.faxbots.stream().map(FaxBotDatabase.FaxBot::getName);
  }

  @ParameterizedTest
  @MethodSource("provideFaxbotNames")
  void usesLastSuccessfulFaxbot(String lastFaxbot) {
    var cleanups = new Cleanups(withProperty("lastSuccessfulFaxbot", lastFaxbot));

    try (cleanups) {
      // Start the process of faxing in a Knob Goblin Embezzler
      execute("embezzler");

      var requests = getRequests();

      // Assert that the first faxbot we try, is the faxbot that was last successful
      assertGetRequest(
          requests.get(0), "/submitnewchat.php", "pwd=&playerid=0&graf=/whois+" + lastFaxbot);
    }
  }

  private static boolean isWhois(final HttpRequest request) {
    return request.uri().getPath().equals("/submitnewchat.php")
        && String.valueOf(request.uri().getQuery()).contains("/whois");
  }

  private static FakeHttpResponse<String> wrongFaxThatWillNotGoBack(final HttpRequest request) {
    if (isWhois(request)) {
      return new FakeHttpResponse<>("This player is currently online.");
    }
    String path = request.uri().getPath();
    if (path.equals("/clan_viplounge.php")) {
      String body = getPostRequestBody(request);
      if (body.contains("preaction=receivefax")) {
        return new FakeHttpResponse<>(html("request/test_clan_fax_receive.html"));
      }
      if (body.contains("preaction=sendfax")) {
        return new FakeHttpResponse<>("");
      }
      return new FakeHttpResponse<>("You approach the fax machine.");
    }
    if (path.equals("/desc_item.php")) {
      return new FakeHttpResponse<>(html("request/test_desc_item_photocopied_mariachi.html"));
    }
    return new FakeHttpResponse<>("");
  }

  private static List<HttpRequest> whoisRequests() {
    return getRequests().stream().filter(FaxbotCommandTest::isWhois).toList();
  }

  @Test
  void stopsAskingBotsWhenWrongFaxWillNotGoBack() {
    var cleanups =
        new Cleanups(
            withContinuationState(),
            withProperty("lastSuccessfulFaxbot", "Easyfax"),
            withProperty("faxbotTimeout", 0),
            withProperty("photocopyMonster", ""),
            withItem(ItemPool.VIP_LOUNGE_KEY),
            withItem(ItemPool.PHOTOCOPIED_MONSTER, 0));

    try (cleanups) {
      HttpClientWrapper.fakeClientBuilder.client.setResponseFunc(
          FaxbotCommandTest::wrongFaxThatWillNotGoBack);

      execute("Knob Goblin Embezzler");

      assertContinueState();
      assertThat(whoisRequests(), hasSize(1));
    }
  }

  @Test
  void ashFaxbotStopsAskingBotsWhenWrongFaxWillNotGoBack() {
    var cleanups =
        new Cleanups(
            withContinuationState(),
            withProperty("lastSuccessfulFaxbot", "Easyfax"),
            withProperty("faxbotTimeout", 0),
            withProperty("photocopyMonster", ""),
            withItem(ItemPool.VIP_LOUNGE_KEY),
            withItem(ItemPool.PHOTOCOPIED_MONSTER, 0));

    try (cleanups) {
      HttpClientWrapper.fakeClientBuilder.client.setResponseFunc(
          FaxbotCommandTest::wrongFaxThatWillNotGoBack);

      CliCaller.callCli("ashq", "faxbot($monster[Knob Goblin Embezzler])");

      assertContinueState();
      assertThat(whoisRequests(), hasSize(1));
    }
  }
}
