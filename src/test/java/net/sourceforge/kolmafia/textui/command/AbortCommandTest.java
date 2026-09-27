package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

public class AbortCommandTest extends AbstractCommandTestBase {

  public AbortCommandTest() {
    this.command = "abort";
  }

  @Test
  public void abortsWithDefaultMessage() {
    String output = execute("");

    assertThat(output, containsString("Script abort"));
  }

  @Test
  public void abortsWithGivenMessage() {
    String output = execute("custom error");

    assertThat(output, containsString("custom error"));
  }
}
