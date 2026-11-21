package net.sourceforge.kolmafia.swingui;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Toolkit;
import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.CreateFrameRunnable;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLDesktop;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.StaticEntity;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.UserPrompt;
import net.sourceforge.kolmafia.swingui.panel.GearChangePanel;
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
  public HexColor getLookAndFeelColor(final String key) {
    Color color = UIManager.getColor(key);

    if (color == null) {
      return null;
    }

    return new HexColor(color.getRed(), color.getGreen(), color.getBlue());
  }
}
