package net.sourceforge.kolmafia.swingui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.is;

import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CommandDisplayFrameTest {
  @AfterEach
  void afterEach() {
    KoLmafia.forceContinue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"clear", "cls", "reset"})
  void clearWorksAfterAbort(String command) throws Exception {
    KoLConstants.commandBuffer.append("KoLmafia declares world peace.");
    KoLmafia.updateDisplay(MafiaState.ABORT, "KoLmafia declares world peace.");

    CommandDisplayFrame.executeCommand(command);
    SwingUtilities.invokeAndWait(() -> {});

    assertThat(KoLConstants.commandBuffer.getContent(), is(emptyString()));
  }
}
