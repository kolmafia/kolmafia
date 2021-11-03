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

    // We were deferred but are no longer deferred. Fire at Will!

    boolean logit = ListenerRegistry.logging && RequestLogger.isDebugging();

    synchronized (this.deferred) {
      Object[] listenerArray = new Object[this.deferred.size()];
      this.deferred.toArray(listenerArray);
      this.deferred.clear();

      for (Object key : listenerArray) {
        ArrayList<WeakReference<Listener>> listenerList = this.listenerMap.get(key);
        if (logit) {
          int count = listenerList == null ? 0 : listenerList.size();
          RequestLogger.updateDebugLog("Firing " + count + " listeners for \"" + key + "\"");
        }
        this.fireListeners(listenerList, null);
      }
    }
  }

  public final void registerListener(final Object key, final Listener listener) {
    ArrayList<WeakReference<Listener>> listenerList = null;

    synchronized (this.listenerMap) {
      listenerList = this.listenerMap.get(key);

      if (listenerList == null) {
        listenerList = new ArrayList<>();
        this.listenerMap.put(key, listenerList);
      }
    }

    WeakReference<Listener> reference = new WeakReference<>(listener);

    synchronized (listenerList) {
      listenerList.add(reference);
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

    if (logit) {
      int count = listenerList.size();
      RequestLogger.updateDebugLog(
          (this.deferring > 0 ? "Deferring " : "Firing ")
              + count
              + " listeners for \""
              + key
              + "\"");
    }

    if (this.deferring > 0) {
      synchronized (this.deferred) {
        this.deferred.add(key);
      }
      return;
    }

    this.fireListeners(listenerList, null);
  }

  public final void fireAllListeners() {
    boolean logit = ListenerRegistry.logging && RequestLogger.isDebugging();

    if (this.deferring > 0) {
      Set<Object> keys = null;
      synchronized (this.listenerMap) {
        keys = this.listenerMap.keySet();
      }
      if (logit) {
        int count = keys.size();
        RequestLogger.updateDebugLog("Deferring all listeners for " + count + " keys");
      }
      synchronized (this.deferred) {
        this.deferred.addAll(keys);
      }
      return;
    }

    HashSet<ArrayList<WeakReference<Listener>>> listeners = new HashSet<>();

    if (logit) {
      Set<Entry<Object, ArrayList<WeakReference<Listener>>>> entries = null;
      synchronized (this.listenerMap) {
        entries = this.listenerMap.entrySet();
      }

      Iterator<Entry<Object, ArrayList<WeakReference<Listener>>>> i1 = entries.iterator();
      while (i1.hasNext()) {
        Entry<Object, ArrayList<WeakReference<Listener>>> entry = i1.next();
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
