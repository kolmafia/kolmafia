package net.sourceforge.kolmafia.swingui;

import static net.sourceforge.kolmafia.swingui.KoLGUIConstants.FLATMAP_DARK_LOOKS;
import static net.sourceforge.kolmafia.swingui.KoLGUIConstants.FLATMAP_LIGHT_LOOKS;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.awt.Frame;
import java.awt.SystemTray;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.java.dev.spellcast.utilities.ActionPanel;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.RequestLogger;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.UserPrompt;
import net.sourceforge.kolmafia.swingui.listener.LicenseDisplayListener;
import net.sourceforge.kolmafia.swingui.panel.ActionPanelRegistry;
import net.sourceforge.kolmafia.swingui.panel.GearChangePanel;
import net.sourceforge.kolmafia.swingui.panel.GenericPanel;
import net.sourceforge.kolmafia.swingui.widget.RequestHTMLEditorKit;
import net.sourceforge.kolmafia.ui.UserInterface;
import net.sourceforge.kolmafia.utilities.HTMLListEntry;
import net.sourceforge.kolmafia.utilities.HexColor;

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
  public void meatChanged() {
    MallSearchFrame.updateMeat();
  }

  @Override
  public void familiarsChanged() {
    GearChangePanel.updateFamiliars();
  }

  @Override
  public void familiarListCleared() {
    GearChangePanel.clearFamiliarList();
  }

  @Override
  public void skillsChanged() {
    SkillBuffFrame.update();
  }

  @Override
  public void safetyDetailsChanged() {
    AdventureFrame.updateSafetyDetails();
  }

  @Override
  public void selectedAdventureChanged(final KoLAdventure location) {
    AdventureFrame.updateSelectedAdventure(location);
  }

  @Override
  public void preferencesChanged() {
    AdventureFrame.updateFromPreferences();
  }

  @Override
  public void charPaneRefreshed() {
    RequestFrame.refreshStatus();
  }

  @Override
  public void pullsRemainingChanged(final int pullsRemaining) {
    ItemManageFrame.updatePullsRemaining(pullsRemaining);
  }

  @Override
  public void pullsBudgetedChanged(final int pullsBudgeted) {
    ItemManageFrame.updatePullsBudgeted(pullsBudgeted);
  }

  @Override
  public void cancelStoreEditing() {
    StoreManageFrame.cancelTableEditing();
  }

  @Override
  public void storeEarningsChanged(final long potentialEarnings) {
    StoreManageFrame.updateEarnings(potentialEarnings);
  }

  @Override
  public void equipmentListsCleared() {
    GearChangePanel.clearEquipmentModels();
  }

  @Override
  public void equipmentSlotChanged(final Slot slot) {
    GearChangePanel.updateSlot(slot);
  }

  @Override
  public void stickersChanged(final int st1, final int st2, final int st3) {
    GearChangePanel.updateStickers(st1, st2, st3);
  }

  @Override
  public void deferEquipmentUpdates() {
    GearChangePanel.deferUpdate();
  }

  @Override
  public void resolveDeferredEquipmentUpdates() {
    GearChangePanel.resolveDeferredUpdate();
  }

  @Override
  public void openChatWindow(final String bufferKey, final boolean highlightOnOpen) {
    ChatFrame.openWindow(bufferKey, highlightOnOpen);
  }

  @Override
  public void highlightChatTab(final String bufferKey) {
    ChatFrame.highlightChatTab(bufferKey);
  }

  @Override
  public void closeChat() {
    ChatFrame.closeChat();
  }

  @Override
  public void showChatContacts(
      final String title, final LockableListModel<HTMLListEntry> contacts) {
    ContactListFrame.showChatContacts(title, contacts);
  }

  @Override
  public void showTriviaContestants(
      final String title, final LockableListModel<String> contestants) {
    ContactListFrame.showTriviaContestants(title, contestants);
  }

  @Override
  public void maximizerExpressionChanged(final String expression) {
    MaximizerFrame.expressionSelect.setSelectedItem(expression);
  }

  @Override
  public void showPlayerProfile(final String player) {
    Object[] parameters = new Object[] {player};
    SwingUtilities.invokeLater(new CreateFrameRunnable(ProfileFrame.class, parameters));
  }

  @Override
  public void setIconBadge(final String text) {
    if (Taskbar.isTaskbarSupported()) {
      Taskbar taskbar = Taskbar.getTaskbar();
      if (taskbar.isSupported(Taskbar.Feature.ICON_BADGE_TEXT)
          || taskbar.isSupported(Taskbar.Feature.ICON_BADGE_NUMBER)) {
        taskbar.setIconBadge(text);
      }
    }
  }

  @Override
  public void installEditorKits() {
    JEditorPane.registerEditorKitForContentType("text/html", RequestHTMLEditorKit.class.getName());
  }

  @Override
  public void installLookAndFeel() {
    // Change the default look and feel to match the player's
    // preferences. Always do this.

    String defaultLookAndFeel;

    // Tell UIManager about Look and Feel files in external jars ( defined in KoLGUIConstants)
    installLookAndFeel(FLATMAP_LIGHT_LOOKS);
    installLookAndFeel(FLATMAP_DARK_LOOKS);

    if (System.getProperty("os.name").startsWith("Mac")
        || System.getProperty("os.name").startsWith("Win")) {
      defaultLookAndFeel = UIManager.getSystemLookAndFeelClassName();
    } else {
      defaultLookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
    }

    String lookAndFeel = Preferences.getString("swingLookAndFeel");

    if (lookAndFeel.isEmpty()) {
      lookAndFeel = defaultLookAndFeel;
    }

    UIManager.LookAndFeelInfo[] installed = UIManager.getInstalledLookAndFeels();
    String[] installedLooks = new String[installed.length];

    for (int i = 0; i < installedLooks.length; ++i) {
      installedLooks[i] = installed[i].getClassName();
    }

    boolean foundLookAndFeel = false;
    for (int i = 0; i < installedLooks.length && !foundLookAndFeel; ++i) {
      foundLookAndFeel = installedLooks[i].equals(lookAndFeel);
    }

    if (!foundLookAndFeel) {
      lookAndFeel = defaultLookAndFeel;
    }

    try {
      UIManager.setLookAndFeel(lookAndFeel);
      KoLmafiaGUI.applyFlatLafMenuBarSettings();
      JFrame.setDefaultLookAndFeelDecorated(System.getProperty("os.name").startsWith("Mac"));
    } catch (Exception e) {
      // Should not happen, as we checked to see if
      // the look and feel was installed first.

      JFrame.setDefaultLookAndFeelDecorated(true);
    }

    if (StaticEntity.usesSystemTray()) {
      SystemTrayFrame.addTrayIcon();
    }

    boolean isWindows = System.getProperty("os.name").startsWith("Win");

    if (Taskbar.isTaskbarSupported()) {
      Taskbar taskbar = Taskbar.getTaskbar();
      if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
        String image = isWindows ? "app_icon256.png" : "limeglass.gif";
        taskbar.setIconImage(JComponentUtilities.getImage(image).getImage());
      }
    }

    if (isWindows || lookAndFeel.equals(UIManager.getCrossPlatformLookAndFeelClassName())) {
      UIManager.put("ProgressBar.foreground", Color.black);
      UIManager.put("ProgressBar.selectionForeground", Color.lightGray);

      UIManager.put("ProgressBar.background", Color.lightGray);
      UIManager.put("ProgressBar.selectionBackground", Color.black);
    }
  }

  private static void installLookAndFeel(Map<String, String> looks) {
    for (Entry<String, String> entry : looks.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      try {
        // try to load the class to confirm it exists
        Class.forName(value);
      } catch (ClassNotFoundException e) {
        RequestLogger.printLine("Failed to load class " + value + " for Theme " + key);
        continue;
      }
      UIManager.installLookAndFeel(key, value);
    }
  }

  @Override
  public void initializeGUI() {
    KoLmafiaGUI.initialize();
  }

  @Override
  public void updateToolTip(final String message) {
    SystemTrayFrame.updateToolTip(message);
  }

  @Override
  public void updateDisplayState(final MafiaState state, final String unicodeMessage) {
    ActionPanel[] panels = ActionPanelRegistry.getExistingPanels();

    for (ActionPanel panel : panels) {
      if (panel instanceof GenericPanel) {
        ((GenericPanel) panel).setStatusMessage(unicodeMessage);
      }

      panel.setEnabled(state != MafiaState.CONTINUE);
    }

    Arrays.stream(Frame.getFrames())
        .filter(GenericFrame.class::isInstance)
        .map(GenericFrame.class::cast)
        .forEach(
            f -> {
              f.setStatusMessage(unicodeMessage);
              f.updateDisplayState(state);
            });

    if (KoLDesktop.instanceExists()) {
      KoLDesktop.getInstance().updateDisplayState(state);
    }
  }

  @Override
  public void progressAdventureMeter(final int value, final int maximum) {
    AdventureFrame.updateRequestMeter(value, maximum);
  }

  @Override
  public void validateEquipmentSelections() {
    GearChangePanel.validateSelections();
  }

  @Override
  public void showLicenses() {
    new LicenseDisplayListener().run();
  }

  @Override
  public void removeTrayIcon() {
    SystemTrayFrame.removeTrayIcon();
  }

  @Override
  public boolean isSystemTraySupported() {
    return SystemTray.isSupported();
  }

  @Override
  public HexColor getLookAndFeelColor(final String key) {
    Color color = UIManager.getColor(key);

    if (color == null) {
      return null;
    }

    return new HexColor(color.getRed(), color.getGreen(), color.getBlue());
  }

  @Override
  public void invokeLater(final Runnable runnable) {
    SwingUtilities.invokeLater(runnable);
  }

  @Override
  public boolean confirmWarning(final String title, final String message) {
    return JOptionPane.showConfirmDialog(
            null, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE)
        == JOptionPane.YES_OPTION;
  }

  @Override
  public void dumpDisabledSkills() {
    SkillBuffFrame.dumpDisabledSkills();
  }

  @Override
  public boolean canBrowse() {
    return Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.BROWSE);
  }

  @Override
  public void browse(final URI uri) throws IOException {
    Desktop.getDesktop().browse(uri);
  }
}
