package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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
  public void showsFilteredModifiers() {
    var mods = execute("*Drop*");

    // a selection
    assertThat(mods, not(containsString("Monster Level Percent")));
    assertThat(mods, containsString("Item Drop"));
    assertThat(mods, containsString("Drops Meat"));
  }

  @Test
  public void showsModifiersForItem() {
    var mods = execute("Cargo Cultist Shorts");

    assertThat(mods, containsString("Muscle: +6, Mysticality: +6, Moxie: +6"));
  }

  @Test
  public void showsModifiersForFilteredItem() {
    var mods = execute("Initiative Cargo Cultist Shorts");

    assertThat(mods, containsString(">66.0<"));
    assertThat(mods, not(containsString(">6.0<")));
  }
}
