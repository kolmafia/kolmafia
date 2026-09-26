package net.sourceforge.kolmafia.swingui.panel;

import java.awt.BorderLayout;
import javax.swing.JPanel;

public class StatusPanel extends GenericPanel {
  public StatusPanel() {
    super(null, false);
    this.container = new JPanel();
    this.container.setLayout(new BorderLayout(0, 0));
    this.setLayout(new BorderLayout(0, 0));
    this.add(this.container, BorderLayout.NORTH);
    this.contentSet = true;
    this.addStatusLabel();
  }

  @Override
  public boolean shouldAddStatusLabel() {
    return true;
  }

  @Override
  public void actionConfirmed() {}

  @Override
  public void actionCancelled() {}
}
