package net.sourceforge.kolmafia.textui.langserver;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * Using Mockito.mock(LanguageClient.class) doesn't work, as Mockito seems to makes a duplicate of
 * every single method, including their annotation, which LSP4J.json-RPC disapproves of.
 *
 * <p>Instead, it's important to use a class which overrides *every* LanguageClient method.
 */
public class TestLanguageClient implements LanguageClient {
  @Override
  public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(ApplyWorkspaceEditParams params) {
    return LanguageClient.super.applyEdit(params);
  }

  @Override
  public CompletableFuture<Void> registerCapability(RegistrationParams params) {
    return LanguageClient.super.registerCapability(params);
  }

  @Override
  public CompletableFuture<Void> unregisterCapability(UnregistrationParams params) {
    return LanguageClient.super.unregisterCapability(params);
  }

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
  public CompletableFuture<ShowDocumentResult> showDocument(ShowDocumentParams params) {
    return LanguageClient.super.showDocument(params);
  }

  @Override
  public void logMessage(MessageParams message) {}

  @Override
  public CompletableFuture<List<WorkspaceFolder>> workspaceFolders() {
    return LanguageClient.super.workspaceFolders();
  }

  @Override
  public CompletableFuture<List<Object>> configuration(ConfigurationParams params) {
    return LanguageClient.super.configuration(params);
  }

  @Override
  public CompletableFuture<Void> createProgress(WorkDoneProgressCreateParams params) {
    return LanguageClient.super.createProgress(params);
  }

  @Override
  public void notifyProgress(ProgressParams params) {
    LanguageClient.super.notifyProgress(params);
  }

  @Override
  public void logTrace(LogTraceParams params) {
    LanguageClient.super.logTrace(params);
  }

  @Override
  public void setTrace(SetTraceParams params) {
    LanguageClient.super.setTrace(params);
  }

  @Override
  public CompletableFuture<Void> refreshSemanticTokens() {
    return LanguageClient.super.refreshSemanticTokens();
  }

  @Override
  public CompletableFuture<Void> refreshCodeLenses() {
    return LanguageClient.super.refreshCodeLenses();
  }
}
