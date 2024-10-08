package net.sourceforge.kolmafia.modifiers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class StringModifierCollection {
  private final Map<StringModifier, List<String>> strings = new EnumMap<>(StringModifier.class);

  public void reset() {
    this.strings.clear();
  }

  public List<String> get(final StringModifier mod) {
    return this.strings.getOrDefault(mod, List.of());
  }

  public boolean set(StringModifier modifier, String value) {
    if (value.isEmpty()) {
      this.strings.remove(modifier);
      return true;
    }

    var oldSet = this.strings.getOrDefault(modifier, new ArrayList<>());

    if (oldSet.contains(value)) return false;
    return oldSet.add(value);
  }

  public boolean set(StringModifier modifier, List<String> values) {
    var set = this.strings.getOrDefault(modifier, new ArrayList<>());
    return set.addAll(values);
  }
}
