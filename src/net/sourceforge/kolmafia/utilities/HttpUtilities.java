package net.sourceforge.kolmafia.utilities;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpUtilities {
  @FunctionalInterface
  public interface ConnectionFactory {
    HttpURLConnection openConnection(URL url) throws IOException;
  }

  private static ConnectionFactory factory = (URL url) -> (HttpURLConnection) url.openConnection();

  // Injects custom URL handling logic, especially in tests.
  public static void setOpen(ConnectionFactory function) {
    HttpUtilities.factory = function;
  }

  public static HttpURLConnection openConnection(URL url) throws IOException {
    return HttpUtilities.factory.openConnection(url);
  }
}
