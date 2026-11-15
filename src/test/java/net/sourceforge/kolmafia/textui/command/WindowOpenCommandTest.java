package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;

import internal.helpers.Cleanups;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ui.UserInterface;
import net.sourceforge.kolmafia.ui.UserInterfaceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class WindowOpenCommandTest extends AbstractCommandTestBase {
  private final List<String> windows = new ArrayList<>();

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("WindowOpenCommandTest");
  }

  private Cleanups withUserInterface() {
    UserInterfaceManager.setUserInterface(
        new UserInterface() {
          @Override
          public void showWindow(final String frameName) {
            windows.add(frameName);
          }
        });
    return new Cleanups(() -> UserInterfaceManager.setUserInterface(null));
  }

  @Test
  void startsChatWithoutAWindow() {
    this.command = "chat";

    try (var cleanups = withUserInterface()) {
      String output = execute("");

      assertThat(output, containsString("Initializing chat interface..."));
      assertThat(windows, empty());
    }
  }

  @Test
  void opensTheOptionsWindow() {
    this.command = "options";

    try (var cleanups = withUserInterface()) {
      execute("");

      assertThat(windows, contains("OptionsFrame"));
    }
  }
}
