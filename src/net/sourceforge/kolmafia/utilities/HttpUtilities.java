package net.sourceforge.kolmafia.utilities;

import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;

public class HttpUtilities {
  private HttpUtilities() {}

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
