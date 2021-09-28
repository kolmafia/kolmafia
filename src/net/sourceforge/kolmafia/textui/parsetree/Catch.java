package net.sourceforge.kolmafia.textui.parsetree;

import java.io.PrintStream;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.textui.AshRuntime;
import net.sourceforge.kolmafia.textui.DataTypes;
import net.sourceforge.kolmafia.textui.ScriptException;
import net.sourceforge.kolmafia.textui.ScriptRuntime;

public class Catch extends Value {
  private final Command node;

  public Catch(final Command node) {
    super(DataTypes.STRING_TYPE);
    this.node = node;
  }

  @Override
  public Value execute(final AshRuntime interpreter) {
    if (!KoLmafia.permitsContinue()) {
      interpreter.setState(ScriptRuntime.State.EXIT);
      return null;
    }

    interpreter.traceIndent();
    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Evaluating catch body");
    }

    String errorMessage = "";
    Value scopeValue = null;

    try {
      KoLmafia.lastMessage = "";
      scopeValue = this.node.execute(interpreter);
    } catch (ScriptException se) {
      errorMessage = "SCRIPT: " + se.getMessage();
    } catch (Exception e) {
      errorMessage = "JAVA: " + e.getMessage();
    }

    // We may have thrown and caught an error within the catch block.
    // Return message only if currently cannot continue.
    if (errorMessage.equals("") && !KoLmafia.permitsContinue()) {
      // Capture the value, permitting continuation
      errorMessage = "CAPTURE: " + KoLmafia.lastMessage;
      interpreter.captureValue(scopeValue);
    }

    if (ScriptRuntime.isTracing()) {
      interpreter.trace("Returning '" + errorMessage + "'");
    }

    interpreter.traceUnindent();

    // If user aborted or exited, don't catch it
    if (interpreter.getState() == ScriptRuntime.State.EXIT) {
      return null;
    }

    return new Value(errorMessage);
  }

  @Override
  public boolean assertBarrier() {
    return this.node.assertBarrier();
  }

  @Override
  public boolean assertBreakable() {
    return this.node.assertBreakable();
  }

  @Override
  public String toString() {
    return "catch";
  }

  @Override
  public void print(final PrintStream stream, final int indent) {
    AshRuntime.indentLine(stream, indent);
    stream.println("<CATCH>");
    this.node.print(stream, indent + 1);
  }
}
