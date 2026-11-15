package net.sourceforge.kolmafia.utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.preferences.Preferences;

/* Like an MRUList, but maintains a list of "default" settings shown below the recent entries. */
public class PartialMRUList extends ScriptMRUList implements Listener {
  private final LinkedList<String> defaultList = new LinkedList<>();
  private final String pDefaultList;
  private boolean isInit = false;

  /**
   * @param pList partial MRU list as a semi-colon delimited string
   * @param pLen length of the MRU list
   */
  public PartialMRUList(String pList, String pLen, String pDefaultList) {
    super(pList, pLen);
    this.pDefaultList = pDefaultList;
    update();
    PreferenceListenerRegistry.registerPreferenceListener(pDefaultList, this);
  }

  @Override
  public void update() {
    // Ensure we update the properties in ScriptMRUList
    super.update();

    String[] newlist = Preferences.getString(this.pDefaultList).split(" \\| ");
    this.defaultList.clear();
    Collections.addAll(this.defaultList, newlist);
  }

  /**
   * The entries a combo box offers: the most recent entries, then the defaults. The first call
   * reloads the recent entries from the preferences.
   */
  public synchronized List<String> getComboItems() {
    if (!isInit) {
      init();
      isInit = true;
    }

    List<String> items = new ArrayList<>();

    int mruEntries = this.getRecentCount();
    int entriesAdded = 0;
    for (String ob : mruList) {
      if (entriesAdded >= mruEntries) {
        break;
      }
      items.add(ob);
      entriesAdded++;
    }

    items.addAll(defaultList);
    return items;
  }

  /** How many of the combo items are recent entries rather than defaults. */
  public int getRecentCount() {
    return Math.min(mruList.size(), PartialMRUList.this.maxMRU);
  }
}
