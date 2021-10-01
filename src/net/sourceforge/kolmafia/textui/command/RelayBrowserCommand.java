package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class RelayBrowserCommand extends AbstractCommand {
  public RelayBrowserCommand() {
    this.usage = " [nobrowser] - start the relay server and/or open the relay browser.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.equals("nobrowser")) {
      RelayLoader.startRelayServer();
    } else RelayLoader.openRelayBrowser();
  }
}
