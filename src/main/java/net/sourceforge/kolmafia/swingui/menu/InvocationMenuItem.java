package net.sourceforge.kolmafia.swingui.menu;

import net.sourceforge.kolmafia.swingui.listener.InvocationListener;

/**
 * Internal class used to invoke the given no-parameter method on the given object. This is used
 * whenever there is the need to invoke a method and the creation of an additional class is
 * unnecessary.
 */
public class InvocationMenuItem extends ThreadedMenuItem {
  public InvocationMenuItem(final String title, final Object object, final String methodName) {
    super(
        title,
        new InvocationListener(object, object == null ? null : object.getClass(), methodName));
  }

  public InvocationMenuItem(final String title, final Class<?> c, final String methodName) {
    super(title, new InvocationListener(null, c, methodName));
  }
}
