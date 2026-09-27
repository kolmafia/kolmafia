package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import net.sourceforge.kolmafia.swingui.panel.GearChangePanel;

public class GearChangeFrame extends GenericFrame {
  public GearChangeFrame() {
    super("Gear Changer");

    JPanel centerPanel = new JPanel(new BorderLayout());

    centerPanel.add(new GearChangePanel(), BorderLayout.CENTER);

    this.setCenterComponent(centerPanel);
  }
}
