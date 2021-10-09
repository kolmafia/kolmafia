package net.sourceforge.kolmafia.listener;

import net.sourceforge.kolmafia.objectpool.IntegerPool;

public class ItemListenerRegistry extends ListenerRegistry {
  // The registry of listeners:
  private static final ListenerRegistry INSTANCE = new ListenerRegistry();

  public static final void registerItemListener(final int itemId, final Listener listener) {
    if (itemId < 1) {
      return;
    }

    Integer key = IntegerPool.get(itemId);
    ItemListenerRegistry.INSTANCE.registerListener(key, listener);
  }

  public static final void fireItemChanged(final int itemId) {
    Integer key = IntegerPool.get(itemId);
    ItemListenerRegistry.INSTANCE.fireListener(key);
  }
}
