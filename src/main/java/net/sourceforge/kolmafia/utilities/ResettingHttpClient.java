package net.sourceforge.kolmafia.utilities;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

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
    client.close();

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
}
