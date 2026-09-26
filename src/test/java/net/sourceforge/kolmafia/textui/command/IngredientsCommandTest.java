package net.sourceforge.kolmafia.textui.command;

import static internal.helpers.Player.withItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import internal.helpers.Cleanups;
import org.junit.jupiter.api.Test;

public class IngredientsCommandTest extends AbstractCommandTestBase {

  public IngredientsCommandTest() {
    this.command = "ingredients";
  }

  @Test
  public void simpleIngredientsCase() {
    String output = execute("one-winged stab bat");

    assertThat(
        output,
        containsString("<b>one-winged stab bat</b>: <i>bat wing (0/1)</i>, <i>batblade (0/1)</i>"));
  }

  @Test
  public void presentIngredientsHaveNoItalics() {
    var cleanups = new Cleanups(withItem("batblade"), withItem("bat wing"));
    try (cleanups) {
      String output = execute("one-winged stab bat");

      assertThat(output, containsString("<b>one-winged stab bat</b>: bat wing, batblade"));
    }
  }

  @Test
  public void multipleRecursiveIngredients() {
    String output = execute("stick of firewood, bundle of firewood");

    assertThat(
        output,
        containsString("1. <b>stick of firewood (10)</b>: <i>bundle of firewood (0/1)</i>"));
    assertThat(
        output, containsString("2. <b>bundle of firewood</b>: <i>stick of firewood (0/10)</i>"));
  }

  @Test
  public void errorCaseIsNumbered() {
    String output = execute("stick of firewood, wad of Crovacite");

    assertThat(output, containsString("2. This item cannot be created: <b>wad of Crovacite</b>"));
  }
}
