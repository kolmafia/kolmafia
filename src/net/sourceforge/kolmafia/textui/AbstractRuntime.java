package net.sourceforge.kolmafia.textui;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.RelayRequest;
import net.sourceforge.kolmafia.request.SendMailRequest;
import net.sourceforge.kolmafia.textui.parsetree.Value;
import net.sourceforge.kolmafia.utilities.NullStream;

public abstract class AbstractRuntime implements ScriptRuntime {
  private State runtimeState = State.EXIT;

  // For relay scripts.
  private RelayRequest relayRequest = null;
  private StringBuffer serverReplyBuffer = null;

  // For use by RuntimeLibrary's CLI command batching feature
  private LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched;

  private int traceIndentation = 0;

  @Override
  public Value execute(final String functionName, final Object[] parameters) {
    return execute(functionName, parameters, true);
  }

  @Override
  public abstract Value execute(
      final String functionName, final Object[] parameters, final boolean executeTopLevel);

  @Override
  public abstract ScriptException runtimeException(final String message);

  @Override
  public abstract ScriptException runtimeException2(final String message1, final String message2);

  @Override
  public void initializeRelayScript(final RelayRequest request) {
    this.relayRequest = request;
    if (this.serverReplyBuffer == null) {
      this.serverReplyBuffer = new StringBuffer();
    } else {
      this.serverReplyBuffer.setLength(0);
    }

    // Allow a relay script to execute regardless of error state
    KoLmafia.forceContinue();
  }

  @Override
  public RelayRequest getRelayRequest() {
    return relayRequest;
  }

  @Override
  public StringBuffer getServerReplyBuffer() {
    return serverReplyBuffer;
  }

  @Override
  public void finishRelayScript() {
    this.relayRequest = null;
    this.serverReplyBuffer = null;
  }

  @Override
  public void cloneRelayScript(final ScriptRuntime caller) {
    this.finishRelayScript();
    if (caller != null) {
      this.relayRequest = caller.getRelayRequest();
      this.serverReplyBuffer = caller.getServerReplyBuffer();
    }
  }

  @Override
  public State getState() {
    return runtimeState;
  }

  @Override
  public void setState(final State newState) {
    runtimeState = newState;
  }

  @Override
  public LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> getBatched() {
    return batched;
  }

  @Override
  public void setBatched(LinkedHashMap<String, LinkedHashMap<String, StringBuilder>> batched) {
    this.batched = batched;
  }

  // **************** Tracing *****************

  @Override
  public final void resetTracing() {
    this.traceIndentation = 0;
  }

  private static final String indentation = " " + " " + " ";

  public static void indentLine(final PrintStream stream, final int indent) {
    if (stream != null && stream != NullStream.INSTANCE) {
      for (int i = 0; i < indent; ++i) {
        stream.print(indentation);
      }
    }
  }

  protected void indentLine(final int indent) {
    if (ScriptRuntime.isTracing()) {
      AbstractRuntime.indentLine(ScriptRuntime.traceStream.getStream(), indent);
    }
  }

  @Override
  public final void traceIndent() {
    this.traceIndentation++;
  }

  @Override
  public final void traceUnindent() {
    this.traceIndentation--;
  }

  @Override
  public final void trace(final String string) {
    if (ScriptRuntime.isTracing()) {
      this.indentLine(this.traceIndentation);
      traceStream.println(string);
    }
  }

  public static void handleNotify(
      String scriptFilename, String scriptName, String notifyRecipient) {
    if (!Preferences.getBoolean("permitScriptNotify")) {
      return;
    }

    String notifyList = Preferences.getString("previousNotifyList");
    String notifyKey = (scriptFilename == null) ? "<>" : "<" + scriptFilename + ">";
    if (notifyRecipient != null && !notifyRecipient.isBlank() && !notifyList.contains(notifyKey)) {
      Preferences.setString("previousNotifyList", notifyList + notifyKey);

      String message =
          "I have opted to let you know that I have chosen to run <"
              + scriptName
              + ">.  Thanks for writing this script!";
      SendMailRequest notifier = new SendMailRequest(notifyRecipient, message);
      RequestThread.postRequest(notifier);
    }
  }

  public record SinceStatus(String status, String message) {}

  public static SinceStatus handleSince(String revision, String filename) {
    try {
      if (revision.startsWith("r")) { // revision
        revision = revision.substring(1);
        int targetRevision = Integer.parseInt(revision);
        int currentRevision = StaticEntity.getRevision();
        // A revision of zero means you're probably running in a debugger, in which
        // case you should be able to run anything.
        if (currentRevision != 0 && currentRevision < targetRevision) {
          String template =
              "'%s' requires revision r%s of kolmafia or higher (current: r%s).  Up-to-date builds can be found at https://github.com/kolmafia/kolmafia/releases/.";
          return new SinceStatus(
              "SINCE", String.format(template, filename, targetRevision, currentRevision));
        }
      } else { // version (or syntax error)
        String[] target = revision.split("\\.");
        if (target.length != 2) {
          return new SinceStatus("SYNTAX", "invalid 'since' format");
        }

        int targetMajor = Integer.parseInt(target[0]);
        int targetMinor = Integer.parseInt(target[1]);

        if (targetMajor > 21 || targetMajor == 21 && targetMinor > 9) {
          return new SinceStatus(
              "SYNTAX", "invalid 'since' format (21.09 was the final point release)");
        }
      }
    } catch (NumberFormatException e) {
      return new SinceStatus("SYNTAX", "invalid 'since' format");
    }

    return new SinceStatus("OK", null);
  }
}
