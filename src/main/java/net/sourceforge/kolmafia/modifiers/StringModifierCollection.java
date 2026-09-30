package net.sourceforge.kolmafia.modifiers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The strings of one Modifiers, and the map they live in.
 *
 * <p>Access to that map is synchronized on the collection that owns it, for the reasons set out on
 * {@link DoubleModifierCollection}. This one cannot fail as loudly - an EnumMap is not fail-fast,
 * so a read during a write returns an answer rather than throwing - which makes it the more
 * dangerous of the two: set() empties the map before refilling it, and a reader arriving in that
 * window is told, with no complaint at all, that the modifier it asked about is not set.
 */
public class StringModifierCollection {
  private static final StringOrList DEFAULT = new StringOrList("");
  private final Map<StringModifier, StringOrList> strings = new EnumMap<>(StringModifier.class);

  public synchronized void reset() {
    this.strings.clear();
  }

  public void set(StringModifierCollection source) {
    // The source is copied under its own monitor and this map refilled under ours, never both at
    // once: a.set(b) and b.set(a) on two threads would otherwise wait on each other for ever.
    Map<StringModifier, StringOrList> copy = source.copyOfStrings();
    synchronized (this) {
      this.strings.clear();
      this.strings.putAll(copy);
    }
  }

  private synchronized Map<StringModifier, StringOrList> copyOfStrings() {
    return new EnumMap<>(this.strings);
  }

  private StringOrList get(final StringModifier mod) {
    return this.strings.getOrDefault(mod, DEFAULT);
  }

  public synchronized String getString(final StringModifier mod) {
    var entry = this.strings.get(mod);
    if (entry == null) return "";
    return entry.getStringValue();
  }

  public synchronized List<String> getList(final StringModifier mod) {
    var entry = this.strings.get(mod);
    if (entry == null) return new ArrayList<>(List.of());
    return entry.getListValue();
  }

  public synchronized boolean contains(final StringModifier mod) {
    return this.strings.containsKey(mod);
  }

  public synchronized boolean set(final StringModifier mod, final String value) {
    return set(mod, new StringOrList(value));
  }

  public synchronized boolean set(final StringModifier mod, final List<String> value) {
    return set(mod, new StringOrList(value));
  }

  public synchronized boolean set(StringModifier mod, StringOrList value) {
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
