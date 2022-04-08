package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

class SendMessageCommandTest extends AbstractCommandTestBase {

  public SendMessageCommandTest() {
    this.command = "csend";
  }

  @Test
  public void itShouldParseMeatWithCommas() {
    String output = execute(" 1,000,000 meat to buffy");
    assertThat(output, containsString("Sending kmail to buffy..."));
    assertContinueState();
  }

  @Test
  public void itShouldParseMeatWithoutCommas() {
    String output = execute(" 1000000 meat to buffy");
    assertThat(output, containsString("Sending kmail to buffy..."));
    assertContinueState();
  }
}
