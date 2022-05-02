package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

public class ModiferListCommandTest extends AbstractCommandTestBase {

  public ModiferListCommandTest() {
    this.command = "modifies";
  }

  @Test
  public void noMatchIsError() {
    String output = execute("fhqwhgads");

    assertErrorState();
    assertThat(output.trim(), equalTo("No matching modifiers - use 'modref' to list."));
  }

  @Test
  public void tooManyMatchIsError() {
    String output = execute("");

    assertErrorState();
    assertThat(output.trim(), equalTo("Too many matching modifiers - use 'modref' to list."));
  }

  @Test
  public void matchDisplaysAllModifiers() {
    String output = execute("Hot Spell Damage");

    assertContinueState();
    assertThat(output, containsString("You're Not Cooking"));
    assertThat(output, containsString("half-melted spoon"));
  }
}
