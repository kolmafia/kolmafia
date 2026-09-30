package net.sourceforge.kolmafia.modifiers;

import java.util.EnumSet;
import java.util.function.Consumer;

/**
 * The booleans of one Modifiers, and the set they live in. Synchronized for the reasons set out on
 * {@link DoubleModifierCollection}, and silent when it goes wrong for the reasons set out on {@link
 * StringModifierCollection}.
 */
public class BooleanModifierCollection {
  private final EnumSet<BooleanModifier> booleans = EnumSet.noneOf(BooleanModifier.class);

  public synchronized void reset() {
    this.booleans.clear();
  }

  public void set(BooleanModifierCollection source) {
    // Two monitors, never held together; see StringModifierCollection.set.
    EnumSet<BooleanModifier> copy = source.raw();
    synchronized (this) {
      this.booleans.clear();
      this.booleans.addAll(copy);
    }
  }

  public synchronized boolean get(final BooleanModifier mod) {
    return this.booleans.contains(mod);
  }

  public synchronized boolean set(BooleanModifier modifier, boolean value) {
    return value ? this.booleans.add(modifier) : this.booleans.remove(modifier);
  }

  public synchronized EnumSet<BooleanModifier> raw() {
    return this.booleans.clone();
  }

  /** Outside the lock, over a copy: the action writes to another collection. */
  public void forEach(Consumer<? super BooleanModifier> action) {
    this.raw().forEach(action);
  }
}
