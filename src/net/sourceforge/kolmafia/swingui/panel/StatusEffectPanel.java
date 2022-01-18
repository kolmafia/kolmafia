package net.sourceforge.kolmafia.swingui.panel;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

public class StatusEffectPanel extends ScrollablePanel<ShowDescriptionList<AdventureResult>> {
  private final ShowDescriptionList<AdventureResult> elementList;

  public StatusEffectPanel() {
    this(
        new ShowDescriptionList<>((LockableListModel<AdventureResult>) KoLConstants.activeEffects));
  }

  public StatusEffectPanel(final ShowDescriptionList<AdventureResult> list) {
    super("Active Effects", "uneffect", "add to mood", list);
    this.elementList = this.scrollComponent;
  }

  @Override
  public void actionConfirmed() {
    AdventureResult[] effects =
        this.elementList.getSelectedValuesList().toArray(new AdventureResult[0]);
    for (int i = 0; i < effects.length; ++i) {
      RequestThread.postRequest(new UneffectRequest(effects[i]));
    }
  }

  @Override
  public void actionCancelled() {
    AdventureResult[] effects =
        this.elementList.getSelectedValuesList().toArray(new AdventureResult[0]);
    this.elementList.clearSelection();

    if (Preferences.getString("currentMood").equals("apathetic")) {
      MoodManager.setMood("default");
    }

    int count = 0;

    for (int i = 0; i < effects.length; ++i) {
      String name = effects[i].getName();
      String action = MoodManager.getDefaultAction("lose_effect", name);
      if (!action.equals("")) {
        MoodManager.addTrigger("lose_effect", name, action);
        count++;
        continue;
      }

      action = MoodManager.getDefaultAction("gain_effect", name);
      if (!action.equals("")) {
        MoodManager.addTrigger("gain_effect", name, action);
        count++;
      }
    }

    if (count > 0) {
      MoodManager.saveSettings();
    }
  }
}
