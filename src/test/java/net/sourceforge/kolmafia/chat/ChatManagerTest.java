package net.sourceforge.kolmafia.chat;

import static internal.helpers.Player.withUserInterface;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import internal.helpers.Cleanups;
import java.util.ArrayList;
import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ui.UserInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChatManagerTest {
  private static final String TESTUSERNAME = "ChatManager TestUser";

  private static class RecordingUserInterface implements UserInterface {
    final List<String> calls = new ArrayList<>();

    @Override
    public void closeChat() {
      calls.add("closeChat");
    }

    @Override
    public void showTriviaContestants(
        final String title, final LockableListModel<String> contestants) {
      calls.add(title);
    }
  }

  @BeforeEach
  void beforeEach() {
    KoLCharacter.reset(TESTUSERNAME);
  }

  @AfterEach
  void afterEach() {
    ChatManager.stopTriviaGame();
  }

  @Test
  void showsTheTriviaContestants() {
    var ui = new RecordingUserInterface();

    try (var cleanups = withUserInterface(ui)) {
      ChatManager.startTriviaGame();
    }

    assertThat(ui.calls.size(), is(1));
    assertThat(ui.calls.get(0), startsWith("Contestants for [trivia"));
  }

  @Test
  void tellsTheUserInterfaceWhenChatCloses() {
    var ui = new RecordingUserInterface();

    try (var cleanups = withUserInterface(ui)) {
      ChatManager.dispose();
    }

    assertThat(ui.calls, contains("closeChat"));
  }

  private static Cleanups withImmediateUserInterface() {
    return withUserInterface(
        new UserInterface() {
          @Override
          public void invokeLater(final Runnable runnable) {
            runnable.run();
          }
        });
  }

  // KoL's JSON chat names the recipient with spaces; ChatPoller fills it in with underscores
  @ParameterizedTest
  @ValueSource(strings = {TESTUSERNAME, "ChatManager_TestUser"})
  void filesAPrivateMessageToMeUnderItsSender(final String recipient) {
    try (var cleanups = withImmediateUserInterface()) {
      ChatManager.processMessage(new ChatMessage("Other Player", recipient, recipient, false));

      assertThat(ChatManager.getBuffer("other player").getContent(), containsString(recipient));
    }
  }

  @Test
  void filesAPrivateMessageToAPlayerWithASimilarNameUnderThatPlayer() {
    try (var cleanups = withImmediateUserInterface()) {
      ChatManager.processMessage(
          new ChatMessage(TESTUSERNAME, TESTUSERNAME + "s", "to similar name", false));

      assertThat(
          ChatManager.getBuffer("chatmanager testusers").getContent(),
          containsString("to similar name"));
      assertThat(
          ChatManager.getBuffer("other player").getContent(),
          not(containsString("to similar name")));
    }
  }
}
