package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.Test;

public class RecipeCommandTest extends AbstractCommandTestBase {

  public RecipeCommandTest() {
    this.command = "recipe";
  }

  @Test
  public void errorsIfNoItems() {
    String output = execute("");

    assertThat(output, containsString("Need to provide an item to match"));
  }

  @Test
  public void errorsIfCannotBeCreated() {
    String output = execute("wad of Crovacite");

    assertThat(output, containsString("This item cannot be created: <b>wad of Crovacite</b>"));
  }

  @Test
  public void ignoresInvalidItems() {
    String output = execute("foobar, plain pizza");

    assertThat(output, containsString("2. <b>plain pizza</b>"));
  }

  @Test
  public void simpleRecipe() {
    String output = execute("one-winged stab bat");

    assertThat(
        output,
        containsString("<b>one-winged stab bat</b>: <i>[Meatpasting]</i> batblade + bat wing"));
  }

  @Test
  public void pizzaRecipe() {
    String output = execute("plain pizza");

    assertThat(
        output,
        containsString(
            "<b>plain pizza</b>: <i>[Cooking]</i> wad of dough + tomato<br>\u00a0\u00a0\u00a0<b>wad of dough</b>: <i>[rolling pin/unrolling pin]</i> flat dough<br>\u00a0\u00a0\u00a0<b>tomato</b>"));
  }

  @Test
  public void multipleRecursiveRecipes() {
    String output = execute("stick of firewood, bundle of firewood");

    assertThat(
        output,
        containsString("1. <b>stick of firewood (10)</b>: <i>[single-use]</i> bundle of firewood"));
    assertThat(
        output,
        containsString(
            "2. <b>bundle of firewood</b>: <i>[Coin Master purchase]</i> stick of firewood (10)"));
  }

  @Test
  public void errorCaseIsNumbered() {
    String output = execute("stick of firewood, wad of Crovacite");

    assertThat(output, containsString("2. This item cannot be created: <b>wad of Crovacite</b>"));
  }
}
