package net.sourceforge.kolmafia.swingui;

import javax.swing.JFrame;
import javax.swing.ToolTipManager;
import net.sourceforge.kolmafia.ImageCachingEditorKit;
import net.sourceforge.kolmafia.request.GenericRequest;

public class ShowHTMLFrame extends RequestFrame {
  private static ShowHTMLFrame INSTANCE = null;

  public ShowHTMLFrame() {
    super("HTML");
    this.mainDisplay.setEditorKit(new ImageCachingEditorKit());
    ToolTipManager.sharedInstance().registerComponent(this.mainDisplay);
    ShowHTMLFrame.INSTANCE = this;
  }

  @Override
  public boolean hasSideBar() {
    return false;
  }

  public static final void showRequest(final GenericRequest request) {
    if (ShowHTMLFrame.INSTANCE == null) {
      GenericFrame.createDisplay(ShowHTMLFrame.class);
    } else {
      // Ensure it is brought to front if only refreshing
      int sta = ShowHTMLFrame.INSTANCE.getExtendedState() & ~JFrame.ICONIFIED & JFrame.NORMAL;
      ShowHTMLFrame.INSTANCE.setExtendedState(sta);
      ShowHTMLFrame.INSTANCE.setAlwaysOnTop(true);
      ShowHTMLFrame.INSTANCE.toFront();
      ShowHTMLFrame.INSTANCE.requestFocus();
      ShowHTMLFrame.INSTANCE.setAlwaysOnTop(false);
    }
    ShowHTMLFrame.INSTANCE.refresh(request);
  }
}
