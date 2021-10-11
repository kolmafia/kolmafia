package net.sourceforge.kolmafia.swingui;

import javax.swing.JPanel;
import net.sourceforge.kolmafia.swingui.panel.MushroomPlotPanel;
import net.sourceforge.kolmafia.swingui.panel.MushroomScriptPanel;

public class MushroomFrame extends GenericFrame {
  public static final int MAX_FORECAST = 11;

  public MushroomFrame() {
    super("Mushroom Plot");

    JPanel plantPanel = new JPanel();
    plantPanel.add(new MushroomPlotPanel());

    this.tabs.addTab("One Day Planting", plantPanel);

    JPanel planPanel = new JPanel();
    planPanel.add(new MushroomScriptPanel());

    this.tabs.addTab("Script Generator", planPanel);

    this.setCenterComponent(this.tabs);
  }
}
