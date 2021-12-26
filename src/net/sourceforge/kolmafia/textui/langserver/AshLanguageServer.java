package net.sourceforge.kolmafia.textui.langserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.textui.langserver.textdocumentservice.AshTextDocumentService;
import net.sourceforge.kolmafia.textui.langserver.workspaceservice.AshWorkspaceService;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.ServerInfo;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * The thread in charge of listening for the client's messages. Its methods all quickly delegate to
 * other threads, because we want to avoid blocking the reading of new messages.
 *
 * <p>Was made abstract, because the actual class to use is {@link
 * StateCheckWrappers.AshLanguageServer}.
 */
public abstract class AshLanguageServer implements LanguageClientAware, LanguageServer {

  /* The Launcher */

  public static void main(String... args) throws IOException {
    if (args.length > 0 && args[0].equals("socket")) {
      Socket socket = new ServerSocket(60180, 25, InetAddress.getByName("127.0.0.1")).accept();
      AshLanguageServer.launch(socket.getInputStream(), socket.getOutputStream());
    } else {
      AshLanguageServer.launch(System.in, System.out);
    }
  }

  public static AshLanguageServer launch(final InputStream in, final OutputStream out) {
    final AshLanguageServer server = new StateCheckWrappers.AshLanguageServer();

    final Launcher<LanguageClient> launcher =
        LSPLauncher.createServerLauncher(server, in, out, server.executor, null);
    server.connect(launcher.getRemoteProxy());

    // TODO find a way to rename the thread that will be created by startListening()
    Future<Void> connectionListener = launcher.startListening();

    server.listenForEarlyConnectionTermination(connectionListener);

    return server;
  }

  /* The server */

  private ServerState state = ServerState.STARTED;

  enum ServerState {
    STARTED,
    INITIALIZED,
    SHUTDOWN
  }

  public final ServerState getState() {
    return this.state;
  }

  protected abstract boolean notInitialized();

  protected abstract boolean wasShutdown();

  protected abstract boolean isActive();

  protected abstract void initializeCheck();

  protected abstract void shutdownCheck();

  protected abstract void stateCheck();

  public LanguageClient client;
  public ClientCapabilities clientCapabilities;

  public final AshTextDocumentService textDocumentService =
      new StateCheckWrappers.AshTextDocumentService(this);
  public final AshWorkspaceService workspaceService =
      new StateCheckWrappers.AshWorkspaceService(this);

  public final ExecutorService executor = Executors.newCachedThreadPool();
  public final FilesMonitor monitor = new FilesMonitor(this);

  public final Map<File, Script> scripts = Collections.synchronizedMap(new Hashtable<>(20));

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    this.state = ServerState.INITIALIZED;

    this.clientCapabilities = params.getCapabilities();
    // params.getTrace(); for when we implement trace
    // params.getClientInfo(); do we need/care about that?
    // params.getWorkspaceFolders(); look into this later

    this.executor.execute(
        () -> {
          this.monitor.scan();
        });

    return CompletableFuture.supplyAsync(
        () -> {
          final ServerCapabilities capabilities = new ServerCapabilities();

          this.textDocumentService.setCapabilities(capabilities);
          this.workspaceService.setCapabilities(capabilities);

          final ServerInfo info = new ServerInfo(StaticEntity.getVersion());

          return new InitializeResult(capabilities, info);
        },
        this.executor);
  }

  @Override
  public CompletableFuture<Object> shutdown() {
    this.state = ServerState.SHUTDOWN;

    return CompletableFuture.supplyAsync(
        () -> {
          this.close();
          return null;
        },
        this.executor);
  }

  @Override
  public void exit() {
    // Call close() in case the client didn't send a shutdown notification
    this.close();
    this.executor.shutdownNow();
  }

  private void close() {
    synchronized (this.scripts) {
      final Iterator<Script> it = this.scripts.values().iterator();
      while (it.hasNext()) {
        final Script script = it.next();
        if (script.handler != null) {
          script.handler.close();
        }

        it.remove();
      }
    }
  }

  /**
   * Creates a thread running in parallel with the Language Server for its whole existence, with the
   * task of listening for the early termination of the thread listening to the client.
   *
   * <p>An early termination is defined as the server's input stream being closed before receiving
   * the {@code exit} notification.
   *
   * <p>This makes sure that {@link #exit} still gets called, preventing this process becoming a
   * zombie process.
   */
  private void listenForEarlyConnectionTermination(final Future<Void> connectionListener) {
    this.executor.execute(
        () -> {
          final String previousThreadName = Thread.currentThread().getName();
          Thread.currentThread().setName("Language Server early connection termination listener");

          try {
            connectionListener.get();
          } catch (InterruptedException | ExecutionException | CancellationException e) {
          } finally {
            this.exit();

            Thread.currentThread().setName(previousThreadName);
          }
        });
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return this.textDocumentService;
  }

  @Override
  public WorkspaceService getWorkspaceService() {
    return this.workspaceService;
  }
}
