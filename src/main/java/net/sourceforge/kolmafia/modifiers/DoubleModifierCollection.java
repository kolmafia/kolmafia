package net.sourceforge.kolmafia.modifiers;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * The doubles of one Modifiers, and the map they live in.
 *
 * <p>Access to that map is synchronized on the collection that owns it. Modifiers held in
 * ModifierDatabase's cache are shared, and are read by whichever thread asks for them while another
 * may still be filling one in: the relay browser rendering a page, a script running in the CLI, the
 * main thread after a request. An unsynchronized TreeMap read during a write to it does not fail
 * cleanly - putAll() builds from a sorted iterator and throws NoSuchElementException when the size
 * it was promised and the entries it finds disagree, which reaches the user as a script aborting
 * somewhere unrelated, and leaves a corrupted map in the cache for every later read to trip over.
 */
public class DoubleModifierCollection {
  public static final int SPARSE_DOUBLES_MAX_SIZE = 32;
  private static final DoubleOrList DEFAULT = new DoubleOrList(0.0);

  // If only a few values are set in doubles, we instead store all modifiers in a sparse TreeMap.
  // When that map gets bigger than SPARSE_DOUBLES_MAX_SIZE, we copy it over to the dense EnumMap.
  private Map<DoubleModifier, DoubleOrList> doubles = new TreeMap<>();

  public synchronized void reset() {
    this.doubles.clear();
  }

  public void set(DoubleModifierCollection source) {
    // Two separate locks, never held together. Holding this collection's monitor while taking the
    // source's would let a.set(b) and b.set(a) on two threads wait on each other for ever.
    Map<DoubleModifier, DoubleOrList> copy = source.copyOfDoubles();
    synchronized (this) {
      this.doubles = copy;
    }
  }

  private synchronized Map<DoubleModifier, DoubleOrList> copyOfDoubles() {
    Map<DoubleModifier, DoubleOrList> copy =
        this.doubles instanceof EnumMap ? new EnumMap<>(DoubleModifier.class) : new TreeMap<>();
    copy.putAll(this.doubles);
    return copy;
  }

  public synchronized void densify() {
    if (this.doubles instanceof EnumMap) return;
    Map<DoubleModifier, DoubleOrList> newDoubles = new EnumMap<>(DoubleModifier.class);
    newDoubles.putAll(this.doubles);
    this.doubles = newDoubles;
  }

  private DoubleOrList get(final DoubleModifier mod) {
    return this.doubles.getOrDefault(mod, DEFAULT);
  }

  public synchronized double getDouble(final DoubleModifier mod) {
    var entry = this.doubles.get(mod);
    if (entry == null) return 0.0;
    return entry.getDoubleValue();
  }

  public synchronized List<Double> getList(final DoubleModifier mod) {
    var entry = this.doubles.get(mod);
    if (entry == null) return new ArrayList<>(List.of());
    return entry.getListValue();
  }

  public synchronized boolean set(final DoubleModifier mod, final double value) {
    return set(mod, new DoubleOrList(value));
  }

  public synchronized boolean set(final DoubleModifier mod, final List<Double> value) {
    return set(mod, new DoubleOrList(value));
  }

  private synchronized boolean set(final DoubleModifier mod, final DoubleOrList value) {
    var isMultiple = mod.isMultiple();
    var oldValue = get(mod);
    if (isMultiple) {
      // if multi-modifier, then:
      // * if new element is list, replace or remove if default
      // * if new element is double, append to existing list (creating if absent)
      if (value.isList()) {
        if (value.isDefault()) {
          this.doubles.remove(mod);
        } else {
          this.doubles.put(mod, value);
        }
      } else {
        var lst = oldValue.append(value);
        this.doubles.put(mod, lst);
      }
    } else {
      // if not multi-modifier, new value should be a double, so just replace or remove
      if (value.isDefault()) {
        this.doubles.remove(mod);
      } else {
        this.doubles.put(mod, value);
      }
    }

    if (this.doubles.size() >= DoubleModifierCollection.SPARSE_DOUBLES_MAX_SIZE) {
      this.densify();
    }

    // TODO: does anything use this return value, or can we save ourselves a check?
    return !oldValue.equals(value);
  }

  public synchronized double increment(final DoubleModifier mod, final double value) {
    // Anything being accumulated onto should be dense.
    this.densify();
    var asDouble = new DoubleOrList(value);
    return this.doubles.merge(mod, asDouble, DoubleOrList::sum).getDoubleValue();
  }

  /**
   * The action is run outside the lock, over a copy. Callers pass actions that write to a
   * <em>different</em> collection - Modifiers.add() reads one and sets another - so running them
   * while holding this one's monitor would take two locks in an order another thread may take the
   * other way round.
   */
  public void forEach(BiConsumer<? super DoubleModifier, ? super DoubleOrList> action) {
    this.copyOfDoubles().forEach(action);
  }
}
