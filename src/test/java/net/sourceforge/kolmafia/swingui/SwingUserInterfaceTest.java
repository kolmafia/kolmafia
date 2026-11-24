package net.sourceforge.kolmafia.swingui;

import static internal.helpers.Player.withSwingUserInterface;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.maximizer.Maximizer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SwingUserInterfaceTest {
  @AfterEach
  void afterEach() {
    Maximizer.setExpression(null);
  }

  @Test
  void showsTheMaximizerExpression() {
    try (var cleanups = withSwingUserInterface()) {
      Maximizer.setExpression("cold res");
    }

    assertThat(MaximizerFrame.expressionSelect.getSelectedItem(), is("cold res"));
  }

  @Test
  void choosingAnExpressionSetsTheMaximizer() {
    MaximizerFrame.expressionSelect.setSelectedItem("hot res");

    assertThat(Maximizer.getExpression(), is("hot res"));
  }

  @Test
  void knowsTheEventDispatchThread() throws Exception {
    var ui = new SwingUserInterface();
    var onDispatchThread = new AtomicBoolean();

    SwingUtilities.invokeAndWait(() -> onDispatchThread.set(ui.isEventDispatchThread()));

    assertThat(onDispatchThread.get(), is(true));
    assertThat(ui.isEventDispatchThread(), is(false));
  }
}
