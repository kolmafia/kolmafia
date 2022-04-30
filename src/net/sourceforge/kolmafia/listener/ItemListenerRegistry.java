package net.sourceforge.kolmafia.listener;

public class ItemListenerRegistry extends ListenerRegistry {
  // The registry of listeners:
  private static final ListenerRegistry INSTANCE = new ListenerRegistry();

  public static void registerItemListener(final int itemId, final Listener listener) {
    if (itemId < 1) {
      return;
    }

    ItemListenerRegistry.INSTANCE.registerListener(itemId, listener);
  }

  public static void unregisterItemListener(final int itemId, final Listener listener) {
    ItemListenerRegistry.INSTANCE.unregisterListener(itemId, listener);
  }

  public static void fireItemChanged(final int itemId) {
    ItemListenerRegistry.INSTANCE.fireListener(itemId);
  }
}
