package net.sourceforge.kolmafia.textui.parsetree;

import org.eclipse.lsp4j.Location;

public abstract class Symbol implements ParseTreeNode, Comparable<Symbol> {
  public final String name;
  public final Location location;

  public Symbol(final String name, final Location location) {
    this.name = name;
    this.location = location;
  }

  public String getName() {
    return this.name;
  }

  public Location getLocation() {
    return this.location;
  }

  public Location getDefinitionLocation() {
    return this.location;
  }

  @Override
  public String toString() {
    return this.name;
  }

  public int compareTo(final Symbol o) {
    if (!(o instanceof Symbol)) {
      throw new ClassCastException();
    }
    if (this.name == null) {
      return 1;
    }
    return this.name.compareToIgnoreCase(o.name);
  }

  /** For error propagation only */
  public static interface BadNode {}

  public boolean isBad() {
    return this instanceof BadNode;
  }
}
