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
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import net.sourceforge.kolmafia.extensions.ForbidNetworkAccess;
import net.sourceforge.kolmafia.utilities.HttpUtilities;
import net.sourceforge.kolmafia.utilities.HttpUtilities.ConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoSession;
import org.mockito.quality.Strictness;

/**
 * Extend this class when making tests involving {@link GenericRequest} and/or its descendants.
 *
 * <ul>
 *   <li>Allows this class's tests to run {@link GenericRequest}s (otherwise blocked pre-login and
 *       due to {@link ForbidNetworkAccess}).
 *   <li>For {@link GenericRequest}s made locally, gives access to {@link
 *       RequestTestBase#expectSuccess}, allowing the simulation of running a {@link
 *       org.mockito.Mockito#spy spy} of that request.
 *   <li>For the rest, gives access to {@link #respondIfContains}/{@link #respondIfMatches}, causing
 *       future executed requests to succeed with the given response if the path of the URL
 *       submitted contains the submitted text(s)/matches the submitted pattern(s)/matches the
 *       submitted {@link ArgumentMatcher}.
 * </ul>
 */
abstract class RequestTestBase {
  @Mock protected ConnectionFactory factory;
  private MockitoSession mockito;

  @BeforeAll
  static final void setSessionId() {
    GenericRequest.sessionId = "fake session id";
  }

  @BeforeEach
  final void setupSession() {
    mockito =
        Mockito.mockitoSession().initMocks(this).strictness(Strictness.STRICT_STUBS).startMocking();
    factory = mock(ConnectionFactory.class);
    HttpUtilities.setOpen(factory);
  }

  @AfterEach
  final void tearDownSession() {
    ForbidNetworkAccess.blockNetwork();
    mockito.finishMocking();
  }

  @AfterAll
  static final void cleanSessionId() {
    GenericRequest.sessionId = null;
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

  /** Set {@link #factory} to generate the given response if the path contains {@code text}. */
  protected void respondIfContains(final String text, final String response) {
    respondIfContains(Collections.singletonList(text), response);
  }

  /**
   * Set {@link #factory} to generate the given response if the path contains every element of
   * {@code texts}.
   */
  protected void respondIfContains(final List<String> texts, final String response) {
    respondIfMatches(
        path -> {
          for (final String text : texts) {
            if (!path.contains(text)) {
              return false;
            }
          }
          return true;
        },
        response);
  }

  /** Set {@link #factory} to generate the given response if the path matches {@code pattern}. */
  protected void respondIfMatches(final Pattern pattern, final String response) {
    respondIfMatches(Collections.singletonList(pattern), response);
  }

  /**
   * Set {@link #factory} to generate the given response if the path matches every element of {@code
   * patterns}.
   */
  protected void respondIfMatches(final List<Pattern> patterns, final String response) {
    respondIfMatches(
        path -> {
          for (final Pattern pattern : patterns) {
            if (!pattern.matcher(path).matches()) {
              return false;
            }
          }
          return true;
        },
        response);
  }

  /**
   * Set {@link #factory} to generate the given response if the path matches with {@code matcher}.
   */
  protected void respondIfMatches(final ArgumentMatcher<String> matcher, final String response) {
    prepareResponse(url -> matcher.matches(url.getPath()), response);
  }

  private void prepareResponse(final ArgumentMatcher<URL> matcher, final String response) {
    try {
      when(factory.openConnection(argThat(matcher))).thenReturn(new FakeConnection(response));
    } catch (IOException e) {
    }
  }

  private class FakeConnection extends HttpURLConnection {
    private final String responseText;

    private FakeConnection(final String responseText) {
      super(null);
      this.responseText = responseText;
      this.responseCode = 200;
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
}
