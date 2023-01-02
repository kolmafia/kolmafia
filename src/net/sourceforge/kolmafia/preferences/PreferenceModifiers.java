package net.sourceforge.kolmafia.preferences;

import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;

public class PreferenceModifiers implements Listener {
  private String property;
  private ModifierType modifierType;
  private String value;
  private Modifiers modifiers;

  public PreferenceModifiers(String property, ModifierType modifierType) {
    this.property = property;
    this.modifierType = modifierType;
    PreferenceListenerRegistry.registerPreferenceListener(property, this);
    this.update();
  }

  @Override
  public void update() {
    this.value = Preferences.getString(this.property);
    this.modifiers =
        Modifiers.evaluatedModifiers(
            new Modifiers.Lookup(this.modifierType, this.property), this.value);
  }

  public Modifiers get() {
    return this.modifiers;
  }
}
