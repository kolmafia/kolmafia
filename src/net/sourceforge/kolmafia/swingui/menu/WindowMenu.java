package net.sourceforge.kolmafia.swingui.menu;

import javax.swing.JComponent;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.KoLGUIConstants;

public class WindowMenu extends MenuItemList<String> {
  public WindowMenu() {
    super("Window", KoLGUIConstants.existingFrames);
  }

  @Override
  public JComponent constructMenuItem(final Object o) {
    String frameKey = (String) o;
    String frameTitle = frameKey;

    for (String[] frame : KoLConstants.FRAME_NAMES) {
      if (frame[1].equals(frameKey)) {
        frameTitle = frame[0];
      }
    }

    return new DisplayFrameMenuItem(frameTitle, frameKey);
  }

  @Override
  public JComponent[] getHeaders() {
    JComponent[] headers = new JComponent[1];
    headers[0] = new DisplayFrameMenuItem("Show All Displays", null);
    return headers;
  }
}
