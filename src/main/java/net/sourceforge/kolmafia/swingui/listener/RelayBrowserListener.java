package net.sourceforge.kolmafia.swingui.listener;

import net.sourceforge.kolmafia.webui.RelayLoader;

public class RelayBrowserListener extends ThreadedListener {
  private final String location;

  public RelayBrowserListener(String location) {
    this.location = location;
  }

  @Override
  protected void execute() {
    if (this.location == null) {
      RelayLoader.openRelayBrowser();
    } else {
      RelayLoader.openSystemBrowser(this.location);
    }
  }
}
