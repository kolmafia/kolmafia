package net.sourceforge.kolmafia.listener;

public class NamedListenerRegistry extends ListenerRegistry {
  // The registry of listeners:
  private static final ListenerRegistry INSTANCE = new ListenerRegistry();

  public static void deferNamedListeners(boolean deferring) {
    NamedListenerRegistry.INSTANCE.deferListeners(deferring);
  }

  public static final void registerNamedListener(final String name, final Listener listener) {
    NamedListenerRegistry.INSTANCE.registerListener(name, listener);
  }

  public static final void fireChange(final String name) {
    NamedListenerRegistry.INSTANCE.fireListener(name);
  }
}
