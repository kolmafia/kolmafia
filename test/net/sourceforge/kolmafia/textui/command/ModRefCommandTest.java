package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

public class ModRefCommandTest extends AbstractCommandTestBase {

  public ModRefCommandTest() {
    this.command = "modref";
  }

  @Test
  public void showsAllModifiers() {
    var mods = execute("");

    // a selection
    assertThat(mods, containsString("Monster Level Percent"));
    assertThat(mods, containsString("Cloathing"));
    assertThat(mods, containsString("Drops Meat"));
  }

  @Test
  public void showsModifiersForItem() {
    var mods = execute("Cargo Cultist Shorts");

    // a selection
    assertThat(mods, containsString("Muscle: +6, Mysticality: +6, Moxie: +6"));
  }
}
