package net.sourceforge.kolmafia.swingui.menu;

import javax.swing.JComponent;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLConstants;

/**
 * A special class which renders the menu holding the list of bookmarks. This class also
 * synchronizes with the list of available bookmarks.
 */
public class BookmarkMenu extends MenuItemList<String> {
  public BookmarkMenu() {
    super("Bookmarks", (LockableListModel<String>) KoLConstants.bookmarks, false);
  }

  @Override
  public JComponent constructMenuItem(final Object o) {
    String[] bookmarkData = ((String) o).split("\\|");

    String name = bookmarkData[0];
    String location = bookmarkData[1];
    String pwdhash = bookmarkData[2];

    if (location.startsWith("http://")
        || location.startsWith("https://")
        || (location.startsWith("/") && location.contains(".php"))) {
      if (pwdhash.equals("true")) {
        location += "&pwd";
      }

      return new RelayBrowserMenuItem(name, location);
    } else {
      return new LoadScriptMenuItem(name, location);
    }
  }

  @Override
  public JComponent[] getHeaders() {
    JComponent[] headers = new JComponent[0];
    return headers;
  }
}
