package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withProperty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.function.BooleanSupplier;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.preferences.Preferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommandQueueManagerTest {
  private static final String PROPERTY = "_commandQueueTest";

  @BeforeAll
  static void beforeAll() {
    KoLCharacter.reset("CommandQueueManagerTest");
  }

  @AfterEach
  void afterEach() {
    KoLmafia.forceContinue();
  }

  /** Waits up to ten seconds for the queue's thread to get there, and fails if it does not. */
  private static void waitUntil(final BooleanSupplier condition) throws InterruptedException {
    long deadline = System.currentTimeMillis() + 10000;
    while (!condition.getAsBoolean()) {
      assertThat("timed out waiting for the command queue", System.currentTimeMillis() < deadline);
      Thread.sleep(10);
    }
  }

  private static boolean propertyIs(final String value) {
    return Preferences.getString(PROPERTY).equals(value);
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
    try (var cleanups = withProperty(PROPERTY, "")) {
      CommandQueueManager.executeCommand("set " + PROPERTY + " = first");
      CommandQueueManager.executeCommand("set " + PROPERTY + " = second");

      // Run in the wrong order, the queue would finish with "first"
      waitUntil(() -> propertyIs("second") && !CommandQueueManager.hasQueuedCommands());

      assertThat(Preferences.getString(PROPERTY), is("second"));
    }
  }

  @Test
  void dropsQueuedCommandsWhenACommandAborts() throws Exception {
    try (var cleanups = withProperty(PROPERTY, "")) {
      // set takes the rest of its line, so it gets a command of its own. The second command is
      // still running when the third is queued; then it aborts.
      CommandQueueManager.executeCommand("set " + PROPERTY + " = started");
      CommandQueueManager.executeCommand("waitq 1; abort");
      CommandQueueManager.executeCommand("set " + PROPERTY + " = after");

      waitUntil(() -> !propertyIs(""));
      waitUntil(() -> !CommandQueueManager.hasQueuedCommands());

      assertThat(Preferences.getString(PROPERTY), is("started"));
    }
  }
}
