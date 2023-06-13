package net.sourceforge.kolmafia.modifiers;

import java.util.EnumMap;
import java.util.Map;

public class StringModifierCollection {
  private final Map<StringModifier, String> strings = new EnumMap<>(StringModifier.class);

  public void reset() {
    this.strings.clear();
  }

  public String get(final StringModifier mod) {
    return this.strings.getOrDefault(mod, "");
  }

  public boolean set(StringModifier modifier, String value) {
    String oldValue =
        value.isEmpty() ? this.strings.remove(modifier) : this.strings.put(modifier, value);

    // TODO: does anything use this return value, or can we save ourselves a check?
    return oldValue == null || !oldValue.equals(value);
  }
}
