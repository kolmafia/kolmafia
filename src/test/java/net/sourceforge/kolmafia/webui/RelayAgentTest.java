package net.sourceforge.kolmafia.webui;

import static internal.helpers.Player.withPasswordHash;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import internal.helpers.Cleanups;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.utilities.StringUtilities;
import org.junit.jupiter.api.Nested;
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
    "/KoLmafia/sideCommand?pwd=xxxx&cmd=echo hi,'HTTP/1.1 302 Found|Date: GMT|Server: KoLmafia r0|Location: /charpane.php|Connection: close'",
    "/KoLmafia/sideCommand?cmd=echo hi,'HTTP/1.1 401 Unauthorized|Date: GMT|Server: KoLmafia r0|Connection: close|| '",
    "/KoLmafia/sideCommand?pwd=yyy&cmd=echo hi,'HTTP/1.1 401 Unauthorized|Date: GMT|Server: KoLmafia r0|Connection: close|| '"
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

  @Nested
  class ConditionalRequests {
    private static final long LAST_MODIFIED = 1_700_000_000_123L;
    private static final String LAST_MODIFIED_HEADER = StringUtilities.formatDate(LAST_MODIFIED);

    private Path createRelayFile(String suffix, Cleanups cleanups) throws IOException {
      Files.createDirectories(KoLConstants.RELAY_LOCATION.toPath());
      Path file =
          Files.createTempFile(KoLConstants.RELAY_LOCATION.toPath(), "relay-agent-", suffix);
      cleanups.add(file.toFile()::delete);
      Files.setLastModifiedTime(file, FileTime.fromMillis(LAST_MODIFIED));
      return file;
    }

    @Test
    void sendsNotModifiedForUnchangedLocalFile() throws IOException {
      var cleanups = new Cleanups();
      try (cleanups) {
        Path file = this.createRelayFile(".txt", cleanups);

        assertThat(
            RelayAgent.shouldSendNotModified("/" + file.getFileName(), LAST_MODIFIED_HEADER),
            is(true));
      }
    }

    @Test
    void doesNotSendNotModifiedForModifiedLocalFile() throws IOException {
      var cleanups = new Cleanups();
      try (cleanups) {
        Path file = this.createRelayFile(".txt", cleanups);
        String older = StringUtilities.formatDate(LAST_MODIFIED - 2000);

        assertThat(RelayAgent.shouldSendNotModified("/" + file.getFileName(), older), is(false));
      }
    }

    @Test
    void doesNotSendNotModifiedForQueryString() throws IOException {
      var cleanups = new Cleanups();
      try (cleanups) {
        Path file = this.createRelayFile(".txt", cleanups);

        assertThat(
            RelayAgent.shouldSendNotModified(
                "/" + file.getFileName() + "?x=1", LAST_MODIFIED_HEADER),
            is(false));
      }
    }

    @Test
    void doesNotSendNotModifiedForAshFile() throws IOException {
      var cleanups = new Cleanups();
      try (cleanups) {
        Path file = this.createRelayFile(".ash", cleanups);

        assertThat(
            RelayAgent.shouldSendNotModified("/" + file.getFileName(), LAST_MODIFIED_HEADER),
            is(false));
      }
    }

    @Test
    void doesNotSendNotModifiedForPhpFile() throws IOException {
      var cleanups = new Cleanups();
      try (cleanups) {
        Path file = this.createRelayFile(".php", cleanups);

        assertThat(
            RelayAgent.shouldSendNotModified("/" + file.getFileName(), LAST_MODIFIED_HEADER),
            is(false));
      }
    }
  }
}
