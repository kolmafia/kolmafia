package net.sourceforge.kolmafia.chat;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ui.UserInterface;
import net.sourceforge.kolmafia.ui.UserInterfaceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ChatParserTest {
  private static final String PRIVATE_MESSAGE =
      "<a target=mainpane href=\"showplayer.php?who=1\"><font color=blue><b>Easyfax"
          + " (private):</b></font></a> <font color=blue>Your fax is ready.</font><br>";

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("ChatParserTest");
  }

  private static class BeepCounter implements UserInterface {
    int beeps = 0;

    @Override
    public void beep() {
      this.beeps++;
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void beepsForPrivateMessagesWhenAsked(final boolean chatBeep) {
    var ui = new BeepCounter();
    UserInterfaceManager.setUserInterface(ui);
    var cleanups =
        new Cleanups(
            withProperty("chatBeep", chatBeep),
            new Cleanups(() -> UserInterfaceManager.setUserInterface(null)));

    try (cleanups) {
      List<ChatMessage> messages = ChatParser.parseLines(PRIVATE_MESSAGE);

      assertThat(messages, hasSize(1));
      assertThat(messages.get(0).getSender(), is("Easyfax"));
      assertThat(messages.get(0).getContent(), is("Your fax is ready."));
      assertThat(ui.beeps, is(chatBeep ? 1 : 0));
    }
  }
}
