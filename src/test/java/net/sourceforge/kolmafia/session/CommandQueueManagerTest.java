package net.sourceforge.kolmafia.session;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommandQueueManagerTest {
  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("CommandQueueManagerTest");
  }

  @AfterEach
  void afterEach() {
    KoLmafia.forceContinue();
  }

  private static void waitFor(final String property, final String value) throws Exception {
    long deadline = System.currentTimeMillis() + 10000;
    while (!Preferences.getString(property).equals(value)
        && System.currentTimeMillis() < deadline) {
      Thread.sleep(10);
    }
  }

  @Test
  void ignoresEmptyCommand() {
    CommandQueueManager.executeCommand("");

    assertThat(CommandQueueManager.hasQueuedCommands(), is(false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"abort", "--"})
  void abortDeclaresWorldPeaceWithoutQueueing(String command) {
    CommandQueueManager.executeCommand(command);

    assertThat(KoLmafia.refusesContinue(), is(true));
    assertThat(CommandQueueManager.hasQueuedCommands(), is(false));
  }

  @Test
  void runsQueuedCommandsInOrder() throws Exception {
    Preferences.setString("_commandQueueTest", "");

    CommandQueueManager.executeCommand("set _commandQueueTest = first");
    CommandQueueManager.executeCommand("set _commandQueueTest = second");
    waitFor("_commandQueueTest", "second");

    assertThat(Preferences.getString("_commandQueueTest"), is("second"));
  }
}
