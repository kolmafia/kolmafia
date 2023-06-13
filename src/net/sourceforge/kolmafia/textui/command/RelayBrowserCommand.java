package net.sourceforge.kolmafia.textui.command;

import net.sourceforge.kolmafia.webui.RelayLoader;
import net.sourceforge.kolmafia.webui.RelayServer;

public class RelayBrowserCommand extends AbstractCommand {
  public RelayBrowserCommand() {
    this.usage = " [nobrowser|stop] - start/stop the relay server and/or open the relay browser.";
  }

  @Override
  public void run(final String cmd, final String parameters) {
    if (parameters.equals("nobrowser")) {
      RelayLoader.startRelayServer();
    } else if (parameters.equals("stop")) {
      RelayServer.stop();
    } else RelayLoader.openRelayBrowser();
  }
}
