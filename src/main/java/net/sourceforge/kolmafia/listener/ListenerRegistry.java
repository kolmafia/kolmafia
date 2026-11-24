package net.sourceforge.kolmafia.listener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;

public class ListenerRegistry {
  // A registry of listeners:
  private final HashMap<Object, ArrayList<WeakReference<Listener>>> listenerMap = new HashMap<>();

  // For testing
  protected void clear() {
    this.listenerMap.clear();
  }

  // Logging. For now, this applies to all types of listeners
  private static boolean logging = false;

  public static final void setLogging(final boolean logging) {
    ListenerRegistry.logging = logging;
  }

  // Deferring
  private final HashSet<Object> deferred = new HashSet<>();
  private int deferring = 0;

  public ListenerRegistry() {}

  public void deferListeners(boolean deferring) {
    Object[] deferredKeys;

    synchronized (this.deferred) {
      // If we are deferring, increment defer level
      if (deferring) {
        this.deferring += 1;
        return;
      }

      // If we are undeferring but are not deferred, do nothing
      if (this.deferring == 0) {
        return;
      }

      // If we are undeferring and are still deferred, nothing more to do
      if (--this.deferring > 0) {
        return;
      }

      deferredKeys = this.deferred.toArray();
      this.deferred.clear();
    }

    // We were deferred but are no longer deferred. Fire at Will!
    for (var key : deferredKeys) {
      this.fireListener(key);
    }
  }

  public final void registerListener(final Object key, final Listener listener) {
    ArrayList<WeakReference<Listener>> listenerList = null;

    synchronized (this.listenerMap) {
      listenerList = this.listenerMap.computeIfAbsent(key, k -> new ArrayList<>());
    }

    WeakReference<Listener> reference = new WeakReference<>(listener);

    synchronized (listenerList) {
      listenerList.add(reference);
    }
  }

  public final void unregisterListener(final Object key, final Listener listener) {
    ArrayList<WeakReference<Listener>> listenerList;

    synchronized (this.listenerMap) {
      listenerList = this.listenerMap.get(key);
    }

    if (listenerList == null) {
      return;
    }

    synchronized (listenerList) {
      listenerList.removeIf(ref -> listener.equals(ref.get()));
    }
  }

  public final void fireListener(final Object key) {
    ArrayList<WeakReference<Listener>> listenerList = null;

    synchronized (this.listenerMap) {
      listenerList = this.listenerMap.get(key);
    }

    if (listenerList == null) {
      return;
    }

    boolean logit = ListenerRegistry.logging && RequestLogger.isDebugging();

    boolean isDeferred;
    synchronized (this.deferred) {
      isDeferred = this.deferring > 0;
      if (isDeferred) {
        this.deferred.add(key);
      }
    }

    if (logit) {
      int count = listenerList.size();
      RequestLogger.updateDebugLog(
          (isDeferred ? "Deferring " : "Firing ") + count + " listeners for \"" + key + "\"");
    }

    if (isDeferred) {
      return;
    }

    this.fireListeners(listenerList, null);
  }

  public final void fireAllListeners() {
    boolean logit = ListenerRegistry.logging && RequestLogger.isDebugging();

    synchronized (this.deferred) {
      if (this.deferring > 0) {
        synchronized (this.listenerMap) {
          this.deferred.addAll(this.listenerMap.keySet());
        }
        if (logit) {
          RequestLogger.updateDebugLog("Deferring all listeners");
        }
        return;
      }
    }

    HashSet<ArrayList<WeakReference<Listener>>> listeners = new HashSet<>();

    if (logit) {
      Set<Entry<Object, ArrayList<WeakReference<Listener>>>> entries = null;
      synchronized (this.listenerMap) {
        entries = this.listenerMap.entrySet();
      }

      for (Entry<Object, ArrayList<WeakReference<Listener>>> entry : entries) {
        Object key = entry.getKey();
        ArrayList<WeakReference<Listener>> listenerList = entry.getValue();
        int count = listenerList == null ? 0 : listenerList.size();
        RequestLogger.updateDebugLog("Firing " + count + " listeners for \"" + key + "\"");
        listeners.add(listenerList);
      }
    } else {
      Collection<ArrayList<WeakReference<Listener>>> values = null;
      synchronized (this.listenerMap) {
        values = this.listenerMap.values();
      }
      listeners.addAll(values);
    }

    Iterator<ArrayList<WeakReference<Listener>>> i2 = listeners.iterator();
    HashSet<Listener> notified = new HashSet<>();

    while (i2.hasNext()) {
      this.fireListeners(i2.next(), notified);
    }
  }

  private void fireListeners(
      final ArrayList<WeakReference<Listener>> listenerList, final HashSet<Listener> notified) {
    if (listenerList == null) {
      return;
    }

    synchronized (listenerList) {
      Iterator<WeakReference<Listener>> i = listenerList.iterator();

      while (i.hasNext()) {
        WeakReference<Listener> reference = i.next();

        Listener listener = reference.get();

        if (listener == null) {
          i.remove();
          continue;
        }

        if (notified != null) {
          if (notified.contains(listener)) {
            continue;
          }

          notified.add(listener);
        }

        try {
          listener.update();
        } catch (Exception e) {
          // Don't let a botched listener interfere with
          // the code that modified the preference.

          StaticEntity.printStackTrace(e);
        }
      }
    }
  }
}
