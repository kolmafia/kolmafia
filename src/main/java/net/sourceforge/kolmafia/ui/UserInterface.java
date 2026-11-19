package net.sourceforge.kolmafia.ui;

import java.io.File;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.UserPrompt;
import net.sourceforge.kolmafia.utilities.HTMLListEntry;

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
}
