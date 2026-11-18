package net.sourceforge.kolmafia.ui;

import java.io.File;
import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.request.GenericRequest;
import net.sourceforge.kolmafia.session.UserPrompt;

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

  default void openChatWindow(final String bufferKey, final boolean highlightOnOpen) {}

  default void highlightChatTab(final String bufferKey) {}

  default void chatClosed() {}

  default void showContacts(final String title, final LockableListModel<?> contacts) {}

  default void maximizerExpressionChanged(final String expression) {}
}
