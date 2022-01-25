package net.sourceforge.kolmafia.textui.langserver;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import net.sourceforge.kolmafia.KoLConstants;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class FilesMonitorTest extends AshLanguageServerTest {
  private static final File badScript =
      new File(KoLConstants.SCRIPT_LOCATION, "test_function_coercion_override_builtin.ash");
  private static final File goodScript =
      new File(KoLConstants.SCRIPT_LOCATION, "test_function_coercion.ash");

  private static class MonitorAshLanguageServer extends StateCheckWrappers.AshLanguageServer {
    @Override
    protected boolean canParse(final File file) {
      return file.equals(badScript) || file.equals(goodScript);
    }
  }

  @Override
  protected AshLanguageServer launchServer(InputStream in, OutputStream out) {
    return AshLanguageServer.launch(in, out, new MonitorAshLanguageServer());
  }

  @Test
  public void filesMonitorTest() {
    // Note: this test is actually 5 tests at once. This was preferred to having 5 different tests
    // because of how each relies on the state of the server at the end of the previous one, and
    // making 5 different tests would require doing everything from the previous tests again,
    // stacking the waiting time from each of them

    Assertions.assertTrue(
        badScript.exists() && goodScript.exists(),
        "This test is currently based on the existence of test_function_coercion_override_builtin.ash and test_function_coercion.ash. If removing them, please update this test.");

    initialize(new InitializeParams());

    // Once initialized, the server will automatically scan the root for .ash scripts. Just give it
    // some time.

    pauser.pause(3000);

    // The server should have autonomously sent some diagnostics to the client
    Mockito.verify(client)
        .publishDiagnostics(
            ArgumentMatchers.argThat(
                params ->
                    params.getUri().equals(badScript.toURI().toString())
                        && params.getDiagnostics().stream()
                            .anyMatch(
                                diagnostic ->
                                    diagnostic.getSeverity() == DiagnosticSeverity.Error)));
    Mockito.verify(client)
        .publishDiagnostics(
            ArgumentMatchers.argThat(
                params ->
                    params.getUri().equals(goodScript.toURI().toString())
                        && params.getDiagnostics().stream()
                            .noneMatch(
                                diagnostic ->
                                    diagnostic.getSeverity() == DiagnosticSeverity.Error)));

    // ---------- didOpen() test ----------

    // Reset invocations...
    Mockito.clearInvocations(client);

    DidOpenTextDocumentParams openParams =
        new DidOpenTextDocumentParams(
            new TextDocumentItem(badScript.toURI().toString(), "ash", 1, "unknown_function()"));
    proxyServer.getTextDocumentService().didOpen(openParams);

    // Wait for diagnostics...
    pauser.pause(3000);

    Mockito.verify(client)
        .publishDiagnostics(
            ArgumentMatchers.argThat(
                params ->
                    params.getUri().equals(badScript.toURI().toString())
                        && params.getDiagnostics().stream()
                            .anyMatch(
                                diagnostic ->
                                    diagnostic.getSeverity() == DiagnosticSeverity.Error
                                        && diagnostic
                                            .getMessage()
                                            .startsWith(
                                                "Function 'unknown_function( )' undefined."))));
    // goodScript was not changed, and so was not updated.
    Mockito.verify(client, Mockito.never())
        .publishDiagnostics(
            ArgumentMatchers.argThat(
                params -> params.getUri().equals(goodScript.toURI().toString())));

    // ---------- didChange() test ----------

    // Reset invocations...
    Mockito.clearInvocations(client);

    DidChangeTextDocumentParams changeParams =
        new DidChangeTextDocumentParams(
            new VersionedTextDocumentIdentifier(badScript.toURI().toString(), 2),
            Collections.singletonList(
                new TextDocumentContentChangeEvent("import " + goodScript.getName())));
    proxyServer.getTextDocumentService().didChange(changeParams);

    // Wait for diagnostics...
    pauser.pause(3000);

    Mockito.verify(client)
        .publishDiagnostics(
            ArgumentMatchers.argThat(
                params -> params.getUri().equals(badScript.toURI().toString())));
    // goodScript was *imported* by the new badScript, and so *was* updated.
    Mockito.verify(client)
        .publishDiagnostics(
            ArgumentMatchers.argThat(
                params -> params.getUri().equals(goodScript.toURI().toString())));

    // ---------- didChange() test, obsolete version ----------

    // Reset invocations...
    Mockito.clearInvocations(client);

    // Note how we're reverting to version 1
    changeParams =
        new DidChangeTextDocumentParams(
            new VersionedTextDocumentIdentifier(badScript.toURI().toString(), 1),
            Collections.singletonList(new TextDocumentContentChangeEvent("unknown_function()")));
    proxyServer.getTextDocumentService().didChange(changeParams);

    // Wait for diagnostics...
    pauser.pause(3000);

    // The version was lower than the one in memory, and so was ignored
    Mockito.verify(client, Mockito.never()).publishDiagnostics(ArgumentMatchers.any());

    // ---------- didClose() test ----------

    // Reset invocations...
    Mockito.clearInvocations(client);

    DidCloseTextDocumentParams closeParams =
        new DidCloseTextDocumentParams(new TextDocumentIdentifier(badScript.toURI().toString()));
    proxyServer.getTextDocumentService().didClose(closeParams);

    // Wait for diagnostics...
    pauser.pause(3000);

    // badScript should have returned to its pre-opened state.
    // goodScript, no longer imported by our version of badScript, should also have been updated.
    Mockito.verify(client)
        .publishDiagnostics(
            ArgumentMatchers.argThat(
                params ->
                    params.getUri().equals(badScript.toURI().toString())
                        && params.getDiagnostics().stream()
                            .anyMatch(
                                diagnostic ->
                                    diagnostic.getSeverity() == DiagnosticSeverity.Error)));
    Mockito.verify(client)
        .publishDiagnostics(
            ArgumentMatchers.argThat(
                params -> params.getUri().equals(goodScript.toURI().toString())));
  }

  @Test
  public void filesMonitorNewFileTest() {
    initialize(new InitializeParams());

    final File newScript = new File("/foo/bar/baz.ash");

    DidOpenTextDocumentParams openParams =
        new DidOpenTextDocumentParams(
            new TextDocumentItem(newScript.toURI().toString(), "ash", 1, "unknown_function()"));
    proxyServer.getTextDocumentService().didOpen(openParams);

    // Wait for diagnostics...
    pauser.pause(3000);

    // Since the content was supplied, the server doesn't care that the file doesn't exist
    Mockito.verify(client)
        .publishDiagnostics(
            ArgumentMatchers.argThat(
                params ->
                    params.getUri().equals(newScript.toURI().toString())
                        && params.getDiagnostics().stream()
                            .anyMatch(
                                diagnostic ->
                                    diagnostic.getSeverity() == DiagnosticSeverity.Error
                                        && diagnostic
                                            .getMessage()
                                            .startsWith(
                                                "Function 'unknown_function( )' undefined."))));

    // ---------- didClose() test ----------

    // Reset invocations...
    Mockito.clearInvocations(client);

    DidCloseTextDocumentParams closeParams =
        new DidCloseTextDocumentParams(new TextDocumentIdentifier(newScript.toURI().toString()));
    proxyServer.getTextDocumentService().didClose(closeParams);

    // Wait for diagnostics...
    pauser.pause(3000);

    // closing a file that doesn't exist causes it to be forgotten
    Mockito.verify(client, Mockito.never()).publishDiagnostics(ArgumentMatchers.any());
  }

  @Test
  public void UriToFileNullUri() {
    Assertions.assertNull(FilesMonitor.UriToFile(null));
  }

  @Test
  public void UriToFileCorrectUri() {
    Assertions.assertEquals(goodScript, FilesMonitor.UriToFile(goodScript.toURI().toString()));
  }

  @Test
  public void UriToFileNoScheme() {
    Assertions.assertNull(FilesMonitor.UriToFile("/foo/bar/baz.ash"));
  }

  @Test
  public void UriToFileBadScheme() {
    Assertions.assertNull(FilesMonitor.UriToFile("data:/foo/bar/baz.ash"));
  }

  @Test
  public void UriToFileNonEncoded() {
    StringBuilder uri = new StringBuilder("file://");

    if (!System.getProperty("os.name").toLowerCase().contains("win")) {
      uri.append('/');
    }

    // An encoded URI would read "%20a%20b.ash"
    uri.append(" a b.ash");

    Assertions.assertEquals(new File("/ a b.ash"), FilesMonitor.UriToFile(uri.toString()));
  }
}
