package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class BugbearsCommandTest extends AbstractCommandTestBase {
  public BugbearsCommandTest() {
    this.command = "bugbears";
  }

  @Test
  public void listBugbearLocations() {
    String output = execute("");

    assertThat(output, containsString("Mothership Zone"));
    assertThat(output, containsString("The Penultimate Fantasy Airship"));
    assertThat(output, containsString("Post-Cyrpt Cemetary"));
  }
}
