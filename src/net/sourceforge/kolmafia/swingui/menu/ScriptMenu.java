package net.sourceforge.kolmafia.swingui.menu;

import java.io.File;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

/** A special class which renders the list of available scripts. */
public class ScriptMenu extends JMenu implements Listener {
  public ScriptMenu() {
    super("Scripts");

    init();

    PreferenceListenerRegistry.registerPreferenceListener("scriptMRUList", this);
  }

  @Override
  public void update() {
    SwingUtilities.invokeLater(this::init);
  }

  protected void init() {
    removeAll();

    add(new DisplayFrameMenuItem("Script Manager", "ScriptManageFrame"));
    add(new LoadScriptMenuItem());

    File[] files = KoLConstants.scriptMRUList.listAsFiles();

    if (files.length == 0) {
      return;
    }

    JMenuItem shiftToEditMenuItem = new JMenuItem("(Shift key to edit)");
    shiftToEditMenuItem.setEnabled(false);

    add(shiftToEditMenuItem);
    add(new JSeparator());

    for (int i = 0; i < files.length; i++) {
      add(new LoadScriptMenuItem(files[i]));
    }
  }

  public void dispose() {
    PreferenceListenerRegistry.unregisterPreferenceListener("scriptMRUList", this);
  }
}
