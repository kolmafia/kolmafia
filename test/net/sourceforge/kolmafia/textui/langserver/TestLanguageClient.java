package net.sourceforge.kolmafia.textui.langserver;

import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;

// Using Mockito.mock(LanguageClient.class) doesn't seem to work, as Mockito seems to makes a
// duplicate of every single method, including their annotation, which LSP4J disapproves of

public class TestLanguageClient implements LanguageClient {
  @Override
  public void telemetryEvent(Object object) {}

  @Override
  public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {}

  @Override
  public void showMessage(MessageParams messageParams) {}

  @Override
  public CompletableFuture<MessageActionItem> showMessageRequest(
      ShowMessageRequestParams requestParams) {
    return null;
  }

  @Override
  public void logMessage(MessageParams message) {}
}
