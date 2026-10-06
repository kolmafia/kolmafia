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

      boolean first1 = isDefaultOrAncestor(o1, defaultComponent);
      boolean first2 = isDefaultOrAncestor(o2, defaultComponent);

      if (first1 != first2) {
        return first1 ? -1 : 1;
      }

      return this.parent.compare(o1, o2);
    }

    private static boolean isDefaultOrAncestor(Component c, Component defaultComponent) {
      return c == defaultComponent
          || (c instanceof Container container && container.isAncestorOf(defaultComponent));
    }
  }
}
