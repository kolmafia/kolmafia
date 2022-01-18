package net.sourceforge.kolmafia.textui.langserver;

import java.io.InputStream;
import java.io.OutputStream;
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

  public LanguageClient client;
  public ClientCapabilities clientCapabilities;

  public final AshTextDocumentService textDocumentService =
      new StateCheckWrappers.AshTextDocumentService(this);
  public final AshWorkspaceService workspaceService =
      new StateCheckWrappers.AshWorkspaceService(this);

  public final ExecutorService executor = Executors.newCachedThreadPool();

  @Override
  public void connect(LanguageClient client) {
    this.client = client;
  }

  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    this.clientCapabilities = params.getCapabilities();

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
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public void exit() {
    this.executor.shutdownNow();
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
