package net.sourceforge.kolmafia.textui.command;

public class MemoryCleanupCommand extends AbstractCommand {
  public MemoryCleanupCommand() {
    this.usage = " - force Java garbage collection.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    System.gc();
  }
}
