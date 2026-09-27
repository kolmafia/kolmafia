package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class EdServantCommandTest extends AbstractCommandTestBase {
  public EdServantCommandTest() {
    this.command = "servants";
  }

  @Test
  public void listAllServants() {
    String output = execute("");

    assertThat(output, containsString("Belly-Dancer"));
    assertThat(output, containsString("Level 14: Improves Ka drops"));
    assertThat(output, containsString("Level 21: Teaches you how to improve physical attacks"));
  }
}
