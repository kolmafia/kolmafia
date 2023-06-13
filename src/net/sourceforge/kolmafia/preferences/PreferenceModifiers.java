package net.sourceforge.kolmafia.preferences;

import net.sourceforge.kolmafia.ModifierType;
import net.sourceforge.kolmafia.Modifiers;
import net.sourceforge.kolmafia.listener.Listener;
import net.sourceforge.kolmafia.listener.PreferenceListenerRegistry;
import net.sourceforge.kolmafia.modifiers.Lookup;
import net.sourceforge.kolmafia.persistence.ModifierDatabase;

public class PreferenceModifiers implements Listener {
  private final String property;
  private final ModifierType modifierType;
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
        ModifierDatabase.evaluatedModifiers(
            new Lookup(this.modifierType, this.property), this.value);
  }

  public Modifiers get() {
    return this.modifiers;
  }
}
