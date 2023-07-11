package net.sourceforge.kolmafia.modifiers;

public enum ModifierValueType {
  NONE,
  NUMERIC,
  BOOLEAN,
  STRING;

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }
}
