package net.sourceforge.kolmafia.session;

import java.awt.Frame;
import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.BuffBotHome;
import net.sourceforge.kolmafia.KoLCharacter;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLmafia;
import net.sourceforge.kolmafia.KoLmafiaCLI;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.chat.ChatManager;
import net.sourceforge.kolmafia.persistence.AdventureQueueDatabase;
import net.sourceforge.kolmafia.persistence.AdventureSpentDatabase;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.request.LogoutRequest;
import net.sourceforge.kolmafia.swingui.LoginFrame;

public class LogoutManager {
  private static boolean isRunning = false;

  public static void prepare() {
    // If there's no user to worry about, we're done now.

    String userName = KoLCharacter.getUserName();

    if (userName == null || userName.equals("")) {
      return;
    }

    // Only Swing's Event Dispatch Thread can change the GUI
    if (SwingUtilities.isEventDispatchThread()) {
      // If we are within that thread, do it
      LogoutManager.resetGUI();
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
      LogoutManager.resetGUI();
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

    for (int i = 0; i < frames.length; ++i) {
      if (frames[i].getClass() != LoginFrame.class) {
        frames[i].dispose();
      }
    }
  }

  public static void logout() {
    if (LogoutManager.isRunning) {
      return;
    }

    try {
      LogoutManager.isRunning = true;
      doLogout();
    } finally {
      LogoutManager.isRunning = false;
    }
  }

  private static void doLogout() {
    // If there's no user to worry about, we're done now.

    String userName = KoLCharacter.getUserName();

    if (userName == null || userName.equals("")) {
      return;
    }

    // Shut down the GUI first, after all.
    if (!KoLmafia.isSessionEnding()) {
      LogoutManager.prepare();
    }

    KoLmafia.updateDisplay("Preparing for logout...");

    // Shut down chat-related activity

    BuffBotHome.setBuffBotActive(false);
    ChatManager.dispose();

    // It we submitted a request during rollover, we are already
    // logged out and cannot do game actions
    if (GenericRequest.sessionId != null) {
      // Run on-logout scripts
      String scriptSetting = Preferences.getString("logoutScript");
      if (!scriptSetting.equals("")) {
        KoLmafia.updateDisplay("Executing logout script...");
        KoLmafiaCLI.DEFAULT_SHELL.executeLine(scriptSetting);
      }
    }

    if (Preferences.getBoolean("sharePriceData")) {
      KoLmafia.updateDisplay("Sharing mall price data with other users...");
      KoLmafiaCLI.DEFAULT_SHELL.executeLine(
          "spade prices https://kolmafia.us/scripts/updateprices.php");
    }

    // Serialize adventure queue data
    AdventureQueueDatabase.serialize();
    AdventureSpentDatabase.serialize();

    // Clear out user data

    RequestLogger.closeSessionLog();
    RequestLogger.closeMirror();

    GenericRequest.reset();
    KoLCharacter.reset("");

    // Execute the logout request

    RequestThread.postRequest(new LogoutRequest());
    KoLmafia.updateDisplay("Logout completed.");

    // For some reason KoL gives you new cookies after you are logged out.
    // Forget them.
    GenericRequest.reset();

    RequestLogger.closeDebugLog();
  }
}
