package net.sourceforge.kolmafia.listener;

public class PreferenceListenerRegistry extends ListenerRegistry {
  // The registry of listeners:
  private static final ListenerRegistry INSTANCE = new ListenerRegistry();

  // For testing
  public static void reset() {
    INSTANCE.clear();
  }

  public static void deferPreferenceListeners(boolean deferring) {
    PreferenceListenerRegistry.INSTANCE.deferListeners(deferring);
  }

  public static void registerPreferenceListener(final String name, final Listener listener) {
    PreferenceListenerRegistry.INSTANCE.registerListener(name, listener);
  }

  public static void registerPreferenceListener(final String[] names, final Listener listener) {
    for (var name : names) {
      registerPreferenceListener(name, listener);
    }
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
