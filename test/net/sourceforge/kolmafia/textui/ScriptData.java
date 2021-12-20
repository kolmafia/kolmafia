package net.sourceforge.kolmafia.textui;

import static org.eclipse.lsp4j.DiagnosticSeverity.Error;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.sourceforge.kolmafia.textui.parsetree.Scope;

public abstract class ScriptData {
  /**
   * Shortcut method for the creation of a valid script containing the given tokens, each of them
   * starting on its respective position.
   *
   * <p>Positions are serialized as {@code "L-C"} where L is the starting line, C is the starting
   * character, and both are 1-indexed.
   */
  public static ScriptData valid(
      final String desc,
      final String script,
      final List<String> tokens,
      final List<String> positions) {
    return new ValidScriptData(desc, script, tokens, positions);
  }

  /**
   * An extension of {@link #valid(String, String, List, List)}, allowing one to re-use tests while
   * appending additional tests to them aiming at confirming the location of its components.
   */
  public static ScriptData valid(
      final String desc,
      final String script,
      final List<String> tokens,
      final List<String> positions,
      final Consumer<Scope> locationTests) {
    return new ValidScriptDataWithLocationTests(desc, script, tokens, positions, locationTests);
  }

  /** Shortcut method for the creation of an invalid script failing with the given error message. */
  public static ScriptData invalid(
      final String desc,
      final String script,
      final String errorText,
      final String errorLocationString) {
    return new InvalidScriptData(desc, script, errorText, errorLocationString);
  }

  /**
   * Creates two invalid scripts failing with the given error messages.
   *
   * <p>The first script acts normally, in that only the first given error is looked at.
   *
   * <p>The <i>second</i> script, however, is meant to test the error filtering behaviour. It is a
   * script similar to the first, which, in addition to looking at the first given error of {@code
   * newScript}, looks at <b>all</b> the errors made by the script, asserting that <b>none</b> of
   * them match {@code errorText}, because it has (allegedly) been filtered due to a prior error.
   *
   * @param desc the description of the script
   * @param script the content of the first script
   * @param errorText the error message expected from the first script, and <i>not</i> from the
   *     second
   * @param errorLocationString the location of the error expected from the first script
   * @param newScript the content of the second script. Should be similar to the first
   * @param newErrorText the error message expected from the second script
   * @param newErrorLocationString the location of the error expected from the second script
   */
  public static ScriptData invalid(
      final String desc,
      final String script,
      final String errorText,
      final String errorLocationString,
      final String newScript,
      final String newErrorText,
      final String newErrorLocationString) {
    return new InvalidScriptDataWithErrorFilterTest(
        desc,
        script,
        errorText,
        errorLocationString,
        newScript,
        newErrorText,
        newErrorLocationString);
  }

  public final String desc;
  public final Parser parser;
  public final Scope scope;
  public final List<String> errors;

  /** Exception thrown by Parser.parse(), if any */
  public final Throwable parsingException;

  private ScriptData(final String description, final String script) {
    this.desc = description;
    ByteArrayInputStream istream =
        new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
    this.parser = new Parser(/*scriptFile=*/ null, /*stream=*/ istream, /*imports=*/ null);

    Scope scope;
    try {
      scope = this.parser.parse();
    } catch (Throwable e) {
      this.parsingException = e;
      this.scope = null;
      this.errors = null;
      return;
    }

    this.parsingException = null;
    this.scope = scope;
    this.errors =
        this.parser.getDiagnostics().stream()
            .filter(diagnostic -> diagnostic.severity == Error)
            .map(diagnostic -> diagnostic.toString())
            .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return this.desc;
  }

  public static class ValidScriptData extends ScriptData {
    public final List<String> tokens;
    public final List<String> positions;

    private ValidScriptData(
        final String description,
        final String script,
        final List<String> tokens,
        final List<String> positions) {
      super(description, script);
      this.tokens = tokens;
      this.positions = positions;
    }
  }

  public static class ValidScriptDataWithLocationTests extends ValidScriptData {
    final Consumer<Scope> locationTests;

    private ValidScriptDataWithLocationTests(
        final String description,
        final String script,
        final List<String> tokens,
        final List<String> positions,
        final Consumer<Scope> locationTests) {
      super(description, script, tokens, positions);
      this.locationTests = locationTests;
    }
  }

  public static class InvalidScriptData extends ScriptData {
    public final String errorText;
    public final String errorLocationString;

    private InvalidScriptData(
        final String description,
        final String script,
        final String errorText,
        final String errorLocationString) {
      super(description, script);
      this.errorText = errorText;
      this.errorLocationString = errorLocationString;
    }
  }

  public static class InvalidScriptDataWithErrorFilterTest extends InvalidScriptData {
    public final InvalidScriptData filteredScript;

    private InvalidScriptDataWithErrorFilterTest(
        final String description,
        final String script,
        final String errorText,
        final String errorLocationString,
        final String newScript,
        final String newErrorText,
        final String newErrorLocationString) {
      super(description, script, errorText, errorLocationString);
      this.filteredScript =
          new InvalidScriptData(
              description + " - filtered", newScript, newErrorText, newErrorLocationString);
    }
  }
}
