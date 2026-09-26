package net.sourceforge.kolmafia.swingui;

import java.awt.BorderLayout;
import javax.swing.JPanel;
import net.sourceforge.kolmafia.swingui.panel.StatusPanel;
import net.sourceforge.kolmafia.swingui.panel.SynthesizePanel;

public class SynthesizeFrame extends GenericFrame {
  public SynthesizeFrame() {
    super("Sweet Synthesis");

    JPanel centerPanel = new JPanel(new BorderLayout());

    centerPanel.add(new SynthesizePanel(), BorderLayout.CENTER);
    centerPanel.add(new StatusPanel(), BorderLayout.SOUTH);

    this.setCenterComponent(centerPanel);
  }
}
