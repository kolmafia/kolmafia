package net.sourceforge.kolmafia.utilities;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import net.sourceforge.kolmafia.KoLConstants;
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
    this.addItem(ScriptMRUList.getRelativePath(file));
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

  public String getFirst() {
    String NONE = "Unknown";
    if (maxMRU <= 0) return NONE;
    if (mruList.size() < 1) return NONE;
    return mruList.getFirst();
  }

  /** The entries, most recent first. */
  public List<String> getItems() {
    synchronized (this) {
      return new ArrayList<>(this.mruList);
    }
  }

  /** A script's path relative to the directory it is run from, as the script menus show it. */
  public static final String getRelativePath(File file) {
    URI baseURI = KoLConstants.ROOT_LOCATION.getAbsoluteFile().toURI();
    URI fileURI = file.getAbsoluteFile().toURI();

    URI relativeURI = baseURI.relativize(fileURI);

    String relativePath = relativeURI.getPath();

    while (relativePath.startsWith("./")) {
      relativePath = relativePath.substring(2);
    }

    if (relativePath.startsWith(KoLConstants.SCRIPT_DIRECTORY)) {
      return relativePath.substring(KoLConstants.SCRIPT_DIRECTORY.length());
    } else if (relativePath.startsWith(KoLConstants.PLOTS_DIRECTORY)) {
      return relativePath.substring(KoLConstants.PLOTS_DIRECTORY.length());
    } else if (relativePath.startsWith(KoLConstants.RELAY_DIRECTORY)) {
      return relativePath.substring(KoLConstants.RELAY_DIRECTORY.length());
    } else {
      return file.getAbsolutePath();
    }
  }
}
