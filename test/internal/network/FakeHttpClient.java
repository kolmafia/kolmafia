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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.SubmissionPublisher;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class FakeHttpClient extends HttpClient {

  private final List<HttpRequest> requests = new ArrayList<>();
  private final Queue<FakeHttpResponse<String>> responses = new LinkedList<>();
  private final Map<String, FakeHttpResponse<String>> responseMap = new HashMap<>();

  public void addResponse(int responseCode, String response) {
    addResponse(responseCode, new HashMap<>(), response);
  }

  public void addResponse(int responseCode, Map<String, List<String>> headers, String response) {
    responses.add(new FakeHttpResponse<>(responseCode, headers, response));
  }

  public void addResponse(FakeHttpResponse<String> response) {
    responses.add(response);
  }

  public void addResponse(String uri, FakeHttpResponse<String> response) {
    responseMap.put(uri, response);
  }

  public List<HttpRequest> getRequests() {
    return requests;
  }

  public HttpRequest getLastRequest() {
    if (requests.size() == 0) {
      return null;
    }

    return requests.get(requests.size() - 1);
  }

  public void clear() {
    this.requests.clear();
    this.responses.clear();
  }

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
    this.requests.add(request);
    var response = responseMap.getOrDefault(request.uri().toString(), responses.poll());

    var responseCode = response != null ? response.statusCode() : 0;
    var headers = response != null ? response.rawHeaders() : new HashMap<String, List<String>>();
    var responseBody = response != null ? response.body() : "";

    T body;

    var subscriber = responseBodyHandler.apply(new ResponseInfoImpl(responseCode, headers));
    var publisher = new SubmissionPublisher<List<ByteBuffer>>();
    publisher.subscribe(subscriber);
    publisher.submit(List.of(ByteBuffer.wrap(responseBody.getBytes(StandardCharsets.UTF_8))));
    publisher.close();
    try {
      body = subscriber.getBody().toCompletableFuture().get();
    } catch (InterruptedException | ExecutionException e) {
      body = null;
    }
    return new FakeHttpResponse<>(responseCode, headers, body);
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

    int statusCode;
    Map<String, List<String>> headers;

    public ResponseInfoImpl(int statusCode, Map<String, List<String>> headers) {
      this.statusCode = statusCode;
      this.headers = headers;
    }

    @Override
    public int statusCode() {
      return statusCode;
    }

    @Override
    public HttpHeaders headers() {
      return HttpHeaders.of(this.headers, (x, y) -> true);
    }

    @Override
    public Version version() {
      return Version.HTTP_1_1;
    }
  }
}
