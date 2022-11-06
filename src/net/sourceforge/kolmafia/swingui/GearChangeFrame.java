package net.sourceforge.kolmafia.swingui;

import net.sourceforge.kolmafia.swingui.panel.GearChangePanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public class GearChangeFrame extends GenericFrame {
  public GearChangeFrame() {
    super("Gear Changer");

    JPanel centerPanel = new JPanel(new BorderLayout());

    centerPanel.add(new GearChangePanel(), BorderLayout.CENTER);

    this.setCenterComponent(centerPanel);
  }
}
