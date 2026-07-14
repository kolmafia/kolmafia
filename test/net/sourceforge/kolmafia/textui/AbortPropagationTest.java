package net.sourceforge.kolmafia.textui;

import static internal.helpers.Player.withProperty;
import static internal.matchers.StringMatcher.containsStringTimes;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import internal.helpers.RequestLoggerOutput;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.textui.javascript.AbortException;
import net.sourceforge.kolmafia.textui.javascript.AshStub;
import net.sourceforge.kolmafia.textui.javascript.JavascriptRuntime;
import net.sourceforge.kolmafia.textui.parsetree.Try;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mozilla.javascript.JavaScriptException;

/**
 * When a script fails, KoLmafia has three situations and ASH and Javascript are meant to agree on
 * all three.
 *
 * <ul>
 *   <li>ASH is the reference behavior, the nested classes below test the situation in both
 *       languages.
 *   <li><b>Situation 1: Thrown exceptions</b>
 *       <ul>
 *         <li>A library function throws, bad script, thrown error. This is handled by the language,
 *             unwinding the script's own stack, the state stays {@link MafiaState#CONTINUE}. Both
 *             languages may catch these and carry on.
 *       </ul>
 *   <li><b>Situation 2: Soft stops</b>
 *       <ul>
 *         <li>Something failed and set {@link MafiaState#ERROR}, {@link KoLmafia#refusesContinue()}
 *             is still false. Unlike situation '1' there is no exception to catch, only the state.
 *             Exceptions can't cross a script boundary, so a nested script dying to one just leaves
 *             the caller looking at ERROR.
 *         <li>ASH halts unless the script handles the error, either by capturing the return value
 *             or wrapping it in a catch.
 *         <li>Both reset the state to CONTINUE.
 *         <li>Javascript treats every ASH call as captured, so the call just returns false, the
 *             state is reset, and the script decides what to do.
 *       </ul>
 *   <li><b>Situation 3: Aborts, or hard stops</b>
 *       <ul>
 *         <li>On {@link MafiaState#ABORT}, or the user hit stop {@link StaticEntity#userAborted},
 *             so {@link KoLmafia#refusesContinue} is true. These halt every script on the stack, no
 *             matter how deeply nested, eg 'betweenBattleScript' run from inside adv1(), and no
 *             language may swallow them. The state stays ABORT until the outermost request sequence
 *             closes.
 *       </ul>
 *   <li>For the tests we run them quietly, as otherwise it prints "Returned:" whose
 *       updateDisplay(CONTINUE, ...) resets the state.
 *   <li>Each run is wrapped in a RequestThread because hook scripts run inside an existing request
 *       sequence.
 *       <ul>
 *         <li>Otherwise, {@link RequestThread#closeRequestSequence} would treat the nested call as
 *             the outermost sequence and reset the state.
 *         <li>See {@link Situation3Aborts#abortClearsWhenTheOutermostRequestSequenceCloses}.
 *       </ul>
 * </ul>
 */
public class AbortPropagationTest {
  @BeforeEach
  void beforeEach() {
    KoLmafia.forceContinue();
    KoLCharacter.reset("AbortPropagationTest");
    Preferences.reset("AbortPropagationTest");
  }

  @AfterEach
  void afterEach() {
    KoLmafia.forceContinue();
  }

  /**
   * @param output Output of the script
   * @param stateDuringRun The state right after the script returned, while still inside the request
   *     sequence, as a caller like {@link RuntimeLibrary#adv1} would observe
   * @param stateAfterRun the state after the outer request sequence closed
   */
  private record ScriptRun(String output, MafiaState stateDuringRun, MafiaState stateAfterRun) {}

  private static ScriptRun run(Runnable script) {
    RequestLoggerOutput.startStream();
    MafiaState stateDuringRun;
    Integer seq = RequestThread.openRequestSequence();
    try {
      script.run();
      stateDuringRun = StaticEntity.getContinuationState();
    } finally {
      RequestThread.closeRequestSequence(seq);
    }
    MafiaState stateAfterRun = StaticEntity.getContinuationState();
    String output = RequestLoggerOutput.stopStream();
    return new ScriptRun(output, stateDuringRun, stateAfterRun);
  }

  private static ScriptRun runAsh(String source) {
    return run(
        () -> {
          var istream = new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8));
          AshRuntime interpreter = new AshRuntime();
          interpreter.validate(null, istream);
          interpreter.execute("main", null);
        });
  }

  private static ScriptRun runJs(String source) {
    return run(() -> new JavascriptRuntime(source).execute(null, null, true));
  }

  @Nested
  class Situation1ThrownExceptions {
    @Test
    void ashCatchCapturesAThrownException() {
      var run =
          runAsh(
              """
                string err = catch {
                  session_logs(-1);
                  print("Unseen");
                };
                print("Caught: " + (err != ""));
                print("After exception");
                """);

      assertThat(run.output(), not(containsString("Unseen")));
      assertThat(run.output(), containsString("Caught: true"));
      assertThat(run.output(), containsString("After exception"));
      assertThat(run.stateDuringRun(), is(MafiaState.CONTINUE));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    @Test
    void javascriptTryCatchCatchesAThrownException() {
      var run =
          runJs(
              """
                try {
                  sessionLogs(-1);
                  print("Unseen");
                } catch (e) {
                  print("Caught: " + (e !== ""));
                }
                print("After exception");
                """);

      assertThat(run.output(), not(containsString("Unseen")));
      assertThat(run.output(), containsString("Caught: true"));
      assertThat(run.output(), containsString("After exception"));
      assertThat(run.stateDuringRun(), is(MafiaState.CONTINUE));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }
  }

  /**
   * The nested script fails at runtime, its interpreter turns the thrown error into a soft stop
   * {@link MafiaState#ERROR} which the calling script sees.
   */
  @Nested
  class Situation2SoftFailures {
    @Test
    void ashHaltsWhenANestedScriptFails() {
      var run =
          runAsh(
              """
                cli_execute("ashq session_logs(-1)");
                print("After error");
                """);

      assertThat(run.output(), not(containsString("After error")));
      assertThat(run.stateDuringRun(), is(MafiaState.ERROR));
      assertThat(run.stateAfterRun(), is(MafiaState.ERROR));
    }

    @Test
    void ashContinuesWhenANestedScriptFailureIsCaptured() {
      var run =
          runAsh(
              """
                boolean success = cli_execute("ashq session_logs(-1)");
                print("Captured: " + success);
                print("After error");
                """);

      assertThat(run.output(), containsString("Captured: false"));
      assertThat(run.output(), containsString("After error"));
      assertThat(run.stateDuringRun(), is(MafiaState.CONTINUE));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    @Test
    void ashContinuesWhenANestedScriptFailureIsCaught() {
      var run =
          runAsh(
              """
                string err = catch cli_execute("ashq session_logs(-1)");
                print("Caught: " + (err != ""));
                print("After error");
                """);

      assertThat(run.output(), containsString("Caught: true"));
      assertThat(run.output(), containsString("After error"));
      assertThat(run.stateDuringRun(), is(MafiaState.CONTINUE));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    /**
     * Javascript's version has every call already behaves like a captured one {@link AshStub#call},
     * the failure just comes back as false, the state is cleared, and the script carries on.
     */
    @Test
    void javascriptContinuesWhenANestedScriptFails() {
      var run =
          runJs(
              """
                var success = cliExecute("jsq throw new Error('Nested error')");
                print("Captured: " + success);
                print("After error");
                """);

      assertThat(run.output(), containsString("Nested error"));
      assertThat(run.output(), containsString("Captured: false"));
      assertThat(run.output(), containsString("After error"));
      assertThat(run.stateDuringRun(), is(MafiaState.CONTINUE));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }
  }

  @Nested
  class Situation3Aborts {
    @Test
    void ashHaltsWhenANestedAshScriptAborts() {
      var run =
          runAsh(
              """
                cli_execute("ashq abort('Nested abort')");
                print("After abort");
                """);

      assertThat(run.output(), containsString("Nested abort"));
      assertThat(run.output(), not(containsString("After abort")));
      assertThat(run.stateDuringRun(), is(MafiaState.ABORT));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    @Test
    void ashHaltsWhenANestedJavascriptScriptAborts() {
      var run =
          runAsh(
              """
                cli_execute("jsq abort('Nested abort')");
                print("After abort");
                """);

      assertThat(run.output(), containsString("Nested abort"));
      assertThat(run.output(), not(containsString("After abort")));
      assertThat(run.stateDuringRun(), is(MafiaState.ABORT));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    @Test
    void javascriptHaltsWhenANestedAshScriptAborts() {
      var run =
          runJs(
              """
                cliExecute("ashq abort('Nested abort')");
                print("After abort");
                """);

      assertThat(run.output(), containsString("Nested abort"));
      assertThat(run.output(), not(containsString("After abort")));
      assertThat(run.stateDuringRun(), is(MafiaState.ABORT));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    @Test
    void javascriptHaltsWhenANestedJavascriptScriptAborts() {
      var run =
          runJs(
              """
                cliExecute("jsq abort('Nested abort')");
                print("After abort");
                """);

      assertThat(run.output(), containsString("Nested abort"));
      assertThat(run.output(), not(containsString("After abort")));
      assertThat(run.stateDuringRun(), is(MafiaState.ABORT));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    /**
     * Capturing the return value recovers from a soft failure; it shouldn't recover from an abort.
     */
    @Test
    void ashCaptureCannotSwallowAnAbort() {
      var run =
          runAsh(
              """
                boolean success = cli_execute("ashq abort('Nested abort')");
                print("After abort");
                """);

      assertThat(run.output(), containsString("Nested abort"));
      assertThat(run.output(), not(containsString("After abort")));
      assertThat(run.stateDuringRun(), is(MafiaState.ABORT));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    /** ASH's catch recovers from a soft failure, same deal, it shouldn't recover from an abort. */
    @Test
    void ashCatchCannotSwallowAnAbort() {
      var run =
          runAsh(
              """
                string err = catch {
                  cli_execute("ashq abort('Nested abort')");
                };
                print("After abort");
                """);

      assertThat(run.output(), containsString("Nested abort"));
      assertThat(run.output(), not(containsString("After abort")));
      assertThat(run.stateDuringRun(), is(MafiaState.ABORT));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    /**
     * A Javascript catch never sees the abort at all, Rhino only offers the exception to finally
     * blocks. Same as ASH's catch refusing to swallow one {@link AshRuntime#captureValue}. The
     * catch block here is skipped entirely and the script still halts.
     */
    @Test
    void javascriptTryCatchCannotSwallowAnAbort() {
      var run =
          runJs(
              """
                try {
                  cliExecute("jsq abort('Nested abort')");
                } catch (e) {
                  print("Caught: " + e);
                }
                print("After abort");
                """);

      assertThat(run.output(), containsString("Nested abort"));
      assertThat(run.output(), not(containsString("Caught:")));
      assertThat(run.output(), not(containsString("After abort")));
      assertThat(run.stateDuringRun(), is(MafiaState.ABORT));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    /** With both clauses present, catch is skipped, finally still runs, and the script halts. */
    @Test
    void javascriptTryCatchFinallySkipsCatchAndRunsFinallyOnAbort() {
      var run =
          runJs(
              """
                try {
                  cliExecute("jsq abort('Nested abort')");
                } catch (e) {
                  print("Caught: " + e);
                } finally {
                  print("Finally ran");
                }
                print("After abort");
                """);

      assertThat(run.output(), containsString("Nested abort"));
      assertThat(run.output(), not(containsString("Caught:")));
      assertThat(run.output(), containsString("Finally ran"));
      assertThat(run.output(), not(containsString("After abort")));
      assertThat(run.stateDuringRun(), is(MafiaState.ABORT));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    /**
     * The reference behavior, from {@link Try}. A 'finally' clause always runs, the abort state is
     * suspended while it does so its ash calls still work, then restored after so the script still
     * halts.
     */
    @Test
    void ashFinallyRunsCleanupAfterAnAbortAndStillHalts() {
      var run =
          runAsh(
              """
                try {
                  cli_execute("ashq abort('Nested abort')");
                } finally {
                  print("Finally ran");
                }
                print("After abort");
                """);

      assertThat(run.output(), containsString("Nested abort"));
      assertThat(run.output(), containsString("Finally ran"));
      assertThat(run.output(), not(containsString("After abort")));
      assertThat(run.stateDuringRun(), is(MafiaState.ABORT));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    /**
     * Javascript matching ASH, finally runs its cleanup (ash calls included), and the script still
     * halts.
     */
    @Test
    void javascriptFinallyRunsCleanupAfterAnAbortAndStillHalts() {
      var run =
          runJs(
              """
                try {
                  cliExecute("jsq abort('Nested abort')");
                } finally {
                  print("Finally ran");
                }
                print("After abort");
                """);

      assertThat(run.output(), containsString("Nested abort"));
      assertThat(run.output(), containsString("Finally ran"));
      assertThat(run.output(), not(containsString("After abort")));
      assertThat(run.stateDuringRun(), is(MafiaState.ABORT));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }

    /**
     * As in ASH, an abort prints its message once (from {@link RuntimeLibrary#abort(ScriptRuntime)}
     * itself) and unwinds quietly. With 'printStackOnAbort', the script the abort originated in
     * also prints its stack trace, the Javascript version of {@link
     * AshRuntime#setState(ScriptRuntime.State)}'s stack printing. Nested scripts share the context,
     * so only the first trace gets printed, the remaining traces are suppressed for the current
     * thread.
     *
     * <p>Note that the preference does not control if non-abort error stacks are printed.
     */
    @Test
    void abortPrintsOneStackTraceWhenPreferenceSet() {
      try (var cleanups = withProperty("printStackOnAbort", true)) {
        var run =
            runJs(
                """
                    cliExecute("jsq abort('Nested abort')");
                    print("After abort");
                    """);

        // We expect only one "Script aborted: Nested abort\n<trace>"
        assertThat(run.output(), containsStringTimes("Script aborted", 1));
        assertThat(run.output(), containsStringTimes("Nested abort", 2));
        assertThat(
            run.output(), containsStringTimes("Nested abort\nScript aborted: Nested abort\n", 1));
        assertThat(run.output(), containsStringTimes("at command line", 2));
      }
    }

    @Test
    void abortTraceUnwindingCleanlyCloses() {
      try (var cleanups = withProperty("printStackOnAbort", true)) {
        var first = runJs("cliExecute(\"jsq abort('Nested abort')\");");
        KoLmafia.forceContinue();
        var second = runJs("cliExecute(\"jsq abort('Nested abort')\");");

        assertThat(first.output(), containsStringTimes("Script aborted: Nested abort", 1));
        assertThat(first.output(), containsStringTimes("at command line", 2));
        assertThat(second.output(), containsStringTimes("Script aborted: Nested abort", 1));
        assertThat(second.output(), containsStringTimes("at command line", 2));
      }
    }

    /**
     * Without 'printStackOnAbort' the unwind is silent past {@link
     * RuntimeLibrary#abort(ScriptRuntime)}'s own message, same as ASH.
     */
    @Test
    void abortUnwindsQuietlyByDefault() {
      try (var cleanups = withProperty("printStackOnAbort", false)) {
        var run =
            runJs(
                """
                cliExecute("jsq abort('Nested abort')");
                print("After abort");
                """);

        assertThat(run.output(), containsStringTimes("Nested abort", 1));
        assertThat(run.output(), not(containsString("at command line")));
      }
    }

    /**
     * An abort outlives every script on the stack but not the command itself. Once the outer
     * request sequence closes, the state resets so the next command starts clean, the same life an
     * ASH abort has. We don't get stuck aborted.
     */
    @Test
    void abortClearsWhenTheOutermostRequestSequenceCloses() {
      var run =
          runJs(
              """
                cliExecute("jsq abort('Nested abort')");
                """);

      assertThat(run.stateDuringRun(), is(MafiaState.ABORT));
      assertThat(run.stateAfterRun(), is(MafiaState.CONTINUE));
    }
  }

  /**
   * Tests for the two methods everything above rests on. Each language has one place that decides
   * whether a stopped state can be cleared so the script continues, {@link
   * JavascriptRuntime#checkInterrupted} for Javascript and {@link AshRuntime#captureValue} for ASH.
   * A {@link KoLmafia#refusesContinue} state is never cleared.
   */
  @Nested
  class Checkpoints {
    @Test
    void javascriptCheckpointClearsASoftStopAndThrows() {
      KoLmafia.updateDisplay(MafiaState.ERROR, "Soft failure, situation 2");

      assertThrows(JavaScriptException.class, JavascriptRuntime::checkInterrupted);
      assertThat(StaticEntity.getContinuationState(), is(MafiaState.CONTINUE));
    }

    /**
     * A hard stop is held in the exception, so 'finally' can still make ash calls while the script
     * unwinds, then restored at the script boundary {@link JavascriptRuntime#executeFunction}. The
     * same save / forceContinue / restore that ASH's {@link Try} does on a 'finally' clause.
     */
    @Test
    void javascriptCheckpointSuspendsAnAbortIntoTheException() {
      KoLmafia.updateDisplay(MafiaState.ABORT, "Hard abort, situation 3");

      var exception = assertThrows(AbortException.class, JavascriptRuntime::checkInterrupted);
      assertThat(exception.getMessage(), is("Hard abort, situation 3"));
      assertThat(StaticEntity.getContinuationState(), is(MafiaState.CONTINUE));

      exception.restore();
      assertThat(StaticEntity.getContinuationState(), is(MafiaState.ABORT));
    }

    @Test
    void javascriptCheckpointIsQuietWhileContinuePermitted() {
      assertDoesNotThrow(JavascriptRuntime::checkInterrupted);
      assertThat(StaticEntity.getContinuationState(), is(MafiaState.CONTINUE));
    }

    @Test
    void ashCaptureClearsASoftStop() {
      var interpreter = new AshRuntime();
      KoLmafia.updateDisplay(MafiaState.ERROR, "Soft failure, situation 2");

      interpreter.captureValue(new Value(false));

      assertThat(StaticEntity.getContinuationState(), is(MafiaState.CONTINUE));
      assertThat(interpreter.getState(), is(ScriptRuntime.State.NORMAL));
    }

    @Test
    void ashCaptureRefusesAnAbort() {
      var interpreter = new AshRuntime();
      KoLmafia.updateDisplay(MafiaState.ABORT, "Hard abort, situation 3");

      interpreter.captureValue(new Value(false));

      assertThat(StaticEntity.getContinuationState(), is(MafiaState.ABORT));
      assertThat(interpreter.getState(), is(ScriptRuntime.State.EXIT));
    }
  }
}
