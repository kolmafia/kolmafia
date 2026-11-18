package net.sourceforge.kolmafia.swingui;

import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.UserPrompt;
import net.sourceforge.kolmafia.ui.UserInterface;

public class SwingUserInterface implements UserInterface {
  @Override
  public <R> void showPrompt(final UserPrompt<R> prompt) {
    UserPromptDialog.show(prompt);
  }

  @Override
  public void showBalloon(final String message, final boolean onlyShowWhenHidden) {
    SystemTrayFrame.showBalloon(message, onlyShowWhenHidden);
  }

  @Override
  public boolean isDarkTheme() {
    return KoLmafiaGUI.isDarkTheme();
  }

  @Override
  public boolean isEventDispatchThread() {
    return SwingUtilities.isEventDispatchThread();
  }

  @Override
  public boolean hasOpenWindows() {
    return GenericFrame.instanceExists();
  }

  @Override
  public void showWindow(final String frameName) {
    KoLmafiaGUI.constructFrame(frameName);
  }

  @Override
  public void showMainWindows() {
    KoLmafiaGUI.intializeMainInterfaces();
  }

  @Override
  public void showLoginWindow() {
    KoLmafiaGUI.initializeLoginInterface();
  }

  @Override
  public void showRequest(final GenericRequest request) {
    RequestSynchFrame.showRequest(request);
  }

  @Override
  public void showDescription(final GenericRequest request) {
    DescriptionFrame.showRequest(request);
  }

  @Override
  public void showHTML(final GenericRequest request) {
    ShowHTMLFrame.showRequest(request);
  }

  @Override
  public void closeSessionWindows() {
    // Only Swing's Event Dispatch Thread can change the GUI
    if (SwingUtilities.isEventDispatchThread()) {
      // If we are within that thread, do it
      SwingUserInterface.resetGUI();
    } else {
      // Otherwise, make a Runnable to do it and hand it to Swing
      ResetGUIRunnable resetGUIRunnable = new ResetGUIRunnable();
      try {
        SwingUtilities.invokeAndWait(resetGUIRunnable);
      } catch (Exception e) {
      }
    }
  }

  private static class ResetGUIRunnable implements Runnable {
    @Override
    public void run() {
      SwingUserInterface.resetGUI();
    }
  }

  private static void resetGUI() {
    if (StaticEntity.isGUIRequired()) {
      KoLmafiaGUI.constructFrame(LoginFrame.class);
    }

    // Shut down main frame

    if (KoLDesktop.instanceExists()) {
      KoLDesktop.getInstance().dispose();
    }

    // Close down any other active frames.	Since
    // there is at least one active, logout will
    // not be called again.

    Frame[] frames = Frame.getFrames();

    for (Frame frame : frames) {
      if (frame.getClass() != LoginFrame.class) {
        frame.dispose();
      }
    }
  }

  @Override
  public void debugLogOpened(final File file) {
    if (KoLDesktop.instanceExists()) {
      SwingUtilities.invokeLater(
          () ->
              KoLDesktop.getInstance()
                  .getRootPane()
                  .putClientProperty("Window.documentFile", file));
    }
  }

  @Override
  public void beep() {
    Toolkit.getDefaultToolkit().beep();
  }

  @Override
  public void maximizerExpressionChanged(final String expression) {
    MaximizerFrame.expressionSelect.setSelectedItem(expression);
  }
}
