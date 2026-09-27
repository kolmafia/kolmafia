package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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

  @Test
  public void mentionsChangedBooleanModifiers() {
    var spec = execute("equip Mer-kin scholar mask");

    assertThat(spec, containsString("Adventure Underwater"));
    assertThat(spec, not(containsString("Never Fumble")));
  }
}
