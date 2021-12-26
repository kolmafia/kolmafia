package net.sourceforge.kolmafia.textui.langserver.workspaceservice;

import net.sourceforge.kolmafia.textui.langserver.AshLanguageServer;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.WorkspaceService;

public abstract class AshWorkspaceService implements WorkspaceService {
  protected final AshLanguageServer parent;

  public AshWorkspaceService(final AshLanguageServer parent) {
    this.parent = parent;
  }

  public final void setCapabilities(final ServerCapabilities capabilities) {
    // executeCommandProvider

    // workspaceSymbolProvider

    // workspace
  }

  @Override
  public void didChangeConfiguration(DidChangeConfigurationParams params) {
    // TODO Auto-generated method stub

  }

  @Override
  public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
    // TODO Auto-generated method stub

  }
}
