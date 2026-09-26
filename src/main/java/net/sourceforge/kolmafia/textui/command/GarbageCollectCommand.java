package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.KoLmafia;

public class GarbageCollectCommand extends AbstractCommand {
  public GarbageCollectCommand() {
    this.usage = " attempts to reclaim unused memory to temporarily reduce KoLmafia's memory usage";
  }

  @Override
  public void run(String cmd, String parameters) {
    KoLmafia.gc();
  }
}
