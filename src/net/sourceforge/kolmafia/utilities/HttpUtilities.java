package net.sourceforge.kolmafia.utilities;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;

public class HttpUtilities {
  private HttpUtilities() {}

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

  @FunctionalInterface
  public interface ClientFactory {
    HttpClient.Builder getClientBuilder();
  }

  private static ClientFactory clientFactory =
      () -> HttpClient.newBuilder().followRedirects(Redirect.ALWAYS);

  // Injects custom URL handling logic, especially in tests.
  public static void setClientBuilder(ClientFactory function) {
    HttpUtilities.clientFactory = function;
  }

  public static HttpClient.Builder getClientBuilder() {
    return HttpUtilities.clientFactory.getClientBuilder();
  }
}
