package net.sourceforge.kolmafia.swingui.button;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.swingui.listener.DisplayFrameListener;

/**
 * In order to keep the user interface from freezing (or at least appearing to freeze), this
 * internal class is used to process the request for viewing frames.
 */
public class DisplayFrameButton extends ThreadedButton {

  public DisplayFrameButton(final String text, final String frameClass) {
    super(text, new DisplayFrameListener(frameClass));
  }

  public DisplayFrameButton(final String tooltip, final String icon, final String frameClass) {
    super(JComponentUtilities.getImage(icon), new DisplayFrameListener(frameClass));
    JComponentUtilities.setComponentSize(this, 32, 32);
    this.setToolTipText(tooltip);
  }
}
