package net.sourceforge.kolmafia.listener;

public class PreferenceListenerRegistry extends ListenerRegistry {
  // The registry of listeners:
  private static final ListenerRegistry INSTANCE = new ListenerRegistry();

  public static void deferPreferenceListeners(boolean deferring) {
    PreferenceListenerRegistry.INSTANCE.deferListeners(deferring);
  }

  public static void registerPreferenceListener(final String name, final Listener listener) {
    PreferenceListenerRegistry.INSTANCE.registerListener(name, listener);
  }

  public static void unregisterPreferenceListener(String name, final Listener listener) {
    PreferenceListenerRegistry.INSTANCE.unregisterListener(name, listener);
  }

  public static void firePreferenceChanged(final String name) {
    PreferenceListenerRegistry.INSTANCE.fireListener(name);
  }

  public static void fireAllPreferencesChanged() {
    PreferenceListenerRegistry.INSTANCE.fireAllListeners();
  }
}
