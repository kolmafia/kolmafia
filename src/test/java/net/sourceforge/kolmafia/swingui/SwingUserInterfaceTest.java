package net.sourceforge.kolmafia.swingui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class SwingUserInterfaceTest {
  @Test
  void knowsTheEventDispatchThread() throws Exception {
    var ui = new SwingUserInterface();
    var onDispatchThread = new AtomicBoolean();

    SwingUtilities.invokeAndWait(() -> onDispatchThread.set(ui.isEventDispatchThread()));

    assertThat(onDispatchThread.get(), is(true));
    assertThat(ui.isEventDispatchThread(), is(false));
  }
}
