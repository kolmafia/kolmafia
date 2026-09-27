package net.sourceforge.kolmafia.textui.javascript;

import java.io.Serial;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.parsetree.Try;
import org.mozilla.javascript.EvaluatorException;

/**
 * Thrown to stop the whole script when KoLmafia aborts. Rhino does not let JavaScript code catch a
 * plain RuntimeException, but still runs its 'finally' clauses as the exception unwinds. ASH treats
 * an abort the same way.
 *
 * <p>{@link #suspend} moves the abort out of the global continuation state and into this exception,
 * so 'finally' blocks can still make library calls while the script unwinds. At the script
 * boundary, {@link JavascriptRuntime#executeFunction} calls {@link #restore} to put the abort back
 * for whoever ran the script.
 *
 * <p>ASH's {@link Try} does the same around a 'finally' clause; Saves the abort, forceContinue, run
 * the clause, restore.
 */
public class AbortException extends RuntimeException {
  @Serial private static final long serialVersionUID = 1L;

  private final MafiaState continuationState;
  private final boolean userAborted;
  private final String scriptStackTrace;

  private AbortException(final String message) {
    super(message);
    this.continuationState = StaticEntity.getContinuationState();
    this.userAborted = StaticEntity.userAborted;
    // Use Rhino's way to capture the current script stack.
    // See NativeConsole.js_trace
    this.scriptStackTrace = new EvaluatorException(message).getScriptStackTrace();
  }

  /** Moves the pending abort out of the global continuation state, into the returned exception. */
  static AbortException suspend() {
    AbortException abort = new AbortException(KoLmafia.getLastMessage());
    KoLmafia.forceContinue();
    return abort;
  }

  /** Puts the suspended abort back, as {@link #suspend} found it. */
  public void restore() {
    StaticEntity.setContinuationState(this.continuationState);
    StaticEntity.userAborted = this.userAborted;
  }

  String getScriptStackTrace() {
    return this.scriptStackTrace;
  }
}
