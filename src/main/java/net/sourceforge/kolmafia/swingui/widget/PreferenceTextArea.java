package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Dimension;
import javax.swing.JTextArea;
import net.sourceforge.kolmafia.KoLGUIConstants;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

public abstract class PreferenceTextArea extends JTextArea implements Listener {
  public PreferenceTextArea(String... prefs) {
    this.setColumns(40);
    this.setLineWrap(true);
    this.setWrapStyleWord(true);
    this.setEditable(false);
    this.setOpaque(false);
    this.setFont(KoLGUIConstants.DEFAULT_FONT);

    for (String pref : prefs) {
      PreferenceListenerRegistry.registerPreferenceListener(pref, this);
    }
  }

  @Override
  public Dimension getMaximumSize() {
    return this.getPreferredSize();
  }
}
