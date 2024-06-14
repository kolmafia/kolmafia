package net.sourceforge.kolmafia.utilities;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import net.sourceforge.kolmafia.StaticEntity;

public class ResettingHttpClient {

  private final AtomicInteger clientRequestsSent = new AtomicInteger();

  /**
   * At 10k client requests, the server will send a GOAWAY exception. We recreate the HttpClient
   * before that to avoid the problem.
   *
   * <p>The limit is set at 7k as the server may not acknowledge the new connection immediately
   */
  private static final int HTTP_CLIENT_REQUEST_LIMIT = 7000;

  private final Supplier<HttpClient> createClient;
  private HttpClient client;

  public ResettingHttpClient(Supplier<HttpClient> createClient) {
    this.createClient = createClient;
    this.client = createClient.get();
  }

  public void resetClient() {
    closeClient(client);

    this.client = createClient.get();
    clientRequestsSent.set(0);
  }

  public <T> HttpResponse<T> send(HttpRequest req, HttpResponse.BodyHandler<T> handler)
      throws IOException, InterruptedException {
    if (clientRequestsSent.incrementAndGet() >= HTTP_CLIENT_REQUEST_LIMIT) {
      resetClient();
    }

    return this.client.send(req, handler);
  }

  private void closeClient(HttpClient httpClient) {
    // Closing the client is a new java feature
    if (Runtime.version().feature() < 21) {
      return;
    }

    // As we compile against an older jdk, we must use reflection to access the method
    try {
      Method method = Class.forName("java.net.http.HttpClient").getMethod("close");
      method.invoke(httpClient);
    } catch (Exception ex) {
      // If this does error, we should log it as this means something has changed in java internals
      StaticEntity.printStackTrace(ex);
    }
  }
}
