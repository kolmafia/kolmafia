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
    assertThat(output, not(containsString("/**")));
  }

  @Test
  void showsSignatureForDocumentedFunction() {
    String output = execute("user_confirm");
    assertThat(output, containsString("userConfirm"));
    assertThat(output, containsString("Prompts the user with a yes/no confirmation dialog."));
  }

  @Test
  void showsParameterDescriptions() {
    String output = execute("contains_text");
    assertThat(output, containsString("@param source The string to search in"));
    assertThat(output, containsString("@param search The substring to search for"));
  }

  @Test
  void docBlockAppearsAboveSignature() {
    String output = execute("contains_text");
    int docPos = output.indexOf("/**");
    int sigPos = output.indexOf("containsText(");
    assertThat("doc block should appear before signature", docPos < sigPos);
  }

  @Test
  void usesTypescriptStyleSignature() {
    String output = execute("contains_text");
    assertThat(
        output, containsString("function containsText(source: string, search: string): boolean;"));
  }
}
