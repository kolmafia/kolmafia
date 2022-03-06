package internal.network;

import java.net.URI;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import javax.net.ssl.SSLSession;

public class FakeHttpResponse<T> implements HttpResponse<T> {

  private final T body;

  public FakeHttpResponse(T body) {
    this.body = body;
  }

  @Override
  public int statusCode() {
    return 0;
  }

  @Override
  public HttpRequest request() {
    return null;
  }

  @Override
  public Optional<HttpResponse<T>> previousResponse() {
    return Optional.empty();
  }

  @Override
  public HttpHeaders headers() {
    return null;
  }

  @Override
  public T body() {
    return body;
  }

  @Override
  public Optional<SSLSession> sslSession() {
    return Optional.empty();
  }

  @Override
  public URI uri() {
    return null;
  }

  @Override
  public Version version() {
    return null;
  }
}
