package internal.network;

import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.net.http.HttpResponse.ResponseInfo;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.SubmissionPublisher;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class FakeHttpClient extends HttpClient {

  public HttpRequest request;

  @Override
  public Optional<CookieHandler> cookieHandler() {
    return Optional.empty();
  }

  @Override
  public Optional<Duration> connectTimeout() {
    return Optional.empty();
  }

  @Override
  public Redirect followRedirects() {
    return null;
  }

  @Override
  public Optional<ProxySelector> proxy() {
    return Optional.empty();
  }

  @Override
  public SSLContext sslContext() {
    return null;
  }

  @Override
  public SSLParameters sslParameters() {
    return null;
  }

  @Override
  public Optional<Authenticator> authenticator() {
    return Optional.empty();
  }

  @Override
  public Version version() {
    return null;
  }

  @Override
  public Optional<Executor> executor() {
    return Optional.empty();
  }

  @Override
  public <T> HttpResponse<T> send(HttpRequest request, BodyHandler<T> responseBodyHandler)
      throws IOException, InterruptedException {
    this.request = request;

    T body;

    var response = "";
    var subscriber = responseBodyHandler.apply(new ResponseInfoImpl());
    var publisher = new SubmissionPublisher<List<ByteBuffer>>();
    publisher.subscribe(subscriber);
    publisher.submit(List.of(ByteBuffer.wrap(response.getBytes(StandardCharsets.UTF_8))));
    publisher.close();
    try {
      body = subscriber.getBody().toCompletableFuture().get();
    } catch (InterruptedException | ExecutionException e) {
      body = null;
    }
    return new FakeHttpResponse<>(body);
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
      HttpRequest request, BodyHandler<T> responseBodyHandler) {
    return null;
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
      HttpRequest request,
      BodyHandler<T> responseBodyHandler,
      PushPromiseHandler<T> pushPromiseHandler) {
    return null;
  }

  static class ResponseInfoImpl implements ResponseInfo {
    @Override
    public int statusCode() {
      return 0;
    }

    @Override
    public HttpHeaders headers() {
      return HttpHeaders.of(new HashMap<>(), (x, y) -> true);
    }

    @Override
    public Version version() {
      return Version.HTTP_1_1;
    }
  }
}
