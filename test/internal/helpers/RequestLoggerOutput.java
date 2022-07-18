package internal.helpers;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import net.sourceforge.kolmafia.RequestLogger;

public class RequestLoggerOutput {

  private static ByteArrayOutputStream baos;

  public static void startStream() {
    baos = new ByteArrayOutputStream();
    RequestLogger.openCustom(new PrintStream(baos));
  }

  public static String stopStream() {
    RequestLogger.closeCustom();
    return baos.toString();
  }
}
