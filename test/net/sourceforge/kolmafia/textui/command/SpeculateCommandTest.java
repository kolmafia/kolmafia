package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

public class SpeculateCommandTest extends AbstractCommandTestBase {

  public SpeculateCommandTest() {
    this.command = "speculate";
  }

  @Test
  public void mentionsChangedModifiers() {
    var spec = execute("equip disco mask");

    assertThat(spec, containsString("Moxie"));
    assertThat(spec, containsString("Buffed Moxie"));
  }
}
