package net.sourceforge.kolmafia.utilities;

import net.java.dev.spellcast.utilities.LockableListModel;

public class SwinglessUIUtils {

  static boolean isSwingAvailable;

  static {
    try {
      // try to instantiate a LockableListModel, which implements Swing
      // interfaces
      new LockableListModel<Object>();
      isSwingAvailable = true;
    } catch (NoClassDefFoundError e) {
      // if unable to do so, we are in a Swing-less environment
      isSwingAvailable = false;
    }
  }

  public static boolean isSwingAvailable() {
    return SwinglessUIUtils.isSwingAvailable;
  }
}
