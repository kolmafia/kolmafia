package net.sourceforge.kolmafia.preferences;

import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.listener.Listener;

public class PreferenceModifiers implements Listener {
  private String property;
  private String modifierType;
  private String value;
  private Modifiers modifiers;

  public PreferenceModifiers(String property, String modifierType) {
    this.property = property;
    this.modifierType = modifierType;
    this.update();
  }

  @Override
  public void update() {
    this.value = Preferences.getString(this.property);
    this.modifiers =
        Modifiers.evaluatedModifiers(
            Modifiers.getLookupName(this.modifierType, this.property), this.value);
  }

  public Modifiers get() {
    return this.modifiers;
  }
}
