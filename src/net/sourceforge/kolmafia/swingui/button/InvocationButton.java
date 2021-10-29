package net.sourceforge.kolmafia.swingui.button;

import net.java.dev.spellcast.utilities.JComponentUtilities;
import net.sourceforge.kolmafia.swingui.listener.InvocationListener;

/**
 * Internal class used to invoke the given no-parameter method on the given object. This is used
 * whenever there is the need to invoke a method and the creation of an additional class is
 * unnecessary.
 */
public class InvocationButton extends ThreadedButton {
  public InvocationButton(final String text, final Object object, final String methodName) {
    super(
        text,
        new InvocationListener(object, object == null ? null : object.getClass(), methodName));
  }

  public InvocationButton(final String text, final Class<?> c, final String methodName) {
    super(text, new InvocationListener(null, c, methodName));
  }

  public InvocationButton(
      final String tooltip, final String icon, final Object object, final String methodName) {
    super(
        JComponentUtilities.getImage(icon),
        new InvocationListener(object, object == null ? null : object.getClass(), methodName));
    JComponentUtilities.setComponentSize(this, 32, 32);

    this.setToolTipText(tooltip);
  }

  public InvocationButton(
      final String tooltip, final String icon, final Class<?> c, final String methodName) {
    super(JComponentUtilities.getImage(icon), new InvocationListener(null, c, methodName));
    JComponentUtilities.setComponentSize(this, 32, 32);

    this.setToolTipText(tooltip);
  }
}
