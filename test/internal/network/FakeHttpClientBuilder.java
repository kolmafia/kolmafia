package internal.network;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.time.Duration;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

public class FakeHttpClientBuilder implements Builder {

  public FakeHttpClient client;

  @Override
  public Builder cookieHandler(CookieHandler cookieHandler) {
    return this;
  }

  @Override
  public Builder connectTimeout(Duration duration) {
    return this;
  }

  @Override
  public Builder sslContext(SSLContext sslContext) {
    return this;
  }

  @Override
  public Builder sslParameters(SSLParameters sslParameters) {
    return this;
  }

  @Override
  public Builder executor(Executor executor) {
    return this;
  }

  @Override
  public Builder followRedirects(Redirect policy) {
    return this;
  }

  @Override
  public Builder version(Version version) {
    return this;
  }

  @Override
  public Builder priority(int priority) {
    return this;
  }

  @Override
  public Builder proxy(ProxySelector proxySelector) {
    return this;
  }

  @Override
  public Builder authenticator(Authenticator authenticator) {
    return this;
  }

  @Override
  public HttpClient build() {
    this.client = new FakeHttpClient();
    return this.client;
  }
}
