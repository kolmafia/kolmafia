package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;

import net.sourceforge.kolmafia.request.GenericRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HelpCommandTest extends AbstractCommandTestBase {
  @BeforeEach
  public void initEach() {
    // Stop requests from actually running
    GenericRequest.sessionId = null;
  }

  public HelpCommandTest() {
    this.command = "help";
  }

  @Test
  void Refresh() {
    String output = execute("refresh");

    assertContinueState();
    assertThat(output, startsWith("refresh all |"));
  }
}
