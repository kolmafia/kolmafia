package net.sourceforge.kolmafia.modifiers;

public enum ModifierValueType {
  NUMERIC,
  BOOLEAN,
  STRING;

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }
}
