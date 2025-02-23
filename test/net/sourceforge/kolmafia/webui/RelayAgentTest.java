package net.sourceforge.kolmafia.webui;

import static internal.helpers.Player.withPasswordHash;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RelayAgentTest {
  private static class MockSocket extends Socket {
    private final InputStream inputStream;
    private final OutputStream outputStream;

    public MockSocket(InputStream inputStream, OutputStream outputStream) {
      this.inputStream = inputStream;
      this.outputStream = outputStream;
    }

    @Override
    public InputStream getInputStream() {
      return this.inputStream;
    }

    @Override
    public OutputStream getOutputStream() {
      return this.outputStream;
    }
  }

  private static final RelayAgent agent = new RelayAgent(999);
  ;

  private OutputStream sendInput(String input) {
    OutputStream outputStream = new ByteArrayOutputStream();
    agent.setSocket(new MockSocket(new ByteArrayInputStream(input.getBytes()), outputStream));
    return outputStream;
  }

  @Test
  void readBrowserRequestWithHash() throws IOException {
    var cleanups = withPasswordHash("xxxx");
    try (cleanups) {
      try (var outputStream = this.sendInput("GET /KoLmafia/sideCommand?pwd=xxxx HTTP/1.1")) {
        assertThat(agent.readBrowserRequest(), is(true));
      }
    }
  }

  @Test
  void readBrowserRequestWithNoHash() throws IOException {
    var cleanups = withPasswordHash("xxxx");
    try (cleanups) {
      this.sendInput("GET /KoLmafia/sideCommand HTTP/1.1");
      assertThat(agent.readBrowserRequest(), is(false));
    }
  }

  @Test
  void readBrowserRequestWithWrongHash() throws IOException {
    var cleanups = withPasswordHash("xxxx");
    try (cleanups) {
      this.sendInput("GET /KoLmafia/sideCommand?pwd=yyyy HTTP/1.1");
      assertThat(agent.readBrowserRequest(), is(false));
    }
  }

  @ParameterizedTest
  @CsvSource({
    "/KoLmafia/sideCommand?pwd=xxxx&cmd=echo hi,'HTTP/1.1 302 Found|Date: GMT|Server: KoLmafia r0|Location: /charpane.php'",
    "/KoLmafia/sideCommand?cmd=echo hi,'HTTP/1.1 401 Unauthorized|Date: GMT|Server: KoLmafia r0|| '",
    "/KoLmafia/sideCommand?pwd=yyy&cmd=echo hi,'HTTP/1.1 401 Unauthorized|Date: GMT|Server: KoLmafia r0|| '"
  })
  void performRelayWithHash(String url, String expectedString) throws IOException {
    var cleanups = withPasswordHash("xxxx");
    try (cleanups) {
      try (var outputStream = this.sendInput("GET " + url + " HTTP/1.1")) {
        agent.performRelay();
        String[] result =
            outputStream.toString().replaceFirst("Date: .* GMT", "Date: GMT").split("\r\n");
        assertThat(result, arrayContaining(expectedString.split("\\|")));
      }
    }
  }
}
