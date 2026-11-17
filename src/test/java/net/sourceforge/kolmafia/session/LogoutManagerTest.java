package net.sourceforge.kolmafia.session;

import static internal.helpers.Player.withUserInterface;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ui.UserInterface;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class LogoutManagerTest {
  private static class WindowCloser implements UserInterface {
    int closed = 0;

    @Override
    public void closeSessionWindows() {
      this.closed++;
    }
  }

  @AfterAll
  static void afterAll() {
    KoLCharacter.reset("");
  }

  @Test
  void closesSessionWindowsWhenSomeoneIsLoggedIn() {
    var ui = new WindowCloser();

    try (var cleanups = withUserInterface(ui)) {
      KoLCharacter.reset("LogoutManagerTest");
      LogoutManager.prepare();

      assertThat(ui.closed, is(1));
    }
  }

  @Test
  void leavesWindowsAloneWhenNobodyIsLoggedIn() {
    var ui = new WindowCloser();

    try (var cleanups = withUserInterface(ui)) {
      KoLCharacter.reset("");
      LogoutManager.prepare();

      assertThat(ui.closed, is(0));
    }
  }
}
