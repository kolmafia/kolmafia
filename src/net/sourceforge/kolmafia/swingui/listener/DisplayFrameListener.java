package net.sourceforge.kolmafia.swingui.listener;

import java.awt.Frame;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.swingui.GenericFrame;

public class DisplayFrameListener extends ThreadedListener {
  private final String frameClass;

  public DisplayFrameListener(String frameClass) {
    this.frameClass = frameClass;
  }

  @Override
  protected void execute() {
    if (this.frameClass == null) {
      String interfaceSetting = Preferences.getString("initialDesktop");

      Frame[] frames = Frame.getFrames();

      for (Frame value : frames) {
        if ((value instanceof GenericFrame frame)) {
          if (frame.showInWindowMenu() && !interfaceSetting.contains(frame.getFrameName())) {
            frame.setVisible(true);
          }
        }
      }

      if (KoLDesktop.instanceExists()) {
        KoLDesktop.getInstance().setVisible(true);
      }
    } else {
      KoLmafiaGUI.constructFrame(this.frameClass);
    }
  }
}
