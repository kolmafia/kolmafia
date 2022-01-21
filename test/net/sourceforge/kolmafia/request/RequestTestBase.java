package net.sourceforge.kolmafia.request;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.extensions.ForbidNetworkAccess;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import net.sourceforge.kolmafia.utilities.HttpUtilities.ConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

/**
 * A library with tools facilitating the test of network requests and our response to them.
 *
 * <p>Extend this class when making tests involving {@link GenericRequest} and/or its descendants.
 *
 * <ul>
 *   <li>Allows this class's tests to run {@link GenericRequest}s (otherwise blocked pre-login and
 *       due to {@link ForbidNetworkAccess}).
 *   <li>For {@link GenericRequest}s made locally, gives access to {@link
 *       RequestTestBase#expectSuccess}, allowing the simulation of running a {@link
 *       org.mockito.Mockito#spy spy} of that request.
 *   <li>For the rest, gives access to {@link #respondIf} and {@link #redirectIf}, causing future
 *       executed requests to succeed with the given response or redirection if the path of the URL
 *       submitted matches the submitted {@link UrlMatcher}s (generated using {@link
 *       #hasPath}/{@link #hasParam}).
 * </ul>
 */
public abstract class RequestTestBase {
  @Mock protected ConnectionFactory factory;
  private MockitoSession mockito;

  @BeforeAll
  static void setSessionId() {
    GenericRequest.sessionId = "fake session id";
  }

  @BeforeEach
  void setupSession() {
    mockito =
        Mockito.mockitoSession().initMocks(this).strictness(Strictness.STRICT_STUBS).startMocking();
    factory = mock(ConnectionFactory.class);
    HttpUtilities.setOpen(factory);
  }

  @AfterEach
  void tearDownSession() {
    ForbidNetworkAccess.blockNetwork();
    mockito.finishMocking();
  }

  // Inject expected success (responseCode = 200) response text.
  protected void expectSuccess(GenericRequest spy, String responseText) {
    doAnswer(
            invocation -> {
              GenericRequest m = (GenericRequest) invocation.getMock();
              m.responseCode = 200;
              m.responseText = responseText;
              // This is normally done by retrieveServerReply(), which is called by
              // externalExecute().
              m.processResponse();
              return null;
            })
        .when(spy)
        .externalExecute();
  }

  /**
   * Sets {@link #factory} to generate the given response if the path matches every one of {@code
   * matchers}.
   *
   * <p>Use {@link #hasPath} and/or {@link #hasParam} to generate them.
   */
  protected void respondIf(final String response, final UrlMatcher... matchers) {
    respondIf(response, Arrays.asList(matchers));
  }

  /**
   * Sets {@link #factory} to generate the given response if the path matches every one of {@code
   * matchers}.
   *
   * <p>Use {@link #hasPath} and/or {@link #hasParam} to generate them.
   */
  protected void respondIf(final String response, final Iterable<UrlMatcher> matchers) {
    prepareResponse(
        response,
        url -> {
          final ParsedUrl parsedUrl = new ParsedUrl(url);
          for (final UrlMatcher matcher : matchers) {
            if (!matcher.matches(parsedUrl)) {
              return false;
            }
          }
          return true;
        });
  }

  /**
   * Sets {@link #factory} to generate the given redirect if the path matches every one of {@code
   * matchers}.
   *
   * <p>Use {@link #hasPath} and/or {@link #hasParam} to generate them.
   */
  protected void redirectIf(final String response, final UrlMatcher... matchers) {
    redirectIf(response, Arrays.asList(matchers));
  }

  /**
   * Sets {@link #factory} to generate the given redirect if the path matches every one of {@code
   * matchers}.
   *
   * <p>Use {@link #hasPath} and/or {@link #hasParam} to generate them.
   */
  protected void redirectIf(final String redirectLocation, final Iterable<UrlMatcher> matchers) {
    prepareRedirect(
        redirectLocation,
        url -> {
          final ParsedUrl parsedUrl = new ParsedUrl(url);
          for (final UrlMatcher matcher : matchers) {
            if (!matcher.matches(parsedUrl)) {
              return false;
            }
          }
          return true;
        });
  }

  /**
   * Is true if the base of the URL matches {@code path}. The "base" is the part coming before the
   * {@code ?} (if any), e.g., the base of {@code inventory.php?which=2} is {@code inventory.php}
   */
  protected PathMatcher hasPath(final String path) {
    return new PathMatcher(path);
  }

  /**
   * Is true if one of the URL's parameters has a key matching {@code param}, and a value matching
   * {@code value}. The parameters are the part coming after the {@code ?} (if any), separated by
   * {@code &}s.
   *
   * <p>For example, in {@code inventory.php?which=2&foo&bar=&pwd=abc}, the parameters are {@code
   * which=2}, {@code foo}, {@code bar=} and {@code pwd=abc}.
   *
   * <p>Parameters are split into a key and a value, the key being what comes before the {@code =}
   * (if any), and the value being what's after (again, if any).
   *
   * <p>If {@code value} is {@code null}, the parameter's value is allowed to be anything.
   */
  protected ParamMatcher hasParam(final String param, final String value) {
    return new ParamMatcher(param, value);
  }

  /**
   * Is true if one of the URL's parameters has a key matching {@code param}, and a value matching
   * {@code value}. The parameters are the part coming after the {@code ?} (if any), separated by
   * {@code &}s.
   *
   * <p>For example, in {@code inventory.php?which=2&foo&bar=&pwd=abc}, the parameters are {@code
   * which=2}, {@code foo}, {@code bar=} and {@code pwd=abc}.
   *
   * <p>Parameters are split into a key and a value, the key being what comes before the {@code =}
   * (if any), and the value being what's after (again, if any).
   */
  protected ParamMatcher hasParam(final String param, final Pattern valueMatcher) {
    return new ParamMatcher(param, valueMatcher);
  }

  private void prepareResponse(final String response, final ArgumentMatcher<URL> matcher) {
    prepareConnection(new FakeResponse(response), matcher);
  }

  private void prepareRedirect(final String redirectLocation, final ArgumentMatcher<URL> matcher) {
    prepareConnection(new FakeRedirect(redirectLocation), matcher);
  }

  private void prepareConnection(
      final FakeConnection connection, final ArgumentMatcher<URL> matcher) {
    try {
      when(factory.openConnection(argThat(matcher))).thenReturn(connection);
    } catch (IOException e) {
    }
  }

  private abstract class FakeConnection extends HttpURLConnection {
    private final String responseText;

    private FakeConnection(final String responseText, final int responseCode) {
      super(null);
      this.responseText = responseText;
      this.responseCode = responseCode;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(this.responseText.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void disconnect() {}

    @Override
    public boolean usingProxy() {
      return false;
    }

    @Override
    public void connect() throws IOException {}
  }

  private class FakeResponse extends FakeConnection {
    private FakeResponse(final String responseText) {
      super(responseText, 200);
    }
  }

  private class FakeRedirect extends FakeConnection {
    private final String redirectLocation;

    private FakeRedirect(final String redirectLocation) {
      super("", 302);
      this.redirectLocation = redirectLocation;
    }

    @Override
    public String getHeaderField(final String name) {
      if ("Location".equals(name)) {
        return this.redirectLocation;
      }

      return super.getHeaderField(name);
    }
  }

  private class ParsedUrl {
    private final String path;
    private final Map<String, String> params = new HashMap<>();

    private ParsedUrl(final URL url) {
      this.path = url.getPath();
      final String query = url.getQuery();

      if (query != null) {
        final String[] paramsString = query.split("&");
        for (final String param : paramsString) {
          if (param.isEmpty()) {
            continue;
          }

          final String[] splitParam = param.split("=", 2);
          params.put(splitParam[0], splitParam.length > 1 ? splitParam[1] : null);
        }
      }
    }
  }

  private abstract class UrlMatcher {
    abstract boolean matches(final ParsedUrl url);
  }

  private class PathMatcher extends UrlMatcher {
    final ArgumentMatcher<String> matcher;

    private PathMatcher(final String exactPath) {
      this.matcher = path -> path.equals(exactPath);
    }

    @Override
    boolean matches(final ParsedUrl url) {
      return this.matcher.matches(url.path);
    }
  }

  private class ParamMatcher extends UrlMatcher {
    final String param;
    final ArgumentMatcher<String> matcher;

    private ParamMatcher(final String param, final String value) {
      this.param = param;
      this.matcher = val -> value == null ? true : value.equals(val);
    }

    private ParamMatcher(final String param, final Pattern valueMatcher) {
      this.param = param;
      this.matcher = val -> valueMatcher.matcher(val).matches();
    }

    @Override
    boolean matches(final ParsedUrl url) {
      return url.params.containsKey(this.param) && this.matcher.matches(url.params.get(this.param));
    }
  }
}
