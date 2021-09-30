package net.sourceforge.kolmafia.swingui.widget;

import net.sourceforge.kolmafia.persistence.ConcoctionDatabase;
import net.sourceforge.kolmafia.preferences.PreferenceListenerCheckBox;

public class CreationSettingCheckBox extends PreferenceListenerCheckBox {
  public CreationSettingCheckBox(final String property) {
    super(property);
  }

  public CreationSettingCheckBox(final String label, final String property, final String tooltip) {
    super(label, property);

    this.setToolTipText(tooltip);
  }

  @Override
  protected void handleClick() {
    ConcoctionDatabase.refreshConcoctions();
  }
}
