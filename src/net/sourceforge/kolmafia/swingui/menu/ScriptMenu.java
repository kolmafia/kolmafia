package net.sourceforge.kolmafia.swingui.menu;

import darrylbu.util.MenuScroller;
import java.io.File;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.swingui.GenericFrame;

/** A special class which renders the list of available scripts. */
public class ScriptMenu extends MenuItemList<File> {
  public ScriptMenu() {
    super("Scripts", (LockableListModel<File>) KoLConstants.scripts);
  }

  @Override
  public JComponent constructMenuItem(final Object o) {
    return o instanceof JSeparator ? new JSeparator() : this.constructMenuItem((File) o, "scripts");
  }

  private JComponent constructMenuItem(final File file, final String prefix) {
    // Get path components of this file
    String[] pieces;

    try {
      pieces = file.getCanonicalPath().split("[\\\\/]");
    } catch (Exception e) {
      // This should not happen.  Therefore, print
      // a stack trace for debug purposes.

      StaticEntity.printStackTrace(e);
      return null;
    }

    String name = pieces[pieces.length - 1];
    String path = prefix + File.separator + name;

    if (file.isDirectory()) {
      // Get a list of all the files
      File[] scriptList = DataUtilities.listFiles(file);

      //  Convert the list into a menu
      JMenu menu = new JMenu(name);

      MenuScroller.setScrollerFor(menu, 25);

      // Iterate through the files.  Do this in two
      // passes to make sure that directories start
      // up top, followed by non-directories.

      for (int i = 0; i < scriptList.length; ++i) {
        if (scriptList[i].isDirectory() && ScriptMenu.shouldAddScript(scriptList[i])) {
          menu.add(this.constructMenuItem(scriptList[i], path));
        }
      }

      for (int i = 0; i < scriptList.length; ++i) {
        if (!scriptList[i].isDirectory()) {
          menu.add(this.constructMenuItem(scriptList[i], path));
        }
      }

      // Return the menu
      return menu;
    }

    return new LoadScriptMenuItem(name, path);
  }

  @Override
  public JComponent[] getHeaders() {
    JComponent[] headers = new JComponent[4];

    headers[0] = new DisplayFrameMenuItem("Script Manager", "ScriptManageFrame");
    headers[1] = new LoadScriptMenuItem();
    headers[2] = new InvocationMenuItem("Refresh menu", GenericFrame.class, "compileScripts");
    headers[3] = new JMenuItem("(Shift key to edit)");
    headers[3].setEnabled(false);

    return headers;
  }

  public static final boolean shouldAddScript(final File script) {
    if (!script.isDirectory()) {
      return true;
    }

    File[] scriptList = DataUtilities.listFiles(script);

    if (scriptList == null || scriptList.length == 0) {
      return false;
    }

    for (int i = 0; i < scriptList.length; ++i) {
      if (ScriptMenu.shouldAddScript(scriptList[i])) {
        return true;
      }
    }

    return false;
  }
}
