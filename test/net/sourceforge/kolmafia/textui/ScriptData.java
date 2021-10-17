package net.sourceforge.kolmafia.textui;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.params.provider.Arguments;

public abstract class ScriptData {
  /** Shortcut method for the creation of a valid script containing the given tokens. */
  public static Arguments valid(final String desc, final String script, final List<String> tokens) {
    return Arguments.of(new ValidScriptData(desc, script, tokens));
  }

  /** Shortcut method for the creation of an invalid script failing with the given error message. */
  public static Arguments invalid(final String desc, final String script, final String errorText) {
    return Arguments.of(new InvalidScriptData(desc, script, errorText));
  }

  public final String desc;
  public final Parser parser;

  private ScriptData(final String description, final String script) {
    this.desc = description;
    ByteArrayInputStream istream =
        new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
    this.parser = new Parser(/*scriptFile=*/ null, /*stream=*/ istream, /*imports=*/ null);
  }

  public static class ValidScriptData extends ScriptData {
    public final List<String> tokens;

    private ValidScriptData(
        final String description, final String script, final List<String> tokens) {
      super(description, script);
      this.tokens = tokens;
    }
  }

  public static class InvalidScriptData extends ScriptData {
    public final String errorText;

    private InvalidScriptData(
        final String description, final String script, final String errorText) {
      super(description, script);
      this.errorText = errorText;
    }
  }
}
