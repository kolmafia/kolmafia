package net.sourceforge.kolmafia.swingui.menu;

import java.io.File;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import javax.swing.JComboBox;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

/**
 * Maintains a most recently used list of scripts
 *
 * @author Fronobulax
 */
public class ScriptMRUList implements Listener {
  protected int maxMRU = 16;
  protected final Deque<String> mruList = new ConcurrentLinkedDeque<>();
  private final String prefList;
  private final String prefLen;
  private static final String SEMICOLON = ";";

  public ScriptMRUList(String pList, String pLen) {
    prefList = pList;
    prefLen = pLen;

    this.init();

    PreferenceListenerRegistry.registerPreferenceListener(pLen, this);
  }

  @Override
  public void update() {
    init();
  }

  protected void init() {
    synchronized (this) {
      maxMRU = Preferences.getInteger(prefLen);
      if (maxMRU <= 0) {
        mruList.clear();
        return;
      }

      // Load list from preference - use whatever is there
      String oldValues = Preferences.getString(prefList);
      if ((oldValues != null) && (!oldValues.equals(""))) {
        // First to last, delimited by semi-colon.  Split and insert.
        String[] items = oldValues.split(SEMICOLON);
        int itemsToAdd = Math.min(maxMRU, items.length);
        for (int i = 0; i < itemsToAdd; i++) {
          mruList.addLast(items[i]);
        }
      }
    }
  }

  public void addItem(File file) {
    this.addItem(LoadScriptMenuItem.getRelativePath(file));
  }

  public void addItemInParallel(File file) {
    RequestThread.runInParallel(new AddItemRunnable(this, file), true);
  }

  private static class AddItemRunnable implements Runnable {
    private final ScriptMRUList list;
    private final File file;

    public AddItemRunnable(ScriptMRUList list, File file) {
      this.list = list;
      this.file = file;
    }

    @Override
    public void run() {
      list.addItem(file);
    }
  }

  public void addItem(String script) {
    if (maxMRU == 0) {
      return;
    }
    // don't add empty or null names
    if ((script == null) || (script.equals(""))) {
      return;
    }

    StringBuilder pref = new StringBuilder();
    synchronized (this) {
      // delete item if it is currently in list
      // note - as implemented this is a case sensitive compare
      while (mruList.contains(script)) {
        mruList.remove(script);
      }
      // add this as the first
      mruList.addFirst(script);
      // delete excess
      while (mruList.size() > maxMRU) {
        mruList.removeLast();
      }

      // save the new list as a preference
      Iterator<String> i8r = mruList.iterator();
      while (i8r.hasNext()) {
        String val = i8r.next();
        pref.append(val);
        if (i8r.hasNext()) {
          pref.append(SEMICOLON);
        }
      }
    }
    // now save it
    Preferences.setString(prefList, pref.toString());
  }

  public List<File> listAsFiles() {
    synchronized (this) {
      List<File> results = new ArrayList<>();

      if (mruList.isEmpty()) {
        return results;
      }

      for (String fileName : mruList) {
        List<File> matches = KoLmafiaCLI.findScriptFile(fileName);

        if (matches.size() == 1) {
          File match = matches.get(0);
          if (!results.contains(match)) {
            results.add(match);
          }
        }
      }

      return results;
    }
  }

  public void updateJComboData(JComboBox<String> jcb) {
    if (mruList.isEmpty()) {
      return;
    }

    jcb.removeAllItems();
    synchronized (this) {
      Iterator<String> i8r = mruList.iterator();
      int i = 0;
      while (i8r.hasNext()) {
        String val = i8r.next();
        jcb.insertItemAt(val, i);
        i++;
      }
    }
    jcb.setSelectedIndex(0);
  }

  public String getFirst() {
    String NONE = "Unknown";
    if (maxMRU <= 0) return NONE;
    if (mruList.size() < 1) return NONE;
    return mruList.getFirst();
  }
}
