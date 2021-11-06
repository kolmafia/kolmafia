package net.sourceforge.kolmafia.swingui.panel;

import net.java.dev.spellcast.utilities.LockableListModel;
import net.sourceforge.kolmafia.AdventureResult;
import net.sourceforge.kolmafia.KoLConstants;
import net.sourceforge.kolmafia.RequestThread;
import net.sourceforge.kolmafia.moods.MoodManager;
import net.sourceforge.kolmafia.preferences.Preferences;
import net.sourceforge.kolmafia.request.UneffectRequest;
import net.sourceforge.kolmafia.swingui.widget.ShowDescriptionList;

public class StatusEffectPanel extends ScrollablePanel {
  private final ShowDescriptionList elementList;

  public StatusEffectPanel() {
    this(new ShowDescriptionList((LockableListModel<AdventureResult>) KoLConstants.activeEffects));
  }

  public StatusEffectPanel(final ShowDescriptionList list) {
    super("Active Effects", "uneffect", "add to mood", list);
    this.elementList = (ShowDescriptionList) this.scrollComponent;
  }

  @Override
  public void actionConfirmed() {
    Object[] effects = this.elementList.getSelectedValuesList().toArray();
    for (int i = 0; i < effects.length; ++i) {
      RequestThread.postRequest(new UneffectRequest((AdventureResult) effects[i]));
    }
  }

  @Override
  public void actionCancelled() {
    Object[] effects = this.elementList.getSelectedValuesList().toArray();
    this.elementList.clearSelection();

    if (Preferences.getString("currentMood").equals("apathetic")) {
      MoodManager.setMood("default");
    }

    int count = 0;

    for (int i = 0; i < effects.length; ++i) {
      String name = ((AdventureResult) effects[i]).getName();
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
