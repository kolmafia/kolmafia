package net.sourceforge.kolmafia.swingui.listener;

import java.awt.Component;
import java.awt.Container;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import javax.swing.LayoutFocusTraversalPolicy;

public class DefaultComponentFocusTraversalPolicy extends LayoutFocusTraversalPolicy {
  private final WeakReference<Component> component;

  public DefaultComponentFocusTraversalPolicy(Component component) {
    this.component = new WeakReference<>(component);

    this.setComparator(getComparator());
  }

  @Override
  public void setComparator(Comparator<? super Component> c) {
    if (c != null) {
      super.setComparator(new DefaultComponentFirstComparator(c));
    }
  }

  @Override
  public Component getDefaultComponent(Container container) {
    Component component = this.component.get();

    if (component != null) {
      return component;
    }

    return super.getDefaultComponent(container);
  }

  private class DefaultComponentFirstComparator implements Comparator<Component> {
    private final Comparator<? super Component> parent;

    public DefaultComponentFirstComparator(Comparator<? super Component> parent) {
      this.parent = parent;
    }

    @Override
    public int compare(Component o1, Component o2) {
      Component defaultComponent = DefaultComponentFocusTraversalPolicy.this.component.get();

      if (defaultComponent == null) {
        return this.parent.compare(o1, o2);
      }

      int compare1 = this.parent.compare(o1, defaultComponent);
      int compare2 = this.parent.compare(o2, defaultComponent);

      // If either o1 or o2 is the default component, that
      // comes first

      if (compare1 == 0) {
        return -1;
      }

      if (compare2 == 0) {
        return 1;
      }

      // Otherwise, they both occur in the same direction relative
      // to the default component, just compare them.

      return this.parent.compare(o1, o2);
    }
  }
}
