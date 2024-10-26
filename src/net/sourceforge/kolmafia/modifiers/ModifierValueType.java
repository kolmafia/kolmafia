package net.sourceforge.kolmafia.modifiers;

public enum ModifierValueType {
  NONE,
  NUMERIC,
  BOOLEAN,
  STRING,
  MULTISTRING;

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }
}
