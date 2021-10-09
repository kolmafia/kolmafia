package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class ForumCommand extends AbstractCommand {
  public ForumCommand() {
    this.usage = " - visit the official KoL forums.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    RelayLoader.openSystemBrowser("http://forums.kingdomofloathing.com/");
  }
}
