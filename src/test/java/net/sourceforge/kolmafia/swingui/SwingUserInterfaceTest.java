package net.sourceforge.kolmafia.swingui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

import internal.helpers.Cleanups;
import net.sourceforge.kolmafia.ui.UserInterfaceManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

@DisabledIf(
    value = "java.awt.GraphicsEnvironment#isHeadless",
    disabledReason = "headless environment")
class SwingUserInterfaceTest {
  @Test
  void isTheUserInterfaceFoundOnTheDesktop() {
    UserInterfaceManager.setUserInterface(null);

    try (var cleanups = new Cleanups(() -> UserInterfaceManager.setUserInterface(null))) {
      assertThat(UserInterfaceManager.getUserInterface(), instanceOf(SwingUserInterface.class));
    }
  }
}
