package net.sourceforge.kolmafia.swingui;

import javax.swing.SwingUtilities;
import net.sourceforge.kolmafia.KoLAdventure;
import net.sourceforge.kolmafia.KoLmafiaGUI;
import net.sourceforge.kolmafia.equipment.Slot;
import net.sourceforge.kolmafia.session.UserPrompt;
import net.sourceforge.kolmafia.swingui.panel.GearChangePanel;
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
  public void storeEditingCancelled() {
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
}
