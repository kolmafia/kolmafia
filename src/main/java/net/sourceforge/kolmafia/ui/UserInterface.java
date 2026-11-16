package net.sourceforge.kolmafia.ui;

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
}
