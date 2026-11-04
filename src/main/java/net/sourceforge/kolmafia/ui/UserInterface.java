package net.sourceforge.kolmafia.ui;

import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.equipment.Slot;
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

  // The engine's state changed, and anything showing it should refresh.

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

  default void storeEditingCancelled() {}

  default void storeEarningsChanged(final long potentialEarnings) {}

  default void equipmentListsCleared() {}

  default void equipmentSlotChanged(final Slot slot) {}

  default void stickersChanged(final int st1, final int st2, final int st3) {}

  default void deferEquipmentUpdates() {}

  default void resolveDeferredEquipmentUpdates() {}
}
