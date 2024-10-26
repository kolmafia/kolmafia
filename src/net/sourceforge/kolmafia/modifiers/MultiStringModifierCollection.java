package net.sourceforge.kolmafia.modifiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MultiStringModifierCollection {
  private final Map<MultiStringModifier, List<String>> multiStrings =
      new EnumMap<>(MultiStringModifier.class);

  public void reset() {
    this.multiStrings.clear();
  }

  public List<String> get(final MultiStringModifier mod) {
    return this.multiStrings.getOrDefault(mod, List.of());
  }

  /**
   * Returns last modifier in the list, to reproduce the state before MultiStringModifier that
   * prgressive strings would overwrite the last
   *
   * @param mod Modifier
   * @return Value
   */
  public String getOne(final MultiStringModifier mod) {
    var list = get(mod);
    return list.isEmpty() ? "" : list.get(list.size() - 1);
  }

  public boolean add(MultiStringModifier modifier, String value) {
    var list = new ArrayList<>(get(modifier));
    var success = list.add(value);
    this.set(modifier, Collections.unmodifiableList(list));
    return success;
  }

  public boolean set(MultiStringModifier modifier, List<String> value) {
    var oldValue =
        value.isEmpty()
            ? this.multiStrings.remove(modifier)
            : this.multiStrings.put(modifier, value);

    // TODO: does anything use this return value, or can we save ourselves a check?
    return oldValue == null || !oldValue.equals(value);
  }
}
