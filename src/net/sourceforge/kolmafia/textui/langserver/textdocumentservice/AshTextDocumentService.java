package net.sourceforge.kolmafia.textui.langserver.textdocumentservice;

import java.io.File;
import java.util.List;
import net.sourceforge.kolmafia.textui.langserver.AshLanguageServer;
import net.sourceforge.kolmafia.textui.langserver.FilesMonitor;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.SaveOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.services.TextDocumentService;

public abstract class AshTextDocumentService implements TextDocumentService {
  protected final AshLanguageServer parent;

  public AshTextDocumentService(final AshLanguageServer parent) {
    this.parent = parent;
  }

  public final void setCapabilities(final ServerCapabilities capabilities) {
    TextDocumentSyncOptions textDocumentSyncOptions = new TextDocumentSyncOptions();
    textDocumentSyncOptions.setOpenClose(true);
    textDocumentSyncOptions.setChange(TextDocumentSyncKind.Full);
    textDocumentSyncOptions.setWillSave(false);
    textDocumentSyncOptions.setWillSaveWaitUntil(false);
    textDocumentSyncOptions.setSave(new SaveOptions(false));

    capabilities.setTextDocumentSync(textDocumentSyncOptions);
  }

  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    this.parent.executor.execute(
        () -> {
          TextDocumentItem document = params.getTextDocument();

          File file = FilesMonitor.UriToFile(document.getUri());

          this.parent.monitor.updateFile(file, document.getText(), document.getVersion());
        });
  }

  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    this.parent.executor.execute(
        () -> {
          VersionedTextDocumentIdentifier document = params.getTextDocument();
          List<TextDocumentContentChangeEvent> changes = params.getContentChanges();

          if (changes.size() == 0) {
            // Nothing to see here
            return;
          }

          File file = FilesMonitor.UriToFile(document.getUri());

          // We don't support incremental changes, so we expect the client
          // to put the whole file's content in a single TextDocumentContentChangeEvent
          this.parent.monitor.updateFile(file, changes.get(0).getText(), document.getVersion());
        });
  }

  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    this.parent.executor.execute(
        () -> {
          TextDocumentIdentifier document = params.getTextDocument();

          File file = FilesMonitor.UriToFile(document.getUri());

          this.parent.monitor.updateFile(file, null, -1);
        });
  }

  @Override
  public void didSave(DidSaveTextDocumentParams params) {}
}
