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
              JavaScript evaluator exception: Null / undefined values in JS arrays cannot be converted to ASH.
              Returned: null
              """));
    }

    @Test
    public void nullStartingArray() {
      String output = execute("[null, \"hello\"]");

      assertThat(
          output,
          equalTo(
              """
              JavaScript evaluator exception: Null / undefined values in JS arrays cannot be converted to ASH.
              Returned: null
              """));
    }

    @Test
    public void undefinedContainingArray() {
      String output = execute("[\"hello\", undefined]");

      assertThat(
          output,
          equalTo(
              """
              JavaScript evaluator exception: Null / undefined values in JS arrays cannot be converted to ASH.
              Returned: null
              """));
    }

    @Test
    public void undefinedValueInObject() {
      String output = execute("o = {\"hello\": undefined, \"2\": 1}");

      assertThat(
          output,
          equalTo(
              """
              JavaScript evaluator exception: Null / undefined values in JS objects cannot be converted to ASH.
              Returned: null
              """));
    }

    @Test
    public void nullValueInObject() {
      String output = execute("o = {\"hello\": null}");

      assertThat(
          output,
          equalTo(
              """
              JavaScript evaluator exception: Null / undefined values in JS objects cannot be converted to ASH.
              Returned: null
              """));
    }

    @Test
    public void nullValueInStringifiedObject() {
      String output = execute("JSON.stringify({\"hello\": null})");

      assertThat(output, equalTo("""
              Returned: {"hello":null}
              """));
    }

    @Test
    public void nullCoalescing() {
      // Rhino doesn't support `a ?? b` yet.
      String output = execute("null || true");

      assertThat(output, equalTo("""
              Returned: true
              """));
    }
  }
}
