package net.sourceforge.kolmafia.textui.langserver;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import net.sourceforge.kolmafia.KoLConstants;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;
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
    Assertions.assertTrue(
        badScript.exists() && goodScript.exists(),
        "This test is currently based on the existence of test_function_coercion_override_builtin.ash and test_function_coercion.ash. If removing them, please update this test.");

    Assertions.assertDoesNotThrow(
        (ThrowingSupplier<InitializeResult>) proxyServer.initialize(new InitializeParams())::get,
        "Initialization failed");

    // Once initialized, the server will automatically scan the root for .ash scripts. Just give it
    // some time.

    pauser.pause(3000);

    // The server should have autonomously sent some diagnostics to the client
    Mockito.verify(client)
        .publishDiagnostics(
            ArgumentMatchers.argThat(
                params -> {
                  try {
                    return new File(new URI(params.getUri())).equals(badScript)
                        && params.getDiagnostics().stream()
                            .anyMatch(
                                diagnostic -> diagnostic.getSeverity() == DiagnosticSeverity.Error);
                  } catch (URISyntaxException e) {
                    return false;
                  }
                }));
    Mockito.verify(client)
        .publishDiagnostics(
            ArgumentMatchers.argThat(
                params -> {
                  try {
                    return new File(new URI(params.getUri())).equals(goodScript)
                        && params.getDiagnostics().stream()
                            .noneMatch(
                                diagnostic -> diagnostic.getSeverity() == DiagnosticSeverity.Error);
                  } catch (URISyntaxException e) {
                    return false;
                  }
                }));
  }
}
