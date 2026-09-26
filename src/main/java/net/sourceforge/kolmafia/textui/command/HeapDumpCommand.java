package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.StaticEntity;

public class HeapDumpCommand extends AbstractCommand {
  public HeapDumpCommand() {
    this.usage =
        " - creates a heap snapshot which is used to help debug memory problems (requires use of the JDK instead of the JRE, requires Java 1.6+). "
            + "Heap snapshot files are separate from debug logs but will be placed in the same folder. "
            + "Please note that heap dumps are a KoLmafia memory snapshot so they may contain your KoL passwords. "
            + "As such, please be mindful of how you share these files.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    StaticEntity.generateHeapDump();
  }
}
