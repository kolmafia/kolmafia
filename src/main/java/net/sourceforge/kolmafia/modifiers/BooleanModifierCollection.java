package net.sourceforge.kolmafia.modifiers;

import java.util.EnumSet;
import java.util.function.Consumer;

public class BooleanModifierCollection {
  private final EnumSet<BooleanModifier> booleans = EnumSet.noneOf(BooleanModifier.class);

  public void reset() {
    this.booleans.clear();
  }

  public void set(BooleanModifierCollection source) {
    this.booleans.clear();
    this.booleans.addAll(source.booleans);
  }

  public boolean get(final BooleanModifier mod) {
    return this.booleans.contains(mod);
  }

  public boolean set(BooleanModifier modifier, boolean value) {
    return value ? this.booleans.add(modifier) : this.booleans.remove(modifier);
  }

  public EnumSet<BooleanModifier> raw() {
    return this.booleans.clone();
  }

  public void forEach(Consumer<? super BooleanModifier> action) {
    this.booleans.forEach(action);
  }
}
