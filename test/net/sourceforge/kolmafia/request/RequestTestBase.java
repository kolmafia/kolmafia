package net.sourceforge.kolmafia.request;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentMatcher;
import org.mockito.verification.VerificationMode;

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
 *       submitted {@link ArgumentMatcher}. These methods all return the {@link ArgumentMatcher}
 *       submitted for the stubbing. This allows them to be re-used to {@link
 *       org.mockito.Mockito#verify verify} the access of the method (using the {@link
 *       #verifyRequest} shortcut method).
 * </ul>
 */
abstract class RequestTestBase {
  protected ConnectionFactory factory;

  @BeforeAll
  static final void setSessionId() {
    GenericRequest.sessionId = "fake session id";
  }

  @BeforeEach
  final void openNetwork() {
    factory = mock(ConnectionFactory.class);
    HttpUtilities.setOpen(factory);
  }

  @AfterAll
  static final void cleanSessionId() {
    GenericRequest.sessionId = null;
  }

  @AfterAll
  static final void reBlockNetwork() {
    ForbidNetworkAccess.blockNetwork();
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
  protected ArgumentMatcher<URL> respondIfContains(final String text, final String response) {
    return respondIfContains(Collections.singletonList(text), response);
  }

  /**
   * Set {@link #factory} to generate the given response if the path contains every element of
   * {@code texts}.
   */
  protected ArgumentMatcher<URL> respondIfContains(
      final List<String> texts, final String response) {
    return respondIfMatches(
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
  protected ArgumentMatcher<URL> respondIfMatches(final Pattern pattern, final String response) {
    return respondIfMatches(Collections.singletonList(pattern), response);
  }

  /**
   * Set {@link #factory} to generate the given response if the path matches every element of {@code
   * patterns}.
   */
  protected ArgumentMatcher<URL> respondIfMatches(
      final List<Pattern> patterns, final String response) {
    return respondIfMatches(
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
  protected ArgumentMatcher<URL> respondIfMatches(
      final ArgumentMatcher<String> matcher, final String response) {
    return prepareResponse(url -> matcher.matches(url.getPath()), response);
  }

  private ArgumentMatcher<URL> prepareResponse(
      final ArgumentMatcher<URL> matcher, final String response) {
    try {
      when(factory.openConnection(argThat(matcher))).thenReturn(new FakeConnection(response));
    } catch (IOException e) {
    }

    return matcher;
  }

  protected void verifyRequest(final ArgumentMatcher<URL> matcher) {
    verifyRequest(matcher, times(1));
  }

  protected void verifyRequest(final ArgumentMatcher<URL> matcher, final VerificationMode mode) {
    try {
      verify(factory, mode).openConnection(argThat(matcher));
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
