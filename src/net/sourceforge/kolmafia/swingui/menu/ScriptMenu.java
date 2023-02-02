package net.sourceforge.kolmafia.swingui.menu;

import darrylbu.util.MenuScroller;
import java.io.File;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.GenericFrame;

/** A special class which renders the list of available scripts. */
public class ScriptMenu extends JMenu implements Listener {
  public ScriptMenu() {
    super("Scripts");

    init();

    PreferenceListenerRegistry.registerPreferenceListener("scriptMRUList", this);
    PreferenceListenerRegistry.registerPreferenceListener("scriptMRULength", this);
    PreferenceListenerRegistry.registerPreferenceListener("scriptCascadingMenus", this);
  }

  @Override
  public void update() {
    GenericFrame.compileScripts();
    SwingUtilities.invokeLater(this::init);
  }

  protected void init() {
    boolean useMRUList = Preferences.getInteger("scriptMRULength") > 0;
    boolean useCascadingMenus = Preferences.getBoolean("scriptCascadingMenus");

    removeAll();

    add(new DisplayFrameMenuItem("Script Manager", "ScriptManageFrame"));
    add(new LoadScriptMenuItem());
    int headers = 2;

    if (!useMRUList && !useCascadingMenus) {
      return;
    }

    List<File> files =
        List.copyOf(useMRUList ? KoLConstants.scriptMRUList.listAsFiles() : KoLConstants.scripts);

    if (files.size() == 0) {
      return;
    }

    if (!useMRUList) {
      add(new InvocationMenuItem("Refresh menu", this, "update"));
      headers++;
    }
    JMenuItem shiftToEditMenuItem = new JMenuItem("(Shift key to edit)");
    shiftToEditMenuItem.setEnabled(false);
    add(shiftToEditMenuItem);
    add(new JSeparator());
    headers += 2;

    MenuScroller.setScrollerFor(this, 25, 150, headers, 0);

    for (File file : files) {
      add(useMRUList ? new LoadScriptMenuItem(file) : constructMenuItem(file, "scripts"));
    }
  }

  public void dispose() {
    PreferenceListenerRegistry.unregisterPreferenceListener("scriptMRUList", this);
    PreferenceListenerRegistry.unregisterPreferenceListener("scriptMRULength", this);
    PreferenceListenerRegistry.unregisterPreferenceListener("scriptCascadingMenus", this);
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

      for (File script : scriptList) {
        if (script.isDirectory() && ScriptMenu.shouldAddScript(script)) {
          menu.add(this.constructMenuItem(script, path));
        }
      }

      for (File script : scriptList) {
        if (!script.isDirectory()) {
          menu.add(this.constructMenuItem(script, path));
        }
      }

      // Return the menu
      return menu;
    }

    return new LoadScriptMenuItem(name, path);
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
