package net.sourceforge.kolmafia.textui.langserver;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sourceforge.kolmafia.textui.Parser;
import net.sourceforge.kolmafia.textui.Parser.AshDiagnostic;
import net.sourceforge.kolmafia.textui.parsetree.Scope;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.PublishDiagnosticsParams;

/**
 * A file that was recognized as a KoLmafia script in one of the directories under our authority.
 * Takes care of recording unsaved changes made to it.
 */
public final class Script {
  final AshLanguageServer parent;
  final File file;

  /**
   * The {@link Handler} in charge of this script. <i>Only</i> exists if this script is <b>not</b>
   * imported by any other script.
   *
   * <p>When trying to find the handler(s) in charge of a given file, {@link
   * FilesMonitor#findHandlers(File)} should be used.
   */
  protected Handler handler;

  protected int version = -1;
  /** The current content of the file. Is {@code null} if the file is currently closed */
  protected String text;

  protected Script(final AshLanguageServer parent, final File file) {
    this.parent = parent;
    this.file = file;
  }

  protected Handler makeHandler() {
    this.handler = new Handler();

    this.parent.executor.execute(
        () -> {
          // Check in case the handler is closed before we're ran
          if (this.handler != null) {
            this.handler.parseFile(true);
          }
        });

    return this.handler;
  }

  private InputStream getStream() {
    if (this.text == null) {
      return null;
    }

    return new ByteArrayInputStream(this.text.getBytes());
  }

  /**
   * An object tasked with taking care of a {@link Script}, such as parsing it and sending the
   * resulting diagnostics to the client once the parsing ends.
   *
   * <p>All files imported by this script should also be handled by this object
   */
  public final class Handler {
    protected Parser parser;
    protected Scope scope;

    private Thread parserThread;

    private final Object parserSwapLock = new Object();
    private final Object parserThreadWaitingLock = new Object();

    // Public class, private constructor
    private Handler() {}

    protected void refreshParsing() {
      this.parseFile(false);
    }

    private void parseFile(final boolean initialParsing) {
      final String previousThreadName = Thread.currentThread().getName();

      // If another thread was already parsing this script, kick them out; we're in charge now.
      synchronized (this.parserSwapLock) {
        if (this.parserThread != null) {
          // (...but first, quickly make sure it's not due to congestion)
          if (Script.this.handler != this) {
            // (oh... never mind, then...)
            return;
          }

          this.parserThread.interrupt();
        }
        this.parserThread = Thread.currentThread();
        this.parserThread.setName(Script.this.file.getName() + " - Parser");
      }

      this.parser =
          new LSParser(
              Script.this.file,
              Script.this.getStream(),
              Collections.synchronizedMap(new HashMap<>()));

      try {
        // Parse the script
        this.scope = this.parser.parse();

        // If we managed to parse it without interruption, send the diagnostics
        Script.this.parent.executor.execute(this::sendDiagnostics);
      } catch (InterruptedException e) {
      } finally {
        synchronized (this.parserSwapLock) {
          if (this.parserThread == Thread.currentThread()) {
            this.parserThread = null;

            synchronized (this.parserThreadWaitingLock) {
              this.parserThreadWaitingLock.notifyAll();
            }

            if (!initialParsing) {
              // In case some imports were removed, these scripts are now without a Handler
              // Trigger a scan to find them and give them one
              Script.this.parent.monitor.scan();
            }
          }

          Thread.currentThread().setName(previousThreadName);
        }
      }
    }

    private void sendDiagnostics() {
      if (Script.this.parent.client == null) {
        return;
      }

      this.waitForParsing();

      if (Script.this.handler != this || this.parser == null || Thread.interrupted()) {
        // We've been kicked out
        return;
      }

      final String previousThreadName = Thread.currentThread().getName();
      // Will technically send the diagnostics of every file it imports, but
      // don't change the name accordingly; it would change too fast.
      Thread.currentThread().setName(Script.this.file.getName() + " - Diagnostics");

      synchronized (this.parser.getImports()) {
        for (final Map.Entry<File, Parser> entry : this.parser.getImports().entrySet()) {
          final File file = entry.getKey();
          final Parser parser = entry.getValue();

          final List<Diagnostic> diagnostics = new ArrayList<>();

          for (final AshDiagnostic diagnostic : parser.getDiagnostics()) {
            if (diagnostic.originatesFrom(parser)) {
              diagnostics.add(diagnostic.toLspDiagnostic());
            }
          }

          Script.this.parent.client.publishDiagnostics(
              new PublishDiagnosticsParams(file.toURI().toString(), diagnostics));
        }
      }

      Thread.currentThread().setName(previousThreadName);
    }

    protected void close() {
      Script.this.handler = null;

      synchronized (this.parserSwapLock) {
        if (this.parserThread != null) {
          this.parserThread.interrupt();
        }
      }
    }

    /** Blocks as long as we're still parsing the current script. */
    private void waitForParsing() {
      synchronized (this.parserThreadWaitingLock) {
        while (this.parserThread != null) {
          if (Script.this.handler != this) {
            // We've been kicked out
            return;
          }

          try {
            this.parserThreadWaitingLock.wait();
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    }

    /** Custom Parser that submits edited file contents, if we have them. */
    private class LSParser extends Parser {
      private LSParser(
          final File scriptFile, final InputStream stream, final Map<File, Parser> imports) {
        super(scriptFile, stream, imports);
      }

      @Override
      protected InputStream getInputStream(final File scriptFile) {
        synchronized (Script.this.parent.scripts) {
          final Script script = Script.this.parent.scripts.get(scriptFile);

          if (script != null) {
            if (script.handler != null) {
              // The Handler that made the Parser that called this method
              // is now in charge of this file
              script.handler.close();
            }

            return script.getStream();
          }
        }

        return null;
      }
    }
  }
}
