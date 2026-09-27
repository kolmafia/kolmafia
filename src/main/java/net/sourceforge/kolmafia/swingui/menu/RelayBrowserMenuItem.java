package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.swingui.listener.RelayBrowserListener;

/** Internal class which displays the given request inside of the current frame. */
public class RelayBrowserMenuItem extends ThreadedMenuItem {
  private String url;

  public RelayBrowserMenuItem() {
    this("Relay Browser", null);
  }

  public RelayBrowserMenuItem(final String label, final String url) {
    super(label, new RelayBrowserListener(url));

    this.url = url;
  }

  public String getURL() {
    return this.url;
  }
}
