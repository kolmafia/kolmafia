package net.sourceforge.kolmafia.textui.parsetree;

public abstract class Evaluable extends Command {
  public abstract Type getType();

  public Type getRawType() {
    return this.getType();
  }

  public abstract String toString();

  public String toQuotedString() {
    return this.toString();
  }

  public boolean evaluatesTo(final Value value) {
    return this instanceof Value.LocatedValue && ((Value.LocatedValue) this).value == value;
  }
}
