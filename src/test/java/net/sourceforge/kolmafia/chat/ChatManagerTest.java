package net.sourceforge.kolmafia.chat;

import static internal.helpers.Player.withUserInterface;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;

import java.util.ArrayList;
import java.util.List;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.ui.UserInterface;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ChatManagerTest {
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
}
