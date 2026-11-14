package net.sourceforge.kolmafia.webui;

import static internal.helpers.Player.withPasswordHash;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
    "/KoLmafia/sideCommand?pwd=xxxx&cmd=echo hi,'HTTP/1.1 302 Found|Connection: close|Date: GMT|Server: KoLmafia r0|Location: /charpane.php'",
    "/KoLmafia/sideCommand?cmd=echo hi,'HTTP/1.1 401 Unauthorized|Connection: close|Date: GMT|Server: KoLmafia r0|| '",
    "/KoLmafia/sideCommand?pwd=yyy&cmd=echo hi,'HTTP/1.1 401 Unauthorized|Connection: close|Date: GMT|Server: KoLmafia r0|| '"
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

  private File cssFileModifiedAt(long lastModified) throws IOException {
    File file = File.createTempFile("relayagent", ".css");
    file.deleteOnExit();
    file.setLastModified(lastModified);
    return file;
  }

  @Test
  void notModifiedWhenTheFileIsOlderThanTheBrowsersCopy() throws IOException {
    File file = cssFileModifiedAt(1_000_000_000_000L);
    assertThat(RelayAgent.notModifiedSince("Sun, 09 Sep 2001 01:46:41 GMT", file), is(true));
  }

  @Test
  void modifiedWhenTheFileIsNewerThanTheBrowsersCopy() throws IOException {
    File file = cssFileModifiedAt(2_000_000_000_000L);
    assertThat(RelayAgent.notModifiedSince("Sun, 09 Sep 2001 01:46:41 GMT", file), is(false));
  }

  @Test
  void sendsTheFileWhenThereIsNothingToCompare() throws IOException {
    File file = cssFileModifiedAt(1_000_000_000_000L);
    assertThat(RelayAgent.notModifiedSince("not a date", file), is(false));
    assertThat(RelayAgent.notModifiedSince(null, file), is(false));
    assertThat(RelayAgent.notModifiedSince("Sun, 09 Sep 2001 01:46:41 GMT", null), is(false));
  }

  @ParameterizedTest
  @CsvSource({
    "/chit.css,chit.css",
    "/chit.css?v=2,chit.css",
    "/images/itemimages/box.gif,",
    "/basement.php,",
    "/chit.css.php,",
    "/relay_Guide.js?relay=true,",
    "/relay_Guide.js?v=2,relay_Guide.js"
  })
  void namesOnlyStaticRelayFiles(String path, String expected) {
    assertThat(RelayAgent.staticRelayFileName(path), is(expected));
  }

  @Test
  void announcesThatItClosesTheConnection() throws IOException {
    var cleanups = withPasswordHash("xxxx");
    try (cleanups) {
      try (var outputStream =
          this.sendInput("GET /KoLmafia/sideCommand?pwd=xxxx&cmd=echo hi HTTP/1.1")) {
        agent.performRelay();
        assertThat(outputStream.toString().contains("Connection: close\r\n"), is(true));
      }
    }
  }
}
