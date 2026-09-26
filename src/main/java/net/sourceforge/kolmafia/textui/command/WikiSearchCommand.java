package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.utilities.StringUtilities;
import net.sourceforge.kolmafia.webui.RelayLoader;

public class WikiSearchCommand extends AbstractCommand {
  public WikiSearchCommand() {
    this.usage = " <searchText> - perform search on KoL Wiki.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    RelayLoader.openSystemBrowser(
        "https://wiki.kingdomofloathing.com/Special:Search?search="
            + StringUtilities.getURLEncode(parameters)
            + "&go=Go");
  }
}
