package net.sourceforge.kolmafia.ui;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLConstants.MafiaState;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.UserPrompt;
import net.sourceforge.kolmafia.utilities.HTMLListEntry;
import net.sourceforge.kolmafia.utilities.HexColor;
import net.sourceforge.kolmafia.utilities.InputFieldUtilities;

public interface UserInterface {
  default <R> void showPrompt(final UserPrompt<R> prompt) {
    prompt.askOnConsole();
  }

  default void showBalloon(final String message, final boolean onlyShowWhenHidden) {}

  default boolean isDarkTheme() {
    return false;
  }

  default boolean isEventDispatchThread() {
    return false;
  }

  default boolean hasOpenWindows() {
    return false;
  }

  default void showWindow(final String frameName) {}

  default void showMainWindows() {}

  default void showLoginWindow() {}

  default void showRequest(final GenericRequest request) {}

  default void showDescription(final GenericRequest request) {}

  default void showHTML(final GenericRequest request) {}

  default void closeSessionWindows() {}

  default void debugLogOpened(final File file) {}

  default void beep() {
    System.out.write(0x07);
  }

  default void meatChanged() {}

  default void familiarsChanged() {}

  default void familiarListCleared() {}

  default void skillsChanged() {}

  default void safetyDetailsChanged() {}

  default void selectedAdventureChanged(final KoLAdventure location) {}

  default void preferencesChanged() {}

  default void charPaneRefreshed() {}

  default void pullsRemainingChanged(final int pullsRemaining) {}

  default void pullsBudgetedChanged(final int pullsBudgeted) {}

  default void cancelStoreEditing() {}

  default void storeEarningsChanged(final long potentialEarnings) {}

  default void equipmentListsCleared() {}

  default void equipmentSlotChanged(final Slot slot) {}

  default void stickersChanged(final int st1, final int st2, final int st3) {}

  default void deferEquipmentUpdates() {}

  default void resolveDeferredEquipmentUpdates() {}

  default void openChatWindow(final String bufferKey, final boolean highlightOnOpen) {}

  default void highlightChatTab(final String bufferKey) {}

  default void closeChat() {}

  default void showChatContacts(
      final String title, final LockableListModel<HTMLListEntry> contacts) {}

  default void showTriviaContestants(
      final String title, final LockableListModel<String> contestants) {}

  default void maximizerExpressionChanged(final String expression) {}

  default void showPlayerProfile(final String player) {}

  /** Shows text on the application's icon, such as the dock icon, where the system has one. */
  default void setIconBadge(final String text) {}

  /** Prepares the user interface's HTML views, before any are created. */
  default void installEditorKits() {}

  /** Applies the user's look and feel, tray icon and application icon. */
  default void installLookAndFeel() {}

  /** Starts the graphical interface, when KoLmafia was started with one. */
  default void initializeGUI() {}

  default void updateToolTip(final String message) {}

  /** KoLmafia's state changed: show the message and enable or disable controls to match. */
  default void updateDisplayState(final MafiaState state, final String message) {}

  /** Progress through a series of requests, such as adventures. */
  default void requestProgress(final int value, final int maximum) {}

  default void validateEquipmentSelections() {}

  default void showLicenses() {}

  default void removeTrayIcon() {}

  default boolean isSystemTraySupported() {
    return false;
  }

  /** A color of the current look and feel, or null if it has none. */
  default HexColor getLookAndFeelColor(final String key) {
    return null;
  }

  /** Runs work in order on the user interface's thread, or on a thread of its own without one. */
  default void invokeLater(final Runnable runnable) {
    UserInterfaceThread.execute(runnable);
  }

  default boolean confirmWarning(final String title, final String message) {
    return InputFieldUtilities.confirm(message);
  }

  default void dumpDisabledSkills() {}

  default boolean canBrowse() {
    return false;
  }

  default void browse(final URI uri) throws IOException {}
}
