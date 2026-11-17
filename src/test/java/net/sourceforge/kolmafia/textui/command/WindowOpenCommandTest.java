package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withUserInterface;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;

import internal.helpers.Cleanups;
import java.util.ArrayList;
import java.util.List;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.ui.UserInterface;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class WindowOpenCommandTest extends AbstractCommandTestBase {
  private final List<String> windows = new ArrayList<>();

  @BeforeAll
  public static void beforeAll() {
    KoLCharacter.reset("WindowOpenCommandTest");
  }

  private Cleanups withWindowsRecorded() {
    return withUserInterface(
        new UserInterface() {
          @Override
          public void showWindow(final String frameName) {
            windows.add(frameName);
          }
        });
  }

  @Test
  void startsChatWithoutAWindow() {
    this.command = "chat";

    try (var cleanups = withWindowsRecorded()) {
      String output = execute("");

      assertThat(output, containsString("Initializing chat interface..."));
      assertThat(windows, empty());
    }
  }

  @Test
  void opensTheOptionsWindow() {
    this.command = "options";

    try (var cleanups = withWindowsRecorded()) {
      execute("");

      assertThat(windows, contains("OptionsFrame"));
    }
  }
}
