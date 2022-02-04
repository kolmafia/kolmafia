package net.sourceforge.kolmafia.textui.langserver;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import net.sourceforge.kolmafia.utilities.PauseObject;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.mockito.Mockito;

public class AshLanguageServerTest {
  // Maintain an array of references to the loggers we disable
  // because they are stored as WeakRefs and we need them to last
  // for the duration of this collection of tests.
  static Set<Logger> disabledLoggers = new HashSet<>();

  @BeforeAll
  private static void disableLoggers() {
    Set.of(StreamMessageProducer.class, RemoteEndpoint.class)
        .forEach(
            c -> {
              var logger = Logger.getLogger(c.getName());
              logger.setUseParentHandlers(false);
              disabledLoggers.add(logger);
            });
  }

  @AfterAll
  private static void disposeDisabledLoggers() {
    disabledLoggers.clear();
  }

  private final Closeable[] streams = new Closeable[4];

  // A pauser. We'll need that since interactions between client and server are asynchronous
  final PauseObject pauser = new PauseObject();

  // The two "true" endpoints.
  final LanguageClient client = Mockito.mock(TestLanguageClient.class);
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

    final Launcher<LanguageServer> clientLauncher =
        LSPLauncher.createClientLauncher(client, clientIn, clientOut);
    proxyServer = clientLauncher.getRemoteProxy();
    clientLauncher.startListening();
  }

  @AfterEach
  final void tearDown() throws IOException {
    for (Closeable stream : streams) {
      stream.close();
    }
  }

  protected AshLanguageServer launchServer(InputStream in, OutputStream out) {
    return AshLanguageServer.launch(in, out);
  }

  public final InitializeResult initialize(final InitializeParams params) {
    return Assertions.assertDoesNotThrow(
        (ThrowingSupplier<InitializeResult>) proxyServer.initialize(params)::get,
        "Initialization failed");
  }
}
