package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

public class JsRefCommandTest extends AbstractCommandTestBase {
  public JsRefCommandTest() {
    this.command = "jsref";
  }

  @Test
  void documentedFunctionShowsDescription() {
    String output = execute("print");
    assertThat(output, containsString("Prints a message to the CLI and session log."));
  }

  @Test
  void undocumentedFunctionShowsNoDescription() {
    String output = execute("is_adventuring");
    assertThat(output, containsString("isAdventuring"));
    assertThat(output, not(containsString("//")));
  }

  @Test
  void showsSignatureForDocumentedFunction() {
    String output = execute("user_confirm");
    assertThat(output, containsString("userConfirm"));
    assertThat(output, containsString("Prompts the user with a yes/no confirmation dialog."));
  }
}
