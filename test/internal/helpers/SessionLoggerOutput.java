package internal.helpers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.RequestLogger;

public class SessionLoggerOutput {

  private static ByteArrayOutputStream baos;

  public static void startStream() {
    baos = new ByteArrayOutputStream();
    RequestLogger.setSessionStream(new PrintStream(baos));
  }

  public static String stopStream() {
    RequestLogger.closeSessionLog();
    return baos.toString();
  }
}
