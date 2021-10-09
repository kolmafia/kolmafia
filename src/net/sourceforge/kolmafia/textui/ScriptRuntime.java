package net.sourceforge.kolmafia.textui;

import java.io.PrintStream;
import java.util.Date;
import java.util.LinkedHashMap;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.NullStream;

public interface ScriptRuntime {
  enum State {
    NORMAL,
    RETURN,
    BREAK,
    CONTINUE,
    EXIT
  }

  class PrintStreamWrapper {
    private PrintStream stream;

    public PrintStreamWrapper(PrintStream stream) {
      this.stream = stream;
    }

    public PrintStream getStream() {
      return stream;
    }

    public void setStream(PrintStream stream) {
      this.stream = stream;
    }

    public void println(final String string) {
      this.stream.println(string);
    }
  }

  // GLOBAL control of tracing
  PrintStreamWrapper traceStream = new PrintStreamWrapper(NullStream.INSTANCE);

  static boolean isTracing() {
    return ScriptRuntime.traceStream.getStream() != NullStream.INSTANCE;
  }

  static void openTraceStream() {
    ScriptRuntime.traceStream.setStream(
        RequestLogger.openStream(
            "ASH_" + KoLConstants.DAILY_FORMAT.format(new Date()) + ".txt",
            ScriptRuntime.traceStream.getStream(),
            true));
  }

  static void println(final String string) {
    ScriptRuntime.traceStream.println(string);
  }

  static void closeTraceStream() {
    RequestLogger.closeStream(ScriptRuntime.traceStream.getStream());
    ScriptRuntime.traceStream.setStream(NullStream.INSTANCE);
  }

  Value execute(final String functionName, final Object[] parameters);

  Value execute(
      final String functionName, final Object[] parameters, final boolean executeTopLevel);

  ScriptException runtimeException(final String message);

  ScriptException runtimeException2(final String message1, final String message2);

  void initializeRelayScript(final RelayRequest request);

  RelayRequest getRelayRequest();

  StringBuffer getServerReplyBuffer();

  void finishRelayScript();

  void cloneRelayScript(final ScriptRuntime caller);

  State getState();

  void setState(final State newState);

  LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> getBatched();

  void setBatched(LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched);

  void resetTracing();

  void traceIndent();

  void traceUnindent();

  void trace(final String string);
}
