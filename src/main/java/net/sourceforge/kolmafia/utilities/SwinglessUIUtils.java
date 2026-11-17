package net.sourceforge.kolmafia.utilities;

public class SwinglessUIUtils {

  static boolean isSwingAvailable;

  static {
    try {
      // Swing is part of the java.desktop module, which may be missing
      Class.forName("javax.swing.ListModel", false, SwinglessUIUtils.class.getClassLoader());
      isSwingAvailable = true;
    } catch (ClassNotFoundException | LinkageError e) {
      // if unable to find it, we are in a Swing-less environment
      isSwingAvailable = false;
    }
  }

  private SwinglessUIUtils() {}

  public static boolean isSwingAvailable() {
    return SwinglessUIUtils.isSwingAvailable;
  }
}
