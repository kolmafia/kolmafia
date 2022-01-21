package net.sourceforge.kolmafia.textui.langserver;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public class AshLanguageServerTest {
  private final Closeable[] streams = new Closeable[4];

  // The two "true" endpoints.
  LanguageClient client;
  AshLanguageServer trueServer;

  // The way the client has to access the server through the LSP4J implementation
  LanguageServer proxyServer;

  @BeforeEach
  final void setup() throws IOException {
    PipedOutputStream serverOut = (PipedOutputStream) (streams[0] = new PipedOutputStream());
    PipedOutputStream clientOut = (PipedOutputStream) (streams[1] = new PipedOutputStream());
    PipedInputStream serverIn = (PipedInputStream) (streams[2] = new PipedInputStream(clientOut));
    PipedInputStream clientIn = (PipedInputStream) (streams[3] = new PipedInputStream(serverOut));

    trueServer = launchServer(serverIn, serverOut);

    client = launchClientAndSetProxy(clientIn, clientOut);
  }

  @AfterEach
  final void tearDown() throws IOException {
    for (Closeable stream : streams) {
      stream.close();
    }
  }

  AshLanguageServer launchServer(InputStream in, OutputStream out) {
    return AshLanguageServer.launch(in, out);
  }

  LanguageClient launchClientAndSetProxy(InputStream in, OutputStream out) {
    LanguageClient client = new TestLanguageClient();

    final Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(client, in, out);
    proxyServer = launcher.getRemoteProxy();
    launcher.startListening();

    return client;
  }
}
