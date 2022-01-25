package net.sourceforge.kolmafia.swingui.button;

import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.swingui.GenericFrame;
import net.sourceforge.kolmafia.swingui.GenericPanelFrame;

/**
 * An internal class used to handle requests to open a new frame using a local panel inside of the
 * adventure frame.
 */
public class DisplayPanelFrameButton extends ThreadedButton {
  public DisplayPanelFrameButton(final String tooltip, final String icon, final ActionPanel panel) {
    super(JComponentUtilities.getImage(icon), new DisplayPanelFrameRunnable(tooltip, panel));
    JComponentUtilities.setComponentSize(this, 32, 32);
    this.setToolTipText(tooltip);
  }

  private static class DisplayPanelFrameRunnable implements Runnable {
    private final Object[] parameters;

    public DisplayPanelFrameRunnable(final String tooltip, final ActionPanel panel) {
      this.parameters = new Object[2];
      this.parameters[0] = tooltip;
      this.parameters[1] = panel;
    }

    @Override
    public void run() {
      GenericFrame.createDisplay(GenericPanelFrame.class, this.parameters);
    }
  }
}
