package net.sourceforge.kolmafia.swingui;

import static internal.helpers.Player.withDataFile;
import static internal.helpers.Player.withHttpClientBuilder;
import static internal.helpers.Player.withItem;
import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import internal.network.FakeHttpClientBuilder;
import internal.network.FakeHttpResponse;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.chat.ChatPoller;
import net.sourceforge.kolmafia.objectpool.ItemPool;
import net.sourceforge.kolmafia.persistence.FaxBotDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FaxRequestFrameTest {
  // Non-JSON chat responses, as ChatPoller requests them
  private static final String NO_MESSAGES = "<!--lastseen:100-->";
  private static final String FAX_READY =
      "<a target=mainpane href=\"showplayer.php?who=3690803\"><font color=blue><b>OnlyFax"
          + " (private):</b></font></a> Your fax is ready.<br><!--lastseen:101-->";

  private Cleanups cleanups;

  @BeforeEach
  void beforeEach() {
    cleanups = new Cleanups(withDataFile("onlyfax.xml"));
    FaxBotDatabase.reconfigure();
    ChatManager.setChatLiteracy(true);
    ChatPoller.reset();
    ChatPoller.lastServerPoll = new Date(0);
    ChatPoller.lastIdlePoll = 0;
  }

  @AfterEach
  void afterEach() {
    ChatPoller.lastServerPoll = new Date(0);
    ChatPoller.lastIdlePoll = 0;
    cleanups.close();
  }

  private static String path(HttpRequest request) {
    return request.uri().getPath();
  }

  private static List<String> steps(List<HttpRequest> requests) {
    return requests.stream()
        .map(
            request -> {
              String uri = URLDecoder.decode(request.uri().toString(), StandardCharsets.UTF_8);
              return switch (path(request)) {
                case "/newchatmessages.php" -> "poll chat";
                case "/submitnewchat.php" -> uri.contains("/msg 3690803 ") ? "ask OnlyFax" : "chat";
                case "/clan_viplounge.php" -> "fax machine";
                default -> "other";
              };
            })
        .filter(step -> !step.equals("other") && !step.equals("chat"))
        .toList();
  }

  @Test
  void readsFaxbotReplyWhenChatIsNotOpen() {
    var builder = new FakeHttpClientBuilder();
    boolean[] requested = {false};
    builder.client.setResponseFunc(
        request -> {
          return switch (path(request)) {
            case "/clan_viplounge.php" ->
                new FakeHttpResponse<>(200, "You approach the fax machine.");
            case "/submitnewchat.php" -> {
              String uri = URLDecoder.decode(request.uri().toString(), StandardCharsets.UTF_8);
              requested[0] |= uri.contains("/msg 3690803 ");
              yield new FakeHttpResponse<>(200, "");
            }
            case "/newchatmessages.php" ->
                new FakeHttpResponse<>(200, requested[0] ? FAX_READY : NO_MESSAGES);
            default -> new FakeHttpResponse<>(200, "");
          };
        });

    var monster =
        FaxBotDatabase.getFaxbot("OnlyFax").getMonsterByCommand("[1183]angry cavebugbear");

    try (var cleanups =
        new Cleanups(
            withHttpClientBuilder(builder),
            withItem(ItemPool.VIP_LOUNGE_KEY),
            withProperty("lastSuccessfulFaxbot", ""))) {
      assertThat(ChatManager.isRunning(), is(false));

      boolean result = FaxRequestFrame.requestFax("OnlyFax", monster, false);

      assertThat(result, is(true));
      assertThat(
          steps(builder.client.getRequests()),
          contains("fax machine", "poll chat", "ask OnlyFax", "poll chat", "fax machine"));
    }
  }

  @Test
  void doesNotPollWhenBrowserChatIsReading() {
    var builder = new FakeHttpClientBuilder();

    try (var cleanups = new Cleanups(withHttpClientBuilder(builder))) {
      ChatPoller.serverPolled();

      ChatPoller.pollIfIdle();

      assertThat(builder.client.getRequests(), empty());
    }
  }

  @Test
  void limitsIdlePollsToChatDelay() {
    var builder = new FakeHttpClientBuilder();
    builder.client.setResponseFunc(request -> new FakeHttpResponse<>(200, NO_MESSAGES));

    try (var cleanups = new Cleanups(withHttpClientBuilder(builder))) {
      ChatPoller.pollIfIdle();
      ChatPoller.pollIfIdle();

      assertThat(
          builder.client.getRequests().stream()
              .filter(request -> path(request).equals("/newchatmessages.php"))
              .toList(),
          hasSize(1));
    }
  }
}
