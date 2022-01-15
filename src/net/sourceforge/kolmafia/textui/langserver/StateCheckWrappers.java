package net.sourceforge.kolmafia.textui.langserver;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;

/**
 * Wrappers for {@link net.sourceforge.kolmafia.textui.langserver.AshLanguageServer
 * AshLanguageServer}.
 *
 * <p>Takes care of checking and/or setting the server's state before every request and notification
 * according to the LSP specifications of <a href=
 * https://microsoft.github.io/language-server-protocol/specifications/specification-3-16/#initialize>
 * initialize</a> and <a href=
 * https://microsoft.github.io/language-server-protocol/specifications/specification-3-16/#shutdown>
 * shutdown</a>, which dictate how a server should have a "pre-initialize" and "post-shutdown"
 * behavior.
 *
 * <p>Up-to-date with <a
 * href=https://microsoft.github.io/language-server-protocol/specifications/specification-3-16/>3.16</a>
 */
abstract class StateCheckWrappers {
  static class AshLanguageServer
      extends net.sourceforge.kolmafia.textui.langserver.AshLanguageServer {

    private ServerState state = ServerState.STARTED;

    private enum ServerState {
      STARTED,
      INITIALIZED,
      SHUTDOWN
    }

    private boolean notInitialized() {
      return this.state == ServerState.STARTED;
    }

    private boolean wasShutdown() {
      return this.state == ServerState.SHUTDOWN;
    }

    // Not made private to allow tests
    boolean isActive() {
      return this.state == ServerState.INITIALIZED;
    }

    /** To use with JsonNotifications. They are simply ignored if we are not initialized. */
    private <P> void ifActive(Notification<P> notification, P param) {
      if (this.isActive()) {
        notification.processNotification(param);
      }
    }

    /** To use with @JsonRequests. They must throw an error if we are not initialized. */
    private <P, R> CompletableFuture<R> ifActive(Request<P, R> request, P param) {
      if (this.notInitialized()) {
        CompletableFuture<R> result = new CompletableFuture<>();
        ResponseError error =
            new ResponseError(
                ResponseErrorCode.serverNotInitialized, "Server was not initialized", null);
        result.completeExceptionally(new ResponseErrorException(error));

        return result;
      }

      if (this.wasShutdown()) {
        CompletableFuture<R> result = new CompletableFuture<>();
        ResponseError error =
            new ResponseError(ResponseErrorCode.InvalidRequest, "Server was shut down", null);
        result.completeExceptionally(new ResponseErrorException(error));

        return result;
      }

      return request.processRequest(param);
    }

    // LanguageServer

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
      if (this.notInitialized()) {
        this.state = ServerState.INITIALIZED;
        return super.initialize(params);
      }

      if (this.wasShutdown()) {
        CompletableFuture<InitializeResult> result = new CompletableFuture<>();
        ResponseError error =
            new ResponseError(ResponseErrorCode.InvalidRequest, "Server was shut down", null);
        result.completeExceptionally(new ResponseErrorException(error));

        return result;
      }

      // https://microsoft.github.io/language-server-protocol/specifications/specification-3-16/#initialize
      // The initialize request may only be sent once.
      CompletableFuture<InitializeResult> result = new CompletableFuture<>();
      ResponseError error =
          new ResponseError(
              ResponseErrorCode.InvalidRequest, "Server was already initialized", null);
      result.completeExceptionally(new ResponseErrorException(error));

      return result;
    }

    @Override
    public void initialized(InitializedParams params) {
      // https://microsoft.github.io/language-server-protocol/specifications/specification-3-16/#initialized
      // There are additional specifications, regarding how this should be "the very first
      // notification/request from the client (if sent at all)" and "may only be sent once", but
      // this seems like too much trouble for something that... doesn't actually share any
      // information?? Like, what's the point?
      this.ifActive((Notification<InitializedParams>) super::initialized, params);
    }

    @Override
    public void initialized() {
      this.initialized(new InitializedParams());
    }

    @Override
    public CompletableFuture<Object> shutdown() {
      return this.ifActive(
          x -> {
            this.state = ServerState.SHUTDOWN;
            return super.shutdown();
          },
          (Void) null);
    }

    @Override
    public void exit() {
      // https://microsoft.github.io/language-server-protocol/specifications/specification-3-16/#exit
      // exit() gets processed regardless
      super.exit();
      // In case there is a local reference to us
      this.state = ServerState.SHUTDOWN;
    }

    @Override
    public void cancelProgress(WorkDoneProgressCancelParams params) {
      this.ifActive(super::cancelProgress, params);
    }
  }

  static class AshTextDocumentService
      extends net.sourceforge.kolmafia.textui.langserver.textdocumentservice
          .AshTextDocumentService {
    AshTextDocumentService(
        final net.sourceforge.kolmafia.textui.langserver.AshLanguageServer parent) {
      super(parent);
    }

    @Override
    public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
        CompletionParams position) {
      return ((AshLanguageServer) this.parent).ifActive(super::completion, position);
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
      return ((AshLanguageServer) this.parent).ifActive(super::resolveCompletionItem, unresolved);
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::hover, params);
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::signatureHelp, params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
        declaration(DeclarationParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::declaration, params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
        definition(DefinitionParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::definition, params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
        typeDefinition(TypeDefinitionParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::typeDefinition, params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
        implementation(ImplementationParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::implementation, params);
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::references, params);
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(
        DocumentHighlightParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::documentHighlight, params);
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
        DocumentSymbolParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::documentSymbol, params);
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(
        CodeActionParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::codeAction, params);
    }

    @Override
    public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
      return ((AshLanguageServer) this.parent).ifActive(super::resolveCodeAction, unresolved);
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::codeLens, params);
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
      return ((AshLanguageServer) this.parent).ifActive(super::resolveCodeLens, unresolved);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::formatting, params);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(
        DocumentRangeFormattingParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::rangeFormatting, params);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(
        DocumentOnTypeFormattingParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::onTypeFormatting, params);
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::rename, params);
    }

    @Override
    public CompletableFuture<LinkedEditingRanges> linkedEditingRange(
        LinkedEditingRangeParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::linkedEditingRange, params);
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
      ((AshLanguageServer) this.parent).ifActive(super::didOpen, params);
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
      ((AshLanguageServer) this.parent).ifActive(super::didChange, params);
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
      ((AshLanguageServer) this.parent).ifActive(super::didClose, params);
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
      ((AshLanguageServer) this.parent).ifActive(super::didSave, params);
    }

    @Override
    public void willSave(WillSaveTextDocumentParams params) {
      ((AshLanguageServer) this.parent).ifActive(super::willSave, params);
    }

    @Override
    public CompletableFuture<List<TextEdit>> willSaveWaitUntil(WillSaveTextDocumentParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::willSaveWaitUntil, params);
    }

    @Override
    public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::documentLink, params);
    }

    @Override
    public CompletableFuture<DocumentLink> documentLinkResolve(DocumentLink params) {
      return ((AshLanguageServer) this.parent).ifActive(super::documentLinkResolve, params);
    }

    @Override
    public CompletableFuture<List<ColorInformation>> documentColor(DocumentColorParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::documentColor, params);
    }

    @Override
    public CompletableFuture<List<ColorPresentation>> colorPresentation(
        ColorPresentationParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::colorPresentation, params);
    }

    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::foldingRange, params);
    }

    @Override
    public CompletableFuture<Either<Range, PrepareRenameResult>> prepareRename(
        PrepareRenameParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::prepareRename, params);
    }

    @Override
    public CompletableFuture<TypeHierarchyItem> typeHierarchy(TypeHierarchyParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::typeHierarchy, params);
    }

    @Override
    public CompletableFuture<TypeHierarchyItem> resolveTypeHierarchy(
        ResolveTypeHierarchyItemParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::resolveTypeHierarchy, params);
    }

    @Override
    public CompletableFuture<List<CallHierarchyItem>> prepareCallHierarchy(
        CallHierarchyPrepareParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::prepareCallHierarchy, params);
    }

    @Override
    public CompletableFuture<List<CallHierarchyIncomingCall>> callHierarchyIncomingCalls(
        CallHierarchyIncomingCallsParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::callHierarchyIncomingCalls, params);
    }

    @Override
    public CompletableFuture<List<CallHierarchyOutgoingCall>> callHierarchyOutgoingCalls(
        CallHierarchyOutgoingCallsParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::callHierarchyOutgoingCalls, params);
    }

    @Override
    public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::selectionRange, params);
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::semanticTokensFull, params);
    }

    @Override
    public CompletableFuture<Either<SemanticTokens, SemanticTokensDelta>> semanticTokensFullDelta(
        SemanticTokensDeltaParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::semanticTokensFullDelta, params);
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensRange(SemanticTokensRangeParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::semanticTokensRange, params);
    }

    @Override
    public CompletableFuture<List<Moniker>> moniker(MonikerParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::moniker, params);
    }
  }

  static class AshWorkspaceService
      extends net.sourceforge.kolmafia.textui.langserver.workspaceservice.AshWorkspaceService {
    AshWorkspaceService(final net.sourceforge.kolmafia.textui.langserver.AshLanguageServer parent) {
      super(parent);
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::executeCommand, params);
    }

    @Override
    public CompletableFuture<List<? extends SymbolInformation>> symbol(
        WorkspaceSymbolParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::symbol, params);
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
      ((AshLanguageServer) this.parent).ifActive(super::didChangeConfiguration, params);
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
      ((AshLanguageServer) this.parent).ifActive(super::didChangeWatchedFiles, params);
    }

    @Override
    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
      ((AshLanguageServer) this.parent).ifActive(super::didChangeWorkspaceFolders, params);
    }

    @Override
    public CompletableFuture<WorkspaceEdit> willCreateFiles(CreateFilesParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::willCreateFiles, params);
    }

    @Override
    public void didCreateFiles(CreateFilesParams params) {
      ((AshLanguageServer) this.parent).ifActive(super::didCreateFiles, params);
    }

    @Override
    public CompletableFuture<WorkspaceEdit> willRenameFiles(RenameFilesParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::willRenameFiles, params);
    }

    @Override
    public void didRenameFiles(RenameFilesParams params) {
      ((AshLanguageServer) this.parent).ifActive(super::didRenameFiles, params);
    }

    @Override
    public CompletableFuture<WorkspaceEdit> willDeleteFiles(DeleteFilesParams params) {
      return ((AshLanguageServer) this.parent).ifActive(super::willDeleteFiles, params);
    }

    @Override
    public void didDeleteFiles(DeleteFilesParams params) {
      ((AshLanguageServer) this.parent).ifActive(super::didDeleteFiles, params);
    }
  }

  @FunctionalInterface
  private static interface Notification<P> {
    void processNotification(P param);
  }

  @FunctionalInterface
  private static interface Request<P, R> {
    CompletableFuture<R> processRequest(P param);
  }
}
