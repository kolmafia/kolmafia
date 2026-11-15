package net.sourceforge.kolmafia.combat;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/** A node of a parsed custom combat script: the lookup holds strategies, which hold actions. */
public abstract class CustomCombatNode {
  private final String name;
  private final boolean allowsChildren;
  private final List<CustomCombatNode> children = new ArrayList<>();

  protected CustomCombatNode(final String name, final boolean allowsChildren) {
    this.name = name;
    this.allowsChildren = allowsChildren;
  }

  public boolean getAllowsChildren() {
    return this.allowsChildren;
  }

  public void add(final CustomCombatNode child) {
    if (!this.allowsChildren) {
      throw new IllegalStateException("node does not allow children");
    }
    this.children.add(child);
  }

  public int getChildCount() {
    return this.children.size();
  }

  public CustomCombatNode getChildAt(final int index) {
    return this.children.get(index);
  }

  public CustomCombatNode getLastChild() {
    if (this.children.isEmpty()) {
      throw new NoSuchElementException("node has no children");
    }
    return this.children.get(this.children.size() - 1);
  }

  public void removeAllChildren() {
    this.children.clear();
  }

  @Override
  public String toString() {
    return this.name == null ? "" : this.name;
  }
}
