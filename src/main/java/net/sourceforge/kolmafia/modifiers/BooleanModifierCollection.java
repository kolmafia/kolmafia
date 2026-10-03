package net.sourceforge.kolmafia.modifiers;

import java.util.EnumSet;
import java.util.function.Consumer;

public class BooleanModifierCollection {
  private final EnumSet<BooleanModifier> booleans = EnumSet.noneOf(BooleanModifier.class);

  public synchronized void reset() {
    this.booleans.clear();
  }

  public void set(BooleanModifierCollection source) {
    // Copy under the source's monitor and assign under ours, never both at once:
    // a.set(b) and b.set(a) on two threads would deadlock.
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

  // Run the action outside the lock, over a copy: it writes to a different collection.
  public void forEach(Consumer<? super BooleanModifier> action) {
    this.raw().forEach(action);
  }
}
