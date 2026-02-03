package net.sourceforge.kolmafia.modifiers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class StringModifierCollection {
  private final Map<StringModifier, StringOrList> strings = new EnumMap<>(StringModifier.class);

  public void reset() {
    this.strings.clear();
  }

  private StringOrList get(final StringModifier mod) {
    return this.strings.getOrDefault(mod, new StringOrList(""));
  }

  public String getString(final StringModifier mod) {
    var entry = this.strings.get(mod);
    if (entry == null) return "";
    return entry.getStringValue();
  }

  public List<String> getList(final StringModifier mod) {
    var entry = this.strings.get(mod);
    if (entry == null) return new ArrayList<>(List.of());
    return entry.getListValue();
  }

  public boolean set(final StringModifier mod, final String value) {
    return set(mod, new StringOrList(value));
  }

  public boolean set(final StringModifier mod, final List<String> value) {
    return set(mod, new StringOrList(value));
  }

  public boolean set(StringModifier mod, StringOrList value) {
    var isMultiple = mod.isMultiple();
    var oldValue = get(mod);
    if (isMultiple) {
      // if multi-modifier, then:
      // * if new element is list, replace or remove if default
      // * if new element is string, append to existing list (creating if absent)
      if (value.isList()) {
        if (value.isDefault()) {
          this.strings.remove(mod);
        } else {
          this.strings.put(mod, value);
        }
      } else {
        var lst = oldValue.append(value);
        this.strings.put(mod, lst);
      }
    } else {
      // if not multi-modifier, new value should be a string, so just replace or remove
      if (value.isDefault()) {
        this.strings.remove(mod);
      } else {
        this.strings.put(mod, value);
      }
    }

    // TODO: does anything use this return value, or can we save ourselves a check?
    return !oldValue.equals(value);
  }
}
