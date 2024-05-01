package net.sourceforge.kolmafia.textui.command;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class JavaScriptCommandTest extends AbstractCommandTestBase {
  public JavaScriptCommandTest() {
    this.command = "js";
  }

  @Nested
  class WeirdNullInteractions {
    @Test
    public void printsNull() {
      String output = execute("null");

      assertThat(output, containsString("Returned: null"));
    }

    @Test
    public void printsUndefined() {
      String output = execute("undefined");

      assertThat(output, startsWith("Returned: undefined"));
    }

    @Test
    public void emptyArray() {
      String output = execute("[]");

      assertThat(output, startsWith("Returned: aggregate null [0]"));
    }

    @Test
    public void nullContainingArray() {
      String output = execute("[\"hello\", null]");

      assertThat(
          output,
          equalTo(
              """
          Returned: aggregate string [2]
          0 => hello
          1 => null
          """));
    }

    @Test
    public void nullStartingArray() {
      String output = execute("[null, \"hello\"]");

      assertThat(
          output,
          equalTo(
              """
          Returned: aggregate null [2]
          0 => null
          1 => hello
          """));
    }
  }
}
