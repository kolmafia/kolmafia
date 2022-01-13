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

    /** To use with @JsonNotifications. They are simply ignored if we are not initialized. */
    private boolean isActive() {
      return this.state == ServerState.INITIALIZED;
    }

    private void initializeCheck() {
      if (this.notInitialized()) {
        ResponseError error =
            new ResponseError(
                ResponseErrorCode.serverNotInitialized, "Server was not initialized", null);
        throw new ResponseErrorException(error);
      }
    }

    private void shutdownCheck() {
      if (this.wasShutdown()) {
        ResponseError error =
            new ResponseError(ResponseErrorCode.InvalidRequest, "Server was shut down", null);
        throw new ResponseErrorException(error);
      }
    }

    /** To use with @JsonRequests. They must throw an error if we are not initialized. */
    private void stateCheck() {
      this.initializeCheck();
      this.shutdownCheck();
    }

    // LanguageServer

    @Override
    public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
      this.shutdownCheck();
      this.state = ServerState.INITIALIZED;
      return super.initialize(params);
    }

    @Override
    public void initialized(InitializedParams params) {
      // https://microsoft.github.io/language-server-protocol/specifications/specification-3-16/#initialized
      // The protocol specifies that this notification "may only be sent once", but... doesn't
      // specify what to do if it is sent multiple times...
      if (this.isActive()) {
        super.initialized(params);
      }
    }

    @Override
    public CompletableFuture<Object> shutdown() {
      this.initializeCheck();
      this.state = ServerState.SHUTDOWN;
      return super.shutdown();
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
      if (this.isActive()) {
        super.cancelProgress(params);
      }
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
      ((AshLanguageServer) this.parent).stateCheck();
      return super.completion(position);
    }

    @Override
    public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.resolveCompletionItem(unresolved);
    }

    @Override
    public CompletableFuture<Hover> hover(HoverParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.hover(params);
    }

    @Override
    public CompletableFuture<SignatureHelp> signatureHelp(SignatureHelpParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.signatureHelp(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
        declaration(DeclarationParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.declaration(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
        definition(DefinitionParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.definition(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
        typeDefinition(TypeDefinitionParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.typeDefinition(params);
    }

    @Override
    public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
        implementation(ImplementationParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.implementation(params);
    }

    @Override
    public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.references(params);
    }

    @Override
    public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(
        DocumentHighlightParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.documentHighlight(params);
    }

    @Override
    public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(
        DocumentSymbolParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.documentSymbol(params);
    }

    @Override
    public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(
        CodeActionParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.codeAction(params);
    }

    @Override
    public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.resolveCodeAction(unresolved);
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.codeLens(params);
    }

    @Override
    public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.resolveCodeLens(unresolved);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.formatting(params);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> rangeFormatting(
        DocumentRangeFormattingParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.rangeFormatting(params);
    }

    @Override
    public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(
        DocumentOnTypeFormattingParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.onTypeFormatting(params);
    }

    @Override
    public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.rename(params);
    }

    @Override
    public CompletableFuture<LinkedEditingRanges> linkedEditingRange(
        LinkedEditingRangeParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.linkedEditingRange(params);
    }

    @Override
    public void didOpen(DidOpenTextDocumentParams params) {
      if (((AshLanguageServer) this.parent).isActive()) {
        super.didOpen(params);
      }
    }

    @Override
    public void didChange(DidChangeTextDocumentParams params) {
      if (((AshLanguageServer) this.parent).isActive()) {
        super.didChange(params);
      }
    }

    @Override
    public void didClose(DidCloseTextDocumentParams params) {
      if (((AshLanguageServer) this.parent).isActive()) {
        super.didClose(params);
      }
    }

    @Override
    public void didSave(DidSaveTextDocumentParams params) {
      if (((AshLanguageServer) this.parent).isActive()) {
        super.didSave(params);
      }
    }

    @Override
    public void willSave(WillSaveTextDocumentParams params) {
      if (((AshLanguageServer) this.parent).isActive()) {
        super.willSave(params);
      }
    }

    @Override
    public CompletableFuture<List<TextEdit>> willSaveWaitUntil(WillSaveTextDocumentParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.willSaveWaitUntil(params);
    }

    @Override
    public CompletableFuture<List<DocumentLink>> documentLink(DocumentLinkParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.documentLink(params);
    }

    @Override
    public CompletableFuture<DocumentLink> documentLinkResolve(DocumentLink params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.documentLinkResolve(params);
    }

    @Override
    public CompletableFuture<List<ColorInformation>> documentColor(DocumentColorParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.documentColor(params);
    }

    @Override
    public CompletableFuture<List<ColorPresentation>> colorPresentation(
        ColorPresentationParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.colorPresentation(params);
    }

    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.foldingRange(params);
    }

    @Override
    public CompletableFuture<Either<Range, PrepareRenameResult>> prepareRename(
        PrepareRenameParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.prepareRename(params);
    }

    @Override
    public CompletableFuture<TypeHierarchyItem> typeHierarchy(TypeHierarchyParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.typeHierarchy(params);
    }

    @Override
    public CompletableFuture<TypeHierarchyItem> resolveTypeHierarchy(
        ResolveTypeHierarchyItemParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.resolveTypeHierarchy(params);
    }

    @Override
    public CompletableFuture<List<CallHierarchyItem>> prepareCallHierarchy(
        CallHierarchyPrepareParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.prepareCallHierarchy(params);
    }

    @Override
    public CompletableFuture<List<CallHierarchyIncomingCall>> callHierarchyIncomingCalls(
        CallHierarchyIncomingCallsParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.callHierarchyIncomingCalls(params);
    }

    @Override
    public CompletableFuture<List<CallHierarchyOutgoingCall>> callHierarchyOutgoingCalls(
        CallHierarchyOutgoingCallsParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.callHierarchyOutgoingCalls(params);
    }

    @Override
    public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.selectionRange(params);
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.semanticTokensFull(params);
    }

    @Override
    public CompletableFuture<Either<SemanticTokens, SemanticTokensDelta>> semanticTokensFullDelta(
        SemanticTokensDeltaParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.semanticTokensFullDelta(params);
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensRange(SemanticTokensRangeParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.semanticTokensRange(params);
    }

    @Override
    public CompletableFuture<List<Moniker>> moniker(MonikerParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.moniker(params);
    }
  }

  static class AshWorkspaceService
      extends net.sourceforge.kolmafia.textui.langserver.workspaceservice.AshWorkspaceService {
    AshWorkspaceService(final net.sourceforge.kolmafia.textui.langserver.AshLanguageServer parent) {
      super(parent);
    }

    @Override
    public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.executeCommand(params);
    }

    @Override
    public CompletableFuture<List<? extends SymbolInformation>> symbol(
        WorkspaceSymbolParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.symbol(params);
    }

    @Override
    public void didChangeConfiguration(DidChangeConfigurationParams params) {
      if (((AshLanguageServer) this.parent).isActive()) {
        super.didChangeConfiguration(params);
      }
    }

    @Override
    public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
      if (((AshLanguageServer) this.parent).isActive()) {
        super.didChangeWatchedFiles(params);
      }
    }

    @Override
    public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
      if (((AshLanguageServer) this.parent).isActive()) {
        super.didChangeWorkspaceFolders(params);
      }
    }

    @Override
    public CompletableFuture<WorkspaceEdit> willCreateFiles(CreateFilesParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.willCreateFiles(params);
    }

    @Override
    public void didCreateFiles(CreateFilesParams params) {
      if (((AshLanguageServer) this.parent).isActive()) {
        super.didCreateFiles(params);
      }
    }

    @Override
    public CompletableFuture<WorkspaceEdit> willRenameFiles(RenameFilesParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.willRenameFiles(params);
    }

    @Override
    public void didRenameFiles(RenameFilesParams params) {
      if (((AshLanguageServer) this.parent).isActive()) {
        super.didRenameFiles(params);
      }
    }

    @Override
    public CompletableFuture<WorkspaceEdit> willDeleteFiles(DeleteFilesParams params) {
      ((AshLanguageServer) this.parent).stateCheck();
      return super.willDeleteFiles(params);
    }

    @Override
    public void didDeleteFiles(DeleteFilesParams params) {
      if (((AshLanguageServer) this.parent).isActive()) {
        super.didDeleteFiles(params);
      }
    }
  }
}
