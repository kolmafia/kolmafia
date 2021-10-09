package net.sourceforge.kolmafia.swingui.widget;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JColorChooser;
import javax.swing.JLabel;
import net.java.dev.spellcast.utilities.DataUtilities;
import net.sourceforge.kolmafia.preferences.Preferences;

public class ColorChooser extends JLabel implements MouseListener {
  protected String property;

  public ColorChooser(final String property) {
    this.property = property;
    this.setOpaque(true);
    this.addMouseListener(this);
  }

  public void mousePressed(final MouseEvent e) {
    Color c = JColorChooser.showDialog(null, "Choose a color:", this.getBackground());
    if (c == null) {
      return;
    }

    Preferences.setString(this.property, DataUtilities.toHexString(c));
    this.setBackground(c);
    this.applyChanges();
  }

  public void mouseReleased(final MouseEvent e) {}

  public void mouseClicked(final MouseEvent e) {}

  public void mouseEntered(final MouseEvent e) {}

  public void mouseExited(final MouseEvent e) {}

  public void applyChanges() {}
}
