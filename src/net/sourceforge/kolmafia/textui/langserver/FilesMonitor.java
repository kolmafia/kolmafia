package net.sourceforge.kolmafia.textui.langserver;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants;

/**
 * Module in charge of the operations regarding our files, {@link Script scripts} and {@link
 * Script.Handler handlers}, such as finding them, making them or traversing them.
 */
public final class FilesMonitor {
  final AshLanguageServer parent;

  protected FilesMonitor(final AshLanguageServer parent) {
    this.parent = parent;
  }

  /** Fetches or makes a Script for the given file. */
  private Script getScript(final File file) {
    synchronized (this.parent.scripts) {
      Script script = this.parent.scripts.get(file);
      if (script == null) {
        script = new Script(this.parent, file);
        this.parent.scripts.put(file, script);
      }

      return script;
    }
  }

  /** Returns the currently existing {@link Script.Handler handler(s)} for the given file. */
  protected List<Script.Handler> findHandlers(final File file) {
    final List<Script.Handler> handlers = new LinkedList<>();

    synchronized (this.parent.scripts) {
      for (final Script script : this.parent.scripts.values()) {
        if (script.handler != null
            && script.handler.parser != null
            && script.handler.parser.getImports().containsKey(file)) {
          // TODO: currently untested, will be tested after server can update scripts' content
          handlers.add(script.handler);
        }
      }
    }

    return handlers;
  }

  /**
   * Returns the currently existing {@link Script.Handler handler(s)} for the given file. If there
   * is none, immediately makes one.
   */
  public List<Script.Handler> findOrMakeHandler(final File file) {
    final List<Script.Handler> handlers;

    synchronized (this.parent.scripts) {
      final Script script = this.getScript(file);

      handlers = this.findHandlers(file);

      if (handlers.size() == 0) {
        // make a new handler
        handlers.add(script.makeHandler());
      }
    }

    return handlers;
  }

  /**
   * Checks every sub-directory under our authority as a KoLmafia root folder, and ensures that
   * every {@code .ash} file in them has a {@link Script.Handler handler}.
   */
  protected void scan() {
    for (final File directory :
        Arrays.asList(
            KoLConstants.SCRIPT_LOCATION,
            KoLConstants.PLOTS_LOCATION,
            KoLConstants.RELAY_LOCATION)) {
      this.scan(directory);
    }
  }

  /**
   * Ensures that every {@code .ash} file in the given directory (and sub-directories thereof) have
   * a {@link Script.Handler handler}.
   */
  private void scan(final File directory) {
    for (final File file : DataUtilities.listFiles(directory)) {
      if (Thread.interrupted()) {
        break;
      }

      if (file.isDirectory()) {
        this.scan(file);
      } else if (file.isFile() && this.parent.canParse(file)) {
        this.findOrMakeHandler(file);
      }
    }
  }
}
