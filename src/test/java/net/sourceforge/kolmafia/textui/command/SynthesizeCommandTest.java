package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

public class SynthesizeCommandTest extends AbstractCommandTestBase {

  public SynthesizeCommandTest() {
    this.command = "synthesize";
  }

  @Test
  public void checksCandyUsage() {
    String output = execute("yummy tummy, crimbo candied pecan", true);

    assertContinueState();
    assertThat(output, containsString("Item 'Yummy Tummy bean' is a simple candy."));
    assertThat(output, containsString("Item 'Crimbo candied pecan' is a complex candy."));
    assertThat(
        output,
        containsString(
            "Synthesizing those two candies will give you 30 turns of Synthesis: Strong (Mus +300%)"));
  }
}
